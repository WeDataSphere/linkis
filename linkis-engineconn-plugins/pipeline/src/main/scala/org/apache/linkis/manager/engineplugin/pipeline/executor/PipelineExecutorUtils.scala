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

package org.apache.linkis.manager.engineplugin.pipeline.executor

import org.apache.linkis.common.conf.{CommonVars, Configuration}
import org.apache.linkis.common.io.{Fs, FsPath}
import org.apache.linkis.common.utils.Utils
import org.apache.linkis.storage.FSFactory

import org.slf4j.LoggerFactory

/**
 * REQ-01 _ai 用户切换：pipeline 写入侧工具。 设计文档 v1.2 定稿 §3.5（M5）。
 */
object PipelineExecutorUtils {

  private val logger = LoggerFactory.getLogger(getClass)

  // 与 WorkSpaceConfiguration 同 key 镜像（pipeline 模块不依赖 pes-publicservice，
  // 同 key 声明保证两侧取值天然一致，模式同 ADR-001）
  private val LOCAL_USER_ROOT_PATH: CommonVars[String] =
    CommonVars("wds.linkis.filesystem.root.path", "file:///tmp/linkis/")

  private val HDFS_USER_ROOT_PATH_PREFIX: CommonVars[String] =
    CommonVars("wds.linkis.filesystem.hdfs.root.path", "hdfs:///tmp/")

  private val HDFS_USER_ROOT_PATH_SUFFIX: CommonVars[String] =
    CommonVars("wds.linkis.workspace.filesystem.hdfsuserrootpath.suffix", "/linkis/")

  /**
   * 获取用于写入目标路径的 FS。 触发条件（三者同时成立）才切换为原用户代理 FS： 1) 开关 linkis.ai.user.enable 开启（热加载） 2) 执行身份为
   * _ai（executorUser 以 AI_USER_SUFFIX 结尾） 3) 目标路径归属原用户工作空间根（本地 file 根或 HDFS 根） 任一不满足或异常 → 返回
   * None，调用方回落原逻辑 FSFactory.getFs（现网行为）。
   */
  def getDestFsByOwner(executorUser: String, destFsPath: FsPath): Option[Fs] = {
    Utils.tryCatch {
      val suffix = Configuration.AI_USER_SUFFIX.getValue
      if (executorUser == null || !Configuration.AI_USER_ENABLE.getHotValue) {
        None
      } else if (suffix.isEmpty || !executorUser.endsWith(suffix)) {
        None
      } else {
        val origUser = executorUser.substring(0, executorUser.length - suffix.length)
        if (isUnderOwnerRoot(destFsPath, origUser)) {
          Option(FSFactory.getFsByProxyUser(destFsPath, origUser))
        } else {
          None
        }
      }
    } { t =>
      logger.warn(
        s"getDestFsByOwner failed, fallback to default fs, executorUser=$executorUser, " +
          s"destPath=${if (destFsPath == null) "" else destFsPath.getPath}",
        t
      )
      None
    }
  }

  /** destFsPath 是否归属原用户的工作空间根（仅支持本地 file 与 HDFS，其他类型不切换） */
  private def isUnderOwnerRoot(destFsPath: FsPath, origUser: String): Boolean = {
    if (destFsPath == null) {
      return false
    }
    // FsPath.getPath() 返回不含 scheme 的路径，配置根（file:/// 或 hdfs:///）须剥掉 scheme 再比较
    val fsType = destFsPath.getFsType
    if ("file".equalsIgnoreCase(fsType)) {
      isUnder(destFsPath.getPath, stripScheme(LOCAL_USER_ROOT_PATH.getValue) + origUser)
    } else if ("hdfs".equalsIgnoreCase(fsType)) {
      isUnder(
        destFsPath.getPath,
        stripScheme(
          HDFS_USER_ROOT_PATH_PREFIX.getValue + origUser + HDFS_USER_ROOT_PATH_SUFFIX.getValue
        )
      )
    } else {
      false
    }
  }

  /** 剥离路径的 scheme 与 authority（如 hdfs://nameservice1），返回裸路径，与 FsPath.getPath() 对齐 */
  private def stripScheme(p: String): String = {
    var n = normalizePath(p)
    val idx = n.indexOf("://")
    if (idx >= 0) {
      n = n.substring(idx + 3)
      val slash = n.indexOf('/')
      if (slash > 0) {
        n = n.substring(slash)
      } else if (slash != 0) {
        return ""
      }
    }
    n
  }

  /** 路径归一：统一分隔符、去尾部斜杠 */
  private def normalizePath(path: String): String = {
    var p = path.trim.replace('\\', '/')
    while (p.length > 1 && p.endsWith("/")) {
      p = p.substring(0, p.length - 1)
    }
    p
  }

  /** p 是否等于 root 或在 root 之下（按路径段判定，不用 contains；忽略大小写） */
  private def isUnder(p: String, root: String): Boolean = {
    val r = normalizePath(root)
    val n = normalizePath(p)
    n.equalsIgnoreCase(r) || n.toLowerCase.startsWith(r.toLowerCase + "/")
  }

}
