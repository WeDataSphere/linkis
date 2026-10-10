# ContextService日志优化 测试用例

**需求编号**: REQ-02
**需求名称**: ContextService日志优化
**所属版本**: LinkisV2.1.4
**子系统**: WDSL-UJES (5435, ps-cs 后端)
**需求属性**: 后端开发
**测试类型**: 单元测试（JUnit 5 + Mockito）
**生成日期**: 2026-10-08
**关联commits**: 9235b6aec, 38e156050

---

## 一、测试范围总览

### 1.1 被测对象

| 序号 | 文件 | Commit | 修改类型 | 说明 |
|:---:|------|--------|:--------:|------|
| 1 | `DefaultContextCache.java` | 9235b6aec | 修改 | 新增 scheduleStatsLog + printCacheStats + get未命中warn + put区分new/replace + error补异常对象 |
| 2 | `ContextIDValueGeneratorImpl.java` | 9235b6aec | 修改 | createContextIDValue 增加 startTime + keyValueSize + cost ms 日志 |
| 3 | `ContextIDRemoveListener.java` | 9235b6aec | 修改 | onRemoval 增加 RemovalCause + cachedMillis，SIZE驱逐走warn |
| 4 | `ContextCacheConf.java` | 9235b6aec | 修改 | 新增 CACHE_STATS_LOG_INTERVAL_MILLS 配置常量 |
| 5 | `ContextIDServiceImpl.java` | 38e156050 | 修改 | createContextID/updateContextID 日志补 csIdType 字段 |
| 6 | `ContextServiceImpl.java` | 38e156050 | 修改 | getContextValue/setValueByKey/setValue/searchContextValue/resetValue/removeValue/removeAllValue/removeAllValueByKeyPrefix 日志补 csType/csScope/value/keywords/resultSize/update 等字段 |

### 1.2 测试策略

本需求为纯后端日志增强，核心测试全部通过单元测试（JUnit 5 + Mockito）在本地 JVM 环境完成，不依赖远端服务。日志验证使用 ListAppender / LogCaptor 捕获日志输出验证字段。Mock 对象复用现有 `TestContextID` / `TestContextKey` / `TestContextValue` / `TestContextKeyValue` 等测试数据类。

### 1.3 验收标准覆盖

| AC编号 | 验收标准 | 必测项数 | 状态 |
|:------:|---------|:------:|:----:|
| AC1 | 缓存统计周期输出 | 2 | 待测 |
| AC2 | 间隔配置为0关闭统计 | 1 | 待测 |
| AC3 | ContextID加载耗时埋点 | 1 | 待测 |
| AC4 | SIZE驱逐warn | 1 | 待测 |
| AC5 | 非容量驱逐走info | 1 | 待测 |
| AC6 | ContextService KV日志补全 | 4 | 待测 |
| AC7 | ContextIDService日志补csIdType | 2 | 待测 |
| AC8 | 调度异常不传播 | 1 | 待测 |

---

## 二、必测项用例 (mustTest)

### TC001: 缓存统计周期调度 - 默认30min触发

**来源**: 代码变更分析 - DefaultContextCache.java, scheduleStatsLog() / printCacheStats()
**关联AC**: AC1
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- DefaultContextCache 实例可正常构造
- ContextCacheConf.CACHE_STATS_LOG_INTERVAL_MILLS 使用默认值 1800000（30min）
- Guava Cache 已开启 recordStats()

**测试步骤**:
1. 构造 DefaultContextCache 实例，验证构造期 scheduleStatsLog() 被调用
2. 验证日志输出 "ContextID cache stats log is scheduled, interval mills:1800000"
3. 触发若干次 ContextID 读写（命中 + 未命中）
4. 等待一个调度周期（Mock调度器执行 printCacheStats）
5. 验证日志输出 "ContextID cache stats, size:X, requestCount:Y, hitCount:Z, missCount:W, hitRate:P, evictionCount:Q"

**预期结果**:
- 构造期日志包含 "ContextID cache stats log is scheduled, interval mills:1800000"
- printCacheStats 日志包含 size、requestCount、hitCount、missCount、hitRate、evictionCount 字段
- 日志级别为 INFO

