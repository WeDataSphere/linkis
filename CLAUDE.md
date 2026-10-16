# Linkis 项目 AI 协作指引（CLAUDE.md）

> 本文件用于指导 AI（如 Claude Code）在协助开发 Linkis 时遵循项目既有规范。
> 当前版本：`dev-2.0.0`（pom.xml `<revision>2.0.0</revision>`）。
> 今日日期：2026/07/07。
>
> **代码片段库**：可复用的代码模板见 [`.ai/snippets.md`](.ai/snippets.md)（12 类高频片段）。
> **深度参考库**：踩坑反例 / 领域规则 / 模块文档见 [`.ai/`](.ai/README.md) 目录（`common-pitfalls.md`、`rules.md`、`project-context.md`、`modules/`）。

---

## 1. 项目定位

Apache Linkis 是一款计算治理引擎，负责连接上层应用与底层各种计算/存储引擎（Spark、Hive、Python、JDBC、Flink、Presto、Trino 等）。本项目**已在生产环境稳定运行**，所有新功能必须以保证生产稳定为第一优先级。

---

## 2. 模块总览

| 顶层模块 | 职责（简） |
| --- | --- |
| `linkis-commons` | 基础库：`CommonVars`/`Configuration`、RPC、协议、存储、调度 |
| `linkis-spring-cloud-services` | Eureka 服务发现 + `linkis-mg-gateway` 网关 |
| `linkis-orchestrator` | 任务编排器（computation/code/core） |
| `linkis-public-enhancements` | 公共服务：configuration / bml / datasource / jobhistory / cs / udf / instance-label |
| `linkis-computation-governance` | 计算治理：`linkis-entrance`、`linkis-manager`（AM/LM/RM）、`linkis-engineconn`、`linkis-engineconn-manager`、`linkis-client`、`linkis-jdbc-driver` |
| `linkis-engineconn-plugins` | 各引擎插件（hive/spark/python/jdbc/flink/presto/trino/doris/nebula/openlookeng/elasticsearch/hbase/impala/pipeline/repl/seatunnel/shell/sqoop/io_file） |
| `linkis-dist` | 打包、部署、默认配置（`linkis-dist/package/conf/`） |
| `linkis-web` / `linkis-web-next` | 前端（详见 §3.2） |

