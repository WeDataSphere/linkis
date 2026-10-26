> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/5rk9dzJvKBiMzjqx)

# 修复 Hive 引擎 Kerberos 认证失败问题分析报告

**需求类型**: FIX（Bug 修复）
**问题等级**: P1（高）
**文档版本**: v1.0
**创建日期**: 2026-07-07
**所属版本**: Linkis 2.1.6（分支 `release_v2.1.6`）
**功能属性**: 后端（接口/业务逻辑/服务/数据处理，不涉及前端）
**前端开发类型**: 不涉及前端
**DPMS Story ID**: 555373

---

## 📋 问题速览

| 维度 | 内容 |
|-----|------|
| **问题概述** | 开启 Kerberos 认证的集群中，Hive 引擎执行 24 小时后，TGT 刷新成功但 HDFS 认证仍然失败 |
| **紧急程度** | P1-高（影响长时间运行的 Hive 引擎持续可用性） |
| **影响范围** | 所有开启 Kerberos 认证集群环境中的 Hive 引擎，运行超过 24 小时后任务报认证失败 |
| **问题模块** | `linkis-commons/linkis-hadoop-common`（KerberosTgtUtils）、`linkis-engineconn-plugins/hive`（HiveExecutor） |
| **复现概率** | 必现（Kerberos 环境 + 引擎存活 > 24h + TGT 刷新后访问 HDFS） |
| **发现环境** | 生产环境（Kerberos 认证集群） |

> 💡 **阅读指引**：速览问题看本卡片 -> 根因分析看第二章 -> 修复方案看第三章 -> 配置说明看第六章

---

## 1. 需求背景

### 1.1 业务背景

在开启 Kerberos 认证的 Hadoop 集群环境中，KDC 每小时自动刷新一次 Kerberos 票据。Linkis Hive 引擎（EngineConn）作为长驻进程，在初始化时通过 `HDFSUtils.getUserGroupInformation` 获取 UGI（UserGroupInformation）对象并持有，后续每次执行 SQL 时通过 `ugi.doAs()` 访问 HDFS。

**问题现象**：Hive 引擎在执行 24 小时后，出现 Kerberos 认证异常 TGT。表现为：Kerberos 刷新成功（日志显示 UGI refreshed successfully），但 HDFS 认证失败（`FileSystem.get()` 命中的缓存 FS 实例使用的是旧 TGT 换取的 delegation token，已过期）。

**影响面**：
- 所有开启 Kerberos 认证的集群环境
- 仅影响 Hive 引擎（JDBC/HBase 引擎使用 JVM 全局 loginUser，走 `refreshLoginUserTgtIfNeeded`，不受此 Bug 影响）
- 引擎存活超过 24 小时后，后续所有 SQL 均会认证失败，必须重启引擎才能恢复

### 1.2 根因分析

#### 5Why 分析

| 层级 | 问题 | 答案 |
|:----:|------|------|
| Why 1 | 为什么 HDFS 认证失败？ | 因为 `FileSystem.get()` 命中了 Hadoop FS 缓存中的旧 FS 实例，其 delegation token 是首次执行时用旧 TGT 换的，24h 后已过期 |
| Why 2 | 为什么 TGT 刷新成功但 FS 缓存还是旧的？ | 因为 `refreshUgiIfNeeded` 通过 `loginUserFromKeytabAndReturnUGI` 创建了一个**全新的独立 UGI 对象**并替换 executor 持有的引用，但 Hive 真正访问 HDFS 时走的是 `FileSystem.get()` -> 命中 FS 缓存 -> 旧 FS 实例 -> 旧 delegation token |
| Why 3 | 为什么换新 UGI 会导致 FS 缓存不一致？ | `loginUserFromKeytabAndReturnUGI` 创建的全新 UGI **不设置全局 loginUser**，也**不 spawn Hadoop 自动续期线程**。新 UGI 的 TGT 是有效的，但 Hadoop 的 FS 缓存是按 UGI/Subject 维度管理的——旧 FS 缓存实例仍绑定在旧 UGI 的 Subject 上，其 delegation token 不会被新 UGI 的 TGT 续期 |
| Why 4 | 为什么没有清理旧 FS 缓存？ | `refreshUgiIfNeeded` 只换了 executor 持有的 UGI 引用，没有调用 `FileSystem.closeAllForUGI(ugi)` 清理旧 delegation token 缓存。这是原始实现的遗漏 |
| Why 5 | **根本原因是什么？** | 原始实现采用了"换新 UGI 对象"的策略（注释中写明"to avoid concurrency issues"），但这导致了三层不一致：① UGI 对象引用漂移（executor 持有的引用变了）；② FS 缓存绑定在旧 UGI 的 Subject 上（新 UGI 无法继承）；③ delegation token 过期（旧 TGT 换的 token 不会自动刷新）。正确做法应该是**原地续期**（`reloginFromKeytab` 在同一 UGI/Subject 上重新 login），保持对象引用不变，并清理旧 FS 缓存 |

