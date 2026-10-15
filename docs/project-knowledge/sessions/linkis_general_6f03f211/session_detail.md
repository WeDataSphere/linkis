
## 交互记录 (2026-07-14T10:15:45.871Z)

**用户**：
[{'type': 'text', 'text': '分析下全局变量的使用'}]

**AI**：
[思考] 用户让我分析"全局变量的使用"。这个请求比较模糊。让我先理解一下上下文。

当前 git 状态显示有几个修改的文件：
- `linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala`
- `linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala`
- `linkis-dist/package/conf/linkis-cg-entrance.properties`

还有一些新文件，看起来是关于 spark3-coercion-creator 功能的设计文档、需求文档、feature 文件，以及一个测试目录。

用户说"分析下全局变量的使用"。这里的"全局变量"可能指：
1. 当前正在开发的 spark3-coercion-creator 功能中涉及的全局变量（配置项）
2. 整个 Linkis 项目中全局变量（CommonVars/Configuration 配置项）的使用模式
3. 当前修改的文件中新增的全局变量

考虑到当前的 git 状态和上下文（正在开发 spark3-coercion-creator 功能），用户可能是想让我分析当前改动中涉及的全局变量（配置开关）的使用情况，特别是 EntranceConfiguration.scala 中新增的配置项，以及它们在 CommonEntranceParser.scala 中的使用是否合理。

不过，"全局变量"也可能字面指 Scala 的 `object`/`val` 这类全局常量。

我应该先看看当前改动的文件，理解上下文，然后再判断用户到底想分析什么。但为了避免猜错，我可以先快速看一下 git diff 来了解当前改动。

让我先并行地查看当前改动的 diff 和相关文件。这样我能更好地理解"全局变量"在这个语境下指什么。

实际上，让我先用 git diff 看看当前改了什么。考虑到 CLAUDE.md 中重点提到了 CommonVars 配置开关，而当前改动的文件正是 EntranceConfiguration.scala（配置类）和 CommonEntranceParser.scala（解析器），我推测用户想知道：本次改动新增/使用了哪些全局配置变量，使用是否规范（命名、默认值、开关模式、降级等）。

让我先看一下 diff。我会用 Bash 跑 git diff，同时读取相关的设计文档了解背景。

不，先聚焦。先看 git diff，这是最快理解上下文的方式。我会同时：
1. git diff 看当前改动
2. 读 design 文档了解 spark3-coercion-creator 是什么

但这些文档名是中文乱码转义的，路径里有特殊字符。让我先看 diff。

我先并行执行 git diff（staged + unstaged）和列出相关文件。...


## 交互记录 (2026-07-14T10:16:22.170Z)

**用户**：
[{'tool_use_id': 'call_f58ffce074be4ee8b5e3bfb8', 'type': 'tool_result', 'content': 'diff --git a/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala b/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala\nindex e1a13837e..238db292d 100644\n--- a/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala\n+++ b/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala\n@@ -349,6 +349,9 @@ object EntranceConfiguration {\n   val SPARK3_VERSION_COERCION_DEPARTMENT: String =\n     CommonVars[String]("spark.version.coercion.department.id", "").getHotValue()\n \n+  val SPARK3_VERSION_COERCION_CREATORS: String =\n+    CommonVars[String]("spark.version.coercion.creators", "").getHotValue()\n+\n   val SPARK3_VERSION_COERCION_SWITCH: Boolean =\n     CommonVars[Boolean]("spark.version.coercion.switch", false).getValue\n \ndiff --git a/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala b/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala\nindex 5cd29c275..a5fe85e2c 100644\n--- a/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala\n+++ b/linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala\n@@ -21,6 +21,7 @@ import org.apache.linkis.common.conf.Configuration\n import org.apache.linkis.common.utils.{Logging, Utils}\n import org.apache.linkis.entrance.conf.EntranceConfiguration\n import org.apache.linkis.entrance.conf.EntranceConfiguration.{\n+  SPARK3_VERSION_COERCION_CREATORS,\n   SPARK3_VERSION_COERCION_DEPARTMENT,\n   SPARK3_VERSION_COERCION_SWITCH,\n   SPARK3_VERSION_COERCION_USERS\n@@ -357,7 +358,7 @@ class CommonEntranceParser(val persistenceManager: PersistenceManager)\n       executeUser: String,\n       submitUser: String\n   ): util.HashMap[String, Label[_]] = {\n-    // 个人>部门\n+    // 个人>部门>应用(creator)\n     // 是否强制转换\n     if (SPARK3_VERSION_COERCION_SWITCH && (null != labels && !labels.isEmpty)) {\n       val engineTypeLabel = labels.get(LabelKeyConstant.ENGINE_TYPE_KEY)\n@@ -404,6 +405,26 @@ class CommonEntranceParser(val persistenceManager: PersistenceManager)\n             )\n             return labels\n           }\n+          // 应用级(creator)判定：优先级最低，用户级和部门级均未命中后执行\n+          val userCreatorLabel = labels\n+            .getOrDefault(LabelKeyConstant.USER_CREATOR_TYPE_KEY, null)\n+            .asInstanceOf[UserCreatorLabel]\n+          if (null != userCreatorLabel) {\n+            val creator = userCreatorLabel.getCreator\n+            if (StringUtils.isNotBlank(creator) && SPARK3_VERSION_COERCION_CREATORS.contains(creator)) {\n+              logger.info(\n+                s"Spark version will be change 3.4.4 by creator:${creator},executeUser:${executeUser} "\n+              )\n+              labels.replace(\n+                LabelKeyConstant.ENGINE_TYPE_KEY,\n+                EngineTypeLabelCreator.createEngineTypeLabel(\n+                  EngineType.SPARK.toString,\n+                  LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue\n+                )\n+              )\n+              return labels\n+            }\n+          }\n         }(s"error to Spark 3 version coercion: ${executeUser}")\n       }\n     }\ndiff --git a/linkis-dist/package/conf/linkis-cg-entrance.properties b/linkis-dist/package/conf/linkis-cg-entrance.properties\nindex e89ced215..b3a01a467 100644\n--- a/linkis-dist/package/conf/linkis-cg-entrance.properties\n+++ b/linkis-dist/package/conf/linkis-cg-entrance.properties\n@@ -33,4 +33,13 @@ spring.server.port=9104\n wds.linkis.entrance.user.creator.ip.interceptor.switch=false\n \n ## you may set service version if you want to distinguish different configuration version\n-spring.eureka.instance.metadata-map.linkis.conf.version=v1\n\\ No newline at end of file\n+spring.eureka.instance.metadata-map.linkis.conf.version=v1\n+\n+## spark3 强制切换开关（默认关闭，开启后按 用户>部门>应用(creator) 优先级强制将 Spark2 任务切换到 Spark3）\n+#spark.version.coercion.switch=false\n+## 强制切换的用户名单（逗号分隔，热加载，getHotValue）\n+#spark.version.coercion.users=\n+## 强制切换的部门ID名单（逗号分隔，热加载，getHotValue）\n+#spark.version.coercion.department.id=\n+## 强制切换的应用(creator)名单（逗号分隔，热加载，getHotValue）【新增】\n+#spark.version.coercion.creators=\n\\ No newline at end of file', 'is_error': False}]

**AI**：
我先看一下当前改动的具体内容，了解"全局变量"在这个上下文里指什么。...


## 交互记录 (2026-07-14T10:16:23.631Z)

**用户**：
[{'tool_use_id': 'call_bc7a759e318d4b7da2c1d685', 'type': 'tool_result', 'content': ' M linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala\n M linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/parser/CommonEntranceParser.scala\n M linkis-dist/package/conf/linkis-cg-entrance.properties\n?? "docs/dev-2.0.0/design/spark3-coercion-creator_\\350\\256\\276\\350\\256\\241.md"\n?? docs/dev-2.0.0/features/spark3-coercion-creator.feature\n?? "docs/dev-2.0.0/requirements/spark3-coercion-creator_\\351\\234\\200\\346\\261\\202.md"\n?? linkis-computation-governance/linkis-entrance/src/test/scala/org/apache/linkis/entrance/parser/\n?? output/\n?? project-context.json\n---DIFFSTAT---\n .../entrance/conf/EntranceConfiguration.scala      |  3 +++\n .../entrance/parser/CommonEntranceParser.scala     | 23 +++++++++++++++++++++-\n .../package/conf/linkis-cg-entrance.properties     | 11 ++++++++++-\n 3 files changed, 35 insertions(+), 2 deletions(-)', 'is_error': False}]

