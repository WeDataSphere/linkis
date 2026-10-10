# BES 容器适配改造 测试用例

**需求编号**: REQ-04
**需求名称**: XC改造，排除jetty和tomcat包（BES容器适配改造）
**需求类型**: REFACTOR（架构级改造）
**风险等级**: 高风险（涉及 Web 容器/嵌入式服务器主链路）
**功能属性**: 后端（不涉及前端）
**关联 commit**: `050b7aa55`
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**测试澄清纪要**: [REQ-04_BES容器适配改造_测试澄清.md](./REQ-04_BES容器适配改造_测试澄清.md)
**生成日期**: 2026-10-08

---

## 一、测试概述

### 1.1 测试范围

本测试用例文档基于 Stage 5.5 测试澄清纪要的 `testScope` 生成，覆盖以下范围：

| 范围类型 | 数量 | 说明 |
|----------|:----:|------|
| 必测项（mustTest） | 18 | 覆盖 AC1-AC6 全部验收标准，P0 用例 12 个，P1 用例 6 个 |
| 选测项（optionalTest） | 5 | 边界场景与回归验证，非阻塞 |
| 不测项（notTest） | 3 | Maven 构建解析、BES 中间件本身、Spring Boot 自动配置 |
| **总计** | **23** | 必测 18 + 选测 5 |

### 1.2 验收标准覆盖

| AC编号 | 验收标准 | 对应必测项 | 覆盖状态 |
|:------:|----------|:-----------|:--------:|
| AC1 | 开源版（保留Jetty）行为一致：REST API + WebSocket 正常 | MT-01, MT-02, MT-03, MT-12, MT-13, MT-18 | 100% |
| AC2 | 企业版（排除Jetty + 引入BES）正常启动：REST + WS 正常 | MT-04, MT-05, MT-06, MT-12, MT-13, MT-18 | 100% |
| AC3 | 中文响应无乱码：SecurityFilter.filterResponse 中文正常 | MT-07 | 100% |
| AC4 | EC classpath 含 BES 路径：BES_PATH 设置后 classpath 含路径 | MT-08, MT-11 | 100% |
| AC5 | BDPJettyServerHelper 公开 API 兼容 | MT-09, MT-17 | 100% |
| AC6 | TestContextHAManager 编译通过 | MT-10 | 100% |

### 1.3 被测对象清单

| 编号 | 被测对象 | 文件 | 修改类型 |
|:----:|----------|------|:--------:|
| TU-01 | BDPJettyServerHelper facade | BDPJettyServerHelper.scala | 修改(-177行) |
| TU-02 | BDPServerHelper | BDPServerHelper.scala | 新增(+104行) |
| TU-03 | JettyServerCustomizerConfiguration | JettyServerCustomizerConfiguration.java | 新增(+141行) |
| TU-04 | LinkisServletContextInitializer | LinkisServletContextInitializer.scala | 新增(+119行) |
| TU-05 | LinkisWebSocketEndpoint | LinkisWebSocketEndpoint.scala | 新增(+331行) |
| TU-06 | HttpRequestCaptureConfigurator | HttpRequestCaptureConfigurator.scala | 新增(+45行) |
| TU-07 | ControllerServer 适配 | ControllerServer.scala | 修改(15行) |
| TU-08 | ServerSocket 适配 | ServerSocket.scala | 修改(17行) |
| TU-09 | SecurityFilter 编码修复 | SecurityFilter.scala | 修改(7行) |
| TU-10 | EnvConfiguration BES_PATH | EnvConfiguration.scala | 修改(6行) |
| TU-11 | JavaProcessEngineConnLaunchBuilder | JavaProcessEngineConnLaunchBuilder.scala | 修改(5行) |
| TU-12 | DataWorkCloudApplication | DataWorkCloudApplication.java | 修改(-44行) |
| TU-13 | TestContextHAManager | TestContextHAManager.java | 修改 |
| TU-14 | linkis-module pom.xml | pom.xml | 修改(26行) |

### 1.4 测试环境信息

| 环境类型 | 地址 | 可用性 | VPN要求 |
|----------|------|--------|---------|
| 开源版测试环境（保留Jetty） | `dev-2.1.4-webank` 分支构建部署 | 已部署（Stage 5 完成） | 否 |
| 企业版测试环境（排除Jetty+引入BES） | DSS-DVS（5902，bes） | 需确认BES环境是否已就绪 | 需确认 |
| 引擎测试环境（EC classpath BES） | 需设置 `BES_PATH` 环境变量 | 需确认 | 需确认 |

### 1.5 鉴权信息

| 维度 | 说明 |
|------|------|
| 登录方式 | Linkis 标准认证：Token + Cookie（SecurityFilter.getLoginUser） |
| token获取 | Linkis Gateway 统一认证接口，测试账号通过 Gateway 登录获取 token |
| 测试账号来源 | 开源版：hadoop 基础测试账号；企业版：需确认 |
| WebSocket鉴权 | 通过 HandshakeRequest 捕获 Cookie/Header，适配为 HttpServletRequest 调用 SecurityFilter.getLoginUser |

### 1.6 前置数据策略

