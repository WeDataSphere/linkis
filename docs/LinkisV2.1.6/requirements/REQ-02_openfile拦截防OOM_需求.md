> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/9030MdNjJlcGDxqw)

# openfile 拦截防 OOM 需求文档

**需求类型**: ENHANCE（功能增强/防护增强）
**风险等级**: 中风险（涉及公共服务的文件读取主链路）
**文档版本**: v1.0
**创建日期**: 2026-10-21
**所属版本**: Linkis 2.1.6（分支 `release_v2.1.6`）
**功能属性**: 后端（接口/服务/数据处理，不涉及前端）
**DPMS Story ID**: 555374

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | 对 linkis-publicservice 的 `openFile` 接口增加"结果集行数超限"拦截能力：超过 10w 行后继续翻页时，开关关闭仅打 warn 日志、开关打开直接返回错误提示，防止服务 OOM |
| **改动服务** | linkis-public-enhancements/linkis-pes-publicservice（fs 模块）+ linkis-commons/linkis-storage（collect 行数统计） |
| **关键改造** | ① 新增功能开关 `linkis.filesystem.resultset.openfile.limit.enable`（默认 false）；② 新增"已读取行数超 10w"判定逻辑（基于分页累计读取行数，而非文件总行数）；③ 开关打开返回错误信息提示用户；④ 开关关闭打 warn 日志可追溯 |
| **开关方式** | `CommonVars` 配置项，默认 `false`（关闭），测试环境（bdap-dev / bdap-uat）打开 |
| **降级回退** | 开关关闭时仅增加一行 warn 日志，行为与改造前完全一致；无任何主链路逻辑变化 |

---

## 1. 需求背景

### 1.1 业务背景

Linkis 任务执行完成后，用户通过前端控制台打开结果集，前端调用 `openFile` 接口分页读取 `.dolphin` 结果集文件。当前 `openFile` 接口：

- 默认 `pageSize=5000`（上限 10000，由 `FILESYSTEM_RESULTSET_ROW_LIMIT` 控制）；
- 每翻一页**重新打开文件流**，通过 `StorageResultSetReader.skip()` 从头跳过前 `start` 行（`(page-1)*pageSize`），再逐行读取当前页；
- `collect()` 把**整页数据全量装入内存**（`ArrayList<String[]>`）返回给前端。

当结果集超过 10w 行时，用户不断翻页到 10w 行之后，每次请求服务端都需要：

1. 重新打开 HDFS 文件流；
2. `skip()` 逐行跳过前面 **9.5w+ 行**（虽然只跳字节不解析，但每行都要先读 10 字节行长度再 skip，是 O(N) 的流定位）；
3. 逐行反序列化当前页并组装 `ArrayList<String[]>` 返回。

**风险点**：