**AI**：
[思考] 现在我理解了上下文。用户正在开发 `spark3-coercion-creator` 功能，这是在已有的 spark3 版本强制切换功能基础上，新增了"应用(creator)级"的强制切换判定。

用户说"分析下全局变量的使用"。结合上下文，这里的"全局变量"指的就是这几个在 `EntranceConfiguration` object 中声明的配置常量（CommonVars）：

```scala
val SPARK3_VERSION_COERCION_DEPARTMENT: String = CommonVars[String]("spark.version.coercion.department.id", "").getHotValue()
val SPARK3_VERSION_COERCION_CREATORS: String = CommonVars[String]("spark.version.coercion.creators", "").getHotValue()
val SPARK3_VERSION_COERCION_SWITCH: Boolean = CommonVars[Boolean]("spark.version.coercion.switch", false).getValue
val SPARK3_VERSION_COERCION_USERS = ... (上面应该也有，用 getHotValue)
```

让我仔细分析这些"全局变量"的使用。我需要看完整的 EntranceConfiguration.scala 中这一段，以及 CommonEntranceParser.scala 中如何使用它们，才能给出准确分析。

让我读取相关代码的完整上下文。我需要看：
1. EntranceConfiguration.scala 中 SPARK3_VERSION_COERCION_* 的完整声明
2. CommonEntranceParser.scala 中完整的判定逻辑（user 级、department 级怎么处理的）

让我并行读取这两个文件的相关部分。

关键的分析点（基于我看到的 diff 和 CLAUDE.md 的规则）：

1. **`getHotValue()` 的使用方式问题**：
   - `SPARK3_VERSION_COERCION_CREATORS` 用 `getHotValue()`，但它是 `object` 中的 `val`，意味着**只在 object 初始化（类加载）时调用一次** `getHotValue()`。
   - `val` 的值在 object 初始化后就固定了，之后不会重新读取。即使 `getHotValue()` 本身支持热加载，但因为它只在 object 初始化时被调用一次，所以**实际上并不热加载**！
   - 这是一个严重的反模式 bug！CLAUDE.md §9.3 明确说了：
     > `key.getHotValue` 每次都读"热加载缓存"。但这是在 `val` 里调用的，`val` 只初始化一次。
   - 同样的问题存在于 `SPARK3_VERSION_COERCION_DEPARTMENT` 和 `SPARK3_VERSION_COERCION_USERS`（它们也用了 getHotValue）。

   等等，让我确认一下。在 Scala `object` 中：
   ```scala
   object EntranceConfiguration {
     val SPARK3_VERSION_COERCION_CREATORS: String =
       CommonVars[String]("spark.version.coercion.creators", "").getHotValue()
   }
   ```
   
   `getHotValue()` 在 object 类加载时被调用一次，返回当时热加载缓存的值，赋给 `val`。之后引用 `SPARK3_VERSION_COERCION_CREATORS` 拿到的永远是那个固定值。**热加载失效**。

   而 `SPARK3_VERSION_COERCION_SWITCH: Boolean = CommonVars[Boolean]("spark.version.coercion.switch", false).getValue` 用的是 `getValue`，同样是 object 初始化时调用一次，固定。

   所以这里有一个**关键的设计缺陷**：注释和 properties 文件里都声称"热加载，getHotValue"...


## 交互记录 (2026-07-14T10:17:10.282Z)

**用户**：
[{'tool_use_id': 'call_6520e1c723924923804f3aa6', 'type': 'tool_result', 'content': '335\t    CommonVars[Boolean]("linkis.task.retry.switch", false)\n336\t\n337\t  val TASK_RETRY_CODE_TYPE: String =\n338\t    CommonVars[String]("linkis.task.retry.code.type", "aisql,jdbc").getValue\n339\t\n340\t  val RETRY_NUM_KEY: CommonVars[Int] =\n341\t    CommonVars[Int]("linkis.task.retry.num", 1)\n342\t\n343\t  val AI_SQL_RETRY_ONCE: CommonVars[Boolean] =\n344\t    CommonVars[Boolean]("linkis.ai.sql.once.enable", true)\n345\t\n346\t  val SPARK3_VERSION_COERCION_USERS: String =\n347\t    CommonVars[String]("spark.version.coercion.users", "").getHotValue()\n348\t\n349\t  val SPARK3_VERSION_COERCION_DEPARTMENT: String =\n350\t    CommonVars[String]("spark.version.coercion.department.id", "").getHotValue()\n351\t\n352\t  val SPARK3_VERSION_COERCION_CREATORS: String =\n353\t    CommonVars[String]("spark.version.coercion.creators", "").getHotValue()\n354\t\n355\t  val SPARK3_VERSION_COERCION_SWITCH: Boolean =\n356\t    CommonVars[Boolean]("spark.version.coercion.switch", false).getValue\n357\t\n358\t  val PYTHON_SAFE_CHECK_SWITCH = CommonVars("linkis.python.safe.check.switch", false).getValue\n359\t\n360\t  val DOCTOR_URL = CommonVars("linkis.doctor.url", "").getValue\n361\t\n362\t  val DOCTOR_DYNAMIC_ENGINE_URL = CommonVars(\n363\t    "linkis.aisql.doctor.api",\n364\t    "/api/v1/external/engine/diagnose?app_id=$app_id&timestamp=$timestamp&nonce=$nonce&signature=$signature"\n365\t  ).getValue\n366\t\n367\t  val DOCTOR_ENCRYPT_SQL_URL = CommonVars(\n368\t    "linkis.encrypt.doctor.api",\n369\t    "/api/v1/external/plaintext/diagnose?app_id=$app_id&timestamp=$timestamp&nonce=$nonce&signature=$signature"'}]

**AI**：
上下文清楚了：这是 `spark3-coercion-creator` 功能——在已有的 Spark3 强制版本切换基础上，新增"应用(creator)级"判定。我需要看完整的代码上下文才能准确分析这些"全局变量"（配置常量）的用法是否正确。...


## 交互记录 (2026-07-14T10:17:10.692Z)