本需求为架构级改造（REFACTOR），不涉及业务数据变更，测试数据主要服务于功能验证：

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|----------|------|----------|
| REST API 测试请求 | 直接调用 Linkis REST API（如 /api/rest_j/v1/userinfo） | 服务已启动 + 测试账号登录 | 无需清理（只读操作） |
| WebSocket 测试消息 | 通过 WS Client 发送标准 Linkis WS 消息 | WS 连接已建立 + ControllerServer 已初始化 | 连接关闭后自动清理 |
| SecurityFilter 中文响应触发 | 发送无认证/过期 token 请求触发 filterResponse | 服务已启动 | 无需清理 |
| EC 启动测试数据 | 提交简单 SQL 任务（如 `SELECT 1`）触发 EC 启动 | Linkis 服务正常 + 引擎可用 | 任务完成后自动清理 |
| BES_PATH 环境变量 | 设置 `export BES_PATH=/opt/bes` | BES 安装路径存在 | 测试后 unset |

---

## 二、必测项测试用例（mustTest）

---

### TC-001：开源版（保留Jetty）服务启动 + REST API 响应验证

**来源**：必测项 MT-01 | AC1 | P0

**测试类型**：功能测试

**被测对象**：TU-12 DataWorkCloudApplication, TU-03 JettyServerCustomizerConfiguration, TU-04 LinkisServletContextInitializer

**前置条件**：
- 不排除 Jetty 依赖，标准 `dev-2.1.4-webank` 分支构建
- 测试账号已通过 Gateway 登录获取 token
- 服务端口可用

**测试步骤**：
1. 执行标准构建 `mvn clean package`（开源版配置）
2. 启动 linkis-mg-gateway 服务
3. 启动 ps-publicservice 服务
4. 使用 HTTP 客户端发送 GET 请求到 `/api/rest_j/v1/userinfo`，Header 携带测试 token Cookie
5. 检查 HTTP 响应状态码和响应体
6. 发送 POST 请求到 `/api/rest_j/v1/entrance/operation/info`，验证业务接口可用

**预期结果**：
- DataWorkCloudApplication 移除 Jetty 专属 main 逻辑后启动正常，无启动异常
- JettyServerCustomizerConfiguration 加载（Jetty 在 classpath，`@ConditionalOnClass` 条件满足）
- REST API 返回 HTTP 200，响应体为标准 JSON 格式
- 响应体包含 `data` 字段和 `status` 字段
- 行为与改造前完全一致

**测试数据**：
```
GET /api/rest_j/v1/userinfo
Cookie: linkis_user_session_ticket_id={test_token}
```

**优先级**：P0

**覆盖场景**：正向场景 - 开源版服务启动 + REST API 响应

**关联验收标准**：AC1

---

### TC-002：开源版（保留Jetty）WebSocket 连接 + 消息收发验证

**来源**：必测项 MT-02 | AC1 | P0

**测试类型**：功能测试

**被测对象**：TU-04 LinkisServletContextInitializer, TU-05 LinkisWebSocketEndpoint, TU-06 HttpRequestCaptureConfigurator

**前置条件**：
- 开源版服务已启动（TC-001 通过）
- 测试账号已登录获取 token
- WebSocket 端点路径为 `/ws`

**测试步骤**：
1. 使用 WebSocket 客户端连接 `ws://{host}:{port}/ws`，握手请求携带认证 Cookie
2. 验证连接建立成功（onOpen 触发）
3. 发送标准 Linkis WS 消息（如请求引擎状态：`{"method":"/engine/list","data":{}}`）
4. 等待服务端响应消息
5. 验证响应消息内容正确
6. 主动关闭 WebSocket 连接

**预期结果**：
- WebSocket 连接成功建立
- LinkisWebSocketEndpoint.onOpen 正确从 userProperties 获取 ControllerServer
- HttpRequestCaptureConfigurator 在握手期捕获 HandshakeRequest，存入 userProperties
- 消息发送后服务端正确响应
- 连接关闭后 onClose 被触发，Session 清理完成
- 行为与改造前一致

**测试数据**：
```json
{
  "method": "/engine/list",
  "data": {}
}
```

**优先级**：P0

**覆盖场景**：正向场景 - 开源版 WebSocket 消息收发

**关联验收标准**：AC1

---

### TC-003：@ConditionalOnClass 条件加载验证 - Jetty在classpath时加载

**来源**：必测项 MT-03 | AC1 | P0

**测试类型**：功能测试

**被测对象**：TU-03 JettyServerCustomizerConfiguration

**前置条件**：
- 开源版构建（保留 Jetty 依赖）
- 服务启动后可通过 Spring ApplicationContext 检查 Bean 加载状态

**测试步骤**：
1. 构建开源版（不排除 Jetty）
2. 启动服务
3. 通过 Spring Actuator 或日志检查 JettyServerCustomizerConfiguration 是否被加载
4. 检查 `jettyFactoryCustomizer` Bean 是否存在
5. 检查 Jetty 专属 WebAppContext/FilterHolder/ServletHolder 配置是否生效
6. 验证日志中无 `@ConditionalOnClass` 条件跳过记录

**预期结果**：
- JettyServerCustomizerConfiguration 类被 Spring 扫描并加载
- `jettyFactoryCustomizer` Bean 正常创建
- Jetty 专属配置（WebAppContext、CharacterEncodingFilter、DefaultServlet、MultipartConfigElement）全部生效
- 嵌入式 Jetty 容器正常初始化
- 日志中无条件跳过记录

**优先级**：P0

**覆盖场景**：正向场景 - 条件加载验证

**关联验收标准**：AC1

