# BES 容器适配改造 设计文档

**需求类型**: REFACTOR（架构级改造）
**风险等级**: 高风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端
**关联需求**: [REQ-04_BES容器适配改造_需求.md](../requirements/REQ-04_BES容器适配改造_需求.md)
**关联 commit**: `050b7aa55`

---

## 0. 执行摘要

| 维度 | 内容 |
|------|------|
| **改造目标** | 解除 Jetty 硬耦合，支持 BES 等国产化 servlet 容器替换 |
| **核心机制** | ① `@ConditionalOnClass(JettyServletWebServerFactory.class)` 隔离 Jetty 专属逻辑；② JSR-356 `javax.websocket.Endpoint` + `ServerContainer` 标准化 WS；③ `BDPJettyServerHelper` facade 保留公开 API |
| **关键风险** | 🔴 Web 容器主链路 + EC classpath 改造，但 `@ConditionalOnClass` 保障开源版零影响 |
| **向后兼容** | 🟢 不排除 Jetty 时行为逐字节一致，79+ 调用方零改动 |

---

## 1. 设计背景

详见需求文档 §1。核心矛盾：原 `BDPJettyServerHelper` 170 行硬编码 Jetty API（`WebAppContext`/`FilterHolder`/`ServletHolder`/`SessionHandler`），企业版排除 Jetty 后 `NoClassDefFoundError`。

---

## 2. 设计方案

### 2.1 整体架构

**改造前**：
```
DataWorkCloudApplication.main()
  └─ BDPJettyServerHelper（170 行，硬编码 Jetty）
       ├─ setupSpringRestApiContextHandler(webApp)   ← Jetty API
       ├─ setupControllerServer(webApp)              ← Jetty WS API
       └─ setupWebAppContext(webApp)                 ← Jetty API

SecurityFilter.filterResponse
  └─ response.getOutputStream.print(message)        ← ISO-8859-1 乱码
```

**改造后**：
```
Spring Boot 自动配置
  ├─ @ConditionalOnClass(JettyServletWebServerFactory)
  │     └─ JettyServerCustomizerConfiguration        【新增】
  │          └─ jettyFactoryCustomizer Bean
  │               └─ JettyServerCustomizer（Jetty 专属 context/controller/webapp setup）
  │
  └─ LinkisServletContextInitializer                【新增】
       └─ ServerContainer.addEndpoint(LinkisWebSocketEndpoint)  ← JSR-356 标准

LinkisWebSocketEndpoint                             【新增】
  ├─ onOpen: 从 userProperties 取 ControllerServer + HandshakeRequest
  ├─ onMessage: 委托 ServerSocket.handleMessage
  ├─ onClose/onError: 清理 Session

BDPJettyServerHelper（33 行 facade）                【瘦身】
  └─ 委托 BDPServerHelper（gson/jacksonJson/controllerServer/EventBus）

SecurityFilter.filterResponse
  └─ response.getWriter.write(json)                ← 修复编码

JavaProcessEngineConnLaunchBuilder
  └─ classpath 追加 $BES_PATH/* （非空时）
```

### 2.2 关键设计决策

#### Q1: 为什么 `@ConditionalOnClass` 而不是 Profile？

| 方案 | 切换成本 | 自动性 | 选择 |
|------|:--------:|:------:|:----:|
| `@Profile` | 需显式配置 `spring.profiles.active=bes` | 低 | ✗ |
| `@ConditionalOnClass` | classpath 自动生效 | 高 | ✅ |

排除 Jetty 依赖 → `JettyServletWebServerFactory` 不在 classpath → 条件不满足 → 不加载。零配置。

#### Q2: 为什么 Jetty 专属类型只在方法体内引用？

**问题**：Spring 在 bean 定义处理期调用 `getDeclaredMethods()` 扫描方法签名。若方法签名含 Jetty 类型（如 `JettyServletWebServerFactory`）且 classpath 缺失，抛 `NoClassDefFoundError`——此时 `@ConditionalOnClass` 条件**尚未求值**。

**解决**：
```java
@ConditionalOnClass(JettyServletWebServerFactory.class)
public class JettyServerCustomizerConfiguration {

  // ✅ 正确：返回类型用 Spring 标准类型，Jetty 类型在匿名内部类方法体内
  @Bean
  public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyFactoryCustomizer() {
    return new WebServerFactoryCustomizer<JettyServletWebServerFactory>() {
      @Override
      public void customize(JettyServletWebServerFactory factory) {
        factory.addServerCustomizers(new JettyServerCustomizer() { ... });  // Jetty 类型在此
      }
    };
  }

  // ✗ 错误：方法签名直接含 Jetty 类型会触发 NoClassDefFoundError
  // public void customize(JettyServletWebServerFactory factory) { ... }
}
```