**用户**：
[{'tool_use_id': 'call_75ea4c7709f54124a65b7494', 'type': 'tool_result', 'content': '350\t        }\n351\t      }\n352\t    }\n353\t    labelKeyValueMap\n354\t  }\n355\t\n356\t  private def sparkVersionCoercion(\n357\t      labels: util.HashMap[String, Label[_]],\n358\t      executeUser: String,\n359\t      submitUser: String\n360\t  ): util.HashMap[String, Label[_]] = {\n361\t    // 个人>部门>应用(creator)\n362\t    // 是否强制转换\n363\t    if (SPARK3_VERSION_COERCION_SWITCH && (null != labels && !labels.isEmpty)) {\n364\t      val engineTypeLabel = labels.get(LabelKeyConstant.ENGINE_TYPE_KEY)\n365\t      val engineType = LabelUtil.getFromLabelStr(engineTypeLabel.getStringValue, "engine")\n366\t      val version = LabelUtil.getFromLabelStr(engineTypeLabel.getStringValue, "version")\n367\t      if (\n368\t          engineType.equals(EngineType.SPARK.toString) && (!version.equals(\n369\t            LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue\n370\t          ))\n371\t      ) {\n372\t        Utils.tryAndWarnMsg {\n373\t          // 判断用户是否是个人配置中的一员\n374\t          if (\n375\t              SPARK3_VERSION_COERCION_USERS.contains(executeUser) || SPARK3_VERSION_COERCION_USERS\n376\t                .contains(submitUser)\n377\t          ) {\n378\t            logger.info(\n379\t              s"Spark version will be change 3.4.4,submitUser:${submitUser},executeUser:${executeUser} "\n380\t            )\n381\t            labels.replace(\n382\t              LabelKeyConstant.ENGINE_TYPE_KEY,\n383\t              EngineTypeLabelCreator.createEngineTypeLabel(\n384\t                EngineType.SPARK.toString,\n385\t                LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue\n386\t              )\n387\t            )\n388\t            return labels\n389\t          }\n390\t          val executeUserDepartmentId = EntranceUtils.getUserDepartmentId(executeUser)\n391\t          val submitUserDepartmentId = EntranceUtils.getUserDepartmentId(submitUser)\n392\t          if (\n393\t              (StringUtils.isNotBlank(executeUserDepartmentId) && SPARK3_VERSION_COERCION_DEPARTMENT\n394\t                .contains(executeUserDepartmentId)) ||\n395\t              (StringUtils.isNotBlank(submitUserDepartmentId) && SPARK3_VERSION_COERCION_DEPARTMENT\n396\t                .contains(submitUserDepartmentId))\n397\t          ) {\n398\t            logger.info(s"Spark version will be change 3.4.4 by department:${executeUser} ")\n399\t            labels.replace(\n400\t              LabelKeyConstant.ENGINE_TYPE_KEY,\n401\t              EngineTypeLabelCreator.createEngineTypeLabel(\n402\t                EngineType.SPARK.toString,\n403\t                LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue\n404\t              )\n405\t            )\n406\t            return labels\n407\t          }\n408\t          // 应用级(creator)判定：优先级最低，用户级和部门级均未命中后执行\n409\t          val userCreatorLabel = labels\n410\t            .getOrDefault(LabelKeyConstant.USER_CREATOR_TYPE_KEY, null)\n411\t            .asInstanceOf[UserCreatorLabel]\n412\t          if (null != userCreatorLabel) {\n413\t            val creator = userCreatorLabel.getCreator\n414\t            if (StringUtils.isNotBlank(creator) && SPARK3_VERSION_COERCION_CREATORS.contains(creator)) {\n415\t              logger.info(\n416\t                s"Spark version will be change 3.4.4 by creator:${creator},executeUser:${executeUser} "\n417\t              )\n418\t              labels.replace(\n419\t                LabelKeyConstant.ENGINE_TYPE_KEY,\n420\t                EngineTypeLabelCreator.createEngineTypeLabel(\n421\t                  EngineType.SPARK.toString,\n422\t                  LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue\n423\t                )\n424\t              )\n425\t              return labels\n426\t            }\n427\t          }\n428\t        }(s"error to Spark 3 version coercion: ${executeUser}")\n429\t      }\n430\t    }\n431\t    labels;\n432\t  }\n433\t\n434\t  // todo to format code using proper way\n435\t  private def format(code: String): String = code\n436\t\n437\t}\n438\t'}]

**AI**：
[思考] 现在我有了完整的上下文。让我分析这些"全局变量"（在 `EntranceConfiguration` object 中声明的配置常量）的使用。

这是一个非常值得深挖的分析，因为这里有一个**严重的、隐蔽的 bug**。

让我系统分析：

## 全局变量清单（本次功能相关的 4 个）

在 `object EntranceConfiguration` 中：
```scala
val SPARK3_VERSION_COERCION_USERS: String =
  CommonVars[String]("spark.version.coercion.users", "").getHotValue()       // L346-347

val SPARK3_VERSION_COERCION_DEPARTMENT: String =
  CommonVars[String]("spark.version.coercion.department.id", "").getHotValue() // L349-350

val SPARK3_VERSION_COERCION_CREATORS: String =
  CommonVars[String]("spark.version.coercion.creators", "").getHotValue()     // L352-353 【新增】

val SPARK3_VERSION_COERCION_SWITCH: Boolean =
  CommonVars[Boolean]("spark.version.coercion.switch", false).getValue        // L355-356
```

## 问题 1（最严重）：getHotValue 写在 val 里 = 热加载失效

这是核心问题。需要理解 Scala `object` 的语义：
- `object EntranceConfiguration` 是单例，里面的 `val` 在**类加载（首次访问）时初始化一次**，之后值固定。
- `getHotValue()` 调用发生在 `val` 初始化时刻，返回当时热加载缓存的快照值，赋给 `val`。之后引用 `SPARK3_VERSION_COERCION_CREATORS` 拿到的是**那个固定快照**，永远不会再调用 `getHotValue()`。
- 结论：**虽然名为 "hot value"，但实际只在 EntranceConfiguration 首次被类加载时读一次**，改配置后不重启不会生效。这与 properties 文件注释里写的"热加载，getHotValue"以及功能诉求（出问题立刻关掉不重启）完全矛盾。

这点 CLAUDE.md §9.3 专门强调了：
> `key.getHotValue` 每次都读"热加载缓存"。若新增功能开关期望"出问题立刻关掉，不重启"，必须用 `getHotValue`，并在 PR 中说明。

但 CLAUDE.md 的示例代码是：
```scala
if (EntranceConfiguration.FOO_BAR_SWITCH.getValue) { ... }
```
这里 `getValue` 也是在 val 里被调用一次——同样的问题。实际上，要真正热加载，正确写法是**把 `CommonVars[T](...)` 这个对象存成 val，然后每次用的时候调用 `.getHotValue`**：

```scala
// 正确写法：val 存的是 CommonVars 句柄，不是快照值
val SPARK3_VERSION_COERCION_CREATORS: CommonVars[String] =
  CommonVars[String]("spark.version.coercion.creators", "")

// 使用处（每次调用都读热缓存）：
if (SPARK3_VERSION_COERCION_CREATORS.getHotValue.contains(creator)) { ... }
```

对比项目里其他正确用法，例如 RETRY_NUM_KEY：
```scala
val RETRY_NUM_KEY: CommonVars[Int] = CommonVars[Int]("linkis.task.retry.num", 1)  // 存句柄
```
这才是正确模式——存 `CommonVars[T]` 句柄，使用时再 `.getValue`/`.getHotValue`。

所以 USERS / DEPARTMENT / CREATORS 三个都...


## 交互记录 (2026-07-14T10:18:23.358Z)

**用户**：
[{'tool_use_id': 'call_d990897987194b4ba7428788', 'type': 'tool_result', 'content': '50:  def tryCatch[T](tryOp: => T)(catchOp: Throwable => T): T = {\n51-    try tryOp\n52-    catch {\n53-      case t: ControlThrowable => throw t\n54-      case fatal: FatalException =>\n55-        logger.error("Fatal error, system exit...", fatal)\n56-        System.exit(fatal.getErrCode)\n57-        null.asInstanceOf[T]\n58-      case e: VirtualMachineError =>\n59-        logger.error("Fatal error, system exit...", e)\n60-        System.exit(-1)\n61-        throw e\n62-      case exp\n--\n87:  def tryAndWarn[T](tryOp: => T)(implicit log: Logger): T = tryCatch(tryOp) {\n88-    case error: ErrorException =>\n89-      val errorMsg =\n90-        s"error code（错误码）: ${error.getErrCode}, Error message（错误信息）: ${error.getDesc}."\n91-      log.error(errorMsg, error)\n92-      null.asInstanceOf[T]\n93-    case warn: WarnException =>\n94-      val warnMsg =\n95-        s"Warning code（警告码）: ${warn.getErrCode}, Warning message（警告信息）: ${warn.getDesc}."\n96-      log.warn(warnMsg, warn)\n97-      null.asInstanceOf[T]\n98-    case t: Throwable =>\n99-      log.warn("", t)\n--\n103:  def tryAndWarnMsg[T](tryOp: => T)(message: String)(implicit log: Logger): T = tryCatch(tryOp) {\n104-    case error: ErrorException =>\n105-      log.warn(s"error code（错误码）: ${error.getErrCode}, Error message（错误信息）: ${error.getDesc}.")\n106-      log.warn(message, error)\n107-      null.asInstanceOf[T]\n108-    case warn: WarnException =>\n109-      log.warn(s"Warning code（警告码）: ${warn.getErrCode}, Warning message（警告信息）: ${warn.getDesc}.")\n110-      log.warn(message, warn)\n111-      null.asInstanceOf[T]\n112-    case t: Throwable =>\n113-      log.warn(message, t)\n114-      null.asInstanceOf[T]\n115-  }'}]

**AI**：
关键上下文已具备。在给结论前，我要确认两处影响判断的细节：`getHotValue` 写进 `val` 后是否真能热加载，以及 `tryAndWarnMsg` 闭包内 `return` 的行为。...


## 交互记录 (2026-07-14T10:18:25.471Z)

**用户**：
[{'tool_use_id': 'call_9de967ed92824065a97acd75', 'type': 'tool_result', 'content': 'No matches found'}]

**AI**：
[思考] 啊，我误解了用户的意思。用户说的"全局变量"不是 Scala 的全局配置常量，而是 Linkis 业务概念里的"全局变量"——也就是任务执行时的变量（variables）。这对应 TaskUtils 三套 Map 里的 **variable**（自定义变量，用于代码 `${var}` 占位符替换）。

