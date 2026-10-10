# BES 容器适配改造 需求文档

**需求类型**: REFACTOR（架构级改造）
**风险等级**: 高风险（涉及 Web 容器/嵌入式服务器主链路）
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端（不涉及前端）
**关联 commit**: `050b7aa55`

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | 解除 Linkis 服务对 Jetty 的硬耦合，通过 Spring Boot `@ConditionalOnClass` + JSR-356 标准 WebSocket API，使企业版排除 Jetty 后可切换到 BES（宝信中间件）嵌入式容器运行 |
| **改动服务** | linkis-commons/linkis-module（核心）+ linkis-engineconn-plugin-core（classpath 增 BES 路径）+ 测试类适配 |
| **关键改造** | ① `BDPJettyServerHelper` 瘦身为 facade，Jetty 专属逻辑迁至 `JettyServerCustomizerConfiguration`（`@ConditionalOnClass`）；② 新增 `LinkisServletContextInitializer` + `LinkisWebSocketEndpoint`（JSR-356 标准 WS）；③ `SecurityFilter.filterResponse` 改用 `getWriter` 修复字符编码；④ EC classpath 增加 BES_PATH |
| **开关方式** | 通过 Maven `<exclusion>` 排除 Jetty 依赖 + classpath 是否存在 `JettyServletWebServerFactory` 自动生效（`@ConditionalOnClass`） |
| **降级回退** | 不排除 Jetty 时，所有 `@ConditionalOnClass` Bean 加载，行为与改造前完全一致 |

---

## 1. 需求背景

### 1.1 业务背景

企业版生产环境要求使用 BES（宝信中间件，国产化替代 Tomcat/Jetty 的 servlet 容器）作为嵌入式 Web 服务器。原 Linkis 在 `linkis-commons/linkis-module` 中**硬编码依赖 Jetty**：
- `BDPJettyServerHelper` 直接 import `org.eclipse.jetty.*`，调用 `WebAppContext`/`FilterHolder`/`ServletHolder` 等 Jetty 专属 API；
- WebSocket 通过 Jetty 的 `ControllerServer` 直接绑定 Jetty `ServletContextHandler`；
- 测试类 `TestContextHAManager` 直接调用 `BDPJettyServerHelper.setupSpringRestApiContextHandler` 等 Jetty 专属方法。

这导致企业版排除 Jetty 依赖后：① Jetty 类不在 classpath → 编译/运行 `NoClassDefFoundError`；② Spring Boot 默认走 Jetty 自动配置，无法切换 BES；③ WebSocket 无法注册。

### 1.2 目标

1. **Jetty 逻辑条件化**：所有 Jetty 专属代码用 `@ConditionalOnClass(JettyServletWebServerFactory.class)` 隔离，企业版排除 Jetty 时不加载；
2. **BES 自动接管**：企业版排除 Jetty + 引入 BES starter 后，Spring Boot 自动选择 BES 嵌入式容器（标准 `WebServerFactoryCustomizer` 机制）；
3. **WebSocket 容器无关**：使用 JSR-356 标准 `javax.websocket.Endpoint` + `ServerContainer` API，替换 Jetty 专属 WS 实现，兼容 Jetty/Tomcat/BES；
4. **EC classpath 支持 BES**：`JavaProcessEngineConnLaunchBuilder` 在 classpath 中增加 `BES_PATH` 环境变量指向的路径；
5. **字符编码修复**：`SecurityFilter.filterResponse` 原用 `getOutputStream.print` 按 ISO-8859-1 输出中文乱码，改用 `getWriter.write` 遵循 response charset；
6. **向后兼容**：不排除 Jetty 时，改造前后行为完全一致（79+ 调用 `BDPJettyServerHelper` 的地方零改动）。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**企业版部署**，我希望排除 Jetty 依赖 + 引入 BES starter 后，Linkis 服务能正常启动并对外提供 REST/WebSocket 服务 |
| US-2 | 作为**开源版部署**，我希望不排除 Jetty 时，行为与改造前完全一致，无需改动配置 |
| US-3 | 作为**开发**，希望 `BDPJettyServerHelper` 的公开 API（gson/jacksonJson/getControllerServer/addServerEventService）保持不变，避免 79+ 调用方改动 |
| US-4 | 作为**引擎侧**，希望 EC 启动时 classpath 能自动包含 BES_PATH 路径，使引擎进程内也能使用 BES |

