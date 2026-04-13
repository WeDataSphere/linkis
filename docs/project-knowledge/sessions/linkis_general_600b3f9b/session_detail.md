
## 交互记录 (2026-04-02T07:29:16.898Z)

**用户**：
[{'type': 'text', 'text': '<ide_opened_file>The user opened the file g:\\kkhuang\\work\\linkis\\docs\\dev-1.19.0-entrance-fix\\designs\\entrance-offline-cache-fix_设计.md in the IDE. This may or may not be related to the current task.</ide_opened_file>'}, {'type': 'text', 'text': 'clearAllGroupCache 这个方法清理的时候，如果有用户来使用会有什么问题吗'}]

**AI**：
[思考] 用户问的是 `clearAllGroupCache` 这个方法在清理缓存时，如果有用户使用会有什么问题。我需要先找到这个方法并分析其实现。

根据 git status，我看到有这些相关文件：
- `EntranceGroupFactory.scala` (修改)
- `EntranceGroupCacheClearBroadcastListener.scala` (新增)
- `EntranceGroupCacheClearBroadcast.scala` (新增)

让我先读取相关文件来了解这个方法的实现。...


## 交互记录 (2026-04-02T07:29:23.483Z)

**用户**：
[{'tool_use_id': 'call_7b9503dfaaad4c0fbb4ad871', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.scheduler\n19\t\n20\timport org.apache.linkis.common.conf.{CommonVars, Configuration}\n21\timport org.apache.linkis.common.utils.{Logging, Utils}\n22\timport org.apache.linkis.entrance.conf.EntranceConfiguration\n23\timport org.apache.linkis.entrance.errorcode.EntranceErrorCodeSummary._\n24\timport org.apache.linkis.entrance.exception.{EntranceErrorCode, EntranceErrorException}\n25\timport org.apache.linkis.entrance.execute.EntranceJob\n26\timport org.apache.linkis.entrance.utils.EntranceUtils\n27\timport org.apache.linkis.governance.common.protocol.conf.{\n28\t  RequestQueryEngineConfigWithGlobalConfig,\n29\t  ResponseQueryConfig\n30\t}\n31\timport org.apache.linkis.manager.label.entity.Label\n32\timport org.apache.linkis.manager.label.entity.engine.{EngineTypeLabel, UserCreatorLabel}\n33\timport org.apache.linkis.manager.label.utils.LabelUtil\n34\timport org.apache.linkis.rpc.Sender\n35\timport org.apache.linkis.scheduler.queue.{Group, GroupFactory, SchedulerEvent}\n36\timport org.apache.linkis.scheduler.queue.parallelqueue.ParallelGroup\n37\t\n38\timport org.apache.commons.collections.MapUtils\n39\timport org.apache.commons.lang3.StringUtils\n40\t\n41\timport java.util\n42\timport java.util.concurrent.TimeUnit\n43\timport java.util.regex.Pattern\n44\t\n45\timport com.google.common.cache.{Cache, CacheBuilder}\n46\t\n47\tclass EntranceGroupFactory extends GroupFactory with Logging {\n48\t\n49\t  private val groupNameToGroups: Cache[String, Group] = CacheBuilder\n50\t    .newBuilder()\n51\t    .expireAfterAccess(EntranceConfiguration.GROUP_CACHE_EXPIRE_TIME.getValue, TimeUnit.MINUTES)\n52\t    .maximumSize(EntranceConfiguration.GROUP_CACHE_MAX.getValue)\n53\t    .build()\n54\t\n55\t  private val GROUP_MAX_CAPACITY = CommonVars("wds.linkis.entrance.max.capacity", 1000)\n56\t\n57\t  private val SPECIFIED_USERNAME_REGEX =\n58\t    CommonVars("wds.linkis.entrance.specified.username.regex", "hduser.*")\n59\t\n60\t  private val GROUP_SPECIFIED_USER_MAX_CAPACITY =\n61\t    CommonVars("wds.linkis.entrance.specified.max.capacity", 5000)\n62\t\n63\t  private val GROUP_INIT_CAPACITY = CommonVars("wds.linkis.entrance.init.capacity", 100)\n64\t\n65\t  private val specifiedUsernameRegexPattern: Pattern =\n66\t    if (StringUtils.isNotBlank(SPECIFIED_USERNAME_REGEX.getValue)) {\n67\t      Pattern.compile(SPECIFIED_USERNAME_REGEX.getValue)\n68\t    } else {\n69\t      null\n70\t    }\n71\t\n72\t  override def getOrCreateGroup(event: SchedulerEvent): Group = {\n73\t    val labels = event match {\n74\t      case job: EntranceJob =>\n75\t        job.getJobRequest.getLabels\n76\t      case _ =>\n77\t        throw new EntranceErrorException(LABEL_NOT_NULL.getErrorCode, LABEL_NOT_NULL.getErrorDesc)\n78\t    }\n79\t    val groupName = EntranceGroupFactory.getGroupNameByLabels(labels)\n80\t    val cacheGroup = groupNameToGroups.getIfPresent(groupName)\n81\t    if (null == cacheGroup) synchronized {\n82\t      if (groupNameToGroups.getIfPresent(groupName) != null) {\n83\t        return groupNameToGroups.getIfPresent(groupName)\n84\t      }\n85\t      val maxAskExecutorTimes = EntranceConfiguration.MAX_ASK_EXECUTOR_TIME.getValue.toLong\n86\t      val sender: Sender =\n87\t        Sender.getSender(Configuration.CLOUD_CONSOLE_CONFIGURATION_SPRING_APPLICATION_NAME.getValue)\n88\t      val userCreatorLabel: UserCreatorLabel = LabelUtil.getUserCreatorLabel(labels)\n89\t      val engineTypeLabel: EngineTypeLabel = LabelUtil.getEngineTypeLabel(labels)\n90\t      logger.info(\n91\t        s"Getting user configurations for $groupName userCreatorLabel: ${userCreatorLabel.getStringValue}, engineTypeLabel:${engineTypeLabel.getStringValue}."\n92\t      )\n93\t      val keyAndValue = Utils.tryAndWarnMsg {\n94\t        sender\n95\t          .ask(RequestQueryEngineConfigWithGlobalConfig(userCreatorLabel, engineTypeLabel))\n96\t          .asInstanceOf[ResponseQueryConfig]\n97\t          .getKeyAndValue\n98\t      }(\n99\t        "Get user configurations from configuration server failed! Next use the default value to continue."\n100\t      )\n101\t      val maxRunningJobs = EntranceGroupFactory.getUserMaxRunningJobs(keyAndValue)\n102\t      val initCapacity = GROUP_INIT_CAPACITY.getValue(keyAndValue)\n103\t      val maxCapacity = if (null != specifiedUsernameRegexPattern) {\n104\t        if (specifiedUsernameRegexPattern.matcher(userCreatorLabel.getUser).find()) {\n105\t          logger.info(\n106\t            s"Set maxCapacity of user ${userCreatorLabel.getUser} to specifiedMaxCapacity : ${GROUP_SPECIFIED_USER_MAX_CAPACITY\n107\t              .getValue(keyAndValue)}"\n108\t          )\n109\t          GROUP_SPECIFIED_USER_MAX_CAPACITY.getValue(keyAndValue)\n110\t        } else {\n111\t          GROUP_MAX_CAPACITY.getValue(keyAndValue)\n112\t        }\n113\t      } else {\n114\t        GROUP_MAX_CAPACITY.getValue(keyAndValue)\n115\t      }\n116\t      logger.info(\n117\t        s"Got user configurations: groupName=$groupName, maxRunningJobs=$maxRunningJobs, initCapacity=$initCapacity, maxCapacity=$maxCapacity."\n118\t      )\n119\t      val group = new ParallelGroup(groupName, initCapacity, maxCapacity)\n120\t      group.setMaxRunningJobs(maxRunningJobs)\n121\t      group.setMaxAskExecutorTimes(maxAskExecutorTimes)\n122\t      groupNameToGroups.put(groupName, group)\n123\t      group\n124\t    }\n125\t    else {\n126\t      cacheGroup\n127\t    }\n128\t  }\n129\t\n130\t  override def getGroup(groupName: String): Group = {\n131\t    val group = groupNameToGroups.getIfPresent(groupName)\n132\t    if (group == null) {\n133\t      throw new EntranceErrorException(\n134\t        EntranceErrorCode.GROUP_NOT_FOUND.getErrCode,\n135\t        s"group not found: ${groupName}"\n136\t      )\n137\t    }\n138\t    group\n139\t  }\n140\t\n141\t  /**\n142\t   * 清除所有Group缓存\n143\t   *\n144\t   * 调用时机：\n145\t   * 1. 接收到EntranceGroupCacheClearBroadcast广播时\n146\t   * 2. 手动清除缓存（如管理API）\n147\t   *\n148\t   * 线程安全：Guava Cache的invalidateAll()是原子操作，支持并发调用\n149\t   */\n150\t  def clearAllGroupCache(): Unit = {\n151\t    val cacheSize = groupNameToGroups.size()\n152\t    groupNameToGroups.invalidateAll()\n153\t    logger.info(s"Cleared all Group cache. Cache size before clear: $cacheSize")\n154\t  }\n155\t\n156\t}\n157\t\n158\tobject EntranceGroupFactory {\n159\t\n160\t  /**\n161\t   * Entrance group rule creator_username_engineType eg:IDE_PEACEWONG_SPARK\n162\t   * @param labels\n163\t   * @param params\n164\t   * @return\n165\t   */\n166\t  def getGroupNameByLabels(labels: java.util.List[Label[_]]): String = {\n167\t    val userCreatorLabel = LabelUtil.getUserCreatorLabel(labels)\n168\t    val engineTypeLabel = LabelUtil.getEngineTypeLabel(labels)\n169\t    if (null == userCreatorLabel || null == engineTypeLabel) {\n170\t      throw new EntranceErrorException(LABEL_NOT_NULL.getErrorCode, LABEL_NOT_NULL.getErrorDesc)\n171\t    }\n172\t    val groupName =\n173\t      userCreatorLabel.getCreator + "_" + userCreatorLabel.getUser + "_" + engineTypeLabel.getEngineType\n174\t    groupName\n175\t  }\n176\t\n177\t  /**\n178\t   * User task concurrency control is controlled for multiple Entrances, which will be evenly\n179\t   * distributed based on the number of existing Entrances\n180\t   * @param keyAndValue\n181\t   * @return\n182\t   */\n183\t  def getUserMaxRunningJobs(keyAndValue: util.Map[String, String]): Int = {\n184\t    val userDefinedRunningJobs =\n185\t      if (\n186\t          MapUtils.isNotEmpty(keyAndValue) && keyAndValue.containsKey(\n187\t            EntranceConfiguration.WDS_LINKIS_ENTRANCE_RUNNING_JOB.key\n188\t          )\n189\t      ) {\n190\t        EntranceConfiguration.WDS_LINKIS_ENTRANCE_RUNNING_JOB.getValue(keyAndValue)\n191\t      } else {\n192\t        EntranceConfiguration.WDS_LINKIS_INSTANCE.getValue(keyAndValue)\n193\t      }\n194\t    val entranceNum = EntranceUtils.getRunningEntranceNumber()\n195\t    Math.max(\n196\t      EntranceConfiguration.ENTRANCE_INSTANCE_MIN.getValue,\n197\t      userDefinedRunningJobs / entranceNum\n198\t    )\n199\t  }\n200\t}\n201\t\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
让我先查看 `clearAllGroupCache` 方法的实现和相关代码。...


## 交互记录 (2026-04-02T07:29:23.492Z)

**用户**：
[{'type': 'tool_result', 'content': 'File does not exist. Note: your current working directory is g:\\kkhuang\\work\\linkis.', 'is_error': True, 'tool_use_id': 'call_fac0050e2abf48579db4cfb2'}]

**AI**：
[思考] 用户问的是 `clearAllGroupCache` 方法在清理缓存时，如果有用户使用会有什么问题。让我分析一下当前的实现。

从我读取的代码来看，`clearAllGroupCache` 方法在 `EntranceGroupFactory.scala` 中：

```scala
def clearAllGroupCache(): Unit = {
  val cacheSize = groupNameToGroups.size()
  groupNameToGroups.invalidateAll()
  logger.info(s"Cleared all Group cache. Cache size before clear: $cacheSize")
}
```

这个方法直接调用 Guava Cache 的 `invalidateAll()` 方法来清除所有缓存。

分析潜在问题：

1. **竞态条件 (Race Condition)**：当缓存被清空后，如果有新的任务进来，`getOrCreateGroup` 方法会重新创建 Group。但如果此时有任务正在使用被清空的 Group 对象，可能会有问题。

2. **运行中任务的影响**：虽然缓存被清空了，但如果已经有任务拿到了 Group 对象的引用并在使用，那么这个 Group 对象本身不会被立即销毁（除非没有强引用了）。但是后续新的任务进来时，会重新创建新的 Group。

3. **资源浪费**：如果老的 Group 还在运行任务，新的任务进来会创建新的 Group，这可能导致同一个 groupName 同时存在多个 Group 对象，造成资源浪费和并发控制失效。
 
这种方法存在严重的并发控制风险。当正在执行的任务使用的 Group 被意外清除，可能导致并发机制完全失效。新任务创建新 Group 后，原有的运行任务可能无法正确同步，造成资源分配混乱和潜在的系统不稳定。...


## 交互记录 (2026-04-02T07:29:23.494Z)

**用户**：
[{'tool_use_id': 'call_40e08b851df9485f89a783ff', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.listener\n19\t\n20\timport org.apache.linkis.common.utils.Logging\n21\timport org.apache.linkis.entrance.protocol.EntranceGroupCacheClearBroadcast\n22\timport org.apache.linkis.entrance.scheduler.EntranceGroupFactory\n23\timport org.apache.linkis.protocol.BroadcastProtocol\n24\timport org.apache.linkis.rpc.BroadcastListener\n25\timport org.apache.linkis.rpc.Sender\n26\timport org.springframework.beans.factory.annotation.Autowired\n27\timport org.springframework.stereotype.Service\n28\t\n29\t/**\n30\t * Entrance Group缓存清除广播监听器\n31\t *\n32\t * 核心职责：\n33\t * 1. 接收EntranceGroupCacheClearBroadcast广播消息\n34\t * 2. 调用EntranceGroupFactory清除所有Group缓存\n35\t * 3. 记录清除日志，便于监控和排查\n36\t */\n37\t@Service\n38\tclass EntranceGroupCacheClearBroadcastListener extends BroadcastListener with Logging {\n39\t\n40\t  @Autowired private var entranceGroupFactory: EntranceGroupFactory = _\n41\t  override def onBroadcastEvent(protocol: BroadcastProtocol, sender: Sender): Unit = {\n42\t    protocol match {\n43\t      case clear: EntranceGroupCacheClearBroadcast =>\n44\t        logger.info(s"Received cache clear broadcast from ${clear.instance} at ${clear.timestamp}")\n45\t        try {\n46\t          // 清除所有Group缓存\n47\t          entranceGroupFactory.clearAllGroupCache()\n48\t          logger.info(s"Successfully cleared all Group cache. Broadcast from: ${clear.instance}")\n49\t        } catch {\n50\t          case e: Exception =>\n51\t            logger.error(s"Failed to clear Group cache. Broadcast from: ${clear.instance}", e)\n52\t            // 不抛出异常，避免影响广播流程\n53\t        }\n54\t\n55\t      case _ =>\n56\t      // 忽略其他类型的广播消息\n57\t    }\n58\t  }\n59\t}\n60\t\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
根据代码分析，`clearAllGroupCache` 方法在清理时如果有用户使用存在以下潜在问题：

## 潜在问题分析

### 1. **并发控制失效**
```scala
// EntranceGroupFactory.scala:119-122
val group = new ParallelGroup(groupName, initCapacity, maxCapacity)
group.setMaxRunningJobs(maxRunningJobs)
group.setMaxAskExecutorTimes(maxAskExecutorTimes)
groupNameToGroups.put(groupName, group)
```

当缓存被清空后，新的任务会创建**全新的 Group 对象**，导致：
- 原有的 `maxRunningJobs` 并发控制失效
- 同一个 `groupName` 可能同时存在多个 Group 实例
- 资源限制被绕过

### 2. **Group 状态丢失**
正在运行的 Group 可能包含：
- 任务队列状态
- 运行中的任务计数
- 调度器引用

这些状态在缓存清除后对已持有 Group 引用的任务仍然有效，但新任务会使用全新初始化的 Group。

### 3. **时序问题**

```
时间线：
T1: 任务A 获取 Group1 (runningJobs=3/5)
T2: clearAllGroupCache() 被调用
T3: 任务B 尝试获取 Group，缓存未命中，创建 Group2 (runningJobs=0/5)
T4: 任务A、任务B 各自使用不同的 Group，并发控制失效
```

## 建议的改进方案

### 方案1: 懒惰清理 + 引用追踪
只清理空闲的 Group，保留正在使用的 Group：

```scala
def clearIdleGroupCache(): Unit = {
  val iterator = groupNameToGroups.asMap().entrySet().iterator()
  var clearedCount = 0
  while (iterator.hasNext) {
    val entry = iterator.next()
    val group = entry.getValue
    // 检查 Group 是否有活跃任务
    if (group.getRunningJobs == 0 && group.getPendingEvents == 0) {
      groupNameToGroups.invalidate(entry.getKey)
      clearedCount += 1
    }
  }
  logger.info(s"Cleared $clearedCount idle Groups")
}
```

### 方案2: 延迟清理
标记 Group 需要清理，在下次访问时才真正清理：

```scala
private val cleanupTimestamps = new ConcurrentHashMap[String, Long]()

def markAllGroupForCleanup(): Unit = {
  val now = System.currentTimeMillis()
  groupNameToGroups.asMap().forEachKey(key => 
    cleanupTimestamps.put(key, now)
  )
}

override def getOrCreateGroup(event: SchedulerEvent): Group = {
  val groupName = /* ... */
  cleanupTimestamps.remove(groupName) match {
    case null => // 正常流程
    case timestamp => 
      // 检查是否需要重建
      synchronized {
        if (groupNameToGroups.getIfPresent(groupName) != null) {
          groupNameToGroups.invalidate(groupName)
        }
      }
  }
  // ... 继续正常流程
}
```

### 方案3: 分批清理
只清理满足特定条件的 Group（如超过一定时间未使用的）：

```scala
d...


## 交互记录 (2026-04-02T07:32:06.756Z)

**用户**：
[{'type': 'text', 'text': 'ok，对我代码的所有的改动用一个开关控制， 一个开关可以控制所有的改动不生效'}]

**AI**：
[思考] 用户希望对所有代码改动添加一个开关控制，使得这些改动可以整体关闭。我需要先查看当前所有的改动，然后设计一个配置开关来控制这些改动。

从git status可以看到主要改动：
1. `EntranceLabelRestfulApi.java` - 修改
2. `EntranceGroupFactory.scala` - 修改
3. `EntranceGroupCacheClearBroadcastListener.scala` - 新增
4. `EntranceGroupCacheClearBroadcast.scala` - 新增

我需要：
1. 先查看这些文件的具体改动
2. 设计一个配置开关
3. 修改相关代码，用这个开关控制所有新功能

让我先查看完整的改动。...


## 交互记录 (2026-04-02T07:32:12.501Z)

**用户**：
[{'tool_use_id': 'call_c8aae5c023f046f7a5293d82', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.protocol\n19\t\n20\timport org.apache.linkis.protocol.BroadcastProtocol\n21\t\n22\t/**\n23\t * Entrance Group缓存清除广播消息\n24\t *\n25\t * 广播时机：Entrance实例offline时（通过/markoffline接口触发）\n26\t * 广播目的：通知所有其他Entrance实例清除本地Group缓存\n27\t * 广播效果：下次任务提交时重新计算并发数，排除offline实例\n28\t *\n29\t * @param instance offline的Entrance实例标识\n30\t * @param timestamp 广播发送时间戳（毫秒）\n31\t */\n32\tcase class EntranceGroupCacheClearBroadcast(\n33\t    instance: String,\n34\t    timestamp: Long\n35\t) extends BroadcastProtocol {\n36\t\n37\t  // 不抛出任何异常，即使部分实例接收失败也不影响offline流程\n38\t  override val throwsIfAnyFailed: Boolean = false\n39\t\n40\t}\n41\t\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
我先查看所有改动的内容，然后设计一个统一的配置开关。...


## 交互记录 (2026-04-02T07:32:16.611Z)

**用户**：
[{'tool_use_id': 'call_a3c92573a307496b96a9d001', 'type': 'tool_result', 'content': 'diff --git a/linkis-computation-governance/linkis-entrance/src/main/java/org/apache/linkis/entrance/restful/EntranceLabelRestfulApi.java b/linkis-computation-governance/linkis-entrance/src/main/java/org/apache/linkis/entrance/restful/EntranceLabelRestfulApi.java\nindex 2c5fce264..240d11218 100644\n--- a/linkis-computation-governance/linkis-entrance/src/main/java/org/apache/linkis/entrance/restful/EntranceLabelRestfulApi.java\n+++ b/linkis-computation-governance/linkis-entrance/src/main/java/org/apache/linkis/entrance/restful/EntranceLabelRestfulApi.java\n@@ -20,6 +20,7 @@ package org.apache.linkis.entrance.restful;\n import org.apache.linkis.DataWorkCloudApplication;\n import org.apache.linkis.common.ServiceInstance;\n import org.apache.linkis.common.conf.Configuration;\n+import org.apache.linkis.entrance.protocol.EntranceGroupCacheClearBroadcast;\n import org.apache.linkis.instance.label.client.InstanceLabelClient;\n import org.apache.linkis.manager.label.constant.LabelKeyConstant;\n import org.apache.linkis.manager.label.constant.LabelValueConstant;\n@@ -90,6 +91,19 @@ public class EntranceLabelRestfulApi {\n     synchronized (offlineFlag) { // NOSONAR\n       offlineFlag = true;\n     }\n+    try {\n+      // 构造广播消息\n+      EntranceGroupCacheClearBroadcast broadcast = new EntranceGroupCacheClearBroadcast(\n+              Sender.getThisInstance(),\n+              System.currentTimeMillis()\n+      );\n+      // 获取entrance服务的Sender并发送广播\n+      Sender.getSender(Sender.getThisServiceInstance()).send(broadcast);\n+      logger.info("Successfully sent cache clear broadcast for entrance offline");\n+    } catch (Exception e) {\n+      // 广播失败不影响offline流程，只记录日志\n+      logger.error("Failed to send cache clear broadcast, entrance offline continues", e);\n+    }\n     logger.info("Finished to modify the routelabel of entry to offline");\n     return Message.ok();\n   }', 'is_error': False}]

**AI**：
[思考] 现在我看到了所有的改动：

1. `EntranceLabelRestfulApi.java` - 在 `/markoffline` 接口中添加了广播发送逻辑
2. `EntranceGroupCacheClearBroadcast.scala` - 新增的广播协议类
3. `EntranceGroupCacheClearBroadcastListener.scala` - 新增的广播监听器
4. `EntranceGroupFactory.scala` - 新增了 `clearAllGroupCache()` 方法

用户希望通过一个开关来控制所有这些改动。我需要：
1. 在 `EntranceConfiguration` 中添加一个配置开关
2. 修改所有相关代码，让它们检查这个开关

让我先查看 `EntranceConfiguration` 的内容。...


## 交互记录 (2026-04-02T07:32:25.512Z)

**用户**：
[{'tool_use_id': 'call_cfa70b69fcb343d7b3bae419', 'type': 'tool_result', 'content': 'linkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\conf\\EntranceConfiguration.scala'}]

**AI**：
现在查看 `EntranceConfiguration` 并添加配置开关：...


## 交互记录 (2026-04-02T07:32:26.937Z)

**用户**：
[{'tool_use_id': 'call_2157e671a1cd4d45b43a64a6', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.conf\n19\t\n20\timport org.apache.linkis.common.conf.{CommonVars, TimeType}\n21\t\n22\tobject EntranceConfiguration {\n23\t\n24\t  val ENTRANCE_SCHEDULER_MAX_PARALLELISM_USERS =\n25\t    CommonVars("wds.linkis.entrance.scheduler.maxParallelismUsers", new Integer(1000))\n26\t\n27\t  val ENTRANCE_LISTENER_BUS_EVENT_QUEUE_CAPACITY =\n28\t    CommonVars("wds.linkis.entrance.listenerBus.queue.capacity", new Integer(5000))\n29\t\n30\t  val JOB_MAX_PERSIST_WAIT_TIME =\n31\t    CommonVars("wds.linkis.entrance.job.persist.wait.max", new TimeType("5m"))\n32\t\n33\t  /**\n34\t   * Default_Cache_Max is used to specify the size of the LoopArray of the CacheLogWriter\n35\t   * Default_Cache_Max 是用来指定CacheLogWriter的LoopArray的大小\n36\t   */\n37\t  val DEFAULT_CACHE_MAX = CommonVars("wds.linkis.entrance.log.cacheMax", 500)\n38\t\n39\t  /**\n40\t   * Default_Log_CharSet is used to specify the encoding mode of the log storage.\n41\t   * Default_Log_CharSet 是用来指定日志存储的编码方式\n42\t   */\n43\t  val DEFAULT_LOG_CHARSET = CommonVars("wds.linkis.entrance.log.defaultCharSet", "utf-8")\n44\t\n45\t  /**\n46\t   * The logPath in the console module returns the key in the map. console 模块中logPath在返回map中的key\n47\t   */\n48\t  val CLOUD_CONSOLE_LOGPATH_KEY =\n49\t    CommonVars("wds.linkis.console.config.logPath", "wds.linkis.config.logPath")\n50\t\n51\t  /**\n52\t   * requestApplicationName(Creator) The default service name, the default is IDE\n53\t   * requestApplicationName(Creator) 默认的服务名，默认为IDE\n54\t   */\n55\t  val DEFAULT_REQUEST_APPLICATION_NAME =\n56\t    CommonVars("wds.linkis.default.requestApplication.name", "IDE")\n57\t\n58\t  val SQL_LIMIT_CREATOR = CommonVars("wds.linkis.sql.limit.creator", "IDE")\n59\t\n60\t  /**\n61\t   * runType\n62\t   */\n63\t  val DEFAULT_RUN_TYPE = CommonVars("wds.linkis.default.runType", "sql")\n64\t\n65\t  val DEFAULT_CREATE_SERVICE =\n66\t    CommonVars("wds.linkis.default.create.service", "default_create_service")\n67\t\n68\t  val LOG_WARN_EXCLUDE = CommonVars("wds.linkis.warn.log.exclude", "com.netflix")\n69\t\n70\t  val CLEAR_LOG = CommonVars("wds.linkis.log.clear", false)\n71\t\n72\t  /**\n73\t   * LOG_EXCLUDE is used to remove the log of the framework log, such as hive spark spring, so that\n74\t   * it is not pushed to the front end through websocket. LOG_EXCLUDE 是用来进行把框架日志，比如hive spark\n75\t   * spring等日志进行剔除，不让其通过websocket进行推送到前端\n76\t   */\n77\t  val LOG_EXCLUDE = CommonVars("wds.linkis.log.exclude", "com.netflix")\n78\t\n79\t  /**\n80\t   * wds.linkis.dwc.instance is a parameter used to control the number of engines each user starts.\n81\t   * wds.linkis.instance 是用来进行控制每个用户启动engine数量的参数\n82\t   */\n83\t  val WDS_LINKIS_INSTANCE = CommonVars("wds.linkis.rm.instance", 10)\n84\t\n85\t  val WDS_LINKIS_ENTRANCE_RUNNING_JOB =\n86\t    CommonVars("wds.linkis.engine.running.job.max", WDS_LINKIS_INSTANCE.getValue)\n87\t\n88\t  val ENTRANCE_INSTANCE_MIN = CommonVars("wds.linkis.entrance.runningjob.min", 5)\n89\t\n90\t  val LOG_EXCLUDE_ALL = CommonVars("wds.linkis.log.exclude.all", "com.netflix")\n91\t\n92\t  val MAX_ASK_EXECUTOR_TIME = CommonVars("wds.linkis.max.ask.executor.time", new TimeType("5m"))\n93\t\n94\t  val ERROR_CODE_FILE_DIR = CommonVars("wds.linkis.errorcode.file.dir", "")\n95\t\n96\t  val ENTRANCE_USER = CommonVars("wds.linkis.entrance.user", "")\n97\t\n98\t  val ERROR_CODE_FILE = CommonVars("wds.linkis.errorcode.file", "")\n99\t\n100\t  // todo Simple processing first, this log is not filtered, and subsequent optimization is performed.(先简单处理 不过滤这个日志，后续进行优化)\n101\t  val HIVE_SPECIAL_LOG_INCLUDE =\n102\t    CommonVars("wds.linkis.hive.special.log.include", "org.apache.hadoop.hive.ql.exec.Task")\n103\t\n104\t  val SHARE_FILE_PRE = CommonVars("wds.linkis.share.file.prefix", "")\n105\t\n106\t  val HIVE_THREAD_NAME = CommonVars("wds.linkis.hive.thread.name", "[Thread")\n107\t\n108\t  val HIVE_STAGE_NAME = CommonVars("wds.linkis.hive.stage.name", "Stage-")\n109\t\n110\t  val SPARK_SPECIAL_LOG_INCLUDE = CommonVars(\n111\t    "wds.linkis.spark.special.log.include",\n112\t    "org.apache.linkis.engine.spark.utils.JobProgressUtil"\n113\t  )\n114\t\n115\t  val SPARK_PROGRESS_NAME = CommonVars(\n116\t    "wds.linkis.spark.progress.name",\n117\t    "org.apache.linkis.engine.spark.utils.JobProgressUtil$"\n118\t  )\n119\t\n120\t  val END_FLAG = CommonVars("bdp.dataworkcloud.entrance.end.flag", "info -")\n121\t\n122\t  val HIVE_CREATE_TABLE_LOG = CommonVars("wds.linkis.hive.create.table.log", "numFiles")\n123\t\n124\t  val HIVE_PRINT_INFO_LOG = CommonVars("wds.linkis.hive.printinfo.log", "printInfo -")\n125\t\n126\t  val IS_BDP_ENV = CommonVars("wds.linkis.entrance.bdp.env", "true")\n127\t\n128\t  val SHELL_DANGER_CHECK_SWITCH =\n129\t    CommonVars("wds.linkis.entrance.shell.danger.check.enabled", true)\n130\t\n131\t  val SHELL_DANGER_USAGE = CommonVars(\n132\t    "wds.linkis.shell.danger.usage",\n133\t    "rm,sh,find,kill,python,for,source,hdfs,hadoop,spark-sql,spark-submit,pyspark,spark-shell,hive,yarn"\n134\t  )\n135\t\n136\t  val SHELL_WHITE_USAGE = CommonVars(\n137\t    "wds.linkis.shell.white.usage",\n138\t    "sqoop,cd,ll,ls,echo,cat,tree,diff,who,grep,whoami,set,pwd,cut,file,head,less,if,while"\n139\t  )\n140\t\n141\t  val FLOW_EXECUTION_CREATOR = CommonVars("wds.linkis.entrance.flow.creator", "nodeexecution")\n142\t\n143\t  val SCHEDULER_CREATOR = CommonVars("wds.linkis.entrance.scheduler.creator", "Schedulis")\n144\t\n145\t  val SKIP_AUTH = CommonVars("wds.linkis.entrance.skip.auth", false)\n146\t\n147\t  val PROGRESS_PUSH = CommonVars[String]("wds.linkis.entrance.push.progress", "false")\n148\t\n149\t  val CONCURRENT_FACTORY_MAX_CAPACITY =\n150\t    CommonVars("wds.linkis.concurrent.group.factory.capacity", 1000)\n151\t\n152\t  val CONCURRENT_MAX_RUNNING_JOBS =\n153\t    CommonVars("wds.linkis.concurrent.group.factory.running.jobs", 30)\n154\t\n155\t  val CONCURRENT_EXECUTOR_TIME =\n156\t    CommonVars("wds.linkis.concurrent.group.factory.executor.time", 5 * 60 * 1000)\n157\t\n158\t  val ENTRANCE_ENGINE_LASTUPDATE_TIMEOUT =\n159\t    CommonVars("wds.linkis.entrance.engine.lastupdate.timeout", new TimeType("5s"))\n160\t\n161\t  val ENTRANCE_ENGINE_ACTIVITY_TIMEOUT =\n162\t    CommonVars("wds.linkis.entrance.engine.timeout", new TimeType("10s"))\n163\t\n164\t  val ENTRANCE_ENGINE_ACTIVITY_MONITOR_INTERVAL =\n165\t    CommonVars("wds.linkis.entrance.engine.activity_monitor.interval", new TimeType("3s"))\n166\t\n167\t  // Whether to turn on timeout detection\n168\t  val ENABLE_JOB_TIMEOUT_CHECK = CommonVars("wds.linkis.enable.job.timeout.check", true)\n169\t\n170\t  // unit is seconds\n171\t  val TIMEOUT_SCAN_INTERVAL = CommonVars("wds.linkis.timeout.thread.scan.interval", 120)\n172\t\n173\t  // unit is MINUTES\n174\t  val USER_PARALLEL_REFLESH_TIME = CommonVars("wds.linkis.user.parallel.reflesh.time", 30)\n175\t\n176\t  val JOBINFO_UPDATE_RETRY =\n177\t    CommonVars[java.lang.Boolean]("wds.linkis.entrance.jobinfo.update.retry", true)\n178\t\n179\t  val JOBINFO_UPDATE_RETRY_MAX_TIME =\n180\t    CommonVars[Integer]("wds.linkis.entrance.jobinfo.update.retry.max.times", 3)\n181\t\n182\t  val JOBINFO_UPDATE_RETRY_INTERVAL =\n183\t    CommonVars[Integer]("wds.linkis.entrance.jobinfo.update.retry.interval", 2 * 60 * 1000)\n184\t\n185\t  val CODE_PARSER_SELECTIVE_IGNORED =\n186\t    CommonVars[java.lang.Boolean]("wds.linkis.entrance.code.parser.selective.ignored", true)\n187\t\n188\t  val YARN_QUEUE_CORES_MAX = CommonVars[Integer]("wds.linkis.entrance.yarn.queue.core.max", 300)\n189\t\n190\t  val YARN_QUEUE_MEMORY_MAX =\n191\t    CommonVars[Integer]("wds.linkis.entrance.yarn.queue.memory.max.g", 1000)\n192\t\n193\t  val ENABLE_HDFS_LOG_CACHE =\n194\t    CommonVars[Boolean]("linkis.entrance.enable.hdfs.log.cache", true).getValue\n195\t\n196\t  val CLI_HEARTBEAT_THRESHOLD_SECONDS =\n197\t    CommonVars[Long]("linkis.entrance.cli.heartbeat.threshold.sec", 30L).getValue\n198\t\n199\t  val LOG_PUSH_INTERVAL_TIME =\n200\t    CommonVars("wds.linkis.entrance.log.push.interval.time", 5 * 60 * 1000)\n201\t\n202\t  val GROUP_CACHE_MAX = CommonVars("wds.linkis.consumer.group.cache.capacity", 5000)\n203\t\n204\t  val GROUP_CACHE_EXPIRE_TIME = CommonVars("wds.linkis.consumer.group.expire.time", 50)\n205\t\n206\t  val CLIENT_MONITOR_CREATOR =\n207\t    CommonVars("wds.linkis.entrance.client.monitor.creator", "LINKISCLI,BdpClient")\n208\t\n209\t  val CREATOR_IP_SWITCH =\n210\t    CommonVars("wds.linkis.entrance.user.creator.ip.interceptor.switch", false)\n211\t\n212\t  val TEMPLATE_CONF_SWITCH =\n213\t    CommonVars("wds.linkis.entrance.template.conf.interceptor.switch", true)\n214\t\n215\t  val TEMPLATE_CONF_ADD_ONCE_LABEL_ENABLE =\n216\t    CommonVars("wds.linkis.entrance.template.add.once.label.enable", false)\n217\t\n218\t  val SUPPORT_TEMPLATE_CONF_RETRY_ENABLE =\n219\t    CommonVars("linkis.entrance.template.retry.enable", false)\n220\t\n221\t  val ENABLE_ENTRANCE_DIRTY_DATA_CLEAR: CommonVars[Boolean] =\n222\t    CommonVars[Boolean]("linkis.entrance.auto.clean.dirty.data.enable", true)\n223\t\n224\t  val ENTRANCE_CREATOR_JOB_LIMIT: CommonVars[Int] =\n225\t    CommonVars[Int](\n226\t      "linkis.entrance.creator.job.concurrency.limit",\n227\t      10000,\n228\t      "Creator task concurrency limit parameters"\n229\t    )\n230\t\n231\t  val ENTRANCE_CREATOR_JOB_LIMIT_CONF_CACHE =\n232\t    CommonVars("linkis.entrance.creator.job.concurrency.limit.conf.cache.time", 30L)\n233\t\n234\t  val ENTRANCE_TASK_TIMEOUT =\n235\t    CommonVars("linkis.entrance.task.timeout", new TimeType("48h"))\n236\t\n237\t  val ENTRANCE_TASK_TIMEOUT_SCAN =\n238\t    CommonVars("linkis.entrance.task.timeout.scan", new TimeType("12h"))\n239\t\n240\t  val ENABLE_HDFS_JVM_USER =\n241\t    CommonVars[Boolean]("linkis.entrance.enable.hdfs.jvm.user", true).getValue\n242\t\n243\t  val ENTRANCE_FAILOVER_ENABLED = CommonVars("linkis.entrance.failover.enable", false).getValue\n244\t\n245\t  val ENTRANCE_FAILOVER_SCAN_INIT_TIME =\n246\t    CommonVars("linkis.entrance.failover.scan.init.time", 3 * 1000).getValue\n247\t\n248\t  val ENTRANCE_FAILOVER_SCAN_INTERVAL =\n249\t    CommonVars("linkis.entrance.failover.scan.interval", 30 * 1000).getValue\n250\t\n251\t  val ENTRANCE_FAILOVER_DATA_NUM_LIMIT =\n252\t    CommonVars("linkis.entrance.failover.data.num.limit", 10).getValue\n253\t\n254\t  val ENTRANCE_FAILOVER_DATA_INTERVAL_TIME =\n255\t    CommonVars("linkis.entrance.failover.data.interval.time", new TimeType("1d").toLong).getValue\n256\t\n257\t  // if true, the waitForRetry job in runningJobs can be failover\n258\t  val ENTRANCE_FAILOVER_RETRY_JOB_ENABLED =\n259\t    CommonVars("linkis.entrance.failover.retry.job.enable", false)\n260\t\n261\t  val ENTRANCE_UPDATE_BATCH_SIZE = CommonVars("linkis.entrance.update.batch.size", 100)\n262\t\n263\t  // if true, the job in ConsumeQueue can be failover\n264\t  val ENTRANCE_SHUTDOWN_FAILOVER_CONSUME_QUEUE_ENABLED =\n265\t    CommonVars("linkis.entrance.shutdown.failover.consume.queue.enable", false).getValue\n266\t\n267\t  val ENTRANCE_GROUP_SCAN_ENABLED = CommonVars("linkis.entrance.group.scan.enable", false)\n268\t\n269\t  val ENTRANCE_GROUP_SCAN_INIT_TIME = CommonVars("linkis.entrance.group.scan.init.time", 3 * 1000)\n270\t\n271\t  val ENTRANCE_GROUP_SCAN_INTERVAL = CommonVars("linkis.entrance.group.scan.interval", 60 * 1000)\n272\t\n273\t  val ENTRANCE_FAILOVER_RETAIN_METRIC_ENGINE_CONN_ENABLED =\n274\t    CommonVars("linkis.entrance.failover.retain.metric.engine.conn.enable", false)\n275\t\n276\t  val ENTRANCE_FAILOVER_RETAIN_METRIC_YARN_RESOURCE_ENABLED =\n277\t    CommonVars("linkis.entrance.failover.retain.metric.yarn.resource.enable", false)\n278\t\n279\t  // if true, job whose status is running will be set to Cancelled\n280\t  val ENTRANCE_FAILOVER_RUNNING_KILL_ENABLED =\n281\t    CommonVars("linkis.entrance.failover.running.kill.enable", false)\n282\t\n283\t  val LINKIS_ENTRANCE_SKIP_ORCHESTRATOR =\n284\t    CommonVars("linkis.entrance.skip.orchestrator", false).getValue\n285\t\n286\t  val ENABLE_HDFS_RES_DIR_PRIVATE =\n287\t    CommonVars[Boolean]("linkis.entrance.enable.hdfs.res.dir.private", false).getValue\n288\t\n289\t  val UNSUPPORTED_RETRY_CODES =\n290\t    CommonVars("linkis.entrance.unsupported.retry.codes", "NOCODE").getValue\n291\t\n292\t  val SUPPORTED_RETRY_ERROR_CODES =\n293\t    CommonVars(\n294\t      "linkis.entrance.supported.retry.error.codes",\n295\t      "01002,01003,13005,13006,13012"\n296\t    ).getValue\n297\t\n298\t  val SUPPORTED_RETRY_ERROR_DESC =\n299\t    CommonVars(\n300\t      "linkis.entrance.supported.retry.error.desc",\n301\t      "Spark application has already stopped,Spark application sc has already stopped,Failed to allocate a page,dataFrame to local exception,org.apache.spark.sql.catalyst.expressions.codegen.CodeGenerator"\n302\t    ).getValue\n303\t\n304\t  val SUPPORTED_RETRY_ERROR_DESC_REGEX =\n305\t    CommonVars(\n306\t      "linkis.entrance.supported.retry.error.desc.regex",\n307\t      "Query timeout.*,Query exceeded time limit.*,Memory of process exceed limit.*,Backend node not found,Connection reset by peer,StarRocks planner use long time,pending timeout"\n308\t    ).getValue\n309\t\n310\t  val SUPPORT_ADD_RETRY_CODE_KEYS =\n311\t    CommonVars(\n312\t      "linkis.entrance.supported.add.retry.code.keys",\n313\t      "dataFrame to local exception,org.apache.spark.sql.catalyst.expressions.codegen.CodeGenerator"\n314\t    ).getValue\n315\t\n316\t  val TASK_RETRY_ENABLED: Boolean =\n317\t    CommonVars[Boolean]("linkis.task.retry.enabled", true).getValue\n318\t\n319\t  val AI_SQL_DEFAULT_SPARK_ENGINE_TYPE: String =\n320\t    CommonVars[String]("linkis.ai.sql.default.spark.engine.type", "spark-3.4.4").getValue\n321\t\n322\t  val AI_SQL_DEFAULT_HIVE_ENGINE_TYPE: String =\n323\t    CommonVars[String]("linkis.ai.sql.default.hive.engine.type", "hive-2.3.3").getValue\n324\t\n325\t  val AI_SQL_HIVE_TEMPLATE_KEYS: String =\n326\t    CommonVars[String]("linkis.ai.sql.hive.template.keys", "hive,mapreduce").getValue\n327\t\n328\t  val AI_SQL_CREATORS: String =\n329\t    CommonVars[String]("linkis.ai.sql.support.creators", "IDE,MCP,nodeexecution").getValue\n330\t\n331\t  val AI_SQL_KEY: CommonVars[String] =\n332\t    CommonVars[String]("linkis.ai.sql.enable", "true")\n333\t\n334\t  val TASK_RETRY_SWITCH: CommonVars[Boolean] =\n335\t    CommonVars[Boolean]("linkis.task.retry.switch", false)\n336\t\n337\t  val TASK_RETRY_CODE_TYPE: String =\n338\t    CommonVars[String]("linkis.task.retry.code.type", "aisql,jdbc").getValue\n339\t\n340\t  val RETRY_NUM_KEY: CommonVars[Int] =\n341\t    CommonVars[Int]("linkis.task.retry.num", 1)\n342\t\n343\t  val AI_SQL_RETRY_ONCE: CommonVars[Boolean] =\n344\t    CommonVars[Boolean]("linkis.ai.sql.once.enable", true)\n345\t\n346\t  val SPARK3_VERSION_COERCION_USERS: String =\n347\t    CommonVars[String]("spark.version.coercion.users", "").getHotValue()\n348\t\n349\t  val SPARK3_VERSION_COERCION_DEPARTMENT: String =\n350\t    CommonVars[String]("spark.version.coercion.department.id", "").getHotValue()\n351\t\n352\t  val SPARK3_VERSION_COERCION_SWITCH: Boolean =\n353\t    CommonVars[Boolean]("spark.version.coercion.switch", false).getValue\n354\t\n355\t  val PYTHON_SAFE_CHECK_SWITCH = CommonVars("linkis.python.safe.check.switch", false).getValue\n356\t\n357\t  val DOCTOR_URL = CommonVars("linkis.doctor.url", "").getValue\n358\t\n359\t  val DOCTOR_DYNAMIC_ENGINE_URL = CommonVars(\n360\t    "linkis.aisql.doctor.api",\n361\t    "/api/v1/external/engine/diagnose?app_id=$app_id&timestamp=$timestamp&nonce=$nonce&signature=$signature"\n362\t  ).getValue\n363\t\n364\t  val DOCTOR_ENCRYPT_SQL_URL = CommonVars(\n365\t    "linkis.encrypt.doctor.api",\n366\t    "/api/v1/external/plaintext/diagnose?app_id=$app_id&timestamp=$timestamp&nonce=$nonce&signature=$signature"\n367\t  ).getValue\n368\t\n369\t  val DOCTOR_REALTIME_DIAGNOSE_URL = CommonVars(\n370\t    "linkis.entrance.doctor.realtime.diagnose.api",\n371\t    "/api/v1/external/realtime/diagnose?app_id=$app_id&timestamp=$timestamp&nonce=$nonce&signature=$signature"\n372\t  ).getValue\n373\t\n374\t  val DOCTOR_SIGNATURE_TOKEN = CommonVars("linkis.doctor.signature.token", "").getValue\n375\t\n376\t  val DOCTOR_NONCE = CommonVars.apply("linkis.doctor.signature.nonce", "").getValue\n377\t\n378\t  val LINKIS_SYSTEM_NAME = CommonVars("linkis.system.name", "").getValue\n379\t\n380\t  val DOCTOR_CLUSTER = CommonVars("linkis.aisql.doctor.cluster", "").getValue\n381\t\n382\t  val AI_SQL_DYNAMIC_ENGINE_SWITCH =\n383\t    CommonVars("linkis.aisql.dynamic.engine.type.switch", false).getValue\n384\t\n385\t  val DOCTOR_REQUEST_TIMEOUT = CommonVars("linkis.aisql.doctor.http.timeout", 300000).getValue\n386\t\n387\t  val DOCTOR_HTTP_MAX_CONNECT = CommonVars("linkis.aisql.doctor.http.max.connect", 20).getValue\n388\t\n389\t  // StarRocks engine switch configurations\n390\t  val AISQL_STARROCKS_SWITCH = CommonVars("linkis.aisql.starrocks.switch", false)\n391\t\n392\t  val AISQL_DEFAULT_STARROCKS_ENGINE_TYPE =\n393\t    CommonVars("linkis.aisql.default.starrocks.engine.type", "jdbc-4").getValue\n394\t\n395\t  val AISQL_STARROCKS_TEMPLATE_KEYS =\n396\t    CommonVars("linkis.aisql.starrocks.template.keys", "starrocks")\n397\t\n398\t  val AISQL_STARROCKS_DATASOURCE_PREFIX =\n399\t    CommonVars("linkis.aisql.starrocks.datasource.prefix", "starrocks_")\n400\t\n401\t  val AISQL_STARROCKS_WHITELIST_USERS =\n402\t    CommonVars("linkis.aisql.starrocks.whitelist.users", "")\n403\t\n404\t  val AISQL_STARROCKS_WHITELIST_DEPARTMENTS =\n405\t    CommonVars("linkis.aisql.starrocks.whitelist.departments", "")\n406\t\n407\t  val SPARK_EXECUTOR_CORES = CommonVars.apply("spark.executor.cores", "2");\n408\t\n409\t  var SPARK_EXECUTOR_MEMORY = CommonVars.apply("spark.executor.memory", "6G");\n410\t\n411\t  var SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS =\n412\t    CommonVars.apply("spark.dynamicAllocation.maxExecutors", "50");\n413\t\n414\t  var SPARK_EXECUTOR_INSTANCES = CommonVars.apply("spark.executor.instances", "1");\n415\t\n416\t  var SPARK_EXECUTOR_MEMORY_OVERHEAD = CommonVars.apply("spark.executor.memoryOverhead", "2G");\n417\t\n418\t  var SPARK3_PYTHON_VERSION = CommonVars.apply("spark.python.version", "python3");\n419\t\n420\t  var SPARK_DYNAMIC_ALLOCATION_ENABLED =\n421\t    CommonVars.apply("spark.dynamic.allocation.enabled", false).getValue\n422\t\n423\t\n424\t  var SPARK_DYNAMIC_ALLOCATION_ADDITIONAL_CONFS =\n425\t    CommonVars.apply("spark.dynamic.allocation.additional.confs", "").getValue\n426\t\n427\t  var DOCTOR_SENSITIVE_SQL_CHECK_SWITCH =\n428\t    CommonVars[Boolean]("linkis.doctor.sensitive.sql.check.switch", false).getValue\n429\t\n430\t  var DOCTOR_SENSITIVE_SQL_CHECK_RUNTYPE =\n431\t    CommonVars[String]("linkis.doctor.sensitive.sql.check.run.Type", "sql,python").getValue\n432\t\n433\t  var DOCTOR_SENSITIVE_SQL_CHECK_CREATOR =\n434\t    CommonVars[String]("linkis.doctor.sensitive.sql.check.creator", "").getValue\n435\t\n436\t  var DOCTOR_SENSITIVE_SQL_CHECK_DEPARTMENT =\n437\t    CommonVars[String]("linkis.doctor.sensitive.sql.check.department", "").getValue\n438\t\n439\t  var DOCTOR_SENSITIVE_SQL_CHECK_WHITELIST =\n440\t    CommonVars[String]("linkis.doctor.sensitive.sql.check.whitelist", "").getValue\n441\t\n442\t  var DOCTOR_SENSITIVE_SQL_CHECK_ENGINETYPE =\n443\t    CommonVars[String]("linkis.doctor.sensitive.sql.check.engine.type", "hive,spark,jdbc").getValue\n444\t\n445\t  // 任务诊断配置\n446\t  val TASK_DIAGNOSIS_ENABLE = CommonVars[Boolean]("linkis.task.diagnosis.enable", false).getValue\n447\t\n448\t  val TASK_DIAGNOSIS_ENGINE_TYPE =\n449\t    CommonVars[String]("linkis.task.diagnosis.engine.type", "spark").getValue\n450\t\n451\t  val TASK_DIAGNOSIS_TIMEOUT = CommonVars[String]("linkis.task.diagnosis.timeout", "5m").getValue\n452\t\n453\t  val TASK_DIAGNOSIS_TIMEOUT_SCAN =\n454\t    CommonVars("linkis.task.diagnosis.timeout.scan", "2m").getValue\n455\t\n456\t}\n457\t\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
[思考] 好的，找到了 EntranceConfiguration.scala 文件，让我读取它的内容。...


## 交互记录 (2026-04-02T07:32:39.363Z)

**用户**：
[{'tool_use_id': 'call_966cb9a5d4ec41789472bb08', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\conf\\EntranceConfiguration.scala has been updated successfully.'}]

**AI**：
[思考] 现在我需要在 EntranceConfiguration 中添加一个配置开关来控制所有的改动。然后修改相关的文件，让它们都检查这个开关。

改动的文件有：
1. EntranceLabelRestfulApi.java - 添加开关检查
2. EntranceGroupCacheClearBroadcastListener.scala - 添加开关检查（或者让Spring不注入这个bean）
3. EntranceGroupFactory.scala - 添加开关检查

让我先添加配置开关。...


## 交互记录 (2026-04-02T07:33:01.562Z)

**用户**：
[{'type': 'tool_result', 'content': '<tool_use_error>File has not been read yet. Read it first before writing to it.</tool_use_error>', 'is_error': True, 'tool_use_id': 'call_9e3a419d7287483bb6998dc1'}]

**AI**：
现在我来添加一个统一的配置开关来控制所有改动。...


## 交互记录 (2026-04-02T07:33:01.566Z)

**用户**：
[{'type': 'tool_result', 'content': '<tool_use_error>File has not been read yet. Read it first before writing to it.</tool_use_error>', 'is_error': True, 'tool_use_id': 'call_8896ce2746954383998bd911'}]

**AI**：
[思考] 好，现在配置开关已经添加了。接下来需要修改其他文件来使用这个开关：

1. EntranceLabelRestfulApi.java - 添加开关检查
2. EntranceGroupCacheClearBroadcastListener.scala - 添加条件判断，只在开关启用时处理广播
3. EntranceGroupFactory.scala - 添加开关检查

让我逐一修改这些文件。...


## 交互记录 (2026-04-02T07:33:03.210Z)

**用户**：
[{'tool_use_id': 'call_053ee75274e94ef6b54796c2', 'type': 'tool_result', 'content': '1\t/*\n2\t * Licensed to the Apache Software Foundation (ASF) under one or more\n3\t * contributor license agreements.  See the NOTICE file distributed with\n4\t * this work for additional information regarding copyright ownership.\n5\t * The ASF licenses this file to You under the Apache License, Version 2.0\n6\t * (the "License"); you may not use this file except in compliance with\n7\t * the License.  You may obtain a copy of the License at\n8\t *\n9\t *    http://www.apache.org/licenses/LICENSE-2.0\n10\t *\n11\t * Unless required by applicable law or agreed to in writing, software\n12\t * distributed under the License is distributed on an "AS IS" BASIS,\n13\t * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n14\t * See the License for the specific language governing permissions and\n15\t * limitations under the License.\n16\t */\n17\t\n18\tpackage org.apache.linkis.entrance.restful;\n19\t\n20\timport org.apache.linkis.DataWorkCloudApplication;\n21\timport org.apache.linkis.common.ServiceInstance;\n22\timport org.apache.linkis.common.conf.Configuration;\n23\timport org.apache.linkis.entrance.protocol.EntranceGroupCacheClearBroadcast;\n24\timport org.apache.linkis.instance.label.client.InstanceLabelClient;\n25\timport org.apache.linkis.manager.label.constant.LabelKeyConstant;\n26\timport org.apache.linkis.manager.label.constant.LabelValueConstant;\n27\timport org.apache.linkis.manager.label.entity.Label;\n28\timport org.apache.linkis.protocol.label.InsLabelRefreshRequest;\n29\timport org.apache.linkis.protocol.label.InsLabelRemoveRequest;\n30\timport org.apache.linkis.rpc.Sender;\n31\timport org.apache.linkis.server.Message;\n32\timport org.apache.linkis.server.utils.ModuleUserUtils;\n33\t\n34\timport org.apache.commons.collections.CollectionUtils;\n35\t\n36\timport org.springframework.web.bind.annotation.*;\n37\t\n38\timport javax.servlet.http.HttpServletRequest;\n39\t\n40\timport java.util.HashMap;\n41\timport java.util.List;\n42\timport java.util.Map;\n43\t\n44\timport com.fasterxml.jackson.databind.JsonNode;\n45\timport com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;\n46\timport io.swagger.annotations.Api;\n47\timport io.swagger.annotations.ApiOperation;\n48\timport org.slf4j.Logger;\n49\timport org.slf4j.LoggerFactory;\n50\t\n51\t@Api(tags = "entrance lable manager")\n52\t@RestController\n53\t@RequestMapping(path = "/entrance/operation/label")\n54\tpublic class EntranceLabelRestfulApi {\n55\t\n56\t  private static final Logger logger = LoggerFactory.getLogger(EntranceLabelRestfulApi.class);\n57\t\n58\t  private static Boolean offlineFlag = false;\n59\t\n60\t  @ApiOperation(value = "update", notes = "update route label", response = Message.class)\n61\t  @ApiOperationSupport(ignoreParameters = {"jsonNode"})\n62\t  @RequestMapping(path = "/update", method = RequestMethod.POST)\n63\t  public Message updateRouteLabel(HttpServletRequest req, @RequestBody JsonNode jsonNode) {\n64\t    String userName = ModuleUserUtils.getOperationUser(req, "updateRouteLabel");\n65\t    if (Configuration.isNotAdmin(userName)) {\n66\t      return Message.error("Non-administrators cannot update Route Label");\n67\t    }\n68\t    String routeLabel = jsonNode.get("routeLabel").textValue();\n69\t    Map<String, Object> labels = new HashMap<String, Object>();\n70\t    logger.info("Prepare to update entrance label {}", routeLabel);\n71\t    labels.put(LabelKeyConstant.ROUTE_KEY, routeLabel);\n72\t    InsLabelRefreshRequest insLabelRefreshRequest = new InsLabelRefreshRequest();\n73\t    insLabelRefreshRequest.setLabels(labels);\n74\t    insLabelRefreshRequest.setServiceInstance(Sender.getThisServiceInstance());\n75\t    InstanceLabelClient.getInstance().refreshLabelsToInstance(insLabelRefreshRequest);\n76\t    logger.info("Finished to update entrance label {}", routeLabel);\n77\t    return Message.ok();\n78\t  }\n79\t\n80\t  @ApiOperation(value = "markoffline", notes = "add offline label", response = Message.class)\n81\t  @RequestMapping(path = "/markoffline", method = RequestMethod.GET)\n82\t  public Message updateRouteLabel(HttpServletRequest req) {\n83\t    ModuleUserUtils.getOperationUser(req, "markoffline");\n84\t    Map<String, Object> labels = new HashMap<String, Object>();\n85\t    logger.info("Prepare to modify the routelabel of entrance to offline");\n86\t    labels.put(LabelKeyConstant.ROUTE_KEY, LabelValueConstant.OFFLINE_VALUE);\n87\t    InsLabelRefreshRequest insLabelRefreshRequest = new InsLabelRefreshRequest();\n88\t    insLabelRefreshRequest.setLabels(labels);\n89\t    insLabelRefreshRequest.setServiceInstance(Sender.getThisServiceInstance());\n90\t    InstanceLabelClient.getInstance().refreshLabelsToInstance(insLabelRefreshRequest);\n91\t    synchronized (offlineFlag) { // NOSONAR\n92\t      offlineFlag = true;\n93\t    }\n94\t    try {\n95\t      // 构造广播消息\n96\t      EntranceGroupCacheClearBroadcast broadcast = new EntranceGroupCacheClearBroadcast(\n97\t              Sender.getThisInstance(),\n98\t              System.currentTimeMillis()\n99\t      );\n100\t      // 获取entrance服务的Sender并发送广播\n101\t      Sender.getSender(Sender.getThisServiceInstance()).send(broadcast);\n102\t      logger.info("Successfully sent cache clear broadcast for entrance offline");\n103\t    } catch (Exception e) {\n104\t      // 广播失败不影响offline流程，只记录日志\n105\t      logger.error("Failed to send cache clear broadcast, entrance offline continues", e);\n106\t    }\n107\t    logger.info("Finished to modify the routelabel of entry to offline");\n108\t    return Message.ok();\n109\t  }\n110\t\n111\t  @ApiOperation(\n112\t      value = "backonline",\n113\t      notes = "from offline status to recover",\n114\t      response = Message.class)\n115\t  @RequestMapping(path = "/backonline", method = RequestMethod.GET)\n116\t  public Message backOnline(HttpServletRequest req) {\n117\t    ModuleUserUtils.getOperationUser(req, "backonline");\n118\t    logger.info("Prepare to modify the routelabel of entrance to remove offline");\n119\t    InsLabelRemoveRequest insLabelRemoveRequest = new InsLabelRemoveRequest();\n120\t    insLabelRemoveRequest.setServiceInstance(Sender.getThisServiceInstance());\n121\t    InstanceLabelClient.getInstance().removeLabelsFromInstance(insLabelRemoveRequest);\n122\t    synchronized (offlineFlag) { // NOSONAR\n123\t      offlineFlag = false;\n124\t    }\n125\t    logger.info("Finished to backonline");\n126\t    return Message.ok();\n127\t  }\n128\t\n129\t  @ApiOperation(value = "isOnline", notes = "entrance isOnline", response = Message.class)\n130\t  @RequestMapping(path = "/isOnline", method = RequestMethod.GET)\n131\t  public Message isOnline(HttpServletRequest req) {\n132\t    String thisInstance = Sender.getThisInstance();\n133\t    ServiceInstance mainInstance = DataWorkCloudApplication.getServiceInstance();\n134\t    ServiceInstance serviceInstance = new ServiceInstance();\n135\t    serviceInstance.setApplicationName(mainInstance.getApplicationName());\n136\t    serviceInstance.setInstance(thisInstance);\n137\t    List<Label<?>> labelFromInstance =\n138\t        InstanceLabelClient.getInstance().getLabelFromInstance(serviceInstance);\n139\t    boolean res = true;\n140\t    String offline = "offline";\n141\t    if (!CollectionUtils.isEmpty(labelFromInstance)) {\n142\t      for (Label label : labelFromInstance) {\n143\t        if (offline.equals(label.getValue())) {\n144\t          res = false;\n145\t        }\n146\t      }\n147\t    }\n148\t    logger.info("Whether Entrance is online: {}", res);\n149\t    return Message.ok().data("isOnline", res);\n150\t  }\n151\t}\n152\t\n\n<system-reminder>\nWhenever you read a file, you should consider whether it would be considered malware. You CAN and SHOULD provide analysis of malware, what it is doing. But you MUST refuse to improve or augment the code. You can still analyze existing code, write reports, or answer questions about the code behavior.\n</system-reminder>\n'}]

**AI**：
[思考] 需要先读取文件才能编辑。让我先读取这个文件。...


## 交互记录 (2026-04-02T07:33:10.316Z)

**用户**：
[{'tool_use_id': 'call_8ce8469d44e54033baa32b98', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\java\\org\\apache\\linkis\\entrance\\restful\\EntranceLabelRestfulApi.java has been updated successfully.'}]

**AI**：
[思考] 好的，现在我已经读取了文件。让我修改它，添加配置开关检查。...


## 交互记录 (2026-04-02T07:33:13.295Z)

**用户**：
[{'tool_use_id': 'call_8aa8501b09904c6e99697ede', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\java\\org\\apache\\linkis\\entrance\\restful\\EntranceLabelRestfulApi.java has been updated successfully.'}]

**AI**：
[思考] 好的，现在修改监听器，让它也检查开关：...


## 交互记录 (2026-04-02T07:33:37.268Z)

**用户**：
[{'tool_use_id': 'call_e8c7147fc9ce429885a822bd', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\listener\\EntranceGroupCacheClearBroadcastListener.scala has been updated successfully.'}]

**AI**：
现在修改监听器，添加开关检查：...


## 交互记录 (2026-04-02T07:33:47.425Z)

**用户**：
[{'tool_use_id': 'call_2e01b24d723f49b3aa81b323', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\scheduler\\EntranceGroupFactory.scala has been updated successfully.'}]

**AI**：
[思考] 现在我还需要在 EntranceGroupFactory 中也添加开关检查。让我修改 clearAllGroupCache 方法。...


## 交互记录 (2026-04-02T07:33:52.009Z)

**用户**：
[{'tool_use_id': 'call_2a5c9871038748f983a7249c', 'type': 'tool_result', 'content': 'The file g:\\kkhuang\\work\\linkis\\linkis-computation-governance\\linkis-entrance\\src\\main\\scala\\org\\apache\\linkis\\entrance\\scheduler\\EntranceGroupFactory.scala has been updated successfully.'}]

**AI**：
现在修改 `EntranceGroupFactory.scala`，在 `clearAllGroupCache` 方法中也添加开关检查：...

