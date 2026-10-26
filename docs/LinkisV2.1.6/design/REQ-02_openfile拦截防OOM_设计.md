> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/16q8MwZjxPsg8Qk7)

# REQ-02 openfile 拦截防 OOM — 设计方案

> 版本：LinkisV2.1.6 ｜ 分支：release_v2.1.6 ｜ 需求类型：ENHANCE/P2 ｜ dpmsStoryId：555374
> 阶段：Stage 2 设计方案 ｜ 子系统：WDSL-UJES/5435（单 git，pipeline 1039300005284218，无灰度）
> 输入：`docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md`（已审定修正）

---

## 1. 速览

| 项 | 内容 |
| --- | --- |
| 需求摘要 | openFile 翻页读取大结果集时，`page` 过大导致底层需读 `(page-1)*pageSize` 行才能定位，内存暴涨触发 OOM；需在翻页前按累计行数拦截。 |
| 设计一句话 | 在 `FsRestfulApi.openFile` 调用 `fileSource.page(page, pageSize)` 之前，新增"翻页累计行数 `(page-1)*pageSize` 阈值拦截"，超阈抛错误码 80038；开关默认关闭，拦截逻辑 try/catch 兜底，异常时降级放行原 openFile 流程。 |
| 影响模块 | `linkis-public-enhancements/linkis-pes-publicservice`（后端纯改，不涉及前端） |
| 涉及文件 | `WorkSpaceConfiguration.java`（新增 2 个开关）、`FsRestfulApi.java`（openFile 内新增拦截段）、`WorkspaceExceptionManager.java`（新增 80038）、`docs/errorcode/`（同步）、`linkis-dist/package/conf/linkis-ps-publicservice.properties`（注释行） |
| 是否触及 §10.2 主流程 | **否**。openFile 属公共服务 workspace 文件预览，不在任务执行 10 步主线（Entrance→Orchestrator→AM→ECM→EC）内。 |

---

## 2. 整体设计

### 2.1 现状与根因（来自需求文档 + 源码核实）

`FsRestfulApi.openFile`（`FsRestfulApi.java:624`）当前对结果集的行数保护只有一处：

```java
// FsRestfulApi.java:643 —— 仅校验 pageSize 自身上限
if (pageSize > FILESYSTEM_RESULTSET_ROW_LIMIT.getValue()) {   // 默认 10000
  throw WorkspaceExceptionManager.createException(80034, FILESYSTEM_RESULTSET_ROW_LIMIT.getValue());
}
```

随后在 `FsRestfulApi.java:704` 执行：

```java
fileSource = fileSource.page(page, pageSize);   // page 默认 1，pageSize 默认 5000
```

`FileSource.page(page, pageSize)` 的语义是"取第 page 页、每页 pageSize 行"。底层读取器为定位到第 `page` 页，需先顺序跳过 `(page-1)*pageSize` 行。当用户翻到深页（如 page=1000、pageSize=5000），底层需读/缓存约 5,000,000 行后才返回目标页 → 堆内存暴涨 → OOM。

**根因**：现有保护只限 `pageSize`（单页大小），未限"为翻到目标页需累计读取的总行数" `(page-1)*pageSize`。

### 2.2 拦截点定位

| 维度 | 选择 |
| --- | --- |
| 拦截层级 | REST Controller 层（`FsRestfulApi.openFile`），与既有 80034/80036 校验同层，风格一致 |
| 拦截时机 | 在 `fileSource.page(page, pageSize)`（行 704）**之前**插入；拦截在真正 `collect()`（行 721，触发实际读取）之前生效 |
| 拦截条件 | `(page - 1) * pageSize > 阈值` 时抛 80038 |
| 降级策略 | 拦截逻辑本身异常 → 记 warn 日志 → 放行原 openFile 流程（不阻断） |

### 2.3 拦截逻辑流程

