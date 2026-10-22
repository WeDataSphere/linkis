# REQ-02 openfile拦截防OOM — 测试报告

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555374 ｜ 阶段：Stage 8 测试报告
> 测试用例：[REQ-02_测试用例.md](REQ-02_openfile拦截防OOM_测试用例.md)

---

## 1. 测试结论

| 维度 | 结果 | 说明 |
| --- | --- | --- |
| 代码自检 | ✅ 通过 | §4 铁律（开关+tryCatch+降级）、逻辑正确性、§10.2 不触及主线 |
| 单元测试 | ⏳ 待执行 | 6 用例（UT-01~06）已设计，待 SIT 环境运行 |
| 集成测试 | ⏳ 待执行 | 3 SIT（大文件超阈告警/小文件放行/拦截异常降级）待环境 |
| 回归测试 | ⏳ 待执行 | 3 回归（非resultset/80034-80036/collectBytes） |

**总体结论**：代码审查通过，可进入版本级回归。SIT 用例待环境执行后补录结果。

---

## 2. 代码改造审查

### 2.1 改造文件
| 文件 | 改动 | 自检 |
| --- | --- | --- |
| `WorkSpaceConfiguration.java` | 2 开关（limit.enable 默认 false / threshold 默认 100000） | ✅ |
| `FsRestfulApi.java` | openFile 拦截段（超阈告警+降级放行，不抛 80038） | ✅ 逻辑正确 |
| `WorkspaceExceptionManager.java` | 80038 错误码定义（保留，当前不抛） | ✅ 无冲突 |

### 2.2 §4 铁律自检
- [x] 开关默认 false（`limit.enable`）
- [x] try/catch 兜底（Throwable 降级放行）
- [x] 异常降级回退（拦截失败→放行原流程）
- [x] §10.2 不触及任务执行主线

### 2.3 关键风险消解
| 风险 | 消解措施 | 验证 |
| --- | --- | --- |
| 深翻页 OOM | (page-1)*pageSize 超阈告警 | ✅ 代码审查 |
| 拦截阻断 openFile | 超阈仅告警不抛 80038，降级放行 | ✅ 代码审查 |
| 与既有校验冲突 | 80034/80036 优先，80038 定义无冲突 | ✅ 代码审查 |

---

## 3. SIT 待执行项

| 用例 | 环境 | 优先级 |
| --- | --- | --- |
| SIT-01 大文件超阈告警不阻断 | pes-publicservice SIT | P0 |
| SIT-02 小文件正常放行 | pes-publicservice SIT | P1 |
| SIT-03 拦截异常降级 | pes-publicservice SIT | P1 |
