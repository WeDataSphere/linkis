> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/KrkEVzDeb4T6MEAJ)

# ContextService 日志增强 需求文档

**需求类型**: OPTIMIZE（优化）
**风险等级**: 低风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端（不涉及前端）
**关联 commits**: `9235b6aec`（content service 增加日志打印）、`38e156050`（cs 服务增强已有日志入参与结果字段）

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | ContextService（CS 服务）全链路补充缓存统计、命中/未命中、KV 读写、移除原因等日志字段，提升线上 CS 缓存问题排查能力 |
| **改动服务** | linkis-ps-cs（linkis-cs-server，单模块） |
| **改动范围** | `DefaultContextCache`、`ContextIDValueGeneratorImpl`、`ContextIDRemoveListener`、`ContextCacheConf`、`ContextIDServiceImpl`、`ContextServiceImpl` |
| **新增配置** | `wds.linkis.cs.cache.stats.log.interval.mills`（默认 30 分钟） |
| **开关方式** | 间隔 ≤0 时关闭统计日志调度 |

---

## 1. 需求背景

### 1.1 业务背景

ContextService 负责跨任务上下文共享，内部使用 Guava Cache 缓存 ContextID → ContextIDValue 映射。现网存在以下排查痛点：

1. **缓存命中率不可见**：Guava Cache 虽 `recordStats()` 已开启，但无周期性输出，运维无法感知命中/驱逐趋势；
2. **加载耗时无埋点**：ContextID 从 DB 加载 KV 列表耗时缺失，无法定位"慢加载"问题；
3. **驱逐原因不可见**：`ContextIDRemoveListener` 仅记录 "Start to remove"，缺少 `RemovalCause`（SIZE/EXPIRE/COLLECTED...）和缓存驻留时长，无法判断是否因容量不足导致频繁驱逐；
4. **KV 读写日志字段不全**：`ContextServiceImpl` 的 `getContextValue`/`setValueByKey`/`setValue` 等方法日志仅含 csId + key，缺少 csType/csScope/value/keywords/update 等关键字段，排查"哪个上下文键写错了"需要翻代码；
5. **create/update ContextID 日志缺 type**：`ContextIDServiceImpl` 日志仅含 csId，无 `contextIDType`，无法区分环境上下文类型。

### 1.2 目标

1. 周期性输出 ContextID 缓存统计（size/请求/命中/未命中/命中率/驱逐数）；
2. ContextID 加载耗时埋点（cost ms + KV size）；
3. 缓存移除日志增加 `RemovalCause` + 驻留时长，SIZE 驱逐单独 warn 告警；
4. CS Service 层 KV 读写日志补全 csType/csScope/value/keywords/update 字段；
5. ContextID create/update 日志补全 contextIDType 字段。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台运维**，我希望每 30 分钟看到一次 ContextID 缓存的命中率和驱逐数，以便发现缓存容量不足或命中率异常问题 |
| US-2 | 作为**平台运维**，希望在缓存因 SIZE 驱逐时看到 warn 日志和驻留时长，以便判断是否需要调大 `wds.linkis.cs.cache.size` |
| US-3 | 作为**开发**，希望在 ContextID 从 DB 加载耗时过高时通过日志快速定位（含 cost ms + KV size） |
| US-4 | 作为**开发**，希望 CS 服务的 getContextValue/setValueByKey 等日志包含 csType/csScope/value/keywords，以便排查上下文键值错乱问题 |

---

## 3. 功能描述

### 3.1 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | 缓存统计周期调度 | `DefaultContextCache` 构造期通过 `Utils.defaultScheduler().scheduleAtFixedRate` 周期输出 `CacheStats`（size/requestCount/hitCount/missCount/hitRate/evictionCount） |
| F-2 | 统计间隔可配置 | 新增 `wds.linkis.cs.cache.stats.log.interval.mills`（默认 30*60*1000=30min），≤0 时不调度（关闭统计日志） |
| F-3 | 调度异常兜底 | `scheduleStatsLog` 和 `printCacheStats` 均用 try/catch 包裹，异常仅 warn 不抛出 |
| F-4 | 加载耗时埋点 | `ContextIDValueGeneratorImpl.createContextIDValue` 增加 `startTime` + `keyValueSize`，完成日志含 `cost ms` |
| F-5 | 移除原因日志增强 | `ContextIDRemoveListener` 增加 `RemovalCause` + `cachedMillis`（驻留时长），SIZE 驱逐走 warn 提示调优 `wds.linkis.cs.cache.size` |
| F-6 | 缓存未命中 warn | `DefaultContextCache.get` 中 `getIfPresent == null` 时打 warn 日志（ContextID not in cache, will load from db） |
| F-7 | put 日志区分新增/替换 | `DefaultContextCache.put` 通过 `getIfPresent` 判断 isNew，新增打 "Put new"，替换打 "Replace" |
| F-8 | ContextIDService 日志补字段 | `ContextIDServiceImpl` 的 create/update 日志补 `csIdType`（即 `contextIDType`） |
| F-9 | ContextService KV 日志补字段 | `ContextServiceImpl` 的 getContextValue/setValueByKey/setValue/searchContextValue 日志补 `csType/csScope/value/keywords/resultSize/update` 等字段 |
| F-10 | searchContextValue 先执行后打日志 | 调整执行顺序：先 `contextSearch.search` 拿到 result，再打日志含 `condition/resultSize`，避免日志先于执行打出空结果 |

### 3.2 关键代码锚点