```
openFile 入参 (path, page, pageSize, ...)
        │
        ▼
[既有] pageSize > ROW_LIMIT? ──是──► 抛 80034
        │否
        ▼
[既有] columnPage/columnPageSize 范围? ──是──► 抛 80036
        │否
        ▼
┌───────────────────────────────────────────┐
│ [新增] 开关 OPENFILE_PAGE_LIMIT_ENABLED?  │
│   否 ──────────────────────────────► 跳过拦截（老路径）
│   是                                       │
│     try:                                   │
│       accRows = (page-1) * pageSize        │
│       accRows > THRESHOLD?                 │
│         是 ──► 抛 80038（业务异常，向上抛）│
│         否 ──► 继续（老路径）              │
│     catch WorkSpaceException ──► re-throw  │  ← 业务异常必须抛出给前端
│     catch Throwable ──► warn + 放行（降级）│  ← 技术异常降级，不阻断
└───────────────────────────────────────────┘
        │
        ▼
[既有] fileSource.page(page, pageSize) → collect() → 返回
```

### 2.4 开关与降级策略

- **开关默认关闭**（`false`）：保守上线，灰度验证后再开。
- **降级**：拦截逻辑用 Java `try/catch` 包裹；`WorkSpaceException`（80038 业务异常）向上抛出让前端感知；其余 `Throwable`（如阈值计算 NPE、配置读取异常）仅打 warn 日志并放行原流程，绝不因拦截自身故障导致 openFile 不可用（遵循 CLAUDE.md §4.2 Java 等价写法）。

---

## 3. 详细设计

### 3.1 开关声明（`WorkSpaceConfiguration.java`）

在既有 `FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED`（行 74）附近新增两项，命名沿用 `linkis.workspace.filesystem.*` / `linkis.filesystem.*` 前缀约定：

```java
// openFile 翻页 OOM 拦截开关，默认关闭（保守）
public static final CommonVars<Boolean> FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED =
    CommonVars$.MODULE$.apply("linkis.workspace.openfile.limit.enable", false);

// 翻页累计行数阈值：(page-1)*pageSize 超过此值则拦截，默认 100000
public static final CommonVars<Integer> FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD =
    CommonVars$.MODULE$.apply("linkis.workspace.openfile.limit.threshold", 100000);
```

**热加载说明**：本开关用 `getValue`（首次读后 JVM 快照，重启更新）。理由：该阈值为容量保护，无需"出问题立刻不重启关掉"；如运维需动态调参，改用 `getHotValue` 即可，二者 API 兼容，后续按需切换。PR 中说明此选择。

### 3.2 拦截实现（`FsRestfulApi.openFile`）

在 `FsRestfulApi.java` 行 649（`columnPage` 校验之后）与行 704（`fileSource.page`）之间插入拦截段。位置选择依据：与既有 80034/80036 参数校验同属"前置参数检查"区，且在获取 `FileSystem`/`fileSource` 之前，可尽早拒绝，避免无谓的 FS 初始化开销。

```java
// === [REQ-02 新增] 翻页 OOM 拦截：累计行数 (page-1)*pageSize 超阈值则抛 80038 ===
if (FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED.getValue()) {
  try {
    // page、pageSize 均 >=1（page 默认 1，pageSize 已被上方 80034 校验上限）
    long accRows = (long) (page - 1) * pageSize;
    int threshold = FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD.getValue();
    LOGGER.info(
        "openFile page limit check（翻页累计行数校验）: path={}, page={}, pageSize={}, "
            + "accRows={}, threshold={}",
        path, page, pageSize, accRows, threshold);
    if (accRows > threshold) {
      // 业务异常：超阈值，向上抛出让前端感知
      throw WorkspaceExceptionManager.createException(80038, threshold);
    }
  } catch (WorkSpaceException e) {
    // 业务异常（80038）必须抛出，不降级
    throw e;
  } catch (Throwable t) {
    // 拦截逻辑自身异常：降级，记录 warn 日志，放行原 openFile 流程（不阻断主链路）
    LOGGER.warn(
        "openFile page limit check failed, fallback to original flow（翻页校验失败，降级放行）, path: {}",
        path,
        t);
  }
}
// === [REQ-02 新增] 结束 ===
```