**测试数据**: Mock ContextID 对象、Mock CacheStats

---

### TC002: 缓存统计间隔=0关闭调度

**来源**: 代码变更分析 - DefaultContextCache.java, scheduleStatsLog()
**关联AC**: AC2
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- 配置 `wds.linkis.cs.cache.stats.log.interval.mills=0`
- ContextCacheConf.CACHE_STATS_LOG_INTERVAL_MILLS 值为 0

**测试步骤**:
1. 设置 ContextCacheConf.CACHE_STATS_LOG_INTERVAL_MILLS = 0
2. 构造 DefaultContextCache 实例
3. 验证 scheduleStatsLog() 中 intervalMills <= 0 时直接 return
4. 验证不注册调度任务（无 "scheduled" 日志输出）
5. 验证无统计日志输出

**预期结果**:
- 无 "ContextID cache stats log is scheduled" 日志
- 无 "ContextID cache stats" 日志
- 调度器 scheduleAtFixedRate 未被调用

**测试数据**: 配置值 intervalMills = 0

---

### TC003: 调度异常不传播

**来源**: 代码变更分析 - DefaultContextCache.java, scheduleStatsLog() / printCacheStats() 双层try/catch
**关联AC**: AC8
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- DefaultContextCache 实例已构造
- Mock 使 printCacheStats 抛出 RuntimeException

**测试步骤**:
1. 构造 DefaultContextCache 实例，正常调度注册
2. Mock cache.stats() 方法抛出 RuntimeException
3. 触发 printCacheStats 执行
4. 验证异常被 try/catch 捕获
5. 验证 warn 日志 "Failed to print ContextID cache stats" 被输出
6. 验证调度任务不中断（scheduleAtFixedRate 不终止）

**预期结果**:
- printCacheStats 异常被捕获，输出 warn 日志
- 调度任务不中断，下一次周期仍可执行
- 异常不传播到外部调用方

**测试数据**: Mock cache.stats() 抛出 RuntimeException("test exception")

---

### TC004: ContextID加载耗时埋点

**来源**: 代码变更分析 - ContextIDValueGeneratorImpl.java, createContextIDValue()
**关联AC**: AC3
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextIDValueGeneratorImpl 实例可正常创建
- Mock contextMapPersistence.getAll(contextID) 返回非空 List<ContextKeyValue>

**测试步骤**:
1. 构造 ContextIDValueGeneratorImpl 实例
2. Mock contextMapPersistence.getAll 返回 3 个 ContextKeyValue
3. 调用 createContextIDValue(contextID)
4. 验证日志输出 "Finished to createContextIDValue of ContextID(X), keyValue size:3, cost:Z ms"

**预期结果**:
- 日志包含 contextID.getContextId()
- 日志包含 keyValue size（值为 3，与 Mock 返回列表大小一致）
- 日志包含 cost ms（值为非负整数）
- 日志级别为 INFO

**测试数据**: Mock ContextID (contextId="test-cs-001")，Mock List<ContextKeyValue> (size=3)

---

### TC005: SIZE驱逐warn日志

**来源**: 代码变更分析 - ContextIDRemoveListener.java, onRemoval()
**关联AC**: AC4
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextIDRemoveListener 实例可正常创建
- 构造 RemovalNotification，RemovalCause = SIZE
- Mock ContextIDValue.getContextIDMetric() 返回有效 metric（cachedTime > 0）

**测试步骤**:
1. 构造 RemovalNotification，设置 RemovalCause.SIZE
2. Mock value.getContextIDMetric() 返回 ContextIDMetric（cachedTime = System.currentTimeMillis() - 60000）
3. 调用 onRemoval(removalNotification)
4. 验证日志级别为 WARN
5. 验证日志包含 "cause:SIZE"
6. 验证日志包含 cachedMillis（值 > 0）
7. 验证日志包含 "the cache reaches the max size, please consider tuning wds.linkis.cs.cache.size"

**预期结果**:
- 日志级别为 WARN
- 日志包含 contextIDStr、cause=SIZE、cachedMillis
- 日志包含 tuning 提示 "please consider tuning wds.linkis.cs.cache.size"