#### 根因确认

- **直接原因**: `refreshUgiIfNeeded` 换新 UGI 后，Hadoop FS 缓存中的旧 FS 实例仍持有过期 delegation token
- **根本原因**: "换新 UGI"策略导致 UGI 引用漂移 + FS 缓存不一致 + delegation token 不会随新 TGT 自动刷新
- **相关代码**: `KerberosTgtUtils.java` 第 129-165 行 `refreshUgiIfNeeded` 方法

### 1.3 复现步骤

| 步骤 | 操作 | 预期结果 | 实际结果 |
|:----:|------|---------|---------|
| 1 | 在 Kerberos 认证集群启动 Hive 引擎 | 引擎正常启动，UGI 初始化成功 | 正常 |
| 2 | 执行 SQL 查询 HDFS 数据 | SQL 正常执行，HDFS 认证通过 | 正常 |
| 3 | 等待 24 小时（TGT 过期） | 引擎自动刷新 TGT，继续正常执行 | TGT 刷新成功（日志显示 refreshed） |
| 4 | 刷新后再次执行 SQL 查询 HDFS 数据 | SQL 正常执行，HDFS 认证通过 | ❌ HDFS 认证失败（FS 缓存中 delegation token 已过期） |

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台运维**，我希望 Hive 引擎在 Kerberos 环境下长时间运行（超过 24 小时）后仍能正常执行 SQL，不需要手动重启引擎 |
| US-2 | 作为**平台运维**，我希望 TGT 刷新后 HDFS 认证也能同步生效，不会出现"刷新成功但认证失败"的矛盾状态 |
| US-3 | 作为**开发者**，我希望修复方案保持 UGI 对象引用不变（原地续期），避免因引用漂移引入其他潜在问题 |
| US-4 | 作为**平台运维**，我希望辅助特性（清 FS 缓存/关 FS 缓存）可通过开关控制，便于在不同环境灵活调整 |

---

## 3. 功能描述

### 3.1 方案总览：三层防线

本修复采用三层防线策略，从 TGT、凭证、入口三个维度彻底解决"刷新成功但认证失败"问题：

| 防线 | 层级 | 机制 | 解决的问题 |
|:----:|------|------|-----------|
| 防线 1 | TGT 层 | 原地 `reloginFromKeytab`（同一 UGI/Subject 上重新 login，不换对象引用） | UGI 的 TGT 被原地刷新为有效状态 |
| 防线 2 | 凭证层 | `FileSystem.closeAllForUGI(ugi)` 清理旧 delegation token 缓存 | 旧 FS 缓存中的过期 delegation token 被清除，后续 `FileSystem.get()` 会用新 TGT 重建 FS |
| 防线 3 | 入口一致 | 原地刷新保持同一 UGI/Subject 对象，杜绝引用漂移 | executor 持有的 UGI 引用不变，Hadoop FS 缓存绑定一致 |