**要点**：
- `(long)(page-1)*pageSize` 强转 long 防整型溢出（page=1000、pageSize=5000 → 5_000_000，int 安全，但大阈值场景仍用 long 防御）。
- `WorkSpaceException` 单独 catch 并 re-throw：保证业务拦截语义不被降级吞掉。
- 其余 `Throwable` 降级放行：满足 §4.2 "catch 块走老路径或返回安全默认值，不把受检异常抛给上层"。

### 3.3 错误码 80038 定义（`WorkspaceExceptionManager.java`）

在 `initialDescMap()` 末尾（行 91 `80037` 之后）新增：

```java
map.put(
    "80038",
    "The page view exceeds the accumulated row limit {0}, page viewing is not supported. "
        + "Please reduce the page size or page number "
        + "(翻页累计行数超过{0}，暂不支持页面查看，请减小页码或页大小)");
```

**错误码占用核实**：80037 已被"参数错误，列筛选最多支持筛选50列"占用（`WorkspaceExceptionManager.java:90-91`），80038 空闲，无冲突。

### 3.4 异常降级路径

| 触发条件 | 行为 | 用户感知 |
| --- | --- | --- |
| 开关关闭 | 跳过拦截，走老路径 | 正常 openFile |
| 开关开 + 未超阈 | 通过，继续老路径 | 正常 openFile |
| 开关开 + 超阈 | 抛 80038（业务异常） | 前端收到 80038 错误提示"翻页累计行数超过N" |
| 开关开 + 拦截逻辑自身抛 Throwable | warn 日志 + 放行老路径 | 正常 openFile（拦截静默降级） |

---

## 4. 关键设计决策（ADR）

### ADR-001 拦截点：REST Controller 层 vs Service 层

- **Option A**：在 `FsRestfulApi.openFile`（Controller）拦截，与既有 80034/80036 同层。
- **Option B**：在 `FsService` / `FileSource.page`（Service/底层）拦截。
- **Selected**：**A**。
- **理由**：① 与既有参数校验（80034 pageSize 上限、80036 columnPage 范围）风格一致，集中可维护；② 尽早拒绝，避免无谓的 `getFileSystemForRead`/FS 初始化开销；③ B 方案需改 Scala Service 层，影响面更大且 FileSource 为通用能力，不宜在底层耦合业务阈值。Controller 层是"参数合法性边界"，天然适合放容量保护。

### ADR-002 超阈行为：抛异常 vs 截断到首页

- **Option A**：超阈抛 80038，前端提示用户减小页码/页大小。
- **Option B**：超阈自动截断为 page=1（静默返回首页）。
- **Selected**：**A**。
- **理由**：① 与既有 80034（pageSize 超限抛异常）语义一致，用户行为可预期；② B 方案静默改写用户请求页码，会导致"明明翻到第 1000 页却看到第 1 页"的迷惑行为，且前端分页状态与后端不一致；③ 抛异常让用户明确知道"翻太深了"，引导其改用下载查看（与 80032/80034 文案一致的产品引导）。

### ADR-003 开关粒度：全局开关 vs 租户级开关

- **Option A**：全局开关 `linkis.workspace.openfile.limit.enable`。
- **Option B**：按用户/租户级开关。
- **Selected**：**A**。
- **理由**：① OOM 是容量风险，对全体租户一致适用，无差异化需求；② 现有同类保护（`FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED`、`FILESYSTEM_RESULTSET_ROW_LIMIT`）均为全局开关，保持一致；③ 阈值 `limit.threshold` 可调，运维按集群容量统一调整即可；租户级差异化非本需求目标，避免过度设计。

### ADR-004 阈值默认值：100000

- **依据**：既有 `FILESYSTEM_RESULTSET_ROW_LIMIT=10000`（单页 pageSize 上限）。翻页累计行数阈值取其 10 倍（100000）作为默认，平衡"允许合理深翻页"与"防 OOM"。即 page=20、pageSize=5000（accRows=95000）可通过；page=21 起（accRows=100000，等于阈值不超）仍可通过；page=22（accRows=105000）拦截。运维可按 JVM 堆大小调参。

---

## 5. 修改文件清单