**测试数据**: RemovalNotification(contextIDStr="test-cs-002", cause=SIZE), ContextIDMetric(cachedTime=now-60000)

---

### TC006: 非容量驱逐info日志

**来源**: 代码变更分析 - ContextIDRemoveListener.java, onRemoval()
**关联AC**: AC5
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextIDRemoveListener 实例可正常创建
- 构造 RemovalNotification，RemovalCause = EXPIRE
- Mock ContextIDValue.getContextIDMetric() 返回有效 metric

**测试步骤**:
1. 构造 RemovalNotification，设置 RemovalCause.EXPIRE
2. Mock value.getContextIDMetric() 返回 ContextIDMetric（cachedTime > 0）
3. 调用 onRemoval(removalNotification)
4. 验证日志级别为 INFO
5. 验证日志包含 "cause:EXPIRE"
6. 验证日志包含 cachedMillis（值 > 0）
7. 验证日志不含 tuning 提示
8. 同样验证 RemovalCause.COLLECTED / EXPLICIT / REPLACED 走 info

**预期结果**:
- 日志级别为 INFO（非 WARN）
- 日志包含 contextIDStr、cause、cachedMillis
- 日志不含 "tuning" 或 "max size" 提示

**测试数据**: RemovalNotification(contextIDStr="test-cs-003", cause=EXPIRE), ContextIDMetric(cachedTime=now-120000)

---

### TC007: 缓存未命中warn日志

**来源**: 代码变更分析 - DefaultContextCache.java, get()
**关联AC**: AC1(间接)
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- DefaultContextCache 实例已构造
- Mock cache.getIfPresent 返回 null（未命中）

**测试步骤**:
1. Mock cache.getIfPresent(contextID.getContextId()) 返回 null
2. 调用 DefaultContextCache.get(contextID)
3. 验证 warn 日志 "ContextID(X) is not in cache, will load from db"

**预期结果**:
- 日志级别为 WARN
- 日志包含 contextID.getContextId()
- 日志消息为 "ContextID(X) is not in cache, will load from db"

**测试数据**: Mock ContextID (contextId="test-cs-004")

---

### TC008: put日志区分新增/替换

**来源**: 代码变更分析 - DefaultContextCache.java, put()
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- DefaultContextCache 实例已构造
- Mock ContextIDValue 对象

**测试步骤**:
1. 首次调用 put(contextIDValue)，Mock cache.getIfPresent 返回 null
2. 验证日志 "Put new ContextID(X) to cache"
3. 二次调用 put(同一contextIDValue)，Mock cache.getIfPresent 返回已有对象
4. 验证日志 "Replace ContextID(X) in cache"

**预期结果**:
- 首次 put：日志为 "Put new ContextID(X) to cache"，日志级别 INFO
- 二次 put：日志为 "Replace ContextID(X) in cache"，日志级别 INFO

**测试数据**: Mock ContextIDValue (contextID="test-cs-005")

---

### TC009: getContextValue日志补全

**来源**: 代码变更分析 - ContextServiceImpl.java, getContextValue()
**关联AC**: AC6
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextCacheService.get 返回有效 ContextKeyValue

**测试步骤**:
1. Mock contextCacheService.get(contextID, contextKey) 返回 ContextKeyValue
2. Mock keyValue.getContextValue() 返回含 value="test-value" 和 keywords="test-keywords" 的对象
3. 调用 contextServiceImpl.getContextValue(contextID, contextKey)
4. 验证日志包含 csId（contextID.getContextId()）
5. 验证日志包含 key（contextKey.getKey()）
6. 验证日志包含 csType（contextKey.getContextType()）
7. 验证日志包含 csScope（contextKey.getContextScope()）
8. 验证日志包含 value（keyValue.getContextValue().getValue()）
9. 验证日志包含 keywords（keyValue.getContextValue().getKeywords()）

**预期结果**:
- 日志格式为 "getContextValue,csId:{},key:{},csType:{},csScope:{},value:{},keywords:{}"
- 所有字段值与 Mock 对象一致

**测试数据**: ContextID(contextId="test-cs-006"), ContextKey(key="test-key", contextType="TYPE_A", contextScope="SCOPE_1"), ContextValue(value="test-value", keywords="test-keywords")