---

### TC-004：企业版（排除Jetty+引入BES）服务启动 + REST API 响应验证

**来源**：必测项 MT-04 | AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-12 DataWorkCloudApplication, TU-03 JettyServerCustomizerConfiguration, TU-04 LinkisServletContextInitializer

**前置条件**：
- 企业版 pom 排除 `spring-boot-starter-jetty` + `jetty-*` 依赖
- 引入 BES starter
- BES 环境（DSS-DVS, 5902, bes）可用
- 测试账号已登录

**测试步骤**：
1. 执行企业版构建（排除 Jetty + 引入 BES）
2. 启动 linkis-mg-gateway 服务
3. 检查启动日志中 BES 嵌入式容器是否接管
4. 发送 GET 请求到 `/api/rest_j/v1/userinfo`，携带测试 token
5. 检查 HTTP 响应状态码和响应体
6. 发送 POST 请求验证业务接口

**预期结果**：
- JettyServerCustomizerConfiguration 不加载（`@ConditionalOnClass` 条件不满足，Jetty 不在 classpath）
- BES 自动接管嵌入式容器（Spring Boot 自动配置选择 BES）
- LinkisServletContextInitializer 加载，通过 BES 的 ServerContainer 注册 WS
- REST API 正常响应 HTTP 200
- 响应体为标准 JSON 格式

**测试数据**：
```
GET /api/rest_j/v1/userinfo
Cookie: linkis_user_session_ticket_id={test_token}
```

**优先级**：P0

**覆盖场景**：正向场景 - 企业版 BES 启动 + REST API

**关联验收标准**：AC2

**环境缺口**：企业版BES环境地址/端口/可用性需确认（W-01）

---

### TC-005：企业版（排除Jetty+引入BES）WebSocket 连接 + 消息收发验证

**来源**：必测项 MT-05 | AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-04 LinkisServletContextInitializer, TU-05 LinkisWebSocketEndpoint, TU-06 HttpRequestCaptureConfigurator

**前置条件**：
- 企业版 BES 服务已启动（TC-004 通过）
- 测试账号已登录获取 token
- BES ServerContainer 可用

**测试步骤**：
1. 使用 WebSocket 客户端连接企业版 BES 服务 `ws://{bes_host}:{bes_port}/ws`
2. 握手请求携带认证 Cookie
3. 验证连接建立成功
4. 发送标准 Linkis WS 消息
5. 等待服务端响应
6. 验证响应消息内容正确
7. 关闭连接

**预期结果**：
- BES ServerContainer 成功注册 LinkisWebSocketEndpoint
- WebSocket 连接成功建立
- onOpen 从 userProperties 获取 ControllerServer（通过 BES 的 JSR-356 实现）
- HttpRequestCaptureConfigurator 在 BES 握手期捕获 HandshakeRequest
- 消息发送后服务端正确响应
- 连接关闭后 onClose 被触发

**测试数据**：
```json
{
  "method": "/engine/list",
  "data": {}
}
```

**优先级**：P0

**覆盖场景**：正向场景 - 企业版 BES WebSocket 消息收发

**关联验收标准**：AC2

**环境缺口**：BES 容器 WebSocket 实现差异未实测（W-04）

---

### TC-006：@ConditionalOnClass 条件加载验证 - Jetty不在classpath时不加载

**来源**：必测项 MT-06 | AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-03 JettyServerCustomizerConfiguration

**前置条件**：
- 企业版构建（排除 Jetty + 引入 BES）
- 服务启动后可通过 Spring ApplicationContext 检查 Bean 加载状态

**测试步骤**：
1. 构建企业版（排除 Jetty 依赖）
2. 启动服务
3. 检查 JettyServerCustomizerConfiguration 是否被加载（应为不加载）
4. 检查 `jettyFactoryCustomizer` Bean 是否存在（应不存在）
5. 检查日志中是否有条件跳过记录
6. 验证 NoClassDefFoundError 未发生（方法签名不含 Jetty 类型）

**预期结果**：
- JettyServerCustomizerConfiguration 类不被加载
- `jettyFactoryCustomizer` Bean 不存在
- 日志中记录 `@ConditionalOnClass` 条件不满足
- 无 `NoClassDefFoundError` 异常（Jetty 专属类型只在方法体内引用，方法签名用标准类型）
- BES 自动接管

**优先级**：P0

**覆盖场景**：正向场景 - 条件加载验证（反向）

**关联验收标准**：AC2

---

### TC-007：SecurityFilter.filterResponse 中文响应无乱码

**来源**：必测项 MT-07 | AC3 | P0

**测试类型**：功能测试

**被测对象**：TU-09 SecurityFilter

**前置条件**：
- 服务已启动（开源版或企业版均可）
- 可发送无认证/过期 token 请求触发 filterResponse

**测试步骤**：
1. 发送不带认证 Cookie 的 GET 请求到 `/api/rest_j/v1/userinfo`
2. 触发 SecurityFilter.filterResponse 返回认证失败消息
3. 检查 HTTP 响应头 `Content-Type` 是否为 `application/json;charset=UTF-8`
4. 检查响应体中文内容是否正常显示（无乱码）
5. 发送过期 token 的请求，重复验证
6. 对比改造前（getOutputStream.print）和改造后（getWriter.write）的输出差异

