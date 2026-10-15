# incubator-linkis - CLAUDE.md

> 本文件由 dev-sync-agent install.bat --core 自动生成
> 生成时间: 2026-10-08 15:06:50
> 语言栈: java + python + frontend
> 生成器版本: 1.0.0

## ━━━ 基础通用部分（所有大数据项目相同）━━━

> 以下内容由 dev-sync-agent 平台维护，适用于所有大数据平台工具系统项目。

### 1. Git 工作流

- **分支策略**：采用 `main`（主分支）+ `develop`（开发分支）+ `feature/*`（功能分支）+ `hotfix/*`（修复分支）模式
- **commit 规范**：使用 `#AI commit#` 前缀标识 AI 生成提交，提交信息使用中文描述
- **PR 流程**：所有变更必须通过 PR 评审合并，至少 1 人审批
- **禁止行为**：
  - ❌ 禁止直接 push 到 `main`/`develop` 分支
  - ❌ 禁止使用 `--no-verify` 跳过 hooks
  - ❌ 禁止使用 `git push --force` 到共享分支

### 2. CI/CD 流程

- **构建阶段**：代码编译 → 单元测试 → 静态扫描 → 打包 → 部署
- **质量门禁**：
  - 单元测试覆盖率 ≥ 80%
  - 静态扫描无 P0/P1 级别问题
  - 代码风格检查通过
- **部署环境**：dev → staging → prod，逐级晋升
- **回滚策略**：保留最近 3 个版本，支持一键回滚

### 3. 代码评审

- **评审清单**：
  - 功能正确性（是否满足需求）
  - 代码质量（命名、结构、可读性）
  - 测试覆盖（单测、集成测试）
  - 安全性（无注入、无敏感信息泄露）
  - 性能影响（无 N+1 查询、无大对象频繁创建）
- **责任人**：每个模块有明确 owner，PR 必须由 owner 或同级开发评审

### 4. 数据安全

- **PII 数据处理**：涉及个人身份信息（PII）的字段必须脱敏存储
- **数据分级**：
  - 公开数据（L1）：可公开访问
  - 内部数据（L2）：仅内部可见
  - 敏感数据（L3）：需权限控制
  - 机密数据（L4）：需加密存储 + 审计
- **数据对外提供**：外部机构或客户数据需经过合规审批
- **监管报送**：涉及监管报送的数据需经过双复核

### 5. 命名总则

- **项目命名**：`{团队}-{领域}-{功能}`，如 `bdp-govern-metadata`
- **包名/模块名**：小写字母，点分层级，如 `com.bank.bdp.govern.metadata`
- **类名**：大驼峰，如 `MetadataService`
- **方法名/变量名**：小驼峰，如 `getMetadataById`
- **常量名**：全大写下划线，如 `MAX_RETRY_COUNT`
- **数据库表名**：小写下划线，如 `metadata_table`
- **数据库字段名**：小写下划线，如 `table_name`

### 6. 跨组件协作

- **接口协议**：REST API 优先使用 JSON，内部高性能场景使用 Protobuf
- **数据格式**：大数据场景优先使用 Parquet/ORC 列式存储
- **消息格式**：Kafka 消息使用 Avro + Schema Registry
- **版本兼容**：接口变更需保持向后兼容，破坏性变更需版本号升级
- **错误码**：统一错误码规范 `{组件代号}-{错误类型}-{序号}`

### 7. 环境规范

- **配置分离**：dev/staging/prod 配置必须分离，禁止硬编码
- **密钥管理**：密钥、密码等敏感信息不得入库，使用配置中心或环境变量
- **日志规范**：
  - 日志级别：DEBUG/INFO/WARN/ERROR
  - 日志格式：JSON 结构化日志
  - 敏感信息禁止打印到日志
- **监控指标**：关键业务指标必须暴露到 Prometheus

### 8. 监控告警总则

- **指标命名**：`{组件}_{指标类型}_{具体指标}`，如 `govern_metadata_query_total`
- **告警分级**：
  - P0：立即处理（电话通知）
  - P1：30 分钟内处理（即时通讯通知）
  - P2：2 小时内处理（工单）
