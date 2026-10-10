# REQ-02 ContextService日志优化 测试澄清纪要

**需求编号**: REQ-02
**需求名称**: ContextService日志优化
**所属版本**: LinkisV2.1.4
**澄清阶段**: Stage 5.5
**澄清日期**: 2026-10-08
**就绪度**: 高
**完成度**: 0.92
**门控结果**: 通过（P0 覆盖率 >= 85%）

---

## 一、被测对象清单

### 1.1 代码变更范围

| 序号 | 文件 | Commit | 修改类型 | 说明 |
|:---:|------|--------|:--------:|------|
| 1 | `DefaultContextCache.java` | 9235b6aec | 修改 | 新增 scheduleStatsLog + printCacheStats + get未命中warn + put区分new/replace + error补异常对象 |
| 2 | `ContextIDValueGeneratorImpl.java` | 9235b6aec | 修改 | createContextIDValue 增加 startTime + keyValueSize + cost ms 日志 |
| 3 | `ContextIDRemoveListener.java` | 9235b6aec | 修改 | onRemoval 增加 RemovalCause + cachedMillis，SIZE驱逐走warn |
| 4 | `ContextCacheConf.java` | 9235b6aec | 修改 | 新增 CACHE_STATS_LOG_INTERVAL_MILLS 配置常量 |
| 5 | `ContextIDServiceImpl.java` | 38e156050 | 修改 | createContextID/updateContextID 日志补 csIdType 字段 |
| 6 | `ContextServiceImpl.java` | 38e156050 | 修改 | getContextValue/setValueByKey/setValue/searchContextValue/resetValue/removeValue/removeAllValue/removeAllValueByKeyPrefix 日志补 csType/csScope/value/keywords/resultSize/update 等字段 |

### 1.2 功能点与验收标准映射

| 功能点 | 验收标准 | 被测方法 | 测试类型 |
|:------:|---------|----------|---------|
| F-1 缓存统计周期调度 | AC1 | `DefaultContextCache.scheduleStatsLog` / `printCacheStats` | 单元测试（Mock调度器+验证日志输出） |
| F-2 统计间隔可配置 | AC2 | `DefaultContextCache.scheduleStatsLog`（intervalMills<=0 return） | 单元测试（配置=0验证不调度） |
| F-3 调度异常兜底 | AC8 | `scheduleStatsLog` / `printCacheStats` 双层 try/catch | 单元测试（Mock抛异常验证不中断） |
| F-4 加载耗时埋点 | AC3 | `ContextIDValueGeneratorImpl.createContextIDValue` | 单元测试（Mock DB加载验证cost+keyValueSize日志） |
| F-5 移除原因日志增强 | AC4/AC5 | `ContextIDRemoveListener.onRemoval` | 单元测试（SIZE驱逐warn / EXPIRE驱逐info） |
| F-6 缓存未命中warn | AC1(间接) | `DefaultContextCache.get` | 单元测试（getIfPresent=null验证warn日志） |
| F-7 put日志区分新增/替换 | - | `DefaultContextCache.put` | 单元测试（isNew验证"Put new" / 非isNew验证"Replace"） |
| F-8 ContextIDService日志补字段 | AC7 | `ContextIDServiceImpl.createContextID` / `updateContextID` | 单元测试（验证日志含csIdType） |
| F-9 ContextService KV日志补字段 | AC6 | `ContextServiceImpl.getContextValue` / `setValueByKey` / `setValue` / `searchContextValue` | 单元测试（验证日志含csType/csScope/value/keywords/resultSize/update） |
| F-10 searchContextValue先执行后打日志 | AC6(部分) | `ContextServiceImpl.searchContextValue` | 单元测试（验证resultSize为真实结果数） |

### 1.3 不涉及接口清单

本次改造为纯日志层面增强，无新增/修改 API 接口、无 RPC 协议变更、无数据库表结构变更。所有被测对象均为 Java 内部方法调用，通过单元测试即可覆盖。

---

## 二、被测范围(testScope)

### 必测项(mustTest)