### 3.2 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | 改造 `refreshUgiIfNeeded` | 从"换新 UGI（`loginUserFromKeytabAndReturnUGI`）"改为"原地续期 + 清缓存"。`ugi.reloginFromKeytab()` 原地续期 -> `isTgtValid(ugi)` 校验 -> `FileSystem.closeAllForUGI(ugi)` 清缓存。返回同一对象引用不变 |
| F-2 | 失败计数降级 | 引入 `ugiRefreshFailCount`（已有），`reloginFromKeytab` 抛异常时 catch -> 计数失败 -> 前 2 次返回原 UGI 降级执行 -> 第 3 次（MAX_REFRESH_FAILURES=3）抛 RuntimeException |
| F-3 | FS 缓存清理开关 | 新增 `linkis.engineconn.tgt.refresh.clear.fs.cache`（默认 true），控制是否在原地续期后清理 HDFS FS 缓存 |
| F-4 | FS 缓存禁用开关 | 新增 `linkis.engineconn.tgt.refresh.disable.fs.cache`（默认 false），控制是否完全禁用 HDFS FS 缓存（极端场景兜底） |
| F-5 | 调用点兼容 | `HiveEngineConnExecutor` 和 `HiveEngineConcurrentConnExecutor` 的调用点无需改动逻辑——返回同一 UGI 对象，`if (refreshedUgi != ugi)` 判定自动恒等为 false，不触发引用替换 |
| F-6 | 生产配置文件注释 | `linkis-dist/package/conf/` 相关配置文件添加新配置注释行 |

### 3.3 关键设计决策

| 编号 | 问题 | 决策 |
|:----:|------|------|
| Q1 | 为什么核心修复不设新旧逻辑切换开关？ | 本需求为 Bug 修复（FIX 类型），核心修复（原地续期）直接落地。原始"换新 UGI"策略本身就是错误的——它不是"旧行为"，而是"有 Bug 的行为"。CLAUDE.md 的铁律针对的是"新增功能不得影响现有行为"，而 Bug 修复的本质就是修正错误行为 |
| Q2 | 为什么辅助特性（清 FS 缓存/关 FS 缓存）用开关控制？ | 清 FS 缓存（`closeAllForUGI`）有轻微性能开销（下次访问 HDFS 需重建 FS），关 FS 缓存（`disable.cache`）影响所有 HDFS 访问性能。不同环境对性能敏感度不同，用开关控制便于灵活调整 |
| Q3 | 为什么用 `reloginFromKeytab` 而不是 `checkTGTAndReloginFromKeytab`？ | `reloginFromKeytab` 是 Hadoop UGI 原地续期的标准方法——在同一 UGI/Subject 上重新 login，不换对象引用。`checkTGTAndReloginFromKeytab` 虽然也能续期，但它内部有额外的 TGT 检查逻辑，与我们已有的 `isTgtValid` 检查重复。直接用 `reloginFromKeytab` 更简洁可控 |
| Q4 | 为什么 `closeAllForUGI` 用 `Utils.tryQuietly` 包裹？ | `closeAllForUGI` 可能因 FS 关闭异常而失败，但这是辅助操作（防线 2），不应因其失败而阻断主流程。即使清缓存失败，原地续期（防线 1）已确保 UGI 的 TGT 有效，只是 delegation token 可能仍过期——此时引擎会因认证失败重新触发刷新流程（下一次 `isTgtValid` 仍为 false -> 再次尝试续期）。用 `Utils.tryQuietly` 静默吞掉异常，不影响主链路 |
| Q5 | 为什么 `closeAllForUGI` 而不是手动遍历 `fileSystemCache`？ | `FileSystem.closeAllForUGI(ugi)` 是 Hadoop 官方 API，会清理与指定 UGI 关联的所有 FS 缓存实例（包括 delegation token）。手动遍历 `HDFSUtils.fileSystemCache` 是 Linkis 内部的 FS 缓存，但 Hive 引擎执行时走的是 Hadoop 原生 `FileSystem.get()`，其缓存由 Hadoop 框架管理，必须用 Hadoop 官方 API 清理 |
| Q6 | 并发场景如何处理？ | `HiveEngineConcurrentConnExecutor` 已有 `synchronized` + double-check 保护（先 `isTgtValid` 检查，过期才进入 synchronized 块再检查再刷新）。原地续期不会换 UGI 引用，所以不会出现"多个线程拿到不同 UGI 引用"的问题。`reloginFromKeytab` 本身是线程安全的（Hadoop 内部有锁） |

### 3.4 与现有机制的关系