用户问的是："GlobalVariable 在任务中怎么生效的"。也就是：Linkis 的全局变量（global variable / variable substitution）机制是怎样的，它在任务执行链路中如何被解析、替换、传递到引擎生效。

这是一个代码探索/原理讲解任务。我需要搞清楚：
1. Linkis 里"全局变量"（GlobalVariable）的定义在哪
2. 它们在任务执行链路中如何生效（哪个拦截器/解析器负责变量替换）
3. `${var}` 占位符替换的流程

让我重新搜索代码。关键词：GlobalVariable、Variable、变量替换、variable substitution、`${`。

Linkis 里常见的相关类：
- `VariableIntercepter` / `VariableInterceptor` - 变量拦截器（entrance 拦截器）
- `TaskUtils.getVariableMap` - 取 variable map
- Variable 在代码里通常是 `${var}` 语法

让我搜索这些。我应该用 Explore agent 或者直接 grep。考虑到这需要跨多文件、理解整条链路，但用户问的是原理"怎么生效的"，我应该聚焦找出核心类和流程，不必扫太多文件。

让我直接 grep 几个关键关键词，快速定位：
- "GlobalVariable"
- "VariableInterceptor" 或 "Variable"
- "${" 变量替换逻辑

让我并行搜索。

实际上 Linkis 有一个专门的变量替换机制。我回忆一下：
- `linkis-entrance` 有 `VariableInterceptor` 拦截器
- `linkis-commons/linkis-common` 里有变量相关的解析
- 代码占位符 `${var}` 替换通常在 Parser 或 Interceptor 阶段

让我搜索。我先并行 grep 多个关键词来找 GlobalVariable 和变量替换的核心实现。...


## 交互记录 (2026-07-14T10:18:30.766Z)

**用户**：
[{'type': 'text', 'text': '[Request interrupted by user]'}]

**AI**：
明白了，我问错方向了——你问的是 Linkis 业务里的"全局变量"（GlobalVariable / `${var}` 变量替换）在任务链路中怎么生效。我重新去代码里找这条链路。...


## 交互记录 (2026-07-14T10:21:44.929Z)

**用户**：
[{'type': 'text', 'text': '我说的是GlobalVariable的怎么在任务中生效的'}]

**AI**：
[思考] 找到了关键文件。现在我理解了 Linkis 的"全局变量"机制涉及两部分：

**1. 后台管理端（PES publicservice）**：变量的存储与管理
- `linkis-public-enhancements/linkis-pes-publicservice/.../variable/` - 全局变量的 CRUD（DB 存储 + RESTful + RPC）
  - `VariableRestfulApi.java` - 前端调用的 RESTful
  - `VariableServiceImpl.scala` - 服务实现
  - `VariableReceiver.scala` - RPC 接收端（其他服务通过 RPC 查询全局变量）

**2. 任务执行端（entrance）**：变量在任务提交时如何被替换进代码
- `VarSubstitutionInterceptor.scala` - 变量替换拦截器（这是核心！在 entrance 拦截器链里）
- `CustomVariableUtils.scala` - 自定义变量工具
- `ParserVarLabelInterceptor.scala` - 解析阶段的变量
- `CommonEntranceParser.scala` - parser 里也用了 variableMap
- `TaskUtils.getVariableMap` - 从任务 params 取 variable map

我需要搞清楚的核心问题："全局变量在任务中怎么生效"。这是一个端到端链路：
1. 用户在管理后台配置全局变量（按用户/全局维度）→ 存 DB
2. 任务提交到 entrance
3. VarSubstitutionInterceptor 拦截器：从 PES 拉取该用户的全局变量 + 任务的 variable map，合并
4. 对代码里的 `${var}` 占位符做替换
5. 替换后的代码进入 orchestrator → EC 执行

让我读关键文件来确认这个链路的真实实现，而不是凭记忆编造。重点读：
1. `VarSubstitutionInterceptor.scala` - 入口拦截器（最关键）
2. `CustomVariableUtils.scala` - 替换逻辑
3. `VariableReceiver.scala` + `VariableServiceImpl.scala` - RPC 查询全局变量
4. TaskUtils.getVariableMap - 确认 variable map 结构

让我并行读这几个文件。这是值得的，因为我要给用户讲清楚"怎么生效的"，必须基于真实代码。

我用 Explore agent 来并行调研整条链路可能更高效，但用户的问题比较聚焦，我自己读核心文件更可控、结论更准。让我直接读核心文件。

先并行读 4 个最关键的文件。考虑到文件可能较大，我先读 VarSubstitutionInterceptor 和 CustomVariableUtils 全文，再读 VariableReceiver 和 TaskUtils 的相关部分。...


## 交互记录 (2026-07-14T10:22:25.542Z)

**用户**：
[{'tool_use_id': 'call_3407ec3eb302401eb472bd88', 'type': 'tool_result', 'content': 'Found 8 files\nlinkis-web\\src\\apps\\linkis\\module\\globalValiable\\index.vue\nlinkis-web\\src\\apps\\linkis\\module\\globalValiable\\index.js\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\scala\\org\\apache\\linkis\\variable\\service\\VariableServiceImpl.scala\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\scala\\org\\apache\\linkis\\variable\\receiver\\VariableReceiver.scala\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\resources\\mapper\\common\\VarMapper.xml\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\java\\org\\apache\\linkis\\variable\\service\\VariableService.java\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\java\\org\\apache\\linkis\\variable\\restful\\api\\VariableRestfulApi.java\nlinkis-public-enhancements\\linkis-pes-publicservice\\src\\main\\java\\org\\apache\\linkis\\variable\\dao\\VarMapper.java'}]

**AI**：
找到了。Linkis 的"全局变量"是一条端到端链路：**PES 后台存储 → RPC 下发 → Entrance 拦截器替换 `${var}` → 代码进入引擎**。我读核心文件确认真实实现，而不是凭记忆讲。...


## 交互记录 (2026-07-14T10:22:25.653Z)

**用户**：
[{'tool_use_id': 'call_e85861685d524058a508a303', 'type': 'tool_result', 'content': 'Found 22 files\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\parser\\CommonEntranceParser.scala\n.ai\\snippets.md\nlinkis-engineconn-plugins\\seatunnel\\src\\main\\scala\\org\\apache\\linkis\\engineconnplugin\\seatunnel\\executor\\SeatunnelFlinkOnceCodeExecutor.scala\nlinkis-engineconn-plugins\\flink\\src\\main\\scala\\org\\apache\\linkis\\engineconnplugin\\flink\\executor\\FlinkCodeOnceExecutor.scala\nlinkis-engineconn-plugins\\seatunnel\\src\\main\\scala\\org\\apache\\linkis\\engineconnplugin\\seatunnel\\executor\\SeatunnelFlinkSQLOnceCodeExecutor.scala\nlinkis-computation-governance\\linkis-jdbc-driver\\src\\main\\scala\\org\\apache\\linkis\\ujes\\jdbc\\UJESSQLDriverMain.scala\nlinkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\computation\\client\\LinkisJobBuilder.scala\nlinkis-orchestrator\\linkis-orchestrator-core\\src\\main\\scala\\org\\apache\\linkis\\orchestrator\\plans\\ast\\QueryParams.scala\nlinkis-computation-governance\\linkis-jdbc-driver\\src\\main\\scala\\org\\apache\\linkis\\ujes\\jdbc\\LinkisSQLConnection.scala\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\interceptor\\impl\\VarSubstitutionInterceptor.scala\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\interceptor\\impl\\ParserVarLabelInterceptor.scala\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\interceptor\\impl\\CustomVariableUtils.scala\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\interceptor\\OnceJobInterceptor.scala\nlinkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\cs\\CSEntranceHelper.scala\nlinkis-computation-governance\\linkis-computation-governance-common\\src\\main\\scala\\org\\apache\\linkis\\governance\\common\\utils\\OnceExecutorContentUtils.scala\nlinkis-computation-governance\\linkis-computation-governance-common\\src\\main\\scala\\org\\apache\\linkis\\governance\\common\\entity\\job\\OnceExecutorContent.scala\nlinkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobSubmitAction.scala\nlinkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\ujes\\client\\request\\JobExecuteAction.scala\nlinkis-computation-governance\\linkis-client\\linkis-computation-client\\src\\main\\scala\\org\\apache\\linkis\\computation\\client\\once\\simple\\SimpleOnceJobBuilder.scala\nlinkis-computation-governance\\linkis-client\\linkis-cli\\src\\main\\java\\org\\apache\\linkis\\cli\\application\\interactor\\validate\\OnceDescValidator.java\nlinkis-computation-governance\\linkis-client\\linkis-cli\\src\\main\\java\\org\\apache\\linkis\\cli\\application\\interactor\\validate\\InteractiveDescValidator.java\nlinkis-commons\\linkis-protocol\\src\\main\\scala\\org\\apache\\linkis\\protocol\\utils\\TaskUtils.scala'}]

