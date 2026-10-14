> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/Wr3DVzDWapHREgkJ)

# ContextService 日志增强 设计文档

**需求类型**: OPTIMIZE（优化）
**风险等级**: 低风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端（不涉及前端）
**关联需求**: [REQ-02_CS服务日志增强_需求.md](../requirements/REQ-02_CS服务日志增强_需求.md)
**关联 commits**: `9235b6aec`、`38e156050`

---

## 0. 执行摘要

| 维度 | 内容 |
|------|------|
| **改造范围** | linkis-cs-server 单模块，6 个文件 |
| **核心机制** | ① Guava Cache `recordStats` 已开启，新增周期调度打印 `CacheStats`；② 加载耗时 `System.currentTimeMillis` 埋点；③ `RemovalListener` 增强 `RemovalCause` + 驻留时长；④ Service 层日志补字段 |
| **新增配置** | `wds.linkis.cs.cache.stats.log.interval.mills`（默认 30min） |
| **异常兜底** | 调度 + 打印双层 try/catch，异常不传播、不中断调度 |

---

## 1. 设计背景

ContextService 内部使用 Guava `Cache<String, ContextIDValue>` 缓存 ContextID → ContextIDValue，已通过 `.recordStats()` 开启统计收集，但缺少周期性输出机制。同时 KV 读写日志字段不全，运维排查"缓存命中率异常""ContextID 被频繁驱逐""哪个 csType 写错了"等问题时缺少线索。

详见需求文档 §1。

---

## 2. 设计方案

### 2.1 缓存统计周期调度（DefaultContextCache）

```
DefaultContextCache 构造期
  ├─ 构建 Guava Cache（recordStats 已开启）
  ├─ scheduleStatsLog()                                  【新增】
  │    ├─ 读 ContextCacheConf.CACHE_STATS_LOG_INTERVAL_MILLS
  │    ├─ intervalMills <= 0 → return（关闭统计）
  │    └─ Utils.defaultScheduler().scheduleAtFixedRate(
  │         this::printCacheStats, intervalMills, intervalMills, MS)
  │
  └─ printCacheStats()                                  【新增】
       ├─ cache.stats() → CacheStats
       └─ logger.info("ContextID cache stats, size:{}, requestCount:{},
                        hitCount:{}, missCount:{}, hitRate:{}, evictionCount:{}")
```

**关键设计点**：
1. **使用 `Utils.defaultScheduler()`**：JVM 全局调度器，符合 CLAUDE.md §8 规范，禁止 `Executors.newXxx`；
2. **调度 + 打印双层 try/catch**：`scheduleStatsLog` 捕获注册异常，`printCacheStats` 捕获运行异常——`scheduleAtFixedRate` 对方法内部捕获的异常不终止任务（只有抛出才会终止）；
3. **间隔配置化**：`wds.linkis.cs.cache.stats.log.interval.mills`，默认 30 分钟，≤0 关闭。

### 2.2 ContextID 加载耗时埋点（ContextIDValueGeneratorImpl）

```
createContextIDValue(contextID)
  ├─ startTime = System.currentTimeMillis()              【新增】
  ├─ contextKeyValueList = contextMapPersistence.getAll(contextID)
  ├─ keyValueSize = null == list ? 0 : list.size()        【新增】
  ├─ ... 原有逻辑（注册 listener 等）
  └─ logger.info("Finished to createContextIDValue of ContextID({}),
                  keyValue size:{}, cost:{} ms",
                  contextID.getContextId(), keyValueSize,
                  System.currentTimeMillis() - startTime)
```

### 2.3 缓存移除日志增强（ContextIDRemoveListener）

```
onRemoval(removalNotification)
  ├─ 原有空值校验
  ├─ RemovalCause cause = removalNotification.getCause()   【新增】
  ├─ long cachedMillis = -1L                               【新增】
  ├─ ContextIDMetric metric = value.getContextIDMetric()  【新增】
  ├─ if (metric != null && metric.getCachedTime() > 0)
  │     cachedMillis = now - metric.getCachedTime()
  ├─ listenerBus.removeListener(value)
  ├─ if (RemovalCause.SIZE == cause)                       【新增分支】
  │     logger.warn("ContextID({}) is removed, cause:SIZE, cachedMillis:{},
  │                  the cache reaches the max size,
  │                  please consider tuning wds.linkis.cs.cache.size", ...)
  └─ else
        logger.info("ContextID({}) is removed, cause:{}, cachedMillis:{}", ...)
```

**关键设计点**：SIZE 驱逐单独 warn，提示运维调优 `wds.linkis.cs.cache.size`；其他原因（EXPIRE/COLLECTED/EXPLICIT/REPLACED）走 info。

### 2.4 ContextIDServiceImpl 日志补字段

```
createContextID(contextID)
  └─ logger.info("createContextID,csId:{},csIdType:{}",     【补字段】
                  res.getContextId(), res.getContextIDType())

updateContextID(contextID)
  └─ logger.info("updateContextID,csId:{},csIdType:{}",     【补字段】
                  contextID.getContextId(), contextID.getContextIDType())
```

### 2.5 ContextServiceImpl KV 读写日志补字段

