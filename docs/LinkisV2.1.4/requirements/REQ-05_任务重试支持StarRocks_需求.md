# 任务重试支持 StarRocks 需求文档

**需求类型**: ENHANCE（功能增强）
**风险等级**: 低风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端
**关联 commit**: `0aae08a4e`

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | `linkis.task.retry.code.type` 默认值从 `aisql,jdbc` 扩展为 `aisql,jdbc,starrocks`，使 StarRocks 引擎任务支持失败自动重试 |
| **改动服务** | linkis-entrance（单模块单配置） |
| **改动范围** | `EntranceConfiguration.scala` 的 `TASK_RETRY_CODE_TYPE` 默认值 |
| **开关** | 依赖全局 `linkis.task.retry.switch`（默认 false），本需求仅扩展 codeType 默认值 |
| **影响链路** | `TaskRetryInterceptor.apply` → 匹配 codeType → 注入 retry 参数 |

---

## 1. 需求背景

### 1.1 业务背景

Linkis 任务重试机制通过 `TaskRetryInterceptor` 拦截器实现：全局开关 `linkis.task.retry.switch` 开启时，按任务 `codeType`（从 Label 提取）匹配 `linkis.task.retry.code.type` 配置的列表，命中则在 startup params 注入 retry 参数，任务失败后自动重试 `linkis.task.retry.num` 次。

原默认值为 `aisql,jdbc`，仅 AI SQL 和 JDBC 引擎支持重试。StarRocks 引擎（通过 `jdbc-starrocks` 执行）接入后，生产要求其失败任务也支持自动重试，但 StarRocks 任务的 `codeType=starrocks` 不在默认列表，需手动配置。

### 1.2 目标

1. 将 `linkis.task.retry.code.type` 默认值扩展为 `aisql,jdbc,starrocks`；
2. 保持配置可运行时覆盖，已部署环境无需改代码；
3. 不改 `TaskRetryInterceptor` 逻辑（已支持 StarRocks，日志文案已含 "The StarRocks task"）。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台运维**，我希望全局重试开关开启后，StarRocks 任务失败能自动重试，减少临时故障导致的任务失败 |
| US-2 | 作为**开发**，希望 codeType 默认值包含 starrocks，避免每个环境都要手动配置 |

---

## 3. 功能描述

### 3.1 功能点

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | codeType 默认值扩展 | `EntranceConfiguration.TASK_RETRY_CODE_TYPE` 默认值 `aisql,jdbc` → `aisql,jdbc,starrocks` |
| F-2 | 运行时可覆盖 | 通过 `linkis.task.retry.code.type` 配置项可覆盖默认值，按逗号分隔 |
| F-3 | 拦截器逻辑不变 | `TaskRetryInterceptor.apply` 已支持 codeType 列表 split + 逐个匹配，无需改动 |

### 3.2 执行链路

```
JobRequest → TaskRetryInterceptor.apply
  ├─ 全局开关 TASK_RETRY_SWITCH.getValue 关闭 → 直接返回（不重试）
  └─ 开启
       ├─ codeType = LabelUtil.getCodeType(labels)   ← StarRocks 任务为 "starrocks"
       ├─ TASK_RETRY_CODE_TYPE.split(",").foreach
       │    └─ 匹配 codeTypeConf == codeType
       │         ├─ 命中 → 注入 retry switch + num 到 startupMap
       │         └─ 日志 "The StarRocks task will initiate a failed retry"
       └─ TaskUtils.addStartupMap
```

---

## 4. 验收标准

### AC1: StarRocks 任务支持重试

**前置条件**: `linkis.task.retry.switch=true`，`linkis.task.retry.num=1`，codeType 默认值

**操作步骤**:
1. 提交一个 StarRocks 任务（codeType=starrocks）
2. 任务执行失败
3. 观察重试行为

**预期结果**:
- 拦截器日志含 "The StarRocks task will initiate a failed retry"
- startup params 含 `linkis.task.retry.switch=true` + `linkis.task.retry.num=1`
- 任务失败后自动重试 1 次

### AC2: AISQL/JDBC 仍支持重试

**预期结果**: codeType=aisql/jdbc 任务仍命中重试（原行为不变）

### AC3: 全局开关关闭不重试

**前置条件**: `linkis.task.retry.switch=false`

**预期结果**: 所有 codeType 任务都不重试，拦截器直接返回

### AC4: codeType 可覆盖

**前置条件**: `linkis.task.retry.code.type=aisql`（覆盖默认）

**预期结果**: StarRocks 任务不命中，不重试

---

## 5. 配置说明

| Key | 默认值（改造后） | 说明 |
|-----|------------------|------|
| `linkis.task.retry.code.type` | `aisql,jdbc,starrocks` | 支持重试的 codeType 列表，逗号分隔 |
| `linkis.task.retry.switch` | `false` | 全局重试开关 |
| `linkis.task.retry.num` | `1` | 重试次数 |

配置位置: `linkis-entrance/.../conf/EntranceConfiguration.scala`

---

## 6. 风险识别

| 风险 | 等级 | 说明 |
|------|:----:|------|
| StarRocks 重试引发数据重复 | 🟡 | 非幂等 SQL 重试可能重复写入，属业务侧需保证幂等，本需求仅开启机制 |
| 配置覆盖遗漏 | 🟢 | 已部署环境若显式配置了 codeType，默认值变更不影响（配置优先） |

---

## 7. 关联影响分析

| 维度 | 影响 |
|------|------|
| 功能模块 | 🟢 仅 entrance 配置默认值 |
| 数据模型 | 🟢 无 |
| 性能 | 🟢 拦截器 split + foreach，O(1) |
| 兼容性 | 🟢 向后兼容，原 aisql/jdbc 仍命中 |

---

## 附录

### A.1 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `EntranceConfiguration.scala` | 修改 | `TASK_RETRY_CODE_TYPE` 默认值扩展 |

### A.2 关联文档

- 设计文档: [REQ-05_任务重试支持StarRocks_设计.md](../design/REQ-05_任务重试支持StarRocks_设计.md)
