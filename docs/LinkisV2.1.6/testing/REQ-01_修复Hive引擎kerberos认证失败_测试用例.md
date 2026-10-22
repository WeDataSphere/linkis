# REQ-01 修复Hive引擎kerberos认证失败 — 测试用例

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555373 ｜ 阶段：Stage 6 测试验证
> 输入：测试澄清纪要 + 设计文档 §2.4 + 代码改造（KerberosTgtUtils.refreshUgiIfNeeded）

---

## 单元测试 — KerberosTgtUtilsTest

### UT-01: TGT有效不续期
- **前置**: `UserGroupInformation.isSecurityEnabled()=true`，ugi TGT 有效
- **步骤**: 调用 `refreshUgiIfNeeded(ugi, userName)`
- **预期**: 返回同一 ugi 引用（`result == ugi`），`ugiRefreshFailCount=0`，不调用 reloginFromKeytab

### UT-02: 原地续期成功不清缓存
- **前置**: ugi TGT 过期，`ugi.isFromKeytab()=true`，`reloginFromKeytab` 成功后 TGT 有效
- **步骤**: 调用 `refreshUgiIfNeeded(ugi, userName)`
- **预期**: 返回同一 ugi 引用，TGT 有效，`ugiRefreshFailCount=0`，**不调用 `FileSystem.closeAllForUGI`**（防 Filesystem closed）

### UT-03: 续期失败降级兜底
- **前置**: ugi 非 keytab / relogin 后 TGT 仍无效 / relogin 抛异常
- **步骤**: 调用 `refreshUgiIfNeeded(ugi, userName)`
- **预期**: 降级到 `HDFSUtils.getUserGroupInformation(userName)` 换新 UGI，返回 newUgi，`ugiRefreshFailCount=0`

### UT-04: 连续失败3次报错
- **前置**: mock 兜底 `getUserGroupInformation` 连续返回 null / 抛异常 3 次
- **步骤**: 连续调用 `refreshUgiIfNeeded` 3 次
- **预期**: 第 3 次抛 `RuntimeException`（"TGT refresh has failed consecutively for 3 times"）

---

## 集成测试 — SIT（Kerberos 环境）

### SIT-01: 24h持续可用
- **环境**: Kerberos SIT，`linkis.engineconn.tgt.refresh.enable=true`，`linkis.kerberos.refresh.interval=60`（加速）
- **步骤**: Hive 引擎跑 SQL，等待 TGT 过期→续期→继续跑 SQL，持续 >24h
- **预期**: TGT 刷新后 SQL 正常执行，HDFS 认证成功，引擎存活 >24h，无认证失败

### SIT-02: 并发无Filesystem closed
- **环境**: 同 SIT-01
- **步骤**: 同一 Hive 引擎并发跑 ≥3 个 SQL，TGT 刷新期间触发（interval=60s）
- **预期**: 无 `java.io.IOException: Filesystem closed`，无 `SemanticException`，所有任务成功

### SIT-03: 续期失败降级
- **环境**: Kerberos SIT，临时移除 keytab（relogin 失败）
- **步骤**: 触发 TGT 过期→relogin 失败→换新 UGI 兜底
- **预期**: 兜底成功则 SQL 继续；连续 3 次失败则引擎退出并报错

---

## 回归测试

### REG-01: 非Kerberos环境零影响
- **前置**: `isSecurityEnabled=false`
- **预期**: `refreshUgiIfNeeded` 直接返回原 ugi，行为不变

### REG-02: JDBC/HBase引擎独立路径
- **预期**: 走 `refreshLoginUserTgtIfNeeded`，不受 Hive 改造影响

### REG-03: clear.fs.cache开关无效化
- **预期**: 代码已去掉 closeAllForUGI 调用，开关 on/off 均不清缓存，无副作用
