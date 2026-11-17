/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.manager.engineplugin.common.launch.process

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.manager.common.protocol.bml.BmlResource
import org.apache.linkis.manager.engineplugin.common.conf.{EngineConnPluginConf, EnvConfiguration}
import org.apache.linkis.manager.engineplugin.common.conf.EnvConfiguration.LINKIS_PUBLIC_MODULE_PATH
import org.apache.linkis.manager.engineplugin.common.exception.EngineConnBuildFailedException
import org.apache.linkis.manager.engineplugin.common.launch.entity.{
  EngineConnBuildRequest,
  RicherEngineConnBuildRequest
}
import org.apache.linkis.manager.engineplugin.common.launch.process.Environment._
import org.apache.linkis.manager.engineplugin.common.launch.process.LaunchConstants._
import org.apache.linkis.manager.engineplugin.common.util.NodeResourceUtils
import org.apache.linkis.manager.engineplugin.errorcode.EngineconnCoreErrorCodeSummary._
import org.apache.linkis.manager.label.entity.engine.EngineTypeLabel

import org.apache.commons.lang3.StringUtils

import java.io.File
import java.nio.file.Paths
import java.util

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

abstract class JavaProcessEngineConnLaunchBuilder
    extends ProcessEngineConnLaunchBuilder
    with Logging {

  private var engineConnResourceGenerator: EngineConnResourceGenerator = _

  def setEngineConnResourceGenerator(
      engineConnResourceGenerator: EngineConnResourceGenerator
  ): Unit =
    this.engineConnResourceGenerator = engineConnResourceGenerator

  protected def getGcLogDir(engineConnBuildRequest: EngineConnBuildRequest): String =
    variable(LOG_DIRS) + "/gc"

  protected def getLogDir(engineConnBuildRequest: EngineConnBuildRequest): String =
    s" -Dlogging.file=${EnvConfiguration.LOG4J2_XML_FILE.getValue} " +
      s" -D$TICKET_ID_KEY=${engineConnBuildRequest.ticketId}"

  override protected def getCommands(implicit
      engineConnBuildRequest: EngineConnBuildRequest
  ): Array[String] = {
    val commandLine: ArrayBuffer[String] = ArrayBuffer[String]()
    commandLine += (variable(JAVA_HOME) + "/bin/java")
    commandLine += "-server"

    val properties = engineConnBuildRequest.engineConnCreationDesc.properties
    var settingClientMemory = ""
    if (properties.containsKey(EngineConnPluginConf.JAVA_ENGINE_REQUEST_MEMORY.key)) {
      settingClientMemory = properties.get(EngineConnPluginConf.JAVA_ENGINE_REQUEST_MEMORY.key)
    }
    if (StringUtils.isNotBlank(settingClientMemory)) {
      commandLine += ("-Xmx" + NodeResourceUtils.formatJavaOptionMemoryWithDefaultUnitG(
        settingClientMemory
      ))
    } else {
      val engineConnMemory = EngineConnPluginConf.JAVA_ENGINE_REQUEST_MEMORY.getValue.toString
      commandLine += ("-Xmx" + engineConnMemory)
    }

    val javaOPTS = getExtractJavaOpts
    if (StringUtils.isNotEmpty(EnvConfiguration.ENGINE_CONN_DEFAULT_JAVA_OPTS.getValue)) {
      EnvConfiguration.ENGINE_CONN_DEFAULT_JAVA_OPTS.getValue
        .format(getGcLogDir(engineConnBuildRequest))
        .split("\\s+")
        .foreach(commandLine += _)
    }
    if (StringUtils.isNotEmpty(javaOPTS)) javaOPTS.split("\\s+").foreach(commandLine += _)
    if (EnvConfiguration.ENGINE_CONN_KERBEROS_ENABLE.getValue) {
      commandLine += "-Dsun.net.inetaddr.ttl=0"
      commandLine += "-Dsun.security.jgss.native=false"
    }
    getLogDir(engineConnBuildRequest).trim.split(" ").foreach(commandLine += _)
    commandLine += ("-Djava.io.tmpdir=" + variable(TEMP_DIRS))
    if (EnvConfiguration.ENGINE_CONN_DEBUG_ENABLE.getValue) {
      commandLine += s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${variable(RANDOM_PORT)}"
    }
    commandLine += "-cp"
    commandLine += variable(CLASSPATH)
    commandLine += getMainClass
    commandLine ++= Seq(
      "1>",
      s"${variable(LOG_DIRS)}/stdout",
      "2>>",
      s"${variable(LOG_DIRS)}/stderr"
    )
    commandLine.toArray
  }

  protected def getMainClass: String = EngineConnPluginConf.ENGINECONN_MAIN_CLASS.getValue

  override protected def getEnvironment(implicit
      engineConnBuildRequest: EngineConnBuildRequest
  ): util.Map[String, String] = {
    logger.info("Setting up the launch environment for engineconn.")
    val environment = new util.HashMap[String, String]
    if (ifAddHiveConfigPath) {
      addPathToClassPath(environment, variable(HADOOP_CONF_DIR))
      addPathToClassPath(environment, variable(HIVE_CONF_DIR))
    }
    // first, add engineconn conf dirs.
    addPathToClassPath(environment, buildPath(Seq(variable(PWD), ENGINE_CONN_CONF_DIR_NAME)))
    // then, add LINKIS_CONF_DIR conf dirs.
    addPathToClassPath(environment, buildPath(Seq(EnvConfiguration.LINKIS_CONF_DIR.getValue)))
    // then, add engineconn libs.
    addPathToClassPath(environment, buildPath(Seq(variable(PWD), ENGINE_CONN_LIB_DIR_NAME + "/*")))
    // then, add public modules.
    if (!enablePublicModule) {
      addPathToClassPath(environment, buildPath(Seq(LINKIS_PUBLIC_MODULE_PATH.getValue + "/*")))
    }
    // then, add the web container dir resolved from WEB_CONTAINER (REQ-06 FR-3).
    // 与 sbin/ext/container-common.sh 三态语义统一：jetty|tomcat 用开源容器目录，bes 用 BES_PATH；
    // 目标目录缺失或无 jar 时回退开源容器目录；两者皆无则打 ERROR（引擎将因无 ServletWebServerFactory 启动失败）。
    val containerDir = resolveWebContainerDir
    containerDir match {
      case Some(dir) =>
        addPathToClassPath(environment, dir + "/*")
        logger.info(s"EngineConn web container added to classpath: $dir")
      case None =>
        logger.error(
          "ERROR|引擎侧 Web 容器目录均缺失，classpath 无容器，EngineConn 将因 " +
            "MissingWebServerFactoryBeanException 启动失败，请检查 WEB_CONTAINER / " +
            "JETTY_CONTAINER_PATH / BES_PATH 配置"
        )
    }
    // finally, add the suitable properties key to classpath
    val taskClassPathFiles = EnvConfiguration.ENGINE_CONN_CLASSPATH_FILES.getValue(
      engineConnBuildRequest.engineConnCreationDesc.properties
    )
    if (StringUtils.isNotBlank(taskClassPathFiles)) {
      taskClassPathFiles
        .split(",")
        .filter(StringUtils.isNotBlank(_))
        .foreach(file => addPathToClassPath(environment, buildPath(Seq(file))))
    }
    getExtraClassPathFile.filter(StringUtils.isNotBlank(_)).foreach { file: String =>
      addPathToClassPath(environment, buildPath(Seq(new File(file).getName)))
    }
    environment
  }

  override protected def getNecessaryEnvironment(implicit
      engineConnBuildRequest: EngineConnBuildRequest
  ): Array[String] =
    if (!ifAddHiveConfigPath) Array.empty
    else Array(HADOOP_CONF_DIR.toString, HIVE_CONF_DIR.toString)

  /**
   * REQ-06 FR-3：按 WEB_CONTAINER 解析引擎侧 Web 容器目录。
   *
   * 三态语义与 sbin/ext/container-common.sh 保持一致：
   * -jetty|tomcat -> 开源容器目录（ENGINE_CONN_CONTAINER_PATH，目录名 linkis-jetty，内容由出包 profile 决定，运行时以
   * WEB_CONTAINER 对齐 Spring 定制器条件）
   * -bes -> BES_PATH（ENGINE_CONN_BES_PATH）
   * -非法值 -> 回退 jetty
   *
   * fail-safe：主目录缺失或无 jar 时回退开源容器目录（与 container-common.sh 一致）， 两者皆不可用返回 None，由调用侧打 ERROR
   * 日志，绝不静默拼入空目录。
   */
  protected def resolveWebContainerDir: Option[String] = {
    val mode = Option(EnvConfiguration.ENGINE_CONN_WEB_CONTAINER.getValue)
      .map(_.trim.toLowerCase)
      .filter(m => m == "jetty" || m == "tomcat" || m == "bes")
      .getOrElse {
        logger.warn(
          s"非法 WEB_CONTAINER=${EnvConfiguration.ENGINE_CONN_WEB_CONTAINER.getValue}，回退 jetty"
        )
        "jetty"
      }
    val openSourceDir = Option(EnvConfiguration.ENGINE_CONN_CONTAINER_PATH.getValue)
      .map(_.trim)
      .filter(_.nonEmpty)
    val primaryDir =
      if (mode == "bes") {
        Option(EnvConfiguration.ENGINE_CONN_BES_PATH.getValue).map(_.trim).filter(_.nonEmpty)
      } else openSourceDir
    val resolved =
      primaryDir.filter(containerDirUsable).orElse(openSourceDir.filter(containerDirUsable))
    if (resolved.isEmpty) {
      logger.error(
        s"ERROR|引擎侧 Web 容器目录不可用(mode=$mode, primary=${primaryDir.orNull}, " +
          s"fallback=${openSourceDir.orNull})：目录不存在、无 jar 或过滤后无剩余 jar"
      )
    }
    resolved
  }

  /**
   * 判断容器目录可用：目录存在，且存在至少一个未被排除清单过滤掉的 jar。 排除清单见 ENGINE_CONN_CONTAINER_EXCLUDE_JARS（默认过滤
   * jakarta.servlet-api / tomcat-embed-el， 避免与 Spring Boot 2.7 + JDK8 的 javax.servlet 命名空间冲突，同
   * REQ-04 双代冲突一类）。
   */
  protected def containerDirUsable(dir: String): Boolean = {
    val file = new File(dir)
    if (!file.isDirectory) return false
    val excluded = excludedContainerJarPrefixes
    Option(file.list())
      .exists(names =>
        names.exists(name =>
          name.endsWith(".jar") && !excluded.exists(prefix => name.startsWith(prefix))
        )
      )
  }

  protected def excludedContainerJarPrefixes: Seq[String] =
    Option(EnvConfiguration.ENGINE_CONN_CONTAINER_EXCLUDE_JARS.getValue)
      .map(
        _.split(",")
          .map(_.trim)
          .filter(_.nonEmpty)
          .toSeq
      )
      .getOrElse(Seq.empty)

  protected def getExtractJavaOpts: String = EnvConfiguration.ENGINE_CONN_JAVA_EXTRA_OPTS.getValue

  protected def getExtraClassPathFile: Array[String] =
    EnvConfiguration.ENGINE_CONN_JAVA_EXTRA_CLASSPATH.getValue.split(",")

  protected def ifAddHiveConfigPath: Boolean = false

  protected def enablePublicModule: Boolean = false

  override protected def getBmlResources(implicit
      engineConnBuildRequest: EngineConnBuildRequest
  ): util.List[BmlResource] = {
    val engineType = engineConnBuildRequest.labels.asScala
      .find(_.isInstanceOf[EngineTypeLabel])
      .map { case engineTypeLabel: EngineTypeLabel => engineTypeLabel }
      .getOrElse(
        throw new EngineConnBuildFailedException(
          ETL_NOT_EXISTS.getErrorCode,
          ETL_NOT_EXISTS.getErrorDesc
        )
      )
    val engineConnResource = engineConnResourceGenerator.getEngineConnBMLResources(engineType)
    Array(
      engineConnResource.getConfBmlResource,
      engineConnResource.getLibBmlResource
    ) ++: engineConnResource.getOtherBmlResources.toList
  }.asJava

  private def buildPath(paths: Seq[String]): String =
    Paths.get(paths.head, paths.tail: _*).toFile.getPath

}
