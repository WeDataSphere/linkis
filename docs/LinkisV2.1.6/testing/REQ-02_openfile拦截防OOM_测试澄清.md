# REQ-02 openfile拦截防OOM — 测试用例澄清纪要

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555374 ｜ 阶段：Stage 5.5 测试用例澄清
> 输入：需求文档 + 设计文档 §9 测试设计 + Stage 1.1 触点①风险测试要求（R-001 模块链路 / R-002 性能容量 / R-003 参数配置）

---

## 1. 测试范围确认

### 1.1 核心测试目标

| 目标 | 验证点 | 优先级 |
| --- | --- | --- |
| 超阈值告警不阻断 | `(page-1)*pageSize > threshold` 打 warn 日志，openFile 正常返回（降级放行） | P0 |
| 开关关闭仅告警 | `limit.enable=false` 时超阈值仍打 warn 日志 | P0 |
| 未超阈值零影响 | `(page-1)*pageSize <= threshold` 正常 openFile，无 warn | P1 |
| 拦截异常降级 | 拦截逻辑自身抛 Throwable → warn 日志 + 放行原流程 | P1 |
| 与既有 80034 共存 | pageSize 超限先命中 80034，不进入新拦截 | P1 |
| 阈值可配 | `limit.threshold` 调整后生效 | P2 |
| 非 resultset 文件不受影响 | 普通文本/日志走 80032 分支 | P1 |

### 1.2 风险驱动必测项（来自触点①）

| 风险 ID | 风险类型 | 等级 | 必测要求 |
| --- | --- | --- | --- |
| R-001 | 模块链路 | 高 | openFile 链路：超阈值告警后 openFile 仍可用（降级放行，不阻断） |
| R-002 | 性能容量 | 高 | 大结果集深翻页不 OOM（超阈值告警，避免深翻页读全量） |
| R-003 | 参数配置 | 高 | 开关 `limit.enable`(默认false) / 阈值 `limit.threshold`(默认100000) 配置生效 |

### 1.3 不测项

- 前端（本需求纯后端）
- §10.2 任务执行主线（openFile 属公共服务，不在 10 步主线内）

---

## 2. 测试用例清单

### 2.1 单元测试（`FsRestfulApiOpenFileLimitTest`，6 用例）

| 用例 ID | 场景 | 开关 | 输入(page,pageSize) | accRows | 预期 |
| --- | --- | --- | --- | --- | --- |
| UT-01 | 开关关-走老路径 | false | (1000,5000) | 4995000 | 不抛异常，打 warn（超阈），openFile 走原流程 |
| UT-02 | 开关开-未超阈 | true | (2,5000) | 5000 | 通过，无 warn，正常 openFile |
| UT-03 | 开关开-边界相等 | true | (21,5000) | 100000 | 通过（未超），正常 openFile |
| UT-04 | 开关开-超阈告警 | true | (22,5000) | 105000 | 打 warn（超阈+开关开），降级放行不抛 80038 |
| UT-05 | 开关关-超阈仅告警 | false | (22,5000) | 105000 | 打 warn（超阈+开关关），降级放行 |
| UT-06 | 与 80034 共存 | true | (2,20000) | 5000 | 先命中 80034（pageSize>10000），不进入新拦截 |

### 2.2 集成测试

| 用例 ID | 场景 | 步骤 | 预期 |
| --- | --- | --- | --- |
| SIT-01 | 大文件超阈告警不阻断 | 构造 >100000 行结果集，openFile page=22 pageSize=5000 | 收到 warn 日志，openFile 正常返回内容（降级放行） |
| SIT-02 | 小文件正常放行 | 常规结果集翻前几页 | 无 warn，正常返回 |
| SIT-03 | 拦截异常降级 | mock threshold 读取抛异常 | warn 日志 + 放行原流程 |

### 2.3 回归测试

| 用例 ID | 场景 | 预期 |
| --- | --- | --- |
| REG-01 | 非 resultset 文件 | 走 80032 分支（文件大小检查），不受新拦截影响 |
| REG-02 | 既有 80034/80036 校验 | 保持不变，优先于新拦截 |
| REG-03 | collectBytes 限制 | `FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED` 保持不变 |

---

## 3. 测试环境要求

- linkis-pes-publicservice 部署环境（SIT）
- 构造大结果集文件（>100000 行 resultset）
- 开关 `linkis.workspace.openfile.limit.enable`（on/off 两种状态）

---

## 4. 澄清结论

- 测试范围：超阈告警不阻断 + 开关 on/off + 阈值边界 + 降级路径 + 与既有校验共存
- 风险必测项 R-001/R-002/R-003 已覆盖
- **关键**：当前实现为"告警+降级放行"，80038 错误码定义保留但当前不抛出（供后续硬阻断灰度预留）
- 后续 Stage 6 functional-test-generator 按本纪要生成正式测试用例
