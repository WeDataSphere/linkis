# 领域专属规则

> 本文件放 **CLAUDE.md 没细讲** 的领域规则：数据库变更、配置管理、代码边界。
> 通用规则（开关 + tryCatch 降级、License 头、scalastyle、Git 提交）在 [`../CLAUDE.md`](../CLAUDE.md)。
>
> 适用版本：Apache Linkis `dev-2.0.0`。

---

## 1. 数据库修改原则

### 1.1 决策顺序（优先复用，谨慎改结构）

```
能不改 → 不改
必须改 → 优先新增字段（向后兼容）
       → 次之新增表（辅助表：日志/临时/配置可自行建；核心业务表需架构评审）
       → 最末修改字段类型 / 删字段（必须评审 + 兼容方案）
```

### 1.2 DDL / DML 脚本位置

> **2.0.0 已按模块拆分**，不再是单一大文件。

| 类型 | 路径 |
| --- | --- |
| 基线 DDL | `linkis-dist/package/db/linkis_<module>.sql`（如 `linkis_entrance.sql`、`linkis_publicservice.sql`） |
| 版本升级脚本 | `linkis-dist/package/db/upgrade/<version>/`（如 `upgrade/dev-2.0.0/`） |

### 1.3 变更记录要求

每次 DDL/DML 变更必须：
- 在对应模块 SQL 文件或 upgrade 脚本中加一段；
- 文件头注明 **版本号 + 需求描述 + 日期**；
- 优先 `ADD COLUMN`，避免 `MODIFY` / `DROP`；
- 同步更新对应模块文档（[`.ai/modules/`](./modules/) 下）的表结构章节。

### 1.4 表命名约定

`linkis_[模块]_[功能]_[表名]`，如 `linkis_ps_job_history_group_history`、`linkis_cg_manager_label_value_view`。

---

## 2. 配置管理规则

### 2.1 统一走 `CommonVars`

```scala
import org.apache.linkis.common.conf.CommonVars

object FooConfiguration {
  val FOO_ENABLE = CommonVars("linkis.foo.enable", false)
  val FOO_TIMEOUT_MS = CommonVars("linkis.foo.timeout.ms", 30000L)
}
```

**禁止**：
- 散落硬编码常量；
- 直接读 `System.getProperty` / `java.util.Properties`；
- 自己 `split(",")` 配置 key（用 `CommonVars` 已有的 `getValue` / `getHotValue`）。

### 2.2 命名约定

`linkis.[模块].[功能].[属性]`，如 `linkis.entrance.task.max.retry.times`、`linkis.engine.conn.jdbc.query.timeout`。

新功能开关默认 `false`，命名后缀：`*.switch` / `*.enable` / `*.enabled`（详见 CLAUDE.md §4.1.1）。

### 2.3 热加载边界（getValue ≠ getHotValue）

| 方法 | 行为 | 适用 |
| --- | --- | --- |
| `key.getValue` | JVM 启动时快照，重启才更新 | 90% 场景 |
| `key.getHotValue` | 后台线程定时刷新，运维改了立刻生效 | 期望不重启关掉的功能 |

详见 CLAUDE.md §9.3。

### 2.4 配置类位置

放当前模块的 `conf/` 目录，命名 `XxxConfiguration.scala`（Scala）或 `XxxConfiguration.java`。
参考实现：`linkis-public-enhancements/linkis-jobhistory/src/main/scala/.../conf/JobhistoryConfiguration.scala`。

---

## 3. 代码边界约束

### 3.1 🚫 禁止操作

- **数据库结构**：未经评审严禁 `ALTER` / `DROP` 现有表或字段；
- **第三方依赖**：不允许新增 Maven 依赖（升级需 PR 显式说明，见 CLAUDE.md §3.1）；
- **公开接口签名**：不得修改 protocol case class、公开 Service / Restful 方法签名；
- **核心公共类**：`Utils`、`Message`、`LinkisException`、`CommonVars` 等不动；
- **Bean 名称**：`ServiceNameConsts.*` 命名约定不动（见 CLAUDE.md §9.5 地雷区）。

### 3.2 ✅ 允许操作

- 新增功能模块 / 子包；
- 新增 Restful 接口（命名 `XxxRestfulApi`，路径 `/api/rest_j/v1/<module>/...`）；
- 新增 Mapper / Service；
- 在现有表上 **新增字段**（向后兼容）；
- 在现有配置类中 **新增 CommonVars 项**。

---

## 4. 需求实现工作流（按规模选用）

> 不是所有改动都走完整流程。按规模分级：

### 4.1 小改动（修 bug、加配置项、单接口扩展）
1. 在 `dev-2.0.0` 上直接开 `dev-2.0.0-fix-<bug>` 分支；
2. 加开关 + `Utils.tryCatch` 兜底（CLAUDE.md §4）；
3. 自测 on/off 两种状态；
4. commit 加 `#AI commit#` 前缀，PR 到 `dev-2.0.0`。

### 4.2 中等改动（新模块、新接口群、表结构变更）
1. 走 4.1 全部步骤；
2. 在 PR 描述里列出：影响面、回归用例、配置项说明；
3. 涉及 DDL 时同步更新 `.ai/modules/<对应模块>.md`。

### 4.3 大型需求（跨服务、跨模块、影响主线流程）
1. 写需求文档 + 设计文档（路径自定义，建议 `docs/design/<feature>/`）；
2. **必须**走 CLAUDE.md §10.3-10.4 主流程 PR 自检清单；
3. 涉及 §10.2 高危区域时，开关 on/off 双状态冒烟 + 并发验证。