| 文件 | 类型 | 改动 |
| --- | --- | --- |
| `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/conf/WorkSpaceConfiguration.java` | 修改 | 新增 2 个 `CommonVars` 开关（行 75 附近） |
| `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/restful/api/FsRestfulApi.java` | 修改 | `openFile` 方法内行 649 后插入拦截段（约 20 行） |
| `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/exception/WorkspaceExceptionManager.java` | 修改 | `initialDescMap()` 末尾新增 80038 条目 |
| `docs/errorcode/` 对应错误码文档 | 修改 | 同步登记 80038 |
| `linkis-dist/package/conf/linkis-ps-publicservice.properties` | 修改 | 新增 2 行注释（开关 + 阈值，默认关闭/注释） |
| `linkis-public-enhancements/linkis-pes-publicservice/src/test/.../FsRestfulApiTest.java`（或新增测试类） | 新增/修改 | 单测：开关 on/off、阈值边界、降级路径 |

**不动**：`FileSource.page`（Scala 底层）、`FsService`、protocol 类、前端代码。

---

## 6. 代码锚点与伪代码

### 6.1 WorkSpaceConfiguration.java（开关声明）

```java
// === REQ-02: openFile 翻页 OOM 拦截 ===
// openFile 翻页 OOM 拦截开关，默认关闭（保守上线，灰度后再开）
public static final CommonVars<Boolean> FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED =
    CommonVars$.MODULE$.apply("linkis.workspace.openfile.limit.enable", false);

// 翻页累计行数阈值：(page-1)*pageSize 超过此值则拦截，默认 100000
public static final CommonVars<Integer> FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD =
    CommonVars$.MODULE$.apply("linkis.workspace.openfile.limit.threshold", 100000);
```

### 6.2 WorkspaceExceptionManager.java（80038）

```java
map.put(
    "80038",
    "The page view exceeds the accumulated row limit {0}, page viewing is not supported. "
        + "Please reduce the page size or page number "
        + "(翻页累计行数超过{0}，暂不支持页面查看，请减小页码或页大小)");
```

### 6.3 FsRestfulApi.openFile（拦截段，插入位置：行 649 之后）

```java
// [REQ-02] 翻页 OOM 拦截：(page-1)*pageSize 超阈值抛 80038，异常降级放行
if (FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED.getValue()) {
  try {
    long accRows = (long) (page - 1) * pageSize;
    int threshold = FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD.getValue();
    LOGGER.info(
        "openFile page limit check（翻页累计行数校验）: path={}, page={}, pageSize={}, "
            + "accRows={}, threshold={}",
        path, page, pageSize, accRows, threshold);
    if (accRows > threshold) {
      throw WorkspaceExceptionManager.createException(80038, threshold);
    }
  } catch (WorkSpaceException e) {
    throw e; // 业务异常向上抛
  } catch (Throwable t) {
    LOGGER.warn(
        "openFile page limit check failed, fallback to original flow（翻页校验失败，降级放行）, path: {}",
        path, t); // 技术异常降级放行
  }
}
```

**规范自检**：
- 使用项目 logger（`FsRestfulApi` 已有 `private static final Logger LOGGER`），中英双语日志，符合 §5.3。
- `try/catch` 而非 `e.printStackTrace()`，符合 §4.2 Java 等价写法。
- 未用 Java 9+ 语法，符合 §3.1.1（JDK 1.8）。

---

## 7. 配置项说明

新增 2 个配置项，需在 `linkis-dist/package/conf/linkis-ps-publicservice.properties` 增加注释行（即使关闭也写出，便于运维感知，遵循 §4.3）：

```properties
## REQ-02: openFile 翻页 OOM 拦截（默认关闭，灰度后开启）
#linkis.workspace.openfile.limit.enable=false
#linkis.workspace.openfile.limit.threshold=100000
```

| key | 默认值 | 说明 |
| --- | --- | --- |
| `linkis.workspace.openfile.limit.enable` | `false` | 翻页 OOM 拦截总开关 |
| `linkis.workspace.openfile.limit.threshold` | `100000` | 累计行数阈值，`(page-1)*pageSize` 超此值拦截 |

---

## 8. 回滚方案