---

## 3. 功能描述

### 3.1 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | BDPJettyServerHelper 瘦身 | 原 170 行含 Jetty 专属 setup 逻辑瘦身为 33 行 facade，保留 `gson`/`jacksonJson`/`getControllerServer`/`addServerEventService` 委托给 `BDPServerHelper`，79+ 调用方零改动 |
| F-2 | BDPServerHelper 新增 | 新建 `BDPServerHelper.scala` 承载 `controllerServer`/`serverListenerEventBus`/`services` 状态 + `getControllerServer` + `addServerEventService` + `getSecurityFilterClass` + `getMultipartConfigElement` |
| F-3 | JettyServerCustomizerConfiguration | 新建 Java 配置类，`@Configuration` + `@ConditionalOnClass(JettyServletWebServerFactory.class)`，包含 `jettyFactoryCustomizer` Bean，内部做 Jetty 专属 context/controller/webapp setup |
| F-4 | LinkisServletContextInitializer | 新建 JSR-356 标准 `ServletContextInitializer`，通过 `ServerContainer.addEndpoint` 注册 `LinkisWebSocketEndpoint`，并注册 `HttpRequestCaptureConfigurator` 捕获握手期 HttpRequest |
| F-5 | LinkisWebSocketEndpoint | 新建 JSR-356 `javax.websocket.Endpoint` 实现，`onOpen`/`onClose`/`onError`/`onMessage` 全部适配，`ControllerServer` 从 userProperties 获取 |
| F-6 | HttpRequestCaptureConfigurator | 新建 `ServerContainerInitializer` 子类，在握手期把 `HandshakeRequest` 存入 userProperties，供 Endpoint 适配为 `HttpServletRequest` 调用 `SecurityFilter.getLoginUser` |
| F-7 | ControllerServer 适配 | `ControllerServer` 去除对 Jetty `WebSocketServlet` 的依赖，改为标准 JSR-356 Session 交互 |
| F-8 | ServerSocket 适配 | `ServerSocket` 改用 `javax.websocket.Session` 标准 API |
| F-9 | SecurityFilter 编码修复 | `filterResponse` 改用 `response.getWriter.write(json)` 替代 `getOutputStream.print(message)`，修复 ISO-8859-1 中文乱码 |
| F-10 | EC classpath BES 路径 | `JavaProcessEngineConnLaunchBuilder.buildEngineConnClassPath` 在 public module path 后增加 `EnvConfiguration.ENGINE_CONN_BES_PATH` 指向的路径（`BES_PATH` 环境变量） |
| F-11 | DataWorkCloudApplication 调整 | 移除原 Jetty 专属 main 启动逻辑中硬编码的 `BDPJettyServerHelper` 调用 |
| F-12 | 测试类适配 | `TestContextHAManager` 移除对 `BDPJettyServerHelper.setupSpringRestApiContextHandler/setupControllerServer/setupWebAppContext` 的调用，改为注释说明"Jetty context/controller/webapp setup is now auto-configured by JettyServerCustomizerConfiguration" |

### 3.2 关键设计决策

| 编号 | 问题 | 决策 |
|:----:|------|------|
| Q1 | 为什么用 `@ConditionalOnClass` 而不是 Profile？ | Profile 需显式配置 `spring.profiles.active`，切换成本高；`@ConditionalOnClass` 根据 classpath 自动生效，企业版排除 Jetty 依赖即自动关闭，零配置 |
| Q2 | 为什么 Jetty 专属类型只在方法体内引用？ | Spring 在 bean 定义处理期会调用 `getDeclaredMethods()`，若方法签名含 Jetty 类型且 classpath 缺失，会抛 `NoClassDefFoundError`——`@ConditionalOnClass` 在此之前未求值。把 Jetty 类型限定在匿名内部类/方法体内，方法签名只含 Spring/Servlet 标准类型，避免此问题 |
| Q3 | 为什么用 JSR-356 而不是 Spring WebSocket？ | JSR-356 是标准 API，Jetty/Tomcat/BES 均支持；Spring WebSocket 需额外依赖且对 BES 兼容性未验证 |
| Q4 | 为什么 `BDPJettyServerHelper` 不直接删除？ | 79+ 调用方跨模块，删除会引发大规模改动；保留 facade 委托给 `BDPServerHelper`，向后兼容 |

