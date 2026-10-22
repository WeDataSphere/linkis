# REQ-01 修复Hive引擎kerberos认证失败 — 测试用例澄清纪要

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555373 ｜ 阶段：Stage 5.5 测试用例澄清
> 输入：需求文档 + 设计文档 §2.4 测试策略 + Stage 1.1 触点①风险测试要求（R-001 模块链路 / R-002 参数配置）

---

## 1. 测试范围确认

### 1.1 核心测试目标

| 目标 | 验证点 | 优先级 |
| --- | --- | --- |
| TGT 原地续期有效性 | `reloginFromKeytab` 后 TGT 有效、UGI 引用不变 | P0 |
| 续期失败降级兜底 | 非 keytab / TGT 仍无效 / relogin 异常 → 换新 UGI 兜底 | P0 |
| 不清 FS 缓存无副作用 | 原地续期后 FS 缓存天然复用新 TGT，不触发 Filesystem closed | P0 |
| 失败计数降级 | 连续失败 <3 返回原 UGI / >=3 抛异常 | P1 |
| 非 Kerberos 环境零影响 | `!isSecurityEnabled()` 直接返回 | P1 |
| Hive 引擎持续可用 | 突破 24h TGT 过期后认证失败限制 | P0 |
| 非 Hive 引擎不受影响 | JDBC/HBase 走 `refreshLoginUserTgtIfNeeded` 独立路径 | P1 |

### 1.2 风险驱动必测项（来自触点①）

| 风险 ID | 风险类型 | 等级 | 必测要求 |
| --- | --- | --- | --- |
| R-001 | 模块链路 | 高 | Hive HDFS 认证链路：TGT 过期→续期→HDFS 操作成功（覆盖原地续期+兜底两条路径） |
| R-002 | 参数配置 | 高 | 开关配置：`ENGINE_TGT_REFRESH_ENABLE` / `ENGINE_TGT_REFRESH_CLEAR_FS_CACHE`（默认false）/ `LINKIS_KERBEROS_REFRESH_INTERVAL` |

### 1.3 不测项

- 非 Kerberos 集群的 openFile/SQL 执行（不涉及 TGT 刷新）
- ECM/AM 主流程（本需求不触及 §10.2 任务执行主线）

---

## 2. 测试用例清单

### 2.1 单元测试（`KerberosTgtUtilsTest`，4 场景）

| 用例 ID | 场景 | 输入 | 预期 |
| --- | --- | --- | --- |
| UT-01 | TGT 有效不续期 | ugi TGT 有效 | 返回同一 ugi 引用，failCount=0 |
| UT-02 | 原地续期成功 | ugi TGT 过期 + isFromKeytab + relogin 成功 | TGT 有效，返回同一 ugi 引用，failCount=0，**不清 FS 缓存** |
| UT-03 | 续期失败降级兜底 | ugi 非 keytab / relogin 后 TGT 仍无效 / relogin 抛异常 | 降级 `HDFSUtils.getUserGroupInformation` 换新 UGI，failCount=0 |
| UT-04 | 连续失败 3 次报错 | 兜底连续失败 3 次 | 第 3 次抛 RuntimeException |

### 2.2 集成测试（SIT，Kerberos 环境）

| 用例 ID | 场景 | 步骤 | 预期 |
| --- | --- | --- | --- |
| SIT-01 | 24h 持续可用 | 开启 `tgt.refresh.enable=true`，Hive 引擎跑 SQL，等待 TGT 过期（缩短 interval 加速） | TGT 刷新后 SQL 正常执行，HDFS 认证成功，引擎存活 >24h |
| SIT-02 | 并发无 Filesystem closed | 同一 Hive 引擎并发跑 ≥3 个 SQL，TGT 刷新期间触发 | 无 `Filesystem closed` 异常，所有任务成功 |
| SIT-03 | 续期失败降级 | 模拟 keytab 丢失（relogin 失败）→ 换新 UGI 兜底 | 兜底成功则 SQL 继续；连续 3 次失败则引擎退出并报错 |

### 2.3 回归测试

| 用例 ID | 场景 | 预期 |
| --- | --- | --- |
| REG-01 | 非 Kerberos 环境 | `isSecurityEnabled=false`，`refreshUgiIfNeeded` 直接返回，行为不变 |
| REG-02 | JDBC/HBase 引擎 | 走 `refreshLoginUserTgtIfNeeded` 独立路径，不受 Hive 改造影响 |
| REG-03 | `clear.fs.cache` 开关 on | 原地续期成功后不清缓存（代码已去掉 closeAllForUGI），开关无效化，无副作用 |

---

## 3. 测试环境要求

- Kerberos 认证集群（SIT），keytab 可用
- Hive 引擎（`linkis-cg-engineconn`），开启 `linkis.engineconn.tgt.refresh.enable=true`
- TGT 刷新 interval 缩短至 60s（加速 24h 验证，`linkis.kerberos.refresh.interval=60`）
- 并发执行环境（`HiveEngineConcurrentConnExecutor`）

---

## 4. 澄清结论

- 测试范围：原地续期 + 降级兜底 + 不清缓存无副作用 + 24h 持续可用 + 并发安全
- 风险必测项 R-001/R-002 已覆盖
- 不涉及前端、不涉及 §10.2 主流程回归
- 后续 Stage 6 functional-test-generator 按本纪要生成正式测试用例
