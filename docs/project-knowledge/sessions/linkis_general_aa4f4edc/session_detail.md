
## 交互记录 (2026-04-21T09:18:52.618Z)

**用户**：
[{'type': 'text', 'text': '<ide_opened_file>The user opened the file g:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java in the IDE. This may or may not be related to the current task.</ide_opened_file>'}, {'type': 'text', 'text': '帮忙分析下spark引擎启动的时候是不是会执行init_sql'}]

**AI**：
[思考] 用户想了解 Spark 引擎启动时是否会执行 init_sql。我需要分析 Spark 引擎的启动流程，特别是在初始化阶段是否会处理 init_sql 配置。

让我先搜索相关代码：
1. Spark 引擎配置相关
2. init_sql 相关的配置和处理逻辑
3. Spark 引擎启动流程

我应该搜索关键词：
- init_sql
- spark 初始化配置
- SparkEngineConn 或相关启动类...


## 交互记录 (2026-04-21T09:18:59.811Z)

**用户**：
[{'tool_use_id': 'call_c9f40c742b3f4c1bacefd7c0', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
我来帮你分析 Spark 引擎启动时是否会执行 init_sql。让我先搜索相关代码。...


## 交互记录 (2026-04-21T09:18:59.815Z)

**用户**：
[{'tool_use_id': 'call_2d2a853641af449d88226236', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
[思考] 没有找到 init_sql 相关的配置。让我换个角度搜索 Spark 引擎的配置和初始化相关内容。...


## 交互记录 (2026-04-21T09:19:11.977Z)

**用户**：
[{'tool_use_id': 'call_5dc1224ed1a743c88777947a', 'type': 'tool_result', 'content': 'Found 7 files\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\SparkEngineConnExecutor.scala\nlinkis-engineconn-plugins\\spark\\src\\test\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\TestSparkSqlExecutor.scala\nlinkis-engineconn-plugins\\spark\\src\\test\\scala\\org\\apache\\linkis\\engineplugin\\spark\\cs\\TestCSSparkHelper.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\SparkPythonExecutor.scala\nlinkis-engineconn-plugins\\spark\\src\\test\\scala\\org\\apache\\linkis\\engineplugin\\spark\\cs\\TestCSSparkPostExecutionHook.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\SparkScalaExecutor.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\resources\\python\\mix_pyspark.py'}]

**AI**：
[思考] 让我搜索更广泛的关键词，可能 init_sql 是一个通用的配置项，不仅限于 Spark 引擎。让我在整个项目中搜索。...


## 交互记录 (2026-04-21T09:19:24.145Z)

**用户**：
[{'tool_use_id': 'call_6c6f22a3f587486d86ec626d', 'type': 'tool_result', 'content': 'linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\SparkEngineConnPlugin.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\context\\SparkEngineConnContext.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\factory\\SparkEngineConnResourceFactory.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\factory\\SparkEngineConnFactory.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\launch\\SparkEngineConnLaunchBuilder.scala\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\SparkEngineConnExecutor.scala'}]

**AI**：
[思考] 找到了 `InitSQLHook.scala`，这看起来是处理 init_sql 的关键文件。让我读取这个文件以及相关的配置文件。...


## 交互记录 (2026-04-21T09:19:28.478Z)

**用户**：
[{'tool_use_id': 'call_527efccdf7c54eb88f08be85', 'type': 'tool_result', 'content': 'Found 1 file\nlinkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\config\\SparkConfiguration.scala'}]

**AI**：
找到了关键文件！让我读取相关文件来分析：...


## 交互记录 (2026-04-21T09:19:48.072Z)

**用户**：
[{'tool_use_id': 'call_c39731d05800471bb7a706b8', 'type': 'tool_result', 'content': 'Found 7 files\nlinkis-engineconn-plugins\\hive\\src\\main\\resources\\linkis-engineconn.properties\nlinkis-engineconn-plugins\\spark\\src\\main\\resources\\linkis-engineconn.properties\ndocs\\configuration\\linkis-computation-engineconn.md\n.github\\workflows\\check-sql-script.yml\nlinkis-public-enhancements\\linkis-udf-service\\src\\main\\java\\org\\apache\\linkis\\udf\\service\\impl\\UDFServiceImpl.java\nlinkis-public-enhancements\\linkis-udf-service\\src\\main\\java\\org\\apache\\linkis\\udf\\service\\UDFService.java\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\InitSQLHook.scala'}]

**AI**：
[思考] 非常好！我已经找到了相关代码。让我总结分析一下：

1. **InitSQLHook 类**：这是执行 init_sql 的核心类
2. **SparkInitSQLHook**：Spark 引擎专用的 init_sql hook，继承自 InitSQLHook
3. **配置项**：
   - `wds.linkis.bdp.hive.init.sql.dir`：init_sql 文件目录
   - `wds.linkis.bdp.hive.init.sql.enable`：是否启用 init_sql

从代码分析：
1. Spark 引擎配置中注册了 `SparkInitSQLHook`（在 linkis-engineconn.properties 第27行）
2. InitSQLHook 在 `afterExecutionExecute` 方法中执行 init_sql
3. 执行流程：
   - 检查配置是否启用
   - 读取用户对应的 init_sql 文件（格式：{user}_hive.sql）
   - 创建一个内部任务执行 SQL

让我再确认一下这个 hook 的执行时机。...


## 交互记录 (2026-04-21T09:19:52.371Z)

**用户**：
[{'tool_use_id': 'call_72207e436d6d4da1b109600d', 'type': 'tool_result', 'content': '1\t# \n2\t# Licensed to the Apache Software Foundation (ASF) under one or more\n3\t# contributor license agreements.  See the NOTICE file distributed with\n4\t# this work for additional information regarding copyright ownership.\n5\t# The ASF licenses this file to You under the Apache License, Version 2.0\n6\t# (the "License"); you may not use this file except in compliance with\n7\t# the License.  You may obtain a copy of the License at\n8\t# http://www.apache.org/licenses/LICENSE-2.0\n9\t# Unless required by applicable law or agreed to in writing, software\n10\t# distributed under the License is distributed on an "AS IS" BASIS,\n11\t# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n12\t# See the License for the specific language governing permissions and\n13\t# limitations under the License.\n14\t#\n15\t\n16\t#\n17\twds.linkis.server.version=v1\n18\t\n19\twds.linkis.engineconn.spark.conf.enable=true\n20\t\n21\twds.linkis.engineconn.debug.enable=true\n22\t\n23\t#wds.linkis.keytab.enable=true\n24\t\n25\twds.linkis.engineconn.plugin.default.class=org.apache.linkis.engineplugin.spark.SparkEngineConnPlugin\n26\t\n27\twds.linkis.engine.connector.hooks=org.apache.linkis.engineconn.computation.executor.hook.ComputationEngineConnHook,org.apache.linkis.engineconn.computation.executor.hook.PyUdfEngineHook,org.apache.linkis.engineconn.computation.executor.hook.ScalaUdfEngineHook,org.apache.linkis.engineconn.computation.executor.hook.JarUdfEngineHook,org.apache.linkis.engineconn.computation.executor.hook.SparkInitSQLHook,org.apache.linkis.engineconn.computation.executor.hook.PythonSparkEngineHook,org.apache.linkis.engineplugin.spark.hooks.SparkContainerizationEngineConnHook\n28\t\n29\tlinkis.spark.once.yarn.restful.url=http://127.0.0.1:8088\n30\t\n31\twds.linkis.fs.hdfs.impl.disable.cache=true'}]

**AI**：
现在让我查看 Hook 的执行时机：...


## 交互记录 (2026-04-21T09:19:52.375Z)

**用户**：
[{'tool_use_id': 'call_651905fabd4848c4988deaf7', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.engineplugin.spark.config\n19\t\n20\timport org.apache.linkis.common.conf.{CommonVars, TimeType}\n21\timport org.apache.linkis.common.utils.{ClassUtils, Logging}\n22\timport org.apache.linkis.engineplugin.spark.factory.SparkEngineConnFactory\n23\t\n24\timport java.io.File\n25\t\n26\t/**\n27\t */\n28\tobject SparkConfiguration extends Logging {\n29\t\n30\t  val SPARK_HOME_ENV = "SPARK_HOME"\n31\t  val SPARK_CONF_DIR_ENV = "SPARK_CONF_DIR"\n32\t\n33\t  val SPARK_YARN_CLIENT = "client"\n34\t\n35\t  val SPARK_YARN_CLUSTER = "cluster"\n36\t\n37\t  val PROCESS_MAX_THREADS = CommonVars[Int]("wds.linkis.process.threadpool.max", 100)\n38\t\n39\t  val SPARK_SESSION_HOOK = CommonVars[String]("wds.linkis.engine.spark.session.hook", "")\n40\t\n41\t  val SPARK_LOOP_INIT_TIME =\n42\t    CommonVars[TimeType]("wds.linkis.engine.spark.spark-loop.init.time", new TimeType("120s"))\n43\t\n44\t  val SPARK_LANGUAGE_REPL_INIT_TIME =\n45\t    CommonVars[TimeType]("wds.linkis.engine.spark.language-repl.init.time", new TimeType("30s"))\n46\t\n47\t  val SPARK_REPL_CLASSDIR = CommonVars[String]("spark.repl.classdir", "", "默认master")\n48\t\n49\t  val PROXY_USER = CommonVars[String]("spark.proxy.user", "")\n50\t\n51\t  val SPARK_DEPLOY_MODE = CommonVars[String]("spark.submit.deployMode", "client")\n52\t\n53\t  val SPARK_YARN_CLUSTER_JARS =\n54\t    CommonVars[String]("linkis.spark.yarn.cluster.jars", "hdfs:///spark/cluster")\n55\t\n56\t  val SPARK_APP_NAME = CommonVars[String]("spark.app.name", "Linkis-EngineConn-Spark")\n57\t  val SPARK_APP_RESOURCE = CommonVars[String]("spark.app.resource", "")\n58\t  val SPARK_APP_CONF = CommonVars[String]("spark.extconf", "")\n59\t\n60\t  val SPARK_K8S_CONFIG_FILE = CommonVars[String]("linkis.spark.k8s.config.file", "~/.kube/config")\n61\t  val SPARK_K8S_SERVICE_ACCOUNT = CommonVars[String]("linkis.spark.k8s.serviceAccount", "")\n62\t  val SPARK_K8S_MASTER_URL = CommonVars[String]("linkis.spark.k8s.master.url", "")\n63\t  val SPARK_K8S_USERNAME = CommonVars[String]("linkis.spark.k8s.username", "")\n64\t  val SPARK_K8S_PASSWORD = CommonVars[String]("linkis.spark.k8s.password", "")\n65\t  val SPARK_K8S_IMAGE = CommonVars[String]("linkis.spark.k8s.image", "apache/spark:v3.2.1")\n66\t  val SPARK_K8S_IMAGE_PULL_POLICY = CommonVars[String]("linkis.spark.k8s.imagePullPolicy", "Always")\n67\t  val SPARK_K8S_LANGUAGE_TYPE = CommonVars[String]("linkis.spark.k8s.languageType", "Scala")\n68\t  val SPARK_K8S_RESTART_POLICY = CommonVars[String]("linkis.spark.k8s.restartPolicy", "Never")\n69\t  val SPARK_K8S_SPARK_VERSION = CommonVars[String]("linkis.spark.k8s.sparkVersion", "3.2.1")\n70\t  val SPARK_K8S_NAMESPACE = CommonVars[String]("linkis.spark.k8s.namespace", "default")\n71\t  val SPARK_K8S_UI_PORT = CommonVars[String]("linkis.spark.k8s.ui.port", "4040")\n72\t\n73\t  val SPARK_K8S_EXECUTOR_REQUEST_CORES =\n74\t    CommonVars[String]("linkis.spark.k8s.executor.request.cores", "1")\n75\t\n76\t  val SPARK_K8S_DRIVER_REQUEST_CORES =\n77\t    CommonVars[String]("linkis.spark.k8s.driver.request.cores", "1")\n78\t\n79\t  val SPARK_KUBERNETES_FILE_UPLOAD_PATH =\n80\t    CommonVars[String]("spark.kubernetes.file.upload.path", "local:///opt/spark/tmp")\n81\t\n82\t  val SPARK_PYTHON_VERSION = CommonVars[String]("spark.python.version", "python")\n83\t\n84\t  val SPARK_PYTHON_FILES = CommonVars[String]("spark.submit.pyFiles", "")\n85\t\n86\t  val SPARK_PYTHON_TEST_MODE_ENABLE =\n87\t    CommonVars[Boolean]("linkis.spark.python.test.mode.enable", false)\n88\t\n89\t  val SPARK_SCALA_KILL_COLSE_THREAD_ENABLE =\n90\t    CommonVars[Boolean]("linkis.spark.scala.kill.close.thread.enable", true)\n91\t\n92\t  val SPARK_PYTHON_TEST_MODE_MIX__PYSHELL_PATH = CommonVars[String](\n93\t    "linkis.spark.python.mix.pyshell.path",\n94\t    "/appcom/Install/linkis/mix_pyspark.py"\n95\t  )\n96\t\n97\t  val SPARK_EXTRA_JARS = CommonVars[String](\n98\t    "spark.jars",\n99\t    "",\n100\t    "Additional jar package, Driver and Executor take effect（额外的jar包，Driver和Executor生效）"\n101\t  )\n102\t\n103\t  val SPARK_SUBMIT_PATH = CommonVars[String]("wds.linkis.spark.sparksubmit.path", "spark-submit")\n104\t\n105\t  val MAPRED_OUTPUT_COMPRESS = CommonVars[Boolean](\n106\t    "mapred.output.compress",\n107\t    false,\n108\t    "Whether the map output is compressed（map输出结果是否压缩）"\n109\t  )\n110\t\n111\t  val MAPRED_OUTPUT_COMPRESSION_CODEC = CommonVars[String](\n112\t    "mapred.output.compression.codec",\n113\t    "org.apache.hadoop.io.compress.GzipCodec",\n114\t    "Map output compression method（map输出结果压缩方式）"\n115\t  )\n116\t\n117\t  val SPARK_MASTER = CommonVars[String]("spark.master", "yarn", "Default yarn（默认yarn）")\n118\t\n119\t  val SPARK_CONSOLE_OUTPUT_NUM = CommonVars[Int]("wds.linkis.spark.output.line.limit", 10)\n120\t\n121\t  val LINKIS_SPARK_USEHIVECONTEXT = CommonVars[Boolean]("wds.linkis.spark.useHiveContext", true)\n122\t\n123\t  val DEFAULT_SPARK_JAR_NAME =\n124\t    CommonVars[String]("wds.linkis.ecp.spark.default.jar", "linkis-engineconn-core-1.8.0.jar")\n125\t\n126\t  val ENGINE_JAR = CommonVars[String]("wds.linkis.enginemanager.core.jar", getMainJarName)\n127\t\n128\t  val SPARK_DRIVER_CLASSPATH = CommonVars[String]("spark.driver.extraClassPath", "")\n129\t\n130\t  val SPARK_DRIVER_EXTRA_JAVA_OPTIONS = CommonVars[String](\n131\t    "spark.driver.extraJavaOptions",\n132\t    "\\"-Dwds.linkis.server.conf=linkis-engine.properties " + "\\""\n133\t  )\n134\t\n135\t  val SPARK_DEFAULT_EXTERNAL_JARS_PATH = CommonVars[String]("spark.external.default.jars", "")\n136\t\n137\t  val SPARK_CONF_DIR = CommonVars[String](\n138\t    "spark.config.dir",\n139\t    CommonVars[String]("SPARK_CONF_DIR", "/appcom/config/spark-config").getValue\n140\t  )\n141\t\n142\t  val SPARK_HOME = CommonVars[String](\n143\t    "spark.home",\n144\t    CommonVars[String]("SPARK_HOME", "/appcom/Install/spark").getValue\n145\t  )\n146\t\n147\t  val SQL_EXTENSION_TIMEOUT = CommonVars("wds.linkis.dws.ujes.spark.extension.timeout", 3000L)\n148\t  val SPARK_NF_FRACTION_LENGTH = CommonVars[Int]("wds.linkis.engine.spark.fraction.length", 30)\n149\t  val SHOW_DF_MAX_RES = CommonVars("wds.linkis.show.df.max.res", Int.MaxValue)\n150\t  val MDQ_APPLICATION_NAME = CommonVars("wds.linkis.mdq.application.name", "linkis-ps-datasource")\n151\t  val DOLPHIN_LIMIT_LEN = CommonVars("wds.linkis.dolphin.limit.len", 5000)\n152\t\n153\t  val IS_VIEWFS_ENV = CommonVars("wds.linkis.spark.engine.is.viewfs.env", true)\n154\t\n155\t  val ENGINE_SHUTDOWN_LOGS =\n156\t    CommonVars("wds.linkis.spark.engineconn.fatal.log", "error writing class;OutOfMemoryError")\n157\t\n158\t  val PYSPARK_PYTHON3_PATH =\n159\t    CommonVars[String]("pyspark.python3.path", "python3")\n160\t\n161\t  val ENABLE_REPLACE_PACKAGE_NAME =\n162\t    CommonVars("wds.linkis.spark.engine.scala.replace_package_header.enable", true)\n163\t\n164\t  val REPLACE_PACKAGE_HEADER = CommonVars(\n165\t    "wds.linkis.spark.engine.scala.replace_package.header",\n166\t    "com.webank.wedatasphere.linkis"\n167\t  )\n168\t\n169\t  val REPLACE_PACKAGE_TO_HEADER = "org.apache.linkis"\n170\t\n171\t  val LINKIS_SPARK_CONF = CommonVars[String]("spark.conf", "")\n172\t  val SPARK_APPLICATION_ARGS = CommonVars("spark.app.args", "")\n173\t  val SPARK_APPLICATION_MAIN_CLASS = CommonVars("spark.app.main.class", "")\n174\t\n175\t  val SPARK_ONCE_APP_STATUS_FETCH_INTERVAL =\n176\t    CommonVars("linkis.spark.once.app.fetch.status.interval", new TimeType("5s"))\n177\t\n178\t  val SPARK_ONCE_APP_STATUS_FETCH_FAILED_MAX =\n179\t    CommonVars("linkis.spark.once.app.fetch.status.failed.num", 3)\n180\t\n181\t  val SPARK_ONCE_YARN_RESTFUL_URL = CommonVars[String]("linkis.spark.once.yarn.restful.url", "")\n182\t\n183\t  val LINKIS_SPARK_ETL_SUPPORT_HUDI = CommonVars[Boolean]("linkis.spark.etl.support.hudi", false)\n184\t\n185\t  val LINKIS_PYSPARK_USE_SECURE_RANDOM =\n186\t    CommonVars[Boolean]("linkis.pyspark.use.secure.random", false).getValue\n187\t\n188\t  val SCALA_PARSE_APPEND_CODE =\n189\t    CommonVars("linkis.scala.parse.append.code", "val linkisVar=1").getValue\n190\t\n191\t// 多个配置用英文逗号分割\n192\t  val SPARK_ENGINE_EXTENSION_CONF =\n193\t    CommonVars("linkis.spark.engine.extension.conf", "spark.sql.shuffle.partitions=200").getValue\n194\t\n195\t  val SPARKMEASURE_AGGREGATE_TYPE = "linkis.sparkmeasure.aggregate.type"\n196\t\n197\t  val SPARKMEASURE_FLIGHT_RECORDER_TYPE =\n198\t    CommonVars[String]("linkis.sparkmeasure.flight.recorder.type", "")\n199\t\n200\t  val SPARKMEASURE_OUTPUT_PREFIX =\n201\t    CommonVars[String]("linkis.sparkmeasure.output.prefix", "/appcom/sparkmeasure")\n202\t\n203\t  val SPARKMEASURE_FLIGHT_STAGE_CLASS =\n204\t    "ch.cern.sparkmeasure.FlightRecorderStageMetrics"\n205\t\n206\t  val SPARKMEASURE_FLIGHT_TASK_CLASS = "ch.cern.sparkmeasure.FlightRecorderTaskMetrics"\n207\t\n208\t  val SPARKMEASURE_FLIGHT_RECORDER_KEY = "spark.extraListeners"\n209\t\n210\t  val SPARKMEASURE_FLIGHT_RECORDER_OUTPUT_FORMAT_KEY = "spark.sparkmeasure.outputFormat"\n211\t\n212\t  val SPARKMEASURE_FLIGHT_RECORDER_OUTPUT_FORMAT_JSON = "json"\n213\t\n214\t  val SPARKMEASURE_FLIGHT_RECORDER_OUTPUT_FORMAT_JSON_HADOOP = "json_to_hadoop"\n215\t\n216\t  val SPARKMEASURE_FLIGHT_RECORDER_OUTPUT_FILENAME_KEY = "spark.sparkmeasure.outputFilename"\n217\t\n218\t  val ENGINE_CONN_CONTAINERIZATION_MAPPING_HOST =\n219\t    CommonVars("linkis.engine.containerization.mapping.host", "")\n220\t\n221\t  val ENGINE_CONN_CONTAINERIZATION_MAPPING_PORTS =\n222\t    CommonVars("linkis.engine.containerization.mapping.ports", "")\n223\t\n224\t  val SPARK_DRIVER_HOST = CommonVars[String]("spark.driver.host", "")\n225\t\n226\t  val SPARK_DRIVER_PORT = CommonVars[String]("spark.driver.port", "")\n227\t\n228\t  val SPARK_DRIVER_BIND_ADDRESS = CommonVars[String]("spark.driver.bindAddress", "0.0.0.0")\n229\t\n230\t  val SPARK_DRIVER_BLOCK_MANAGER_PORT = CommonVars[String]("spark.driver.blockManager.port", "")\n231\t\n232\t  val SPARK_PROHIBITS_DYNAMIC_RESOURCES_SWITCH =\n233\t    CommonVars[Boolean]("linkis.spark.dynamic.resource.switch", false).getValue\n234\t\n235\t  val SPARK_EXECUTOR_PARAMS_ENABLED = CommonVars[Boolean](\n236\t    "wds.linkis.spark.executor.params.enabled",\n237\t    false,\n238\t    "Enable spark executor params setting to executor side（启用Spark executor参数设置）"\n239\t  )\n240\t\n241\t  val SPARK_EXECUTOR_PARAMS_EXCLUDE = CommonVars[String](\n242\t    "wds.linkis.spark.executor.params.exclude",\n243\t    "",\n244\t    "Exclude params from setting to executor side, split by comma（排除的executor参数，逗号分隔）"\n245\t  )\n246\t\n247\t  private def getMainJarName(): String = {\n248\t    val somePath = ClassUtils.jarOfClass(classOf[SparkEngineConnFactory])\n249\t    if (somePath.isDefined) {\n250\t      val path = new File(somePath.get)\n251\t      path.getName\n252\t    } else {\n253\t      DEFAULT_SPARK_JAR_NAME.getValue\n254\t    }\n255\t  }\n256\t\n257\t}\n258\t'}]

**AI**：
[思考] 让我查看 `afterExecutionExecute` 是在什么时候被调用的。我需要找到调用这个 hook 的地方。...


## 交互记录 (2026-04-21T09:19:52.380Z)

**用户**：
[{'tool_use_id': 'call_61268f35ad1a40ad8282e8d1', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.engineconn.computation.executor.hook\n19\t\n20\timport org.apache.linkis.common.conf.CommonVars\n21\timport org.apache.linkis.common.utils.{Logging, Utils}\n22\timport org.apache.linkis.engineconn.common.creation.EngineCreationContext\n23\timport org.apache.linkis.engineconn.common.engineconn.EngineConn\n24\timport org.apache.linkis.engineconn.common.hook.EngineConnHook\n25\timport org.apache.linkis.engineconn.computation.executor.entity.CommonEngineConnTask\n26\timport org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor\n27\timport org.apache.linkis.engineconn.core.executor.ExecutorManager\n28\timport org.apache.linkis.governance.common.entity.ExecutionNodeStatus\n29\timport org.apache.linkis.manager.label.entity.Label\n30\timport org.apache.linkis.manager.label.entity.engine.{CodeLanguageLabel, RunType}\n31\t\n32\timport org.apache.commons.io.FileUtils\n33\timport org.apache.commons.lang3.StringUtils\n34\t\n35\timport java.io.File\n36\timport java.util\n37\t\n38\tabstract class InitSQLHook extends EngineConnHook with Logging {\n39\t\n40\t  private val INIT_SQL_DIR =\n41\t    CommonVars("wds.linkis.bdp.hive.init.sql.dir", "/appcom/config/hive-config/init_sql/").getValue\n42\t\n43\t  private val INIT_SQL_ENABLE = CommonVars("wds.linkis.bdp.hive.init.sql.enable", false)\n44\t\n45\t  override def beforeCreateEngineConn(engineCreationContext: EngineCreationContext): Unit = {}\n46\t\n47\t  override def beforeExecutionExecute(\n48\t      engineCreationContext: EngineCreationContext,\n49\t      engineConn: EngineConn\n50\t  ): Unit = {}\n51\t\n52\t  protected def getRunType(): String\n53\t\n54\t  override def afterExecutionExecute(\n55\t      engineCreationContext: EngineCreationContext,\n56\t      engineConn: EngineConn\n57\t  ): Unit = Utils.tryAndError {\n58\t    val user: String =\n59\t      if (StringUtils.isNotBlank(engineCreationContext.getUser)) engineCreationContext.getUser\n60\t      else {\n61\t        Utils.getJvmUser\n62\t      }\n63\t    if (!INIT_SQL_ENABLE.getValue(engineCreationContext.getOptions)) {\n64\t      logger.info(s"$user engineConn skip execute init_sql")\n65\t      return\n66\t    }\n67\t\n68\t    val initSql = readFile(INIT_SQL_DIR + user + "_hive.sql")\n69\t    if (StringUtils.isBlank(initSql)) {\n70\t      logger.info(s"$user init_sql is empty")\n71\t      return\n72\t    }\n73\t    logger.info(s"$user engineConn begin to run init_sql")\n74\t    val codeLanguageLabel = new CodeLanguageLabel\n75\t    codeLanguageLabel.setCodeType(getRunType())\n76\t    val labels = Array[Label[_]](codeLanguageLabel)\n77\t    val engineConnTask = new CommonEngineConnTask("init_sql")\n78\t    engineConnTask.setLabels(labels)\n79\t    engineConnTask.setCode(initSql)\n80\t    engineConnTask.setProperties(new util.HashMap[String, AnyRef]())\n81\t    engineConnTask.setStatus(ExecutionNodeStatus.Scheduled)\n82\t    ExecutorManager.getInstance.getExecutorByLabels(labels) match {\n83\t      case executor: ComputationExecutor =>\n84\t        executor.toExecuteTask(engineConnTask, internalExecute = true)\n85\t      case _ =>\n86\t    }\n87\t    logger.info(s"$user engineConn finished to run init_sql")\n88\t  }\n89\t\n90\t  protected def readFile(path: String): String = {\n91\t    logger.info("read file: " + path)\n92\t    val file = new File(path)\n93\t    if (file.exists()) {\n94\t      FileUtils.readFileToString(file)\n95\t    } else {\n96\t      logger.info("file: [" + path + "] doesn\'t exist, ignore it.")\n97\t      ""\n98\t    }\n99\t  }\n100\t\n101\t}\n102\t\n103\tclass SparkInitSQLHook extends InitSQLHook {\n104\t\n105\t  override protected def getRunType(): String = RunType.SQL.toString\n106\t\n107\t}\n108\t\n109\tclass HiveInitSQLHook extends InitSQLHook {\n110\t\n111\t  override protected def getRunType(): String = RunType.HIVE.toString\n112\t\n113\t}\n114\t'}]

**AI**：
[思考] 找到了调用位置。让我查看 EngineConnServer.scala 中是如何调用的。...


## 交互记录 (2026-04-21T09:20:00.405Z)

**用户**：
[{'tool_use_id': 'call_4c8aae55bf1c4de8a72f1a9c', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\test\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHookTest.scala:31:class PythonModuleLoadEngineConnHookTest {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\test\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHookTest.scala:45://    hook.afterExecutionExecute(mockEngineCreationContext, mockEngineConn)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UseDatabaseEngineHook.scala:24:import org.apache.linkis.engineconn.common.hook.EngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UseDatabaseEngineHook.scala:35:abstract class UseDatabaseEngineHook extends EngineConnHook with Logging {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UseDatabaseEngineHook.scala:48:  override def afterExecutionExecute(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\ComputationEngineConnHook.scala:20:import org.apache.linkis.engineconn.callback.hook.CallbackEngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\ComputationEngineConnHook.scala:26:class ComputationEngineConnHook extends CallbackEngineConnHook {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:25:import org.apache.linkis.engineconn.common.hook.EngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:212:abstract class UDFLoadEngineConnHook extends UDFLoad with EngineConnHook with Logging {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:214:  override def afterExecutionExecute(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:246:class JarUdfEngineHook extends UDFLoadEngineConnHook {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:264:class PyUdfEngineHook extends UDFLoadEngineConnHook {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\UDFLoadEngineConnHook.scala:280:class ScalaUdfEngineHook extends UDFLoadEngineConnHook {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\InitSQLHook.scala:24:import org.apache.linkis.engineconn.common.hook.EngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\InitSQLHook.scala:38:abstract class InitSQLHook extends EngineConnHook with Logging {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\InitSQLHook.scala:54:  override def afterExecutionExecute(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:25:import org.apache.linkis.engineconn.common.hook.EngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:176:abstract class PythonModuleLoadEngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:178:    with EngineConnHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:181:  override def afterExecutionExecute(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:216:class PythonSparkEngineHook extends PythonModuleLoadEngineConnHook {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHook.scala:237:class PythonEngineHook extends PythonModuleLoadEngineConnHook {'}]

**AI**：
[思考] 现在我已经理解了完整的流程。让我总结一下分析结果。...


## 交互记录 (2026-04-21T09:20:02.949Z)

**用户**：
[{'tool_use_id': 'call_e054bd4a1b344d9681d03289', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\launch\\EngineConnServer.scala:104:      logger.info("Finished to execute hook of afterExecutionExecute")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\test\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\hook\\PythonModuleLoadEngineConnHookTest.scala:45://    hook.afterExecutionExecute(mockEngineCreationContext, mockEngineConn)'}]

**AI**：
## 分析结果

**是的，Spark 引擎启动时会执行 init_sql**，具体分析如下：

### 1. 执行时机

init_sql 的执行在 **EngineConn 启动流程** 中，具体调用链为：

```
EngineConnServer.start() 
  → initExecutors() 初始化 Executor
  → EngineConnObject.setReady() 设置就绪状态
  → afterExecutionExecute() ← 这里执行 init_sql
  → afterEngineServerStartSuccess()
```

参考代码位置：[EngineConnServer.scala:103](linkis-computation-governance/linkis-engineconn/linkis-engineconn-core/src/main/scala/org/apache/linkis/engineconn/launch/EngineConnServer.scala#L103)

### 2. 核心代码逻辑

参考 [InitSQLHook.scala:54-88](linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/hook/InitSQLHook.scala#L54-L88)：

```scala
override def afterExecutionExecute(...): Unit = {
  // 1. 检查是否启用 init_sql
  if (!INIT_SQL_ENABLE.getValue(engineCreationContext.getOptions)) {
    logger.info(s"$user engineConn skip execute init_sql")
    return
  }
  
  // 2. 读取用户对应的 init_sql 文件
  val initSql = readFile(INIT_SQL_DIR + user + "_hive.sql")
  
  // 3. 创建内部任务并执行
  val engineConnTask = new CommonEngineConnTask("init_sql")
  executor.toExecuteTask(engineConnTask, internalExecute = true)
}
```

### 3. 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `wds.linkis.bdp.hive.init.sql.enable` | `false` | 是否启用 init_sql 执行 |
| `wds.linkis.bdp.hive.init.sql.dir` | `/appcom/config/hive-config/init_sql/` | init_sql 文件目录 |

### 4. Spark 引擎的 Hook 注册

在 [linkis-engineconn.properties:27](linkis-engineconn-plugins/spark/src/main/resources/linkis-engineconn.properties#L27) 中注册了：

```properties
wds.linkis.engine.connector.hooks=...,org.apache.linkis.engineconn.computation.executor.hook.SparkInitSQLHook,...
```

### 5. 执行特点

1. **文件命名规则**：`{用户名}_hive.sql`，例如用户 `hadoop` 对应的文件是 `hadoop_hive.sql`
2. **执行方式**：作为内部任务（`internalExecute = true`）执行
3. **执行顺序**：在 Executor 初始化完成后、引擎完全就绪前执行
4. **RunType**：SparkInitSQLHook 使用 `sql` 类型执行

### 6. 注意事项

- 默认情况下 **init_sql 功能是关闭的**，需要显式配置 `wds.linkis.bdp.hive.init.sql.enable=true`
- 如果文件...


## 交互记录 (2026-04-21T09:20:22.054Z)

**用户**：
[{'tool_use_id': 'call_8807a6075490478f8d729067', 'type': 'tool_result', 'content': '95\t        throw new EngineConnExecutorErrorException(\n96\t          EngineConnExecutorErrorCode.INIT_EXECUTOR_FAILED,\n97\t          "Init executors failed. ",\n98\t          t\n99\t        )\n100\t      }\n101\t      EngineConnObject.setReady()\n102\t      logger.info("Finished to execute executions.")\n103\t      ecHooks.foreach(_.afterExecutionExecute(getEngineCreationContext, engineConn))\n104\t      logger.info("Finished to execute hook of afterExecutionExecute")\n105\t      ecHooks.foreach(_.afterEngineServerStartSuccess(getEngineCreationContext, engineConn))\n106\t    } catch {\n107\t      case t: Throwable =>\n108\t        logger.error("EngineConnServer Start Failed.", t)\n109\t        val ecHooks = EngineConnHook.getEngineConnHooks(onceMode)\n110\t        ecHooks.foreach(_.afterEngineServerStartFailed(getEngineCreationContext, t))\n111\t        System.exit(1)\n112\t    }\n113\t\n114\t    // 4. wait Executions execute\n115\t    ShutdownHook.getShutdownHook.waitForStopOrError()\n116\t    logger.info("<<---------------------EngineConnServer Exit --------------------->>")\n117\t    System.exit(ShutdownHook.getShutdownHook.getExitCode())\n118\t  }\n119\t\n120\t  private def init(args: Array[String]): Unit = {\n121\t    val arguments = EngineConnArgumentsParser.getEngineConnArgumentsParser.parseToObj(args)\n122\t    val engineConf = arguments.getEngineConnConfMap\n123\t    this.engineCreationContext.setUser(engineConf.getOrElse("user", Utils.getJvmUser))\n124\t    this.engineCreationContext.setTicketId(engineConf.getOrElse(ECConstants.EC_TICKET_ID_KEY, ""))'}]

**AI**：
[思考] 用户说在他们的环境中 init_sql 开关是打开的，但启动 Spark 时会报空指针。我需要分析可能的原因。

让我看看 InitSQLHook 的代码，找出可能导致空指针的地方：

1. `engineCreationContext.getUser()` - 可能为空
2. `executor` 可能为空
3. `labels` 或 `codeLanguageLabel` 可能有问题

让我搜索一下相关的错误日志或者空指针可能发生的地方。...


## 交互记录 (2026-04-21T09:29:38.857Z)

**用户**：
[{'type': 'text', 'text': '在我们的特殊环境里面开关是打开的，然后启动spark的时候会报空指针'}]

**AI**：
让我帮你分析空指针的原因。先看看可能出问题的地方：...


## 交互记录 (2026-04-21T09:29:45.290Z)

**用户**：
[{'tool_use_id': 'call_1bc437d470264c2c85ca54c0', 'type': 'tool_result', 'content': 'File unchanged since last read. The content from the earlier Read tool_result in this conversation is still current — refer to that instead of re-reading.'}]

**AI**：
[思考] 让我搜索 ExecutorManager 的 getExecutorByLabels 方法，看看它可能返回什么。...


## 交互记录 (2026-04-21T09:29:45.583Z)

**用户**：
[{'tool_use_id': 'call_f411d1dd8a2a4f549c9adaa6', 'type': 'tool_result', 'content': 'No matches found'}]

**AI**：
[思考] 让我继续查看 getExecutorByLabels 的完整实现，看看它可能返回 null 的情况。...


## 交互记录 (2026-04-21T09:29:51.140Z)

**用户**：
[{'tool_use_id': 'call_3461f85f050a453b88bb123f', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-54-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-55-}\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-56-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-57-trait LabelExecutorManager extends ExecutorManager {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-58-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala:59:  def getExecutorByLabels(labels: Array[Label[_]]): LabelExecutor\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-60-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-61-  def removeExecutor(labels: Array[Label[_]]): LabelExecutor\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-62-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-63-  def containExecutor(labels: Array[Label[_]]): Boolean\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-64-\n--\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-184-    executors.get(labelKey)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-185-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-186-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-187-  override def generateExecutorId(): Int = idCreator.getAndIncrement()\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-188-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala:189:  override def getExecutorByLabels(labels: Array[Label[_]]): LabelExecutor = {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-190-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-191-    if (!executors.isEmpty && factories.size <= 1) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-192-      logger.info("For a single Executor EC, if an Executor exists, it will be returned directly")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-193-      return getReportExecutor.asInstanceOf[LabelExecutor]\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-194-    }'}]

**AI**：
[思考] 现在我看到了 `getExecutorByLabels` 方法的实现。在第 197 行：

```scala
val labelKey = getLabelKey(labels)
if (null == labelKey) return null
```

如果 `labelKey` 为 null，会返回 null。然后回到 InitSQLHook：

```scala
ExecutorManager.getInstance.getExecutorByLabels(labels) match {
  case executor: ComputationExecutor =>
    executor.toExecuteTask(engineConnTask, internalExecute = true)
  case _ =>
}
```

这里使用模式匹配，如果返回 null，就会匹配到 `case _ =>`，不会执行任务。但这不会导致空指针。

让我再看看其他可能的问题点：

1. `engineCreationContext.getUser()` 为 null - 但代码中有处理，会使用 `Utils.getJvmUser`
2. `labels` 数组或 `codeLanguageLabel` 为 null
3. `engineConnTask` 或其属性

让我看看 getLabelKey 方法和 toExecuteTask 方法。...


## 交互记录 (2026-04-21T09:29:52.625Z)

**用户**：
[{'tool_use_id': 'call_c29b01fec5e248b29804a3d4', 'type': 'tool_result', 'content': '185\t  }\n186\t\n187\t  override def generateExecutorId(): Int = idCreator.getAndIncrement()\n188\t\n189\t  override def getExecutorByLabels(labels: Array[Label[_]]): LabelExecutor = {\n190\t\n191\t    if (!executors.isEmpty && factories.size <= 1) {\n192\t      logger.info("For a single Executor EC, if an Executor exists, it will be returned directly")\n193\t      return getReportExecutor.asInstanceOf[LabelExecutor]\n194\t    }\n195\t\n196\t    val labelKey = getLabelKey(labels)\n197\t    if (null == labelKey) return null\n198\t    if (!executors.containsKey(labelKey)) executors synchronized {\n199\t      if (!executors.containsKey(labelKey)) {\n200\t        createExecutor(engineConn.getEngineCreationContext, labels)\n201\t      }\n202\t    }\n203\t    executors.get(labelKey)\n204\t  }\n205\t\n206\t  override def containExecutor(labels: Array[Label[_]]): Boolean = {\n207\t    val labelKey = getLabelKey(labels)\n208\t    null != labelKey && executors.containsKey(labelKey)\n209\t  }\n210\t\n211\t  override def getExecutor(id: String): Executor =\n212\t    executors.values().asScala.find(_.getId == id).orNull\n213\t\n214\t  override def getExecutors: Array[Executor] = executors.values().asScala.toArray\n215\t\n216\t  override def removeExecutor(labels: Array[Label[_]]): LabelExecutor = executors.synchronized {\n217\t    val labelKey = getLabelKey(labels)\n218\t    if (labelKey != null && executors.containsKey(labelKey)) executors.remove(labelKey)\n219\t    else null\n220\t  }\n221\t\n222\t  override def removeExecutor(id: String): Executor = executors.synchronized {\n223\t    executors.asScala\n224\t      .find(_._2.getId == id)\n225\t      .map { case (k, _) =>\n226\t        executors.remove(k)\n227\t      }\n228\t      .orNull\n229\t  }\n230\t\n231\t  override def getReportExecutor: Executor = if (getExecutors.isEmpty) {\n232\t    val labels = defaultFactory match {\n233\t      case labelExecutorFactory: CodeLanguageLabelExecutorFactory =>\n234\t        Array[Label[_]](labelExecutorFactory.getDefaultCodeLanguageLabel)\n235\t      case _ =>\n236\t        if (null == engineConn.getEngineCreationContext.getLabels()) {\n237\t          Array.empty[Label[_]]\n238\t        } else {\n239\t          engineConn.getEngineCreationContext.getLabels().toArray[Label[_]](Array.empty[Label[_]])\n240\t        }\n241\t    }\n242\t    createExecutor(engineConn.getEngineCreationContext, labels)\n243\t  } else {\n244\t    getExecutors.maxBy {'}]

**AI**：
[思考] 我看到 `getLabelKey` 方法的实现：

```scala
protected def getLabelKey(labels: Array[Label[_]]): String =
  labels.filter(null != _).map(_.getStringValue).mkString("&")
```

如果 labels 数组为空，这会返回空字符串 ""，不是 null。

让我看看 InitSQLHook 中的代码，可能的问题点：

1. `codeLanguageLabel.setCodeType(getRunType())` - RunType.SQL.toString
2. `labels = Array[Label[_]](codeLanguageLabel)` 
3. `engineConnTask.setLabels(labels)`

问题可能出在：
1. `CodeLanguageLabel` 的 `getStringValue()` 返回 null
2. `toExecuteTask` 方法内部有问题

让我检查一下 CodeLanguageLabel 的实现。...


## 交互记录 (2026-04-21T09:29:56.855Z)

**用户**：
[{'tool_use_id': 'call_bc4acfe71a804317ad2b56f6', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-129-    ExecutorHook\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-130-      .getAllExecutorHooks()\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-131-      .filter(_.isAccepted(codeType))\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-132-      .foreach(_.afterExecutorInit(labelExecutor))\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-133-    logger.info(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-134-      s"Finished to init ${labelExecutor.getClass.getSimpleName}(${labelExecutor.getId})."\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-135-    )\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-136-    labelExecutor\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-137-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-138-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala:139:  protected def getLabelKey(labels: Array[Label[_]]): String =\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-140-    labels.filter(null != _).map(_.getStringValue).mkString("&")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-141-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-142-  protected def createExecutor(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-143-      engineCreationContext: EngineCreationContext,\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-144-      labels: Array[Label[_]] = null\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-145-  ): LabelExecutor = {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-146-    if (null == labels || labels.isEmpty) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-147-      defaultFactory match {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-148-        case labelExecutorFactory: CodeLanguageLabelExecutorFactory =>\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-core\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\core\\executor\\ExecutorManager.scala-149-          val createExecutorLabels ='}]

**AI**：
[思考] 现在我找到了关键信息。让我看看 CodeLanguageLabel 的完整实现，特别是 getStringValue 方法。...


## 交互记录 (2026-04-21T09:30:00.574Z)

**用户**：
[{'tool_use_id': 'call_f8a23b23c2224060b06d68a9', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.engineconn.computation.executor.entity\n19\t\n20\timport org.apache.linkis.common.ServiceInstance\n21\timport org.apache.linkis.governance.common.entity.ExecutionNodeStatus\n22\timport org.apache.linkis.manager.label.entity.Label\n23\t\n24\timport java.util\n25\t\n26\tclass CommonEngineConnTask(val taskId: String, var taskSupportRetry: Boolean = false)\n27\t    extends EngineConnTask {\n28\t\n29\t  private var code: String = _\n30\t  private var prop: util.Map[String, Object] = _\n31\t  private var status: ExecutionNodeStatus = ExecutionNodeStatus.Inited\n32\t  private var labels: Array[Label[_]] = new Array[Label[_]](0)\n33\t  private var serviceInstance: ServiceInstance = _\n34\t\n35\t  override def getTaskId: String = taskId\n36\t\n37\t  override def isTaskSupportRetry: Boolean = taskSupportRetry\n38\t\n39\t  override def getCode: String = code\n40\t\n41\t  override def setCode(code: String): Unit = this.code = code\n42\t\n43\t  override def getProperties: util.Map[String, Object] = prop\n44\t\n45\t  override def setProperties(properties: util.Map[String, Object]): Unit = this.prop = properties\n46\t\n47\t  override def data(key: String, value: Object): Unit = this.prop.put(key, value)\n48\t\n49\t  override def getStatus: ExecutionNodeStatus = status\n50\t\n51\t  override def setStatus(status: ExecutionNodeStatus): Unit = this.status = status\n52\t\n53\t  override def getLables: Array[Label[_]] = labels\n54\t\n55\t  override def setLabels(labels: Array[Label[_]]): Unit = this.labels = labels\n56\t\n57\t  override def getCallbackServiceInstance(): ServiceInstance = serviceInstance\n58\t\n59\t  override def setCallbackServiceInstance(serviceInstance: ServiceInstance): Unit =\n60\t    this.serviceInstance = serviceInstance\n61\t\n62\t}\n63\t'}]

**AI**：
[思考] 现在让我看看 GenericLabel 的 getStringValue 方法。...


## 交互记录 (2026-04-21T09:30:03.308Z)

**用户**：
[{'tool_use_id': 'call_887ea75f3a024e31b5cdda98', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-16- */\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-17-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-18-package org.apache.linkis.manager.label.entity.engine;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-19-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-20-import org.apache.linkis.manager.label.constant.LabelKeyConstant;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-21-import org.apache.linkis.manager.label.entity.GenericLabel;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-22-import org.apache.linkis.manager.label.entity.annon.ValueSerialNum;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-23-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-24-import java.util.HashMap;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-25-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java:26:public class CodeLanguageLabel extends GenericLabel {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-27-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-28-  public CodeLanguageLabel() {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-29-    setLabelKey(LabelKeyConstant.CODE_TYPE_KEY);\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-30-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-31-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-32-  public String getCodeType() {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-33-    if (null == getValue()) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-34-      return null;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-35-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\CodeLanguageLabel.java-36-    return getValue().get(LabelKeyConstant.CODE_TYPE_KEY);'}]

**AI**：
[思考] 让我直接读取 GenericLabel 类的文件来查看其实现。...


## 交互记录 (2026-04-21T09:30:05.125Z)

**用户**：
[{'tool_use_id': 'call_ad0029c7738e4b1a95856b65', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.entity.engine;\n19\t\n20\timport org.apache.linkis.manager.label.constant.LabelKeyConstant;\n21\timport org.apache.linkis.manager.label.entity.GenericLabel;\n22\timport org.apache.linkis.manager.label.entity.annon.ValueSerialNum;\n23\t\n24\timport java.util.HashMap;\n25\t\n26\tpublic class CodeLanguageLabel extends GenericLabel {\n27\t\n28\t  public CodeLanguageLabel() {\n29\t    setLabelKey(LabelKeyConstant.CODE_TYPE_KEY);\n30\t  }\n31\t\n32\t  public String getCodeType() {\n33\t    if (null == getValue()) {\n34\t      return null;\n35\t    }\n36\t    return getValue().get(LabelKeyConstant.CODE_TYPE_KEY);\n37\t  }\n38\t\n39\t  @ValueSerialNum(0)\n40\t  public void setCodeType(String runType) {\n41\t    if (null == getValue()) {\n42\t      setValue(new HashMap<>());\n43\t    }\n44\t    getValue().put(LabelKeyConstant.CODE_TYPE_KEY, runType);\n45\t  }\n46\t}\n47\t'}]

**AI**：
[思考] 好的，让我读取这个文件。...


## 交互记录 (2026-04-21T09:30:07.402Z)

**用户**：
[{'tool_use_id': 'call_ea0b52fbb2584d099ab045ce', 'type': 'tool_result', 'content': '<persisted-output>\nOutput too large (45.3KB). Full output saved to: C:\\Users\\v_kkhuang\\.claude\\projects\\g--kkhuang-work-linkis\\aa4f4edc-bf9c-498e-bcec-e2f79df69b93\\tool-results\\call_ea0b52fbb2584d099ab045ce.txt\n\nPreview (first 2KB):\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-47-    if (isEmpty()) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-48-      return LabelKeyConstant.COMBINED_LABEL_KEY_PREFIX;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-49-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-50-    List<String> keyList = getValue().stream().map(Label::getLabelKey).collect(Collectors.toList());\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-51-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-52-    String labelKey = LabelKeyConstant.COMBINED_LABEL_KEY_PREFIX + StringUtils.join(keyList, "_");\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-53-    return labelKey;\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-54-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-55-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\CombinedLabelImpl.java-56-  @Override\n...\n</persisted-output>'}]

**AI**：
[思考] 让我继续查看父类 InheritableLabel，特别是 getStringValue 方法。...


## 交互记录 (2026-04-21T09:30:12.983Z)

**用户**：
[{'tool_use_id': 'call_1e95344cc70c4df4a9cd0847', 'type': 'tool_result', 'content': 'linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\GenericLabel.java'}]

**AI**：
[思考] 继续查看 SerializableLabel 类。...


## 交互记录 (2026-04-21T09:30:14.821Z)

**用户**：
[{'tool_use_id': 'call_0652e646a69540ceba5bd116', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.entity;\n19\t\n20\timport java.util.HashMap;\n21\timport java.util.Map;\n22\t\n23\tpublic class GenericLabel extends InheritableLabel<Map<String, String>> {\n24\t\n25\t  public GenericLabel() {\n26\t    super.value = new HashMap<>();\n27\t  }\n28\t\n29\t  @Override\n30\t  public Feature getFeature() {\n31\t    Feature feature = super.getFeature();\n32\t    if (null == feature) {\n33\t      // Get feature from label value;\n34\t      try {\n35\t        return Feature.valueOf(\n36\t            super.getValue().getOrDefault(getFeatureKey(), Feature.OPTIONAL.name()));\n37\t      } catch (IllegalArgumentException e) {\n38\t        // Ignore\n39\t      }\n40\t    }\n41\t    return feature;\n42\t  }\n43\t\n44\t  @Override\n45\t  public void setValue(Map<String, String> value) {\n46\t    super.setValue(value);\n47\t  }\n48\t}\n49\t'}]

**AI**：
[思考] 现在我找到了 `getStringValue` 方法的实现。让我分析一下可能的问题：

在 InitSQLHook 中：
```scala
val codeLanguageLabel = new CodeLanguageLabel
codeLanguageLabel.setCodeType(getRunType())  // 这里设置的是 "sql"
```

然后 `getLabelKey(labels)` 会调用：
```scala
labels.filter(null != _).map(_.getStringValue).mkString("&")
```

对于 `CodeLanguageLabel`，`getStringValue()` 的逻辑是：1. 如果 `value` 是 null，返回 null
2. 如果 `value` 不是 null，则尝试序列化

问题可能出在：
1. `value` 虽然不是 null，但是空的 Map（因为 GenericLabel 初始化了 `new HashMap<>()`）
2. 在序列化时，如果 value 是空的 Map，可能会导致问题

让我仔细看看 InitSQLHook 中可能空指针的地方。回到代码：

```scala
ExecutorManager.getInstance.getExecutorByLabels(labels) match {
  case executor: ComputationExecutor =>
    executor.toExecuteTask(engineConnTask, internalExecute = true)
  case _ =>
}
```

如果 `getExecutorByLabels` 返回 null，会匹配 `case _ =>`，不会报空指针。

让我再看看 toExecuteTask 方法，看看内部是否可能抛出空指针。...


## 交互记录 (2026-04-21T09:30:16.419Z)

**用户**：
[{'tool_use_id': 'call_2782379ac5ff445c95020cc3', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.entity;\n19\t\n20\t/** Mutable label */\n21\tpublic class InheritableLabel<T> extends SerializableLabel<T> {\n22\t\n23\t  private static final String FEATURE_KEY = "feature";\n24\t\n25\t  private String labelKey;\n26\t\n27\t  private Feature feature;\n28\t\n29\t  public void setLabelKey(String labelKey) {\n30\t    this.labelKey = labelKey;\n31\t  }\n32\t\n33\t  @Deprecated\n34\t  public void setFeature(Feature feature) {\n35\t    this.feature = feature;\n36\t  }\n37\t\n38\t  @Override\n39\t  public final String getLabelKey() {\n40\t    return labelKey;\n41\t  }\n42\t\n43\t  @Override\n44\t  public final T getValue() {\n45\t    return value;\n46\t  }\n47\t\n48\t  @Override\n49\t  public Feature getFeature() {\n50\t    return feature;\n51\t  }\n52\t\n53\t  public void setValue(T value) {\n54\t    this.value = value;\n55\t  }\n56\t\n57\t  @Override\n58\t  public String getFeatureKey() {\n59\t    return FEATURE_KEY;\n60\t  }\n61\t}\n62\t'}]

**AI**：
让我查看 `toExecuteTask` 方法，看看里面是否可能抛出空指针：...


## 交互记录 (2026-04-21T09:30:17.868Z)

**用户**：
[{'tool_use_id': 'call_09f11fc9b4f14db68620b51c', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.entity;\n19\t\n20\timport org.apache.linkis.manager.label.utils.LabelUtils;\n21\t\n22\timport org.apache.commons.lang3.StringUtils;\n23\t\n24\timport java.lang.reflect.Method;\n25\timport java.util.ArrayList;\n26\timport java.util.Collections;\n27\timport java.util.List;\n28\timport java.util.Map;\n29\timport java.util.Objects;\n30\timport java.util.WeakHashMap;\n31\timport java.util.stream.Collectors;\n32\t\n33\timport com.fasterxml.jackson.annotation.JsonIgnore;\n34\t\n35\t/** Serialize label value to string value and override the equals method */\n36\tpublic abstract class SerializableLabel<T> implements Label<T> {\n37\t\n38\t  /** Cache the value names, weak hashMap */\n39\t  private static final Map<Class<?>, List<String>> CACHE =\n40\t      Collections.synchronizedMap(new WeakHashMap<>());\n41\t\n42\t  protected static final String VALUE_SEPARATOR = "-";\n43\t\n44\t  protected static final String[] VALUE_METHOD_PREFIX = new String[] {"is", "get", "set"};\n45\t\n46\t  protected static final String SET_VALUE_METHOD_PREFIX = "set";\n47\t\n48\t  @JsonIgnore protected T value;\n49\t\n50\t  private String stringValue;\n51\t\n52\t  public abstract String getFeatureKey();\n53\t\n54\t  /**\n55\t   * Serialize method\n56\t   *\n57\t   * @return serialized string\n58\t   */\n59\t  @Override\n60\t  public String getStringValue() {\n61\t    if (StringUtils.isBlank(stringValue) && null != value) {\n62\t      if (LabelUtils.isBasicType(value.getClass())) {\n63\t        this.stringValue = String.valueOf(value);\n64\t      } else if (value instanceof List) {\n65\t        this.stringValue = StringUtils.join((List<?>) value, VALUE_SEPARATOR);\n66\t      } else {\n67\t        // Use jackson default\n68\t        Map<String, String> valueMap =\n69\t            LabelUtils.Jackson.convert(value, Map.class, String.class, String.class);\n70\t        // Deprecated?\n71\t        if (StringUtils.isNotBlank(getFeatureKey())) {\n72\t          valueMap.remove(getFeatureKey());\n73\t        }\n74\t        List<String> orderedValueNames = CACHE.get(this.getClass());\n75\t        if (null == orderedValueNames) {\n76\t          orderedValueNames =\n77\t              LabelUtils.getOrderedValueNameInLabelClass(this.getClass(), VALUE_METHOD_PREFIX);\n78\t          CACHE.put(this.getClass(), orderedValueNames);\n79\t        }\n80\t        List<String> valueSerial =\n81\t            new ArrayList<>(orderedValueNames)\n82\t                .stream().map(valueMap::get).filter(Objects::nonNull).collect(Collectors.toList());\n83\t        // Match the number\n84\t        if (valueSerial.size() == orderedValueNames.size()) {\n85\t          this.stringValue = StringUtils.join(valueSerial, VALUE_SEPARATOR);\n86\t        }\n87\t      }\n88\t    }\n89\t    return this.stringValue;\n90\t  }\n91\t\n92\t  /**\n93\t   * Deserialize method\n94\t   *\n95\t   * @param stringValue\n96\t   */\n97\t  protected void setStringValue(String stringValue) {\n98\t    if (StringUtils.isNotBlank(stringValue)) {\n99\t      String[] stringValueArray = stringValue.split(VALUE_SEPARATOR);\n100\t      List<String> orderedValueNames = CACHE.get(this.getClass());\n101\t      if (null == orderedValueNames) {\n102\t        orderedValueNames =\n103\t            LabelUtils.getOrderedValueNameInLabelClass(this.getClass(), VALUE_METHOD_PREFIX);\n104\t        CACHE.put(this.getClass(), orderedValueNames);\n105\t      }\n106\t      if (stringValueArray.length != orderedValueNames.size()) {\n107\t        return;\n108\t      }\n109\t      for (int i = 0; i < orderedValueNames.size(); i++) {\n110\t        String valueName = orderedValueNames.get(i);\n111\t        try {\n112\t          Method method =\n113\t              this.getClass()\n114\t                  .getMethod(\n115\t                      SET_VALUE_METHOD_PREFIX\n116\t                          + valueName.substring(0, 1).toUpperCase()\n117\t                          + valueName.substring(1),\n118\t                      String.class);\n119\t          method.invoke(this, stringValueArray[i]);\n120\t        } catch (Exception e) {\n121\t          // Ignore\n122\t        }\n123\t      }\n124\t    }\n125\t  }\n126\t\n127\t  @Override\n128\t  public final String toString() {\n129\t    return "[key: "\n130\t        + getLabelKey()\n131\t        + ", value: "\n132\t        + LabelUtils.Jackson.toJson(value, null)\n133\t        + ", str: "\n134\t        + getStringValue()\n135\t        + "]";\n136\t  }\n137\t\n138\t  @Override\n139\t  public boolean equals(Object other) {\n140\t    if (other instanceof SerializableLabel) {\n141\t      String otherSerialStr = other.toString();\n142\t      String serialStr = toString();\n143\t      return StringUtils.isNotBlank(serialStr) && this.toString().equals(otherSerialStr);\n144\t    }\n145\t    return super.equals(other);\n146\t  }\n147\t\n148\t  @Override\n149\t  public int hashCode() {\n150\t    return Objects.hash(value, stringValue);\n151\t  }\n152\t\n153\t  @Override\n154\t  public Boolean isEmpty() {\n155\t    return null == getValue();\n156\t  }\n157\t}\n158\t'}]

**AI**：
[思考] 好的，让我继续查看 toExecuteTask 方法的完整实现，找到可能空指针的地方。...


## 交互记录 (2026-04-21T09:30:24.202Z)

**用户**：
[{'tool_use_id': 'call_47c7f7b61a8b4787bd8260e7', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-54-import java.util\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-55-import java.util.concurrent.ConcurrentHashMap\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-56-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-57-import DataWorkCloudApplication.getApplicationContext\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-58-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-59-abstract class AsyncConcurrentComputationExecutor(override val outputPrintLimit: Int = 1000)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-60-    extends ComputationExecutor(outputPrintLimit)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-61-    with ConcurrentExecutor\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-62-    with JobListener {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-63-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-64-  private lazy val scheduler =\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-65-    AsyncExecuteContext.getAsyncExecuteContext.getOrCreateScheduler(this)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-66-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-67-  private val taskIdToJobMap: util.Map[String, Job] = new ConcurrentHashMap[String, Job](8)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-68-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala:69:  override def toExecuteTask(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-70-      engineConnTask: EngineConnTask,\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-71-      internalExecute: Boolean = false\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-72-  ): ExecuteResponse = {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-73-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-74-    transformTaskStatus(engineConnTask, ExecutionNodeStatus.Running)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-75-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-76-    val job = parserTaskToJob(engineConnTask)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-77-    job.setJobListener(this)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-78-    job.setId(engineConnTask.getTaskId)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-79-    taskIdToJobMap.put(engineConnTask.getTaskId, job)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-80-    Utils.tryCatch(scheduler.submit(job)) { t =>\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-81-      val msg = s" submit task(${engineConnTask.getTaskId}) failed"\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-82-      getEngineSyncListenerBus.postToAll(TaskResponseErrorEvent(engineConnTask.getTaskId, msg))\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-83-      logger.error(msg, t)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\async\\AsyncConcurrentComputationExecutor.scala-84-      onJobCompleted(job)\n--\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-192-        MAX_TASK_EXECUTE_NUM > 0 && runningTasks\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-193-          .getCount() == 0 && executorNumber > MAX_TASK_EXECUTE_NUM\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-194-    ) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-195-      logger.error(s"Task has reached max execute number $MAX_TASK_EXECUTE_NUM, now  tryShutdown. ")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-196-      ExecutorManager.getInstance.getReportExecutor.tryShutdown()\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-197-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-198-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-199-    // unhealthy node should try to shutdown\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-200-    if (runningTasks.getCount() == 0 && currentEngineIsUnHealthy) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-201-      logger.info("no task running and ECNode is unHealthy, now to mark engine to Finished.")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-202-      ExecutorManager.getInstance.getReportExecutor.tryShutdown()\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-203-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-204-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-205-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-206-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala:207:  def toExecuteTask(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-208-      engineConnTask: EngineConnTask,\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-209-      internalExecute: Boolean = false\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-210-  ): ExecuteResponse = {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-211-    runningTasks.increase()\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-212-    this.internalExecute = internalExecute\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-213-    Utils.tryFinally {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-214-      transformTaskStatus(engineConnTask, ExecutionNodeStatus.Running)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-215-      val engineExecutionContext = createEngineExecutionContext(engineConnTask)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-216-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-217-      val engineCreationContext = EngineConnObject.getEngineCreationContext\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-218-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-219-      var hookedCode = engineConnTask.getCode\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-220-      Utils.tryCatch {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-221-        ComputationExecutorHook.getComputationExecutorHooks.foreach(hook => {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-222-          hookedCode ='}]

**AI**：
[思考] 我需要看一下 `createEngineExecutionContext` 方法，这可能是空指针发生的地方。...


## 交互记录 (2026-04-21T09:30:26.364Z)

**用户**：
[{'tool_use_id': 'call_44ed632c3c96467985fbebc2', 'type': 'tool_result', 'content': '205\t  }\n206\t\n207\t  def toExecuteTask(\n208\t      engineConnTask: EngineConnTask,\n209\t      internalExecute: Boolean = false\n210\t  ): ExecuteResponse = {\n211\t    runningTasks.increase()\n212\t    this.internalExecute = internalExecute\n213\t    Utils.tryFinally {\n214\t      transformTaskStatus(engineConnTask, ExecutionNodeStatus.Running)\n215\t      val engineExecutionContext = createEngineExecutionContext(engineConnTask)\n216\t\n217\t      val engineCreationContext = EngineConnObject.getEngineCreationContext\n218\t\n219\t      var hookedCode = engineConnTask.getCode\n220\t      Utils.tryCatch {\n221\t        ComputationExecutorHook.getComputationExecutorHooks.foreach(hook => {\n222\t          hookedCode =\n223\t            hook.beforeExecutorExecute(engineExecutionContext, engineCreationContext, hookedCode)\n224\t        })\n225\t      } { e =>\n226\t        e match {\n227\t          case hookExecuteException: HookExecuteException =>\n228\t            failedTasks.increase()\n229\t            logger.error("failed to do with hook", e)\n230\t            return ErrorExecuteResponse("hook execute failed task will be failed", e)\n231\t          case _ => logger.info("failed to do with hook", e)\n232\t        }\n233\t      }\n234\t      logger.info(s"ComputationExecutor code hook completed.")\n235\t\n236\t      // task params log\n237\t      // spark engine: at org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor.executeLine log special conf\n238\t      Utils.tryAndWarn {\n239\t        val engineType = LabelUtil.getEngineType(engineCreationContext.getLabels())\n240\t        EngineType.mapStringToEngineType(engineType) match {\n241\t          case EngineType.HIVE | EngineType.TRINO => printTaskParamsLog(engineExecutionContext)\n242\t          case _ =>\n243\t        }\n244\t      }\n245\t\n246\t      val localPath = EngineConnConf.getLogDir\n247\t      engineExecutionContext.appendStdout(\n248\t        LogUtils.generateInfo(\n249\t          s"EngineConn local log path: ${DataWorkCloudApplication.getServiceInstance.toString} $localPath"\n250\t        )\n251\t      )\n252\t\n253\t      var response: ExecuteResponse = null\n254\t      val incomplete = new StringBuilder\n255\t      val codes =\n256\t        Utils.tryCatch(getCodeParser.map(_.parse(hookedCode)).getOrElse(Array(hookedCode))) { e =>\n257\t          logger.info("Your code will be submitted in overall mode.", e)\n258\t          Array(hookedCode)\n259\t        }\n260\t      engineExecutionContext.setTotalParagraph(codes.length)\n261\t\n262\t      val retryEnable: Boolean = SUPPORT_PARTIAL_RETRY_FOR_FAILED_TASKS_ENABLED\n263\t\n264\t      codes.indices.foreach({ index =>\n265\t        if (ExecutionNodeStatus.Cancelled == engineConnTask.getStatus) {\n266\t          return ErrorExecuteResponse("Job is killed by user!", null)\n267\t        }\n268\t        var executeFlag = true\n269\t        val errorIndex: Int = Integer.valueOf(\n270\t          engineConnTask.getProperties\n271\t            .getOrDefault(Configuration.EXECUTE_ERROR_CODE_INDEX.key, "-1")\n272\t            .toString\n273\t        )\n274\t        engineExecutionContext.getProperties\n275\t          .put(Configuration.EXECUTE_ERROR_CODE_INDEX.key, errorIndex.toString)\n276\t        val props: util.Map[String, String] = engineCreationContext.getOptions\n277\t        val taskRetry: String =\n278\t          props.getOrDefault("linkis.task.retry.switch", "false").toString\n279\t        if (java.lang.Boolean.parseBoolean(taskRetry)) {\n280\t          // jdbc执行任务重试，如果sql有被set进sql，会导致sql的index错位，这里会将日志打印的index进行减一，保证用户看的index是正常的，然后重试的errorIndex需要加一，保证重试的位置是正确的\n281\t          var newIndex = index\n282\t          var newErrorIndex = errorIndex\n283\t          if (adjustErrorIndexForSetScenarios(engineConnTask)) {\n284\t            newIndex = index - 1'}]

**AI**：
[思考] 我找到了！问题很可能在这里：

```scala
val userCreator = engineConnTask.getLables
  .find(_.isInstanceOf[UserCreatorLabel])
  .map { case label: UserCreatorLabel => label }
  .orNull

val engineExecutionContext =
  if (null != userCreator && StringUtils.isNotBlank(userCreator.getUser)) {
    new EngineExecutionContext(this, userCreator.getUser)
  ...
```

在 InitSQLHook 中创建的 `CommonEngineConnTask`，它的 labels 只有 `CodeLanguageLabel`，没有 `UserCreatorLabel`。

所以 `userCreator` 会是 null。让我继续看看当 userCreator 为 null 时会发生什么。...


## 交互记录 (2026-04-21T09:30:28.547Z)

**用户**：
[{'tool_use_id': 'call_9c550ed7091b4916953b4867', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-501-            ComputationExecutorConf.JDBC_SET_STATEMENT_PREFIXES.getValue.split(",")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-502-          jdbcSetPrefixes.exists(upperCode.startsWith)\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-503-        } else {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-504-          false\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-505-        }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-506-      }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-507-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-508-    result\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-509-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-510-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala:511:  protected def createEngineExecutionContext(\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-512-      engineConnTask: EngineConnTask\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-513-  ): EngineExecutionContext = {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-514-    val userCreator = engineConnTask.getLables\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-515-      .find(_.isInstanceOf[UserCreatorLabel])\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-516-      .map { case label: UserCreatorLabel => label }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-517-      .orNull\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-518-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-519-    val engineExecutionContext =\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-520-      if (null != userCreator && StringUtils.isNotBlank(userCreator.getUser)) {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\ComputationExecutor.scala-521-        new EngineExecutionContext(this, userCreator.getUser)'}]

**AI**：
[思考] 好的，看起来即使 userCreator 为 null，也会创建一个 `EngineExecutionContext(this)`，不会报空指针。让我继续检查后面的代码。

问题可能在 engineExecutionContext 创建之后的某处。让我继续看看完整的 createEngineExecutionContext 方法。...


## 交互记录 (2026-04-21T09:30:32.307Z)

**用户**：
[{'tool_use_id': 'call_83ae50b0497645dd8c01671a', 'type': 'tool_result', 'content': '510\t\n511\t  protected def createEngineExecutionContext(\n512\t      engineConnTask: EngineConnTask\n513\t  ): EngineExecutionContext = {\n514\t    val userCreator = engineConnTask.getLables\n515\t      .find(_.isInstanceOf[UserCreatorLabel])\n516\t      .map { case label: UserCreatorLabel => label }\n517\t      .orNull\n518\t\n519\t    val engineExecutionContext =\n520\t      if (null != userCreator && StringUtils.isNotBlank(userCreator.getUser)) {\n521\t        new EngineExecutionContext(this, userCreator.getUser)\n522\t      } else {\n523\t        new EngineExecutionContext(this)\n524\t      }\n525\t    if (engineConnTask.getProperties.containsKey(RequestTask.RESULT_SET_STORE_PATH)) {\n526\t      engineExecutionContext.setStorePath(\n527\t        engineConnTask.getProperties.get(RequestTask.RESULT_SET_STORE_PATH).toString\n528\t      )\n529\t    }\n530\t    logger.info(s"StorePath : ${engineExecutionContext.getStorePath.orNull}.")\n531\t    engineExecutionContext.setJobId(engineConnTask.getTaskId)\n532\t    engineExecutionContext.getProperties.putAll(engineConnTask.getProperties)\n533\t    engineExecutionContext.setLabels(engineConnTask.getLables)\n534\t\n535\t    val errorIndex: Int = Integer.valueOf(\n536\t      engineConnTask.getProperties\n537\t        .getOrDefault(Configuration.EXECUTE_ERROR_CODE_INDEX.key, "-1")\n538\t        .toString\n539\t    )\n540\t    if (errorIndex > 0) {\n541\t      val savedAliasNum = Integer.valueOf(\n542\t        engineConnTask.getProperties\n543\t          .getOrDefault(Configuration.EXECUTE_RESULTSET_ALIAS_NUM.key, "0")\n544\t          .toString\n545\t      )\n546\t      engineExecutionContext.setResultSetNum(savedAliasNum)\n547\t      logger.info(s"Restore aliasNum to $savedAliasNum for retry task")\n548\t    }\n549\t'}]

**AI**：
[思考] 让我重新看看 InitSQLHook 的代码，特别关注可能导致空指针的地方。

从之前看到的代码，问题可能出在：
1. `engineCreationContext.getLabels()` 可能为 null
2. LabelUtil.getEngineType 可能返回 null

让我检查一下 toExecuteTask 方法中第 239 行的代码：
```scala
val engineType = LabelUtil.getEngineType(engineCreationContext.getLabels())
```

如果 `engineCreationContext.getLabels()` 返回 null，这里可能会有问题。...


## 交互记录 (2026-04-21T09:30:34.814Z)

**用户**：
[{'tool_use_id': 'call_c079efd2cf054f439fde6e8a', 'type': 'tool_result', 'content': '545\t      )\n546\t      engineExecutionContext.setResultSetNum(savedAliasNum)\n547\t      logger.info(s"Restore aliasNum to $savedAliasNum for retry task")\n548\t    }\n549\t\n550\t    engineExecutionContext\n551\t  }\n552\t\n553\t  def killTask(taskId: String): Unit = {\n554\t    Utils.tryAndWarn {\n555\t      val task = getTaskById(taskId)\n556\t      if (null != task) {\n557\t        transformTaskStatus(task, ExecutionNodeStatus.Cancelled)\n558\t      }\n559\t    }\n560\t  }\n561\t\n562\t  /**\n563\t   * 判断是否为上下文语句，重试时需要保留执行\n564\t   *\n565\t   * @param code\n566\t   *   SQL代码（已转换为大写并去除首尾空格）\n567\t   * @return\n568\t   *   true表示是上下文语句，false表示不是\n569\t   */\n570\t  private def isContextStatement(code: String): Boolean = {\n571\t    ComputationExecutorConf.CONTEXT_STATEMENT_PREFIXES.getValue.split(",").exists(code.startsWith)\n572\t  }\n573\t\n574\t  /**\n575\t   * job task log print task params info\n576\t   *\n577\t   * @param engineExecutorContext\n578\t   * @return\n579\t   *   Unit\n580\t   */\n581\t\n582\t  def printTaskParamsLog(engineExecutorContext: EngineExecutionContext): Unit = {\n583\t    val sb = new StringBuilder\n584\t    EngineConnObject.getEngineCreationContext.getOptions.asScala.foreach({ case (key, value) =>'}]

**AI**：
[思考] 让我看看 LabelUtil.getEngineType 的实现。...


## 交互记录 (2026-04-21T09:30:37.897Z)

**用户**：
[{'tool_use_id': 'call_41e8110190114fb595718d06', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-11- * Unless required by applicable law or agreed to in writing, software\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-12- * distributed under the License is distributed on an "AS IS" BASIS,\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-13- * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-14- * See the License for the specific language governing permissions and\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-15- * limitations under the License.\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-16- */\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-17-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-18-package org.apache.linkis.common.utils;\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-19-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-20-import org.apache.commons.lang3.StringUtils;\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-21-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-22-/**\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-23- * Code masking utility for security logging.\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-24- * Prevents sensitive user code from being logged in plain text.\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-25- */\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java:26:public class CodeUtils {\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-27-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-28-  /** Default maximum preview length */\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-29-  private static final int DEFAULT_MAX_PREVIEW_LENGTH = 50;\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-30-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-31-  /** Default preview length for code snippet (first N characters) */\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-32-  private static final int DEFAULT_CODE_SNIPPET_LENGTH = 6;\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-33-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-34-  /** Maximum lines to preview */\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-35-  private static final int MAX_PREVIEW_LINES = 3;\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-36-\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-37-  /**\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-38-   * Mask code for logging - only shows length and line count\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-39-   *\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-40-   * @param code the code to mask\ng:\\kkhuang\\work\\linkis\\linkis-commons\\linkis-common\\src\\main\\java\\org\\apache\\linkis\\common\\utils\\CodeUtils.java-41-   * @return masked code information'}]

**AI**：
[思考] 让我直接搜索 LabelUtil 的源码文件。...


## 交互记录 (2026-04-21T09:30:42.301Z)

**用户**：
[{'tool_use_id': 'call_f3f91944e8034012a71f833c', 'type': 'tool_result', 'content': '<persisted-output>\nOutput too large (29.3KB). Full output saved to: C:\\Users\\v_kkhuang\\.claude\\projects\\g--kkhuang-work-linkis\\aa4f4edc-bf9c-498e-bcec-e2f79df69b93\\tool-results\\call_f3f91944e8034012a71f833c.txt\n\nPreview (first 2KB):\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-301-    .get(TemplateConfUtils.confEngineTypeKey) match {\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-302-      case null => null\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-303-      case value => value.toString\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-304-    }\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-305-}\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-306-```\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-307-\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-308-#### 3.3.4 模板配置获取\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-309-\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-310-```scala\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md:311:private def getEngineTypeFromTemplate(jobRequest: JobRequest): String = {\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-312-  val templateName = TemplateConfUtils.getCustomTemplateConfName(\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-313-    jobRequest.getExecutionCode,\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-314-    CodeAndRunTypeUtils.getLanguageTypeByRunType(jobRequest.getRunType)\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-315-  )\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-316-\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-317-  if (templateName == null) return null\ng:\\kkhuang\\work\\linkis\\docs\\1.17.0\\design\\aisql-starrocks-engine-switch-design.md-318-\n...\n</persisted-output>'}]

**AI**：
[思考] 好的，让我读取这个文件。...


## 交互记录 (2026-04-21T09:30:51.626Z)

**用户**：
[{'tool_use_id': 'call_6ffb8108bba34266856fc102', 'type': 'tool_result', 'content': 'linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\scala\\org\\apache\\linkis\\manager\\label\\utils\\LabelUtil.scala'}]

**AI**：
[思考] 现在我找到了！问题很可能出在 `LabelUtil.getEngineType` 方法（第 49-57 行）：

```scala
def getEngineType(labels: util.List[Label[_]]): String = {
  if (null == labels) return null
  val engineTypeLabel = getEngineTypeLabel(labels)
  if (null != engineTypeLabel) {
    engineTypeLabel.getEngineType
  } else {
    null
  }
}
```

如果 `engineTypeLabel` 为 null，这里会返回 null。然后在 `ComputationExecutor.toExecuteTask` 的第 240 行：

```scala
EngineType.mapStringToEngineType(engineType) match {
  case EngineType.HIVE | EngineType.TRINO => printTaskParamsLog(engineExecutionContext)
  case _ =>
}
```

如果 `engineType` 为 null，`EngineType.mapStringToEngineType(null)` 可能会抛出空指针。

让我看看 `EngineType.mapStringToEngineType` 方法。...


## 交互记录 (2026-04-21T09:30:54.644Z)

**用户**：
[{'tool_use_id': 'call_eaa98a4895dd408bb54bd3e0', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.utils\n19\t\n20\timport org.apache.linkis.manager.label.constant.LabelValueConstant\n21\timport org.apache.linkis.manager.label.entity.{Label, TenantLabel}\n22\timport org.apache.linkis.manager.label.entity.engine.{\n23\t  CodeLanguageLabel,\n24\t  EngineConnModeLabel,\n25\t  EngineTypeLabel,\n26\t  EngingeConnRuntimeModeLabel,\n27\t  UserCreatorLabel\n28\t}\n29\timport org.apache.linkis.manager.label.entity.entrance.{\n30\t  BindEngineLabel,\n31\t  ExecuteOnceLabel,\n32\t  LoadBalanceLabel\n33\t}\n34\timport org.apache.linkis.manager.label.entity.route.RouteLabel\n35\t\n36\timport org.apache.commons.lang3.StringUtils\n37\t\n38\timport java.util\n39\t\n40\timport scala.collection.JavaConverters.asScalaBufferConverter\n41\timport scala.reflect.ClassTag\n42\t\n43\tobject LabelUtil {\n44\t\n45\t  def getEngineTypeLabel(labels: util.List[Label[_]]): EngineTypeLabel = {\n46\t    getLabelFromList[EngineTypeLabel](labels)\n47\t  }\n48\t\n49\t  def getEngineType(labels: util.List[Label[_]]): String = {\n50\t    if (null == labels) return null\n51\t    val engineTypeLabel = getEngineTypeLabel(labels)\n52\t    if (null != engineTypeLabel) {\n53\t      engineTypeLabel.getEngineType\n54\t    } else {\n55\t      null\n56\t    }\n57\t  }\n58\t\n59\t  def getUserCreatorLabel(labels: util.List[Label[_]]): UserCreatorLabel = {\n60\t    getLabelFromList[UserCreatorLabel](labels)\n61\t  }\n62\t\n63\t  def getUserCreator(labels: util.List[Label[_]]): (String, String) = {\n64\t    if (null == labels) return null\n65\t    val userCreatorLabel = getUserCreatorLabel(labels)\n66\t    if (null != userCreatorLabel) {\n67\t      (userCreatorLabel.getUser, userCreatorLabel.getCreator)\n68\t    } else {\n69\t      null\n70\t    }\n71\t  }\n72\t\n73\t  def getCodeType(labels: util.List[Label[_]]): String = {\n74\t    if (null == labels) return null\n75\t    val codeTypeLabel = getCodeTypeLabel(labels)\n76\t    if (null != codeTypeLabel) {\n77\t      codeTypeLabel.getCodeType\n78\t    } else {\n79\t      null\n80\t    }\n81\t  }\n82\t\n83\t  def getCodeTypeLabel(labels: util.List[Label[_]]): CodeLanguageLabel = {\n84\t    getLabelFromList[CodeLanguageLabel](labels)\n85\t  }\n86\t\n87\t  def getTenantValue(labels: util.List[Label[_]]): String = {\n88\t    if (null == labels) return ""\n89\t    val tentantLabel = getTenantLabel(labels)\n90\t    if (null != tentantLabel) {\n91\t      tentantLabel.getTenant\n92\t    } else {\n93\t      ""\n94\t    }\n95\t  }\n96\t\n97\t  def getTenantLabel(labels: util.List[Label[_]]): TenantLabel = {\n98\t    getLabelFromList[TenantLabel](labels)\n99\t  }\n100\t\n101\t  def getEngingeConnRuntimeModeLabel(labels: util.List[Label[_]]): EngingeConnRuntimeModeLabel = {\n102\t    getLabelFromList[EngingeConnRuntimeModeLabel](labels)\n103\t  }\n104\t\n105\t  def getEngineConnModeLabel(labels: util.List[Label[_]]): EngineConnModeLabel = {\n106\t    getLabelFromList[EngineConnModeLabel](labels)\n107\t  }\n108\t\n109\t  def getEngineConnMode(labels: util.List[Label[_]]): String = {\n110\t    if (null == labels) return null\n111\t    val engineConnModeLabel = getEngineConnModeLabel(labels)\n112\t    if (null != engineConnModeLabel) {\n113\t      engineConnModeLabel.getEngineConnMode\n114\t    } else {\n115\t      null\n116\t    }\n117\t  }\n118\t\n119\t  def getBindEngineLabel(labels: util.List[Label[_]]): BindEngineLabel = {\n120\t    getLabelFromList[BindEngineLabel](labels)\n121\t  }\n122\t\n123\t  def getRouteLabel(labels: util.List[Label[_]]): RouteLabel = {\n124\t    getLabelFromList[RouteLabel](labels)\n125\t  }\n126\t\n127\t  def getExecuteOnceLabel(labels: util.List[Label[_]]): ExecuteOnceLabel = {\n128\t    getLabelFromList[ExecuteOnceLabel](labels)\n129\t  }\n130\t\n131\t  def getLoadBalanceLabel(labels: util.List[Label[_]]): LoadBalanceLabel = {\n132\t    getLabelFromList[LoadBalanceLabel](labels)\n133\t  }\n134\t\n135\t  def getLabelFromList[A: ClassTag](labels: util.List[Label[_]]): A = {\n136\t    if (null == labels) return null.asInstanceOf[A]\n137\t    labels.asScala.foreach {\n138\t      case label: A =>\n139\t        return label\n140\t      case _ =>\n141\t    }\n142\t    null.asInstanceOf[A]\n143\t  }\n144\t\n145\t  def getLabelFromArray[A: ClassTag](labels: Array[Label[_]]): A = {\n146\t    if (null == labels) return null.asInstanceOf[A]\n147\t    labels.foreach {\n148\t      case label: A =>\n149\t        return label\n150\t      case _ =>\n151\t    }\n152\t    null.asInstanceOf[A]\n153\t  }\n154\t\n155\t  def isYarnClusterMode(labels: util.List[Label[_]]): Boolean = {\n156\t    val label = LabelUtil.getEngingeConnRuntimeModeLabel(labels)\n157\t    val isYarnClusterMode: Boolean = {\n158\t      if (null != label && label.getModeValue.equals(LabelValueConstant.YARN_CLUSTER_VALUE)) true\n159\t      else false\n160\t    }\n161\t    isYarnClusterMode\n162\t  }\n163\t\n164\t  def getFromLabelStr(labelStr: String, key: String): String = {\n165\t    //  hadoop-IDE,hive-2.3.3  or hadoop-IDE  or hive-2.3.3\n166\t    if (StringUtils.isNotBlank(labelStr)) {\n167\t      val labelArray = labelStr.split(",")\n168\t      (labelArray.length, key.toLowerCase()) match {\n169\t        case (1, "user") => labelStr.split("-")(0)\n170\t        case (1, "creator") => labelStr.split("-")(1)\n171\t        case (1, "engine") => labelStr.split("-")(0)\n172\t        case (1, "version") => labelStr.split("-")(1)\n173\t        case (2, "user") => labelArray(0).split("-")(0)\n174\t        case (2, "creator") => labelArray(0).split("-")(1)\n175\t        case (2, "engine") => labelArray(1).split("-")(0)\n176\t        case (2, "version") => labelArray(1).split("-")(1)\n177\t      }\n178\t    } else {\n179\t      ""\n180\t    }\n181\t  }\n182\t\n183\t  def isTargetEngine(\n184\t      labels: util.List[Label[_]],\n185\t      engine: String,\n186\t      version: String = null\n187\t  ): Boolean = {\n188\t    if (null == labels || StringUtils.isBlank(engine)) return false\n189\t    val engineTypeLabel = getEngineTypeLabel(labels)\n190\t    if (null != engineTypeLabel) {\n191\t      val isEngineMatch = engineTypeLabel.getEngineType.equals(engine)\n192\t      val isVersionMatch =\n193\t        StringUtils.isBlank(version) || engineTypeLabel.getVersion.contains(version)\n194\t      isEngineMatch && isVersionMatch\n195\t    } else {\n196\t      false\n197\t    }\n198\t  }\n199\t\n200\t}\n201\t'}]

**AI**：
让我检查 `EngineType.mapStringToEngineType` 方法：...


## 交互记录 (2026-04-21T09:31:01.258Z)

**用户**：
[{'tool_use_id': 'call_27712ca5256f4b94bd1775d9', 'type': 'tool_result', 'content': 'g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-14- * See the License for the specific language governing permissions and\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-15- * limitations under the License.\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-16- */\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-17-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-18-package org.apache.linkis.manager.label.entity.engine\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-19-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-20-import org.apache.linkis.common.utils.Logging\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-21-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-22-import java.util\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-23-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala:24:object EngineType extends Enumeration with Logging {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-25-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-26-  type EngineType = Value\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-27-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-28-  val SPARK = Value("spark")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-29-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-30-  val HIVE = Value("hive")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-31-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-32-  val HBASE = Value("hbase")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-33-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-34-  val PYTHON = Value("python")\n--\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-81-        EngineType.IO_ENGINE_HDFS.toString\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-82-      case _ =>\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-83-        logger.error(s"In method mapFsTypeToEngineType(): Invalid fsType : ${fsType}, will not convert.")\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-84-        fsType\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-85-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-86-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-87-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-88-  /**\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-89-   * Please add it here when new engineType was added.\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-90-   */\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala:91:  def mapStringToEngineType(str: String): EngineType = str match {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-92-    case _ if null == str || "".equals(str) => null\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-93-    case _ if SPARK.toString.equalsIgnoreCase(str) => SPARK\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-94-    case _ if HIVE.toString.equalsIgnoreCase(str) => HIVE\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-95-    case _ if HBASE.toString.equalsIgnoreCase(str) => HBASE\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-96-    case _ if PYTHON.toString.equalsIgnoreCase(str) => PYTHON\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-97-    case _ if SHELL.toString.equalsIgnoreCase(str) => SHELL\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-98-    case _ if JDBC.toString.equalsIgnoreCase(str) => JDBC\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-99-    case _ if IO_ENGINE_FILE.toString.equalsIgnoreCase(str) => IO_ENGINE_FILE\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-100-    case _ if IO_ENGINE_HDFS.toString.equalsIgnoreCase(str) => IO_ENGINE_HDFS\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-manager\\linkis-label-common\\src\\main\\java\\org\\apache\\linkis\\manager\\label\\entity\\engine\\EngineType.scala-101-    case _ if PIPELINE.toString.equalsIgnoreCase(str) => PIPELINE\n--\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-179-    }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-180-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-181-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-182-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-183-  trait EngineType {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-184-    def getDefaultRunType: RunType\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-185-  }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-186-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-187-  trait RunType\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-188-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala:189:  object EngineType {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-190-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-191-    val SPARK = new EngineType {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-192-      override val toString: String = "spark"\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-193-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-194-      val SQL = new RunType {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-195-        override val toString: String = "sql"\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-196-      }\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-197-\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-198-      val SCALA = new RunType {\ng:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala-199-        override val toString: String = "scala"'}]

**AI**：
[思考] 现在我找到了 `mapStringToEngineType` 方法的实现：

```scala
def mapStringToEngineType(str: String): EngineType = str match {
  case _ if null == str || "".equals(str) => null
  case _ if SPARK.toString.equalsIgnoreCase(str) => SPARK
  ...
}
```

如果 str 为 null，这个方法会返回 null。

现在让我重新分析。在 InitSQLHook 中：

```scala
val codeLanguageLabel = new CodeLanguageLabel
codeLanguageLabel.setCodeType(getRunType())
val labels = Array[Label[_]](codeLanguageLabel)
val engineConnTask = new CommonEngineConnTask("init_sql")
engineConnTask.setLabels(labels)
```

然后当调用 `toExecuteTask` 时：

```scala
val engineType = LabelUtil.getEngineType(engineCreationContext.getLabels())
EngineType.mapStringToEngineType(engineType) match {
  case EngineType.HIVE | EngineType.TRINO => printTaskParamsLog(engineExecutionContext)
  case _ =>
}
```

这里使用的是 `engineCreationContext.getLabels()`，不是 `engineConnTask.getLables()`。

`engineCreationContext.getLabels()` 返回的是 `java.util.List<Label>`，它应该包含 EngineTypeLabel 等信息。

如果 `engineCreationContext.getLabels()` 为 null 或者没有 EngineTypeLabel，那么 `engineType` 就会是 null。

但我看到 `mapStringToEngineType(null)` 返回 null，然后在 match 语句中：


我注意到在 Scala 中，匹配 null 可能会导致不同的行为。当 `engineType` 为 null 时，匹配逻辑可能会引发意外的异常。这意味着在处理 null 引擎类型时需要额外的安全检查，以防止可能的空指针异常。...


## 交互记录 (2026-04-21T09:31:08.219Z)

**用户**：
[{'tool_use_id': 'call_5e9b21e8406c4d9886b63d50', 'type': 'tool_result', 'content': '235\t\n236\t      // task params log\n237\t      // spark engine: at org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor.executeLine log special conf\n238\t      Utils.tryAndWarn {\n239\t        val engineType = LabelUtil.getEngineType(engineCreationContext.getLabels())\n240\t        EngineType.mapStringToEngineType(engineType) match {\n241\t          case EngineType.HIVE | EngineType.TRINO => printTaskParamsLog(engineExecutionContext)\n242\t          case _ =>\n243\t        }\n244\t      }\n245\t\n246\t      val localPath = EngineConnConf.getLogDir\n247\t      engineExecutionContext.appendStdout(\n248\t        LogUtils.generateInfo(\n249\t          s"EngineConn local log path: ${DataWorkCloudApplication.getServiceInstance.toString} $localPath"\n250\t        )\n251\t      )\n252\t\n253\t      var response: ExecuteResponse = null\n254\t      val incomplete = new StringBuilder'}]

**AI**：
让我再仔细分析一下。查看完整的 match 语句：...


## 交互记录 (2026-04-21T09:31:14.859Z)

**用户**：
[{'tool_use_id': 'call_41c12fc995394073ba6506da', 'type': 'tool_result', 'content': '260\t      engineExecutionContext.setTotalParagraph(codes.length)\n261\t\n262\t      val retryEnable: Boolean = SUPPORT_PARTIAL_RETRY_FOR_FAILED_TASKS_ENABLED\n263\t\n264\t      codes.indices.foreach({ index =>\n265\t        if (ExecutionNodeStatus.Cancelled == engineConnTask.getStatus) {\n266\t          return ErrorExecuteResponse("Job is killed by user!", null)\n267\t        }\n268\t        var executeFlag = true\n269\t        val errorIndex: Int = Integer.valueOf(\n270\t          engineConnTask.getProperties\n271\t            .getOrDefault(Configuration.EXECUTE_ERROR_CODE_INDEX.key, "-1")\n272\t            .toString\n273\t        )\n274\t        engineExecutionContext.getProperties\n275\t          .put(Configuration.EXECUTE_ERROR_CODE_INDEX.key, errorIndex.toString)\n276\t        val props: util.Map[String, String] = engineCreationContext.getOptions\n277\t        val taskRetry: String =\n278\t          props.getOrDefault("linkis.task.retry.switch", "false").toString\n279\t        if (java.lang.Boolean.parseBoolean(taskRetry)) {\n280\t          // jdbc执行任务重试，如果sql有被set进sql，会导致sql的index错位，这里会将日志打印的index进行减一，保证用户看的index是正常的，然后重试的errorIndex需要加一，保证重试的位置是正确的\n281\t          var newIndex = index\n282\t          var newErrorIndex = errorIndex\n283\t          if (adjustErrorIndexForSetScenarios(engineConnTask)) {\n284\t            newIndex = index - 1\n285\t            newErrorIndex = errorIndex + 1\n286\t          }\n287\t          // 重试的时候如果执行过则跳过执行\n288\t          if (retryEnable && errorIndex > 0 && index < newErrorIndex) {\n289\t            val code = codes(index).trim.toUpperCase()\n290\t            val shouldSkip = !isContextStatement(code)\n291\t\n292\t            if (shouldSkip) {\n293\t              engineExecutionContext.appendStdout(\n294\t                LogUtils.generateInfo(\n295\t                  s"task retry with errorIndex: ${errorIndex}, current sql index: ${newIndex} will skip."\n296\t                )\n297\t              )\n298\t              executeFlag = false\n299\t            } else {\n300\t              if (newIndex >= 0) {\n301\t                engineExecutionContext.appendStdout(\n302\t                  LogUtils.generateInfo(\n303\t                    s"task retry with errorIndex: ${errorIndex}, current sql index: ${newIndex} is a context statement, will execute."\n304\t                  )\n305\t                )\n306\t              }\n307\t            }\n308\t          }\n309\t        } else {\n310\t          if (retryEnable && errorIndex > 0 && index < errorIndex) {\n311\t            engineExecutionContext.appendStdout(\n312\t              LogUtils.generateInfo(\n313\t                s"aisql retry with errorIndex: ${errorIndex}, current sql index: ${index} will skip."\n314\t              )\n315\t            )\n316\t            executeFlag = false\n317\t          }\n318\t        }\n319\t        if (executeFlag) {\n320\t          val code = codes(index)\n321\t          engineExecutionContext.setCurrentParagraph(index + 1)\n322\t          response = Utils.tryCatch(if (incomplete.nonEmpty) {\n323\t            executeCompletely(engineExecutionContext, code, incomplete.toString())\n324\t          } else executeLine(engineExecutionContext, code)) { t =>\n325\t            ErrorExecuteResponse(ExceptionUtils.getRootCauseMessage(t), t)\n326\t          }\n327\t          // info(s"Finished to execute task ${engineConnTask.getTaskId}")\n328\t          incomplete ++= code\n329\t          response match {\n330\t            case e: ErrorExecuteResponse =>\n331\t              val props: util.Map[String, String] = engineCreationContext.getOptions\n332\t              val taskRetry: String =\n333\t                props.getOrDefault("linkis.task.retry.switch", "false").toString\n334\t              val retryNum: Int =\n335\t                Integer.valueOf(props.getOrDefault("linkis.task.retry.num", "0").toString)\n336\t\n337\t              if (retryEnable && !props.isEmpty && "true".equals(taskRetry) && retryNum > 0) {\n338\t                logger.info(\n339\t                  s"task execute failed, with index: ${index} retryNum: ${retryNum}, and will retry",\n340\t                  e.t\n341\t                )\n342\t                engineExecutionContext.appendStdout(\n343\t                  LogUtils.generateInfo(\n344\t                    s"task execute failed, with index: ${index} retryNum: ${retryNum}, and will retry"\n345\t                  )\n346\t                )\n347\t                engineConnTask.getProperties\n348\t                  .put(Configuration.EXECUTE_ERROR_CODE_INDEX.key, index.toString)\n349\t                return ErrorRetryExecuteResponse(e.message, index, e.t)\n350\t              } else {\n351\t                failedTasks.increase()\n352\t                logger.error("execute code failed!", e.t)\n353\t                return response\n354\t              }\n355\t            case SuccessExecuteResponse() =>\n356\t              engineExecutionContext.appendStdout("\\n")\n357\t              incomplete.setLength(0)\n358\t            case e: OutputExecuteResponse =>\n359\t              incomplete.setLength(0)'}]

**AI**：
[思考] 这段代码被 `Utils.tryAndWarn` 包裹，所以即使这里抛出异常，也不会导致程序崩溃，只会打印警告日志。

让我再看看其他可能导致空指针的地方。实际上，用户说空指针发生在启动 Spark 引擎时执行 init_sql。

让我再看看 InitSQLHook 的代码逻辑，看看是否有其他可能导致空指针的地方。

回看 InitSQLHook：

```scala
val codeLanguageLabel = new CodeLanguageLabel
codeLanguageLabel.setCodeType(getRunType())
val labels = Array[Label[_]](codeLanguageLabel)
val engineConnTask = new CommonEngineConnTask("init_sql")
engineConnTask.setLabels(labels)
engineConnTask.setCode(initSql)
engineConnTask.setProperties(new util.HashMap[String, AnyRef]())
engineConnTask.setStatus(ExecutionNodeStatus.Scheduled)
ExecutorManager.getInstance.getExecutorByLabels(labels) match {
  case executor: ComputationExecutor =>
    executor.toExecuteTask(engineConnTask, internalExecute = true)
  case _ =>
}
```

可能的问题点：
1. `initSql` 可能为 null 或空字符串
2. `labels` 可能为 null 或空数组
3. `executor` 可能为 null（虽然模式匹配会处理这种情况）

让我看看 `CommonEngineConnTask` 的初始化，特别是 `setProperties` 方法。如果传入的是一个空的 HashMap，但后续代码中有对这个 map 的操作，可能会导致 null。

初始化时将 `response` 设置为 null，这可能会引入空指针风险。如果后续代码直接使用 `response` 而没有进行空值检查，就会触发空指针异常。这种情况下，需要在访问 `response` 之前先判断其是否为 null，以确保代码的健壮性。...


## 交互记录 (2026-04-21T09:31:32.689Z)

**用户**：
[{'tool_use_id': 'call_5c7acdf916fa4803b69af9de', 'type': 'tool_result', 'content': '<persisted-output>\nOutput too large (42.9KB). Full output saved to: C:\\Users\\v_kkhuang\\.claude\\projects\\g--kkhuang-work-linkis\\aa4f4edc-bf9c-498e-bcec-e2f79df69b93\\tool-results\\call_5c7acdf916fa4803b69af9de.txt\n\nPreview (first 2KB):\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-32-      "spark.starrocks.write.fe.urls.http" -> config.getUrl,\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-33-      "spark.starrocks.write.fe.urls.jdbc" -> config.getJdbcUrl,\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-34-      "spark.starrocks.write.username" -> config.getUser,\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-35-      "spark.starrocks.write.password" -> config.getPassword,\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-36-      "spark.starrocks.write.properties.ignore_json_size" -> "true",\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-37-      "spark.starrocks.write.database" -> config.getTargetDatabase,\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-38-      "spark.starrocks.write.table" -> config.getTargetTable\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-39-    )\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-40-\ng:\\kkhuang\\work\\linkis\\linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\datacalc\\sink\\StarrocksSink.scala-41-    if (config.getOptions != null && !config.getOptions.isEmpty) {\n...\n</persisted-output>'}]

**AI**：
[思考] 我看到第 276 行：

```scala
val props: util.Map[String, String] = engineCreationContext.getOptions
val taskRetry: String = props.getOrDefault("linkis.task.retry.switch", "false").toString
```

如果 `engineCreationContext.getOptions()` 返回 null，那么这里就会抛出空指针异常！

这可能就是问题所在。让我检查一下 `EngineCreationContext` 的 `getOptions` 方法是否可能返回 null。...


## 交互记录 (2026-04-21T09:31:38.990Z)

**用户**：
[{'tool_use_id': 'call_9b560f0e6ad244e88a7a3dd9', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
这可能就是问题所在！让我检查 `EngineCreationContext` 的 `getOptions` 方法：...


## 交互记录 (2026-04-21T09:31:44.853Z)

**用户**：
[{'tool_use_id': 'call_ec49b0c77e474d8b8137c95d', 'type': 'tool_result', 'content': 'linkis-computation-governance\\linkis-engineconn\\linkis-engineconn-common\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\common\\creation\\EngineCreationContext.scala'}]

**AI**：
[思考] 让我直接搜索 EngineCreationContext 类的定义。...


## 交互记录 (2026-04-21T09:31:46.703Z)

**用户**：
[{'tool_use_id': 'call_0f39dcd02d134bd3a36d3f2f', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.engineconn.common.creation\n19\t\n20\timport org.apache.linkis.common.ServiceInstance\n21\timport org.apache.linkis.manager.label.entity.Label\n22\t\n23\timport java.util\n24\t\n25\ttrait EngineCreationContext {\n26\t\n27\t  def getUser: String\n28\t\n29\t  def setUser(user: String): Unit\n30\t\n31\t  def getTicketId: String\n32\t\n33\t  def setTicketId(ticketId: String): Unit\n34\t\n35\t  def getLabels(): util.List[Label[_]]\n36\t\n37\t  def setLabels(labels: util.List[Label[_]]): Unit\n38\t\n39\t  def getEMInstance: ServiceInstance\n40\t\n41\t  def setEMInstance(instance: ServiceInstance): Unit\n42\t\n43\t  def getOptions: util.Map[String, String]\n44\t\n45\t  def setOptions(options: util.Map[String, String])\n46\t\n47\t  def getExecutorId: Int\n48\t\n49\t  def setExecutorId(id: Int)\n50\t\n51\t  def setArgs(args: Array[String])\n52\t\n53\t  def getArgs: Array[String]\n54\t}\n55\t\n56\tclass DefaultEngineCreationContext extends EngineCreationContext {\n57\t\n58\t  private var user: String = _\n59\t\n60\t  private var ticketId: String = _\n61\t\n62\t  private var labels: util.List[Label[_]] = _\n63\t\n64\t  private var options: util.Map[String, String] = _\n65\t\n66\t  private var emInstance: ServiceInstance = _\n67\t\n68\t  private var executorId: Int = 0\n69\t\n70\t  private var args: Array[String] = null\n71\t\n72\t  override def getTicketId: String = this.ticketId\n73\t\n74\t  override def setTicketId(ticketId: String): Unit = this.ticketId = ticketId\n75\t\n76\t  override def getLabels(): util.List[Label[_]] = this.labels\n77\t\n78\t  override def setLabels(labels: util.List[Label[_]]): Unit = this.labels = labels\n79\t\n80\t  override def getEMInstance: ServiceInstance = this.emInstance\n81\t\n82\t  override def setEMInstance(instance: ServiceInstance): Unit = this.emInstance = instance\n83\t\n84\t  override def getOptions: util.Map[String, String] = this.options\n85\t\n86\t  override def setOptions(options: util.Map[String, String]): Unit = this.options = options\n87\t\n88\t  override def getUser: String = user\n89\t\n90\t  override def setUser(user: String): Unit = this.user = user\n91\t\n92\t  override def getExecutorId: Int = executorId\n93\t\n94\t  override def setExecutorId(id: Int): Unit = executorId = id\n95\t\n96\t  override def setArgs(args: Array[String]): Unit = this.args = args\n97\t\n98\t  override def getArgs: Array[String] = args\n99\t\n100\t  override def toString: String =\n101\t    s"DefaultEngineCreationContext(user-$user, ticketID-$ticketId, emInstance-$emInstance)"\n102\t\n103\t}\n104\t'}]

**AI**：
[思考] 让我搜索 scala 文件。...


## 交互记录 (2026-04-21T09:34:05.909Z)

**用户**：
[{'type': 'text', 'text': 'Id-] - hduser0105 engineConn skip execute use default db\n2026-04-21 17:27:07.126 [INFO ] [main                                    ] o.a.l.e.c.e.h.SparkInitSQLHook (91) [readFile] [JobId-] - read file: /appcom/config/hive-config/init_sql/hduser0105_hive.sql\n2026-04-21 17:27:07.139 [INFO ] [main                                    ] o.a.l.e.c.e.h.SparkInitSQLHook (73) [apply$mcV$sp] [JobId-] - hduser0105 engineConn begin to run init_sql\n2026-04-21 17:27:07.147 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (599) [transformTaskStatus] [JobId-] - task init_sql from status Scheduled to new status Running\n2026-04-21 17:27:07.198 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (526) [createEngineExecutionContext] [JobId-] - StorePath : null.\n2026-04-21 17:27:07.213 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (230) [apply] [JobId-] - ComputationExecutor code hook completed.\n2026-04-21 17:27:07.218 [INFO ] [main                                    ] o.a.l.e.c.e.e.EngineExecutionContext (191) [appendStdout] [JobId-] - 2026-04-21 17:27:07.027 INFO EngineConn local log path: ServiceInstance(linkis-cg-engineconn, gz.xg.bdplinkis110004.webank:28425) /data/bdp/linkis/hduser0105/20260421/spark/deb6930f-6fcb-4c1b-8980-a798fbca7529/logs\n2026-04-21 17:27:07.248 [INFO ] [main                                    ] o.a.l.e.c.e.e.EngineExecutionContext (191) [appendStdout] [JobId-] - 2026-04-21 17:27:07.027 INFO Yarn application url:http://hadoop:bdphdp@2021@10.107.117.176:8088/cluster/app/application_1776414798678_40917\n2026-04-21 17:27:07.265 [INFO ] [main                                    ] o.a.l.e.s.c.CSSparkPreExecutionHook (60) [callPreExecutionHook] [JobId-] - cs info is null skip CSSparkPreExecutionHook\n2026-04-21 17:27:07.271 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (196) [apply] [JobId-] - Ready to run code with kind sparksql.\n2026-04-21 17:27:07.271 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (204) [apply] [JobId-] - Set jobGroup to linkis-spark-mix-code-1\n2026-04-21 17:27:07.274 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (302) [org$apache$linkis$engineplugin$spark$executor$SparkEngineConnExecutor$$setSparkExecutorParams] [JobId-] - Spark executor params setting is disabled\n2026-04-21 17:27:07.279 [WARN ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (262) [apply] [JobId-] - Get actual used resource exception java.lang.NullPointerException: null\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2$$anonfun$apply$4.apply$mcV$sp(SparkEngineConnExecutor.scala:234) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2$$anonfun$apply$4.apply(SparkEngineConnExecutor.scala:217) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2$$anonfun$apply$4.apply(SparkEngineConnExecutor.scala:217) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryCatch(Utils.scala:51) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2.apply(SparkEngineConnExecutor.scala:261) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2.apply(SparkEngineConnExecutor.scala:115) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryFinally(Utils.scala:79) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor.executeLine(SparkEngineConnExecutor.scala:278) ~[linkis-engineplugin-spark-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2$$anonfun$apply$8$$anonfun$apply$9.apply(ComputationExecutor.scala:320) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2$$anonfun$apply$8$$anonfun$apply$9.apply(ComputationExecutor.scala:318) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryCatch(Utils.scala:51) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2$$anonfun$apply$8.apply(ComputationExecutor.scala:320) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2$$anonfun$apply$8.apply(ComputationExecutor.scala:260) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat scala.collection.immutable.Range.foreach(Range.scala:160) ~[scala-library-2.11.12.jar:?]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2.apply(ComputationExecutor.scala:260) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor$$anonfun$toExecuteTask$2.apply(ComputationExecutor.scala:209) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryFinally(Utils.scala:79) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.execute.ComputationExecutor.toExecuteTask(ComputationExecutor.scala:398) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.hook.InitSQLHook$$anonfun$afterExecutionExecute$1.apply$mcV$sp(InitSQLHook.scala:84) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.hook.InitSQLHook$$anonfun$afterExecutionExecute$1.apply(InitSQLHook.scala:57) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.hook.InitSQLHook$$anonfun$afterExecutionExecute$1.apply(InitSQLHook.scala:57) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryCatch(Utils.scala:51) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.common.utils.Utils$.tryAndError(Utils.scala:117) ~[linkis-common-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.computation.executor.hook.InitSQLHook.afterExecutionExecute(InitSQLHook.scala:57) ~[linkis-computation-engineconn-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.launch.EngineConnServer$$anonfun$main$5.apply(EngineConnServer.scala:103) ~[linkis-engineconn-core-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.launch.EngineConnServer$$anonfun$main$5.apply(EngineConnServer.scala:103) ~[linkis-engineconn-core-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat scala.collection.IndexedSeqOptimized$class.foreach(IndexedSeqOptimized.scala:33) ~[scala-library-2.11.12.jar:?]\n\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:186) ~[scala-library-2.11.12.jar:?]\n\tat org.apache.linkis.engineconn.launch.EngineConnServer$.main(EngineConnServer.scala:103) ~[linkis-engineconn-core-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat org.apache.linkis.engineconn.launch.EngineConnServer.main(EngineConnServer.scala) ~[linkis-engineconn-core-1.18.0-wds-hadoop3.jar:1.18.0-wds-hadoop3]\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:1.8.0_382]\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[?:1.8.0_382]\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:1.8.0_382]\n\tat java.lang.reflect.Method.invoke(Method.java:498) ~[?:1.8.0_382]\n\tat org.apache.spark.deploy.JavaMainApplication.start(SparkApplication.scala:52) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit.org$apache$spark$deploy$SparkSubmit$$runMain(SparkSubmit.scala:849) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit.doRunMain$1(SparkSubmit.scala:167) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit.submit(SparkSubmit.scala:195) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit.doSubmit(SparkSubmit.scala:86) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit$$anon$2.doSubmit(SparkSubmit.scala:924) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit$.main(SparkSubmit.scala:933) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\tat org.apache.spark.deploy.SparkSubmit.main(SparkSubmit.scala) ~[spark-core_2.11-2.4.3.jar:2.4.3]\n\n2026-04-21 17:27:07.312 [INFO ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (69) [runCode] [JobId-] - SQLExecutor run query: set mapreduce.job.queuename=q01\n 这个是真实的报错，看下和你分析的一致吗'}]

**AI**：
[思考] 让我读取这个文件。...


## 交互记录 (2026-04-21T09:34:12.070Z)

**用户**：
[{'tool_use_id': 'call_30eca1ea975f421abd9be98c', 'type': 'tool_result', 'content': '215\t      Utils.tryCatch({\n216\t        val executorNum: Int = sc.getConf.get("spark.executor.instances").toInt\n217\t        val executorMem: Long =\n218\t          ByteTimeUtils.byteStringAsGb(sc.getConf.get("spark.executor.memory"))\n219\t        val driverMem: Long = ByteTimeUtils.byteStringAsGb(sc.getConf.get("spark.driver.memory"))\n220\t        val sparkExecutorCores = sc.getConf.get("spark.executor.cores", "2").toInt\n221\t        val sparkDriverCores = sc.getConf.get("spark.driver.cores", "1").toInt\n222\t        val queue = sc.getConf.get("spark.yarn.queue")\n223\t        // with unit if set configuration with unit\n224\t        // if not set sc get will get the value of spark.yarn.executor.memoryOverhead such as 512(without unit)\n225\t        val memoryOverhead = sc.getConf.get("spark.executor.memoryOverhead", "1G")\n226\t        val pythonVersion = SparkConfiguration.SPARK_PYTHON_VERSION.getValue(\n227\t          EngineConnObject.getEngineCreationContext.getOptions\n228\t        )\n229\t        var engineType = ""\n230\t        val labels = engineExecutorContext.getLabels\n231\t        if (labels.length > 0) {\n232\t          engineType = LabelUtil.getEngineTypeLabel(labels.toList.asJava).getStringValue\n233\t        }\n234\t        val sb = new StringBuilder\n235\t        sb.append(s"spark.executor.instances=$executorNum\\n")\n236\t        sb.append(s"spark.executor.memory=${executorMem}G\\n")\n237\t        sb.append(s"spark.driver.memory=${driverMem}G\\n")\n238\t        sb.append(s"spark.executor.cores=$sparkExecutorCores\\n")\n239\t        sb.append(s"spark.driver.cores=$sparkDriverCores\\n")\n240\t        sb.append(s"spark.yarn.queue=$queue\\n")\n241\t        sb.append(s"spark.executor.memoryOverhead=${memoryOverhead}\\n")\n242\t        sb.append(s"spark.python.version=$pythonVersion\\n")\n243\t        sb.append(s"spark.engineType=$engineType\\n")\n244\t        val dynamicAllocation: String = sc.getConf.get("spark.dynamicAllocation.enabled", "false")\n245\t        if ("true".equals(dynamicAllocation)) {\n246\t          val shuffleEnabled: String = sc.getConf.get("spark.shuffle.service.enabled", "false")\n247\t          val minExecutors: Int = sc.getConf.get("spark.dynamicAllocation.minExecutors", "1").toInt\n248\t          val maxExecutors: Int =\n249\t            sc.getConf.get("spark.dynamicAllocation.maxExecutors", "50").toInt\n250\t          sb.append("spark.dynamicAllocation.enabled=true\\n")\n251\t          sb.append(s"spark.shuffle.service.enabled=$shuffleEnabled\\n")\n252\t          sb.append(s"spark.dynamicAllocation.minExecutors=$minExecutors\\n")\n253\t          sb.append(s"spark.dynamicAllocation.maxExecutors=$maxExecutors\\n")\n254\t        }\n255\t        sb.append("\\n")\n256\t        engineExecutionContext.appendStdout(\n257\t          LogUtils.generateInfo(s" Your spark job exec with configs:\\n${sb.toString()}")\n258\t        )\n259\t      })(t => {\n260\t        logger.warn("Get actual used resource exception", t)\n261\t      })\n262\t    }\n263\t\n264\t    val response = Utils.tryFinally(runCode(this, _code, engineExecutorContext, jobGroup)) {'}]

**AI**：
[思考] 我找到了！`DefaultEngineCreationContext` 的 `options` 字段初始值是 `_`（即 null）：

```scala
private var options: util.Map[String, String] = _
```

如果 `setOptions` 没有被调用，`getOptions` 就会返回 null。

现在回到 `ComputationExecutor.toExecuteTask` 方法中的代码：

```scala
val props: util.Map[String, String] = engineCreationContext.getOptions
val taskRetry: String = props.getOrDefault("linkis.task.retry.switch", "false").toString
```

如果 `engineCreationContext.getOptions()` 返回 null，那么 `props.getOrDefault(...)` 就会抛出 `NullPointerException`。

**这就是问题的根本原因！**

在 Spark 引擎启动时，`EngineCreationContext` 的 `options` 可能还没有被设置，当 `InitSQLHook` 尝试执行 init_sql 时，`toExecuteTask` 方法中访问 `engineCreationContext.getOptions()` 返回 null，导致空指针异常。

让我总结一下问题和解决方案。...


## 交互记录 (2026-04-21T09:34:24.262Z)

**用户**：
[{'type': 'tool_result', 'content': '<tool_use_error>Path does not exist: linkis-commons/linkis-common/src/main/java/org/apache/linkis/common/conf. Note: your current working directory is g:\\kkhuang\\work\\linkis.</tool_use_error>', 'is_error': True, 'tool_use_id': 'call_70fd613484af42b09cf8ac2c'}]

**AI**：
## 问题分析

我找到了空指针的原因！

### 根本原因

在 [ComputationExecutor.scala:276-278](linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/execute/ComputationExecutor.scala#L276-L278) 中：

```scala
val props: util.Map[String, String] = engineCreationContext.getOptions
val taskRetry: String = props.getOrDefault("linkis.task.retry.switch", "false").toString
```

如果 `engineCreationContext.getOptions()` 返回 `null`，第二行调用 `props.getOrDefault(...)` 就会抛出 `NullPointerException`。

### 为什么会返回 null？

从 [DefaultEngineCreationContext.scala:64](linkis-computation-governance/linkis-engineconn/linkis-engineconn-common/src/main/scala/org/apache/linkis/engineconn/common/creation/DefaultEngineCreationContext.scala#L64) 可以看到：

```scala
private var options: util.Map[String, String] = _  // 初始值为 null
```

在 Spark 引擎启动流程中，`InitSQLHook` 的 `afterExecutionExecute` 在引擎初始化早期被调用，此时 `EngineCreationContext` 的 `options` 可能还没有被正确设置。

### 解决方案

有两种解决方案：

**方案 1：修复 ComputationExecutor 中的空指针问题**（推荐）

在 [ComputationExecutor.scala:276-278](linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/execute/ComputationExecutor.scala#L276-L278) 添加空值检查：

```scala
val props: util.Map[String, String] = engineCreationContext.getOptions
val taskRetry: String = if (props != null) {
  props.getOrDefault("linkis.task.retry.switch", "false").toString
} else {
  "false"
}
```

**方案 2：在 InitSQLHook 中确保 options 被设置**

在执行任务前，确保 `engineCreationContext` 的 `options` 不为 null。但这需要修改 Hook 的实现，可能影响范围更大。

需要我帮你生成修复代码吗？...


## 交互记录 (2026-04-21T09:34:29.960Z)

**用户**：
[{'tool_use_id': 'call_0aca7427e88744ac83e8feb9', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
[思考] 用户提供了真实的错误日志。让我仔细分析一下：

```
2026-04-21 17:27:07.279 [WARN ] [main                                    ] o.a.l.e.s.e.SparkSqlExecutor (262) [apply] [JobId-] - Get actual used resource exception java.lang.NullPointerException: null
	at org.apache.linkis.engineplugin.spark.executor.SparkEngineConnExecutor$$anonfun$executeLine$2$$anonfun$apply$4.apply$mcV$sp(SparkEngineConnExecutor.scala:234)
```

关键信息：
1. 错误发生在 `SparkEngineConnExecutor.scala:234`
2. 调用栈显示是在 `executeLine` 方法中
3. 错误信息是 "Get actual used resource exception"

这和我之前分析的位置不太一样。我之前分析的是 `ComputationExecutor.scala:276` 的 `engineCreationContext.getOptions`，但实际错误是在 `SparkEngineConnExecutor.scala:234`。

让我查看一下 SparkEngineConnExecutor 的代码，特别是第 234 行附近的代码。...


## 交互记录 (2026-04-21T09:34:35.644Z)

**用户**：
[{'tool_use_id': 'call_5eb6e41120b04a509db060c2', 'type': 'tool_result', 'content': 'linkis-commons\\linkis-common\\src\\main\\scala\\org\\apache\\linkis\\common\\conf\\CommonVars.scala'}]

**AI**：
让我查看实际的错误位置：...


## 交互记录 (2026-04-21T09:34:38.009Z)

**用户**：
[{'tool_use_id': 'call_b8ceaaf524e04c28a0687dc6', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.common.conf\n19\t\n20\timport java.util.Properties\n21\t\n22\timport scala.collection.JavaConverters._\n23\t\n24\tcase class CommonVars[T](key: String, defaultValue: T, value: T, description: String = null) {\n25\t  val getValue: T = BDPConfiguration.getOption(this).getOrElse(defaultValue)\n26\t\n27\t  def getHotValue(): T = BDPConfiguration.getOption(this, true).getOrElse(defaultValue)\n28\t\n29\t  def getValue(properties: java.util.Map[String, String]): T = {\n30\t    if (properties == null || !properties.containsKey(key) || properties.get(key) == null) {\n31\t      getValue\n32\t    } else BDPConfiguration.formatValue(defaultValue, Option(properties.get(key))).get\n33\t  }\n34\t\n35\t  def getValue(properties: Map[String, String], hotload: Boolean = false): T = getValue(\n36\t    properties.asJava\n37\t  )\n38\t\n39\t  def getValue(properties: java.util.Map[String, String], hotload: Boolean): T = {\n40\t    if (properties == null || !properties.containsKey(key) || properties.get(key) == null) {\n41\t      if (hotload) {\n42\t        getHotValue()\n43\t      } else {\n44\t        getValue\n45\t      }\n46\t    } else BDPConfiguration.formatValue(defaultValue, Option(properties.get(key))).get\n47\t  }\n48\t\n49\t  def acquireNew: T = getHotValue()\n50\t}\n51\t\n52\tobject CommonVars {\n53\t\n54\t  def apply[T](key: String, defaultValue: T, description: String): CommonVars[T] =\n55\t    CommonVars(key, defaultValue, null.asInstanceOf[T], description)\n56\t\n57\t  implicit def apply[T](key: String, defaultValue: T): CommonVars[T] =\n58\t    new CommonVars(key, defaultValue, null.asInstanceOf[T], null)\n59\t\n60\t  implicit def apply[T](key: String): CommonVars[T] =\n61\t    apply(key, null.asInstanceOf[T], null.asInstanceOf[T], null)\n62\t\n63\t  def properties: Properties = BDPConfiguration.properties\n64\t\n65\t  def hotProperties: Properties = BDPConfiguration.hotProperties\n66\t}\n67\t'}]

**AI**：
[思考] 现在我找到了！错误发生在第 227 行：

```scala
val pythonVersion = SparkConfiguration.SPARK_PYTHON_VERSION.getValue(
  EngineConnObject.getEngineCreationContext.getOptions
)
```

这里调用了 `EngineConnObject.getEngineCreationContext.getOptions`，如果 `getOptions` 返回 `null`，就会导致 `NullPointerException`。

让我再看看第 230-232 行：

```scala
val labels = engineExecutorContext.getLabels
if (labels.length > 0) {
  engineType = LabelUtil.getEngineTypeLabel(labels.toList.asJava).getStringValue
}
```

这里也有潜在的问题，因为 `getStringValue()` 可能返回 `null`，然后 `engineType` 为 `null`，但这不会导致空指针，因为它只是一个字符串。

综合分析：
1. 错误发生在 `SparkEngineConnExecutor.scala:227` 行
2. 原因是 `EngineConnObject.getEngineCreationContext.getOptions` 返回 `null`
3. 这个 null 被传递给 `getValue` 方法，导致空指针

这和我之前的分析是一致的，只是具体位置稍有不同。我之前分析的是 `ComputationExecutor.scala:276`，但实际上在这个代码路径执行之前，就已经在 `SparkEngineConnExecutor.scala:227` 抛出异常了。

让我再看看 `getValue` 方法的实现，看看它是否能处理 null 参数。...


## 交互记录 (2026-04-21T09:34:48.956Z)

**用户**：
[{'tool_use_id': 'call_d4ef3cba68ef410c93b06dad', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.manager.label.utils\n19\t\n20\timport org.apache.linkis.manager.label.constant.LabelValueConstant\n21\timport org.apache.linkis.manager.label.entity.{Label, TenantLabel}\n22\timport org.apache.linkis.manager.label.entity.engine.{\n23\t  CodeLanguageLabel,\n24\t  EngineConnModeLabel,\n25\t  EngineTypeLabel,\n26\t  EngingeConnRuntimeModeLabel,\n27\t  UserCreatorLabel\n28\t}\n29\timport org.apache.linkis.manager.label.entity.entrance.{\n30\t  BindEngineLabel,\n31\t  ExecuteOnceLabel,\n32\t  LoadBalanceLabel\n33\t}\n34\timport org.apache.linkis.manager.label.entity.route.RouteLabel\n35\t\n36\timport org.apache.commons.lang3.StringUtils\n37\t\n38\timport java.util\n39\t\n40\timport scala.collection.JavaConverters.asScalaBufferConverter\n41\timport scala.reflect.ClassTag\n42\t\n43\tobject LabelUtil {\n44\t\n45\t  def getEngineTypeLabel(labels: util.List[Label[_]]): EngineTypeLabel = {\n46\t    getLabelFromList[EngineTypeLabel](labels)\n47\t  }\n48\t\n49\t  def getEngineType(labels: util.List[Label[_]]): String = {\n50\t    if (null == labels) return null\n51\t    val engineTypeLabel = getEngineTypeLabel(labels)\n52\t    if (null != engineTypeLabel) {\n53\t      engineTypeLabel.getEngineType\n54\t    } else {\n55\t      null\n56\t    }\n57\t  }\n58\t\n59\t  def getUserCreatorLabel(labels: util.List[Label[_]]): UserCreatorLabel = {\n60\t    getLabelFromList[UserCreatorLabel](labels)'}]

**AI**：
找到了！让我再看看 `CommonVars.getValue` 方法：...


## 交互记录 (2026-04-21T09:37:03.486Z)

**用户**：
[{'type': 'text', 'text': '<ide_selection>The user selected the lines 22 to 22 from Untitled-1:\nComputationExecutor\n\nThis may or may not be related to the current task.</ide_selection>'}, {'type': 'text', 'text': '修复下'}]

**AI**：
[思考] 路径不对，让我重新搜索。...


## 交互记录 (2026-04-21T09:37:09.747Z)

**用户**：
[{'tool_use_id': 'call_665de3bef7c54883967446ae', 'type': 'tool_result', 'content': '200\t      queryNum.incrementAndGet()\n201\t    }\n202\t    jobGroup = String.valueOf("linkis-spark-mix-code-" + jobGroupId)\n203\t    //    val executeCount = queryNum.get().toInt - 1\n204\t    logger.info("Set jobGroup to " + jobGroup)\n205\t    sc.setJobGroup(jobGroup, _code, true)\n206\t\n207\t    // Set spark executor params to executor side\n208\t    Utils.tryAndWarn(setSparkExecutorParams(sc, engineContext))\n209\t\n210\t    // print job configuration, only the first paragraph or retry\n211\t    val errorIndex: Integer = Integer.valueOf(\n212\t      engineExecutionContext.getProperties.getOrDefault("execute.error.code.index", "-1").toString\n213\t    )\n214\t    if (isFirstParagraph || (errorIndex + 1 == engineExecutorContext.getCurrentParagraph)) {\n215\t      Utils.tryCatch({\n216\t        val executorNum: Int = sc.getConf.get("spark.executor.instances").toInt\n217\t        val executorMem: Long =\n218\t          ByteTimeUtils.byteStringAsGb(sc.getConf.get("spark.executor.memory"))\n219\t        val driverMem: Long = ByteTimeUtils.byteStringAsGb(sc.getConf.get("spark.driver.memory"))\n220\t        val sparkExecutorCores = sc.getConf.get("spark.executor.cores", "2").toInt\n221\t        val sparkDriverCores = sc.getConf.get("spark.driver.cores", "1").toInt\n222\t        val queue = sc.getConf.get("spark.yarn.queue")\n223\t        // with unit if set configuration with unit\n224\t        // if not set sc get will get the value of spark.yarn.executor.memoryOverhead such as 512(without unit)\n225\t        val memoryOverhead = sc.getConf.get("spark.executor.memoryOverhead", "1G")\n226\t        val pythonVersion = SparkConfiguration.SPARK_PYTHON_VERSION.getValue(\n227\t          EngineConnObject.getEngineCreationContext.getOptions\n228\t        )\n229\t        var engineType = ""\n230\t        val labels = engineExecutorContext.getLabels\n231\t        if (labels.length > 0) {\n232\t          engineType = LabelUtil.getEngineTypeLabel(labels.toList.asJava).getStringValue\n233\t        }\n234\t        val sb = new StringBuilder\n235\t        sb.append(s"spark.executor.instances=$executorNum\\n")\n236\t        sb.append(s"spark.executor.memory=${executorMem}G\\n")\n237\t        sb.append(s"spark.driver.memory=${driverMem}G\\n")\n238\t        sb.append(s"spark.executor.cores=$sparkExecutorCores\\n")\n239\t        sb.append(s"spark.driver.cores=$sparkDriverCores\\n")\n240\t        sb.append(s"spark.yarn.queue=$queue\\n")\n241\t        sb.append(s"spark.executor.memoryOverhead=${memoryOverhead}\\n")\n242\t        sb.append(s"spark.python.version=$pythonVersion\\n")\n243\t        sb.append(s"spark.engineType=$engineType\\n")\n244\t        val dynamicAllocation: String = sc.getConf.get("spark.dynamicAllocation.enabled", "false")\n245\t        if ("true".equals(dynamicAllocation)) {\n246\t          val shuffleEnabled: String = sc.getConf.get("spark.shuffle.service.enabled", "false")\n247\t          val minExecutors: Int = sc.getConf.get("spark.dynamicAllocation.minExecutors", "1").toInt\n248\t          val maxExecutors: Int =\n249\t            sc.getConf.get("spark.dynamicAllocation.maxExecutors", "50").toInt\n250\t          sb.append("spark.dynamicAllocation.enabled=true\\n")\n251\t          sb.append(s"spark.shuffle.service.enabled=$shuffleEnabled\\n")\n252\t          sb.append(s"spark.dynamicAllocation.minExecutors=$minExecutors\\n")\n253\t          sb.append(s"spark.dynamicAllocation.maxExecutors=$maxExecutors\\n")\n254\t        }\n255\t        sb.append("\\n")\n256\t        engineExecutionContext.appendStdout(\n257\t          LogUtils.generateInfo(s" Your spark job exec with configs:\\n${sb.toString()}")\n258\t        )\n259\t      })(t => {\n260\t        logger.warn("Get actual used resource exception", t)\n261\t      })\n262\t    }\n263\t\n264\t    val response = Utils.tryFinally(runCode(this, _code, engineExecutorContext, jobGroup)) {\n265\t      // Utils.tryAndWarn(this.engineExecutionContext.pushProgress(1, getProgressInfo("")))\n266\t      jobGroup = null\n267\t      sc.clearJobGroup()\n268\t    }\n269\t    // Post-execution hook\n270\t    Utils.tryQuietly(\n271\t      SparkPostExecutionHook\n272\t        .getSparkPostExecutionHooks()\n273\t        .foreach(_.callPostExecutionHook(engineExecutorContext, response, code))\n274\t    )\n275\t    response\n276\t  } {\n277\t    this.engineExecutionContext = null\n278\t  }\n279\t'}]

**AI**：
[思考] 让我搜索 Scala 文件。...


## 交互记录 (2026-04-21T09:37:18.706Z)

**用户**：
[{'tool_use_id': 'call_6750d58b7ca943228e480911', 'type': 'tool_result', 'content': 'The file linkis-engineconn-plugins/spark/src/main/scala/org/apache/linkis/engineplugin/spark/executor/SparkEngineConnExecutor.scala has been updated successfully.'}]

**AI**：
[思考] 找到了，让我读取这个文件。...


## 交互记录 (2026-04-21T09:37:23.965Z)

**用户**：
[{'tool_use_id': 'call_cfa9be447fac4493bbba08a2', 'type': 'tool_result', 'content': '223\t        // with unit if set configuration with unit\n224\t        // if not set sc get will get the value of spark.yarn.executor.memoryOverhead such as 512(without unit)\n225\t        val memoryOverhead = sc.getConf.get("spark.executor.memoryOverhead", "1G")\n226\t        val engineCreationOptions = EngineConnObject.getEngineCreationContext.getOptions\n227\t        val pythonVersion = if (engineCreationOptions != null) {\n228\t          SparkConfiguration.SPARK_PYTHON_VERSION.getValue(engineCreationOptions)\n229\t        } else {\n230\t          SparkConfiguration.SPARK_PYTHON_VERSION.getValue\n231\t        }\n232\t        var engineType = ""\n233\t        val labels = engineExecutorContext.getLabels\n234\t        if (labels.length > 0) {\n235\t          val engineTypeLabel = LabelUtil.getEngineTypeLabel(labels.toList.asJava)\n236\t          if (engineTypeLabel != null) {\n237\t            engineType = engineTypeLabel.getStringValue\n238\t          }\n239\t        }\n240\t        val sb = new StringBuilder\n241\t        sb.append(s"spark.executor.instances=$executorNum\\n")\n242\t        sb.append(s"spark.executor.memory=${executorMem}G\\n")\n243\t        sb.append(s"spark.driver.memory=${driverMem}G\\n")\n244\t        sb.append(s"spark.executor.cores=$sparkExecutorCores\\n")\n245\t        sb.append(s"spark.driver.cores=$sparkDriverCores\\n")\n246\t        sb.append(s"spark.yarn.queue=$queue\\n")\n247\t        sb.append(s"spark.executor.memoryOverhead=${memoryOverhead}\\n")\n248\t        sb.append(s"spark.python.version=$pythonVersion\\n")\n249\t        sb.append(s"spark.engineType=$engineType\\n")\n250\t        val dynamicAllocation: String = sc.getConf.get("spark.dynamicAllocation.enabled", "false")\n251\t        if ("true".equals(dynamicAllocation)) {\n252\t          val shuffleEnabled: String = sc.getConf.get("spark.shuffle.service.enabled", "false")'}]

**AI**：
[思考] 现在我看到了 `getValue` 方法的实现：```scala
def getValue(properties: java.util.Map[String, String]): T = {
  if (properties == null || !properties.containsKey(key) || properties.get(key) == null) {
    getValue
  } else BDPConfiguration.formatValue(defaultValue, Option(properties.get(key))).get
}
```

这个方法实际上已经处理了 `properties == null` 的情况！如果 properties 为 null，它会返回 `getValue`（默认值）。

让我再看看 SparkEngineConnExecutor.scala:227 行的代码：```scala
val pythonVersion = SparkConfiguration.SPARK_PYTHON_VERSION.getValue(
  EngineConnObject.getEngineCreationContext.getOptions
)
```

等等，再看一下第 230-233 行：```scala
val labels = engineExecutorContext.getLabels
if (labels.length > 0) {
  engineType = LabelUtil.getEngineTypeLabel(labels.toList.asJava).getStringValue
}
```

问题可能出在这里！`LabelUtil.getEngineTypeLabel(labels.toList.asJava)` 可能返回 null，然后调用 `getStringValue()` 就会导致空指针！让我再看看 LabelUtil.getEngineTypeLabel 的实现。...