---

### TC010: setValueByKey日志补全

**来源**: 代码变更分析 - ContextServiceImpl.java, setValueByKey()
**关联AC**: AC6
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextCacheService.put 正常执行

**测试步骤**:
1. 构造 ContextID、ContextKey、ContextValue
2. 调用 contextServiceImpl.setValueByKey(contextID, contextKey, contextValue)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 key（contextKey.getKey()）
5. 验证日志包含 csType（contextKey.getContextType()）
6. 验证日志包含 csScope（contextKey.getContextScope()）
7. 验证日志包含 keywords（contextValue.getKeywords()）
8. 验证日志包含 value（contextValue.getValue()）
9. 验证日志包含 update 字段（keyValue != null 时为 true）

**预期结果**:
- 日志格式为 "setValueByKey, csId:{},key: {},csType:{},csScope:{},keywords:{},value:{},update: {}"
- 所有字段值与 Mock 对象一致

**测试数据**: ContextID(contextId="test-cs-007"), ContextKey(key="test-key", contextType="TYPE_A", contextScope="SCOPE_1"), ContextValue(value="test-value", keywords="test-keywords")

---

### TC011: setValue日志补全

**来源**: 代码变更分析 - ContextServiceImpl.java, setValue() (from db and cache)
**关联AC**: AC6
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextCacheService.put 正常执行

**测试步骤**:
1. 构造 ContextID、ContextKeyValue
2. 调用 contextServiceImpl.setValue(contextID, contextKeyValue)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 key（contextKeyValue.getContextKey().getKey()）
5. 验证日志包含 csType（contextKeyValue.getContextKey().getContextType()）
6. 验证日志包含 csScope（contextKeyValue.getContextKey().getContextScope()）
7. 验证日志包含 keywords（contextKeyValue.getContextValue().getKeywords()）
8. 验证日志包含 value（contextKeyValue.getContextValue().getValue()）
9. 验证日志包含 update 字段

**预期结果**:
- 日志格式为 "From db and cache  to setValue, csId:{},key:{},csType:{},csScope:{},keywords:{},value:{},update: {}"
- 所有字段值与 Mock 对象一致

**测试数据**: ContextID(contextId="test-cs-008"), ContextKeyValue(key="test-key", contextType="TYPE_A", contextScope="SCOPE_1", value="test-value", keywords="test-keywords")

---

### TC012: searchContextValue先执行后打日志

**来源**: 代码变更分析 - ContextServiceImpl.java, searchContextValue()
**关联AC**: AC6
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextSearch.search 返回非空 List<ContextKeyValue>

**测试步骤**:
1. Mock contextSearch.search 返回 3 个 ContextKeyValue
2. 调用 contextServiceImpl.searchContextValue(contextID, conditionMap)
3. 验证日志在 contextSearch.search 执行后输出（非执行前）
4. 验证日志包含 csId（contextID.getContextId()）
5. 验证日志包含 condition（conditionMap 内容）
6. 验证日志包含 resultSize（值为 3，与 Mock 返回列表大小一致）

**预期结果**:
- 日志格式为 "searchContextValue,csId:{},condition:{},resultSize:{}"
- resultSize 为真实结果数（3），非空
- 日志在 search 执行后输出

**测试数据**: ContextID(contextId="test-cs-009"), conditionMap={"key":"test"}, Mock返回 List<ContextKeyValue> (size=3)

---

### TC013: ContextIDService create日志补csIdType

**来源**: 代码变更分析 - ContextIDServiceImpl.java, createContextID()
**关联AC**: AC7
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextIDServiceImpl 实例可正常创建
- Mock getPersistence().createContextID 返回 ContextID

**测试步骤**:
1. Mock getPersistence().createContextID(contextID) 返回 res（contextId="test-cs-010", contextIDType="ENV")
2. 调用 contextIDServiceImpl.createContextID(contextID)
3. 验证日志包含 "createContextID,csId:test-cs-010,csIdType:ENV"

**预期结果**:
- 日志格式为 "createContextID,csId:%s,csIdType:%s"
- csId 值为 res.getContextId()
- csIdType 值为 res.getContextIDType()