**AI**：
[思考] 现在我对链路有了清晰的把握。让我再确认两个细节：

1. `VariableUtils.replace` - 真正的 `${var}` 占位符替换逻辑（在 linkis-common），以及替换优先级
2. `VariableServiceImpl.queryGolbalVariable` / `queryAppVariable` - DB 查询逻辑（全局变量 vs 应用变量的区别）
3. 拦截器在拦截器链中的注册顺序（确认它在 parser 之前/之后执行）

但用户问的是"全局变量在任务中怎么生效"，核心链路已经清楚。让我读 VariableUtils.replace 确认占位符替换的语法和优先级，这是"怎么生效"的最后一块拼图。同时读一下 VariableServiceImpl 确认全局变量 vs 应用变量的查询维度。

我并行读这两个。VariableUtils 在 linkis-common。让我先找它的路径。

从 import: `org.apache.linkis.common.utils.{Logging, Utils, VariableUtils}` - 在 linkis-common 的 utils 包。

让我读 VariableUtils.scala 和 VariableServiceImpl.scala。

实际上我已经掌握了足够回答用户问题的信息。核心链路：

**生效链路（任务提交时）：**

1. **存储层**（PES publicservice）：
   - 全局变量存在 DB 表（`linkis_ps_variable_list` 之类，看 VarMapper.xml）
   - 维度：全局变量（按用户 userName）和应用变量（按 userName + creator + appName）

2. **任务提交 → Entrance 拦截器链**：
   - `VarSubstitutionInterceptor.apply()` 被调用
   - 调用 `CustomVariableUtils.replaceCustomVar()`

3. **拉取变量**（CustomVariableUtils.replaceCustomVar）：
   - 通过 RPC（`Sender.getSender(CLOUD_CONSOLE_VARIABLE_SPRING_APPLICATION_NAME)`）向 PES 发请求
   - 如果有 userCreator label → `RequestQueryAppVariable(umUser, creator, codeType)` 查应用级变量
   - 否则 → `RequestQueryGlobalVariable(umUser)` 查全局变量
   - 服务端 `VariableReceiver` 接收 → `VariableService.queryGolbalVariable / queryAppVariable` 查 DB

4. **合并变量**（优先级，代码注释已写明 1/2/3，但实际实现是 putAll 覆盖）：
   - 先放 RPC 返回的 console 变量（全局/应用变量）
   - 再 `putAll(variableMap)` —— 任务 params 里的 variable（用户提交时带的 args/自定义变量）**覆盖** console 变量
   - 再放系统变量 `user`/`execute_user`/`submit_user`（最高优先级，强制覆盖）

   注意：注释说"1. 从代码取 2. 从 args 取 3. 从 console 取"，但实际代码顺序是 console → args(variableMap) → 系统变量。`VariableUtils.replace` 才是真正按这个 variables map 替换 code 里的 `${var}`。代码内联变量（如 SQL 里 `${a=1}` 带默认值的）应该在 VariableUtils.replace 里处理。

5. **占位符替换**（VariableUtils.replace）：
   - 把 `${var}` 替换成 variables map 里的值
   - 替换后的 code 写回 `jobRequest.setExecutionCode(realCode)`

6. **后续**：替换后的代码进入 orchestrator → EC 执行