- **关键 SLI**：
  - 可用性：成功率 ≥ 99.9%
  - 响应时间：P99 ≤ 规定阈值
  - 错误率：≤ 0.1%
- **告警通道**：企微机器人 + 邮件 + 电话（P0）

---

## ━━━ 项目特色部分（本项目特有）━━━

> 以下内容由 dev-sync-agent 生成器根据 incubator-linkis 项目信息自动提取，如有不准确处请人工修正。

### D1. 组件身份卡

- 项目一句话定位: <h2 align="center">
- 工具系统类型: 数据服务
- 项目名: incubator-linkis
- 平台角色: 数据服务层，提供数据API/网关能力
- 协作组件: spring-jdbc

<details><summary>证据来源 (8 条)</summary>

- `doc` D:\kkhuang\project\incubator-linkis\README.md - <h2 align="center">
- `doc` D:\kkhuang\project\incubator-linkis\docs - 匹配关键词: ['API', '网关', '服务', '数据中台', 'service', 'gateway'] (总分 749)
- `config` D:\kkhuang\project\incubator-linkis\pom.xml - artifactId/project: incubator-linkis
- `doc` D:\kkhuang\project\incubator-linkis\README.md - 数据服务层，提供数据API/网关能力
- `code` D:\kkhuang\project\incubator-linkis\linkis-public-enhancements\linkis-cs-server\src\test\java\org\apache\linkis\cs\persistence\Scan.java:24 - JdbcTemplate

</details>

---

### D2. 技术栈特征

- java 版本: 1.8
- 核心框架: Spring Boot:${spring.boot.version}, Spring Boot Web:${spring.boot.version}, Spring Boot Test:
- 入口点框架: spring-mvc, spring-boot (共 399 个入口)
- 关键依赖: org.apache.linkis:linkis-module:${project.version}, org.apache.linkis:linkis-common:${project.version}, org.apache.linkis:linkis-mybatis:${project.version}, com.thoughtworks.xstream:xstream:${xstream.core.version}, org.scala-lang:scala-library:${scala.version}, org.scala-lang:scala-compiler:${scala.version}, org.scala-lang:scala-reflect:${scala.version}, org.scala-lang:scalap:${scala.version}, com.google.guava:guava:${guava.version}, xml-apis:xml-apis:1.4.01, org.apache.logging.log4j:log4j-bom:${log4j2.version}, org.slf4j:slf4j-api:${slf4j.version}, org.javassist:javassist:${javassist.version}, org.apache.ant:ant:${ant.version}, org.apache.ant:ant-launcher:${ant.version}, org.reflections:reflections:${reflections.version}, commons-collections:commons-collections:${commons-collections.version}, org.apache.commons:commons-lang3:${commons-lang3.version}, org.apache.commons:commons-text:${commons-text.version}, org.apache.commons:commons-math3:${commons-math3.version}

<details><summary>证据来源 (6 条)</summary>

