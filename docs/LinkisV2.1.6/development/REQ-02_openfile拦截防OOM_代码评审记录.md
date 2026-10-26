# 代码评审记录 · REQ-02 openfile 拦截防 OOM

> 版本：LinkisV2.1.6 ｜ 子系统：WDSL-UJES-2.1.6 ｜ 评审日期：2026-10-22
> 评审方式：静态走查（8 维度）｜ 评审人：v_kkhuang
> 变更基线：c5d8db3ae~1 → c5d8db3ae（含 23b5d7a68）

## 一、变更范围

| # | 文件 | 模块 | 变更摘要 |
|---|---|---|---|
| 1 | `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/conf/WorkSpaceConfiguration.java` | pes-publicservice | 新增 2 开关：FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED（默认 false）、FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD（默认 100000） |
| 2 | `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/exception/WorkspaceExceptionManager.java` | pes-publicservice | 新增错误码 80038（翻页累计行数超限） |
| 3 | `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/restful/api/FsRestfulApi.java` | pes-publicservice | openFile 接口新增翻页 OOM 监控段：累计行数 (page-1)*pageSize 超阈值告警降级，不阻断主链路 |
| 4 | `linkis-dist/package/conf/linkis-ps-publicservice.properties` | dist/conf | 部署配置补充新开关注释行 |

## 二、8 维度静态走查

### 1. 注入（SQL/命令/LDAP）
- **结论：无风险**。本次变更为翻页行数计算 `(page-1)*pageSize`，不涉及 SQL/命令/LDAP。page/pageSize 是数值参数，无字符串拼接注入面。✅

### 2. 敏感信息
- **结论：无风险**。日志打印 path/page/pageSize/accRows/threshold，均为业务参数，无凭据/密钥。符合 §8.4。✅

### 3. 权限 ⭐
- **结论：合格**。
  - FsRestfulApi 沿用 `@RestController` + `@RequestMapping("/filesystem")`；
  - openFile 方法所在的 FsRestfulApi 类各接口均通过 `ModuleUserUtils.getOperationUser(req, ...)` 鉴权（§9.4.4），本次新增的监控段位于鉴权之后、openFile 主逻辑之前，不绕过既有鉴权；
  - 未新增免鉴权路径，无需修改 `linkis.properties` 的 `wds.linkis.server.user.restful.uri.pass.auth`。✅

### 4. 异常处理 ⭐
- **结论：合格，符合铁律 §4.2**。
  - 翻页监控段整体包裹 `try { ... } catch (Throwable t)`，catch 块降级放行原 openFile 流程，warn 日志，不抛出；
  - 用 `Throwable` 而非 `Exception` 捕获，覆盖 `OutOfMemoryError` 等虚拟机级异常（OOM 拦截段自身 OOM 时也不阻断主链路），设计合理；
  - 日志用项目 `LOGGER.warn`，未用 `e.printStackTrace()`。✅

### 5. 输入校验
- **结论：合格**。累计行数计算 `(long)(page - 1) * pageSize` 显式强转 long 防溢出；page/pageSize 在进入本段前已有 80036/80037 校验（page size [1-500]）；阈值用 `FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD.getValue()` 读取。✅

### 6. 配置 ⭐
- **结论：合格，符合铁律 §4.1**。
  - 2 新开关均用 `CommonVars` 声明；
  - `FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED` 默认 false（保守关闭，符合 §4.1.1）；
  - `FILESYSTEM_OPENFILE_PAGE_LIMIT_THRESHOLD` 默认 100000，注释说明灰度后再开；
  - 部署配置 `linkis-ps-publicservice.properties` 已补注释行（便于运维感知）；
  - **当前实现策略**：开关仅供后续硬阻断灰度预留，**当前一律降级放行**（开关开/关均只 warn 不阻断），符合 §4"异常时降级不影响主链路"。✅

### 7. 反序列化
- **结论：不涉及**。本次变更无序列化/反序列化逻辑。✅

### 8. 路径穿越
- **结论：合格**。本次新增监控段不处理 path 的文件读写，path 的越权访问由既有 `WorkspaceUtil` / `fsService` 校验。新增逻辑只在 page/pageSize 维度做行数判断，不引入新路径穿越面。✅

## 三、重点风险点核查

### 3.1 降级策略正确性
commit 记录显示原方案"超阈值抛 80038 退出"已修正为"超阈值不抛 80038，降级放行原逻辑；开关未开时超 10w 行打 warn 日志"。
- **核查**：代码中 `if (FILESYSTEM_OPENFILE_PAGE_LIMIT_ENABLED.getValue())` 分支只打 warn（注释"limit enabled but degrade to original flow"），else 分支也只打 warn。两个分支均不抛 80038，不阻断。符合"当前一律降级放行"设计。80038 错误码保留供后续硬阻断灰度。✅

### 3.2 累计行数计算溢出
`(page - 1) * pageSize` 用 `(long)(page - 1) * pageSize`，先转 long 再乘，避免 int 溢出。
- **核查**：page/pageSize 为 int，`(long)(page-1)` 将左操作数提升为 long，乘法在 long 域进行，100000*500=5e7 不溢出。即使 page 极大（Integer.MAX_VALUE），long 域也能容纳。✅

### 3.3 OOM 拦截段自身 OOM
catch 用 `Throwable` 而非 `Exception`。
- **核查**：翻页监控段只做一次乘法+比较+日志，内存占用极小，自身 OOM 概率极低；但用 Throwable 捕获体现了防御性编程，即使段内 OOM 也不影响 openFile 主链路。✅

## 四、单测覆盖

| 文件 | 单测 | 状态 |
|---|---|---|
| WorkSpaceConfiguration | WorkSpaceConfigurationTest | ✅ 存在（验证开关默认值） |
| FsRestfulApi | FsRestfulApiTest | ✅ 存在（验证 openFile 链路） |
| WorkspaceExceptionManager | 无独立单测 | 错误码注册，建议补 80038 单测（low） |

> 注：既有单测覆盖配置默认值与 openFile 链路，翻页监控段建议补"超阈值降级放行"用例。属 low 级建议，不阻断发布。

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
- **依据**: 变更符合 CLAUDE.md 铁律（开关默认 false + tryCatch(Throwable) 降级 + CommonVars 配置 + ModuleUserUtils 鉴权 + 部署配置注释行），降级策略正确（一律放行不阻断），long 域防溢出，OOM 拦截段自身 OOM 也降级。
