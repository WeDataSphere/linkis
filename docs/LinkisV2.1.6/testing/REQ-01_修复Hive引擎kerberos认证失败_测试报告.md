# REQ-01 修复Hive引擎kerberos认证失败 — 测试报告

> 版本：LinkisV2.1.6 ｜ dpmsStoryId：555373 ｜ 阶段：Stage 8 测试报告
> 测试用例：[REQ-01_测试用例.md](REQ-01_修复Hive引擎kerberos认证失败_测试用例.md)

---

## 1. 测试结论

| 维度 | 结果 | 说明 |
| --- | --- | --- |
| 代码自检 | ✅ 通过 | §4 铁律（开关+tryCatch+降级）、逻辑正确性、§10.2 不触及主线 |
| 单元测试 | ⏳ 待执行 | 4 用例（UT-01~04）已设计，待 SIT 环境运行 |
| 集成测试 | ⏳ 待执行 | 3 SIT（24h持续可用/并发无Filesystem closed/续期失败降级）待 Kerberos 环境 |
| 回归测试 | ⏳ 待执行 | 3 回归（非Kerberos/JDBC-HBase/clear.fs.cache开关） |

**总体结论**：代码审查通过，可进入版本级回归。SIT 用例待 Kerberos 环境执行后补录结果。

---

## 2. 代码改造审查

### 2.1 改造文件
| 文件 | 改动 | 自检 |
| --- | --- | --- |
| `KerberosTgtUtils.java` | `refreshUgiIfNeeded` 三层防线（原地续期+降级兜底+不清缓存） | ✅ 逻辑正确，引用一致 |
| `HadoopConf.scala` | `ENGINE_TGT_REFRESH_CLEAR_FS_CACHE` 开关默认 false | ✅ 并发安全 |

### 2.2 §4 铁律自检
- [x] 开关默认 false（`ENGINE_TGT_REFRESH_ENABLE` / `CLEAR_FS_CACHE`）
- [x] try/catch 兜底（relogin 失败→降级换新 UGI）
- [x] 异常降级回退（连续失败 <3 返回原 UGI）
- [x] §10.2 不触及任务执行主线

### 2.3 关键风险消解
| 风险 | 消解措施 | 验证 |
| --- | --- | --- |
| TGT 刷新成功但 HDFS 认证失败 | 原地 reloginFromKeytab 不换引用 | ✅ 代码审查 |
| Filesystem closed（并发清缓存） | 去掉 closeAllForUGI，FS 缓存天然复用 | ✅ 代码审查 |
| 续期失败直接报错 | 降级到换新 UGI 兜底 | ✅ 代码审查 |

---

## 3. SIT 待执行项

| 用例 | 环境 | 优先级 |
| --- | --- | --- |
| SIT-01 24h持续可用 | Kerberos SIT，interval=60s | P0 |
| SIT-02 并发无Filesystem closed | Kerberos SIT，并发≥3任务 | P0 |
| SIT-03 续期失败降级 | Kerberos SIT，移除keytab | P1 |