**测试数据**: ContextID(contextId="test-cs-010", contextIDType="ENV")

---

### TC014: ContextIDService update日志补csIdType

**来源**: 代码变更分析 - ContextIDServiceImpl.java, updateContextID()
**关联AC**: AC7
**测试类型**: 单元测试
**优先级**: P0

**前置条件**:
- ContextIDServiceImpl 实例可正常创建
- Mock getPersistence().updateContextID 正常执行

**测试步骤**:
1. 构造 ContextID（contextId="test-cs-011", contextIDType="USER")
2. 调用 contextIDServiceImpl.updateContextID(contextID)
3. 验证日志包含 "updateContextID,csId:test-cs-011,csIdType:USER"

**预期结果**:
- 日志格式为 "updateContextID,csId:%s,csIdType:%s"
- csId 值为 contextID.getContextId()
- csIdType 值为 contextID.getContextIDType()

**测试数据**: ContextID(contextId="test-cs-011", contextIDType="USER")

---

### TC015: resetValue日志补字段

**来源**: 代码变更分析 - ContextServiceImpl.java, resetValue()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextCacheService.rest 正常执行

**测试步骤**:
1. 构造 ContextID、ContextKey
2. 调用 contextServiceImpl.resetValue(contextID, contextKey)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 key（contextKey.getKey()）
5. 验证日志包含 csType（contextKey.getContextType()）
6. 验证日志包含 csScope（contextKey.getContextScope()）

**预期结果**:
- 日志格式为 "From db and cache  resetValue, csId:{},key:{},csType:{},csScope:{}"
- 所有字段值与 Mock 对象一致

**测试数据**: ContextID(contextId="test-cs-012"), ContextKey(key="test-key", contextType="TYPE_A", contextScope="SCOPE_1")

---

### TC016: removeValue日志补字段

**来源**: 代码变更分析 - ContextServiceImpl.java, removeValue()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock getPersistence().remove 正常执行
- Mock contextCacheService.remove 正常执行

**测试步骤**:
1. 构造 ContextID、ContextKey
2. 调用 contextServiceImpl.removeValue(contextID, contextKey)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 key（contextKey.getKey()）
5. 验证日志包含 csType（contextKey.getContextType()）
6. 验证日志包含 csScope（contextKey.getContextScope()）

**预期结果**:
- 日志格式为 "From db and cache removeValue, csId:{},key:{},csType:{},csScope:{}"
- 所有字段值与 Mock 对象一致

**测试数据**: ContextID(contextId="test-cs-013"), ContextKey(key="test-key", contextType="TYPE_A", contextScope="SCOPE_1")

---

### TC017: removeAllValue日志补csIdType

**来源**: 代码变更分析 - ContextServiceImpl.java, removeAllValue()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock getPersistence().removeAll 正常执行
- Mock contextCacheService.removeAll 正常执行

**测试步骤**:
1. 构造 ContextID（contextId="test-cs-014", contextIDType="ENV"）
2. 调用 contextServiceImpl.removeAllValue(contextID)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 csIdType（contextID.getContextIDType()）

**预期结果**:
- 日志格式为 "From db and cache removeAllValue, csId:{},csIdType:{}"
- csId 值为 "test-cs-014"
- csIdType 值为 "ENV"

**测试数据**: ContextID(contextId="test-cs-014", contextIDType="ENV")

---

### TC018: removeAllValueByKeyPrefix日志补csIdType

**来源**: 代码变更分析 - ContextServiceImpl.java, removeAllValueByKeyPrefix()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock contextCacheService.removeByKeyPrefix 正常执行
- Mock getPersistence().removeByKeyPrefix 正常执行

**测试步骤**:
1. 构造 ContextID（contextId="test-cs-015", contextIDType="USER"）和 keyPrefix="test_prefix_"
2. 调用 contextServiceImpl.removeAllValueByKeyPrefix(contextID, keyPrefix)
3. 验证日志包含 csId（contextID.getContextId()）
4. 验证日志包含 csIdType（contextID.getContextIDType()）
5. 验证日志包含 keyPrefix（"test_prefix_"）