这个链路我基本清楚了。让我读 VariableUtils.replace 确认占位符语法（`${var}` / `${var=default}` / `$var...


## 交互记录 (2026-07-14T10:22:25.719Z)

**用户**：
[{'type': 'tool_result', 'content': 'Ripgrep search timed out after 20 seconds. The search may have matched files but did not complete in time. Try searching a more specific path or pattern.', 'is_error': True, 'tool_use_id': 'call_008f2a39960d4fd4b818c394'}]

**AI**：
链路主体清楚了。最后确认两块拼图：真正的 `${var}` 占位符替换实现，以及 PES 端全局/应用变量的查询维度。...


## 交互记录 (2026-07-14T10:22:43.865Z)

**用户**：
[{'tool_use_id': 'call_6bbf2380c42c4b44b0d0661b', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.interceptor.impl\n19\t\n20\timport org.apache.linkis.common.exception.ErrorException\n21\timport org.apache.linkis.common.log.LogUtils\n22\timport org.apache.linkis.common.utils.Utils\n23\timport org.apache.linkis.entrance.interceptor.EntranceInterceptor\n24\timport org.apache.linkis.entrance.interceptor.exception.VarSubstitutionException\n25\timport org.apache.linkis.governance.common.entity.job.JobRequest\n26\timport org.apache.linkis.manager.label.utils.LabelUtil\n27\timport org.apache.linkis.protocol.utils.TaskUtils\n28\timport org.apache.linkis.server.toScalaMap\n29\t\n30\timport org.apache.commons.lang3.exception.ExceptionUtils\n31\t\n32\timport java.util\n33\t\n34\t/**\n35\t * Description: For variable substitution(用于变量替换)\n36\t */\n37\tclass VarSubstitutionInterceptor extends EntranceInterceptor {\n38\t\n39\t  @throws[ErrorException]\n40\t  override def apply(jobRequest: JobRequest, logAppender: java.lang.StringBuilder): JobRequest = {\n41\t    jobRequest match {\n42\t      case jobRequest: JobRequest =>\n43\t        Utils.tryThrow {\n44\t          logAppender.append(\n45\t            LogUtils.generateInfo("Program is substituting variables for you") + "\\n"\n46\t          )\n47\t          val codeType = LabelUtil.getCodeType(jobRequest.getLabels)\n48\t          val realCode = CustomVariableUtils.replaceCustomVar(jobRequest, codeType, logAppender)\n49\t          jobRequest.setExecutionCode(realCode)\n50\t          logAppender.append(\n51\t            LogUtils.generateInfo("Variables substitution ended successfully") + "\\n"\n52\t          )\n53\t          logAppender.append(LogUtils.generateInfo("Job variables is") + "\\n")\n54\t          logAppender.append(\n55\t            "************************************Variable************************************" + "\\n"\n56\t          )\n57\t          val variableMap = TaskUtils\n58\t            .getVariableMap(jobRequest.getParams)\n59\t            .asInstanceOf[util.HashMap[String, String]]\n60\t          variableMap.foreach { case (key, value) =>\n61\t            logAppender.append(s"$key=$value\\n")\n62\t          }\n63\t          logAppender.append("\\n");\n64\t          logAppender.append(\n65\t            "************************************Variable************************************" + "\\n"\n66\t          )\n67\t          // print  code after variables substitution\n68\t          logAppender.append(\n69\t            LogUtils.generateInfo(\n70\t              "You have submitted a new job, script code (after variable substitution) is"\n71\t            ) + "\\n"\n72\t          );\n73\t          logAppender.append(\n74\t            "************************************SCRIPT CODE************************************" + "\\n"\n75\t          )\n76\t          logAppender.append(realCode);\n77\t          logAppender.append("\\n");\n78\t          logAppender.append(\n79\t            "************************************SCRIPT CODE************************************" + "\\n"\n80\t          );\n81\t\n82\t          jobRequest\n83\t        } {\n84\t          case e: VarSubstitutionException =>\n85\t            val exception = VarSubstitutionException(\n86\t              20050,\n87\t              "Variable replacement failed!(变量替换失败！)" + ExceptionUtils.getRootCauseMessage(e)\n88\t            )\n89\t            exception.initCause(e)\n90\t            exception\n91\t          case t: Throwable =>\n92\t            val exception = VarSubstitutionException(\n93\t              20050,\n94\t              "Variable replacement failed!(变量替换失败！)" + ExceptionUtils.getRootCauseMessage(t)\n95\t            )\n96\t            exception.initCause(t)\n97\t            exception\n98\t        }\n99\t      case _ => jobRequest\n100\t    }\n101\t  }\n102\t\n103\t}\n104\t'}]

**AI**：
[思考] 好的，VariableServiceImpl 的查询逻辑有个有趣的点：
- `queryGolbalVariable(userName)` → 查全局变量
- `queryAppVariable(userName, creator, appName)` → **实际也是查 `listGlobalVariable(userName)`！** 也就是说，应用变量的查询当前实现和全局变量一样，都是按 userName 查（applicationID = -1L 表示全局）。appName/creator 参数实际上没用上。这是个值得指出的实现细节（可能是历史遗留或未完成的功能）。

注意 `insertGlobalVariable` 里 `newKey.setApplicationID(-1L)` 和 `newValue.setApplicationID(-1L)`，-1 表示全局变量。所以应用级变量（applicationID != -1）目前没有写入路径，queryAppVariable 也没按 applicationID 过滤——当前所有变量都是"用户级全局变量"。

现在只剩 `VariableUtils.replace` 这个占位符替换核心没看。它在 linkis-common/utils。Glob 超时了，我直接用已知路径读。包是 `org.apache.linkis.common.utils.VariableUtils`，路径应该是 `linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/VariableUtils.scala`。

让我读它确认 `${var}` 语法和替换逻辑。这是"怎么生效"的最后一环，必须看。...


## 交互记录 (2026-07-14T10:22:44.588Z)

**用户**：
[{'tool_use_id': 'call_568b4143c968495fb5595e64', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.interceptor.impl\n19\t\n20\timport org.apache.linkis.common.conf.Configuration\n21\timport org.apache.linkis.common.log.LogUtils\n22\timport org.apache.linkis.common.utils.{Logging, Utils, VariableUtils}\n23\timport org.apache.linkis.governance.common.entity.job.JobRequest\n24\timport org.apache.linkis.manager.label.utils.LabelUtil\n25\timport org.apache.linkis.protocol.utils.TaskUtils\n26\timport org.apache.linkis.protocol.variable.{\n27\t  RequestQueryAppVariable,\n28\t  RequestQueryGlobalVariable,\n29\t  ResponseQueryVariable\n30\t}\n31\timport org.apache.linkis.rpc.Sender\n32\t\n33\timport java.util\n34\t\n35\tobject CustomVariableUtils extends Logging {\n36\t\n37\t  /**\n38\t   * replace custom variable\n39\t   *   1. Get the user-defined variable from the code and replace it 2. If 1 is not done, then get\n40\t   *      the user-defined variable from args and replace it. 3. If 2 is not done, get the\n41\t   *      user-defined variable from the console and replace it.\n42\t   * @param jobRequest\n43\t   *   : requestPersistTask\n44\t   * @return\n45\t   */\n46\t  def replaceCustomVar(\n47\t      jobRequest: JobRequest,\n48\t      runType: String,\n49\t      logAppender: java.lang.StringBuilder\n50\t  ): String = {\n51\t    val variables: util.Map[String, String] = new util.HashMap[String, String]()\n52\t    val sender =\n53\t      Sender.getSender(Configuration.CLOUD_CONSOLE_VARIABLE_SPRING_APPLICATION_NAME.getValue)\n54\t    val umUser: String = jobRequest.getExecuteUser\n55\t    val codeTypeFromLabel = LabelUtil.getCodeType(jobRequest.getLabels)\n56\t    val userCreator = LabelUtil.getUserCreator(jobRequest.getLabels)\n57\t    val creator: String = if (null != userCreator) userCreator._2 else null\n58\t    val requestProtocol = if (null != userCreator) {\n59\t      RequestQueryAppVariable(umUser, creator, codeTypeFromLabel)\n60\t    } else {\n61\t      RequestQueryGlobalVariable(umUser)\n62\t    }\n63\t    val response: ResponseQueryVariable =\n64\t      Utils.tryAndWarn(sender.ask(requestProtocol).asInstanceOf[ResponseQueryVariable])\n65\t    if (null != response) {\n66\t      val keyAndValue = response.getKeyAndValue\n67\t      variables.putAll(keyAndValue)\n68\t    }\n69\t    val variableMap = TaskUtils\n70\t      .getVariableMap(jobRequest.getParams)\n71\t      .asInstanceOf[util.HashMap[String, String]]\n72\t    variables.putAll(variableMap)\n73\t    variables.put("user", jobRequest.getExecuteUser)\n74\t    // User customization is not supported. If the user has customized it, add a warning log and replace it\n75\t    if (variables.containsKey("submit_user")) {\n76\t      logAppender.append(\n77\t        LogUtils.generateInfo(\n78\t          "submitUser variable will be replaced by system value:" + jobRequest.getSubmitUser + " -> " + variables\n79\t            .get("submit_user") + "\\n"\n80\t        )\n81\t      )\n82\t    }\n83\t    if (variables.containsKey("execute_user")) {\n84\t      logAppender.append(\n85\t        LogUtils.generateInfo(\n86\t          "executeUser variable will be replaced by system value:" + jobRequest.getExecuteUser + " -> " + variables\n87\t            .get("execute_user") + "\\n"\n88\t        )\n89\t      )\n90\t    }\n91\t    variables.put("execute_user", jobRequest.getExecuteUser)\n92\t    variables.put("submit_user", jobRequest.getSubmitUser)\n93\t    VariableUtils.replace(jobRequest.getExecutionCode, runType, variables)\n94\t  }\n95\t\n96\t}\n97\t'}]

**AI**：
[思考] 现在我有了完整的链路图景。我可以给用户一个准确、基于代码的讲解了。让我梳理"全局变量（GlobalVariable）在任务中怎么生效"的完整链路。

我已经读了核心文件，足以回答用户的问题。不需要再读更多。让我组织答案。

核心链路：

**1. 变量来源（三类，有优先级）**

在 `VariableUtils.replace(code, codeType, variables)` 里，变量名→值的最终解析顺序（putNameAndType 用的是 `!nameAndType.contains(key)` 判重，先入为主）：

- **第一优先级：代码内联变量**（`getCustomVar`）—— 从提交的 code 里用正则解析 `@set` 语法的自定义变量
  - SQL: `--@set key=value`
  - Python/Shell: `#@set key=value`
  - Scala: `//@set key=value`
  - Java: `!!@set key=value`
- **第二优先级：variables map**（外部传入）—— 包含：
  - console 全局/应用变量（RPC 从 PES 拉的）
  - 任务 params 里的 variable map（用户提交时带的自定义变量/args）
  - 系统变量 user/execute_user/submit_user
- **特殊：日期变量系统**（run_date 等）—— 由 run_date 派生一大堆 run_today/run_month_begin 等，这是 Linkis 调度场景的核心

注意：CustomVariableUtils.replaceCustomVar 里 putAll 的顺序是 console 变量先、variableMap 后（后者覆盖前者）、系统变量最后（强制覆盖）。但进入 VariableUtils.replace 后，又是 code 内联变量（getCustomVar）先放、variables map 后放，且都受 `!contains` 保护——所以**先放的不会被后放的覆盖**。

这意味着实际优先级是：**代码内联 @set > (console全局变量 与 任务variable，取决于谁先进 variables map) > 系统变量(user等被特殊处理)**。

具体在 CustomVariableUtils 里：
```
variables.putAll(keyAndValue)     // console 全局变量（先）
variables.putAll(variableMap)     // 任务 variable（后，会覆盖 console 同名）
variables.put("user", executeUser)
variables.put("execute_user", executeUser)  // 系统变量强制
variables.put("submit_user", submitUser)
```
然后 VariableUtils.replace(code, codeType, variables)：
```
putNameAndType(nameAndValue)   // code 内联 @set（最高，先放）
nameAndType("user") = ...      // user 特殊
putNameAndType(variables)      // 外部 variables map（console+task+system），受 contains 保护
```
所以 user 在 code @set 之后放，但 variables 里的 user 不会覆盖（因为 contains）。不过 execute_user/submit_user 在 variables map 里是后 putAll 的，会进 variables，但进 nameAndType 时被 contains 拦（如果 code 里没 @set execute_user，则用 variables 的值）。

**结论优先级**（高→低）：
1. 代码内联 `@set`（getCustomVar）
2. 系统注入的 `user`
3. console 全局变量 + 任务 variableMap（合并，task 覆盖 console 同名）+ 系统 execute_user/submit_user
4. 内置日期变量（run_date 及其派生，若未显式提供 run_date 则默认昨天）