- `config` D:\kkhuang\project\incubator-linkis\pom.xml - version=1.8
- `config` D:\kkhuang\project\incubator-linkis\pom.xml - frameworks=['Spring Boot:${spring.boot.version}', 'Spring Boot Web:${spring.boot
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-module\src\main\java\org\apache\linkis\DataWorkCloudApplication.java:55 - DataWorkCloudApplication
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-module\src\main\java\org\apache\linkis\restful\CommonRestfulApi.java:37 - CommonRestfulApi.RequestMapping(/)
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-module\src\main\java\org\apache\linkis\restful\CommonRestfulApi.java:43 - CommonRestfulApi.RequestMapping(/offline)

</details>

---

### D3. 开发范式约束

**[⚠️ 待人工确认]**

- 领域术语: configuration change - | Module Name (Service Name) |Type| Parameter Name | Default Value | Description
- 领域术语: 参数变化 - | 模块名(服务名)| 类型  |     参数名                                                | 默认值  
- 领域术语: 特性说明 - | 模块名(服务名)| 类型   | 特性                                                    | | ---
- 领域术语: 1. 环境准备 - 如果您希望在您的服务器上使用 Trino 引擎，您需要准备 Trino 服务并提供连接信息，如 Trino 集群的连接地址、用户名和密码等 注意: 编译 Tri
- 领域术语: 2. 部署和配置 - 注意: 编译 Trino 引擎之前需要进行 Linkis 项目全量编译 发布的安装部署包中默认不包含此引擎插件， 你可以按[Linkis引擎安装指引](http
- 领域术语: 2.1 版本的选择和编译 - 注意: 编译 Trino 引擎之前需要进行 Linkis 项目全量编译 发布的安装部署包中默认不包含此引擎插件， 你可以按[Linkis引擎安装指引](http
- 领域术语: 2.2 物料的部署和加载 - 将 2.1 步编译出来的引擎包,位于 ```bash ${linkis_code_dir}/linkis-engineconn-plugins/trino/ta
- 领域术语: 2.3 引擎的标签 - Linkis1.X是通过标签来进行的，所以需要在我们数据库中插入数据，插入的方式如下文所示。 管理台的配置是按照引擎标签来进行管理的，如果新增的引擎，有配置参数
- 领域术语: 2.4 Trino 引擎相关配置 - | 配置                                    | 默认值                 |是否必须 | 说明        
- 领域术语: 3. Trino - 您需要配置Trino的连接信息，包括连接地址信息或用户名密码(如果启用)等信息。 ![Trino](https://user-images.githubuser
- 领域术语: 3.1 准备操作 - 您需要配置Trino的连接信息，包括连接地址信息或用户名密码(如果启用)等信息。 ![Trino](https://user-images.githubuser
- 领域术语: 3.2 通过Linkis-cli进行任务提交 - **使用示例** Linkis 1.0后提供了cli的方式提交任务，我们只需要指定对应的EngineConn标签类型即可，Trino的使用如下： ```shel
- 领域术语: 4. Trino引擎的用户设置 - Trino的用户设置主要是设置Trino的连接信息，但是建议用户将此密码等信息进行加密管理。
- 领域术语: AISQL任务支持StarRocks引擎类型切换设计文档 - 为AISQL类型任务增加StarRocks引擎类型切换支持，通过runtime参数或脚本注释两种方式实现引擎切换，并集成Doctoris服务进行引擎决策。 - 
- 领域术语: 1. 设计概述 - 为AISQL类型任务增加StarRocks引擎类型切换支持，通过runtime参数或脚本注释两种方式实现引擎切换，并集成Doctoris服务进行引擎决策。 - 
- 领域术语: 1.1 目标 - 为AISQL类型任务增加StarRocks引擎类型切换支持，通过runtime参数或脚本注释两种方式实现引擎切换，并集成Doctoris服务进行引擎决策。 - 
- 领域术语: 1.2 设计原则 - - **最小改动原则**：在现有架构基础上扩展，不修改现有Spark/Hive引擎切换逻辑 - **可配置原则**：通过功能开关控制，开关关闭时相当于回退到上一
- 领域术语: 1.3 适用范围 - - AISQL类型任务提交流程 - 引擎类型切换逻辑 - Doctoris服务调用
- 领域术语: 2. 整体架构 - ``` 用户提交AISQL任务 ↓
- 领域术语: 2.1 系统架构图 - ``` 用户提交AISQL任务 ↓
- 领域术语: 2.2 处理优先级 - ``` 1. Runtime参数 (ec.engine.type=starrocks) ↓ 若未设置
- 领域术语: 3. 详细设计 - ```scala // StarRocks引擎切换功能开关 val AISQL_STARROCKS_SWITCH = CommonVars("linkis.ai
- 领域术语: 3.1 配置设计 - ```scala // StarRocks引擎切换功能开关 val AISQL_STARROCKS_SWITCH = CommonVars("linkis.ai
- 领域术语: 3.1.2 配置说明 - | 配置项 | 默认值 | 说明 | |--------|--------|------| | linkis.aisql.starrocks.switch | 
- 领域术语: 3.2 脚本注释解析设计 - ```scala object TemplateConfUtils { // 现有配置
- 领域术语: 3.2.2 注释格式支持 - 支持三种注释格式： - **SQL/HQL格式**：`---@set ec.engine.type=starrocks` - **Python/Shell格式*
- 领域术语: 3.2.3 实现逻辑 - 利用现有的`getCustomTemplateConfName`方法机制，扩展支持解析`ec.engine.type`配置： ```scala def getC
- 领域术语: 3.3 引擎切换逻辑设计 - 在进行引擎切换之前，需要先检查用户是否有权限使用StarRocks引擎： ```scala /**
- 领域术语: 3.3.1 白名单检查设计 - 在进行引擎切换之前，需要先检查用户是否有权限使用StarRocks引擎： ```scala /**
- 领域术语: 3.3.2 StarRocks引擎切换实现 - ```scala private def changeToStarRocksEngine( labels: util.List[Label[_]],
- 领域模型:  (类型: entity)
- 领域模型: CgManagerLabel (类型: entity)
- 领域模型: ConfigurationConfigKey (类型: entity)
- 领域模型: ConfigurationConfigValue (类型: entity)
- 领域模型: ConfigurationKeyEngineRelation (类型: entity)
- 领域模型: DatasourceAccessEntity (类型: entity)
- 领域模型: DatasourceEnvEntity (类型: entity)
- 领域模型: DatasourceTypeEntity (类型: entity)
- 领域模型: DatasourceTypeKeyEntity (类型: entity)
- 领域模型: EngineConnPluginBmlResources (类型: entity)
- 领域模型: ErrorCodeEntity (类型: entity)
- 领域模型: GatewayAuthRelationTokenEntity (类型: entity)
- 领域模型: GatewayAuthTokenEntity (类型: entity)
- 领域模型: RmExternalResourceProviderEntity (类型: entity)
- 领域模型: UdfBaseInfoEntity (类型: entity)
- 领域模型: UdfManagerEntity (类型: entity)
- 领域模型: UdfTreeEntity (类型: entity)
- 基于 spring-boot 的业务流程入口: DataWorkCloudApplication, LinksMonitorApplication, TestContextHAManager, SpringCloudEurekaApplication
- 基于 spring-mvc 的业务流程入口: CommonRestfulApi.RequestMapping(/), CommonRestfulApi.RequestMapping(/offline), ECMRestfulApi.RequestMapping(/engineconnManager), ECMRestfulApi.RequestMapping(/downloadEngineLog), EntranceConsumerRestfulApi.RequestMapping(/entrance/operation/consumer), EntranceConsumerRestfulApi.RequestMapping(/kill), EntranceConsumerRestfulApi.RequestMapping(/info), EntranceLabelRestfulApi.RequestMapping(/entrance/operation/label), EntranceLabelRestfulApi.RequestMapping(/update), EntranceLabelRestfulApi.RequestMapping(/markoffline)
- 架构模式: 未识别标准模式，主要目录: 
- 代码规范工具: 未检测到 linter 配置（建议补充）
- 命名约定: 类名: 混合风格

- ⚠️ 架构模式: 未识别标准模式，主要目录: 
- ⚠️ 代码规范工具: 未检测到 linter 配置（建议补充）

<details><summary>证据来源 (20 条)</summary>

- `doc` D:\kkhuang\project\incubator-linkis\docs\configuration-change-records.md - | Module Name (Service Name) |Type| Parameter Name | Default Value | Description
- `doc` D:\kkhuang\project\incubator-linkis\docs\info-1.1.3.md - | 模块名(服务名)| 类型  |     参数名                                                | 默认值  
- `doc` D:\kkhuang\project\incubator-linkis\docs\info-1.2.1.md - | 模块名(服务名)| 类型   | 特性                                                    | | ---
- `doc` D:\kkhuang\project\incubator-linkis\docs\trino-usage.md - 如果您希望在您的服务器上使用 Trino 引擎，您需要准备 Trino 服务并提供连接信息，如 Trino 集群的连接地址、用户名和密码等 注意: 编译 Tri
- `doc` D:\kkhuang\project\incubator-linkis\docs\trino-usage.md - 注意: 编译 Trino 引擎之前需要进行 Linkis 项目全量编译 发布的安装部署包中默认不包含此引擎插件， 你可以按[Linkis引擎安装指引](http

</details>

---

### D4. 构建与提交

- 构建工具: maven, 构建命令: mvn clean package
- 部署方式: JAR 包部署（建议补充具体方式）

<details><summary>证据来源 (2 条)</summary>

- `config` D:\kkhuang\project\incubator-linkis\pom.xml - build_tool=maven
- `config` D:\kkhuang\project\incubator-linkis - JAR 包部署（建议补充具体方式）

</details>

---

### D5. 测试与验证

- 测试框架: junit5, 测试目录: 默认
- 覆盖率配置: D:\kkhuang\project\incubator-linkis\pom.xml

<details><summary>证据来源 (2 条)</summary>

- `code` D:\kkhuang\project\incubator-linkis - test_count=0
- `config` D:\kkhuang\project\incubator-linkis\pom.xml - coverage config

</details>

---

### D6. 运维与监控

**[❌ 待人工补充]**

- ❌ 未提取到 运维与监控 信息，建议人工补充

---

### D7. 坑点与禁忌

- [TODO] D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-rpc\src\main\java\org\apache\linkis\rpc\message\parser\DefaultServiceParser.java:43 - // TODO: 2020/7/15 more analysis
- [历史修复] 141511eb6 #AI commit# 开发阶段：Bug修复 - BDPServerHelper getMultipartConfigElement增加空保护兜底
- [历史修复] 9aedec081 #AI commit# 开发阶段：安全工单修复
- [历史修复] bdbbbc0fb #AI commit# 开发阶段：修复编译异常
- [历史修复] eadf30fc8 Merge pull request #1097 from WeDataSphere/dev-2.1.3-engineplugin-tgt-fix
- [历史修复] 8c66d3d9e #AI commit# feat： 修复引擎keytab认证失败异常
- [历史修复] 944b165ed #AI commit# 测试阶段：修复单元测试 (#1089)
- [历史修复] c62b7bffc #AI commit# 开发阶段：修复keytab开启缓存时，引发TGT错误 (#1093)
- [历史修复] bdee83c6a #AI commit# 测试阶段：修复安全工单
- [历史修复] c8d8653ad Merge pull request #1072 from WeDataSphere/dev-2.0.0-bug-fix
- [历史修复] 8e63affe2 Merge pull request #1082 from WeDataSphere/dev-2.1.0-public-oom-fix
- [历史修复] 5a027f63e Merge pull request #1085 from WeDataSphere/dev-2.1.0-jackson-length
- [历史修复] cc4ae3112 Merge pull request #1086 from WeDataSphere/dev-2.1.0-fix-sdk-result-error
- [历史修复] ce7eb7b00 Merge pull request #1088 from WeDataSphere/dev-2.1.0-security-order
- [历史修复] bbb28ff8b Merge branch 'dev-2.1.0-webank' into dev-2.0.0-bug-fix
- [历史修复] 7f5a634a2 #AI commit# 开发阶段：修复获取结果集null异常
- [历史修复] 9d8e53891 #AI commit# 开发阶段：修复编译异常
- [历史修复] ecf71ff59 #AI commit# 开发阶段：安全工单修复： 1.libthrift 0.9.3 -> 0.23.0 2.移除mchange 3.zookeeper升级 3.8.4 -> 3.8.6
- [历史修复] 01d34f526 #AI commit# 开发阶段：修复client多结果集解析异常
- [历史修复] c7cb43e64 #AI commit# 开发阶段：修复jackson 2.15 限制500000 导致解析异常
- [历史修复] 4ead02cf9 #AI commit# 开发阶段：修复读取大结果导致服务oom异常

<details><summary>证据来源 (20 条)</summary>

- `comment` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-rpc\src\main\java\org\apache\linkis\rpc\message\parser\DefaultServiceParser.java:43 - // TODO: 2020/7/15 more analysis
- `comment` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-storage\src\main\java\org\apache\linkis\storage\factory\impl\BuildHDFSFileSystem.java:46 - // TODO Agent user(代理的用户)
- `comment` D:\kkhuang\project\incubator-linkis\linkis-computation-governance\linkis-client\linkis-cli\src\main\java\org\apache\linkis\cli\application\CtxBuilder.java:61 - /** generate Templates load env variables TODO: load version info */
- `git` D:\kkhuang\project\incubator-linkis - 141511eb6 #AI commit# 开发阶段：Bug修复 - BDPServerHelper getMultipartConfigElement增加空保
- `git` D:\kkhuang\project\incubator-linkis - 9aedec081 #AI commit# 开发阶段：安全工单修复

</details>

---

### D8. 组件集成

- db 集成: 框架=spring-jdbc, 目标示例=

<details><summary>证据来源 (3 条)</summary>

- `code` D:\kkhuang\project\incubator-linkis\linkis-public-enhancements\linkis-cs-server\src\test\java\org\apache\linkis\cs\persistence\Scan.java:24 - JdbcTemplate
- `code` D:\kkhuang\project\incubator-linkis\linkis-public-enhancements\linkis-cs-server\src\test\java\org\apache\linkis\cs\persistence\Scan.java:40 - JdbcTemplate
- `code` D:\kkhuang\project\incubator-linkis\linkis-public-enhancements\linkis-cs-server\src\test\java\org\apache\linkis\cs\persistence\Scan.java:40 - JdbcTemplate

</details>

---

### D9. 性能调优

**[⚠️ 待人工确认]**

- ⚠️ 性能调优 为可选维度，无自动提取结果

---

### D10. 安全合规

- [加密算法] D:\kkhuang\project\incubator-linkis\.claude\templates\testing\visual-regression-steps.java:172 - des
- [TLS 传输加密] D:\kkhuang\project\incubator-linkis\.claude\templates\testing\webui-steps.java:64 - HTTPS
- [加密算法] D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-common\src\main\java\org\apache\linkis\common\errorcode\ErrorCodeUtils.java:23 - deS
- [手机号] D:\kkhuang\project\incubator-linkis\.claude\templates\testing\visual-regression-steps.java:154 - Phone

<details><summary>证据来源 (20 条)</summary>

- `code` D:\kkhuang\project\incubator-linkis\.claude\templates\testing\visual-regression-steps.java:172 - des
- `code` D:\kkhuang\project\incubator-linkis\.claude\templates\testing\webui-steps.java:64 - HTTPS
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-common\src\main\java\org\apache\linkis\common\errorcode\ErrorCodeUtils.java:23 - deS
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-common\src\main\java\org\apache\linkis\common\errorcode\LinkisErrorCode.java:36 - des
- `code` D:\kkhuang\project\incubator-linkis\linkis-commons\linkis-common\src\main\java\org\apache\linkis\common\errorcode\LinkisErrorCode.java:38 - des

</details>

---

## ━━━ 待人工补充项 ━━━

> 以下项目置信度较低或缺失，建议人工补充完善。

- [ ] [D3 开发范式约束] 架构模式: 未识别标准模式，主要目录: 
- [ ] [D3 开发范式约束] 代码规范工具: 未检测到 linter 配置（建议补充）
- [ ] [D6 运维与监控] 未提取到 运维与监控 信息，建议人工补充
- [ ] [D9 性能调优] 性能调优 为可选维度，无自动提取结果

## ━━━ 生成元信息 ━━━

- **生成时间**: 2026-10-08 15:06:50
- **生成器版本**: 1.0.0
- **主语言**: java
- **次语言**: python, frontend
- **语言置信度**: 100%
- **规则执行数**: 10
- **规则失败数**: 0
- **证据覆盖率**: 70%