**预期结果**:
- 日志格式为 "From db and cache  removeAllValueByKeyPrefix, csId:{},csIdType:{},keyPrefix:{}"
- csId 值为 "test-cs-015"
- csIdType 值为 "USER"
- keyPrefix 值为 "test_prefix_"

**测试数据**: ContextID(contextId="test-cs-015", contextIDType="USER"), keyPrefix="test_prefix_"

---

### TC019: clearAllByHAID日志增强

**来源**: 代码变更分析 - ContextServiceImpl.java, clearAllByHAID()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock getIDPersistence().getContextIDByHAID 返回 ContextID 列表
- Mock getIDPersistence().deleteContextID 部分抛出异常

**测试步骤**:
1. Mock getIDPersistence().getContextIDByHAID("test-haid") 返回 3 个 ContextID
2. Mock 第 2 个 deleteContextID 抛出 Exception
3. 调用 contextServiceImpl.clearAllByHAID("test-haid")
4. 验证 warn 日志包含 haid（"test-haid"）
5. 验证 warn 日志包含 successNum（值为 1，前 1 个成功后才失败）

**预期结果**:
- 日志格式为 "clear all for haid : {}, successNum : {}"
- haid 值为 "test-haid"
- successNum 值为失败前已成功删除的数量

**测试数据**: haid="test-haid", Mock返回3个ContextID, 第2个删除失败

---

### TC020: clearAllByID日志增强

**来源**: 代码变更分析 - ContextServiceImpl.java, clearAllByID()
**测试类型**: 单元测试
**优先级**: P1

**前置条件**:
- ContextServiceImpl 实例可正常创建
- Mock ContextID 列表，Mock getIDPersistence().deleteContextID 部分抛出异常

**测试步骤**:
1. 构造 3 个 ContextID（含 contextId 和 source 字段）
2. Mock 第 2 个 deleteContextID 抛出 Exception("test error")
3. 调用 contextServiceImpl.clearAllByID(contextIDList)
4. 验证 error 日志包含 contextId
5. 验证 error 日志包含 source（id.getSource()）
6. 验证 error 日志包含 successNum（值为 1）
7. 验证 error 日志包含 error（e.getMessage()）

**预期结果**:
- 日志格式为 "Clear context of id {} failed, source : {}, successNum : {}, error : {}"
- contextId 值为失败项的 getContextId()
- source 值为失败项的 getSource()
- successNum 值为失败前已成功删除的数量
- error 值为异常消息

**测试数据**: 3个ContextID, 第2个(source="HA")删除失败抛出Exception("test error")

---

## 三、选测项用例 (optionalTest)

### TC021: ContextIDMetric为null时cachedMillis=-1

**来源**: 代码变更分析 - ContextIDRemoveListener.java, onRemoval()
**测试类型**: 单元测试
**优先级**: P2
**标注**: 选测

**前置条件**:
- ContextIDRemoveListener 实例可正常创建
- 构造 RemovalNotification，RemovalCause = EXPIRE
- Mock value.getContextIDMetric() 返回 null

**测试步骤**:
1. Mock value.getContextIDMetric() 返回 null
2. 构造 RemovalNotification（cause=EXPIRE）
3. 调用 onRemoval(removalNotification)
4. 验证日志包含 cachedMillis（值为 -1）
5. 验证日志正常输出 cause

**预期结果**:
- cachedMillis 值为 -1（metric==null 分支）
- 日志级别为 INFO（非 SIZE 驱逐）
- 日志正常输出 contextIDStr 和 cause

**测试数据**: RemovalNotification(contextIDStr="test-cs-016", cause=EXPIRE), metric=null

---

### TC022: contextKeyValueList为null时keyValueSize=0

**来源**: 代码变更分析 - ContextIDValueGeneratorImpl.java, createContextIDValue()
**测试类型**: 单元测试
**优先级**: P2
**标注**: 选测

**前置条件**:
- ContextIDValueGeneratorImpl 实例可正常创建
- Mock contextMapPersistence.getAll 返回 null

**测试步骤**:
1. Mock contextMapPersistence.getAll(contextID) 返回 null
2. 调用 createContextIDValue(contextID)
3. 验证日志包含 keyValue size（值为 0）
4. 验证日志包含 cost ms（正常输出，非负）