| 序号 | 测试项 | 关联AC | 被测类/方法 | 测试策略 |
|:---:|--------|:------:|------------|---------|
| 1 | 缓存统计周期调度 - 默认30min触发 | AC1 | `DefaultContextCache.scheduleStatsLog` / `printCacheStats` | 单元测试：验证调度注册成功，验证日志含 size/requestCount/hitCount/missCount/hitRate/evictionCount |
| 2 | 缓存统计间隔=0关闭调度 | AC2 | `DefaultContextCache.scheduleStatsLog` | 单元测试：设置 intervalMills=0，验证直接 return 不注册调度 |
| 3 | 调度异常不传播 | AC8 | `scheduleStatsLog` / `printCacheStats` | 单元测试：Mock printCacheStats 抛异常，验证调度不中断（双层try/catch） |
| 4 | ContextID加载耗时埋点 | AC3 | `ContextIDValueGeneratorImpl.createContextIDValue` | 单元测试：Mock contextMapPersistence.getAll 返回列表，验证日志含 cost ms + keyValue size |
| 5 | SIZE驱逐warn日志 | AC4 | `ContextIDRemoveListener.onRemoval` | 单元测试：构造 RemovalNotification with RemovalCause.SIZE，验证 warn 级别日志含 tuning 提示 |
| 6 | 非容量驱逐info日志 | AC5 | `ContextIDRemoveListener.onRemoval` | 单元测试：构造 RemovalCause.EXPIRE/COLLECTED，验证 info 级别日志含 cause + cachedMillis |
| 7 | 缓存未命中warn日志 | AC1(间接) | `DefaultContextCache.get` | 单元测试：cache.getIfPresent 返回 null，验证 warn 日志 "not in cache, will load from db" |
| 8 | put日志区分新增/替换 | - | `DefaultContextCache.put` | 单元测试：首次put验证"Put new"，二次put验证"Replace" |
| 9 | getContextValue日志补全 | AC6 | `ContextServiceImpl.getContextValue` | 单元测试：验证日志含 csId/key/csType/csScope/value/keywords |
| 10 | setValueByKey日志补全 | AC6 | `ContextServiceImpl.setValueByKey` | 单元测试：验证日志含 csId/key/csType/csScope/keywords/value/update |
| 11 | setValue日志补全 | AC6 | `ContextServiceImpl.setValue` | 单元测试：验证日志含 csId/key/csType/csScope/keywords/value/update |
| 12 | searchContextValue先执行后打日志 | AC6 | `ContextServiceImpl.searchContextValue` | 单元测试：验证日志 resultSize 为真实结果数（非空），验证执行顺序 |
| 13 | ContextIDService create日志补csIdType | AC7 | `ContextIDServiceImpl.createContextID` | 单元测试：验证日志含 csId + csIdType |
| 14 | ContextIDService update日志补csIdType | AC7 | `ContextIDServiceImpl.updateContextID` | 单元测试：验证日志含 csId + csIdType |
| 15 | resetValue日志补字段 | - | `ContextServiceImpl.resetValue` | 单元测试：验证日志含 csId/key/csType/csScope |
| 16 | removeValue日志补字段 | - | `ContextServiceImpl.removeValue` | 单元测试：验证日志含 csId/key/csType/csScope |
| 17 | removeAllValue日志补csIdType | - | `ContextServiceImpl.removeAllValue` | 单元测试：验证日志含 csId + csIdType |
| 18 | removeAllValueByKeyPrefix日志补csIdType | - | `ContextServiceImpl.removeAllValueByKeyPrefix` | 单元测试：验证日志含 csId/csIdType/keyPrefix |
| 19 | clearAllByHAID日志增强 | - | `ContextServiceImpl.clearAllByHAID` | 单元测试：验证warn日志含 haid + successNum |
| 20 | clearAllByID日志增强 | - | `ContextServiceImpl.clearAllByID` | 单元测试：验证error日志含 source + successNum |

### 选测项(optionalTest)

| 序号 | 测试项 | 说明 |
|:---:|--------|------|
| 1 | ContextIDMetric为null时cachedMillis=-1 | ContextIDRemoveListener.onRemoval 中 metric==null 分支 |
| 2 | contextKeyValueList为null时keyValueSize=0 | ContextIDValueGeneratorImpl.createContextIDValue 中 list==null 分支 |
| 3 | 缓存统计日志调度注册失败兜底 | scheduleStatsLog 的 try/catch 捕获注册异常 |
| 4 | DefaultContextCache.get异常补异常对象 | error日志从 `logger.error(errorMsg)` 改为 `logger.error(errorMsg, e)` |

### 不测项(notTest)