| 现有机制 | 关系 | 说明 |
|---------|------|------|
| `linkis.engineconn.tgt.refresh.enable`（默认 false） | 复用 | 控制整个 TGT 懒刷新逻辑的总开关。本修复在此开关打开时生效。核心修复（原地续期）直接落地，无需此开关控制新旧逻辑切换 |
| `linkis.hadoop.hdfs.tgt.proactive.check.enable`（默认 true） | 互补 | HDFS 缓存层的主动 TGT 检查——获取缓存 FS 时检查 TGT 有效性，过期则重建。本修复是引擎层的 TGT 懒刷新——执行 SQL 前检查并刷新。两者互补，分别守护 FS 缓存层和引擎执行层 |
| `refreshLoginUserTgtIfNeeded` | 不受影响 | JDBC/HBase 引擎使用 JVM 全局 loginUser，走 `checkTGTAndReloginFromKeytab`，本身就是原地续期（操作同一 loginUser 对象）。本修复仅针对 Hive 引擎的 `refreshUgiIfNeeded` |
| `HDFSUtils.getUserGroupInformation` | 不再被调用 | `refreshUgiIfNeeded` 原始实现调用此方法创建新 UGI，修复后改为原地续期，不再调用此方法创建新 UGI。但此方法本身不受影响，Hive 引擎初始化时仍正常使用 |

---

## 4. 验收标准（三段式）

### AC1: TGT 过期检测与原地续期（输入验证 + 处理验证）

**前置条件**: `linkis.engineconn.tgt.refresh.enable=true`，Kerberos 认证环境，Hive 引擎已启动

**操作步骤**:
1. 引擎启动后首次执行 SQL，验证正常
2. 等待 TGT 过期（或手动使 TGT 过期）
3. 再次执行 SQL

**预期结果**:
- `isTgtValid(ugi)` 返回 false（TGT 已过期）
- `ugi.reloginFromKeytab()` 被调用，在同一 UGI 对象上原地续期
- `isTgtValid(ugi)` 再次校验返回 true（TGT 已刷新）
- `FileSystem.closeAllForUGI(ugi)` 被调用，清理旧 FS 缓存
- 返回的 UGI 对象引用与传入的相同（`refreshedUgi == ugi`）
- SQL 正常执行，HDFS 认证通过

### AC2: 续期失败计数降级（处理验证）

**前置条件**: `linkis.engineconn.tgt.refresh.enable=true`，Kerberos 认证环境，模拟 `reloginFromKeytab` 抛异常

**操作步骤**:
1. 使 TGT 过期
2. 模拟 `reloginFromKeytab` 连续失败 1 次、2 次、3 次

**预期结果**:
- 第 1 次失败：`ugiRefreshFailCount` = 1，返回原 UGI（降级执行），日志打印 warn
- 第 2 次失败：`ugiRefreshFailCount` = 2，返回原 UGI（降级执行），日志打印 warn
- 第 3 次失败：`ugiRefreshFailCount` = 3 >= MAX_REFRESH_FAILURES(3)，抛 RuntimeException
- TGT 有效时调用 `refreshUgiIfNeeded`：`ugiRefreshFailCount` 重置为 0

### AC3: 辅助开关控制 FS 缓存清理（输入验证 + 输出验证）

**前置条件**: `linkis.engineconn.tgt.refresh.enable=true`，`linkis.engineconn.tgt.refresh.clear.fs.cache=false`

**操作步骤**:
1. 使 TGT 过期
2. 执行 SQL 触发 `refreshUgiIfNeeded`

**预期结果**:
- `reloginFromKeytab` 原地续期正常执行
- `FileSystem.closeAllForUGI(ugi)` **不**被调用（开关关闭）
- 返回同一 UGI 对象引用不变
- 后续 HDFS 访问可能仍使用旧 FS 缓存（delegation token 可能过期）——此为预期行为，辅助开关关闭时仅依赖防线 1（TGT 层）

### AC4: 非 Kerberos 环境与 JDBC/HBase 引擎回归（输出验证）

**操作步骤**:
1. 非 Kerberos 环境下执行 Hive SQL
2. Kerberos 环境下执行 JDBC 引擎 SQL
3. Kerberos 环境下执行 HBase 引擎 SQL

**预期结果**:
- 非 Kerberos 环境：`refreshUgiIfNeeded` 在 `!UserGroupInformation.isSecurityEnabled()` 时直接返回原 UGI，无任何副作用
- JDBC 引擎：走 `refreshLoginUserTgtIfNeeded`，不受本修复影响
- HBase 引擎：走 `refreshLoginUserTgtIfNeeded`，不受本修复影响
- 所有场景行为与修复前一致

---

## 5. 配置说明

### 5.1 配置项总览