---

## 4. 验收标准

### AC1: 开源版（保留 Jetty）行为一致

**前置条件**: 不排除 Jetty 依赖，标准 `dev-2.1.4-webank` 分支构建

**操作步骤**:
1. 启动 linkis-mg-gateway / ps-publicservice / cs-server 等服务
2. 验证 REST API + WebSocket 正常

**预期结果**:
- `JettyServerCustomizerConfiguration` 加载（Jetty 在 classpath）
- `LinkisServletContextInitializer` 加载，注册 WS Endpoint
- REST API 正常响应
- WebSocket 正常连接、消息收发
- 行为与改造前完全一致

### AC2: 企业版（排除 Jetty + 引入 BES）正常启动

**前置条件**: 企业版 pom 排除 `spring-boot-starter-jetty` + `jetty-*` 依赖，引入 BES starter

**操作步骤**:
1. 构建企业版
2. 启动服务

**预期结果**:
- `JettyServerCustomizerConfiguration` 不加载（`@ConditionalOnClass` 不满足）
- BES 自动接管嵌入式容器（Spring Boot 自动配置）
- `LinkisServletContextInitializer` 加载，通过 BES 的 `ServerContainer` 注册 WS
- REST API 正常
- WebSocket 正常

### AC3: 中文响应无乱码

**操作条件**: 触发 `SecurityFilter.filterResponse`（如认证失败返回 Message）

**预期结果**:
- 响应体中文正常显示
- `Content-Type: application/json;charset=UTF-8`
- 不再出现 ISO-8859-1 乱码

### AC4: EC classpath 含 BES 路径

**前置条件**: 设置 `BES_PATH=/opt/bes` 环境变量

**操作步骤**:
1. 提交任务，触发 EC 启动
2. 检查 EC 进程 classpath

**预期结果**:
- classpath 含 `/opt/bes/*`
- 引擎进程可加载 BES 类

### AC5: BDPJettyServerHelper 公开 API 兼容

**操作步骤**: 调用 `BDPJettyServerHelper.gson` / `jacksonJson` / `getControllerServer` / `addServerEventService`

**预期结果**:
- 全部正常返回/执行，无 NPE
- 等价于改造前行为

### AC6: TestContextHAManager 编译通过

**预期结果**: 测试类移除 Jetty 专属调用后编译通过，不再依赖已删除的 `setupSpringRestApiContextHandler` 等方法

---

## 5. 配置说明

### 5.1 新增配置项

| 配置Key | 默认值 | 说明 |
|---------|--------|------|
| `BES_PATH`（环境变量） | 无 | `EnvConfiguration.ENGINE_CONN_BES_PATH` 读取，EC classpath 追加 `$BES_PATH/*`，未设置则不追加 |