| 序号 | 不测项 | 原因 |
|:---:|--------|------|
| 1 | Guava Cache `recordStats()` 功能本身 | Guava 框架自带功能，非本次改造范围 |
| 2 | `Utils.defaultScheduler()` 调度器实现 | 公共基础设施，非本次改造范围 |
| 3 | ContextCacheConf 已有配置项（CACHE_SIZE/CACHE_EXPIRE等） | 非本次新增，不纳入测试范围 |
| 4 | 其他服务模块（非 linkis-cs-server） | 本次改造仅涉及 cs-server 单模块 |
| 5 | API/RPC 接口契约 | 无 API/RPC 变更 |
| 6 | 数据库表结构 | 无 DDL 变更 |

---

## 三、远端环境信息(environmentInfo)

| 环境 | 地址 | 可用性 | VPN要求 |
|------|------|--------|---------|
| 测试环境(SIT) | 待部署后确认 | 需提测后才有环境 | 否（内网直连） |
| 单元测试环境 | 本地 JVM | 即时可用 | 否 |

**说明**: 本需求为纯后端日志增强，核心测试全部可通过单元测试在本地 JVM 环境完成，不依赖远端服务。集成验证（查看实际日志输出）需在 SIT 环境部署后进行，但不阻塞单元测试用例生成。

---

## 四、登录与鉴权

| 维度 | 内容 |
|------|------|
| 登录方式 | 不适用（单元测试无需登录） |
| Token获取 | 不适用 |
| 测试账号来源 | 不适用 |
| 鉴权方式 | 不适用（本次改造不涉及API/RPC鉴权） |

**说明**: 本需求改造对象为 CS 服务内部 Service/Cache 层方法，不涉及 REST API 端点和 RPC 接口，测试无需登录鉴权。

---

## 五、测试数据策略(dataStrategy)

> 本需求造数策略由分析推导（非用户主动跳过），因需求文档和设计文档已提供明确的测试前置条件。

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|---------|------|---------|
| ContextID 测试对象 | Mock 对象（TestContextID） | 无外部依赖 | Mock对象自动GC，无需清理 |
| ContextKey 测试对象 | Mock 对象（TestContextKey） | 无外部依赖 | 同上 |
| ContextValue 测试对象 | Mock 对象（TestContextValue） | 无外部依赖 | 同上 |
| ContextKeyValue 测试列表 | Mock List<ContextKeyValue> | 无外部依赖 | 同上 |
| RemovalNotification 测试对象 | 构造 Guava RemovalNotification | 无外部依赖 | 同上 |
| CacheStats 测试对象 | Mock Guava CacheStats | 无外部依赖 | 同上 |

**造数方法说明**: 本需求测试数据全部通过 Mock 框架（Mockito）构造内存对象，不依赖数据库、不依赖外部服务。现有测试目录已有 `TestContextID`、`TestContextKey`、`TestContextValue`、`TestContextKeyValue` 等测试数据类可复用。

**清理策略**: 纯 Mock 对象测试，无数据污染风险，无需回滚。

---

## 六、边界与异常约定

| 边界/异常场景 | 预期行为 | 测试要点 |
|--------------|---------|---------|
| `intervalMills = 0` | scheduleStatsLog 直接 return，不注册调度 | 验证不注册调度任务 |
| `intervalMills < 0`（负数） | 同上（<=0 判定） | 验证负数也关闭 |
| `contextKeyValueList = null` | keyValueSize = 0，cost 正常输出 | 验证null安全 |
| `ContextIDMetric = null` | cachedMillis = -1，日志正常输出 cause | 验证null安全 |
| `ContextIDMetric.getCachedTime() <= 0` | cachedMillis = -1 | 验证边界值 |
| `RemovalCause.SIZE` | warn 级别 + tuning 提示 | 验证warn级别和提示文案 |
| `RemovalCause.EXPIRE/COLLECTED/EXPLICIT/REPLACED` | info 级别，无 tuning 提示 | 验证info级别 |
| `printCacheStats 抛异常` | try/catch 捕获，warn 日志，调度不中断 | 验证异常兜底 |
| `scheduleStatsLog 注册失败` | try/catch 捕获，warn 日志，不影响Cache构造 | 验证注册异常兜底 |
| `getIfPresent == null`（未命中） | warn 日志 "not in cache, will load from db" | 验证未命中warn |
| `getIfPresent != null`（命中） | 无额外warn日志 | 验证命中时不打warn |
| `put 首次写入` | 日志 "Put new ContextID(X)" | 验证新增日志 |
| `put 已存在替换` | 日志 "Replace ContextID(X)" | 验证替换日志 |
| `searchContextValue 返回空列表` | resultSize=0（真实结果数） | 验证先执行后打日志 |
| `keyValue == null`（setValueByKey） | update=false | 验证 update 字段为 false |
| `keyValue != null`（setValueByKey） | update=true | 验证 update 字段为 true |
| `clearAllByHAID 异常` | warn日志含 haid + successNum | 验证异常分支日志增强 |
| `clearAllByID 异常` | error日志含 source + successNum + error | 验证异常分支日志增强 |

