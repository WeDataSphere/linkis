# REQ-01 修复资源管理资源展示为负问题 — 代码评审记录

> 版本：LinkisV2.1.5 ｜ 需求：REQ-01 ｜ DPMS 需求 ID：552808
> 评审对象提交：`ef1447b9a`（#AI commit# 开发阶段：修复资源管理资源展示为负问题）
> 评审日期：2026-10-21 ｜ 评审方式：静态代码评审（变更集全量逐文件走查）

## 一、评审范围

本次评审覆盖 REQ-01 变更集内的全部 6 个代码文件（后端 Scala 4 个 + 前端 Vue 2 个）：

| # | 文件 | 变更量 | 角色 |
| --- | --- | --- | --- |
| 1 | `linkis-application-manager/.../manager/rm/utils/RMUtils.scala` | +187 | 核心工具（负数归一 / 剩余重算 / 开关声明） |
| 2 | `linkis-application-manager/.../manager/rm/service/impl/DefaultResourceManager.scala` | 37 | 引擎退出扣减负值丢弃（O3） |
| 3 | `linkis-application-manager/.../manager/rm/restful/RMMonitorRest.scala` | 8 | 查询出口接入归一化（OC1） |
| 4 | `linkis-application-manager/.../test/.../rm/utils/RMUtilsTest.scala` | +212 | 单元测试 |
| 5 | `linkis-web/src/apps/linkis/module/resourceManagement/index.vue` | 24 | 前端展示归零与类型映射修正 |
| 6 | `linkis-web/src/apps/linkis/module/resourceManagement/engineConn.vue` | 3 | 前端展示对齐 |

## 二、评审结论

**评审结论：通过（passed）**
- `review_status`：passed
- 严重问题（critical）：0
- 高优先级问题（high）：0
- `should_block_deploy`：false

代码符合 CLAUDE.md 第 4 节铁律（功能开关 + 异常降级）与第 5 节代码规范；三个新增能力均由 `CommonVars` 开关守护，默认开启且关闭时精确回退原行为；逻辑为展示层/扣减层防御性改造，不改动底层数据结构与对外协议。

## 三、逐项评审要点

### 3.1 RMUtils.scala（核心工具）

- **新增三开关**：`linkis.manager.engine.exit.negative.resource.discard.enable`、`linkis.manager.rm.user.resource.negative.normalize.enable`、`linkis.manager.rm.user.resource.left.recompute.enable`，均 `CommonVars` 声明、默认 `true`，命名符合 §4.1.1（`*.enable` 语义），关闭时逐条 `return vo/or 走老分支`，回退精确、无副作用。符合 §4.1/§4.2。
- **`normalizeNegativeResource`**：null 入参直接返回（不抛 NPE）；对 `Map` 递归处理嵌套结构（兼容 `DriverAndYarn`），覆盖 Integer/Long/Double/Float/Short/Byte/BigDecimal/BigInteger 全数值类型并保持原类型归零；非数值字段（`queueName`/`applicationId` 等字符串）忽略不动，不会误改。原地修改语义在注释中显式声明（"原地修改,便于链式调用"）。
- **`recomputeUserResourceLeftNegative`**：`maxMap`/`usedMap` 为 null 时提前返回，保护到位；顺序为"used 归一 → left = max - used → left 负值归零"，与原 DB 快照口径解耦，仅影响展示出口，不改底层数据（符合"展示层生效、不动 DB"的设计约束）。
- **`subtractNumbers`/`zeroLike`/`normalizeNumberNegative`**：类型不一致时按 `double` 兜底，结果负值统一归零；`zeroLike` 覆盖全部数值类型并以 `Integer` 兜底，无空指针与类型转换异常路径。
- **风险与残余**：`computeLeftMap` 对 max 中存在但 used 中缺失的字段保留 max 值（注释已说明"已用资源无该字段：剩余=最大"），属预期语义，非缺陷。

### 3.2 DefaultResourceManager.scala（O3 扣减保护）