**预期结果**：
- 响应头 `Content-Type: application/json;charset=UTF-8`
- 响应体中文消息（如"用户未登录"或"认证失败"）正常显示，无 ISO-8859-1 乱码
- `response.getWriter.write(json)` 正确遵循 response charset（UTF-8）
- 不再出现 `getOutputStream.print` 导致的 ISO-8859-1 编码乱码

**测试数据**：
```
GET /api/rest_j/v1/userinfo
（不携带 Cookie 或携带过期 token）
```

**预期响应示例**：
```json
{
  "status": -1,
  "message": "用户未登录，请先登录",
  "data": null
}
```

**优先级**：P0

**覆盖场景**：正向场景 - 中文编码验证

**关联验收标准**：AC3

---

### TC-008：EC classpath 追加 BES_PATH 路径验证（BES_PATH 非空时）

**来源**：必测项 MT-08 | AC4 | P0

**测试类型**：功能测试

**被测对象**：TU-10 EnvConfiguration, TU-11 JavaProcessEngineConnLaunchBuilder

**前置条件**：
- Linkis 服务正常启动
- 引擎可用
- 设置环境变量 `export BES_PATH=/opt/bes`（或实际路径）

**测试步骤**：
1. 在 Linkis 服务器设置 `BES_PATH=/opt/bes` 环境变量
2. 提交一个简单 SQL 任务（如 `SELECT 1`）触发 EC 启动
3. 等待引擎进程启动
4. 检查 EC Java 进程的启动参数中的 classpath
5. 验证 classpath 包含 `/opt/bes/*`
6. 验证引擎进程能加载 BES 相关类（如 `Class.forName("com.bes.BESServer")`）

**预期结果**：
- `EnvConfiguration.ENGINE_CONN_BES_PATH` 正确读取 `BES_PATH` 环境变量
- `JavaProcessEngineConnLaunchBuilder.buildEngineConnClassPath` 在 public module path 后追加 `$BES_PATH/*`
- EC 进程 classpath 包含 `/opt/bes/*`
- 引擎进程可加载 BES 类（无 NoClassDefFoundError）

**测试数据**：
```sql
SELECT 1
```

**优先级**：P0

**覆盖场景**：正向场景 - EC classpath BES 路径追加

**关联验收标准**：AC4

**环境缺口**：BES_PATH 实际路径需确认（W-02）

---

### TC-009：BDPJettyServerHelper API 兼容验证

**来源**：必测项 MT-09 | AC5 | P0

**测试类型**：功能测试

**被测对象**：TU-01 BDPJettyServerHelper, TU-02 BDPServerHelper

**前置条件**：
- 服务已启动
- BDPJettyServerHelper facade 可访问

**测试步骤**：
1. 调用 `BDPJettyServerHelper.gson`，验证返回 Gson 实例
2. 调用 `BDPJettyServerHelper.jacksonJson`，验证返回 Jackson ObjectMapper 实例
3. 调用 `BDPJettyServerHelper.getControllerServer`，验证返回 ControllerServer 实例
4. 调用 `BDPJettyServerHelper.addServerEventService(service)`，验证服务注册成功
5. 验证上述调用均委托给 BDPServerHelper，无 NPE
6. 验证 79+ 调用方代码无需改动

**预期结果**：
- `gson` 返回有效的 Gson 实例（非 null）
- `jacksonJson` 返回有效的 ObjectMapper 实例（非 null）
- `getControllerServer` 返回有效的 ControllerServer 实例（非 null）
- `addServerEventService` 正常执行，无异常
- 所有调用委托给 BDPServerHelper，行为与改造前一致
- 无 NPE 或 NoClassDefFoundError

**优先级**：P0

**覆盖场景**：正向场景 - API 兼容验证

**关联验收标准**：AC5

---

### TC-010：TestContextHAManager 编译通过

**来源**：必测项 MT-10 | AC6 | P1

**测试类型**：功能测试

**被测对象**：TU-13 TestContextHAManager

**前置条件**：
- 项目源码可编译
- Maven 构建环境正常

**测试步骤**：
1. 执行 `mvn compile -pl linkis-commons/linkis-module -am`
2. 检查 TestContextHAManager.java 是否编译通过
3. 验证移除的 `setupSpringRestApiContextHandler` / `setupControllerServer` / `setupWebAppContext` 调用不再存在
4. 检查注释说明"Jetty context/controller/webapp setup is now auto-configured by JettyServerCustomizerConfiguration"是否已添加
5. 执行 `mvn test-compile -pl linkis-commons/linkis-module -am` 验证测试编译

**预期结果**：
- TestContextHAManager.java 编译通过，无编译错误
- 不再依赖已删除的 `setupSpringRestApiContextHandler` / `setupControllerServer` / `setupWebAppContext` 方法
- 注释说明已添加
- 测试编译通过

**优先级**：P1

**覆盖场景**：正向场景 - 编译验证

**关联验收标准**：AC6

---

### TC-011：BES_PATH 未设置时 EC classpath 不追加，行为不变

**来源**：必测项 MT-11 | AC4 | P1

**测试类型**：功能测试

**被测对象**：TU-10 EnvConfiguration, TU-11 JavaProcessEngineConnLaunchBuilder

**前置条件**：
- Linkis 服务正常启动
- 引擎可用
- 不设置 `BES_PATH` 环境变量（或设置为空）

