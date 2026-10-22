# REQ-02 openfile拦截防OOM — 测试用例

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555374 ｜ 阶段：Stage 6 测试验证
> 输入：测试澄清纪要 + 设计文档 §9 + 代码改造（FsRestfulApi.openFile 拦截段）

---

## 单元测试 — FsRestfulApiOpenFileLimitTest

### UT-01: 开关关-超阈走老路径告警
- **开关**: `limit.enable=false`
- **输入**: page=1000, pageSize=5000（accRows=4995000 > 100000）
- **预期**: 不抛异常，打 warn 日志（"翻页累计行数超过阈值，开关未开启，仅告警"），openFile 走原流程

### UT-02: 开关开-未超阈通过
- **开关**: `limit.enable=true`
- **输入**: page=2, pageSize=5000（accRows=5000 < 100000）
- **预期**: 通过，无 warn，正常 openFile

### UT-03: 开关开-边界相等通过
- **开关**: `limit.enable=true`
- **输入**: page=21, pageSize=5000（accRows=100000 == 阈值，未超）
- **预期**: 通过（`accRows > threshold` 为 false），正常 openFile

### UT-04: 开关开-超阈告警降级放行
- **开关**: `limit.enable=true`
- **输入**: page=22, pageSize=5000（accRows=105000 > 100000）
- **预期**: 打 warn 日志（"翻页累计行数超过阈值，开关已开启，降级放行不阻断"），**不抛 80038**，openFile 走原流程

### UT-05: 开关关-超阈仅告警
- **开关**: `limit.enable=false`
- **输入**: page=22, pageSize=5000（accRows=105000 > 100000）
- **预期**: 打 warn 日志（"开关未开启，仅告警"），降级放行

### UT-06: 与80034共存
- **开关**: `limit.enable=true`
- **输入**: page=2, pageSize=20000（pageSize > 10000）
- **预期**: 先命中 80034（`FILESYSTEM_RESULTSET_ROW_LIMIT`），不进入新拦截段

---

## 集成测试 — SIT

### SIT-01: 大文件超阈告警不阻断
- **环境**: linkis-pes-publicservice SIT
- **步骤**: 构造 >100000 行 resultset，openFile page=22 pageSize=5000
- **预期**: 收到 warn 日志，openFile 正常返回内容（降级放行）

### SIT-02: 小文件正常放行
- **步骤**: 常规 resultset 翻前几页（page=1~3）
- **预期**: 无 warn，正常返回

### SIT-03: 拦截异常降级
- **步骤**: mock `threshold` 读取抛异常
- **预期**: warn 日志（"翻页校验失败，降级放行"）+ 放行原流程

---

## 回归测试

### REG-01: 非resultset文件不受影响
- **预期**: 普通文本/日志走 80032 分支（文件大小检查），不进入新拦截

### REG-02: 既有80034/80036校验保持
- **预期**: pageSize/columnPage 范围校验优先于新拦截，保持不变

### REG-03: collectBytes限制保持
- **预期**: `FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED` 保持不变