#### Q3: 为什么用 JSR-356 而不是 Spring WebSocket？

| 方案 | 兼容性 | 依赖 | 选择 |
|------|:------:|:----:|:----:|
| JSR-356 `javax.websocket` | Jetty/Tomcat/BES 均支持 | `javax.websocket-api`（已有） | ✅ |
| Spring WebSocket | 需 `spring-websocket`，BES 兼容性未验证 | 额外依赖 | ✗ |

#### Q4: 为什么 `BDPJettyServerHelper` 不直接删除？

79+ 跨模块调用 `BDPJettyServerHelper.gson` / `jacksonJson` / `getControllerServer` / `addServerEventService`。删除引发大规模改动。保留 facade 委托 `BDPServerHelper`：

```scala
private[linkis] object BDPJettyServerHelper extends Logging {
  def gson = BDPServerHelper.gson
  def jacksonJson = BDPServerHelper.jacksonJson
  def getControllerServer = BDPServerHelper.getControllerServer
  def addServerEventService(service: ServerEventService): Unit = BDPServerHelper.addServerEventService(service)
  // ... setupSpringRestApiContextHandler / setupControllerServer / setupWebAppContext 已移除
}
```

### 2.3 模块设计

#### 2.3.1 JettyServerCustomizerConfiguration（新增，Java）

```java
@Configuration
@ConditionalOnClass(JettyServletWebServerFactory.class)
public class JettyServerCustomizerConfiguration {

  @Bean
  public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyFactoryCustomizer() {
    // 内部：addServerCustomizers → JettyServerCustomizer
    //   ├─ 配置 WebAppContext
    //   ├─ 注册 CharacterEncodingFilter
    //   ├─ 注册 DefaultServlet
    //   ├─ 配置 MultipartConfigElement
    //   └─ （原 BDPJettyServerHelper 的 Jetty 专属逻辑全部迁入）
  }
}
```

#### 2.3.2 LinkisServletContextInitializer（新增，Scala）

```scala
class LinkisServletContextInitializer extends ServletContextInitializer {
  override def onStartup(ctx: ServletContext): Unit = {
    // 1. 注册 HttpRequestCaptureConfigurator（握手期捕获 HandshakeRequest）
    // 2. ServerContainer container = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer")
    // 3. container.addEndpoint(new EndpointConfig() { ... }, "/ws")  ← 注册 LinkisWebSocketEndpoint
  }
}
```

#### 2.3.3 LinkisWebSocketEndpoint（新增，Scala，JSR-356）

```scala
class LinkisWebSocketEndpoint extends Endpoint with Logging {
  override def onOpen(session: Session, config: EndpointConfig): Unit = {
    // 1. controllerServer = config.getUserProperties.get("controllerServer")
    // 2. handshakeRequest = config.getUserProperties.get("handshakeRequest")
    // 3. 适配为 HttpServletRequest → SecurityFilter.getLoginUser
    // 4. socket = new ServerSocket(controllerServer, session)
    // 5. session.addMessageHandler(new MessageHandler.Whole[String] { onMessage(s) → socket.handleMessage(s) })
  }
  override def onClose(session: Session, reason: CloseReason): Unit = { ... }
  override def onError(session: Session, t: Throwable): Unit = { ... }
}
```

#### 2.3.4 HttpRequestCaptureConfigurator（新增）

JSR-356 握手期 `HandshakeRequest` 默认不可在 `Endpoint.onOpen` 获取。通过 `ServerContainerInitializer` 子类在握手期把 `HandshakeRequest` 存入 `userProperties`，供 `Endpoint` 读取。

#### 2.3.5 SecurityFilter 编码修复

```scala
// 改造前（ISO-8859-1 乱码）
response.getOutputStream.print(message)
response.getOutputStream.flush()

// 改造后（遵循 response charset）
val json: String = message
response.getWriter.write(json)
response.getWriter.flush()
```

`getOutputStream.print` 按 Servlet 规范使用 ISO-8859-1，中文乱码；`getWriter` 遵循 `response.setCharacterEncoding`（已设 UTF-8）。

#### 2.3.6 EC classpath BES 路径