**预期结果**:
- keyValue size 值为 0（list==null 分支：null == list ? 0 : list.size()）
- cost ms 正常输出
- 不抛出 NullPointerException

**测试数据**: Mock ContextID (contextId="test-cs-017"), contextMapPersistence.getAll 返回 null

---

### TC023: 缓存统计日志调度注册失败兜底

**来源**: 代码变更分析 - DefaultContextCache.java, scheduleStatsLog() try/catch
**测试类型**: 单元测试
**优先级**: P2
**标注**: 选测

**前置条件**:
- Mock Utils.defaultScheduler().scheduleAtFixedRate 抛出异常

**测试步骤**:
1. Mock Utils.defaultScheduler() 返回的 ScheduledExecutorService，使 scheduleAtFixedRate 抛出 RejectedExecutionException
2. 构造 DefaultContextCache 实例
3. 验证 scheduleStatsLog 的 try/catch 捕获异常
4. 验证 warn 日志 "Failed to schedule ContextID cache stats log" 被输出
5. 验证 DefaultContextCache 构造不受影响（Cache 可正常使用）

**预期结果**:
- 异常被 try/catch 捕获
- warn 日志输出 "Failed to schedule ContextID cache stats log"
- DefaultContextCache 构造正常完成，cache 可正常使用

**测试数据**: Mock ScheduledExecutorService 抛出 RejectedExecutionException

---

### TC024: DefaultContextCache.get异常补异常对象

**来源**: 代码变更分析 - DefaultContextCache.java, get() error日志
**测试类型**: 单元测试
**优先级**: P2
**标注**: 选测

**前置条件**:
- DefaultContextCache 实例已构造
- Mock 使 get 方法内部抛出 Exception

**测试步骤**:
1. Mock cache.getIfPresent 和 contextIDValueGenerator.createContextIDValue 正常
2. Mock DefaultContextIDEvent 或 listenerBus.dispatchEvent 抛出 Exception
3. 调用 DefaultContextCache.get(contextID)
4. 验证 error 日志包含异常对象（e）
5. 验证日志格式从 `logger.error(errorMsg)` 变为 `logger.error(errorMsg, e)`

**预期结果**:
- error 日志包含异常堆栈信息（errorMsg + exception object e）
- 抛出 CSErrorException

**测试数据**: Mock ContextID (contextId="test-cs-018"), Mock listenerBus.dispatchEvent 抛出 Exception

---

## 四、边界场景覆盖

| 边界场景 | 用例编号 | 预期行为 |
|---------|---------|---------|
| intervalMills = 0 | TC002 | scheduleStatsLog 直接 return，不注册调度 |
| intervalMills < 0（负数） | TC002(扩展) | 同 intervalMills<=0 判定，关闭调度 |
| contextKeyValueList = null | TC022 | keyValueSize=0，cost 正常输出 |
| ContextIDMetric = null | TC021 | cachedMillis=-1，日志正常输出 cause |
| ContextIDMetric.getCachedTime() <= 0 | TC021(扩展) | cachedMillis=-1 |
| RemovalCause.SIZE | TC005 | warn 级别 + tuning 提示 |
| RemovalCause.EXPIRE/COLLECTED/EXPLICIT/REPLACED | TC006 | info 级别，无 tuning 提示 |
| printCacheStats 抛异常 | TC003 | try/catch 捕获，warn 日志，调度不中断 |
| scheduleStatsLog 注册失败 | TC023 | try/catch 捕获，warn 日志，不影响Cache构造 |
| getIfPresent == null（未命中） | TC007 | warn 日志 "not in cache, will load from db" |
| getIfPresent != null（命中） | TC007(扩展) | 无额外warn日志 |
| put 首次写入 | TC008 | 日志 "Put new ContextID(X)" |
| put 已存在替换 | TC008 | 日志 "Replace ContextID(X)" |
| searchContextValue 返回空列表 | TC012(扩展) | resultSize=0（真实结果数） |
| keyValue == null（setValueByKey） | TC010(扩展) | update=false |
| keyValue != null（setValueByKey） | TC010 | update=true |
| clearAllByHAID 异常 | TC019 | warn日志含 haid + successNum |
| clearAllByID 异常 | TC020 | error日志含 source + successNum + error |