**测试步骤**：
1. 确认 `BES_PATH` 环境变量未设置（`echo $BES_PATH` 返回空）
2. 提交一个简单 SQL 任务触发 EC 启动
3. 等待引擎进程启动
4. 检查 EC Java 进程的 classpath
5. 验证 classpath 不包含 BES 路径
6. 验证引擎进程正常工作（无因缺失 BES 类导致的错误）

**预期结果**：
- `EnvConfiguration.ENGINE_CONN_BES_PATH.getValue` 返回空字符串或 null
- `StringUtils.isNotBlank(besPath)` 返回 false
- classpath 不追加 BES 路径
- EC 行为与改造前完全一致
- 引擎进程正常启动和工作

**测试数据**：
```sql
SELECT 1
```

**优先级**：P1

**覆盖场景**：边界场景 - BES_PATH 为空时不追加

**关联验收标准**：AC4

---

### TC-012：LinkisServletContextInitializer REST DispatcherServlet + SecurityFilter 注册正确性

**来源**：必测项 MT-12 | AC1, AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-04 LinkisServletContextInitializer

**前置条件**：
- 服务已启动（开源版或企业版）
- 可通过日志和功能验证注册正确性

**测试步骤**：
1. 检查 LinkisServletContextInitializer.onStartup 执行日志
2. 验证 REST DispatcherServlet 正确注册到 ServletContext
3. 验证 SecurityFilter 正确注册
4. 发送 REST API 请求验证 DispatcherServlet 正常路由
5. 发送无认证请求验证 SecurityFilter 拦截生效
6. 验证 HttpRequestCaptureConfigurator 正确注册到 ServerContainer

**预期结果**：
- LinkisServletContextInitializer.onStartup 正常执行
- REST DispatcherServlet 正确注册，REST API 请求正常路由
- SecurityFilter 正确注册，无认证请求被拦截
- HttpRequestCaptureConfigurator 正确注册到 ServerContainer
- 容器无关的注册逻辑在 Jetty 和 BES 下均正常

**优先级**：P0

**覆盖场景**：正向场景 - ServletContext 初始化

**关联验收标准**：AC1, AC2

---

### TC-013：LinkisWebSocketEndpoint onOpen/onMessage/onClose/onError 生命周期

**来源**：必测项 MT-13 | AC1, AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-05 LinkisWebSocketEndpoint

**前置条件**：
- 服务已启动
- WebSocket 端点已注册

**测试步骤**：
1. 使用 WebSocket 客户端连接 `/ws` 端点
2. 验证 onOpen 被触发：从 userProperties 获取 ControllerServer，适配 HandshakeRequest 为 HttpServletRequest 调用 SecurityFilter.getLoginUser
3. 发送一条文本消息
4. 验证 onMessage 被触发：委托 ServerSocket.handleMessage 处理消息
5. 验证服务端返回响应消息
6. 模拟异常场景（发送非法格式消息），验证 onError 被触发
7. 主动关闭连接
8. 验证 onClose 被触发：Session 清理完成

**预期结果**：
- onOpen：从 userProperties 正确获取 ControllerServer（非 null 时）；HandshakeRequest 适配为 HttpServletRequest；SecurityFilter.getLoginUser 正确获取登录用户
- onMessage：消息委托 ServerSocket.handleMessage 处理，服务端返回正确响应
- onError：异常被捕获并记录日志，Session 状态正确
- onClose：Session 清理完成，无资源泄漏
- 完整生命周期覆盖

**测试数据**：
```json
{
  "method": "/engine/list",
  "data": {}
}
```

**优先级**：P0

**覆盖场景**：正向场景 + 异常场景 - WS Endpoint 生命周期

**关联验收标准**：AC1, AC2

---

### TC-014：HttpRequestCaptureConfigurator 握手期 HandshakeRequest 捕获

**来源**：必测项 MT-14 | AC1, AC2 | P1

**测试类型**：功能测试

**被测对象**：TU-06 HttpRequestCaptureConfigurator

**前置条件**：
- 服务已启动
- WebSocket 端点已注册

**测试步骤**：
1. 使用 WebSocket 客户端连接 `/ws` 端点，握手请求携带认证 Cookie
2. 验证 HttpRequestCaptureConfigurator 在握手期被调用
3. 验证 HandshakeRequest 被捕获并存入 userProperties
4. 在 onOpen 中验证可从 config.getUserProperties() 获取 handshakeRequest
5. 验证 HandshakeRequest 适配为 HttpServletRequest 调用 SecurityFilter.getLoginUser 成功
6. 验证未携带认证信息的连接被正确处理（获取 loginUser 为 null 时的降级行为）

**预期结果**：
- HttpRequestCaptureConfigurator 在握手期正确执行
- HandshakeRequest 被捕获并存入 userProperties
- onOpen 可从 userProperties 获取 HandshakeRequest
- HandshakeRequest 适配为 HttpServletRequest，SecurityFilter.getLoginUser 正确获取登录用户
- 认证信息缺失时降级处理正常

**优先级**：P1

**覆盖场景**：正向场景 - 握手期请求捕获

**关联验收标准**：AC1, AC2

---

### TC-015：ControllerServer 去除 Jetty WS 依赖后标准 Session 交互

**来源**：必测项 MT-15 | AC1, AC2 | P1

**测试类型**：功能测试

**被测对象**：TU-07 ControllerServer

**前置条件**：
- 服务已启动
- WebSocket 连接已建立