- **服务压力**：翻到最后一页时，单次请求的流定位成本与文件行数成正比（10w 行 = 每次请求做约 10w 次 skip 操作）；
- **内存压力**：`collect()` 是整页内存组装，pageSize=5000 行 × N 列全量进堆；若单行数据大（接近 `LINKIS_READ_ROW_BYTE_MAX_LEN` 20m 上限）或并发翻页（多用户同时翻页），峰值内存叠加容易触发 OOM；
- **无法感知总行数**：`openFile` 返回的 `totalLine` 语义是"当前页实际读取行数"（[FileSplit.scala:100](linkis-commons/linkis-storage/src/main/scala/org/apache/linkis/storage/source/FileSplit.scala#L100)），`totalPage` 写死返回 `0`（[FsRestfulApi.java:854](linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/restful/api/FsRestfulApi.java#L854)），前端**不知道总行数/总页数**，只能一页页盲翻，翻到超大数据集时服务端无法提前拦截。

### 1.2 目标

1. **服务保护**：结果集超过 10w 行、用户翻页到 10w 行之后时，服务端能识别并拦截，避免继续对超大结果集做无意义的流定位 + 整页内存组装；
2. **开关控制**：新功能默认关闭（不影响现有行为），测试环境（bdap-dev / bdap-uat）打开验证；
3. **可追溯**：开关关闭时，超限翻页打 warn 日志记录，便于运维发现"有用户在翻超大结果集"；
4. **体验提示**：开关打开时，直接返回错误信息，明确告知用户"结果集超过 10w 行，无法继续翻页"的规则。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台运维**，我希望在开关打开时，用户翻页超过 10w 行结果集能直接被拦截并返回明确错误，防止服务 OOM |
| US-2 | 作为**平台运维**，我希望在开关关闭时，超限翻页至少打一行 warn 日志，以便发现哪些用户在翻超大结果集，为后续灰度打开开关提供依据 |
| US-3 | 作为**开发者**，希望开关默认关闭，改造后不改变任何现有行为，不影响已有用户的正常翻页 |
| US-4 | 作为**用户**，当结果集超过 10w 行时，希望能看到明确的中英文提示（如"结果集超过10w行，请使用导出功能"），而不是页面卡死或无响应 |

---

## 3. 功能描述

### 3.1 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | 新增功能开关 | 在 `WorkSpaceConfiguration.java` 新增 `FILESYSTEM_RESULTSET_OPENFILE_LIMIT_ENABLE`，配置 key `linkis.filesystem.resultset.openfile.limit.enable`，默认 `false` |
| F-2 | 已读取行数统计 | 在 `FileSplit` 中增加"累计已读取行数"统计字段（含 skip 跳过的行数），使 openFile 能判定当前请求已翻页越过 10w 行阈值 |
| F-3 | 超限判定逻辑 | 在 `FsRestfulApi.openFile` 中，分页读取前用"已读取行数"与阈值（默认 10w）比较：超限则进入拦截分支 |
| F-4 | 开关关闭：warn 日志 | 开关关闭时，超限翻页仅打印 warn 日志（含 path、用户、已读取行数），记录在服务日志中，行为不变 |
| F-5 | 开关打开：返回错误 | 开关打开时，超限翻页直接返回错误信息（含 zh/en 双语提示），提示用户结果集超过 10w 行规则，不继续读取 |
| F-6 | 阈值可配置 | 新增配置 `linkis.filesystem.resultset.openfile.limit.rows`（默认 100000），便于不同环境调整阈值 |
| F-7 | 生产配置文件注释 | `linkis-dist/package/conf/linkis-ps-publicservice.properties` 添加新配置的注释行（即使关闭也写出，便于运维感知） |

### 3.2 关键设计决策

| 编号 | 问题 | 决策 |
|:----:|------|------|
| Q1 | 判定依据用"文件总行数"还是"已翻页累计读取行数"？ | 用**已翻页累计读取行数**。openFile 分页请求是无状态 GET，每次重新打开流。若用文件总行数，需要额外扫一遍文件或依赖文件大小估算，成本高且不准确；而"已读取行数 = (page-1)*pageSize"可直接从分页参数推导，O(1) 判定，无额外 I/O（触发拦截时无需读取当前页） |
| Q2 | 为什么用 `(page-1)*pageSize` 推导而不是在 FileSplit 里计数？ | 分页场景 `skip(start)` 跳过的行数 = `(page-1)*pageSize`，这是服务端已知的分页参数，不需要额外遍历统计；但需要处理 `pageSize` 非法/`page` 非法（已由 Controller 校验）与多 FileSplit 场景（结果集目录多分片时以 max 为准） |
| Q3 | 开关默认为什么是 `false`？ | 按 CLAUDE.md §4 铁律，新功能开关默认 `false` 保守关闭。openFile 是生产主链路接口，默认关闭可保证现有用户零感知，测试环境灰度验证后再评估是否开启 |
| Q4 | 超限拦截放在哪个层级？ | 放在 `FsRestfulApi.openFile` 的 Controller 层（分页参数校验后、`fileSource.collect()` 之前）。这样在发起任何文件读取之前就能拦截，避免无意义的 skip 开销；同时不侵入 `FileSplit` 的通用读取逻辑（CSV 导出等其他调用方不受影响） |

### 3.3 与现有保护机制的关系

openFile 已存在三层保护，本次改造是**第四层（行数维度）**：

| 现有保护 | 机制 | 局限 |
|---------|------|------|
| ① 单行字节上限 | `LINKIS_READ_ROW_BYTE_MAX_LEN`（20m），`readLine()` 检查 | 只防单行超大，不防行数多 |
| ② collect 总字节上限 | `COLLECT_MAX_BYTES`（1g），`FileSplit.collect()` 累计截断 | 默认 1g 阈值对堆内存仍偏大；且"读一行加一行、超限才停"，已加载数据不释放；不防"翻页越过 10w 行"的重复 skip 压力 |
| ③ pageSize 上限 | `FILESYSTEM_RESULTSET_ROW_LIMIT`（10000），Controller 校验 | 只限单页行数，不限总翻页深度 |
| ④ **行数超限拦截（本次）** | 已翻页行数 > 10w 时拦截/告警 | 从"翻页深度"维度阻断超大结果集的持续翻页 |

---

## 4. 验收标准

### AC1: 开关关闭时行为不变（输入验证）

**前置条件**: `linkis.filesystem.resultset.openfile.limit.enable=false`（默认）

**操作步骤**:
1. 打开一个正常结果集（< 10w 行），翻页正常
2. 打开一个超大结果集（> 10w 行），翻页到 10w 行之后

**预期结果**:
- 正常结果集翻页行为与改造前完全一致（返回 metadata + fileContent + totalLine）
- 超大结果集翻页到 10w 行之后，数据照常返回（不拦截），但服务日志打印一条 warn 日志，包含 path、用户、已读取行数

### AC2: 开关打开时超限翻页返回错误（处理验证）

**前置条件**: `linkis.filesystem.resultset.openfile.limit.enable=true`

**操作步骤**:
1. 打开一个超大结果集（> 10w 行）
2. 翻页到 10w 行之后（page > 20，pageSize=5000 时）
3. 翻页在 10w 行之内正常

**预期结果**:
- 10w 行之内的翻页正常返回数据
- 10w 行之后的翻页直接返回错误信息：`结果集超过10w行，无法继续翻页读取，请使用结果集导出功能`（含英文提示）
- 不发起任何文件读取/流定位操作（无 skip 开销）

### AC3: 阈值配置生效（输入验证）

**前置条件**: 设置 `linkis.filesystem.resultset.openfile.limit.rows=5000`

**操作步骤**: 打开一个 6000 行的结果集，翻页到第 2 页（page=2, pageSize=5000）

**预期结果**:
- 已读取行数 = 5000，达到阈值（5000），触发拦截逻辑
- 开关打开时返回错误；开关关闭时打 warn 日志

### AC4: 结果集多分片场景（处理验证）

**前置条件**: 结果集目录包含多个 `.dolphin` 分片（如 `_0.dolphin`、`_1.dolphin`）

**操作步骤**: 打开多分片结果集目录，翻页

**预期结果**:
- 判定以"累计读取行数（所有分片 max 值）"为准，不误伤单分片小结果集
- 多分片合计超限时才触发拦截

### AC5: 下游导出/下载功能不受影响（输出验证）

**操作步骤**: 用 openFile 之外的功能（resultsetToExcel 下载、pipeline 导出）打开同一个超大结果集

**预期结果**:
- 下载/导出功能**不**受本次 openFile 拦截影响（拦截仅作用于 openFile 接口）
- CSV 下载仍走流式写出，行为不变

---

## 5. 配置说明

### 5.1 新增配置项

| 配置Key | 默认值 | 说明 |
|---------|--------|------|
| `linkis.filesystem.resultset.openfile.limit.enable` | `false` | openFile 结果集行数超限拦截开关。关闭时超限仅打 warn 日志；打开时超限返回错误信息 |
| `linkis.filesystem.resultset.openfile.limit.rows` | `100000` | openFile 可翻页读取的结果集行数阈值（已翻页累计读取行数超过该值触发拦截/告警） |

### 5.2 配置位置

- 模块配置类: `WorkSpaceConfiguration.java`（`linkis-pes-publicservice` fs 模块）
- 部署配置文件: `linkis-dist/package/conf/linkis-ps-publicservice.properties` 添加注释行

### 5.3 使用说明

```properties
# openFile 结果集行数超限拦截开关（默认关闭）
# 关闭：结果集超过10w行翻页时仅打warn日志
# 打开：结果集超过10w行翻页时直接返回错误信息
# 建议测试环境(bdap-dev/bdap-uat)打开验证，生产灰度后评估
# linkis.filesystem.resultset.openfile.limit.enable=false

# openFile 可翻页读取的结果集行数阈值（默认10w）
# linkis.filesystem.resultset.openfile.limit.rows=100000
```

---

## 6. 风险识别

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| 误判正常翻页 | 🟡 | 阈值判定用 `(page-1)*pageSize` 推导，若 pageSize 被前端传小（如 1000），翻到第 100 页才超 10w，与"10w 行"语义等价；但需在文档中明确阈值语义为"已翻页读取行数"而非"文件总行数"。缓解：阈值可配置 + 开关默认关闭 |
| 多分片结果集误判 | 🟡 | 多 FileSplit 时各分片行数独立，需以累计（max 或 sum）为准，避免小分片被误拦。缓解：验收标准 AC4 明确多分片判定口径 |
| 影响 openFile 主链路 | 🔴 | openFile 是生产高频接口。缓解：按 CLAUDE.md §4 铁律，开关默认 `false`，关闭时仅增加一行 warn 日志，无任何行为变化 |
| 日志噪音 | 🟢 | 开关关闭时仅超限翻页打一条 warn（低频场景），可接受 |
| 与现有 1g 字节上限叠加 | 🟢 | 行数拦截在字节上限之前（collect 前）触发，先拦截翻页深度，不叠加额外内存开销 |

---

## 7. 关联影响分析

### 7.1 高危区域对照（CLAUDE.md §10.2）

openFile 属于公共服务的文件读取链路，**不触及** §10.2 中列出的 10 个任务执行主线高危环节（主入口/拦截器链/引擎创建复用/EC 执行/回调/持久化/物料链）。但涉及公共服务 fs 模块的读取主链路，仍需遵守 §4 铁律（开关默认关闭 + 异常降级）。

### 7.2 影响清单

| 维度 | 影响 |
|------|------|
| 功能模块 | 🟡 linkis-pes-publicservice（fs 模块 `FsRestfulApi`）+ linkis-storage（`FileSplit` 行数统计，若需） |
| API 契约 | 🟢 openFile 接口参数与返回值不变；仅在开关打开且超限时新增错误返回（Message.error），不影响正常路径 |
| 前端 | 🟢 不涉及前端改动（错误信息由 Message 返回，前端现有错误处理即可展示） |
| 下游导出/下载 | 🟢 拦截仅作用于 openFile 接口，`resultsetToExcel` 下载、pipeline 导出不受影响 |
| 配置 | 🟡 新增 2 个配置项，需在 `linkis-ps-publicservice.properties` 补充注释行 |
| 兼容性 | 🟢 开关默认关闭，行为与改造前一致 |

---

## 8. 发布和回滚方案

### 8.1 发布策略

1. **测试环境先行**：在 bdap-dev / bdap-uat 环境设置 `linkis.filesystem.resultset.openfile.limit.enable=true`，验证超限拦截生效、正常翻页不受影响；
2. **生产灰度**：生产环境默认保持 `false`（仅打 warn 日志），观察一段时间确认 warn 日志量、无误判后，再灰度开启；
3. **监控指标**：
   - 超限翻页 warn 日志数量（开关关闭时）；
   - 超限翻页错误返回数量（开关打开时）；
   - publicservice 服务 GC / OOM 事件（验证拦截是否有效降低内存压力）。

### 8.2 回滚方案

- **回滚条件**: 开启后出现正常翻页被误拦、或其他 openFile 回归问题
- **回滚步骤**: 设置 `linkis.filesystem.resultset.openfile.limit.enable=false` → 刷新配置（若用 `getHotValue` 支持热加载）或重启 publicservice → 行为回退到改造前
- **回滚时间**: 配置修改 + 服务重启，约 5 分钟
- **回滚预案**: 无需代码回滚，仅通过配置开关即可降级

---

## 附录

### A.1 修改文件清单

| 序号 | 文件 | 修改类型 | 说明 |
|:----:|------|:--------:|------|
| 1 | `linkis-pes-publicservice/.../conf/WorkSpaceConfiguration.java` | 修改 | 新增 `FILESYSTEM_RESULTSET_OPENFILE_LIMIT_ENABLE` + `FILESYSTEM_RESULTSET_OPENFILE_LIMIT_ROWS` 配置 |
| 2 | `linkis-pes-publicservice/.../restful/api/FsRestfulApi.java` | 修改 | `openFile` 方法增加超限判定 + 拦截/告警分支 |
| 3 | `linkis-commons/linkis-storage/.../source/FileSplit.scala` | 修改（可选） | 若需精确累计已读取行数，增加统计字段；否则可用 `(page-1)*pageSize` 推导 |
| 4 | `linkis-dist/package/conf/linkis-ps-publicservice.properties` | 修改 | 新增配置注释行 |

### A.2 关键代码锚点（改造前已确认）

| 文件 | 行号 | 说明 |
|------|------|------|
| `FsRestfulApi.java` | 625-871 | `openFile` 方法全流程（pageSize 校验→FileSource.create→page→limitBytes→collect→返回） |
| `FsRestfulApi.java` | 643-649 | pageSize > `FILESYSTEM_RESULTSET_ROW_LIMIT`(10000) 报错 80034——超限拦截可在此区域扩展 |
| `FsRestfulApi.java` | 704 | `fileSource.page(page, pageSize)` 设置分页 |
| `FsRestfulApi.java` | 830-854 | 返回 totalLine/totalPage(0)，超限拦截提示可在此区域扩展 |
| `FileSplit.scala` | 60-66 | `page(page, pageSize)` 计算 start/end |
| `FileSplit.scala` | 76-105 | `while()` 读取循环，skip(start) 跳过前序行 |
| `FileSplit.scala` | 131-163 | `collect()` 整页内存组装 + limitBytes 截断 |
| `StorageResultSetReader.scala` | 138-150 | `skip(recordNum)` 逐行跳过（O(N) 流定位） |
| `WorkSpaceConfiguration.java` | 67-75 | `FILESYSTEM_RESULTSET_ROW_LIMIT` / `FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED` 现有配置 |

### A.3 判定逻辑参考实现（伪代码）

```java
// FsRestfulApi.openFile 中，page 参数校验后、FileSource.create 之前
boolean openFileLimitEnable = FILESYSTEM_RESULTSET_OPENFILE_LIMIT_ENABLE.getValue();
int openFileLimitRows = FILESYSTEM_RESULTSET_OPENFILE_LIMIT_ROWS.getValue();

// 已翻页读取行数 = (page-1) * pageSize（从分页参数直接推导，无额外 I/O）
long readRows = (long)(page - 1) * pageSize;
if (readRows >= openFileLimitRows) {
  if (openFileLimitEnable) {
    throw WorkspaceExceptionManager.createException(80038); // 新增错误码：结果集超过10w行（注：80037已被"列筛选最多50列"占用，沿用下一可用号80038）
  } else {
    LOGGER.warn("openFile read rows {} exceed limit {}, path: {}, user: {}",
        readRows, openFileLimitRows, path, userName); // 仅告警，不拦截
  }
}
```

### A.4 关联文档

- 需求任务上下文: `dev/versions/LinkisV2.1.6/active/REQ-02_openfile拦截防OOM/context.md`
- CLAUDE.md §4（新功能开发铁律：开关默认关闭 + 异常降级）、§9.4.4（RESTful 接口）、§9.3（CommonVars 热加载边界）

## 需求风险摘要

### 风险等级概览

| 风险总数 | 高 | 中 | 低 |
|:-------:|:--:|:--:|:--:|
| 3 | 3 | 0 | 0 |

### 风险概览

| 风险ID | 关联功能点 | 等级 | 类型 | 风险结论 | 影响范围 |
|--------|-------------|:----:|------|---------|---------|
| R-001 | F-3 超限判定逻辑<br>AC2 开关打开时超限返回错误 | 高 | 模块链路 | 改造openFile接口增加行数拦截可能导致结果集读取链路异常 | openFile结果集读取链路（FsRestfulApi.openFile改造） |
| R-002 | F-2 已读取行数统计<br>AC1 开关关闭时行为不变 | 高 | 性能容量 | openFile翻页读取超大结果集可能导致服务OOM | openFile内存（翻页读取超大结果集>10w行，collect整页内存组装） |
| R-003 | F-1 新增功能开关<br>F-6 阈值可配置<br>AC3 阈值配置生效 | 高 | 参数配置 | 新增openFile行数拦截配置开关可能导致结果集翻页行为变化 | openFile行数拦截配置（limit.enable默认false+limit.rows默认100000） |

### 测试要求

| 风险ID | 测试要求 | 判断依据 | 处理建议 |
|--------|---------|---------|---------|
| R-001 | 下游联动验证<br>跨系统调用异常验证<br>级联失败恢复验证 | 修改openFile主链路接口，虽开关默认false但触及公共服务读取主链路 | 设计阶段保持开关默认false；AC1/AC2覆盖开关on/off |
| R-002 | 压测<br>容量评估验证 | OOM防护是本次改造目标，但需压测验证阈值有效性 | Stage5.5覆盖并发翻页压测；AC1验证开关关闭行为不变 |
| R-003 | 配置变更生效验证<br>配置回滚验证<br>配置边界值验证 | 新增配置项，需验证生效/回滚/边界 | AC3覆盖阈值配置生效；§8.2有回滚方案 |
