# 代码评审记录 · REQ-01 修复 Hive 引擎 kerberos 认证失败

> 版本：LinkisV2.1.6 ｜ 子系统：WDSL-UJES-2.1.6 ｜ 评审日期：2026-10-22
> 评审方式：静态走查（8 维度）｜ 评审人：v_kkhuang
> 变更基线：c5d8db3ae~1 → c5d8db3ae

## 一、变更范围

| # | 文件 | 模块 | 变更摘要 |
|---|---|---|---|
| 1 | `linkis-commons/linkis-hadoop-common/src/main/java/org/apache/linkis/hadoop/common/utils/KerberosTgtUtils.java` | hadoop-common | isTgtValid 严格化（拿不到 Subject/票据时由 return true 改为 false 触发续期）；新增 getTgtRemainingMillis；refreshUgiIfNeeded 改为"提前续期（剩余<阈值即续）+ 三层防线（原地 reloginFromKeytab → 换新 UGI 兜底 → 连续 3 次失败抛异常）" |
| 2 | `linkis-commons/linkis-hadoop-common/src/main/java/org/apache/linkis/hadoop/common/utils/KerberosUtils.java` | hadoop-common | getKerberosRefreshInterval 由读环境变量改为读 CommonVars（HadoopConf.LINKIS_KERBEROS_REFRESH_INTERVAL） |
| 3 | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala` | hadoop-common | 新增 2 配置项：LINKIS_KERBEROS_REFRESH_INTERVAL(43200)、ENGINE_TGT_REFRESH_AHEAD_SECONDS(60) |
| 4 | `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/conf/WorkSpaceConfiguration.java` | pes-publicservice | （与 REQ-02 共改，REQ-01 评审范围内无新增） |

## 二、8 维度静态走查

### 1. 注入（SQL/命令/LDAP）
- **结论：无风险**。本次变更不涉及 SQL 拼接、命令执行、LDAP 查询。KerberosTgtUtils 通过反射调用 `UserGroupInformation.getSubject()`（protected 方法），反射目标类是 JDK/hadoop 固定类 `UserGroupInformation`，方法名硬编码 `"getSubject"`，无用户可控入参注入反射。✅

### 2. 敏感信息
- **结论：无风险**。日志均打印 `userName`（checkUgi.getUserName()），未打印 keytab 路径、principal 密码、TGT 票据内容。符合 CLAUDE.md §8.4"日志禁止打印敏感信息"。✅

### 3. 权限
- **结论：无风险**。KerberosTgtUtils 是 hadoop-common 工具类，由引擎侧调用，不走 Restful 鉴权；refreshUgiIfNeeded 的调用方传入的 `userName` 来自已鉴权的引擎上下文，非前端直接入参。无越权路径。✅

### 4. 异常处理 ⭐
- **结论：合格，符合铁律**。
  - `getTgtRemainingMillis`：反射/调用异常 catch 后返回 -1（触发续期），warn 日志，不抛出；✅
  - `refreshUgiIfNeeded` 防线① `checkTGTAndReloginFromKeytab`：异常 catch 后降级到防线②（换新 UGI），warn 日志，不抛出；✅
  - 防线② `HDFSUtils.getUserGroupInformation` 返回 null 时抛 RuntimeException，但被外层 try 包裹，连续失败 MAX_REFRESH_FAILURES=3 次才抛出，避免单次抖动导致任务失败；✅
  - 所有 catch 块均用项目 logger（`logger.warn`），未使用 `e.printStackTrace()`；✅
  - 降级链路完整：原地续期失败 → 换新 UGI → 连续 3 次失败抛异常，每步都有 warn 日志。✅

### 5. 输入校验
- **结论：合格**。`isTgtValid`/`getTgtRemainingMillis`/`refreshUgiIfNeeded` 均对 `ugi == null` 做前置判空；`!UserGroupInformation.isSecurityEnabled()` 非 Kerberos 环境直接返回（no-op），避免误触发续期。proxy UGI 场景正确取 `getRealUser()` 作为续票目标。✅

### 6. 配置 ⭐
- **结论：合格，符合铁律 §4.1**。
  - 新增 2 配置项均用 `CommonVars` 声明（`LINKIS_KERBEROS_REFRESH_INTERVAL`、`ENGINE_TGT_REFRESH_AHEAD_SECONDS`）；
  - `ENGINE_TGT_REFRESH_AHEAD_SECONDS` 默认 60 秒，注释说明仅在 `ENGINE_TGT_REFRESH_ENABLE` 开启时生效；
  - `ENGINE_TGT_REFRESH_ENABLE` 默认 false（保守关闭，符合 §4.1.1 新功能开关默认 false）；
  - `KerberosUtils.getKerberosRefreshInterval` 由读环境变量改为读 CommonVars，使配置可热加载（符合 §9.3），消除了环境变量不可动态调整的缺陷。✅

### 7. 反序列化
- **结论：不涉及**。本次变更无序列化/反序列化逻辑。✅

### 8. 路径穿越
- **结论：不涉及**。本次变更不处理文件路径。✅

## 三、重点风险点核查

### 3.1 去掉 closeAllForUGI 的并发安全性
commit 记录显示去掉了两处 `FileSystem.closeAllForUGI` 调用，理由是"清缓存会关闭并发任务正使用的 FileSystem，触发 Filesystem closed"。
- **核查**：原地 `checkTGTAndReloginFromKeytab` 在同一 Subject 内换新票，不换 UGI 对象引用，已缓存的 FileSystem 的 DFSClient 绑定的 Subject 引用不变，新票自然生效，无需清缓存。去掉 closeAllForUGI 避免了并发误伤，逻辑正确。✅

### 3.2 提前续期阈值
`getTgtRemainingMillis < ENGINE_TGT_REFRESH_AHEAD_SECONDS*1000` 即续期，默认 60 秒。
- **核查**：生产日志 28025 显示 MR 跑 22 秒中途 TGT 过期，60 秒阈值覆盖典型 MR 时长，合理。阈值可配置，过长可按需调整。✅

### 3.3 isTgtValid 严格化影响面
由"拿不到 Subject/票据时 return true"改为"return false"。
- **核查**：仅在 `UserGroupInformation.isSecurityEnabled()` 为 true（Kerberos 环境）时走严格分支；非 Kerberos 环境仍 return true（no-op）。不会影响非安全集群。严格化后，真正异常的 UGI 会触发续期而非误判有效，修复了原缺陷。✅

## 四、单测覆盖

| 文件 | 单测 | 状态 |
|---|---|---|
| KerberosTgtUtils | 无独立单测 | ⚠️ 建议补单测（getTgtRemainingMillis 边界、refreshUgiIfNeeded 三层防线降级） |
| HadoopConf | 无独立单测 | 配置类，沿用 CommonVars 机制 |
| KerberosUtils | 无独立单测 | 建议补 getKerberosRefreshInterval 读 CommonVars 的单测 |

> 注：hadoop-common 模块历史上无 KerberosTgtUtils 单测，本次变更未新增。属 low 级建议，不阻断发布。

## 五、评审结论

| 维度 | critical | high | medium | low |
|---|---|---|---|---|
| 注入 | 0 | 0 | 0 | 0 |
| 敏感信息 | 0 | 0 | 0 | 0 |
| 权限 | 0 | 0 | 0 | 0 |
| 异常处理 | 0 | 0 | 0 | 0 |
| 输入校验 | 0 | 0 | 0 | 0 |
| 配置 | 0 | 0 | 0 | 0 |
| 反序列化 | 0 | 0 | 0 | 0 |
| 路径穿越 | 0 | 0 | 0 | 0 |
| **合计** | **0** | **0** | **0** | **2**（单测建议） |

- **should_block_deploy**: false
- **评审结果**: ✅ 通过
- **依据**: 变更符合 CLAUDE.md 铁律（开关默认 false + tryCatch 降级 + Utils.classForName 等价反射 + CommonVars 配置 + 项目 logger），三层防线降级链路完整，去 closeAllForUGI 的并发安全性论证充分。