**测试步骤**：
1. 建立 WebSocket 连接
2. 验证 ControllerServer 不再依赖 Jetty WebSocketServlet
3. 验证 ControllerServer 使用标准 JSR-356 Session 交互
4. 发送消息验证 ControllerServer 正确处理
5. 验证 ControllerServer 通过 javax.websocket.Session 发送响应
6. 验证多消息连续收发正常

**预期结果**：
- ControllerServer 不引用任何 Jetty WebSocket 类
- 使用 javax.websocket.Session 标准 API 进行消息收发
- 消息处理逻辑正常
- 响应消息通过标准 Session 发送
- 连续消息收发正常

**优先级**：P1

**覆盖场景**：正向场景 - ControllerServer 标准化

**关联验收标准**：AC1, AC2

---

### TC-016：ServerSocket 改用 javax.websocket.Session 标准 API

**来源**：必测项 MT-16 | AC1, AC2 | P1

**测试类型**：功能测试

**被测对象**：TU-08 ServerSocket

**前置条件**：
- 服务已启动
- WebSocket 连接已建立

**测试步骤**：
1. 建立 WebSocket 连接
2. 验证 ServerSocket 使用 javax.websocket.Session 标准 API
3. 验证 ServerSocket 不引用 Jetty WebSocket 类
4. 发送消息验证 ServerSocket.handleMessage 正确处理
5. 验证响应消息通过标准 Session API 发送
6. 验证 Session 关闭后 ServerSocket 清理正常

**预期结果**：
- ServerSocket 使用 javax.websocket.Session 标准 API
- 不引用 Jetty WebSocket 任何类
- handleMessage 正确处理入站消息
- 响应消息通过标准 Session.getBasicRemote().sendText() 或类似 API 发送
- Session 关闭后资源清理正常

**优先级**：P1

**覆盖场景**：正向场景 - ServerSocket 标准化

**关联验收标准**：AC1, AC2

---

### TC-017：BDPServerHelper controllerServer/EventBus/services 状态管理

**来源**：必测项 MT-17 | AC5 | P1

**测试类型**：功能测试

**被测对象**：TU-02 BDPServerHelper

**前置条件**：
- 服务已启动
- BDPServerHelper 可访问

**测试步骤**：
1. 验证 BDPServerHelper.controllerServer 初始化正确
2. 验证 BDPServerHelper.serverListenerEventBus 初始化正确
3. 验证 BDPServerHelper.services 初始化正确
4. 调用 `BDPServerHelper.getControllerServer`，验证返回 ControllerServer 实例
5. 调用 `BDPServerHelper.addServerEventService(service)`，验证服务正确注册到 services
6. 验证 `BDPServerHelper.getSecurityFilterClass` 返回正确
7. 验证 `BDPServerHelper.getMultipartConfigElement` 返回正确（MAX_UPLOAD_SIZE=200MB）

**预期结果**：
- controllerServer 初始化为有效实例（非 null）
- serverListenerEventBus 初始化为有效实例（非 null）
- services 初始化为有效集合
- getControllerServer 返回 ControllerServer 实例
- addServerEventService 正确注册服务
- getSecurityFilterClass 返回 SecurityFilter 类
- getMultipartConfigElement 返回正确的配置（MAX_UPLOAD_SIZE=200MB）

**优先级**：P1

**覆盖场景**：正向场景 - 状态管理验证

**关联验收标准**：AC5

---

### TC-018：DataWorkCloudApplication 移除 Jetty 专属 main 逻辑后启动正常

**来源**：必测项 MT-18 | AC1, AC2 | P0

**测试类型**：功能测试

**被测对象**：TU-12 DataWorkCloudApplication

**前置条件**：
- 开源版和企业版构建均可执行

**测试步骤**：
1. 验证 DataWorkCloudApplication.java 中移除了 Jetty 专属 main 逻辑
2. 验证移除了 `jettyFactoryCustomizer` Bean 的硬编码
3. 验证移除了 `BDPJettyServerHelper.setupSpringRestApiContextHandler` 等调用
4. 启动开源版服务，验证正常启动
5. 启动企业版服务（BES），验证正常启动
6. 检查启动日志中无 Jetty 相关的硬编码初始化

**预期结果**：
- DataWorkCloudApplication 不包含 Jetty 专属 main 逻辑
- 不包含 `jettyFactoryCustomizer` Bean 硬编码
- 不包含 `BDPJettyServerHelper.setupSpringRestApiContextHandler` 调用
- 开源版服务正常启动（Jetty 通过 JettyServerCustomizerConfiguration 自动配置）
- 企业版服务正常启动（BES 通过 Spring Boot 自动配置）
- 启动日志中无 Jetty 硬编码初始化

**优先级**：P0

**覆盖场景**：正向场景 - 启动逻辑验证

**关联验收标准**：AC1, AC2

---

## 三、选测项测试用例（optionalTest）

---

### TC-019：BES 容器 WebSocket 大消息传输（接近 maxTextMessageSize）

**来源**：选测项 OT-01 | 选测

**测试类型**：功能测试

**被测对象**：TU-05 LinkisWebSocketEndpoint, TU-08 ServerSocket

**前置条件**：
- 企业版 BES 服务已启动
- WebSocket 连接已建立