| 方法 | 原日志字段 | 新增字段 |
|------|-----------|---------|
| `getContextValue` | csId, key, csType, csScope | value, keywords |
| `searchContextValue` | csId | condition, resultSize（**先执行 search 再打日志**，避免空结果） |
| `setValueByKey` | csId, key, keywords | csType, csScope, value, update（keyValue != null） |
| `setValue`（from db and cache） | csId, key | csType, csScope, keywords, value, update |

**关键设计点**：`searchContextValue` 调整为先 `contextSearch.search` 拿到 result 再打日志，确保 `resultSize` 为真实结果数，避免先打日志再执行导致日志先于结果输出。

### 2.6 DefaultContextCache.get / put 日志增强

| 方法 | 原行为 | 新行为 |
|------|--------|--------|
| `get` | getIfPresent==null 时静默从 DB 加载 | 打 warn："ContextID(X) is not in cache, will load from db" |
| `get` 异常 | logger.error(errorMsg) | logger.error(errorMsg, e)（补异常对象） |
| `put` | "update contextID:{}" | 通过 `getIfPresent` 判断 isNew：新增打 "Put new ContextID(X)"，替换打 "Replace ContextID(X)" |

---

## 3. 关键代码位置

### 3.1 缓存统计调度

| 文件 | 行号 | 说明 |
|------|------|------|
| `DefaultContextCache.java` | `:80-113` | `scheduleStatsLog()` + `printCacheStats()` |
| `ContextCacheConf.java` | 末尾 | `CACHE_STATS_LOG_INTERVAL_MILLS` |

### 3.2 加载耗时与移除日志

| 文件 | 行号 | 说明 |
|------|------|------|
| `ContextIDValueGeneratorImpl.java` | `:96, :126-130` | startTime + keyValueSize + cost 日志 |
| `ContextIDRemoveListener.java` | `:51-74` | RemovalCause + cachedMillis + SIZE warn |

### 3.3 Service 层日志补字段

| 文件 | 行号 | 说明 |
|------|------|------|
| `ContextIDServiceImpl.java` | `:59-64, :73-78` | create/update 补 csIdType |
| `ContextServiceImpl.java` | `:88-99, :103-113, :126-135, :148-158, :199-210` | KV 读写补字段 |

---

## 4. 异常与降级

| 场景 | 处理 |
|------|------|
| `scheduleStatsLog` 注册失败 | try/catch 捕获，warn 日志，不影响 Cache 构造 |
| `printCacheStats` 抛异常 | try/catch 捕获，warn 日志，调度不中断 |
| `ContextIDMetric` 为 null | cachedMillis 保持 -1，日志正常输出 cause |
| `contextKeyValueList` 为 null | keyValueSize=0，cost 正常输出 |

---

## 5. 配置项

| Key | 默认值 | 读取方式 | 说明 |
|-----|--------|---------|------|
| `wds.linkis.cs.cache.stats.log.interval.mills` | `1800000` | `CommonVars.apply().getValue()` | 统计日志调度间隔，≤0 关闭 |

> 注：该配置使用 `CommonVars.apply(...).getValue()`，非热加载，修改需重启 CS 服务。

---

## 6. 兼容性与影响

| 维度 | 影响 |
|------|------|
| API | 🟢 无签名变更 |
| RPC/Protocol | 🟢 无 |
| 数据库 | 🟢 无表结构变更 |
| 性能 | 🟢 统计 O(1)，调度每 30min 一次 |
| 日志 | 🟡 KV 读写日志字段增加，日志解析脚本需适配新字段（向后兼容，旧字段保留） |
| 兼容性 | 🟢 新增配置默认值与原行为一致（无统计调度），不影响现网 |

---

## 7. 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `DefaultContextCache.java` | 修改 | scheduleStatsLog + printCacheStats + put 日志区分 + get 未命中 warn + error 补异常 |
| `ContextIDValueGeneratorImpl.java` | 修改 | createContextIDValue cost + keyValueSize |
| `ContextIDRemoveListener.java` | 修改 | RemovalCause + cachedMillis + SIZE warn |
| `ContextCacheConf.java` | 修改 | 新增 CACHE_STATS_LOG_INTERVAL_MILLS |
| `ContextIDServiceImpl.java` | 修改 | create/update 补 csIdType |
| `ContextServiceImpl.java` | 修改 | getContextValue/setValueByKey/setValue/searchContextValue 补字段 |

---

## 8. 测试要点

1. **AC1 缓存统计周期输出**：触发读写后等待 30min，验证 stats 日志；
2. **AC2 间隔=0 关闭**：配置 `wds.linkis.cs.cache.stats.log.interval.mills=0`，验证无调度任务；
3. **AC3 加载耗时**：触发未命中加载，验证 cost + keyValueSize；
4. **AC4 SIZE 驱逐 warn**：调小 `wds.linkis.cs.cache.size`，验证 SIZE 驱逐走 warn 且含 tuning 提示；
5. **AC5 非容量驱逐 info**：验证 EXPIRE 等走 info；
6. **AC6 KV 日志补全**：调用 setValueByKey/getContextValue/searchContextValue，验证新字段；
7. **AC7 ContextIDService 日志**：验证 create/update 含 csIdType；
8. **AC8 调度异常不传播**：mock printCacheStats 抛异常，验证调度不中断。