| 文件 | 行号 | 说明 |
|------|------|------|
| `DefaultContextCache.java` | 构造期 | 新增 `scheduleStatsLog()` + `printCacheStats()` |
| `ContextIDValueGeneratorImpl.java` | `createContextIDValue` | 增加 startTime + keyValueSize + cost 日志 |
| `ContextIDRemoveListener.java` | `onRemoval` | 增加 RemovalCause + cachedMillis，SIZE 走 warn |
| `ContextCacheConf.java` | 末尾 | 新增 `CACHE_STATS_LOG_INTERVAL_MILLS` |
| `ContextIDServiceImpl.java` | `createContextID/updateContextID` | 日志补 csIdType |
| `ContextServiceImpl.java` | `getContextValue/setValueByKey/setValue/searchContextValue` | 日志补 csType/csScope/value/keywords/resultSize/update |

---

## 4. 验收标准

### AC1: 缓存统计周期输出

**前置条件**: CS 服务正常运行，`wds.linkis.cs.cache.stats.log.interval.mills` 使用默认 30 分钟

**操作步骤**:
1. 触发若干次 ContextID 读写（命中 + 未命中）
2. 等待一个调度周期

**预期结果**:
- 日志输出 `ContextID cache stats, size:X, requestCount:Y, hitCount:Z, missCount:W, hitRate:P, evictionCount:Q`
- 日志输出 `ContextID cache stats log is scheduled, interval mills:1800000`

### AC2: 间隔配置为 0 关闭统计

**前置条件**: `wds.linkis.cs.cache.stats.log.interval.mills=0`

**预期结果**:
- `scheduleStatsLog` 直接 return
- 不注册调度任务
- 无统计日志输出

### AC3: ContextID 加载耗时埋点

**操作步骤**: 触发一个未命中的 ContextID 加载

**预期结果**:
- 日志含 `Finished to createContextIDValue of ContextID(X), keyValue size:Y, cost:Z ms`

### AC4: SIZE 驱逐 warn

**前置条件**: 设置较小 `wds.linkis.cs.cache.size`，制造容量驱逐

**预期结果**:
- 日志 warn 级别输出 `ContextID(X) is removed from cache, cause:SIZE, cachedMillis:Y, the cache reaches the max size, please consider tuning wds.linkis.cs.cache.size`

### AC5: 非容量驱逐走 info

**预期结果**:
- EXPIRE/COLLECTED 等驱逐走 info 级别，日志含 cause + cachedMillis，无 tuning 提示

### AC6: ContextService KV 日志补全

**操作步骤**: 调用 setValueByKey / getContextValue / searchContextValue

**预期结果**:
- setValueByKey 日志含 `csId,key,csType,csScope,keywords,value,update`
- getContextValue 日志含 `csId,key,csType,csScope,value,keywords`
- searchContextValue 日志含 `csId,condition,resultSize`，且 resultSize 为真实结果数（非空）

### AC7: ContextIDService 日志补 csIdType

**操作步骤**: 调用 createContextID / updateContextID

**预期结果**:
- 日志含 `csId:X,csIdType:Y`

### AC8: 调度异常不传播

**操作步骤**: 模拟 `printCacheStats` 抛异常

**预期结果**:
- 异常被 try/catch 捕获，warn 日志记录
- 调度任务不中断（`scheduleAtFixedRate` 对捕获的异常不终止）

---

## 5. 配置说明

| 配置Key | 默认值 | 说明 |
|---------|--------|------|
| `wds.linkis.cs.cache.stats.log.interval.mills` | `1800000`（30 分钟） | ContextID 缓存统计日志调度间隔（毫秒），≤0 关闭 |

配置位置: `linkis-cs-server/.../contextcache/conf/ContextCacheConf.java`

---

## 6. 风险识别

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| 调度任务泄漏 | 🟢 | `scheduleAtFixedRate` 使用 `Utils.defaultScheduler()`（JVM 全局调度器），生命周期跟随 JVM，无独立线程池泄漏风险 |
| 日志量增加 | 🟡 | 缓存统计每 30min 一条，可接受；KV 读写日志每调用一次输出一条，高频调用场景可能产生日志噪音——通过 INFO 级别过滤即可 |
| 异常未兜底导致调度中断 | 🟢 | `scheduleStatsLog` + `printCacheStats` 双层 try/catch，异常不传播 |

---

## 7. 关联影响分析

| 维度 | 影响 |
|------|------|
| 功能模块 | 🟢 仅 linkis-cs-server 模块，不影响其他服务 |
| 数据模型 | 🟢 无表结构变更 |
| 性能 | 🟢 统计走 Guava `CacheStats`（O(1)），不影响业务路径 |
| 兼容性 | 🟢 新增字段仅日志层面，不影响 API/RPC |

---

## 附录

### A.1 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `DefaultContextCache.java` | 修改 | 新增 scheduleStatsLog + printCacheStats + put 日志区分 new/replace + get 未命中 warn |
| `ContextIDValueGeneratorImpl.java` | 修改 | createContextIDValue 增加 cost + keyValueSize 日志 |
| `ContextIDRemoveListener.java` | 修改 | onRemoval 增加 RemovalCause + cachedMillis，SIZE 走 warn |
| `ContextCacheConf.java` | 修改 | 新增 CACHE_STATS_LOG_INTERVAL_MILLS |
| `ContextIDServiceImpl.java` | 修改 | create/update 日志补 csIdType |
| `ContextServiceImpl.java` | 修改 | getContextValue/setValueByKey/setValue/searchContextValue 日志补字段 |

### A.2 关联文档

- 设计文档: [REQ-02_CS服务日志增强_设计.md](../design/REQ-02_CS服务日志增强_设计.md)
