
## 交互记录 (2026-03-30T03:52:48.994Z)

**用户**：
[{'type': 'text', 'text': '分析下这方法org.apache.linkis.engineplugin.spark.executor.SparkPythonExecutor#appendErrorOutput'}]

**AI**：
[思考] 用户想让我分析 `org.apache.linkis.engineplugin.spark.executor.SparkPythonExecutor#appendErrorOutput` 这个方法。我需要先找到这个文件并阅读这个方法的实现。

让我先搜索这个类文件。...


## 交互记录 (2026-03-30T03:53:13.844Z)

**用户**：
[{'tool_use_id': 'call_902aa48271314f02a7101cfa', 'type': 'tool_result', 'content': 'linkis-engineconn-plugins\\spark\\src\\main\\scala\\org\\apache\\linkis\\engineplugin\\spark\\executor\\SparkPythonExecutor.scala'}]

**AI**：
我来帮你分析这个方法。首先找到这个类文件。...


## 交互记录 (2026-03-30T03:53:15.447Z)

**用户**：
[{'tool_use_id': 'call_1bab134e0d2046a3af1ceae8', 'type': 'tool_result', 'content': '     1→/*\n     2→ * Licensed to the Apache Software Foundation (ASF) under one or more\n     3→ * contributor license agreements.  See the NOTICE file distributed with\n     4→ * this work for additional information regarding copyright ownership.\n     5→ * The ASF licenses this file to You under the Apache License, Version 2.0\n     6→ * (the "License"); you may not use this file except in compliance with\n     7→ * the License.  You may obtain a copy of the License at\n     8→ *\n     9→ *    http://www.apache.org/licenses/LICENSE-2.0\n    10→ *\n    11→ * Unless required by applicable law or agreed to in writing, software\n    12→ * distributed under the License is distributed on an "AS IS" BASIS,\n    13→ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n    14→ * See the License for the specific language governing permissions and\n    15→ * limitations under the License.\n    16→ */\n    17→\n    18→package org.apache.linkis.engineplugin.spark.executor\n    19→\n    20→import org.apache.linkis.common.conf.CommonVars\n    21→import org.apache.linkis.common.utils.Utils\n    22→import org.apache.linkis.engineconn.computation.executor.execute.EngineExecutionContext\n    23→import org.apache.linkis.engineconn.computation.executor.rs.RsOutputStream\n    24→import org.apache.linkis.engineconn.core.executor.ExecutorManager\n    25→import org.apache.linkis.engineconn.launch.EngineConnServer\n    26→import org.apache.linkis.engineplugin.spark.Interpreter.PythonInterpreter._\n    27→import org.apache.linkis.engineplugin.spark.common.{Kind, PySpark}\n    28→import org.apache.linkis.engineplugin.spark.config.SparkConfiguration\n    29→import org.apache.linkis.engineplugin.spark.entity.SparkEngineSession\n    30→import org.apache.linkis.engineplugin.spark.errorcode.SparkErrorCodeSummary._\n    31→import org.apache.linkis.engineplugin.spark.exception.ExecuteError\n    32→import org.apache.linkis.engineplugin.spark.imexport.CsvRelation\n    33→import org.apache.linkis.engineplugin.spark.utils.EngineUtils\n    34→import org.apache.linkis.governance.common.paser.PythonCodeParser\n    35→import org.apache.linkis.governance.common.utils.GovernanceUtils\n    36→import org.apache.linkis.manager.engineplugin.common.conf.EngineConnPluginConf.SPARK_PYTHON_VERSION_KEY\n    37→import org.apache.linkis.scheduler.executer.{ExecuteResponse, SuccessExecuteResponse}\n    38→import org.apache.linkis.storage.resultset.ResultSetWriter\n    39→\n    40→import org.apache.commons.exec.CommandLine\n    41→import org.apache.commons.io.IOUtils\n    42→import org.apache.commons.lang3.StringUtils\n    43→import org.apache.spark.SparkConf\n    44→import org.apache.spark.api.java.JavaSparkContext\n    45→import org.apache.spark.sql.{DataFrame, SparkSession}\n    46→import org.apache.spark.sql.execution.datasources.csv.UDF\n    47→\n    48→import java.io._\n    49→import java.net.InetAddress\n    50→import java.security.SecureRandom\n    51→import java.util\n    52→\n    53→import scala.collection.JavaConverters._\n    54→import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future, Promise}\n    55→import scala.concurrent.duration.Duration\n    56→\n    57→import py4j.GatewayServer\n    58→import py4j.GatewayServer.GatewayServerBuilder\n    59→\n    60→/**\n    61→ */\n    62→class SparkPythonExecutor(val sparkEngineSession: SparkEngineSession, val id: Int)\n    63→    extends SparkEngineConnExecutor(sparkEngineSession.sparkContext, id) {\n    64→\n    65→  private var gatewayServer: GatewayServer = _\n    66→  private var process: Process = _\n    67→  private var pid: Option[String] = None\n    68→  private var jobGroup: String = _\n    69→  private val queryLock = new Array[Byte](0)\n    70→  private var code: String = _\n    71→  private var pythonScriptInitialized = false\n    72→  private[this] var promise: Promise[String] = _\n    73→  private implicit val executor: ExecutionContextExecutor = ExecutionContext.global\n    74→\n    75→  implicit var sparkSession: SparkSession = sparkEngineSession.sparkSession\n    76→  private[spark] var engineExecutionContext: EngineExecutionContext = _\n    77→  private val engineCreationContext = EngineConnServer.getEngineCreationContext\n    78→  private val lineOutputStream = new RsOutputStream\n    79→  val sqlContext = sparkEngineSession.sqlContext\n    80→  val SUCCESS = "success"\n    81→  private lazy val py4jToken: String = SecureRandom.getInstance("SHA1PRNG").nextInt(100000).toString\n    82→\n    83→  private lazy val gwBuilder: GatewayServerBuilder = {\n    84→    val builder = new GatewayServerBuilder()\n    85→      .javaPort(0)\n    86→      .callbackClient(0, InetAddress.getByName(GatewayServer.DEFAULT_ADDRESS))\n    87→      .connectTimeout(GatewayServer.DEFAULT_CONNECT_TIMEOUT)\n    88→      .readTimeout(GatewayServer.DEFAULT_READ_TIMEOUT)\n    89→      .customCommands(null)\n    90→\n    91→    try builder.authToken(py4jToken)\n    92→    catch {\n    93→      case err: Throwable => builder\n    94→    }\n    95→  }\n    96→\n    97→  def getSparkConf: SparkConf = sc.getConf\n    98→\n    99→  def getJavaSparkContext: JavaSparkContext = new JavaSparkContext(sc)\n   100→\n   101→  def getSparkSession: Object = if (sparkSession != null) sparkSession\n   102→  else () => throw new IllegalAccessException("not supported keyword spark in spark1.x versions")\n   103→\n   104→  override def init(): Unit = {\n   105→    setCodeParser(new PythonCodeParser)\n   106→    super.init()\n   107→    logger.info("spark python executor start")\n   108→  }\n   109→\n   110→  override def killTask(taskID: String): Unit = {\n   111→    logger.info(s"Start to kill python task $taskID")\n   112→    super.killTask(taskID)\n   113→    logger.info(s"To close python cli task $taskID")\n   114→    Utils.tryAndError(close)\n   115→  }\n   116→\n   117→  override def close: Unit = {\n   118→\n   119→    logger.info(s"To remove pyspark executor")\n   120→    Utils.tryAndError(\n   121→      ExecutorManager.getInstance.removeExecutor(getExecutorLabels().asScala.toArray)\n   122→    )\n   123→    logger.info(s"Finished remove pyspark executor")\n   124→\n   125→    logger.info("To kill pyspark process")\n   126→    if (process != null) {\n   127→      if (gatewayServer != null) {\n   128→        Utils.tryAndError(gatewayServer.shutdown())\n   129→        gatewayServer = null\n   130→      }\n   131→      IOUtils.closeQuietly(lineOutputStream)\n   132→      Utils.tryAndErrorMsg {\n   133→        // invoke kill process method  to kill all tree process\n   134→        pid.foreach(p => {\n   135→          logger.info(s"Try to kill pyspark process with: [kill -15 ${p}]")\n   136→          GovernanceUtils.killProcess(String.valueOf(p), s"kill pyspark process,pid: $pid", false)\n   137→        })\n   138→\n   139→        Utils.tryQuietly(process.destroy())\n   140→        process = null\n   141→        logger.info("Finished kill pyspark process")\n   142→      }("process close failed")\n   143→    }\n   144→\n   145→    logger.info("python executor finished to close")\n   146→  }\n   147→\n   148→  override def getKind: Kind = PySpark()\n   149→\n   150→  private def initGateway = {\n   151→    //  If the python version set by the user is obtained from the front end as python3, the environment variable of python3 is taken; otherwise, the default is python2\n   152→    logger.info(\n   153→      s"spark.python.version => ${engineCreationContext.getOptions.get(SPARK_PYTHON_VERSION_KEY)}"\n   154→    )\n   155→    val userDefinePythonVersion = engineCreationContext.getOptions\n   156→      .getOrDefault(SPARK_PYTHON_VERSION_KEY, "python")\n   157→      .toString\n   158→      .toLowerCase()\n   159→    val sparkPythonVersion =\n   160→      if (\n   161→          StringUtils\n   162→            .isNotBlank(userDefinePythonVersion) && userDefinePythonVersion.equals("python3")\n   163→      ) {\n   164→        SparkConfiguration.PYSPARK_PYTHON3_PATH.getValue\n   165→      } else {\n   166→        userDefinePythonVersion\n   167→      }\n   168→    val pythonExec = CommonVars("PYSPARK_DRIVER_PYTHON", sparkPythonVersion).getValue\n   169→\n   170→    val pythonScriptPath = CommonVars("python.script.path", "python/mix_pyspark.py").getValue\n   171→\n   172→    val port: Int = EngineUtils.findAvailPort\n   173→    gatewayServer = gwBuilder.entryPoint(this).javaPort(port).build()\n   174→    gatewayServer.start()\n   175→\n   176→    logger.info(\n   177→      "Pyspark process file path is: " + getClass.getClassLoader\n   178→        .getResource(pythonScriptPath)\n   179→        .toURI\n   180→    )\n   181→    val pythonClasspath = new StringBuilder(pythonPath)\n   182→\n   183→    val files = sc.getConf.get("spark.files", "")\n   184→    logger.info(s"output spark files ${files}")\n   185→    if (StringUtils.isNotEmpty(files)) {\n   186→      pythonClasspath ++= File.pathSeparator ++= files\n   187→        .split(",")\n   188→        .filter(_.endsWith(".zip"))\n   189→        .mkString(File.pathSeparator)\n   190→    }\n   191→    // extra python package\n   192→    val pyFiles = sc.getConf.get("spark.submit.pyFiles", "")\n   193→    logger.info(s"spark.submit.pyFiles => ${pyFiles}")\n   194→    // add class path zip\n   195→    val classPath = CommonVars("java.class.path", "").getValue\n   196→    classPath\n   197→      .split(";")\n   198→      .filter(_.endsWith(".zip"))\n   199→      .foreach(pythonClasspath ++= File.pathSeparator ++= _)\n   200→\n   201→    val cmd = CommandLine.parse(pythonExec)\n   202→    if (SparkConfiguration.SPARK_PYTHON_TEST_MODE_ENABLE.getValue) {\n   203→      val path = SparkConfiguration.SPARK_PYTHON_TEST_MODE_MIX__PYSHELL_PATH.getValue\n   204→      logger.info(\n   205→        s"${SparkConfiguration.SPARK_PYTHON_TEST_MODE_ENABLE.key} => true, will use ${SparkConfiguration.SPARK_PYTHON_TEST_MODE_MIX__PYSHELL_PATH.key}:${path}"\n   206→      )\n   207→      cmd.addArgument(path, false)\n   208→    } else {\n   209→      cmd.addArgument(createFakeShell(pythonScriptPath).getAbsolutePath, false)\n   210→    }\n   211→\n   212→    cmd.addArgument(port.toString, false)\n   213→    cmd.addArgument(EngineUtils.sparkSubmitVersion().replaceAll("\\\\.", ""), false)\n   214→    cmd.addArgument(py4jToken, false)\n   215→    cmd.addArgument(pythonClasspath.toString(), false)\n   216→\n   217→    // add java process pid\n   218→    // cmd.addArgument(Utils.getProcessId(), false)\n   219→\n   220→    cmd.addArgument(pyFiles, false)\n   221→\n   222→    val builder = new ProcessBuilder(cmd.toStrings.toSeq.toList.asJava)\n   223→\n   224→    val env = builder.environment()\n   225→    if (StringUtils.isBlank(sc.getConf.get("spark.pyspark.python", ""))) {\n   226→      logger.info("spark.pyspark.python is null")\n   227→      if (userDefinePythonVersion.equals("python3")) {\n   228→        logger.info(s"userDefinePythonVersion is $pythonExec will be set to PYSPARK_PYTHON")\n   229→        env.put("PYSPARK_PYTHON", pythonExec)\n   230→      }\n   231→    } else {\n   232→      val executorPython = sc.getConf.get("spark.pyspark.python")\n   233→      logger.info(s"set PYSPARK_PYTHON spark.pyspark.python is $executorPython")\n   234→      env.put("PYSPARK_PYTHON", executorPython)\n   235→    }\n   236→    env.put("PYTHONPATH", pythonClasspath.toString())\n   237→    env.put("PYTHONUNBUFFERED", "YES")\n   238→    env.put("PYSPARK_GATEWAY_PORT", "" + port)\n   239→    env.put("SPARK_HOME", SparkConfiguration.SPARK_HOME.getValue)\n   240→    //    builder.redirectError(Redirect.INHERIT)\n   241→    logger.info("pyspark builder command:" + builder.command().asScala.mkString(" "))\n   242→    builder.redirectErrorStream(true)\n   243→    builder.redirectInput(ProcessBuilder.Redirect.PIPE)\n   244→    process = builder.start()\n   245→    // add hook to shutdown python\n   246→    Utils.addShutdownHook {\n   247→      close\n   248→//      Utils.tryAndError(pid.foreach(p => {\n   249→//        logger.info(s"try to kill Pyspark process with: [kill -9 ${p}]")\n   250→//        Utils.exec(Array("kill", "-9", p), 3000L)\n   251→//      }))\n   252→    }\n   253→\n   254→    Future {\n   255→      val exitCode = process.waitFor()\n   256→      pythonScriptInitialized = false\n   257→      logger.info("Pyspark process has stopped with exit code " + exitCode)\n   258→      //      close\n   259→      Utils.tryFinally({\n   260→        if (promise != null && !promise.isCompleted) {\n   261→          promise.failure(ExecuteError(PYSPARK_STOPPED.getErrorCode, PYSPARK_STOPPED.getErrorDesc))\n   262→        }\n   263→      }) {\n   264→        close\n   265→      }\n   266→    }\n   267→    // Wait up to 30 seconds（最多等待30秒）\n   268→    Utils.waitUntil(\n   269→      () => pythonScriptInitialized,\n   270→      SparkConfiguration.SPARK_LANGUAGE_REPL_INIT_TIME.getValue.toDuration\n   271→    )\n   272→  }\n   273→\n   274→  override protected def runCode(\n   275→      sparkEngineExecutor: SparkEngineConnExecutor,\n   276→      code: String,\n   277→      engineExecutionContext: EngineExecutionContext,\n   278→      jobGroup: String\n   279→  ): ExecuteResponse = {\n   280→    if (engineExecutionContext != this.engineExecutionContext) {\n   281→      this.engineExecutionContext = engineExecutionContext\n   282→      lineOutputStream.reset(engineExecutionContext)\n   283→      lineOutputStream.ready()\n   284→      //      info("Spark scala executor reset new engineExecutorContext!")\n   285→    }\n   286→    lazyInitGateway()\n   287→    this.jobGroup = jobGroup\n   288→    executeLine(code)\n   289→  }\n   290→\n   291→  def lazyInitGateway(): Unit = {\n   292→    if (process == null) {\n   293→      Utils.tryThrow(initGateway) { t =>\n   294→        {\n   295→          logger.error("initialize python executor failed, please ask administrator for help!", t)\n   296→          Utils.tryAndWarn(close)\n   297→          throw t\n   298→        }\n   299→      }\n   300→    }\n   301→  }\n   302→\n   303→  def executeLine(code: String): ExecuteResponse = {\n   304→    if (sc.isStopped) {\n   305→      throw new IllegalStateException("Application has been stopped, please relogin to try it.")\n   306→    }\n   307→    if (!pythonScriptInitialized) {\n   308→      throw new IllegalStateException(\n   309→        "Pyspark process cannot be initialized, please ask administrator for help."\n   310→      )\n   311→    }\n   312→    promise = Promise[String]()\n   313→    this.code = code\n   314→    engineExecutionContext.appendStdout(s"${EngineUtils.getName} >> $code")\n   315→    queryLock synchronized queryLock.notify()\n   316→    // scalastyle:off awaitresult\n   317→    Await.result(promise.future, Duration.Inf)\n   318→    lineOutputStream.flush()\n   319→    val outStr = lineOutputStream.toString()\n   320→    if (outStr.nonEmpty) {\n   321→      val output = Utils.tryQuietly(\n   322→        ResultSetWriter\n   323→          .getRecordByRes(outStr, SparkConfiguration.SPARK_CONSOLE_OUTPUT_NUM.getValue)\n   324→      )\n   325→      val res = if (output != null) output.map(x => x.toString).toList.mkString("\\n") else ""\n   326→      if (res.nonEmpty) {\n   327→        engineExecutionContext.appendStdout(s"result is $res")\n   328→      }\n   329→    }\n   330→    SuccessExecuteResponse()\n   331→  }\n   332→\n   333→  def onPythonScriptInitialized(pid: Int): Unit = {\n   334→    this.pid = Some(pid.toString)\n   335→    pythonScriptInitialized = true\n   336→    logger.info(s"Pyspark process has been initialized.pid is $pid")\n   337→  }\n   338→\n   339→  def getStatements: PythonInterpretRequest = {\n   340→    queryLock synchronized { while (code == null || !pythonScriptInitialized) queryLock.wait() }\n   341→    logger.info(\n   342→      "Prepare to deal python code, code: " + code\n   343→        .substring(0, if (code.indexOf("\\n") > 0) code.indexOf("\\n") else code.length)\n   344→    )\n   345→    //    lineOutputStream.reset(this.engineExecutorContext)\n   346→    val request = PythonInterpretRequest(code, jobGroup)\n   347→    code = null\n   348→    request\n   349→  }\n   350→\n   351→  def setStatementsFinished(out: String, error: Boolean): Any = {\n   352→    logger.info(s"A python code finished, has some errors happened?  $error.")\n   353→    Utils.tryQuietly(Thread.sleep(10))\n   354→    if (!error) {\n   355→      promise.success(SUCCESS)\n   356→    } else {\n   357→      if (promise.isCompleted) {\n   358→        logger.info("promise is completed and should start another python gateway")\n   359→        close\n   360→      } else {\n   361→        promise.failure(ExecuteError(OUT_ID.getErrorCode, out))\n   362→      }\n   363→    }\n   364→  }\n   365→\n   366→  def appendOutput(message: String): Unit = {\n   367→    if (!pythonScriptInitialized) {\n   368→      logger.info(message)\n   369→    } else {\n   370→      if (SparkConfiguration.SPARK_PYTHON_TEST_MODE_ENABLE.getValue) {\n   371→        logger.info(message)\n   372→      }\n   373→      lineOutputStream.write(message.getBytes("utf-8"))\n   374→    }\n   375→  }\n   376→\n   377→  def appendErrorOutput(message: String): Unit = {\n   378→    if (!pythonScriptInitialized) {\n   379→      logger.info(message)\n   380→    } else {\n   381→      logger.error(message)\n   382→      engineExecutionContext.appendStdout(s"errorMessage is $message")\n   383→    }\n   384→  }\n   385→\n   386→  def showDF(jobGroup: String, df: Any): Unit = {\n   387→    SQLSession.showDF(\n   388→      sc,\n   389→      jobGroup,\n   390→      df.asInstanceOf[DataFrame],\n   391→      null,\n   392→      5000,\n   393→      this.engineExecutionContext\n   394→    )\n   395→    logger.info("Pyspark showDF execute success!")\n   396→  }\n   397→\n   398→  def showAliasDF(jobGroup: String, df: Any, alias: String): Unit = {\n   399→    SQLSession.showDF(\n   400→      sc,\n   401→      jobGroup,\n   402→      df.asInstanceOf[DataFrame],\n   403→      alias,\n   404→      5000,\n   405→      this.engineExecutionContext\n   406→    )\n   407→    logger.info("Pyspark showAliasDF execute success!")\n   408→  }\n   409→\n   410→  def showHTML(jobGroup: String, htmlContent: Any): Unit = {\n   411→    SQLSession.showHTML(sc, jobGroup, htmlContent, this.engineExecutionContext)\n   412→    logger.info("Pyspark showHTML execute success!")\n   413→  }\n   414→\n   415→  def saveDFToCsv(\n   416→      df: Any,\n   417→      path: String,\n   418→      hasHeader: Boolean = true,\n   419→      isOverwrite: Boolean = false,\n   420→      option: util.Map[String, Any] = new util.HashMap()\n   421→  ): Boolean = {\n   422→    CsvRelation.saveDFToCsv(\n   423→      sparkSession,\n   424→      df.asInstanceOf[DataFrame],\n   425→      path,\n   426→      hasHeader,\n   427→      isOverwrite,\n   428→      option.asScala.toMap\n   429→    )\n   430→  }\n   431→\n   432→  def listUDFs(): Unit = UDF.listUDFs\n   433→\n   434→  def existsUDF(name: String): Boolean = UDF.existsUDF(name)\n   435→\n   436→  override protected def getExecutorIdPreFix: String = "SparkPythonExecutor_"\n   437→\n   438→  def printLog(log: Any): Unit = {\n   439→    logger.info(log.toString)\n   440→    if (engineExecutionContext != null) {\n   441→      engineExecutionContext.appendStdout(s"+++++++++++++++\\n${log.toString}\\n+++++++++++++++")\n   442→    } else {\n   443→      logger.warn("engine context is null can not send log")\n   444→    }\n   445→  }\n   446→\n   447→}\n   448→\n   449→case class PythonInterpretRequest(statements: String, jobGroup: String)\n   450→\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
[思考] 找到了文件，现在读取它来查看 appendErrorOutput 方法的实现。...


## 交互记录 (2026-03-30T03:54:42.360Z)

**用户**：
[{'type': 'text', 'text': '这个engineExecutionContext.appendStdout 是输出到哪个日志'}]

**AI**：
[思考] 用户让我分析 `appendErrorOutput` 方法。让我仔细看一下这个方法：

```scala
def appendErrorOutput(message: String): Unit = {
  if (!pythonScriptInitialized) {
    logger.info(message)
  } else {
    logger.error(message)
    engineExecutionContext.appendStdout(s"errorMessage is $message")
  }
}
```

这个方法在 [SparkPythonExecutor.scala:377-384](linkis-engineconn-plugins\spark\src\main\scala\org\apache\linkis\engineplugin\spark\executor\SparkPythonExecutor.scala#L377-L384)。

让我分析一下这个方法的设计意图、问题和改进建议。...


## 交互记录 (2026-03-30T03:54:48.996Z)

**用户**：
[{'tool_use_id': 'call_ac3f528d2cc445f2ac188527', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
## 方法分析：`SparkPythonExecutor#appendErrorOutput`

### 代码位置
[SparkPythonExecutor.scala:377-384](linkis-engineconn-plugins/spark/src/main/scala/org/apache/linkis/engineplugin/spark/executor/SparkPythonExecutor.scala#L377-L384)

### 方法实现

```scala
def appendErrorOutput(message: String): Unit = {
  if (!pythonScriptInitialized) {
    logger.info(message)
  } else {
    logger.error(message)
    engineExecutionContext.appendStdout(s"errorMessage is $message")
  }
}
```

### 功能说明

此方法用于处理 Python 执行过程中的错误输出：

| 状态 | 行为 |
|------|------|
| **未初始化** (`!pythonScriptInitialized`) | 作为 INFO 级别日志输出 |
| **已初始化** | 作为 ERROR 日志 + 追加到标准输出流 |

### 设计问题分析

1. **日志级别不一致** - 初始化阶段错误信息用 `info` 级别，可能丢失重要错误

2. **输出流使用不当** - 错误信息调用了 `appendStdout`，应该用 `appendStderr` 或专门的错误处理方法

3. **对比正常输出方法** - [appendOutput](linkis-engineconn-plugins/spark/src/main/scala/org/apache/linkis/engineplugin/spark/executor/SparkPythonExecutor.scala#L366-L375) 方法会写入 `lineOutputStream`，而错误方法只写日志

### 建议改进方向

- 未初始化时也使用 `logger.warn` 或 `logger.error`
- 使用 `appendStderr` 替代 `appendStdout`
- 考虑是否需要将错误也写入 `lineOutputStream`

需要我帮你修改这个方法吗？...


## 交互记录 (2026-03-30T03:54:53.706Z)

**用户**：
[{'tool_use_id': 'call_63589cd1bcf64ecf9717e9a4', 'type': 'tool_result', 'content': 'linkis-engineconn-plugins\\spark\\target\\classes\\org\\apache\\linkis\\engineplugin\\spark\\executor\\EngineExecutionContextFactory.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\src\\main\\scala\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext.scala\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$appendStdout$1.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$close$1.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$close$2.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$org$apache$linkis$engineconn$computation$executor$execute$EngineExecutionContext$$sendResultSet$1.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$pushProgress$1.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$sendResultSet$1.class\nlinkis-computation-governance\\linkis-engineconn\\linkis-computation-engineconn\\target\\classes\\org\\apache\\linkis\\engineconn\\computation\\executor\\execute\\EngineExecutionContext$$anonfun$sendResultSet$2.class'}]

**AI**：
[思考] 用户问的是 `engineExecutionContext.appendStdout` 输出到哪个日志/文件。我需要找到 `EngineExecutionContext` 的实现来了解 `appendStdout` 方法的具体行为。

让我搜索这个类的定义。...


## 交互记录 (2026-03-30T03:54:55.373Z)

**用户**：
[{'tool_use_id': 'call_a9b81e3c5f344b70811d752f', 'type': 'tool_result', 'content': '     1→/*\n     2→ * Licensed to the Apache Software Foundation (ASF) under one or more\n     3→ * contributor license agreements.  See the NOTICE file distributed with\n     4→ * this work for additional information regarding copyright ownership.\n     5→ * The ASF licenses this file to You under the Apache License, Version 2.0\n     6→ * (the "License"); you may not use this file except in compliance with\n     7→ * the License.  You may obtain a copy of the License at\n     8→ *\n     9→ *    http://www.apache.org/licenses/LICENSE-2.0\n    10→ *\n    11→ * Unless required by applicable law or agreed to in writing, software\n    12→ * distributed under the License is distributed on an "AS IS" BASIS,\n    13→ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n    14→ * See the License for the specific language governing permissions and\n    15→ * limitations under the License.\n    16→ */\n    17→\n    18→package org.apache.linkis.engineconn.computation.executor.execute\n    19→\n    20→import org.apache.linkis.common.io.{FsPath, MetaData, Record}\n    21→import org.apache.linkis.common.io.resultset.{ResultSet, ResultSetWriter}\n    22→import org.apache.linkis.common.utils.{Logging, Utils}\n    23→import org.apache.linkis.cs.client.utils.ContextServiceUtils\n    24→import org.apache.linkis.engineconn.acessible.executor.conf.AccessibleExecutorConfiguration\n    25→import org.apache.linkis.engineconn.acessible.executor.listener.event.{\n    26→  TaskLogUpdateEvent,\n    27→  TaskProgressUpdateEvent,\n    28→  TaskResultCreateEvent,\n    29→  TaskResultSizeCreatedEvent\n    30→}\n    31→import org.apache.linkis.engineconn.acessible.executor.log.LogHelper\n    32→import org.apache.linkis.engineconn.computation.executor.conf.ComputationExecutorConf\n    33→import org.apache.linkis.engineconn.computation.executor.cs.CSTableResultSetWriter\n    34→import org.apache.linkis.engineconn.core.EngineConnObject\n    35→import org.apache.linkis.engineconn.executor.ExecutorExecutionContext\n    36→import org.apache.linkis.engineconn.executor.entity.Executor\n    37→import org.apache.linkis.engineconn.executor.listener.{\n    38→  EngineConnAsyncListenerBus,\n    39→  EngineConnSyncListenerBus,\n    40→  ExecutorListenerBusContext\n    41→}\n    42→import org.apache.linkis.governance.common.exception.engineconn.EngineConnExecutorErrorException\n    43→import org.apache.linkis.protocol.engine.JobProgressInfo\n    44→import org.apache.linkis.scheduler.executer.{AliasOutputExecuteResponse, OutputExecuteResponse}\n    45→import org.apache.linkis.storage.{LineMetaData, LineRecord}\n    46→import org.apache.linkis.storage.resultset.{ResultSetFactory, ResultSetWriter}\n    47→import org.apache.linkis.storage.resultset.table.TableResultSet\n    48→\n    49→import org.apache.commons.io.IOUtils\n    50→import org.apache.commons.lang3.StringUtils\n    51→\n    52→import java.io.File\n    53→import java.util\n    54→\n    55→class EngineExecutionContext(executor: ComputationExecutor, executorUser: String = Utils.getJvmUser)\n    56→    extends ExecutorExecutionContext\n    57→    with Logging {\n    58→\n    59→  private val resultSetFactory = ResultSetFactory.getInstance\n    60→\n    61→  private var defaultResultSetWriter: ResultSetWriter[_ <: MetaData, _ <: Record] = _\n    62→\n    63→  private var resultSize = 0\n    64→\n    65→  private var enableResultsetMetaWithTableName =\n    66→    ComputationExecutorConf.HIVE_RESULTSET_USE_TABLE_NAME.getValue\n    67→\n    68→  private val properties: java.util.Map[String, Object] = new util.HashMap[String, Object]()\n    69→\n    70→  private var totalParagraph = 0\n    71→  private var currentParagraph = 0\n    72→\n    73→  def getTotalParagraph: Int = totalParagraph\n    74→\n    75→  def setTotalParagraph(totalParagraph: Int): Unit = this.totalParagraph = totalParagraph\n    76→\n    77→  def getCurrentParagraph: Int = currentParagraph\n    78→\n    79→  def setCurrentParagraph(currentParagraph: Int): Unit = this.currentParagraph = currentParagraph\n    80→\n    81→  def pushProgress(progress: Float, progressInfo: Array[JobProgressInfo]): Unit =\n    82→    if (!executor.isInternalExecute) {\n    83→      val listenerBus = getEngineSyncListenerBus\n    84→      getJobId.foreach(jId => {\n    85→        listenerBus.postToAll(TaskProgressUpdateEvent(jId, progress, progressInfo))\n    86→      })\n    87→    }\n    88→\n    89→  /**\n    90→   * Note: the writer will be closed at the end of the method\n    91→   * @param resultSetWriter\n    92→   */\n    93→  def sendResultSet(resultSetWriter: ResultSetWriter[_ <: MetaData, _ <: Record]): Unit = {\n    94→    logger.info("Start to send res to entrance")\n    95→    val fileName = new File(resultSetWriter.toFSPath.getPath).getName\n    96→    val index = if (fileName.indexOf(".") < 0) fileName.length else fileName.indexOf(".")\n    97→    val alias =\n    98→      if (fileName.startsWith("_")) fileName.substring(1, index)\n    99→      else fileName.substring(0, fileName.indexOf("_"))\n   100→    // resultSetWriter.flush()\n   101→    Utils.tryFinally(sendResultSet(resultSetWriter.toString(), alias)) {\n   102→      IOUtils.closeQuietly(resultSetWriter)\n   103→      resultSetWriters synchronized resultSetWriters -= resultSetWriter\n   104→    }\n   105→  }\n   106→\n   107→  def sendResultSet(output: String): Unit =\n   108→    sendResultSet(output, "_" + aliasNum.getAndIncrement())\n   109→\n   110→  def appendTextResultSet(output: String): Unit = {\n   111→    if (defaultResultSetWriter == null) aliasNum synchronized {\n   112→      if (defaultResultSetWriter == null) {\n   113→        defaultResultSetWriter = createDefaultResultSetWriter(ResultSetFactory.TEXT_TYPE)\n   114→        defaultResultSetWriter.addMetaData(new LineMetaData())\n   115→      }\n   116→    }\n   117→    defaultResultSetWriter.addRecord(new LineRecord(output))\n   118→  }\n   119→\n   120→  private def sendResultSet(output: String, alias: String): Unit = {\n   121→    if (StringUtils.isEmpty(output)) return\n   122→    if (resultSetFactory.isResultSetPath(output) || resultSetFactory.isResultSet(output)) {\n   123→      val listenerBus = getEngineSyncListenerBus\n   124→      getJobId.foreach(jId => {\n   125→        // TODO executor.getEngineServerContext().getEngineAsyncListenerBus().post(ResultSetCreatedEvent(jId, output, alias))\n   126→        listenerBus.postToAll(TaskResultCreateEvent(jId, output, alias))\n   127→        resultSize += 1\n   128→      })\n   129→    } else throw new EngineConnExecutorErrorException(50050, "unknown resultSet: " + output)\n   130→  }\n   131→\n   132→  def sendResultSet(outputExecuteResponse: OutputExecuteResponse): Unit =\n   133→    outputExecuteResponse match {\n   134→      case AliasOutputExecuteResponse(alias, output) => sendResultSet(output, alias)\n   135→      case output: OutputExecuteResponse =>\n   136→        sendResultSet(output.getOutput, "_" + aliasNum.getAndIncrement())\n   137→    }\n   138→\n   139→  def getProperties: java.util.Map[String, Object] = properties\n   140→\n   141→  def addProperty(key: String, value: String): Unit = properties.put(key, value)\n   142→\n   143→  override protected def getResultSetByType(\n   144→      resultSetType: String\n   145→  ): ResultSet[_ <: MetaData, _ <: Record] =\n   146→    resultSetFactory.getResultSetByType(resultSetType)\n   147→\n   148→  override protected def getDefaultResultSetByType: String = resultSetFactory.getResultSetType(0)\n   149→\n   150→  def newResultSetWriter(\n   151→      resultSet: ResultSet[_ <: MetaData, _ <: Record],\n   152→      resultSetPath: FsPath,\n   153→      alias: String\n   154→  ): ResultSetWriter[_ <: MetaData, _ <: Record] = {\n   155→    // update by 20200402\n   156→    resultSet match {\n   157→      case result: TableResultSet =>\n   158→        val contextIDStr = ContextServiceUtils.getContextIDStrByMap(getProperties)\n   159→        val nodeName = ContextServiceUtils.getNodeNameStrByMap(getProperties)\n   160→        if (StringUtils.isNotBlank(contextIDStr) && StringUtils.isNotBlank(nodeName)) {\n   161→          val csWriter = new CSTableResultSetWriter(\n   162→            result,\n   163→            ComputationExecutorConf.ENGINE_RESULT_SET_MAX_CACHE.getValue.toLong,\n   164→            resultSetPath,\n   165→            contextIDStr,\n   166→            nodeName,\n   167→            alias\n   168→          )\n   169→          csWriter.setProxyUser(executorUser)\n   170→          csWriter\n   171→        } else {\n   172→          ResultSetWriter.getResultSetWriter(\n   173→            resultSet,\n   174→            ComputationExecutorConf.ENGINE_RESULT_SET_MAX_CACHE.getValue.toLong,\n   175→            resultSetPath,\n   176→            executorUser\n   177→          )\n   178→        }\n   179→      case _ =>\n   180→        ResultSetWriter.getResultSetWriter(\n   181→          resultSet,\n   182→          ComputationExecutorConf.ENGINE_RESULT_SET_MAX_CACHE.getValue.toLong,\n   183→          resultSetPath,\n   184→          executorUser\n   185→        )\n   186→    }\n   187→    // update by 20200402 end\n   188→  }\n   189→\n   190→  def appendStdout(log: String): Unit = if (executor.isInternalExecute) {\n   191→    logger.info(log)\n   192→  } else {\n   193→    var taskLog = log\n   194→    val limitLength = ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_LENGTH.getValue\n   195→    val limitEnableObj =\n   196→      properties.get(ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_ENABLED.key)\n   197→    val limitEnable =\n   198→      if (limitEnableObj == null) {\n   199→        ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_ENABLED.getValue\n   200→      } else {\n   201→        limitEnableObj.toString.toBoolean\n   202→      }\n   203→    if (limitEnable) {\n   204→      if (log.length > limitLength) {\n   205→        taskLog = s"${log.substring(0, limitLength)}..."\n   206→        logger.info(\n   207→          "The log is too long and will be intercepted,log limit length : {}",\n   208→          limitLength\n   209→        )\n   210→      }\n   211→    }\n   212→    if (!AccessibleExecutorConfiguration.ENGINECONN_SUPPORT_PARALLELISM.getValue) {\n   213→      LogHelper.cacheLog(taskLog)\n   214→    } else {\n   215→      val listenerBus = getEngineSyncListenerBus\n   216→      getJobId.foreach(jId => listenerBus.postToAll(TaskLogUpdateEvent(jId, taskLog)))\n   217→    }\n   218→  }\n   219→\n   220→  override def close(): Unit = {\n   221→    resultSetWriters.toArray.foreach(sendResultSet)\n   222→    val listenerBus = getEngineSyncListenerBus\n   223→    getJobId.foreach(jId => {\n   224→      listenerBus.postToAll(TaskResultSizeCreatedEvent(jId, resultSize))\n   225→    })\n   226→    resultSetWriters.clear()\n   227→  }\n   228→\n   229→  private def getEngineAsyncListenerBus: EngineConnAsyncListenerBus = {\n   230→    ExecutorListenerBusContext.getExecutorListenerBusContext.getEngineConnAsyncListenerBus\n   231→  }\n   232→\n   233→  private def getEngineSyncListenerBus: EngineConnSyncListenerBus = {\n   234→    ExecutorListenerBusContext.getExecutorListenerBusContext().getEngineConnSyncListenerBus\n   235→  }\n   236→\n   237→  def getExecutor: Executor = executor\n   238→\n   239→  def getEnableResultsetMetaWithTableName: Boolean = enableResultsetMetaWithTableName\n   240→\n   241→  def setEnableResultsetMetaWithTableName(withTableName: Boolean): Unit =\n   242→    this.enableResultsetMetaWithTableName = withTableName\n   243→\n   244→}\n   245→\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
我来帮你追踪 `EngineExecutionContext.appendStdout` 的输出目标。...


## 交互记录 (2026-03-30T03:55:50.396Z)

**用户**：
[{'type': 'text', 'text': '是不是理解就是linkis的任务执行历史里面'}]

**AI**：
[思考] 没有找到 .java 文件，可能是 .scala 文件。让我搜索 scala 文件。...


## 交互记录 (2026-03-30T03:56:58.914Z)

**用户**：
[{'type': 'text', 'text': '那如果有一条错误是error 但是我觉得这个error 不应该标记error ，在不改代码的情况下，是不是可以修改log4j的配置就可以实现呢'}]

**AI**：
[思考] 找到了 scala 文件，让我读取它。...


## 交互记录 (2026-03-30T03:57:10.489Z)

**用户**：
[{'tool_use_id': 'call_8060a76ab0364758b96e7fdd', 'type': 'tool_result', 'content': 'linkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\conf\\linkis-cli\\log4j2.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\conf\\log4j2-console.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\conf\\log4j2.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\lib\\linkis-engineconn-plugins\\hive\\dist\\2.3.3\\conf\\log4j2.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\lib\\linkis-engineconn-plugins\\python\\dist\\python2\\conf\\log4j2.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\lib\\linkis-engineconn-plugins\\shell\\dist\\1\\conf\\log4j2.xml\nlinkis-dist\\target\\apache-linkis-1.18.0-wds-bin\\linkis-package\\lib\\linkis-engineconn-plugins\\spark\\dist\\2.4.3\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\flink\\target\\out\\flink\\dist\\1.12.2\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\hbase\\hbase-core\\target\\out\\hbase\\dist\\1.2.1\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\io_file\\target\\out\\io_file\\dist\\1.0\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\jdbc\\target\\out\\jdbc\\dist\\4\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\nebula\\target\\out\\nebula\\dist\\3.0.0\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\pipeline\\target\\out\\pipeline\\dist\\1\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\presto\\target\\out\\presto\\dist\\0.234\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\python\\target\\out\\python\\dist\\python2\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\seatunnel\\target\\out\\seatunnel\\dist\\2.1.2\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\shell\\target\\out\\shell\\dist\\1\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\spark\\target\\out\\spark\\dist\\3.4.4\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\sqoop\\target\\out\\sqoop\\dist\\1.4.6\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\trino\\target\\out\\trino\\dist\\371\\conf\\log4j2.xml\nlinkis-public-enhancements\\linkis-cs-server\\target\\test-classes\\log4j2.xml\nlinkis-engineconn-plugins\\trino\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\sqoop\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\seatunnel\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\presto\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\nebula\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\jdbc\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\io_file\\target\\classes\\log4j2.xml\nlinkis-engineconn-plugins\\hbase\\hbase-core\\target\\classes\\log4j2.xml\nlinkis-computation-governance\\linkis-client\\linkis-cli\\target\\test-classes\\log4j2.xml\nlinkis-computation-governance\\linkis-client\\linkis-cli\\src\\test\\resources\\log4j2.xml\nlinkis-dist\\package\\conf\\linkis-cli\\log4j2.xml\nlinkis-dist\\package\\conf\\log4j2-console.xml\nlinkis-engineconn-plugins\\hbase\\hbase-core\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\hive\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\io_file\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\jdbc\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\nebula\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\pipeline\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\presto\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\python\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\shell\\src\\main\\resources\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\spark\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\sqoop\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\trino\\src\\main\\resources\\log4j2.xml\nlinkis-public-enhancements\\linkis-cs-server\\src\\test\\resources\\log4j2.xml\nlinkis-extensions\\linkis-et-monitor\\target\\test-classes\\log4j2-console.xml\nlinkis-dist\\package\\conf\\log4j2.xml\nlinkis-engineconn-plugins\\flink\\src\\main\\resources\\log4j2.xml\nlinkis-engineconn-plugins\\seatunnel\\src\\main\\resources\\log4j2.xml\nlinkis-extensions\\linkis-et-monitor\\src\\test\\resources\\log4j2-console.xml\nlinkis-engineconn-plugins\\hive\\target\\classes\\log4j2.xml'}]

**AI**：
[思考] 现在我找到了 `appendStdout` 方法的实现。让我分析一下这个方法的输出目标：

1. 在 [EngineExecutionContext.scala:190-218](linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/execute/EngineExecutionContext.scala#L190-L218)：

```scala
def appendStdout(log: String): Unit = if (executor.isInternalExecute) {
  logger.info(log)
} else {
  var taskLog = log
  val limitLength = ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_LENGTH.getValue
  val limitEnableObj =
    properties.get(ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_ENABLED.key)
  val limitEnable =
    if (limitEnableObj == null) {
      ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_ENABLED.getValue
    } else {
      limitEnableObj.toString.toBoolean
    }
  if (limitEnable) {
    if (log.length > limitLength) {
      taskLog = s"${log.substring(0, limitLength)}..."
      logger.info(
        "The log is too long and will be intercepted,log limit length : {}",
        limitLength
      )
    }
  }
  if (!AccessibleExecutorConfiguration.ENGINECONN_SUPPORT_PARALLELISM.getValue) {
    LogHelper.cacheLog(taskLog)
  } else {
    val listenerBus = getEngineSyncListenerBus
    getJobId.foreach(jId => listenerBus.postToAll(TaskLogUpdateEvent(jId, taskLog)))
  }
}
```

关键点是内部执行模式下的日志处理。在这种模式下，日志直接输出到信息级别，没有额外的缓存或事件分发机制。系统会检查日志长度，并在超过限制时截断。日志会根据并行度配置选择不同的处理方式。

当引擎未启用并行执行时，日志将被缓存；反之，通过同步事件总线发送任务日志更新事件。这种方法确保了日志处理的灵活性和可配置性。...


## 交互记录 (2026-03-30T03:57:10.493Z)

**用户**：
[{'tool_use_id': 'call_3fb9ec68372c42be9955f453', 'type': 'tool_result', 'content': 'No files found'}]

**AI**：
## `appendStdout` 输出分析

### 代码位置
[EngineExecutionContext.scala:190-218](linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/execute/EngineExecutionContext.scala#L190-L218)

### 输出流程

```
appendStdout(message)
       │
       ▼
┌─────────────────────────────────────────┐
│ executor.isInternalExecute ?            │
└─────────────────────────────────────────┘
       │                    │
      YES                  NO
       │                    │
       ▼                    ▼
  logger.info       ┌──────────────────────┐
                    │ 日志长度截断处理      │
                    └──────────────────────┘
                              │
                              ▼
                    ┌─────────────────────────────┐
                    │ ENGINECONN_SUPPORT_         │
                    │   PARALLELISM ?             │
                    └─────────────────────────────┘
                         │                │
                        FALSE           TRUE
                         │                │
                         ▼                ▼
                  LogHelper.cacheLog   TaskLogUpdateEvent
                         │                │
                         ▼                ▼
                   本地缓存日志      发送到 ListenerBus
                                        (前端可见)
```

### 具体输出位置

| 场景 | 输出位置 |
|------|----------|
| **内部执行** | `logger.info` → 引擎日志文件 |
| **非并行模式** | `LogHelper.cacheLog` → 本地内存缓存 |
| **并行模式** | `TaskLogUpdateEvent` → 通过事件总线发送到 **Entrance**，最终展示在**前端用户界面** |

### 关键配置

```scala
// 日志长度限制配置
ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_LENGTH
ComputationExecutorConf.ENGINE_SEND_LOG_TO_ENTRANCE_LIMIT_ENABLED

// 并行执行配置
AccessibleExecutorConfiguration.ENGINECONN_SUPPORT_PARALLELISM
```

### 结论

`appendErrorOutput` 中的 `engineExecutionContext.appendStdout` **不会写入日志文件**，而是：

1. **发送到前端用户界面**（通过事件总线）
2. **用户可在作业日志中看到**（显示为 "errorMessage is xxx"）

...


## 交互记录 (2026-03-30T03:57:13.834Z)

**用户**：
[{'tool_use_id': 'call_509bb442f66b414ca423fcc0', 'type': 'tool_result', 'content': '     1→<?xml version="1.0" encoding="UTF-8"?>\n     2→<!--\n     3→  ~ Licensed to the Apache Software Foundation (ASF) under one or more\n     4→  ~ contributor license agreements.  See the NOTICE file distributed with\n     5→  ~ this work for additional information regarding copyright ownership.\n     6→  ~ The ASF licenses this file to You under the Apache License, Version 2.0\n     7→  ~ (the "License"); you may not use this file except in compliance with\n     8→  ~ the License.  You may obtain a copy of the License at\n     9→  ~ \n    10→  ~   http://www.apache.org/licenses/LICENSE-2.0\n    11→  ~ \n    12→  ~ Unless required by applicable law or agreed to in writing, software\n    13→  ~ distributed under the License is distributed on an "AS IS" BASIS,\n    14→  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n    15→  ~ See the License for the specific language governing permissions and\n    16→  ~ limitations under the License.\n    17→  -->\n    18→\n    19→<configuration status="error" monitorInterval="30">\n    20→    <appenders>\n    21→        <RollingFile name="RollingFile" append="true" fileName="${env:LOG_DIRS:-logs}/stdout"\n    22→                     filePattern="${env:LOG_DIRS}/$${date:yyyy-MM}/linkis-log-%d{yyyy-MM-dd-hh}-%i.log">\n    23→            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%-40t] %c{1.} (%L) [%M] [JobId-%X{jobId}] - %msg%xEx%n"/>\n    24→            <Policies>\n    25→                <SizeBasedTriggeringPolicy size="100MB"/>\n    26→            </Policies>\n    27→            <DefaultRolloverStrategy max="10"/>\n    28→        </RollingFile>\n    29→\n    30→        <File name="YarnAppIdOutputFile" append="true" fileName="${env:LOG_DIRS:-logs}/yarnApp">\n    31→            <RegexFilter regex=".* application .*" onMatch="ACCEPT" onMismatch="DENY"/>\n    32→            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%-40t] %c{1.} (%L) [%M] [JobId-%X{jobId}] - %msg%xEx%n"/>\n    33→        </File>\n    34→        <File name="UDFOutputFile" append="true" fileName="${env:LOG_DIRS:-logs}/udfLog">\n    35→            <RegexFilter regex=".*SpringBoot.*" onMatch="DENY" onMismatch="NEUTRAL"/>\n    36→            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%-40t] %c{1.} (%L) [%M] [JobId-%X{jobId}] - %msg%xEx%n"/>\n    37→        </File>\n    38→        <Send name="Send" >\n    39→            <Filters>\n    40→                <ThresholdFilter level="WARN" onMatch="ACCEPT" onMismatch="DENY" />\n    41→            </Filters>\n    42→            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%t] %logger{36} %L %M [JobId-%X{jobId}] - %msg%xEx%n"/>\n    43→        </Send>\n    44→\n    45→        <Send name="SendPackage" >\n    46→            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%t] %logger{36} %L %M [JobId-%X{jobId}] - %msg%xEx%n"/>\n    47→        </Send>\n    48→\n    49→        <Console name="stderr" target="SYSTEM_ERR">\n    50→            <ThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY" />\n    51→            <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %class{36} %L %M [JobId-%X{jobId}] - %msg%xEx%n"/>\n    52→        </Console>\n    53→    </appenders>\n    54→    <loggers>\n    55→      <root level="INFO">\n    56→            <appender-ref ref="stderr"/>\n    57→          <appender-ref ref="RollingFile"/>\n    58→            <appender-ref ref="Send"/>\n    59→        </root>\n    60→        <logger name="org.apache.hadoop.yarn.util.RackResolver" level="warn" additivity="true">\n    61→            <appender-ref ref="Console"/>\n    62→        </logger>\n    63→        <logger name="org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter" level="error" additivity="true">\n    64→            <appender-ref ref="stderr"/>\n    65→        </logger>\n    66→        <logger name="org.apache.hadoop.hive.ql.exec.mr.ExecDriver" level="fatal" additivity="true">\n    67→            <appender-ref ref="Send"/>\n    68→        </logger>\n    69→        <logger name="org.apache.hadoop.hdfs.KeyProviderCache" level="fatal" additivity="true">\n    70→            <appender-ref ref="Send"/>\n    71→        </logger>\n    72→        <logger name="org.spark_project.jetty" level="ERROR" additivity="true">\n    73→            <appender-ref ref="Send"/>\n    74→        </logger>\n    75→        <logger name="org.eclipse.jetty" level="ERROR" additivity="true">\n    76→            <appender-ref ref="Send"/>\n    77→        </logger>\n    78→        <logger name="org.springframework" level="ERROR" additivity="true">\n    79→            <appender-ref ref="Send"/>\n    80→        </logger>\n    81→        <logger name="org.reflections.Reflections" level="ERROR" additivity="true">\n    82→            <appender-ref ref="Send"/>\n    83→        </logger>\n    84→        <logger name="org.apache.hadoop.ipc.Client" level="ERROR" additivity="true">\n    85→            <appender-ref ref="Send"/>\n    86→        </logger>\n    87→        <logger name="org.apache.spark.deploy.yarn.Client" level="INFO" additivity="true">\n    88→            <appender-ref ref="YarnAppIdOutputFile"/>\n    89→        </logger>\n    90→        <logger name="org.apache.linkis.engineconn.computation.executor.hook" level="INFO" additivity="true">\n    91→            <appender-ref ref="UDFOutputFile"/>\n    92→        </logger>\n    93→    </loggers>\n    94→</configuration>\n    95→\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
[思考] 用户问的是 `appendStdout` 的输出是不是会显示在 Linkis 的任务执行历史里面。让我确认一下这个理解是否正确。

根据我之前的分析：
1. `appendStdout` 会通过 `TaskLogUpdateEvent` 事件发送到 ListenerBus
2. 这个事件会被传递到 Entrance 服务
3. 最终会展示在前端用户界面上

在 Linkis 的架构中：
- Entrance 服务负责任务的入口和日志收集
- 任务执行历史的日志就是通过这种方式传递和存储的
- 用户在 Linkis 的 Web UI 上查看任务历史时，看到的日志就是通过这个机制传递的

所以用户的理解是正确的 —— 这些日志会显示在 Linkis 的任务执行历史（JobHistory）中，用户可以通过 Web UI 或 API 查看任务日志时看到这些信息。

让我给用户一个确认的答复。...


## 交互记录 (2026-03-30T03:58:15.209Z)

**用户**：
[{'type': 'text', 'text': 'spark引擎的，但是只有个别的场景，其他的我还是想保持原来的样子'}]

**AI**：
是的，你的理解是正确的！

## 输出目标确认

```
appendErrorOutput(message)
        │
        ▼
engineExecutionContext.appendStdout("errorMessage is xxx")
        │
        ▼
TaskLogUpdateEvent(jId, taskLog)
        │
        ▼
   ListenerBus 事件总线
        │
        ▼
    Entrance 服务
        │
        ▼
   任务执行历史
```

### 用户可见位置

| 位置 | 说明 |
|------|------|
| **Linkis Web UI** | 任务详情 → 日志标签页 |
| **Client 调用** | 通过 `logPath` 获取的日志内容 |
| **JobHistory 服务** | 历史任务日志查询 |

### 所以说

- `appendErrorOutput` 中的错误信息会**实时推送到前端**
- 用户在执行任务时能**立即看到** "errorMessage is xxx"
- 同时这些日志也会**持久化存储**，在任务执行历史中可查

这就是为什么 `SparkPythonExecutor` 用 `appendStdout` 来输出错误 —— 确保用户能在 Linkis 界面上看到 Python 执行的错误信息。...