| 配置 Key | 类型 | 默认值 | 说明 |
|---------|------|--------|------|
| `linkis.engineconn.tgt.refresh.enable` | Boolean | `false` | 引擎 TGT 懒刷新总开关（已有）。开启后 Hive/JDBC/HBase 引擎在执行任务前检查 TGT 有效性，过期则自动刷新 |
| `linkis.engineconn.tgt.refresh.clear.fs.cache` | Boolean | `true` | **新增**。原地续期后是否清理 HDFS FS 缓存（`FileSystem.closeAllForUGI`）。关闭时仅依赖 TGT 层续期，不清 delegation token 缓存 |
| `linkis.engineconn.tgt.refresh.disable.fs.cache` | Boolean | `false` | **新增**。是否完全禁用 HDFS FS 缓存（`fs.hdfs.impl.disable.cache=true`）。开启后每次 HDFS 访问都创建新 FS 实例，性能下降但彻底避免缓存不一致。仅极端场景兜底 |

### 5.2 配置位置

- 模块配置类: `HadoopConf.scala`（`linkis-commons/linkis-hadoop-common`）
- 部署配置文件: `linkis-dist/package/conf/` 相关引擎配置文件添加注释行

### 5.3 使用说明

```properties
# 引擎 TGT 懒刷新总开关（已有，默认关闭）
# 开启后 Hive/JDBC/HBase 引擎在执行任务前检查 TGT 有效性，过期则自动刷新
# Kerberos 认证环境建议开启
# linkis.engineconn.tgt.refresh.enable=false

# TGT 刷新后清理 HDFS FS 缓存开关（新增，默认打开）
# 开启：原地续期后调用 FileSystem.closeAllForUGI 清理旧 delegation token 缓存
# 关闭：仅依赖 TGT 层续期，不清缓存（性能略优但可能残留过期 token）
# linkis.engineconn.tgt.refresh.clear.fs.cache=true

# 禁用 HDFS FS 缓存开关（新增，默认关闭）
# 开启：每次 HDFS 访问创建新 FS 实例（性能下降，仅极端场景兜底）
# linkis.engineconn.tgt.refresh.disable.fs.cache=false
```

### 5.4 开关决策矩阵

| 开关组合 | TGT 续期 | 清 FS 缓存 | 禁用 FS 缓存 | 适用场景 |
|---------|:--------:|:----------:|:------------:|---------|
| enable=false | 不触发 | - | - | 非 Kerberos 环境，或不需要引擎层 TGT 刷新 |
| enable=true, clear=true, disable=false | 原地续期 | 是 | 否 | **推荐配置**：Kerberos 环境，三层防线全开 |
| enable=true, clear=false, disable=false | 原地续期 | 否 | 否 | 性能敏感环境，仅依赖 TGT 层续期 |
| enable=true, clear=true, disable=true | 原地续期 | 是 | 是 | 极端兜底：彻底禁用 FS 缓存，每次重建 |

---

## 6. 风险识别

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| `reloginFromKeytab` 在并发场景下的线程安全 | 🟡 | `HiveEngineConcurrentConnExecutor` 已有 `synchronized` + double-check 保护。`reloginFromKeytab` 本身是 Hadoop 官方方法，内部有锁。缓解：并发场景充分测试 |
| `closeAllForUGI` 性能开销 | 🟢 | `closeAllForUGI` 关闭旧 FS 实例后，下次 HDFS 访问需重建 FS。但 TGT 刷新本身是低频操作（每小时一次），重建 FS 的开销可接受。缓解：开关可关闭 |
| `reloginFromKeytab` 连续失败 | 🟡 | keytab 文件丢失或 KDC 不可用可能导致连续失败。缓解：失败计数降级机制——前 2 次返回原 UGI 降级执行，第 3 次抛 RuntimeException 报错，避免无限重试 |
| Hadoop 版本兼容性 | 🟢 | `reloginFromKeytab` 和 `closeAllForUGI` 是 Hadoop 2.7+ 和 3.x 的标准 API，本项目支持 hadoop-2.7.2 和 hadoop-3.3.1 双 profile，均已验证兼容 |
| 影响非 Hive 引擎 | 🟢 | 本修复仅改 `refreshUgiIfNeeded`（Hive 路径），JDBC/HBase 走 `refreshLoginUserTgtIfNeeded`（本身就是原地续期），不受影响 |