> 各模块的具体类与扩展点详见 [第 9 节](#9-领域知识与典型任务作弊条)。

---

## 3. 构建与运行

```bash
# 完整构建（默认 spark-2.4.3 + hadoop-2.7.2 + scala-2.11）
./mvnw -DskipTests clean install

# 仅编译某个模块（举例：entrance）
./mvnw -pl linkis-computation-governance/linkis-entrance -am -DskipTests clean install

# 切换到 spark-3 / hadoop-3.3 / hbase-2.5 profile
./mvnw -Pspark-3 -DskipTests clean install
./mvnw -Phadoop-3.3 -DskipTests clean install
./mvnw -Phbase-2.5 -DskipTests clean install      # 通过 -Dhbase.profile=2.5 触发

# 运行单元测试
./mvnw -pl <module> test
```

部署后端配置文件目录：`linkis-dist/package/conf/`，关键文件：
- `linkis.properties`：全局配置（数据源、HDFS/Hive 路径、管理员账号等）
- `linkis-cg-entrance.properties`、`linkis-cg-linkismanager.properties`、`linkis-cg-engineconnmanager.properties`
- `linkis-ps-publicservice.properties`、`linkis-mg-gateway.properties`

---

## 3.1 关键依赖版本与编译约束（写代码前必看）

> 完整版本见 `pom.xml` 顶部 `<properties>`。**不要随意升级依赖**——升级任何一项必须在 PR 中显式说明并验证回归。

### 3.1.1 硬约束（必须记住）

| 项 | 版本 | 约束 |
| --- | --- | --- |
| JDK | **1.8** | **禁用 Java 9+ 语法/API**（`var`、record、`List.of`、`InputStream.readAllBytes` 等）；JDK 11+ 走 `jdk11-on` profile 强制 `release=8` |
| Scala | **2.11.12**（binary `2.11`）；`spark-3` profile 切到 `2.12.18` | 跨版本编译的代码避免用 2.12+ 专属语法（如 trait 参数） |
| Spring Boot | **2.7.12** | 不要使用 Boot 3 的 `jakarta.*` 包名 |
| Spring Cloud | **2021.0.8**（Netflix 3.1.7） | Eureka/Gateway 命名空间统一 `org.springframework.cloud.*` |
| Log4j2 | **2.17.2** | CVE 修复版，**勿降级** |
| XStream | 1.4.21 | 已是修复版，**勿降级** |
| MySQL Connector | **8.0.28** | **勿降级**（兼容性） |
| Guava | 33.2.1-jre | — |

### 3.1.2 大数据生态（受 Maven Profile 影响，写引擎相关代码务必先确认 profile）

| 依赖 | 默认 profile | 可切换版本 |
| --- | --- | --- |
| Hadoop | `hadoop-2.7` → `2.7.2` | `hadoop-3.3` → `3.3.1`（同时切 curator 4.2.0、artifact 改 `hadoop-hdfs-client`） |
| Spark | 默认 `2.4.3` | `spark-3` profile → `3.4.4`，并切换 scala 2.12 / jackson 2.14.2 / revision `2.0.0-spark3` |
| HBase | 默认 `1.2.1` | `hbase-1.4`/`2.2`/`2.5` → `1.4.3`/`2.2.6`/`2.5.3`（通过 `-Dhbase.profile=2.5` 触发） |
| Hive | 见各 engineconn-plugin 子模块 | — |
| ZooKeeper / Kafka client | 3.8.4 / 3.9.1 | — |

### 3.1.3 其余关键版本速查（详情看 pom.xml）

Web/RPC：Netty `4.2.7`、Jackson `2.15.0`（spark-3 切 2.14.2）、Jersey `1.19.4`/servlet `2.23.1`、Jetty `9.4.57`、Gson `2.8.9`、Protobuf `3.25.5`、SnakeYAML `2.0`。

日志/工具：SLF4J `1.7.30`、Commons Lang3 `3.18.0`/IO `2.11.0`/Text `1.10.0`、MyBatis-Plus `3.5.7`、Druid `1.2.4`、Knife4j `2.0.9`（仅 `wds.linkis.test.mode=true` 启用）。

测试：JUnit `5.7.2`、Mockito `3.9.0`、AssertJ `3.17.2`、H2 `2.2.220`。

---

## 3.2 前端栈说明

| 项目 | 状态 | 技术栈 |
| --- | --- | --- |
| `linkis-web/` | **当前使用** | Vue `2.6.12` + iview `3.5.4` + vue-router `3.4.8` + vue-i18n `8.22.1` + `@vue/cli-service 5.0.8`；ESLint `7.21.0` + `eslint-plugin-vue 9.6.0`；husky `1.3.1` + lint-staged（提交前自动 `vue-cli-service lint --no-fix`）；编辑器 monaco-editor `0.30.1`；SQL/语法 dt-sql-parser `3.0.5`、sql-formatter `2.3.3`；终端 xterm `5.3.0` |
| `linkis-web-next/` | **未落地**（仅含 `features/hive_location_control.feature` 一个 Cucumber 特征文件，无 `package.json`） | 暂不需要维护；后续若启用需重新评估栈 |

前端常用命令：

```bash
cd linkis-web
npm install               # 或 wnpm install（项目预置）
npm run serve             # 开发
npm run build             # 打包
npm run lint              # 不修复的检查（CI/pre-commit 使用）
npm run fix               # eslint --fix
```

**前端开发注意**：
- 提交时 husky 会自动跑 `lint-staged`，**不允许有 lint 错误**；
- 严禁升级 Vue 大版本（2→3）或 iview 大版本；
- axios 已是 `1.12.2`（注意 `axios.create` + interceptors 的写法，与旧 0.x 不同）。

### 3.2.1 ★ 前端多 App 架构（评估前端需求必读）

> 详细文档见 [`.ai/frontend/architecture.md`](.ai/frontend/architecture.md)。这里只给 AI 接到前端需求时**第一时间要建立的判断**。

**4 个子应用，对应不同后端**：

| App | 路径 | 用途 | 后端 |
| --- | --- | --- | --- |
| `linkis/` | `src/apps/linkis/` | 主管理后台（30 个 module） | 多后端混用 |
| `URM/` | `src/apps/URM/` | UDF 管理 + 函数管理（2 个 module） | linkis-udf-service |
| `scriptis/` | `src/apps/scriptis/` | 脚本编辑（本分支仅 webSocket/） | entrance |
| `PythonModule/` | `src/apps/PythonModule/` | Python 引擎前端（**独立 React 子项目**，非 Vue） | engineconn-python |

**最容易踩坑**：
- `apps/linkis/module/udfManager/`（基于 basedata-manager，**管理员视角**）≠ `apps/URM/module/udfManagement/`（基于 udf-service，**终端用户视角**）；
- URM 一个后端支撑两个前端页面（udfManagement + functionManagement），靠 `udfType` 字段区分（0/1/2=UDF；3/4=函数）。**给 URM 加功能时两个 module 几乎都要同步改**。

**URL 前缀 → 后端模块映射**（写前端 service.js 时对照）：

| 前端 URL | 后端模块 |
| --- | --- |
| `/basedata-manager/*` | linkis-basedata-manager |
| `/udf/*` | linkis-udf-service |
| `/configuration/*` | linkis-configuration |
| `/jobhistory/*` | linkis-jobhistory |
| `/linkisManager/*` | linkis-application-manager |
| `/datasource/*` | linkis-datasource |
| `/filesystem/*` | linkis-pes-publicservice |

**module 标准目录结构**：

```
apps/<app>/module/<feature>/
├── index.js           模块注册（暴露 name/component/dispatchs）
├── index.vue          主页面（搜索条件 + 表格 + 操作按钮）
├── vlist.vue          列表组件（iview Table + 列定义）
├── addXxxModal.vue    新增/编辑弹窗（可选）
└── service.js         API 调用（推荐抽出）
```

完整模板（可直接复制）：[`.ai/frontend/module-template.md`](.ai/frontend/module-template.md)。
30 个 linkis module 清单：[`.ai/frontend/apps-linkis-modules.md`](.ai/frontend/apps-linkis-modules.md)。
URM 两个 module 详解：[`.ai/frontend/apps-urm-modules.md`](.ai/frontend/apps-urm-modules.md)。

**API 调用约定**（统一走 `@/common/service/api`，baseURL 默认 `/api/rest_j/v1/`）：

```js
import api from '@/common/service/api';
api.fetch(url, data, method);   // method: 'get' | 'post' | 'put' | 'delete'
```

**i18n**：所有展示文字必须用 `$t('message.<app>.<key>')`，新增 key 要**同步**改 `src/common/i18n/zh.json` + `en.json` 与 `src/apps/<app>/i18n/common/{zh,en}.json`。

**路由**：新增页面必须在 `src/apps/<app>/router.js` 注册，否则前端访问不到。

---

## 4. ★ 新功能开发铁律（Feature Switch 与降级回滚）

> **背景**：项目已稳定运行。新功能属于"锦上添花"，**允许在生产出现问题时被一键关闭或回退到旧行为**。
> 因此任何新功能必须满足：① 有开关；② 默认安全；③ 异常时降级不影响主链路。
>
> **★ 重要**：如果新需求会改到 [§10 任务执行主线流程](#10-任务执行主线流程与高危区域) 中列出的 10 个高危环节（含 LM 刷新物料到 HDFS、ECM 拉物料到本地、引擎启动脚本组装），铁律要求更严，参见 [§10.3](#103-修改主流程代码的硬性要求)。

### 4.1 必须有功能开关

**所有新功能**必须在 `XxxConf.scala`（如 `EntranceConfiguration`、`GovernanceCommonConf`、各模块的 `conf/*Conf.scala`）中显式声明开关，禁止"裸写"新逻辑。

#### 4.1.1 开关命名规范（沿用既有约定）

| 后缀 | 含义 | 默认值 | 示例 |
| --- | --- | --- | --- |
| `*.switch` | 业务/策略开关 | `false`（保守关闭） | `linkis.entrance.user.creator.ip.interceptor.switch` |
| `*.enable` / `*.enabled` | 功能启用开关 | `false`（除非该功能早已是默认行为） | `linkis.entrance.failover.enable`、`linkis.aisql.starrocks.switch` |
| `*.safe.check.switch` | 安全检查类开关 | 视场景而定 | `linkis.python.safe.check.switch` |

**约定**：
- **新功能开关默认 `false`**（关闭），灰度验证后再开启；
- 历史上默认开启的能力（如 `linkis.enable.job.timeout.check=true`、`linkis.task.retry.enabled=true`）保持原状；
- key 全部小写、点号分隔，前缀沿用模块归属（`linkis.<module>.<feature>` 或保留 `wds.linkis.<feature>` 兼容历史）。

#### 4.1.2 声明开关的标准写法（含完整降级示例）

> 详细代码片段见 [`.ai/snippets.md` §1 配置开关声明](.ai/snippets.md#1-配置开关声明commonvars)。

```scala
// 1. 在模块的 conf/XxxConfiguration.scala 中声明开关
object EntranceConfiguration {
  // 推荐：Boolean 类型显式声明
  val FOO_BAR_SWITCH = CommonVars("linkis.foo.bar.switch", false)
}

// 2. 使用开关 + tryCatch 兜底
if (EntranceConfiguration.FOO_BAR_SWITCH.getValue) {
  Utils.tryCatch {
    // 新功能主路径
  } { t =>
    // 降级：任何异常都退回老路径
    logger.warn("foo bar feature failed, fallback: ", t)
  }
} else {
  // 老路径
}
```

注意：`CommonVars.getValue` 不接受运行时变更；如需"出问题立刻关掉、不重启"，必须用 `getHotValue`（如 `SPARK3_VERSION_COERCION_USERS.getHotValue`）。详见 [`.ai/snippets.md` §1 热加载边界](.ai/snippets.md#1-配置开关声明commonvars)。

### 4.2 必须使用 `Utils.tryCatch` 等做异常保护

**所有新功能主路径必须用 `Utils.tryCatch` / `Utils.tryAndWarn` / `Utils.tryAndErrorMsg` 包裹**，确保未预料的异常不会污染主链路、能让代码自动回退到旧行为。

| 方法 | 用途 | 失败时行为 |
| --- | --- | --- |
| `Utils.tryCatch(tryOp)(catchOp)` | 显式控制 catch 逻辑 | 由 `catchOp` 决定 |
| `Utils.tryAndWarn(tryOp)` | 记录 warn 日志后返回 null | 返回 `null` |
| `Utils.tryAndWarnMsg(tryOp)(msg)` | 同上，附带自定义消息 | 返回 `null` |
| `Utils.tryAndError(tryOp)` / `Utils.tryAndErrorMsg(tryOp)(msg)` | 记录 error 日志 | 返回 `null` |
| `Utils.tryQuietly(tryOp)` | 静默吞掉异常 | 返回 `null` |
| `Utils.tryThrow(tryOp)(f)` | 包装后重新抛出 | 抛出 |

**完整降级示例**：见 [`.ai/snippets.md` §2 异常降级](.ai/snippets.md#2-异常降级utilstrycatch-三种变体)，包含显式降级 / 仅记录日志 / 重新抛出三种变体。

**Java 代码的等价写法**：使用普通 `try/catch`，但 `catch` 块必须：① 用项目统一 logger 打印（不要 `e.printStackTrace()`）；② 走老路径或返回安全默认值；③ 不要把受检异常直接抛给上层调用方。

### 4.3 新功能开发 Checklist（提交前自检）

- [ ] 开关 key 已在对应 `*Conf.scala/*Conf.java` 中声明，命名符合 4.1.1；
- [ ] 默认值 `false`（或保留历史默认值），生产配置 `linkis-dist/package/conf/*.properties` 已加上注释行（即使关闭也写出来，便于运维感知）；
- [ ] 新功能主路径所有外部调用（RPC、HTTP、DB、文件、反射、第三方 API）均包裹 `Utils.tryCatch` 或等价保护；
- [ ] catch 分支能回退到老路径或返回安全默认值，绝不抛出未捕获异常导致任务失败；
- [ ] 日志使用项目 logger（`extends Logging` 或 `private val logger`），日志级别合理（warn/error）；
- [ ] 关键步骤打 `info` 日志，便于线上排查（参考 `AISQLTransformInterceptor` 的 `logger.info(s"aisql enable for ${jobRequest.getId}")`）。

> 8 个高频踩坑反例（字符编码 / 开关 / 表结构 / 异常 / 日志 / REST 返回 / SQL 注入 / 事务）见 [`.ai/common-pitfalls.md`](.ai/common-pitfalls.md)。

---

## 5. 代码规范（强制）

### 5.1 License 头

**所有新建 `.scala` / `.java` 文件必须带 ASF License 头**，否则 `scalastyle-config.xml` 的 `HeaderMatchesChecker` 会报错。

> License 模板参考：仓库任一现有 `.scala`/`.java` 文件的开头（标准 ASF Apache 2.0 头），或 `linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/Utils.scala`。

### 5.2 Scala 风格（由 `scalastyle-config.xml` 强制）

- 单行最大 **200** 字符（import 行除外）；
- **禁止 Tab**，统一用 2 空格缩进；
- `class`/`object` 命名 `[A-Z][A-Za-z]*`；`if`/`else`/`try`/`catch`/`=`/`=>`/`,`/`:` 等前后必须单空格；
- 禁止直接 `Runtime.getRuntime.addShutdownHook`，用 `ShutdownHookManager.addShutdownHook`；
- 禁止直接 `Class.forName`，用 `Utils.classForName`；
- 禁止 `println`（如必须用，包裹 `// scalastyle:off/on println`）；
- 文件末尾必须空一行。

### 5.3 日志规范

- 优先 `extends Logging`（trait 已定义 `protected lazy implicit val logger`）；
- 推前端的日志用 `LogUtils.generateInfo` / `generateWarn` / `generateError`（`linkis-common-log`），不要直接拼字符串；
- 业务日志用中英文双语说明错误码（项目惯例）：`s"error code（错误码）: ${error.getErrCode}, Error message（错误信息）: ${error.getDesc}."`；
- 详细用法见 [`.ai/snippets.md` §3 日志](.ai/snippets.md#3-日志logutils-推前端--loggerutils-mdc)。

### 5.4 配置类约定

- 用 `CommonVars[T](key, default, description?)` 声明可配置项，避免散落硬编码；
- `CommonVars` 在 `linkis-commons/linkis-common/.../common/conf/CommonVars.scala`；
- `Configuration`（同目录）放跨模块共享的全局配置（如 `JOBHISTORY_SPRING_APPLICATION_NAME`）。

### 5.5 Spring / MyBatis（Java 模块）

- 服务层 `@Service`/`@Component`，注入 `@Autowired`/`@Resource`；
- 写操作 `@Transactional(rollbackFor = Exception.class)`；
- MyBatis Mapper XML 放 `src/main/resources/mapper/common/`，namespace 与 Mapper 接口对齐；
- Restful 入口统一 `@RestController` + `RestfulApi` 命名后缀；
- 详细模板见 [`.ai/snippets.md` §9 Restful Controller](.ai/snippets.md#9-java-restful-controllermessage--鉴权)、[§10 Service + MyBatis](.ai/snippets.md#10-java-service--mybatistransactional--mapper)。

### 5.6 异常体系

- 业务异常用项目自带 `ErrorException` / `WarnException`（带 errCode、desc）；
- 模块专属异常命名 `XxxErrorException`（如 `StorageErrorException`、`EngineConnErrorException`）；
- 错误码定义参考 `docs/errorcode/<module>-errorcode.md`。

---

## 6. Git / 提交规范

### 6.1 分支与命名

- `master`：稳定 release + 偶尔 hotfix；`release-*`：稳定版本；`dev-*`：主开发分支（当前 `dev-2.0.0`）。
- 新功能分支：`dev-2.0.0-<feature>` 或 `dev-2.0.0-fix-<bug>`。

### 6.2 Commit Message（强制）

**本项目要求**：在 commit 描述中加上 **`#AI commit#`** 前缀（参考近期提交 `#AI commit# refactor:修复datasource oom异常`）。

社区规范（无 AI 标识时）格式：`[feat][EC][jdbc] add db2 database support with validation query mapping (#1064)`，即 `[<type>][<module>] <subject>`。

合并示例：

```
#AI commit# feat: 新增 xxx 功能，默认关闭，通过 linkis.xxx.switch=true 开启
```

### 6.3 PR

- 默认 PR 提交到 `dev-*` 分支；
- 新功能 PR 必须包含：① 开关 + 默认关闭；② 异常降级；③ 配置项说明（更新 `linkis-dist/package/conf/*.properties` 或 `docs/configuration/`）。

---

## 7. 常用查找位置

| 想找什么 | 去哪 |
| --- | --- |
| **★ 可复用代码片段** | [`.ai/snippets.md`](.ai/snippets.md)（12 类高频片段） |
| **★ 常见踩坑反例** | [`.ai/common-pitfalls.md`](.ai/common-pitfalls.md)（字符编码 / 开关 / 表结构 / 异常 / 日志 / REST 返回 / SQL 注入 / 事务） |
| **领域专属规则** | [`.ai/rules.md`](.ai/rules.md)（数据库变更 / 配置管理 / 代码边界） |
| **服务清单与模块文档** | [`.ai/project-context.md`](.ai/project-context.md) + [`.ai/modules/`](.ai/modules/) |
| 已有的功能开关 | `find . -name '*Conf.scala' -o -name '*Configuration.scala'` → grep `switch\|enable` |
| 看异常处理范例 | `linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/AISQLTransformInterceptor.scala` |
| 全局基础工具 | `linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/Utils.scala` |
| 部署默认配置 | `linkis-dist/package/conf/` |
| 错误码清单 / 配置文档 | `docs/errorcode/`、`docs/configuration/` |

---

## 8. 与 AI 协作的额外要求

1. **不要随意升级依赖版本**（`pom.xml` 中 `<jedis.version>` 等），改动需显式提出并征求确认。
2. **不要直接删/改公开 API 的签名**，旧调用方可能跨模块依赖。
3. **新建文件**：必须带 License 头（见 5.1），命名遵循 5.2。
4. **日志禁止打印敏感信息**：不要在日志中打印 token、密码、用户凭证、用户原始 SQL/代码全文。需要记录 SQL 时使用 `CodeUtils.maskCode(code, engineType)` 脱敏（参考 commit `996cdcdc7`）。
5. **优先复用现有工具，不要重复造轮子**。动手写新代码前先 grep，以下工具已存在，直接用：
   - 异常处理 → `Utils.tryCatch` / `tryAndWarn` / `tryQuietly`（§4.2）
   - 配置 → `CommonVars` / `Configuration`（§5.4）
   - 任务参数 → `TaskUtils.getStartupMap` / `getRuntimeMap` / `getSpecialMap`（§9.2）
   - 用户与 Label → `LabelUtil`、`Configuration.isAdmin`、`ModuleUserUtils.getOperationUser`
   - JobId → `JobUtils.getJobIdFromStringMap`
   - 反射 → `Utils.classForName`（**禁止 `Class.forName`**）
   - 调度线程池 → `Utils.defaultScheduler`（**禁止 `Executors.newXxx`**）
6. **新建 `Conf` / `Utils` / `Constants` 类前先查重**：项目已有数十个 `*Configuration` / `*Conf`，先 grep 确认没有同类工具，避免散落重复定义。

> 关于"修改业务代码前先加开关 + tryCatch 兜底"的详细规则，见 [第四节 ★ 铁律](#四-新功能开发铁律feature-switch-与降级回滚)。

---

## 9. 领域知识与典型任务作弊条

> 这一节是 AI 真正"能动手"前必须建立的心智模型。

### 9.1 核心概念词典（看到这些名词不要发懵）

| 名词 | 全称 / 含义 | 所在模块 |
| --- | --- | --- |
| **Entrance** | 任务入口服务，接收前端/客户端提交的 JobRequest，跑拦截器链 → 编排 → 执行 | `linkis-entrance` |
| **JobRequest** | 一次任务提交的实体（含 code、labels、params、executeUser、submitUser 等） | `linkis-governance-common` |
| **Orchestrator** | 编排器，把 JobRequest 拆成可执行计划，并发/重试/回滚都在这一层 | `linkis-orchestrator` |
| **EngineConn (EC)** | 引擎连接，与一个真实计算引擎进程（Spark/Hive/JDBC...）一一对应的常驻进程 | `linkis-engineconn-plugins/*` |
| **ECM** | EngineConn Manager，每台机器一个，负责拉起/管理本机 EC 进程 | `linkis-engineconn-manager` |
| **AM / LinkisManager** | Application Manager，全局调度引擎实例的创建、复用、销毁 + 资源管理（RM） | `linkis-manager/linkis-application-manager` |
| **RM** | Resource Manager（AM 子模块），对接 YARN/Kubernetes 资源 | `linkis-manager/linkis-resource-manager`（在 AM 内） |
| **LM** | Label Manager（AM 子模块），管理引擎/用户标签 | `linkis-manager/linkis-label-common` + AM |
| **Label** | 任务的"路由标签"——决定走哪个引擎、哪个用户、哪个租户，下文 9.2 详解 | `linkis-label-common` |
| **EngineConnPlugin** | 每种引擎一个插件实现（如 `JDBCEngineConnPlugin`），告诉框架如何创建/启动该引擎 | `linkis-engineconn-plugins/<engine>` |
| **公共服务（PES）** | Public Enhancement Service：configuration / bml / datasource / jobhistory / cs / udf / instance-label | `linkis-public-enhancements/*` |
| **BML** | Bigscreen Material Library，物料库（脚本、资源文件、UDF jar） | `linkis-bml-server` |
| **CS** | Context Service，跨任务上下文共享 | `linkis-cs-server` |

**任务大致流转链路**：前端 → Gateway → Entrance（拦截器链 → Parser → Orchestrator）→ AM（按 Label 选/建 EC）→ ECM（拉起进程）→ EC（执行代码并回写日志/结果）→ Entrance（持久化 jobhistory）→ 前端。

### 9.2 ★ Label 体系与 TaskUtils 三套 Map（最容易踩坑）

任务的"参数"在 `JobRequest.getParams` 这一个 `Map<String, AnyRef>` 里，但**不是平铺的**——而是嵌套了几个子 map。所有读写都要走 `TaskUtils`（`linkis-commons/linkis-protocol/.../utils/TaskUtils.scala`），**不要直接 `params.get("startup")`**。

| 子 Map | 用途 |
| --- | --- |
| **startup** | 启动参数（影响 EC 怎么拉起来：内存、命令行、引擎版本、`ec.resource.name`、AI SQL 开关…） |
| **runtime** | 运行时参数（影响 EC 内部行为：数据源、超时、并发数、`linkis.engine.runtime.datasource`） |
| **special** | 特殊参数（特殊场景的隐式配置，少用） |
| **variable** | 自定义变量（代码 `${var}` 占位符替换） |
| **labels** | Label 序列化的中间态，**禁止直接读写**，用 Label API |

详细读写代码、Label 体系、`LabelUtil`/`LabelBuilderFactory` 用法见 [`.ai/snippets.md` §4 任务参数](.ai/snippets.md#4-任务参数taskutils-三套-map--enginecreationcontext)、[§5 Label 读写](.ai/snippets.md#5-label-读写labelutil--labelbuilderfactory)。

### 9.3 CommonVars 热加载边界（ getValue ≠ getHotValue）

`CommonVars` 底层走 `BDPConfiguration`，有两个缓存层：

| 方法 | 行为 | 适用场景 |
| --- | --- | --- |
| `key.getValue` | **首次访问时读一次，之后用 JVM 启动时的快照**，重启才更新 | 90% 场景；高性能 |
| `key.getHotValue` / `key.acquireNew` | 每次都读"热加载缓存"（`configReload`，由后台线程定时刷新） | 运维需要不重启动态调参时 |
| `key.getValue(options)` / `key.getValue(options, hotload=true)` | 从任务级 map 读，可指定是否热加载 | 任务参数注入 |

**重要约束**：
- 默认 `getValue` **不感知运行时配置变化**——开关联动通常需要重启服务。
- 若新增功能开关期望"出问题立刻关掉，不重启"，**必须用 `getHotValue`**，并在 PR 中说明。
- 热加载间隔由 `BDPConfiguration.DEFAULT_CONFIG_HOT_LOAD_DELAY_MILLS` 控制（默认秒级）。

### 9.4 典型扩展点作弊条

> 详细代码模板（含完整可复制代码 + 项目内真实参考文件）见 [`.ai/snippets.md`](.ai/snippets.md)，本节只列"做什么 + 在哪里改"的关键步骤。

#### 9.4.1 加一个 Entrance 拦截器

1. 新建 `linkis-computation-governance/linkis-entrance/src/main/scala/.../interceptor/impl/XxxInterceptor.scala`，`extends EntranceInterceptor with Logging`；
2. 实现 `override def apply(jobRequest: JobRequest, logAppender: StringBuilder): JobRequest`；
3. 新逻辑先读开关 + 用 `Utils.tryCatch` 兜底；
4. **注册到拦截器链**：编辑 `linkis-entrance/src/main/java/org/apache/linkis/entrance/conf/EntranceSpringConfiguration.java` 的 `entranceInterceptors()` Bean 数组（**顺序敏感**）；
5. 自测覆盖：开关关闭走老路径 / 开关打开走新路径 / 抛异常时降级。
6. 模板代码：[`.ai/snippets.md` §7 EntranceInterceptor 完整模板](.ai/snippets.md#7-entranceinterceptor-完整模板)。

#### 9.4.2 加一个 EngineConn 引擎插件

1. 在 `linkis-engineconn-plugins/<engine>/src/main/scala/.../<engine>/` 下实现 4 个核心类（Plugin / Factory / Executor / LaunchBuilder）；
2. 在 `src/main/resources/linkis-engineconn.properties` 中注册 `wds.linkis.engineconn.plugin.default.class`；
3. 引擎配置走 `conf/<Engine>Configuration.scala`；
4. 错误码定义在 `errorcode/<Engine>ErrorCodeSummary.java`，同步更新 `docs/errorcode/<engine>-errorcode.md`；
5. 给 plugin 加单测 + 集成测试；
6. 模板代码：[`.ai/snippets.md` §8 EngineConnPlugin 完整模板](.ai/snippets.md#8-engineconnplugin-完整模板)。

#### 9.4.3 加一个 RPC 接口（跨服务调用）

1. **协议类（case class）必须定义在 `linkis-commons/linkis-protocol/`**——跨模块共享，否则编译不过；
2. 协议命名 `XxxRequest` / `XxxResponse`，加 `@BeanProperty` 注解便于 Java 互操作；
3. 服务端实现 `extends Receiver`，并配套 `ReceiverChooser`（必须成对）；
4. 客户端用 `Sender.getSender(serviceName).ask(request)` 调用；服务名常量放 `linkis-commons/.../conf/Configuration.scala`；
5. 大于 1MB 的请求体不要走 RPC，改走 BML 上传 + 传 resourceId；
6. 模板代码：[`.ai/snippets.md` §6 RPC](.ai/snippets.md#6-rpcsender-客户端--receiverreceiverchooser-服务端)。

#### 9.4.4 加一个 RESTful 接口（前端调用）

1. Controller 在 `<service>/src/main/java/.../restful/`，类名后缀 `RestfulApi`；
2. `@RestController` + `@RequestMapping("/api/rest_j/v1/<module>/<action>")`（公共 `rest_j`，私有 `rest_s`）；
3. 鉴权走 `ModuleUserUtils.getOperationUser(req, ...)`，**不要用 `request.getUserPrincipal()`**；
4. 免鉴权路径需在 `linkis.properties` 的 `wds.linkis.server.user.restful.uri.pass.auth` 中显式声明；
5. 返回值统一 `Message.ok()` / `Message.error()`；
6. 模板代码：[`.ai/snippets.md` §9 Java Restful Controller](.ai/snippets.md#9-java-restful-controllermessage--鉴权)。

#### 9.4.5 加一个数据库表 / Mapper

1. DDL 放 `linkis-dist/package/db/linkis_<module>.sql`（升级脚本 `linkis-dist/package/db/upgrade/<version>/`）；
2. Java 实体 + Mapper 接口 + `*Mapper.xml`（路径 `src/main/resources/mapper/common/`）；
3. Service 实现加 `@Transactional(rollbackFor = Exception.class)`；
4. 慢 SQL 注意：所有列表查询加分页、加索引（参考近期 commit `[fix] 修复linkismanager 慢sql`）；
5. 模板代码：[`.ai/snippets.md` §10 Java Service + MyBatis](.ai/snippets.md#10-java-service--mybatistransactional--mapper)。

#### 9.4.6 加一个前端管理页面

> 接到前端需求时**先回答 3 个问题**：① 改哪个 App？② 改哪个 module？③ 是新增还是扩展现有？

1. **定位 App**：对照 [§3.2.1](#321-前端多-app-架构评估前端需求必读)，4 个 App（linkis/URM/scriptis/PythonModule）对应不同后端；
2. **查重**：在 [`.ai/frontend/apps-linkis-modules.md`](.ai/frontend/apps-linkis-modules.md) / [`.ai/frontend/apps-urm-modules.md`](.ai/frontend/apps-urm-modules.md) 里看是否已有同义 module；
3. **新增 module**：按 [`.ai/frontend/module-template.md`](.ai/frontend/module-template.md) 复制标准目录结构（index.js + index.vue + vlist.vue + addXxxModal.vue + service.js）；
4. **扩展 module**：直接改 `index.vue`（加搜索条件/表格列）或 `vlist.vue`（加列）或 `addXxxModal.vue`（加表单字段）；
5. **注册路由**：在 `src/apps/<app>/router.js` 加路由项；
6. **加 i18n**：**同步**改 `zh.json` + `en.json`；
7. **后端联动**：看 URL 前缀定位后端模块（如 `/basedata-manager/*` → linkis-basedata-manager），按 §9.4.4 加 RestfulApi / §9.4.5 加 Mapper；
8. **UDF/函数特殊**：URM 两个 module（udfManagement + functionManagement）共享后端，**加功能几乎都要同步改两个 module**；
9. 模板代码：[`.ai/snippets.md` §13 前端模板](.ai/snippets.md#13-前端模板apifetch--模块注册--路由--i18n)。

### 9.5 ★ 绝对不能动的"地雷区"

| 区域 | 文件/路径 | 风险 |
| --- | --- | --- |
| Protocol 协议类 | `linkis-commons/linkis-protocol/src/main/scala/.../protocol/**` 下的 case class | 跨服务序列化兼容，改字段会直接炸 RPC |
| `codecheck.ignore` 列出的文件 | 见仓库根 `codecheck.ignore`（DES、AES、ContextSerializationHelper、LabelManagerMapper.xml 等） | 已豁免检查，改动需人工 review |
| 自动生成的 Mapper XML | `*/target/classes/mapper/**` | 编译产物，改了会被覆盖 |
| Spring Bean 名称约定 | `ServiceNameConsts.*` | Bean 名变了会导致 `@Qualifier` 注入失败 |
| 拦截器链顺序 | `EntranceSpringConfiguration.entranceInterceptors()` | 顺序错可能导致变量未替换、Label 未填充等 |
| `linkis-engineconn.properties` | 各 engineconn plugin 的 `src/main/resources/linkis-engineconn.properties` | 删 `wds.linkis.engineconn.plugin.default.class` 会导致引擎无法启动 |
| 错误码数值 | `docs/errorcode/<module>-errorcode.md` 中已分配的号段 | 已上报监控/告警，改号会触发误告警 |

> 上述区域如确需改动，**必须在 PR 描述里列出影响面 + 给出兼容方案**，不要静默修改。

---

## 10. ★ 任务执行主线流程与高危区域

> 当新需求涉及以下"主流程"中的任何一环时，**改动优先级最高、回归风险最大**——必须先看 §4（铁律）+ 本节，再动手。

### 10.1 任务执行 10 步主线（前端 → EC 回调）

```
[1] 前端                 ──HTTP──┐
                                 ▼
[2] linkis-mg-gateway    鉴权 / 路由（SecurityFilter + ModuleUserUtils）
                                 │
                                 ▼
[3] linkis-entrance      EntranceServer.execute(params)        ← 主入口
                                 │
        ┌────────────────────────┼────────────────────────────┐
        │                        │                            │
[4] 拦截器链                 [5] Parser                  [6] Orchestrator
    (EntranceInterceptor        (SQL/Python/Scala           编排 → 拆任务
     链: 顺序敏感!)              /Pipeline...)
                                                              │
                                                              ▼
[7] AM (LinkisManager)   DefaultEngineAskEngineService
                          ├── 复用: DefaultEngineReuseService.reuseEngine()
                          └── 新建: DefaultEngineCreateService.createEngine()
                                                              │
                                                              ▼
[8] ECM   EngineConnLaunchService.createEngineConnLaunchRequest()
        └── BmlResourceLocalizationService.handleInitEngineConnResources()  ← 拉物料
                                                              │
                                                              ▼
[9] EC 进程拉起           EngineConnPlugin → Executor 接任务
        └── TaskExecutionServiceImpl.execute()                │
                                                              │
                                                              ▼
[10] EC ──RPC──> Entrance  sendToEntrance()                    │
        │                                                      │
        ▼                                                      ▼
   日志回写（LogManager）            结果集回写（StorageService）
        │                                                      │
        ▼                                                      ▼
   PersistenceManager 持久化 jobhistory        ← 任务终态
```

### 10.2 高危区域清单（修改前必须看 §4 + §9.5）

| # | 环节 | 关键类/方法 | 路径 | 风险点 |
| --- | --- | --- | --- | --- |
| 1 | 任务主入口 | `EntranceServer.execute()` | `linkis-entrance/src/main/scala/.../EntranceServer.scala` | 主入口，任何异常都会让任务失败 |
| 2 | 拦截器链 | `EntranceSpringConfiguration.entranceInterceptors()` | `linkis-entrance/src/main/java/.../conf/EntranceSpringConfiguration.java` | **顺序敏感**：变量未替换/Label 未填充会跑错引擎 |
| 3 | 引擎新建 | `DefaultEngineCreateService.createEngine()` | `linkis-manager/.../am/service/engine/DefaultEngineCreateService.scala` | 6 步流程：资源检查 → 创建 → 启动 → 注册；中途失败会泄漏 EC 进程 |
| 4 | 引擎复用 | `DefaultEngineReuseService.reuseEngine()` | `linkis-manager/.../am/service/engine/DefaultEngineReuseService.scala` | 并发选同一个 EC 导致任务串扰；标签匹配错会路由错引擎 |
| 5 | EC 接任务 | `TaskExecutionServiceImpl.execute()` | `linkis-computation-engineconn/.../executor/service/TaskExecutionServiceImpl.scala` | EC 侧主入口，异常会让任务 hang |
| 6 | 回调 Entrance | `sendToEntrance()`（位于 EC 内） | `linkis-computation-engineconn/...` | 回调失败 → Entrance 拿不到日志/结果/进度 |
| 7 | 任务持久化 | `PersistenceManager` | `linkis-entrance/.../persistence/PersistenceManager.scala` | jobhistory 终态写错会导致状态机错乱 |
| **8** | **★ LM 启动时刷新引擎物料到 BML/HDFS** | `DefaultEngineConnResourceService.refresh()` + `AbstractEngineConnBmlResourceGenerator` | `linkis-application-manager/.../engineplugin/server/service/DefaultEngineConnResourceService.scala`、`.../localize/AbstractEngineConnBmlResourceGenerator.scala` | 改错会让所有新建 EC 拿不到物料；删文件会导致引擎启动 404 |
| **9** | **★ ECM 启动 EC 前从 HDFS 拉物料到本地** | `BmlResourceLocalizationService.handleInitEngineConnResources()` + `downloadBmlResource()` | `linkis-engineconn-manager/.../ecm/server/service/impl/BmlResourceLocalizationService.scala` | workDir/logDirs/tmpDirs/linkDirs 拼错 → EC 进程起不来；并发下载不锁 `resourceId.intern()` 会撕裂文件 |
| **10** | **★ 引擎启动脚本组装** | `JavaProcessEngineConnLaunchBuilder` + `DefaultEngineConnLaunchService.createEngineConnLaunchRequest()` | `linkis-engineconn-plugin-core/.../launch/process/JavaProcessEngineConnLaunchBuilder.scala`、`linkis-application-manager/.../engineplugin/server/service/DefaultEngineConnLaunchService.scala` | JVM 参数/classpath/`--conf` 拼错 → EC 启动 OOM 或 ClassNotFound；hadoop/profile 切换后参数需同步 |

### 10.3 修改主流程代码的硬性要求

> 这些要求叠加在 §4（铁律）之上，是**主流程特有的**：

1. **任何主流程改动必须先加开关**，且开关默认 `false`（参考 §4.1.1）。即使开关关掉时走老路径，老路径的输入输出契约也不能变；
2. **修改 #2 #3 #4 #8 #9 #10 时必须有回归用例**：在 EC 拉起 / 任务回写 / 物料下载三个环节各跑一次冒烟，确认开关 on/off 都能跑通；
3. **修改 #8 / #9（物料链）时尤其注意并发与重试**：`downloadBmlResource` 用 `resourceId.intern().synchronized` 防文件撕裂，不要去掉；ECM 多 EC 并发拉物料时单点失败会导致整机 EC 都起不来；
4. **修改 #10（脚本组装）时必须覆盖所有受影响 profile**：默认 spark-2.4.3 / hadoop-2.7.2 / scala-2.11，spark-3 / hadoop-3.3 / hbase-2.5 profile 下 classpath、`--conf`、JNI 路径都不同；
5. **不能改 9.5 列出的"地雷区"**——如果新需求看起来必须动 protocol case class / Bean 名 / `linkis-engineconn.properties` 的关键 key，先和 reviewer 确认是否有不动的实现路径。

### 10.4 涉及主流程时的 PR 自检清单（除 §4.3 外）

- [ ] 是否已确认改动落在 §10.2 哪一行？把这行号写在 PR 描述里；
- [ ] 开关 on / off 两种状态都跑过冒烟（提交 SQL → EC 拉起 → 日志/结果回写 → jobhistory 落库）；
- [ ] 是否并发场景下验证过（同一用户同时提交 ≥ 3 个任务）；
- [ ] 修改了 #8/#9 时，是否确认 `resourceId.intern()` 锁还在、`linkDirs` 没断；
- [ ] 修改了 #10 时，是否在 spark-2.4 + spark-3 双 profile 下各 build 一次。