---

## 五、验收标准覆盖检查

| AC编号 | 验收标准 | 覆盖用例 | 覆盖状态 |
|:------:|---------|---------|:-------:|
| AC1 | 缓存统计周期输出 | TC001, TC007 | 待测 |
| AC2 | 间隔配置为0关闭统计 | TC002 | 待测 |
| AC3 | ContextID加载耗时埋点 | TC004 | 待测 |
| AC4 | SIZE驱逐warn | TC005 | 待测 |
| AC5 | 非容量驱逐走info | TC006 | 待测 |
| AC6 | ContextService KV日志补全 | TC009, TC010, TC011, TC012 | 待测 |
| AC7 | ContextIDService日志补csIdType | TC013, TC014 | 待测 |
| AC8 | 调度异常不传播 | TC003 | 待测 |

**覆盖率**: 8/8 验收标准 (100%)

---

## 六、测试用例统计

### 6.1 按优先级分布

| 优先级 | 必测项 | 选测项 | 合计 |
|:------:|:------:|:------:|:----:|
| P0 | 14 | 0 | 14 |
| P1 | 6 | 0 | 6 |
| P2 | 0 | 4 | 4 |
| **合计** | **20** | **4** | **24** |

### 6.2 按测试类型分布

| 测试类型 | 数量 |
|---------|:----:|
| 单元测试 | 24 |

### 6.3 按被测类分布

| 被测类 | 用例数 |
|--------|:------:|
| DefaultContextCache | 6 (TC001, TC002, TC003, TC007, TC008, TC024) |
| ContextIDValueGeneratorImpl | 2 (TC004, TC022) |
| ContextIDRemoveListener | 2 (TC005, TC006, TC021) |
| ContextIDServiceImpl | 2 (TC013, TC014) |
| ContextServiceImpl | 10 (TC009-TC012, TC015-TC020) |
| ContextCacheConf | 1 (TC002, 间接) |

### 6.4 按分类映射（Wemind）

| Wemind分类 | 用例数 | 用例编号 |
|-----------|:------:|---------|
| 功能案例 | 20 | TC001-TC020 |
| 参数配置 | 1 | TC002 |
| 性能案例 | 1 | TC004 |
| 安全用例 | 0 | - |
| 界面案例 | 0 | - |
| 流程案例 | 0 | - |
| 接口案例 | 0 | - |

---

## 七、测试框架与工具

| 维度 | 内容 |
|------|------|
| 测试框架 | JUnit 5 + Mockito |
| 日志验证方式 | ListAppender / LogCaptor 捕获日志输出验证字段 |
| Mock 对象 | 复用现有 TestContextID / TestContextKey / TestContextValue / TestContextKeyValue |
| 测试目录 | `linkis-public-enhancements/linkis-cs-server/src/test/java/org/apache/linkis/cs/` |
| 构建命令 | `mvn test -pl linkis-public-enhancements/linkis-cs-server` |

---

## 八、测试数据策略

本需求测试数据全部通过 Mockito 构造内存对象，不依赖数据库、不依赖外部服务。

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|---------|------|---------|
| ContextID 测试对象 | Mock 对象（TestContextID） | 无外部依赖 | Mock对象自动GC |
| ContextKey 测试对象 | Mock 对象（TestContextKey） | 无外部依赖 | 同上 |
| ContextValue 测试对象 | Mock 对象（TestContextValue） | 无外部依赖 | 同上 |
| ContextKeyValue 测试列表 | Mock List<ContextKeyValue> | 无外部依赖 | 同上 |
| RemovalNotification 测试对象 | 构造 Guava RemovalNotification | 无外部依赖 | 同上 |
| CacheStats 测试对象 | Mock Guava CacheStats | 无外部依赖 | 同上 |

---

## 九、Feature覆盖率统计

本次需求无 Feature 文件（.feature），测试用例基于需求文档验收标准和代码变更分析生成。

**验收标准覆盖率**: 8/8 (100%)