---

## 7. 关联影响分析

### 7.1 高危区域对照（CLAUDE.md §10.2）

本需求触及 `linkis-commons/linkis-hadoop-common` 中的 Kerberos 工具类，属于**公共服务工具类**，**不触及** §10.2 中列出的 10 个任务执行主线高危环节（主入口/拦截器链/引擎创建复用/EC 执行/回调/持久化/物料链/脚本组装）。但仍遵守 §4 铁律（异常降级 + 开关控制）。

| §10.2 高危环节 | 是否触及 | 说明 |
|---------------|:--------:|------|
| #1 任务主入口 | 否 | 不修改 `EntranceServer.execute()` |
| #2 拦截器链 | 否 | 不修改拦截器链顺序 |
| #3 引擎新建 | 否 | 不修改 `DefaultEngineCreateService` |
| #4 引擎复用 | 否 | 不修改 `DefaultEngineReuseService` |
| #5 EC 接任务 | 否 | 不修改 `TaskExecutionServiceImpl` |
| #6 回调 Entrance | 否 | 不修改 `sendToEntrance()` |
| #7 任务持久化 | 否 | 不修改 `PersistenceManager` |
| #8 LM 刷新物料 | 否 | 不修改 `DefaultEngineConnResourceService` |
| #9 ECM 拉物料 | 否 | 不修改 `BmlResourceLocalizationService` |
| #10 脚本组装 | 否 | 不修改 `JavaProcessEngineConnLaunchBuilder` |

### 7.2 影响清单

| 维度 | 影响 |
|------|------|
| 功能模块 | 🟡 `linkis-commons/linkis-hadoop-common`（KerberosTgtUtils + HadoopConf）、`linkis-engineconn-plugins/hive`（调用点兼容验证） |
| API 契约 | 🟢 `refreshUgiIfNeeded` 方法签名不变（入参 UGI + userName，返回 UGI），返回值语义从"可能返回新 UGI"变为"始终返回同一 UGI"，调用点 `if (refreshedUgi != ugi)` 自动兼容 |
| 前端 | 🟢 不涉及前端改动 |
| 上下游依赖 | 🟢 Hive 引擎上游（Entrance/Orchestrator）和下游（HDFS/Hive Metastore）不受影响——修复仅改变 UGI 内部续期策略 |
| 配置 | 🟡 新增 2 个配置项，需在部署配置文件补充注释行 |
| 兼容性 | 🟢 非 Kerberos 环境直接返回原 UGI；JDBC/HBase 走独立方法不受影响 |
| 提示词注入风险 | N/A 本需求为 Java/Scala 后端 Bug 修复，不涉及 Claude 插件开发 |

---

## 8. 发布和回滚方案

### 8.1 发布策略

1. **测试环境先行**：在 Kerberos 认证测试环境（bdap-sit）设置 `linkis.engineconn.tgt.refresh.enable=true`，验证：
   - 引擎存活 > 24h 后 SQL 正常执行
   - TGT 刷新日志正确（"TGT expired, refreshing UGI in-place for user" + "UGI refreshed in-place and HDFS FS cache cleared"）
   - 失败计数降级机制有效
2. **生产灰度**：生产环境设置 `linkis.engineconn.tgt.refresh.enable=true` + `clear.fs.cache=true` + `disable.fs.cache=false`（推荐配置），观察 48 小时
3. **监控指标**：
   - TGT 刷新成功次数 + 失败次数（warn 日志计数）
   - HDFS 认证失败次数（验证是否降为零）
   - Hive 引擎存活时长（验证是否突破 24h 限制）
   - `FileSystem.closeAllForUGI` 调用频率 + 重建 FS 耗时

### 8.2 回滚方案

- **回滚条件**: 修复后仍出现 HDFS 认证失败，或 `reloginFromKeytab` 连续失败导致引擎报错
- **回滚步骤**:
  1. 设置 `linkis.engineconn.tgt.refresh.enable=false` 关闭整个 TGT 懒刷新逻辑
  2. 如需进一步回滚，设置 `linkis.engineconn.tgt.refresh.clear.fs.cache=false` 关闭 FS 缓存清理
  3. 重启 Hive 引擎（或等待引擎自然过期重建）