- **开关读取位置**：在 label 遍历前一次性读取 `discardNegativeResource`，避免循环内重复取值（性能友好）。
- **零值资源复用**：将原 `Resource.initResource(...)` 多次调用抽取为 `zeroResource` 复用，消重且语义更清晰。
- **扣减保护逻辑**：扣减前先算 `newUsedResource`，若"减后为负且开关开启"→ 丢弃本次 used 扣减、`logger.warn` 打印 label/currentUsed/releasedResource/计算结果/engineInstance 完整上下文，随后**继续处理 lockedResource 释放**（不因丢弃 used 扣减而中断后续释放），控制流正确、无遗漏。
- **空值防御**：新增 `if (null == labelResource.getUsedResource) labelResource.setUsedResource(zeroResource)`，消除原实现对 null 已用资源的潜在 NPE。属健壮性增强。
- **降级语义**：开关关闭时走原 `setUsedResource`/`setLeftResource` 分支，与老逻辑等价。
- **日志合规**：warn 日志仅打印资源数值与 label 标识，不含 token/密码/用户凭证，符合 §8.4 日志敏感信息禁令。

### 3.3 RMMonitorRest.scala（OC1 查询出口）

- 在 `getAllUserResource` 出口处，对每条 `UserResourceVo` 串联调用 `normalizeUserResourceVoNegative` → `recomputeUserResourceLeftNegative`，顺序正确（先归零 used 再算 left，避免用未归零的 used 推算出错误的 left）。
- 改动仅追加一层展示计算，接口签名、返回结构（`resources`/`total`）与分页逻辑均未变更，前端契约不变。

### 3.4 RMUtilsTest.scala（单元测试）

- 覆盖 `normalizeNegativeResource` 的"仅归零负数、非负数保持"、类型保持、嵌套结构、null 防御等分支；测试用真实 `java.lang.*` 数值类型构造，与生产入参类型一致。
- 测试为纯单测、无外部依赖，可稳定复现。

### 3.5 index.vue / engineConn.vue（前端）

- **归零阈值**：内存换算前增加 `Math.abs(data) < 1073741824 ? 0 : data/1024/1024/1024`，消除极小残值被渲染成科学计数法（如 `9.31e-9G`）的展示缺陷，阈值取 1G，语义合理。
- **类型映射修正**：`yarnType` 增加 `instances/instance → queueInstances` 分支，修复实例数列误取 `queueCores` 的映射错误。
- **0 值兜底修正**：`loadInstanceResource` 取值由 `||` 改为显式 `!== undefined && !== null` 判断，修复"合法值 0 被 `instances` 顶替"的逻辑缺陷（注释已说明）。
- 前端改动均为展示层，无路由/接口签名变更，符合 §3.2 前端约束；未引入 Vue/iview 大版本变更。

## 四、发现的问题清单

本次评审**未发现需阻断部署的问题**（critical_remaining: 0，high_remaining: 0）。

登记 2 项**低优先级改进建议**（不影响本次发布，供后续迭代参考）：

| 编号 | 级别 | 位置 | 说明 | 建议 |
| --- | --- | --- | --- | --- |
| CR-L1 | 低 | `DefaultResourceManager.scala` O3 扣减分支 | 丢弃 used 扣减时仅打印 warn，未做丢弃次数/资源量的度量打点 | 后续可考虑增加计数指标，便于观察该分支在生产的触发频次 |
| CR-L2 | 低 | `RMUtils.scala` `computeLeftMap` | used 缺失字段时 left 直接取 max，未区分"确实未使用"与"数据缺失" | 已有注释说明语义，如后续出现脏数据可再细化 |

## 五、结构化评审结果

```json
{
  "review_status": "passed",
  "critical_remaining": 0,
  "high_remaining": 0,
  "medium_remaining": 0,
  "low_remaining": 2,
  "should_block_deploy": false,
  "changed_files": [
    "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/utils/RMUtils.scala",
    "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/service/impl/DefaultResourceManager.scala",
    "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/restful/RMMonitorRest.scala",
    "linkis-computation-governance/linkis-manager/linkis-application-manager/src/test/scala/org/apache/linkis/manager/rm/utils/RMUtilsTest.scala",
    "linkis-web/src/apps/linkis/module/resourceManagement/index.vue",
    "linkis-web/src/apps/linkis/module/resourceManagement/engineConn.vue"
  ],
  "review_scope": "REQ-01 全量变更集（6 文件）",
  "review_method": "静态代码评审（变更集逐文件走查）",
  "reviewed_at": "2026-10-21"
}
```

## 六、评审人

- 评审执行：AI 代码评审（变更集静态走查）
- 评审依据：CLAUDE.md §4 新功能开发铁律 / §5 代码规范 / §8 协作要求
- 结论：**通过**，五项变更点（O3 开关守护、OC1 查询归零、left 重算、前端归零阈值、前端类型/0 值映射）均符合设计与规范要求。
