# 任务重试支持 StarRocks 设计文档

**需求类型**: ENHANCE
**风险等级**: 低
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端
**关联需求**: [REQ-05_任务重试支持StarRocks_需求.md](../requirements/REQ-05_任务重试支持StarRocks_需求.md)
**关联 commit**: `0aae08a4e`

---

## 0. 执行摘要

| 维度 | 内容 |
|------|------|
| **改造点** | `EntranceConfiguration.TASK_RETRY_CODE_TYPE` 默认值 `aisql,jdbc` → `aisql,jdbc,starrocks` |
| **机制** | `TaskRetryInterceptor` 已支持 codeType 列表 split + 匹配，日志文案已含 StarRocks，本次仅扩展默认值 |
| **风险** | 🟢 低，向后兼容，配置可覆盖 |

---

## 1. 设计背景

详见需求文档 §1。StarRocks 引擎接入后，其 `codeType=starrocks`，原默认值 `aisql,jdbc` 不命中，需扩展。

---

## 2. 设计方案

### 2.1 配置变更

```scala
// EntranceConfiguration.scala:336
val TASK_RETRY_CODE_TYPE: String =
  CommonVars[String]("linkis.task.retry.code.type", "aisql,jdbc,starrocks").getValue
```

### 2.2 拦截器逻辑（不变）

`TaskRetryInterceptor.apply` 已实现：
1. 读全局开关 `TASK_RETRY_SWITCH.getValue`；
2. 从 Label 提取 `codeType`；
3. `TASK_RETRY_CODE_TYPE.split(",").foreach` 逐个匹配；
4. 命中则注入 `TASK_RETRY_SWITCH.key` + `RETRY_NUM_KEY.key` 到 startupMap；
5. 日志 `LogUtils.generateWarn("The StarRocks task will initiate a failed retry")`。

本次仅扩展默认值，拦截器代码零改动。

### 2.3 向后兼容性

| 场景 | 行为 |
|------|------|
| 已部署环境未显式配置 codeType | 升级后默认值生效，StarRocks 自动命中 |
| 已部署环境显式配置 `linkis.task.retry.code.type=aisql,jdbc` | 配置优先，StarRocks 不命中（需运维感知） |
| 全局开关关闭 | 不受影响，拦截器直接返回 |

---

## 3. 关键代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `EntranceConfiguration.scala` | `:336` | `TASK_RETRY_CODE_TYPE` 默认值 |
| `TaskRetryInterceptor.scala` | `:54-66` | 拦截器匹配逻辑（未改动） |

---

## 4. 兼容性与影响

| 维度 | 影响 |
|------|------|
| API | 🟢 无 |
| 配置 | 🟡 默认值变更，显式配置优先 |
| 数据模型 | 🟢 无 |
| 性能 | 🟢 split + foreach O(1) |

---

## 5. 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `EntranceConfiguration.scala` | 修改 | 默认值扩展 |

---

## 6. 测试要点

1. **AC1** StarRocks 任务失败自动重试；
2. **AC2** AISQL/JDBC 仍支持；
3. **AC3** 全局开关关闭不重试；
4. **AC4** 显式配置覆盖默认值。