---

## 七、接口契约约定

| 维度 | 内容 |
|------|------|
| 鉴权方式 | 不适用（内部方法调用，非API/RPC） |
| 限流/熔断 | 不适用（无新增接口） |
| 幂等性 | 不适用（日志输出天然幂等） |
| 版本兼容 | 日志字段新增向后兼容，旧字段保留，日志解析脚本需适配新字段 |
| 配置热加载 | `wds.linkis.cs.cache.stats.log.interval.mills` 非热加载，修改需重启 CS 服务 |
| 字段必填/选填 | 日志参数均为已存在对象的 getter，无空值风险（已有null安全处理） |

---

## 八、澄清问答记录(clarificationLog)

| 轮次 | 维度 | 问题 | 回答 |
|:---:|------|------|------|
| 1 | 远端环境 | CS服务测试需要远端环境地址吗？是否需要VPN/内网？ | 纯后端日志增强，单元测试在本地JVM即可完成，不依赖远端服务。集成验证需SIT环境但不阻塞用例生成。 |
| 1 | 登录鉴权 | 测试是否需要登录/token/账号？ | 不适用。改造对象为内部Service/Cache层方法，不涉及API/RPC端点。 |
| 2 | 被测范围 | 哪些功能点必测？哪些不测？ | 20个必测项覆盖全部AC和所有代码变更方法。不测项：Guava框架自带功能、公共调度器实现、其他服务模块、API/RPC/DDL。 |
| 2 | 测试数据 | 测试数据如何构造？需要数据库准备吗？ | 全部通过Mockito构造内存对象，现有测试目录已有TestContextID等可复用。不依赖数据库。 |
| 2 | 边界异常 | 有哪些关键边界和异常场景？ | 18个边界/异常场景已识别，包括intervalMills=0、null安全、SIZE驱逐、调度异常兜底、未命中warn、put新增/替换区分等。 |

---

## 九、待补充项(qualityWarnings)

| 序号 | 待补充项 | 影响 | 建议 |
|:---:|---------|------|------|
| 1 | SIT环境地址待部署后确认 | 集成验证（查看实际日志输出格式）需远端环境 | 单元测试不阻塞，集成验证在Stage 7补充 |
| 2 | 日志解析脚本适配新字段 | 运维侧日志解析可能需适配新增的csType/csScope/value等字段 | 建议通知运维团队日志格式变更 |
| 3 | 缓存统计周期验证需等待30min | AC1的集成验证需等待一个调度周期 | 单元测试通过Mock调度器验证，集成验证缩短intervalMills到秒级 |

---

## 十、测试框架与工具

| 维度 | 内容 |
|------|------|
| 测试框架 | JUnit 5 + Mockito（CLAUDE.md D5 确认） |
| 日志验证方式 | 使用 LogCaptor / ListAppender 捕获日志输出验证字段 |
| Mock 对象 | 复用现有 `TestContextID` / `TestContextKey` / `TestContextValue` / `TestContextKeyValue` |
| 测试目录 | `linkis-public-enhancements/linkis-cs-server/src/test/java/org/apache/linkis/cs/` |
| 构建命令 | `mvn test -pl linkis-public-enhancements/linkis-cs-server` |

---

## 十一、风险测试增量

本需求 context.md 中无 `stabilityRisks` 和 `stabilityRiskTestRequirements`，无风险测试增量。

---

## 十二、完成度评估

| 评估维度 | 权重 | 得分 | 说明 |
|---------|:---:|:---:|------|
| P0覆盖率 | 0.4 | 0.38 | 远端环境/登录鉴权/测试数据/被测范围 全部覆盖（95%） |
| 回答质量 | 0.3 | 0.28 | 回答具体到被测对象，无含糊 |
| 边界明确度 | 0.2 | 0.18 | 18个边界/异常场景已识别 |
| 约束清晰度 | 0.1 | 0.08 | 必测/选测/不测明确区分 |
| **合计** | **1.0** | **0.92** | **门控通过** |