**2. 生效链路（任务提交时）**

入口：`VarSubstitutio...


## 交互记录 (2026-07-14T10:22:45.102Z)

**用户**：
[{'tool_use_id': 'call_f662c56fd70d4709a3ebc108', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.variable.receiver\n19\t\n20\timport org.apache.linkis.protocol.variable.{RequestQueryAppVariable, RequestQueryGlobalVariable}\n21\timport org.apache.linkis.rpc.{Receiver, Sender}\n22\timport org.apache.linkis.variable.service.VariableService\n23\t\n24\timport scala.concurrent.duration.Duration\n25\t\n26\tclass VariableReceiver extends Receiver {\n27\t\n28\t  private var variableService: VariableService = _\n29\t\n30\t  def this(variableService: VariableService) = {\n31\t    this()\n32\t    this.variableService = variableService\n33\t  }\n34\t\n35\t  override def receive(message: Any, sender: Sender): Unit = {}\n36\t\n37\t  override def receiveAndReply(message: Any, sender: Sender): Any = message match {\n38\t    case RequestQueryGlobalVariable(userName: String) =>\n39\t      variableService.queryGolbalVariable(userName)\n40\t    case e: RequestQueryAppVariable =>\n41\t      variableService.queryAppVariable(e.userName, e.creator, e.appName)\n42\t  }\n43\t\n44\t  override def receiveAndReply(message: Any, duration: Duration, sender: Sender): Any = {}\n45\t}\n46\t'}]

**AI**：
读完整条链路了。Linkis 的"全局变量"生效机制是一条** Entrance 侧拦截器 → RPC 拉取 → 占位符替换 → 改写代码**的流水线，替换发生在任务进入引擎**之前**。下面按"链路 → 变量来源与优先级 → 占位符语法 → 关键注意点"讲清楚。

---

## 一、生效链路（任务提交时）

```
前端提交 JobRequest（含 code + params + labels）
        │
        ▼  entrance 拦截器链（顺序敏感）
VarSubstitutionInterceptor.apply()            ← 入口
        │
        ▼
CustomVariableUtils.replaceCustomVar()        ← 组装"变量名→值"表
        │
        ├─① RPC 向 PES 拉后台配置的全局/应用变量
        │     Sender.getSender(CLOUD_CONSOLE_VARIABLE...) 
        │       .ask(RequestQueryGlobalVariable(umUser))      有 userCreator label
        │       .ask(RequestQueryAppVariable(umUser,creator,codeType))
        │     → PES: VariableReceiver → VariableService 
        │       → VarMapper.listGlobalVariable(userName)  查 DB
        │
        ├─② 合并 TaskUtils.getVariableMap(params)（任务自带的 variable）
        ├─③ 注入系统变量 user / execute_user / submit_user
        │
        ▼
VariableUtils.replace(code, codeType, variables)   ← 真正的 ${var} 替换
        │  正则匹配 ${...} → 查表取值 → 支持四则运算 → 拼回 code
        ▼
jobRequest.setExecutionCode(realCode)          ← 改写后的明文代码
        │
        ▼  后续 Parser / Orchestrator / EC 拿到的都是替换后的 code
```

关键代码位置：
- 入口拦截器 [VarSubstitutionInterceptor.scala:48](linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/VarSubstitutionInterceptor.scala#L48)
- 组装变量表 [CustomVariableUtils.scala:46-94](linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/CustomVariableUtils.scala#L46-L94)
- RPC 拉取 [CustomVariableUtils.scala:52-64](linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/CustomVariableUtils.scala#L52-L64)
- 占位符替换 [VariableUtils.scala:109-172](linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/VariableUtils.scala#L109-L172)

**核心结论：全局变量是在 Entrance 侧被"焊死"进代码字符串的，引擎（EC）拿到时已经是替换后的明文，EC 本身不感知变量机制。**

---

## 二、变量的三类来源与优先级

替换表 `nameAndType` 的填充顺序决定了优先级——用的是 `!nameAn...


## 交互记录 (2026-07-14T10:23:09.664Z)