- **回滚时间**: 配置修改 + 引擎重启，约 5 分钟
- **回滚预案**: 无需代码回滚，仅通过配置开关即可降级。关闭 `enable` 后 `refreshUgiIfNeeded` 不会被调用，行为回退到修复前（但不影响新引擎启动后的首次 UGI 初始化）

---

## 9. 防范措施

### 9.1 测试改进

- [ ] 补充单测：mock UGI 4 场景（TGT 有效/原地续期成功/续期失败计数/达上限抛异常）
- [ ] 增加边界测试：`reloginFromKeytab` 抛不同异常类型的降级行为
- [ ] 集成测试：Kerberos 环境 SIT >= 24h 持续验证

### 9.2 监控改进

- [ ] 增加 TGT 刷新成功/失败计数监控指标
- [ ] 完善 HDFS 认证失败告警规则
- [ ] 增加引擎存活时长监控（验证突破 24h 限制）

### 9.3 流程改进

- [ ] Code Review 检查点：Kerberos 相关修改必须验证"UGI 对象引用一致性"
- [ ] Hadoop UGI/FS 缓存机制知识库沉淀（`linkis-commons` 模块文档）
- [ ] 发布流程优化：Kerberos 环境的 SIT 测试必须覆盖 > 24h 长运行场景

---

## 附录

### A.1 修改文件清单