```scala
// JavaProcessEngineConnLaunchBuilder.buildEngineConnClassPath
if (!enablePublicModule) {
  addPathToClassPath(environment, buildPath(Seq(LINKIS_PUBLIC_MODULE_PATH.getValue + "/*")))
}
// 【新增】BES 路径（非空时追加）
val besPath = EnvConfiguration.ENGINE_CONN_BES_PATH.getValue
if (StringUtils.isNotBlank(besPath)) {
  addPathToClassPath(environment, besPath + "/*")
}
```

`EnvConfiguration.ENGINE_CONN_BES_PATH` 从环境变量 `BES_PATH` 读取。

---

## 3. 关键代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `BDPJettyServerHelper.scala` | 全文件（170→33 行） | facade 委托 BDPServerHelper |
| `BDPServerHelper.scala` | 新增 | controllerServer/EventBus/services 状态管理 |
| `JettyServerCustomizerConfiguration.java` | 新增 141 行 | `@ConditionalOnClass` Jetty 专属配置 |
| `LinkisServletContextInitializer.scala` | 新增 119 行 | JSR-356 WS Endpoint 注册 |
| `LinkisWebSocketEndpoint.scala` | 新增 331 行 | JSR-356 Endpoint 实现 |
| `HttpRequestCaptureConfigurator.scala` | 新增 45 行 | 握手期 HttpRequest 捕获 |
| `ControllerServer.scala` | `:15` 附近 | 去除 Jetty WS 依赖 |
| `ServerSocket.scala` | `:17` 附近 | 改用 javax.websocket.Session |
| `SecurityFilter.scala` | `:51-57` | filterResponse 改 getWriter |
| `EnvConfiguration.scala` | 末尾 | 新增 ENGINE_CONN_BES_PATH |
| `JavaProcessEngineConnLaunchBuilder.scala` | `:130-135` | classpath 追加 BES_PATH |
| `DataWorkCloudApplication.java` | 全文件 | 移除 Jetty 专属 main 逻辑 |
| `TestContextHAManager.java` | `:28-31, :177-186` | 移除 Jetty 专属调用 |

---

## 4. 异常与降级

| 场景 | 处理 |
|------|------|
| 排除 Jetty 后 `JettyServerCustomizerConfiguration` 不加载 | ✅ `@ConditionalOnClass` 保障，BES 自动接管 |
| `BES_PATH` 未设置 | `StringUtils.isNotBlank` 判空，不追加 classpath，EC 行为不变 |
| WS Endpoint 注册失败 | `LinkisServletContextInitializer.onStartup` 需 try/catch 保护（建议补充） |
| `HandshakeRequest` 未捕获 | `HttpRequestCaptureConfigurator` 必须先于 Endpoint 注册 |

---

## 5. 兼容性与影响

| 维度 | 影响 |
|------|------|
| 公开 API | 🟢 `BDPJettyServerHelper` facade 保留，79+ 调用方零改动 |
| 依赖 | 🟡 linkis-module pom 新增 jetty websocket 子依赖（开源版可见） |
| 部署 | 🟡 企业版需排除 Jetty + 引入 BES starter |
| 协议 | 🟢 无 protocol 改动 |
| EC classpath | 🟡 `BES_PATH` 非空时追加，默认不变 |
| 字符编码 | 🟢 `SecurityFilter` 中文响应修复（顺带收益） |

---

## 6. 修改文件清单

详见需求文档附录 A.1，共 14 个文件（4 新增 + 10 修改）。

---

## 7. 测试要点

1. **AC1 开源版行为一致**：保留 Jetty，REST + WS 正常；
2. **AC2 企业版 BES 启动**：排除 Jetty + 引入 BES，REST + WS 正常；
3. **AC3 中文无乱码**：`SecurityFilter.filterResponse` 响应体中文正常；
4. **AC4 EC classpath BES**：`BES_PATH` 设置后 classpath 含路径；
5. **AC5 BDPJettyServerHelper API 兼容**：79+ 调用方零改动；
6. **AC6 TestContextHAManager 编译**：移除 Jetty 专属调用后编译通过。

---

## 8. 高危区域对照（CLAUDE.md §10.2）

| # | 环节 | 触及 | 说明 |
|---|------|:----:|------|
| #10 | 引擎启动脚本组装（JavaProcessEngineConnLaunchBuilder） | ✅ | classpath 追加 BES_PATH，仅在非空时生效 |

按 CLAUDE.md §10.3 要求：① 改动有条件加载（`BES_PATH` 默认空），默认行为不变；② spark-2.4 + spark-3 双 profile 需 build 验证（classpath 逻辑与 profile 无关，但建议双跑）。