**用户**：
[{'tool_use_id': 'call_0a2cff3b29764c08856326b8', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.variable.service\n19\t\n20\timport org.apache.linkis.common.utils.Logging\n21\timport org.apache.linkis.protocol.variable.ResponseQueryVariable\n22\timport org.apache.linkis.server.BDPJettyServerHelper\n23\timport org.apache.linkis.variable.dao.VarMapper\n24\timport org.apache.linkis.variable.entity.{VarKey, VarKeyUser, VarKeyValueVO}\n25\timport org.apache.linkis.variable.exception.VariableException\n26\t\n27\timport org.apache.commons.lang3.StringUtils\n28\t\n29\timport org.springframework.beans.factory.annotation.Autowired\n30\timport org.springframework.stereotype.Service\n31\timport org.springframework.transaction.annotation.Transactional\n32\t\n33\timport java.util\n34\t\n35\t@Service\n36\tclass VariableServiceImpl extends VariableService with Logging {\n37\t\n38\t  @Autowired\n39\t  private var varMapper: VarMapper = _\n40\t\n41\t  override def queryGolbalVariable(userName: String): ResponseQueryVariable = {\n42\t    val globals = listGlobalVariable(userName)\n43\t    val response = new ResponseQueryVariable\n44\t    val map = new util.HashMap[String, String]()\n45\t    import scala.collection.JavaConverters._\n46\t    globals.asScala.foreach(f => map.put(f.getKey, f.getValue))\n47\t    response.setKeyAndValue(map)\n48\t    response\n49\t  }\n50\t\n51\t  override def queryAppVariable(\n52\t      userName: String,\n53\t      creator: String,\n54\t      appName: String\n55\t  ): ResponseQueryVariable = {\n56\t    val globals = listGlobalVariable(userName)\n57\t    val response = new ResponseQueryVariable\n58\t    val map = new util.HashMap[String, String]()\n59\t    import scala.collection.JavaConverters._\n60\t    globals.asScala.foreach(f => map.put(f.getKey, f.getValue))\n61\t    response.setKeyAndValue(map)\n62\t    response\n63\t  }\n64\t\n65\t  override def listGlobalVariable(userName: String): util.List[VarKeyValueVO] = {\n66\t    varMapper.listGlobalVariable(userName)\n67\t  }\n68\t\n69\t  private def removeGlobalVariable(keyID: Long): Unit = {\n70\t    val value = varMapper.getValueByKeyID(keyID)\n71\t    varMapper.removeKey(keyID)\n72\t    varMapper.removeValue(value.getId)\n73\t  }\n74\t\n75\t  private def insertGlobalVariable(saveVariable: VarKeyValueVO, userName: String): Unit = {\n76\t    val newKey = new VarKey\n77\t    newKey.setApplicationID(-1L)\n78\t    newKey.setKey(saveVariable.getKey)\n79\t    varMapper.insertKey(newKey)\n80\t    val newValue = new VarKeyUser\n81\t    newValue.setApplicationID(-1L)\n82\t    newValue.setKeyID(newKey.getId)\n83\t    newValue.setUserName(userName)\n84\t    newValue.setValue(saveVariable.getValue)\n85\t    varMapper.insertValue(newValue)\n86\t  }\n87\t\n88\t  private def updateGlobalVariable(saveVariable: VarKeyValueVO, valueID: Long): Unit = {\n89\t    varMapper.updateValue(valueID, saveVariable.getValue)\n90\t  }\n91\t\n92\t  @Transactional\n93\t  override def saveGlobalVaraibles(\n94\t      globalVariables: util.List[_],\n95\t      userVariables: util.List[VarKeyValueVO],\n96\t      userName: String\n97\t  ): Unit = {\n98\t    import scala.collection.JavaConverters._\n99\t    import scala.util.control.Breaks._\n100\t    val saves = globalVariables.asScala.map(f =>\n101\t      BDPJettyServerHelper.gson\n102\t        .fromJson(BDPJettyServerHelper.gson.toJson(f), classOf[VarKeyValueVO])\n103\t    )\n104\t    saves.foreach { f =>\n105\t      if (StringUtils.isBlank(f.getKey) || StringUtils.isBlank(f.getValue)) {\n106\t        throw new VariableException("Key or value cannot be empty")\n107\t      }\n108\t      var flag = true\n109\t      breakable {\n110\t        for (ele <- userVariables.asScala) {\n111\t          if (f.getKey.equals(ele.getKey)) {\n112\t            flag = false\n113\t            updateGlobalVariable(f, ele.getValueID)\n114\t            break()\n115\t          }\n116\t        }\n117\t      }\n118\t      if (flag) insertGlobalVariable(f, userName)\n119\t    }\n120\t    userVariables.asScala.foreach { f =>\n121\t      var flag = true\n122\t      breakable {\n123\t        for (ele <- saves) {\n124\t          if (ele.getKey.equals(f.getKey)) {\n125\t            flag = false\n126\t            break()\n127\t          }\n128\t        }\n129\t        if (flag) removeGlobalVariable(f.getKeyID)\n130\t      }\n131\t    }\n132\t\n133\t  }\n134\t\n135\t}\n136\t'}]

**AI**：
[思考] 用户问的是：能否把"spark3 强制替换"（即当前的 spark3 版本强制切换逻辑）放到"全局变量"体系里实现。

让我先准确理解用户的意图。当前 spark3-coercion-creator 功能的实现是：
- 在 `CommonEntranceParser.sparkVersionCoercion()` 里，根据 SPARK3_VERSION_COERCION_SWITCH + USERS/DEPARTMENT/CREATORS 名单，强制把 Spark2 任务的 EngineTypeLabel 的 version 改成 spark3。
- 这是**改 Label（引擎版本路由标签）**，发生在 Parser 阶段（变量替换之后或独立逻辑）。

而"全局变量"体系是：
- 在 `VarSubstitutionInterceptor` / `CustomVariableUtils` / `VariableUtils.replace` 里，对**代码字符串里的 `${var}` 占位符**做替换。
- 变量来自 PES DB（按用户/全局维度）+ 任务 variableMap + 代码内联 @set。

用户的问题：能不能把"spark3 强制替换"放到全局变量体系里？

这是个**设计可行性问题**。我需要判断：两者的机制是否兼容，能不能复用全局变量的存储/下发/RPC 机制来实现 spark3 强制切换。

关键差异点（这是回答的核心）：

1. **作用对象不同**：
   - 全局变量：替换的是**代码字符串**（`${var}` → value），改 `jobRequest.executionCode`。
   - spark3 coercion：替换的是**Label（EngineTypeLabel 的 version 字段）**，改 `labels` 里的 `ENGINE_TYPE_KEY`，决定任务路由到哪个引擎版本。

   这是两个完全不同的"作用面"。全局变量机制根本不碰 Label，它只改 code 文本。

2. **触发条件不同**：
   - 全局变量：只要 code 里有 `${var}` 占位符就替换，是"按占位符匹配"。
   - spark3 coercion：按"用户/部门/creator 名单"判定，无差别地把该用户所有 spark2 任务的 version label 改成 spark3。与 code 内容无关。

3. **生效阶段不同**：
   - 全局变量：VarSubstitutionInterceptor（拦截器链）。
   - spark3 coercion：CommonEntranceParser.sparkVersionCoercion（Parser 阶段）。

4. **配置维度相似**：
   - 两者都是"按用户维度"的配置（全局变量按 userName 存 DB；coercion 名单也是按 user/department/creator）。
   - 这是唯一的交集——**存储/下发机制**可以复用，但**执行逻辑不能复用**。

那么用户的真实诉求可能是：
- 嫌现在用 `linkis-cg-entrance.properties` + CommonVars 配置 creators 名单（而且 getHotValue-in-val 实际不热加载的 bug）不好用，想用全局变量的"管理后台 UI + 热加载 + 按用户维度"那套来管理 spark3 强制切换的名单？

这是个合理的思路！全局变量体系有几个现成的好处：
- 有前端管理页面（`globalValiable` module）
- 按用户维度存 DB
- 有 RPC 下发机制
- 增删改不用改 properties 文件、不用重启

但要把"spark3 强制切换"塞进全局变量体系，需要想清楚：

**方案 A：用全局变量"存储名单"，但执行逻辑仍独立**
- 把 creators/users 名单存成一条全局变量（比如 key=`spark3_coercion_creators`，value=`app1,app2`），或者每个 creator 存一条布尔变量。
- 在 sparkVersionCoercion 里，不从 CommonVars 读，而是通过 RPC 去 PES 查这条全局变量的值。
- 这样能复用全局变量的"管理 UI + 按用户维度 + 不重启改配置"。
- 但本质上 coercion 的判定逻辑（改 Label）还得在 Parser 里，只是数据源换了。

**方案 B：纯靠全局变量占位符替换实现——不可行**
- 想让用户在代码里写 `${spark_vers...


## 交互记录 (2026-07-14T10:23:29.116Z)

**用户**：
[{'type': 'tool_result', 'content': 'Ripgrep search timed out after 20 seconds. The search may have matched files but did not complete in time. Try searching a more specific path or pattern.', 'is_error': True, 'tool_use_id': 'call_412b8a4ade9b4da091154973'}]

**AI**：
可以做，但要先理清一个根本区别——这决定了"能不能直接套用"。

## 核心障碍：两者作用在 jobRequest 的不同字段

| | 全局变量体系 | spark3 强制替换 |
|---|---|---|
| **改的对象** | `executionCode`（代码字符串里的 `${var}`） | `labels` 里的 `ENGINE_TYPE_KEY`（EngineTypeLabel 的 version） |
| **决定什么** | 代码内容 | 任务**路由到哪个引擎版本** |
| **触发条件** | code 里有 `${var}` 占位符才替换 | 按用户/部门/creator 名单，与 code 内容无关 |
| **生效阶段** | `VarSubstitutionInterceptor`（拦截器链） | `CommonEntranceParser.sparkVersionCoercion`（Parser） |
| **与 code 关系** | 强依赖 | 完全无关 |

全局变量的本质是 [CustomVariableUtils.scala:93](linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/CustomVariableUtils.scala#L93) → `VariableUtils.replace` **只对 code 文本做正则替换**，它从头到尾不碰 labels。而引擎版本是由 Label 决定的，不是 code 里的字符串——你在 code 里写 `${spark_version}` 替换成 `3.4.4` 不会有任何效果，引擎路由根本不看 code。

所以：**不能直接用"全局变量占位符替换"来实现 spark3 强制切换**。即使 code 里没有 `${}`，全局变量机制也无处发力。

## 但可以借用全局变量的"存储/下发"基建

如果你真正的诉求是"现在用 `linkis-cg-entrance.properties` + CommonVars 管名单不好用"（改完要重启、没有 UI、不能按用户维度），那全局变量体系恰好补这些短板。可行做法是**只把全局变量当名单的数据源，执行逻辑仍留在 Parser**：

```
方案A：名单存 PES 全局变量表，Parser 阶段 RPC 回查

管理后台:  给用户 alice 配一条全局变量  spark3_coercion = true
            （或一条 key=spark3_coercion_creators，value=app1,app2）
              │  存 DB（VarMapper），前端 globalValiable module 增删改
              ▼
Parser.sparkVersionCoercion:
   不再读 SPARK3_VERSION_COERCION_CREATORS.getValue
   改为 RPC: Sender.getSender(CLOUD_CONSOLE_VARIABLE...)
              .ask(RequestQueryGlobalVariable(executeUser))
   判断返回里有没有 spark3_coercion=true → 命中则改 EngineTypeLabel
```

收益：
- 复用现成的**前端管理页**（[globalValiable](linkis-web/src/apps/linkis/module/globalValiable/index.vue)）、**按用户维度**、**RPC 下发**；
- 改名单**不用改 properties、不用重启**（绕开 CommonVars 的热加载问题）；
- coercion 判定逻辑（改 Label）不动，回归风险可控。

代价：
- 多一次同步 RPC（每个 spark 任务多一次 PES 查询，可加短缓存）；
- 变量语义被"挪用"——全局变量本来约定只影响 code，现在多了个"保留 key"驱动引擎路由，需要在团队里约定清楚（比如加文档/校验，避免普通用户误配 `spark3_coercion`）。

## 如果想"用变量驱动引擎版本"，正确方向不是 code 变量

引擎版本的真正来源是 **EngineTypeLabel**（或 startup 参数里的版本 key），不是 `${var}`。若要走"参数驱动"路线，应该看：
- 客户端提交时 lab...