| 序号 | 文件 | 修改类型 | 说明 |
|:----:|------|:--------:|------|
| 1 | `linkis-commons/linkis-hadoop-common/src/main/java/org/apache/linkis/hadoop/common/utils/KerberosTgtUtils.java` | 修改 | 改造 `refreshUgiIfNeeded`（第 129-165 行）：从"换新 UGI"改为"原地续期 + 清缓存" |
| 2 | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala` | 修改 | 新增 2 个配置开关：`ENGINE_TGT_REFRESH_CLEAR_FS_CACHE` + `ENGINE_TGT_REFRESH_DISABLE_FS_CACHE` |
| 3 | `linkis-engineconn-plugins/hive/src/main/scala/org/apache/linkis/engineplugin/hive/executor/HiveEngineConnExecutor.scala` | 验证兼容 | 调用点（第 226-231 行）无需改动逻辑——返回同一 UGI 对象，`if (refreshedUgi != ugi)` 自动恒等 |
| 4 | `linkis-engineconn-plugins/hive/src/main/scala/org/apache/linkis/engineplugin/hive/executor/HiveEngineConcurrentConnExecutor.scala` | 验证兼容 | 调用点（第 187-197 行）无需改动逻辑——`synchronized` + double-check 保护已就绪 |
| 5 | `linkis-dist/package/conf/` 相关引擎配置文件 | 修改 | 新增配置注释行 |

### A.2 关键代码锚点（改造前已确认）

| 文件 | 行号 | 说明 |
|------|------|------|
| `KerberosTgtUtils.java` | 42 | `MAX_REFRESH_FAILURES = 3` 常量定义 |
| `KerberosTgtUtils.java` | 45 | `ugiRefreshFailCount` 失败计数器（已有，复用） |
| `KerberosTgtUtils.java` | 90-111 | `isTgtValid` 方法——TGT 有效性检查逻辑（不改动） |
| `KerberosTgtUtils.java` | 129-165 | `refreshUgiIfNeeded` 方法——**本次改造核心区域** |
| `KerberosTgtUtils.java` | 139 | `HDFSUtils.getUserGroupInformation(userName)` ——**删除此调用**，改为 `ugi.reloginFromKeytab()` |
| `HadoopConf.scala` | 100-101 | `ENGINE_TGT_REFRESH_ENABLE`（已有开关，复用） |
| `HadoopConf.scala` | 103 | 新增 `ENGINE_TGT_REFRESH_CLEAR_FS_CACHE` + `ENGINE_TGT_REFRESH_DISABLE_FS_CACHE`（在此位置后追加） |
| `HiveEngineConnExecutor.scala` | 226-231 | TGT 懒刷新调用点（验证兼容，不改动） |
| `HiveEngineConcurrentConnExecutor.scala` | 187-197 | TGT 懒刷新调用点（验证兼容，不改动） |

### A.3 判定逻辑伪代码

```java
// KerberosTgtUtils.refreshUgiIfNeeded 改造后伪代码
public static UserGroupInformation refreshUgiIfNeeded(UserGroupInformation ugi, String userName) {
    // 输入验证：null 或非安全模式直接返回
    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) return ugi;

    // TGT 仍有效，重置失败计数，返回原 UGI
    if (isTgtValid(ugi)) {
        ugiRefreshFailCount.set(0);
        return ugi;
    }

    logger.info("TGT expired, refreshing UGI in-place for user: {}", userName);
    try {
        // 防线1：TGT 层 - 原地续期（同一 UGI/Subject 上重新 login）
        if (ugi.isFromKeytab()) {
            ugi.reloginFromKeytab();
        } else {
            throw new IllegalStateException("UGI is not keytab-based, cannot relogin");
        }

        // 校验续期成功
        if (!isTgtValid(ugi)) {
            throw new RuntimeException("TGT still invalid after relogin");
        }

        // 防线2：凭证层 - 清理旧 delegation token 缓存
        if (HadoopConf.ENGINE_TGT_REFRESH_CLEAR_FS_CACHE.getValue()) {
            Utils.tryQuietly {
                FileSystem.closeAllForUGI(ugi);
            }
            logger.info("HDFS FS cache cleared for user: {}", userName);
        }

        // 防线3：入口一致 - 返回同一对象引用（ugi 变量未重新赋值）
        ugiRefreshFailCount.set(0);
        logger.info("UGI refreshed in-place for user: {}", userName);
        return ugi;  // 同一对象引用

    } catch (Exception e) {
        // 失败计数降级：前 2 次返回原 UGI，第 3 次抛异常
        int failCount = ugiRefreshFailCount.incrementAndGet();
        logger.warn("Failed to refresh UGI for user: {}, fail count: {}", userName, failCount, e);
        if (failCount >= MAX_REFRESH_FAILURES) {
            throw new RuntimeException(
                "TGT refresh failed " + MAX_REFRESH_FAILURES + " times for user: " + userName
                + ". Please check keytab/KDC.", e);
        }
        return ugi;  // 降级返回原 UGI
    }
}
```

### A.4 关联文档

- CLAUDE.md §4（新功能开发铁律：开关默认关闭 + 异常降级）
- CLAUDE.md §4.2（Utils.tryCatch / tryQuietly 异常保护）
- CLAUDE.md §9.3（CommonVars 热加载边界：getValue vs getHotValue）
- CLAUDE.md §10.2（高危区域对照：本需求不触及 10 个主线高危环节）
- 版本上下文: `dev/versions/LinkisV2.1.6/version-context.md`

## 需求风险摘要

### 风险等级概览

| 风险总数 | 高 | 中 | 低 |
|:-------:|:--:|:--:|:--:|
| 2 | 2 | 0 | 0 |

### 风险概览

| 风险ID | 关联功能点 | 等级 | 类型 | 风险结论 | 影响范围 |
|--------|-------------|:----:|------|---------|---------|
| R-001 | AC1 TGT过期检测与原地续期<br>AC2 续期失败计数降级<br>AC4 非Kerberos与JDBC/HBase回归 | 高 | 模块链路 | 改造KerberosTgtUtils认证续期策略可能导致Hive引擎HDFS认证链路异常 | Hive引擎HDFS认证链路（KerberosTgtUtils.refreshUgiIfNeeded改造） |
| R-002 | AC3 辅助开关控制FS缓存清理 | 高 | 参数配置 | 新增disable.fs.cache配置开关可能导致HDFS访问性能下降 | HDFS FileSystem缓存（disable.fs.cache开关） |

### 测试要求

| 风险ID | 测试要求 | 判断依据 | 处理建议 |
|--------|---------|---------|---------|
| R-001 | 下游联动验证<br>跨系统调用异常验证<br>级联失败恢复验证 | 修改Kerberos认证续期策略影响所有Kerberos环境Hive引擎的HDFS访问，属核心认证链路改造 | 设计阶段补充并发场景线程安全验证；Stage5.5覆盖Kerberos环境≥24h长运行 |
| R-002 | 配置变更生效验证<br>配置回滚验证<br>配置边界值验证 | 禁用FS缓存是核心参数变更，性能下降但需求未提供压测验证安排 | 设计阶段补充性能基线与回归阈值；AC3覆盖辅助开关控制 |
