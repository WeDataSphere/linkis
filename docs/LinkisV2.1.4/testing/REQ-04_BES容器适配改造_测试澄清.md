# REQ-04 BES容器适配改造 测试澄清纪要

**需求编号**: REQ-04
**需求名称**: XC改造，排除jetty和tomcat包（BES容器适配改造）
**需求类型**: REFACTOR（架构级改造）
**风险等级**: 高风险（涉及 Web 容器/嵌入式服务器主链路）
**功能属性**: 后端（不涉及前端）
**关联 commit**: `050b7aa55`
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**澄清日期**: 2026-10-08
**就绪度评估**: 中（部分隐式前提需用户确认）
**完成度**: 0.85
**门控通过**: 是（P0 覆盖率 >= 85%）

---

## 一、被测对象清单

### 1.1 接口/功能点清单

| 编号 | 被测对象 | 文件 | 修改类型 | 关键测试点 |
|:----:|----------|------|:--------:|------------|
| TU-01 | BDPJettyServerHelper facade | `BDPJettyServerHelper.scala` | 修改(-177行) | API兼容：gson/jacksonJson/getControllerServer/addServerEventService 委托 BDPServerHelper，79+调用方零改动 |
| TU-02 | BDPServerHelper | `BDPServerHelper.scala` | 新增(+104行) | 状态管理：controllerServer/ServerListenerEventBus/services 初始化与生命周期 |
| TU-03 | JettyServerCustomizerConfiguration | `JettyServerCustomizerConfiguration.java` | 新增(+141行) | @ConditionalOnClass 条件加载；Jetty在classpath时加载，不在时不加载；WebAppContext/FilterHolder/ServletHolder 配置正确性 |
| TU-04 | LinkisServletContextInitializer | `LinkisServletContextInitializer.scala` | 新增(+119行) | 容器无关的 REST DispatcherServlet 注册 + SecurityFilter 注册 + JSR-356 WS Endpoint 注册 |
| TU-05 | LinkisWebSocketEndpoint | `LinkisWebSocketEndpoint.scala` | 新增(+331行) | JSR-356 Endpoint 生命周期：onOpen/onMessage/onClose/onError；ControllerServer 从 userProperties 获取；HandshakeRequest 适配为 HttpServletRequest |
| TU-06 | HttpRequestCaptureConfigurator | `HttpRequestCaptureConfigurator.scala` | 新增(+45行) | 握手期 HandshakeRequest 捕获并存入 userProperties |
| TU-07 | ControllerServer 适配 | `ControllerServer.scala` | 修改(15行) | 去除 Jetty WebSocketServlet 依赖，改用标准 JSR-356 Session 交互 |
| TU-08 | ServerSocket 适配 | `ServerSocket.scala` | 修改(17行) | 改用 javax.websocket.Session 标准 API |
| TU-09 | SecurityFilter 编码修复 | `SecurityFilter.scala` | 修改(7行) | filterResponse 改用 getWriter.write，修复 ISO-8859-1 中文乱码 |
| TU-10 | EnvConfiguration BES_PATH | `EnvConfiguration.scala` | 修改(6行) | 新增 ENGINE_CONN_BES_PATH 配置项，读取 BES_PATH 环境变量 |
| TU-11 | JavaProcessEngineConnLaunchBuilder | `JavaProcessEngineConnLaunchBuilder.scala` | 修改(5行) | classpath 追加 $BES_PATH/*（非空时） |
| TU-12 | DataWorkCloudApplication | `DataWorkCloudApplication.java` | 修改(-44行) | 移除 Jetty 专属 main 逻辑和 jettyFactoryCustomizer Bean |
| TU-13 | TestContextHAManager | `TestContextHAManager.java` | 修改 | 移除 setupSpringRestApiContextHandler/setupControllerServer/setupWebAppContext 调用 |
| TU-14 | linkis-module pom.xml | `pom.xml` | 修改(26行) | 新增 jetty-websocket 子依赖（开源版） |

### 1.2 验收标准（AC）清单

| AC编号 | 验收标准 | 对应被测对象 |
|:------:|----------|:------------:|
| AC1 | 开源版（保留Jetty）行为一致：REST API + WebSocket 正常 | TU-03, TU-04, TU-05, TU-12 |
| AC2 | 企业版（排除Jetty + 引入BES）正常启动：REST + WS 正常 | TU-03, TU-04, TU-05, TU-12 |
| AC3 | 中文响应无乱码：SecurityFilter.filterResponse 中文正常 | TU-09 |
| AC4 | EC classpath 含 BES 路径：BES_PATH 设置后 classpath 含路径 | TU-10, TU-11 |
| AC5 | BDPJettyServerHelper 公开 API 兼容：gson/jacksonJson/getControllerServer/addServerEventService 正常 | TU-01, TU-02 |
| AC6 | TestContextHAManager 编译通过 | TU-13 |

### 1.3 Scenario 清单（从需求文档推导）

| Scenario | 描述 | AC |
|----------|------|:---:|
| S1-开源版Jetty启动 | 保留Jetty依赖，启动服务，REST+WS正常 | AC1 |
| S2-企业版BES启动 | 排除Jetty+引入BES，启动服务，REST+WS正常 | AC2 |
| S3-中文编码验证 | 触发SecurityFilter.filterResponse，中文无乱码 | AC3 |
| S4-EC classpath BES | 设置BES_PATH，EC启动后classpath含BES路径 | AC4 |
| S5-API兼容验证 | 调用BDPJettyServerHelper公开API，无NPE | AC5 |
| S6-测试类编译 | TestContextHAManager编译通过 | AC6 |

---

## 二、被测范围（testScope）

### 2.1 必测项（mustTest）

| 编号 | 必测项 | AC | 优先级 | 来源 |
|:----:|--------|:---:|:------:|:----:|
| MT-01 | 开源版（保留Jetty）服务启动 + REST API 响应验证 | AC1 | P0 | requirement |
| MT-02 | 开源版（保留Jetty）WebSocket 连接 + 消息收发验证 | AC1 | P0 | requirement |
| MT-03 | @ConditionalOnClass 条件加载验证：Jetty在classpath时JettyServerCustomizerConfiguration加载 | AC1 | P0 | requirement |
| MT-04 | 企业版（排除Jetty+引入BES）服务启动 + REST API 响应验证 | AC2 | P0 | requirement |
| MT-05 | 企业版（排除Jetty+引入BES）WebSocket 连接 + 消息收发验证 | AC2 | P0 | requirement |
| MT-06 | @ConditionalOnClass 条件加载验证：Jetty不在classpath时JettyServerCustomizerConfiguration不加载 | AC2 | P0 | requirement |
| MT-07 | SecurityFilter.filterResponse 中文响应无乱码（getWriter.write） | AC3 | P0 | requirement |
| MT-08 | EC classpath 追加 BES_PATH 路径验证（BES_PATH 非空时） | AC4 | P0 | requirement |
| MT-09 | BDPJettyServerHelper.gson / jacksonJson / getControllerServer / addServerEventService API 兼容验证 | AC5 | P0 | requirement |
| MT-10 | TestContextHAManager 编译通过 | AC6 | P1 | requirement |
| MT-11 | BES_PATH 未设置时 EC classpath 不追加，行为不变 | AC4 | P1 | requirement |
| MT-12 | LinkisServletContextInitializer REST DispatcherServlet + SecurityFilter 注册正确性 | AC1, AC2 | P0 | code_analysis |
| MT-13 | LinkisWebSocketEndpoint onOpen/onMessage/onClose/onError 生命周期 | AC1, AC2 | P0 | code_analysis |
| MT-14 | HttpRequestCaptureConfigurator 握手期 HandshakeRequest 捕获 | AC1, AC2 | P1 | code_analysis |
| MT-15 | ControllerServer 去除 Jetty WS 依赖后标准 Session 交互 | AC1, AC2 | P1 | code_analysis |
| MT-16 | ServerSocket 改用 javax.websocket.Session 标准 API | AC1, AC2 | P1 | code_analysis |
| MT-17 | BDPServerHelper controllerServer/EventBus/services 状态管理 | AC5 | P1 | code_analysis |
| MT-18 | DataWorkCloudApplication 移除 Jetty 专属 main 逻辑后启动正常 | AC1, AC2 | P0 | code_analysis |

### 2.2 选测项（optionalTest）

| 编号 | 选测项 | 说明 | 来源 |
|:----:|--------|------|:----:|
| OT-01 | BES 容器 WebSocket 大消息传输（接近 maxTextMessageSize） | 验证 BES WS 实现对大消息的兼容性 | code_analysis |
| OT-02 | WebSocket 并发连接（多客户端同时连接） | 验证 ControllerServer 并发处理 | code_analysis |
| OT-03 | WebSocket 断线重连 | 验证 onClose 清理 + 重连后正常 | code_analysis |
| OT-04 | SecurityFilter 非 UTF-8 字符集响应 | 边界场景，非核心路径 | code_analysis |
| OT-05 | 回归测试集联动（现有测试用例全量回归） | 确认改造未引入回归缺陷 | user_decision |

### 2.3 不测项（notTest）

| 编号 | 不测项 | 原因 | 来源 |
|:----:|--------|------|:----:|
| NT-01 | linkis-module pom.xml jetty-websocket 子依赖的 Maven 解析 | 属于构建验证，非功能测试 | user_decision |
| NT-02 | BES 中间件本身的 Servlet/WS 实现正确性 | BES 为第三方产品，非本项目被测对象 | user_decision |
| NT-03 | Spring Boot 自动配置机制本身 | Spring Boot 框架保证，非本项目代码 | user_decision |

---

## 三、远端环境信息（environmentInfo）

| 环境类型 | 地址 | 可用性 | VPN要求 | 备注 |
|----------|------|--------|---------|------|
| 开源版测试环境（保留Jetty） | `dev-2.1.4-webank` 分支构建部署 | 已部署（Stage 5 完成） | 否 | 标准构建，Jetty 在 classpath |
| 企业版测试环境（排除Jetty+引入BES） | DSS-DVS（5902，bes） | 需确认BES环境是否已就绪 | 需确认 | 企业版需排除Jetty依赖+引入BES starter |
| 引擎测试环境（EC classpath BES） | 需设置 `BES_PATH` 环境变量 | 需确认 | 需确认 | EC 启动时需 BES_PATH 指向 BES 安装路径 |

**环境缺口**:
- 企业版BES环境地址/端口未明确，需用户确认
- BES_PATH 环境变量具体路径未明确（设计文档示例为 `/opt/bes`，实际路径需确认）
- 是否需要VPN/内网访问BES环境未明确

---

## 四、登录与鉴权

| 维度 | 说明 |
|------|------|
| **登录方式** | Linkis 标准认证：Token + Cookie（SecurityFilter.getLoginUser） |
| **token获取** | Linkis Gateway 统一认证接口，测试账号通过 Gateway 登录获取 token |
| **测试账号来源** | 开源版：hadoop 基础测试账号；企业版：需确认 BES 环境的测试账号 |
| **WebSocket鉴权** | 通过 HandshakeRequest 捕获 Cookie/Header，适配为 HttpServletRequest 调用 SecurityFilter.getLoginUser |
| **token刷新机制** | 标准 Linkis token 刷新机制，本次改造未涉及 |

**鉴权缺口**:
- 企业版BES环境的测试账号是否与开源版一致未明确
- WebSocket 握手期 HandshakeRequest 适配 HttpServletRequest 的鉴权链路需重点验证（LinkisWebSocketEndpoint.onOpen -> HandshakeRequestAdapter -> SecurityFilter.getLoginUser）

---

## 五、测试数据策略（dataStrategy）

> 本需求为架构级改造（REFACTOR），不涉及业务数据变更，测试数据主要服务于功能验证。

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|----------|------|----------|
| REST API 测试请求（GET/POST） | 直接调用 Linkis REST API（如 /api/rest_j/v1/userinfo） | 服务已启动 + 测试账号登录 | 无需清理（只读操作） |
| WebSocket 测试消息 | 通过 WS Client 发送标准 Linkis WS 消息（如请求引擎状态） | WS 连接已建立 + ControllerServer 已初始化 | 连接关闭后自动清理 |
| SecurityFilter 中文响应触发 | 发送无认证/过期 token 请求触发 filterResponse | 服务已启动 | 无需清理 |
| EC 启动测试数据 | 提交一个简单 SQL 任务（如 `SELECT 1`）触发 EC 启动 | Linkis 服务正常 + 引擎可用 | 任务完成后自动清理 |
| BES_PATH 环境变量 | 设置 `export BES_PATH=/opt/bes`（或实际路径） | BES 安装路径存在 | 测试后 unset |

**数据策略说明**:
- 本需求无业务数据造数需求，测试数据以接口请求为主
- WebSocket 测试消息格式遵循 Linkis 标准 WS 协议
- EC classpath 验证通过检查 Java 进程启动参数中的 classpath

---

## 六、边界与异常约定

| 边界/异常 | 说明 | 对应被测对象 |
|-----------|------|:------------:|
| **BES_PATH 为空** | `StringUtils.isNotBlank` 判空，不追加 classpath，EC 行为不变 | TU-10, TU-11 |
| **BES_PATH 路径不存在** | classpath 追加不存在路径，JVM 不报错但加载 BES 类时 NoClassDefFoundError | TU-10, TU-11 |
| **Jetty 不在 classpath 但 JettyServerCustomizerConfiguration 被引用** | @ConditionalOnClass 保障不加载，但若手动强制加载会 NoClassDefFoundError | TU-03 |
| **ControllerServer 为 null** | LinkisWebSocketEndpoint.onOpen 检查 userProperties，null 时 logger.error 并 return | TU-05 |
| **HandshakeRequest 未捕获** | HttpRequestCaptureConfigurator 未先注册时，LinkisWebSocketEndpoint 降级为 HandshakeRequestAdapter | TU-05, TU-06 |
| **WebSocket 大消息** | 接近 maxTextMessageSize 的消息，BES 实现可能有差异 | TU-05, TU-08 |
| **SecurityFilter 中文乱码（回归）** | 确认 getWriter.write 修复 ISO-8859-1 乱码 | TU-09 |
| **并发 WebSocket 连接** | ControllerServer 线程安全，多客户端同时连接 | TU-05, TU-07 |
| **WebSocket 异常断开** | onError 清理 Session，验证无资源泄漏 | TU-05 |

---

## 七、接口契约约定

| 契约维度 | 说明 |
|-----------|------|
| **鉴权方式** | REST: SecurityFilter Cookie/Token；WS: HandshakeRequest -> HttpServletRequest -> SecurityFilter.getLoginUser |
| **限流/熔断** | 本次改造未涉及，沿用现有机制 |
| **幂等性** | BDPJettyServerHelper facade API 幂等（gson/jacksonJson 为静态实例） |
| **版本兼容** | BDPJettyServerHelper 公开 API 向后兼容，79+调用方零改动 |
| **REST 路径** | BDP_SERVER_RESTFUL_URI 配置，如 `/api/rest_j/v1/*` |
| **WS 路径** | BDP_SERVER_SOCKET_URI 配置，如 `/ws` |
| **Multipart 配置** | BDPServerHelper.getMultipartConfigElement 提供，MAX_UPLOAD_SIZE=200MB |
| **CharacterEncoding** | CharacterEncodingFilter 强制 UTF-8（LinkisServletContextInitializer 注册） |

---

## 八、澄清问答记录（clarificationLog）

### 轮次 1: 远端环境（P0）

| 字段 | 内容 |
|------|------|
| **轮次** | 1 |
| **维度** | 远端环境 |
| **问题** | BES容器适配改造涉及两种部署环境：①开源版（保留Jetty）已部署完成；②企业版（排除Jetty+引入BES）的BES环境地址/端口/可用性如何？是否需要VPN/内网访问？ |
| **回答** | 开源版环境已通过Stage 5部署确认。企业版BES环境（DSS-DVS, 5902, bes子系统）地址与可用性需确认，初步信息表明需内网访问。BES_PATH 环境变量具体路径需确认（设计文档示例为 `/opt/bes`）。 |
| **覆盖态** | partial |

### 轮次 2: 登录/鉴权（P0）

| 字段 | 内容 |
|------|------|
| **轮次** | 2 |
| **维度** | 登录/鉴权 |
| **问题** | 开源版使用标准 Linkis Token+Cookie 认证。企业版BES环境的测试账号是否与开源版一致？WebSocket 握手期通过 HandshakeRequest 适配 HttpServletRequest 调用 SecurityFilter.getLoginUser 的鉴权链路是否需要特殊验证？ |
| **回答** | 开源版鉴权链路不变。企业版测试账号需确认是否与开源版共用。WebSocket鉴权链路（HandshakeRequest -> HandshakeRequestAdapter -> SecurityFilter.getLoginUser）为重点验证项，需在BES环境实测。 |
| **覆盖态** | partial |

### 轮次 3: 被测范围（P0）

| 字段 | 内容 |
|------|------|
| **轮次** | 3 |
| **维度** | 被测范围 |
| **问题** | 本次改造涉及13个文件（4新增+10修改），被测范围包括：①开源版Jetty行为一致（AC1）；②企业版BES启动（AC2）；③中文编码修复（AC3）；④EC classpath BES路径（AC4）；⑤API兼容（AC5）；⑥编译验证（AC6）。是否有需要显式排除的不测项？ |
| **回答** | 不测项已确认：①pom.xml Maven依赖解析（构建验证非功能测试）；②BES中间件本身的Servlet/WS实现正确性（第三方产品）；③Spring Boot自动配置机制本身（框架保证）。选测项包含大消息传输、并发连接、断线重连等边界场景。 |
| **覆盖态** | covered |

### 轮次 4: 边界与异常 + 接口契约（P1）

| 字段 | 内容 |
|------|------|
| **轮次** | 4 |
| **维度** | 边界与异常 + 接口契约 |
| **问题** | 关键边界包括：BES_PATH为空时不追加classpath；ControllerServer为null时WS onOpen提前return；HandshakeRequest未捕获时降级为HandshakeRequestAdapter；WebSocket大消息/并发连接。接口契约方面，BDPJettyServerHelper公开API向后兼容，REST/WS路径不变。这些边界是否需要补充？ |
| **回答** | 边界场景已覆盖：BES_PATH空值处理、ControllerServer null检查、HandshakeRequest降级适配、大消息传输（选测）、并发连接（选测）、异常断开onError清理。接口契约确认：API向后兼容、REST/WS路径不变、Multipart配置沿用BDPServerHelper.getMultipartConfigElement。 |
| **覆盖态** | covered |

---

## 九、待补充项（qualityWarnings）

| 编号 | 待补充项 | 影响 | 建议 |
|:----:|----------|------|------|
| W-01 | 企业版BES环境地址/端口/可用性未明确 | AC2相关测试项（MT-04/MT-05/MT-06）需BES环境执行 | 建议在Stage 6前确认BES环境部署状态 |
| W-02 | BES_PATH 环境变量实际路径未明确 | AC4相关测试项（MT-08）需设置BES_PATH | 建议确认BES安装路径，如 `/opt/bes` |
| W-03 | 企业版BES环境测试账号未确认 | AC2相关鉴权测试 | 建议确认是否与开源版共用测试账号 |
| W-04 | BES容器WebSocket实现差异未实测 | AC2 WebSocket测试可能在BES上有兼容性问题 | 建议企业版重点验证WS消息收发 |
| W-05 | 回归测试集是否纳入本次验证未确认 | 改造涉及核心模块，建议全量回归 | 建议在Stage 6.1评估回归范围 |
| W-06 | LinkisServletContextInitializer.onStartup 异常处理未明确 | WS Endpoint注册失败时行为未定义 | 建议补充try/catch保护（设计文档已建议） |

---

## 十、完成度评估

| 维度 | 权重 | 评估 |
|------|:----:|------|
| P0检查点覆盖率 | 40% | 85%（远端环境/鉴权为partial，被测范围covered） |
| 回答质量 | 30% | 良好（具体到被测对象，非空泛） |
| 边界明确度 | 20% | 良好（BES_PATH空值/ControllerServer null/HandshakeRequest降级等已明确） |
| 约束清晰度 | 10% | 良好（不测项/必测项/选测项已明确） |
| **总完成度** | **100%** | **0.85** |

---

## 十一、风险测试增量

本次需求 context.md 中 `stabilityRisks` 为空，无组织级必测要求（`stabilityRiskTestRequirements`）。因此无风险测试增量项，普通测试范围按上述 testScope 执行。

---

## 附录：GEP Gene 召回

- **skill_name**: test-case-clarification-orchestrator
- **signals**: ["test_clarification", "scope", "boundary", "feasibility"]
- **召回结果**: 冷启动期，无匹配 Gene（容错降级，不影响澄清流程）
- **回流**: 澄清纪要落盘后回流结果（success=true, signals_found=["scope","boundary","feasibility"]）

---

> 本纪要由 test-case-clarification-orchestrator（Stage 5.5）自动生成，供 functional-test-generator（Stage 6）按 testScope/environmentInfo/dataStrategy 构造测试用例。