### 5.2 Maven 排除 Jetty 示例（企业版）

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jetty</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<!-- 排除所有 jetty 依赖 -->
```

引入 BES starter 后，Spring Boot 自动选择 BES。

---

## 6. 风险识别

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| Web 容器主链路改造 | 🔴 | 按 CLAUDE.md §4 铁律：`@ConditionalOnClass` 条件加载，不排除 Jetty 时行为不变；企业版需全量冒烟 REST/WS |
| `@ConditionalOnClass` 与方法签名陷阱 | 🔴 | Jetty 类型只在方法体内引用，方法签名只用标准类型，避免 `getDeclaredMethods` 期 `NoClassDefFoundError`（已在代码注释明确） |
| WebSocket 兼容性 | 🟡 | JSR-356 标准支持 Jetty/Tomcat/BES，但 BES 实现差异需实测；建议企业版重点验证 WS 消息收发 |
| 79+ 调用方兼容 | 🟢 | `BDPJettyServerHelper` facade 保留公开 API，委托 `BDPServerHelper`，调用方零改动 |
| EC classpath 变更 | 🟡 | `BES_PATH` 未设置时不追加，行为不变；设置后需验证引擎进程能正确加载 BES 类 |

---

## 7. 关联影响分析

### 7.1 高危区域对照（CLAUDE.md §10.2）

| # | 环节 | 是否触及 | 说明 |
|---|------|:--------:|------|
| #10 | 引擎启动脚本组装（JavaProcessEngineConnLaunchBuilder） | ✅ | classpath 追加 BES_PATH，但仅在 `BES_PATH` 非空时生效，默认行为不变 |

### 7.2 影响清单

| 维度 | 影响 |
|------|------|
| 功能模块 | 🔴 linkis-module（核心）+ engineconn-plugin-core（classpath）+ 测试类 |
| API | 🟢 `BDPJettyServerHelper` 公开 API 兼容 |
| 依赖 | 🟡 linkis-module pom 新增 jetty websocket 子依赖（开源版） |
| 部署 | 🟡 企业版需排除 Jetty + 引入 BES starter |
| 兼容性 | 🟢 开源版零改动 |

---

## 8. 发布和回滚方案

### 8.1 发布策略

1. 开源版先行：不排除 Jetty，`@ConditionalOnClass` 全部生效，行为不变；
2. 企业版灰度：排除 Jetty + 引入 BES，重点验证 REST + WS；
3. 联调：EC 启动 + `BES_PATH` classpath 验证。

### 8.2 回滚方案

- 不排除 Jetty 即回退开源版行为，无需代码回滚；
- `BES_PATH` 不设置即不追加 classpath，EC 行为不变。

---

## 附录

### A.1 修改文件清单

| 序号 | 文件 | 修改类型 | 说明 |
|:----:|------|:--------:|------|
| 1 | `linkis-module/pom.xml` | 修改 | 新增 jetty websocket 子依赖 |
| 2 | `DataWorkCloudApplication.java` | 修改 | 移除 Jetty 专属 main 逻辑 |
| 3 | `BDPJettyServerHelper.scala` | 修改 | 瘦身为 facade，委托 BDPServerHelper |
| 4 | `BDPServerHelper.scala` | 新增 | 承载 controllerServer/EventBus/services 状态 |
| 5 | `conf/JettyServerCustomizerConfiguration.java` | 新增 | `@ConditionalOnClass` Jetty 专属配置 |
| 6 | `conf/LinkisServletContextInitializer.scala` | 新增 | JSR-356 WS Endpoint 注册 |
| 7 | `security/SecurityFilter.scala` | 修改 | filterResponse 改 getWriter 修复编码 |
| 8 | `socket/ControllerServer.scala` | 修改 | 去除 Jetty WS 依赖，改标准 API |
| 9 | `socket/HttpRequestCaptureConfigurator.scala` | 新增 | 握手期 HttpRequest 捕获 |
| 10 | `socket/LinkisWebSocketEndpoint.scala` | 新增 | JSR-356 Endpoint 实现 |
| 11 | `socket/ServerSocket.scala` | 修改 | 改用 javax.websocket.Session |
| 12 | `EnvConfiguration.scala` | 修改 | 新增 ENGINE_CONN_BES_PATH |
| 13 | `JavaProcessEngineConnLaunchBuilder.scala` | 修改 | classpath 追加 BES_PATH |
| 14 | `TestContextHAManager.java` | 修改 | 移除 Jetty 专属调用 |

### A.2 关联文档

- 设计文档: [REQ-04_BES容器适配改造_设计.md](../design/REQ-04_BES容器适配改造_设计.md)
- CLAUDE.md §4（铁律）、§9.5（地雷区）、§10.2 #10（引擎启动脚本组装）