**测试步骤**：
1. 建立 WebSocket 连接到 BES 服务
2. 构造接近 maxTextMessageSize 的大消息（如 64KB JSON）
3. 发送大消息到服务端
4. 验证服务端正确接收和处理
5. 验证服务端返回的响应消息正确
6. 构造超过 maxTextMessageSize 的消息发送
7. 验证服务端正确处理异常（onError 触发，连接不崩溃）

**预期结果**：
- 接近 maxTextMessageSize 的大消息正确接收和处理
- 超过 maxTextMessageSize 时触发异常处理
- BES WS 实现对大消息的兼容性正常
- 连接在大消息后仍可用（或按预期关闭）

**测试数据**：
```json
{
  "method": "/engine/list",
  "data": { "large_field": "<64KB string>" }
}
```

**优先级**：P2

**覆盖场景**：边界场景 - 大消息传输

**标注**：选测

---

### TC-020：WebSocket 并发连接（多客户端同时连接）

**来源**：选测项 OT-02 | 选测

**测试类型**：功能测试

**被测对象**：TU-05 LinkisWebSocketEndpoint, TU-07 ControllerServer

**前置条件**：
- 服务已启动
- 多个测试客户端可用

**测试步骤**：
1. 使用 5 个 WebSocket 客户端同时连接 `/ws` 端点
2. 验证所有连接均建立成功（onOpen 触发）
3. 每个客户端发送不同消息
4. 验证每个客户端收到正确的响应
5. 验证 ControllerServer 线程安全（无并发异常）
6. 逐个关闭连接，验证每个 onClose 正确触发

**预期结果**：
- 5 个并发连接全部建立成功
- 每个客户端收到正确的响应消息
- ControllerServer 线程安全，无并发异常
- 连接关闭时每个 Session 正确清理
- 无资源泄漏

**优先级**：P2

**覆盖场景**：边界场景 - 并发连接

**标注**：选测

---

### TC-021：WebSocket 断线重连

**来源**：选测项 OT-03 | 选测

**测试类型**：功能测试

**被测对象**：TU-05 LinkisWebSocketEndpoint

**前置条件**：
- 服务已启动
- WebSocket 连接已建立

**测试步骤**：
1. 建立 WebSocket 连接
2. 模拟网络中断（强制断开客户端连接）
3. 验证 onClose 被触发，Session 清理完成
4. 等待一段时间后重新建立连接
5. 验证重连后消息收发正常
6. 验证无资源泄漏（无残留 Session）

**预期结果**：
- 断线时 onClose 正确触发
- Session 清理完成，无资源泄漏
- 重连后消息收发正常
- ControllerServer 状态正确

**优先级**：P2

**覆盖场景**：边界场景 - 断线重连

**标注**：选测

---

### TC-022：SecurityFilter 非 UTF-8 字符集响应

**来源**：选测项 OT-04 | 选测

**测试类型**：功能测试

**被测对象**：TU-09 SecurityFilter

**前置条件**：
- 服务已启动
- 可发送请求触发 filterResponse

**测试步骤**：
1. 发送请求，Accept-Charset 设置为 `GBK`
2. 触发 SecurityFilter.filterResponse
3. 检查响应头 Content-Type 和 charset
4. 检查响应体中文内容编码
5. 发送请求，Accept-Charset 设置为 `ISO-8859-1`
6. 重复验证响应编码

**预期结果**：
- CharacterEncodingFilter 强制 UTF-8，无论 Accept-Charset 如何设置
- 响应头 Content-Type 始终为 `application/json;charset=UTF-8`
- 中文内容始终以 UTF-8 编码输出
- 非 UTF-8 字符集请求不影响响应编码

**优先级**：P3

**覆盖场景**：边界场景 - 非 UTF-8 字符集

**标注**：选测

---

### TC-023：回归测试集联动（现有测试用例全量回归）

**来源**：选测项 OT-05 | 选测

**测试类型**：功能测试

**被测对象**：全部被测对象

**前置条件**：
- 现有回归测试集可用
- 服务已启动

**测试步骤**：
1. 执行现有 Linkis 全量回归测试用例
2. 检查所有测试用例通过率
3. 对比改造前后的测试结果
4. 识别新增的失败用例（如有）
5. 分析失败原因是否与本次改造相关

**预期结果**：
- 现有回归测试用例通过率与改造前一致
- 无因本次改造引入的新失败用例
- 改造未引入回归缺陷

**优先级**：P2

**覆盖场景**：回归场景 - 全量回归

**标注**：选测

---

## 四、验收标准覆盖率统计

### 4.1 AC 覆盖详情

| AC编号 | 验收标准 | 对应测试用例 | 覆盖状态 |
|:------:|----------|:-----------|:--------:|
| AC1 | 开源版（保留Jetty）行为一致 | TC-001, TC-002, TC-003, TC-012, TC-013, TC-018 | 100% |
| AC2 | 企业版（排除Jetty+引入BES）正常启动 | TC-004, TC-005, TC-006, TC-012, TC-013, TC-018 | 100% |
| AC3 | 中文响应无乱码 | TC-007, TC-022 | 100% |
| AC4 | EC classpath 含 BES 路径 | TC-008, TC-011 | 100% |
| AC5 | BDPJettyServerHelper API 兼容 | TC-009, TC-017 | 100% |
| AC6 | TestContextHAManager 编译通过 | TC-010 | 100% |

### 4.2 必测项覆盖详情