- **一键回滚**：`linkis.workspace.openfile.limit.enable=false`（重启生效），拦截段被跳过，完全回退原 openFile 行为。
- **运行时降级**：拦截逻辑任何未预期异常均 `catch + warn` 放行原流程，无需人工介入即可自愈。
- **代码回退**：仅 3 处增量（2 开关 + 1 拦截段 + 1 错误码），revert commit 即可，无 schema/protocol 变更，无兼容风险。

---

## 9. 测试设计

### 9.1 单元测试（`FsRestfulApiTest` 或新增 `OpenFileLimitTest`）

| 用例 | 开关 | 输入 | 预期 |
| --- | --- | --- | --- |
| 开关关-走老路径 | false | page=1000, pageSize=5000 | 不抛 80038，走原 openFile（由下游决定结果） |
| 开关开-未超阈 | true | page=2, pageSize=5000（accRows=5000 < 100000） | 通过，不抛 80038 |
| 开关开-边界相等 | true | page=21, pageSize=5000（accRows=100000 == 阈值，未超） | 通过，不抛 80038 |
| 开关开-超阈 | true | page=22, pageSize=5000（accRows=105000 > 100000） | 抛 80038 |
| 降级-阈值计算异常 | true | mock threshold 读取抛异常 | warn 日志 + 放行（不抛 80038） |
| 与 80034 共存 | true | pageSize=20000（>10000）+ page=2 | 先命中 80034（pageSize 上限），不进入新拦截 |

### 9.2 集成测试

- **大文件触发拦截**：构造 > 阈值的结果集，openFile 请求 page 使 accRows 超阈 → 收到 80038，服务无 OOM。
- **小文件正常放行**：常规结果集翻前几页 → 正常返回内容。
- **开关 on/off 对照**：同一深翻页请求，off 时走下游（可能 OOM 或慢），on 时返回 80038。

### 9.3 回归

- 非 resultset 文件（普通文本/日志）走 80032 分支，不受本拦截影响。
- `resultset` 类型文件既有 80034/80036/collectBytes 限制保持不变。

---

## 10. §10.2 主流程高危对照

| §10.2 环节 | 是否触及 | 说明 |
| --- | --- | --- |
| 1~7（Entrance/拦截器链/Parser/Orchestrator/AM/ECM/EC） | 否 | openFile 不在任务执行链路 |
| 8（LM 刷新物料）/ 9（ECM 拉物料）/ 10（脚本组装） | 否 | 与引擎物料/启动无关 |
| **本需求落点** | 公共服务 workspace 文件预览 | `linkis-pes-publicservice` 的 `FsRestfulApi.openFile`，属公共服务，非主流程 |

**结论**：本需求不触及 §10.2 任一高危环节，但已遵循 §4 铁律（开关默认 false + try/catch 兜底 + 异常降级回退），满足新功能开发要求。

---

## 附录 A：需求风险摘要（来自 Stage 1.1 触点①）

| 风险 ID | 类型 | 等级 | 描述 |
| --- | --- | --- | --- |
| R-001 | 模块链路 (module-link) | 高 | openFile 链路改造影响结果集预览可用性 |
| R-002 | 性能容量 (performance-capacity) | 高 | 大结果集翻页 OOM |
| R-003 | 参数配置 (configuration) | 高 | limit 阈值/开关配置不当 |

**本设计对风险的消解**：
- R-001：try/catch 降级放行 + 开关默认关闭，保证 openFile 可用性；
- R-002：`(page-1)*pageSize` 阈值拦截，从源头阻断深翻页 OOM；
- R-003：阈值可配 + 默认值合理（100000）+ properties 注释行。

---

## 附录 B：参考文件锚点

- 需求文档：`docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md`
- 错误码现状：`linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/exception/WorkspaceExceptionManager.java:90-91`（80037 占用）
- openFile 方法：`linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/restful/api/FsRestfulApi.java:624`（方法签名）、`:643`（既有 80034 校验）、`:704`（page 调用 OOM 点）、`:721`（collect）
- 开关声明参照：`.../filesystem/conf/WorkSpaceConfiguration.java:74`（`FILESYSTEM_RESULTSET_SIZE_CHECK_ENABLED`）
- 项目铁律：`CLAUDE.md` §4（开关+tryCatch+降级）、§9.5（地雷区）、§10（主流程）