| 必测项编号 | 必测项名称 | 对应测试用例 | 覆盖状态 |
|:---------:|-----------|:-----------|:--------:|
| MT-01 | 开源版服务启动 + REST API | TC-001 | 100% |
| MT-02 | 开源版 WebSocket 连接 + 消息收发 | TC-002 | 100% |
| MT-03 | @ConditionalOnClass Jetty在classpath时加载 | TC-003 | 100% |
| MT-04 | 企业版服务启动 + REST API | TC-004 | 100% |
| MT-05 | 企业版 WebSocket 连接 + 消息收发 | TC-005 | 100% |
| MT-06 | @ConditionalOnClass Jetty不在classpath时不加载 | TC-006 | 100% |
| MT-07 | SecurityFilter 中文无乱码 | TC-007 | 100% |
| MT-08 | EC classpath 追加 BES_PATH | TC-008 | 100% |
| MT-09 | BDPJettyServerHelper API 兼容 | TC-009 | 100% |
| MT-10 | TestContextHAManager 编译通过 | TC-010 | 100% |
| MT-11 | BES_PATH 未设置时不追加 | TC-011 | 100% |
| MT-12 | LinkisServletContextInitializer 注册正确性 | TC-012 | 100% |
| MT-13 | LinkisWebSocketEndpoint 生命周期 | TC-013 | 100% |
| MT-14 | HttpRequestCaptureConfigurator 握手捕获 | TC-014 | 100% |
| MT-15 | ControllerServer 标准 Session 交互 | TC-015 | 100% |
| MT-16 | ServerSocket 标准 API | TC-016 | 100% |
| MT-17 | BDPServerHelper 状态管理 | TC-017 | 100% |
| MT-18 | DataWorkCloudApplication 启动正常 | TC-018 | 100% |

### 4.3 选测项覆盖详情

| 选测项编号 | 选测项名称 | 对应测试用例 | 覆盖状态 |
|:---------:|-----------|:-----------|:--------:|
| OT-01 | BES 大消息传输 | TC-019 | 100% |
| OT-02 | WebSocket 并发连接 | TC-020 | 100% |
| OT-03 | WebSocket 断线重连 | TC-021 | 100% |
| OT-04 | SecurityFilter 非 UTF-8 字符集 | TC-022 | 100% |
| OT-05 | 回归测试集联动 | TC-023 | 100% |

---

## 五、测试用例统计

### 5.1 按优先级统计

| 优先级 | 必测项 | 选测项 | 总计 |
|:------:|:------:|:------:|:----:|
| P0 | 12 | 0 | 12 |
| P1 | 6 | 0 | 6 |
| P2 | 0 | 3 | 3 |
| P3 | 0 | 1 | 1 |
| **总计** | **18** | **5** | **23** |

### 5.2 按验收标准统计

| AC编号 | 测试用例数 | 优先级分布 |
|:------:|:---------:|:---------|
| AC1 | 6 | P0:6 |
| AC2 | 6 | P0:6 |
| AC3 | 2 | P0:1, P3:1 |
| AC4 | 2 | P0:1, P1:1 |
| AC5 | 2 | P0:1, P1:1 |
| AC6 | 1 | P1:1 |

### 5.3 按测试类型统计

| 测试类型 | 数量 |
|----------|:----:|
| 功能测试 | 23 |
| 接口测试 | 0 |
| 性能测试 | 0 |
| 安全测试 | 0 |
| **总计** | **23** |

### 5.4 按覆盖场景统计

| 场景类型 | 数量 |
|----------|:----:|
| 正向场景 | 16 |
| 边界场景 | 4 |
| 异常场景 | 1 |
| 回归场景 | 1 |
| 编译验证 | 1 |
| **总计** | **23** |

---

## 六、质量警告与待补充项

| 编号 | 待补充项 | 影响 | 建议 |
|:----:|----------|------|------|
| W-01 | 企业版BES环境地址/端口/可用性未明确 | TC-004, TC-005, TC-006 需 BES 环境执行 | 建议在测试执行前确认 BES 环境部署状态 |
| W-02 | BES_PATH 环境变量实际路径未明确 | TC-008 需设置 BES_PATH | 建议确认 BES 安装路径，如 `/opt/bes` |
| W-03 | 企业版BES环境测试账号未确认 | TC-004, TC-005 鉴权测试 | 建议确认是否与开源版共用测试账号 |
| W-04 | BES容器WebSocket实现差异未实测 | TC-005 可能有兼容性问题 | 建议企业版重点验证 WS 消息收发 |
| W-05 | 回归测试集是否纳入本次验证未确认 | TC-023 全量回归 | 建议在测试执行前评估回归范围 |
| W-06 | LinkisServletContextInitializer.onStartup 异常处理未明确 | TC-012 WS Endpoint 注册失败时行为 | 建议补充 try/catch 保护 |

---

## 七、不测项说明

| 编号 | 不测项 | 原因 |
|:----:|--------|------|
| NT-01 | linkis-module pom.xml jetty-websocket 子依赖的 Maven 解析 | 属于构建验证，非功能测试 |
| NT-02 | BES 中间件本身的 Servlet/WS 实现正确性 | BES 为第三方产品，非本项目被测对象 |
| NT-03 | Spring Boot 自动配置机制本身 | Spring Boot 框架保证，非本项目代码 |

---

> 本测试用例文档由 functional-test-generator（Stage 6）基于 Stage 5.5 测试澄清纪要的 testScope 自动生成。
