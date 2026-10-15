# Linkis SSO 单点登录技术分析报告

## 📋 概述

Linkis **支持 SSO 单点登录**，但采用**扩展接口模式**而非开箱即用。系统提供了完整的 SSO 集成框架，用户需要根据具体 SSO 协议（CAS、OAuth2.0、SAML等）进行定制开发。

---

## 🏗️ 架构设计

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                     前端 (Vue.js)                             │
│  api.js: 检测 enableSSO + SSOURL，自动跳转                    │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP Request
                       ↓
┌─────────────────────────────────────────────────────────────┐
│              Linkis Gateway (网关层)                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  SecurityFilter (安全过滤器)                          │    │
│  │  - 多种认证方式路由                                  │    │
│  │  - Token 认证 → TokenAuthentication                 │    │
│  │  - SSO 认证 → SSOInterceptor                         │    │
│  │  - 用户名密码 → UserRestful                          │    │
│  └─────────────────────────────────────────────────────┘    │
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  SSOInterceptor (SSO拦截器接口)                      │    │
│  │  getUser()      - 从SSO响应解析用户                 │    │
│  │  redirectTo()   - 构建SSO登录URL                     │    │
│  │  logout()       - SSO登出处理                       │    │
│  └─────────────────────────────────────────────────────┘    │
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  GatewaySSOUtils (会话管理)                           │    │
│  │  - Cookie 管理 (userTicketId)                       │    │
│  │  - Session 超时控制                                 │    │
│  │  - ProxyUser 支持                                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                       ↓
            [用户实现的 SSO 协议适配器]
            - CAS 认证
            - OAuth2.0 认证  
            - SAML 认证
            - LDAP 认证
            - 企业自定义认证
```

---

## 🔧 支持的认证方式

### 1. Token 认证 ✅

**配置参数**：
```properties
# 启用 Token 认证
wds.linkis.gateway.conf.enable.token.auth=true

# Token 超时时间（可选）
wds.linkis.gateway.auth.token.alive.enable=false
```

**HTTP Header 认证**：
```http
Token-Code: your_token_value
Token-User: username
Token-Alive: true  # 可选，保持token活跃
```

**Cookie 认证**：
```javascript
// 设置 Cookie
document.cookie = "Token-Code=your_token_value; path=/";
document.cookie = "Token-User=username; path=/";
```

**实现类**：[TokenAuthentication.scala:49](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/token/TokenAuthentication.scala)

---

### 2. SSO 单点登录 ⚠️ 需扩展

**核心配置**：
```properties
# 启用 SSO 登录
wds.linkis.gateway.conf.enable.sso=true

# SSO 拦截器实现类（必须）
wds.linkis.gateway.conf.sso.interceptor=com.yourCompany.YourSSOInterceptor

# Cookie 域名设置（跨域 SSO 需要）
wds.linkis.gateway.cookie.domain.setup.switch=false
wds.linkis.gateway.domain.level=3
```

**认证流程**：

```
┌─────────┐   ① 未登录请求        ┌─────────┐
│  浏览器  │ ──────────────────→ │ Gateway │
└─────────┘                      └─────────┘
    ↑                                 │
    │  ④ ② 返回 enableSSO=true       │
    │     + SSOURL                   │
    │                                 ↓
    │                           ┌─────────┐
    │  ③ 跳转到 SSO 登录页         │SSOInterceptor│
    └────────────────────────← ──────────┘
                                    ↓
                              ⑤ getUser() 尝试获取用户
                              ⑥ 失败 → redirectTo() 返回登录URL
```

**响应格式**：
```json
{
  "status": -1,
  "message": "您尚未登录，请先登录!",
  "data": {
    "enableSSO": true,
    "SSOURL": "https://sso.company.com/login?callback=http://linkis.company.com/api/login"
  }
}
```

**前端处理**：[api.js:112](../linkis-web/src/common/service/api.js)
```javascript
// 自动跳转到 SSO 登录页面
if (res.data && res.data.enableSSO && res.data.SSOURL) {
  return window.location.replace(res.data.SSOURL);
}
```

---

### 3. 用户名密码认证 ✅

**配置参数**：
```properties
# 启用密码加密传输
wds.linkis.login_encrypt.enable=true

# 管理员账号
wds.linkis.admin.user=hadoop
wds.linkis.admin.password=
```

**登录接口**：
```
POST /api/user/login
Content-Type: application/json

{
  "userName": "hadoop",
  "password": "encrypted_password",
  "passwdEncrypt": "base64_rsa_encrypted_password"
}
```

**实现类**：[UserPwdAbstractUserRestful](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala)

---

## 💻 SSO 扩展开发指南

### Step 1: 实现 SSOInterceptor 接口

**接口定义**：[SSOInterceptor.scala:27](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/sso/SSOInterceptor.scala)

```scala
trait SSOInterceptor {
  /**
   * 从 SSO 响应中获取用户信息
   * @param gatewayContext 网关上下文
   * @return 用户名（如果已登录），null 或空字符串（未登录）
   */
  def getUser(gatewayContext: GatewayContext): String

  /**
   * 构建 SSO 登录页面 URL
   * @param requestUrl 原始请求URL（用于登录后跳转）
   * @return SSO 登录页面完整 URL
   */
  def redirectTo(requestUrl: URI): String

  /**
   * 用户退出登录时调用
   * @param gatewayContext 网关上下文
   */
  def logout(gatewayContext: GatewayContext): Unit
}
```

### Step 2: 实现示例 - CAS 认证

```scala
package com.yourCompany.linkis.sso

import org.apache.linkis.gateway.http.GatewayContext
import org.apache.linkis.gateway.security.sso.SSOInterceptor
import java.net.URI
import org.jasig.cas.client.validation.{Cas20ServiceTicketValidator, TicketValidationException}
import org.jasig.cas.client.authentication.AttributePrincipalImpl

class CasSSOInterceptor extends SSOInterceptor {

  private val CAS_SERVER_URL = "https://cas.yourcompany.com/cas"
  private val SERVICE_URL = "http://linkis.yourcompany.com"

  private val ticketValidator = new Cas20ServiceTicketValidator(CAS_SERVER_URL)

  override def getUser(gatewayContext: GatewayContext): String = {
    // 1. 从请求中获取 CAS ticket
    val ticket = Option(gatewayContext.getRequest.getQueryParams.get("ticket"))
      .map(_.head)
      .orNull

    if (ticket == null) {
      return null  // 没有 ticket，说明未登录
    }

    // 2. 验证 ticket
    try {
      val assertion = ticketValidator.validate(ticket, SERVICE_URL)
      val principal = assertion.getPrincipal
      principal.getName  // 返回用户名
    } catch {
      case e: TicketValidationException =>
        // ticket 无效
        null
    }
  }

  override def redirectTo(requestUrl: URI): String = {
    val callbackUrl = s"$SERVICE_URL${requestUrl.getPath}"
    s"$CAS_SERVER_URL/login?service=$callbackUrl"
  }

  override def logout(gatewayContext: GatewayContext): Unit = {
    // 调用 CAS 的 logout 接口
    val logoutUrl = s"$CAS_SERVER_URL/logout?service=$SERVICE_URL"
    // 可以发送 HTTP 请求，或者让前端跳转
  }
}
```

### Step 3: OAuth2.0 实现示例

```scala
class OAuth2SSOInterceptor extends SSOInterceptor {
  
  private val OAUTH_SERVER = "https://oauth.yourcompany.com"
  private val CLIENT_ID = "linkis_client"
  private val CLIENT_SECRET = "your_secret"
  private val REDIRECT_URI = "http://linkis.yourcompany.com/api/callback"

  override def getUser(gatewayContext: GatewayContext): String = {
    // 1. 获取授权码
    val code = Option(gatewayContext.getRequest.getQueryParams.get("code"))
      .map(_.head)
      .orNull

    if (code == null) return null

    // 2. 换取 access token
    val tokenResponse = Http(s"$OAUTH_SERVER/oauth/token")
      .param("grant_type", "authorization_code")
      .param("code", code)
      .param("redirect_uri", REDIRECT_URI)
      .param("client_id", CLIENT_ID)
      .param("client_secret", CLIENT_SECRET)
      .asString()

    val accessToken = (parseJson(tokenResponse.body) \ "access_token").asString()

    // 3. 获取用户信息
    val userResponse = Http(s"$OAUTH_SERVER/oauth/userinfo")
      .header("Authorization", s"Bearer $accessToken")
      .asString()

    (parseJson(userResponse.body) \ "username").asString()
  }

  override def redirectTo(requestUrl: URI): String = {
    s"$OAUTH_SERVER/oauth/authorize?" +
    s"client_id=$CLIENT_ID&" +
    s"response_type=code&" +
    s"redirect_uri=$REDIRECT_URI&" +
    s"state=${UUID.randomUUID().toString()}"
  }

  override def logout(gatewayContext: GatewayContext): Unit = {
    // 清除本地 session，OAuth2 通常不需要服务端登出
  }
}
```

### Step 4: 注册 Spring Bean

```java
@Configuration
public class SSOConfiguration {
    
    @Bean
    public SSOInterceptor ssoInterceptor() {
        return new CasSSOInterceptor(); // 或 OAuth2SSOInterceptor
    }
}
```

### Step 5: 配置启用

**linkis-gateway.properties**：
```properties
# 启用 SSO
wds.linkis.gateway.conf.enable.sso=true

# 指定实现类（如果未通过 Spring 注册）
wds.linkis.gateway.conf.sso.interceptor=com.yourCompany.linkis.sso.CasSSOInterceptor

# Cookie 域名设置（如果跨域）
wds.linkis.gateway.cookie.domain.setup.switch=true
wds.linkis.gateway.domain.level=2
```

---

## 🔐 会话管理机制

### Session 存储方式

#### 1. 内存模式（默认）

**实现**：[SSOUtils.scala:49](../linkis-commons/linkis-module/src/main/scala/org/apache/linkis/server/security/SSOUtils.scala)

```scala
private val userTicketIdToLastAccessTime = new ConcurrentHashMap[String, Long]
```

**超时配置**：
```properties
# Session 超时时间（毫秒）
linkis.server.web.session.timeout=28800000  # 8小时
```

#### 2. Redis 分布式模式

**配置**：
```properties
# 启用 Redis 存储 Session
linkis.server.session.sso.redis=true
linkis.server.session.sso.redis.host=localhost
linkis.server.session.sso.redis.port=6379
linkis.server.session.sso.redis.password=
linkis.server.session.sso.redis.db=0
```

**实现**：[UserTicketService](../linkis-commons/linkis-module/src/main/scala/org/apache/linkis/server/ticket/UserTicketService.scala)

### Cookie 管理

**Cookie 结构**：
```
userTicketId: username,loginTimestamp
  ↓ 解密验证
  1. 从 Cookie 获取 userTicketId
  2. 从 Session 存储中获取最后访问时间
  3. 检查是否超时
  4. 返回用户名
```

**Cookie 配置**：
```properties
# Cookie 域名层级
wds.linkis.gateway.domain.level=3  # 如 .company.com

# SSL Cookie 安全
linkis.server.security.ssl=true   # 启用后 Cookie 设置 Secure 标志
```

**跨域 Cookie 支持**：[GatewaySSOUtils.scala:48](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/GatewaySSOUtils.scala)

```scala
def getCookieDomain(host: String, level: Int): String = host match {
  case DOMAIN_REGEX() =>
    // "dss.com" → ".dss.com"
    // "app.dss.com" → ".dss.com" (level=2)
    val domains = host.split("\\.")
    val parsedDomains = domains.takeRight(level)
    "." + parsedDomains.mkString(".")
  case IP_REGEX(ip) => ip
}
```

---

## 🚨 认证流程详解

### SecurityFilter 核心逻辑

**实现文件**：[SecurityFilter.scala:60](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/SecurityFilter.scala)

```scala
def doFilter(gatewayContext: GatewayContext): Boolean = {
  // 1. IP 白名单检查
  if (ENABLE_GATEWAY_AUTH.getValue) {
    if (!ipSet.contains(host)) return false
  }

  // 2. Referer 跨站检查
  if (refererValidate && !isValidReferer(referer)) {
    return false
  }

  // 3. 用户登录接口
  if (requestUri.startsWith(BDP_SERVER_USER_URI.getValue)) {
    userRestful.doUserRequest(gatewayContext)
    return false
  }

  // 4. 代理接口（某些情况下跳过认证）
  if (isPassAuthRequest && !ENABLE_SSO_LOGIN.getValue) {
    return true  // 跳过认证
  }

  // 5. Token 认证
  if (TokenAuthentication.isTokenRequest(gatewayContext)) {
    return TokenAuthentication.tokenAuth(gatewayContext)
  }

  // 6. Cookie/Session 认证
  val userName = GatewaySSOUtils.getLoginUser(gatewayContext)
  if (userName.isDefined) {
    return true  // 已登录
  }

  // 7. 测试模式
  if (Configuration.IS_TEST_MODE.getValue) {
    GatewaySSOUtils.setLoginUser(gatewayContext, testUser)
    return true
  }

  // 8. SSO 认证
  if (ENABLE_SSO_LOGIN.getValue) {
    val user = SSOInterceptor.getSSOInterceptor.getUser(gatewayContext)
    if (StringUtils.isNotBlank(user)) {
      GatewaySSOUtils.setLoginUser(gatewayContext.getRequest, user)
      return true
    } else if (isPassAuthRequest) {
      // 重定向到 SSO 登录页
      val ssoUrl = SSOInterceptor.getSSOInterceptor.redirectTo(requestUrl)
      gatewayContext.getResponse.redirectTo(ssoUrl)
      return false
    } else {
      // 返回 JSON 响应，包含 SSOURL
      return Message.noLogin("您尚未登录，请先登录!")
        .data("enableSSO", true)
        .data("SSOURL", SSOInterceptor.getSSOInterceptor.redirectTo(requestUrl))
    }
  }

  // 9. 正常登录响应
  return Message.noLogin("您尚未登录，请先登录!")
}
```

---

## 🎯 完整认证时序图

```
┌─────────┐                      ┌─────────────┐                      ┌──────────────┐
│  Browser│                      │ Linkis GW   │                      │ SSO Server    │
└────┬────┘                      └──────┬──────┘                      └──────┬───────┘
     │                                 │                                     │
     │ ① GET /api/resource/userInfo   │                                     │
     ├────────────────────────────────→│                                     │
     │                                 │ ② 检查 Cookie                        │
     │                                 │    无 userTicketId                   │
     │                                 │                                     │
     │                                 │ ③ 调用 SSOInterceptor.getUser()    │
     │                                 │    返回 null                         │
     │                                 │                                     │
     │ ④ Response: {                   │                                     │
     │   "enableSSO": true,            │                                     │
     │   "SSOURL": "https://sso..."    │                                     │
     │ }                                │                                     │
     │←────────────────────────────────┤                                     │
     │                                 │                                     │
     │ ⑤ window.location.replace(SSOURL)                                      │
     ├─────────────────────────────────────────────────────────────────────→│
     │                                 │                                     │
     │                                 │                                     │ ⑥ 显示登录页
     │                                 │                                     │
     │ ⑦ 用户输入用户名密码                                                   │
     ├─────────────────────────────────────────────────────────────────────→│
     │                                 │                                     │
     │                                 │                                     │ ⑧ 认证成功，生成 ticket
     │                                 │                                     │
     │ ⑨ Redirect: http://linkis/api/callback?ticket=TGT-xxx                 │
     │←────────────────────────────────────────────────────────────────────┤
     │                                 │                                     │
     │ ⑩ GET /api/callback?ticket=TGT-xxx                                    │
     ├────────────────────────────────→│                                     │
     │                                 │ ⑪ 调用 SSOInterceptor.getUser()     │
     │                                 │    验证 ticket，获取用户名           │
     │                                 │                                     │
     │                                 │ ⑫ 验证 ticket                      │
     │                                 ├─────────────────────────────────────→│
     │                                 │                                     │
     │                                 │ ⑬ 返回用户名                        │
     │                                 │←────────────────────────────────────┤
     │                                 │                                     │
     │                                 │ ⑭ 设置 userTicketId Cookie           │
     │                                 │    存入 Session                      │
     │                                 │                                     │
     │ ⑮ Response: { "status": 0 }     │                                     │
     │←────────────────────────────────┤                                     │
     │                                 │                                     │
     │ ⑯ 自动重定向到原始页面                                               │
     │    GET /api/resource/userInfo                                         │
     ├────────────────────────────────→│                                     │
     │                                 │ ⑰ 从 Cookie 读取 userTicketId       │
     │                                 │    验证 Session                     │
     │                                 │                                     │
     │ ⑱ Response: {                   │                                     │
     │   "userName": "hadoop"          │                                     │
     │ }                                │                                     │
     │←────────────────────────────────┤                                     │
```

---

## 📦 相关模块

### 1. linkis-um-sso 模块

**目录结构**：
```
linkis-public-enhancements/
└── linkis-um-sso/
    └── linkis-um-sso-common/
        └── src/main/java/org/apache/linkis/um/common/
            ├── exception/
            │   ├── UMException.java
            │   └── UMErrorCode.java
            └── (预留扩展空间)
```

**状态**：框架已搭建，待扩展开发

### 2. Gateway 核心类

| 类名 | 功能 | 文件路径 |
|------|------|----------|
| `SecurityFilter` | 安全过滤器，认证路由 | [linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/SecurityFilter.scala](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/SecurityFilter.scala) |
| `SSOInterceptor` | SSO 拦截器接口 | [linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/sso/SSOInterceptor.scala](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/sso/SSOInterceptor.scala) |
| `GatewaySSOUtils` | SSO 工具类 | [linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/GatewaySSOUtils.scala](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/GatewaySSOUtils.scala) |
| `SSOUtils` | Session 管理工具 | [linkis-module/src/main/scala/org/apache/linkis/server/security/SSOUtils.scala](../linkis-commons/linkis-module/src/main/scala/org/apache/linkis/server/security/SSOUtils.scala) |
| `TokenAuthentication` | Token 认证 | [linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/token/TokenAuthentication.scala](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/token/TokenAuthentication.scala) |
| `UserRestful` | 用户登录接口 | [linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala](../linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala) |

---

## 🔧 配置参数汇总

### Gateway 配置 (linkis-gateway.properties)

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `wds.linkis.gateway.conf.enable.sso` | `false` | 启用 SSO 登录 |
| `wds.linkis.gateway.conf.sso.interceptor` | `""` | SSO 拦截器实现类全限定名 |
| `wds.linkis.gateway.conf.enable.token.auth` | `false` | 启用 Token 认证 |
| `wds.linkis.gateway.auth.token.alive.enable` | `false` | Token 保持活跃 |
| `wds.linkis.gateway.conf.url.pass.auth` | `"/dws/"` | 跳过认证的 URL 前缀 |
| `wds.linkis.gateway.domain.level` | `3` | Cookie 域名层级 |
| `wds.linkis.gateway.cookie.domain.setup.switch` | `false` | Cookie 域名设置开关 |
| `wds.linkis.login_encrypt.enable` | `false` | 密码加密传输 |
| `wds.linkis.admin.user` | `"hadoop"` | 管理员用户名 |
| `wds.linkis.enable.gateway.auth` | `false` | 启用 IP 白名单认证 |

### Server 配置 (linkis.properties)

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `linkis.server.web.session.timeout` | `28800000` | Session 超时时间（毫秒） |
| `linkis.server.session.sso.redis` | `false` | 启用 Redis 存储 Session |
| `linkis.server.security.ssl` | `false` | 启用 SSL（Cookie Secure） |
| `linkis.server.session.ticketid.key` | `"userTicketId"` | Session Cookie 名称 |

---

## 🚀 部署指南

### 1. 打包部署 SSO 扩展

```bash
# 创建 SSO 扩展模块
mkdir -p linkis-public-enhancements/linkis-um-sso/linkis-um-sso-custom
cd linkis-public-enhancements/linkis-um-sso/linkis-um-sso-custom

# pom.xml
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.apache.linkis</groupId>
        <artifactId>linkis</artifactId>
        <version>1.3.0</version>
    </parent>
    <artifactId>linkis-um-sso-custom</artifactId>
    
    <dependencies>
        <dependency>
            <groupId>org.apache.linkis</groupId>
            <artifactId>linkis-gateway-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- 添加 SSO 协议依赖 -->
        <dependency>
            <groupId>org.jasig.cas.client</groupId>
            <artifactId>cas-client-core</artifactId>
            <version>3.6.4</version>
        </dependency>
    </dependencies>
</project>
EOF

# 编译
mvn clean package
```

### 2. 配置部署

**将 jar 包部署到 Gateway**：
```bash
cp target/linkis-um-sso-custom-1.3.0.jar \
   $LINKIS_HOME/lib/linkis-gateway-server-support/
```

**修改配置**：
```bash
# 编辑 linkis-gateway.properties
vim $LINKIS_HOME/conf/linkis-gateway.properties

# 添加配置
wds.linkis.gateway.conf.enable.sso=true
wds.linkis.gateway.conf.sso.interceptor=com.yourCompany.linkis.sso.CasSSOInterceptor
wds.linkis.gateway.cookie.domain.setup.switch=true
wds.linkis.gateway.domain.level=2
```

**重启 Gateway**：
```bash
cd $LINKIS_HOME
sh sbin/linkis-daemon.sh restart cg-linkis
```

### 3. 验证测试

```bash
# 1. 测试未登录访问
curl -i http://localhost:9001/api/user/info

# 预期响应：
# HTTP/1.1 200 OK
# {
#   "status": -1,
#   "data": {
#     "enableSSO": true,
#     "SSOURL": "https://sso.company.com/login?service=..."
#   }
# }

# 2. 模拟 SSO 登录后的请求
curl -i "http://localhost:9001/api/callback?ticket=TGT-xxx"

# 3. 验证登录成功
curl -i http://localhost:9001/api/user/info

# 预期响应：
# {
#   "status": 0,
#   "data": {
#     "userName": "hadoop"
#   }
# }
```

---

## 🐛 常见问题

### 1. SSO 无限重定向

**原因**：`redirectTo()` 返回的 URL 未正确携带原始请求 URL

**解决**：
```scala
override def redirectTo(requestUrl: URI): String = {
  val callbackUrl = s"$SERVICE_URL${requestUrl.getPath}?" +
                    s"${requestUrl.getQuery}"  // 保留原始查询参数
  s"$SSO_LOGIN_URL?service=$callbackUrl"
}
```

### 2. 跨域 Cookie 不生效

**原因**：Cookie 域名设置不正确

**解决**：
```properties
# 启用 Cookie 域名设置
wds.linkis.gateway.cookie.domain.setup.switch=true

# 设置正确的域名层级
# linkis.company.com → level=2 → .company.com
wds.linkis.gateway.domain.level=2
```

### 3. Session 频繁过期

**原因**：多个 Gateway 实例 Session 不同步

**解决**：
```properties
# 启用 Redis 存储 Session
linkis.server.session.sso.redis=true
linkis.server.session.sso.redis.host=redis-master
linkis.server.session.sso.redis.port=6379
linkis.server.session.sso.redis.password=your_password
```

### 4. ProxyUser 认证失败

**原因**：代理用户配置未正确加载

**解决**：
```properties
# 启用代理用户
wds.linkis.gateway.conf.enable.proxy.user=true

# 配置代理用户列表
wds.linkis.gateway.conf.proxy.user.list=user1,user2,user3

# 代理用户配置文件
wds.linkis.gateway.conf.proxy.user.config=proxy.properties
```

**proxy.properties**：
```properties
# 格式：proxyUser=validationCode
user1=abc123
user2=def456
```

---

## 📊 对比其他系统

| 功能 | Linkis | DSS | Apache Atlas | Keycloak |
|------|--------|-----|--------------|----------|
| **SSO 支持** | ✅ 扩展接口 | ✅ 扩展接口 | ✅ 开箱即用 (Knox) | ✅ 完整 SSO |
| **内置协议** | ❌ 无 | ❌ 无 | ✅ Knox SSO | ✅ OIDC/SAML |
| **Token 认证** | ✅ 支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| **扩展难度** | 中等 | 中等 | 低 | 无需扩展 |
| **Session 存储** | 内存/Redis | 内存/Redis | 内存 | 内存/DB |
| **跨域支持** | ✅ | ✅ | ✅ | ✅ |
| **代理用户** | ✅ 支持 | ✅ 支持 | ❌ | ✅ |

---

## 🎓 最佳实践

### 1. 生产环境配置建议

```properties
# 启用 SSO
wds.linkis.gateway.conf.enable.sso=true

# 启用密码加密
wds.linkis.login_encrypt.enable=true

# 启用 Redis Session
linkis.server.session.sso.redis=true

# 启用 Cookie 域名设置
wds.linkis.gateway.cookie.domain.setup.switch=true
wds.linkis.gateway.domain.level=2

# 启用 SSL
linkis.server.security.ssl=true

# 启用 IP 白名单（可选）
wds.linkis.enable.gateway.auth=true

# Session 超时时间
linkis.server.web.session.timeout=28800000
```

### 2. 监控和日志

**关键日志位置**：
```bash
# Gateway 认证日志
tail -f $LINKIS_HOME/logs/linkis-gateway.log

# 关键日志关键字：
# - "Token authentication succeed"
# - "SSO login"
# - "getLoginUser"
```

**监控指标**：
- 认证成功率
- Session 超时次数
- SSO 重定向次数
- Token 验证失败次数

### 3. 安全加固

1. **启用 HTTPS**
   ```bash
   # 配置 SSL 证书
   linkis.server.security.ssl=true
   linkis.server.security.ssl.keystore=/path/to/keystore.jks
   ```

2. **启用 IP 白名单**
   ```properties
   wds.linkis.enable.gateway.auth=true
   wds.linkis.gateway.auth.file=$LINKIS_HOME/conf/auth.txt
   ```
   
   **auth.txt**：
   ```
   192.168.1.100
   192.168.1.101
   ```

3. **禁用测试模式**
   ```properties
   # 确保生产环境禁用测试模式
   linkis.server.is.test.mode=false
   ```

---

## 📚 参考资料

### 官方文档

- [Linkis Gateway 配置文档](../docs/configuration/linkis-gateway-core.md)
- [Linkis 错误码文档](../docs/errorcode/linkis-gateway-authentication-errorcode.md)

### 相关协议

- **CAS Protocol**: https://apereo.github.io/cas/6.6.x/protocol/CAS-Protocol-Specification.html
- **OAuth 2.0**: https://oauth.net/2/
- **SAML 2.0**: https://www.oasis-open.org/committees/security/

### 扩展阅读

- Spring Security SSO 集成
- JWT Token 认证最佳实践
- Redis 分布式 Session 实现原理

---

## ✅ 总结

### Linkis SSO 支持 ✓

| 功能 | 状态 | 说明 |
|------|------|------|
| **SSO 扩展框架** | ✅ 完整 | 提供 SSOInterceptor 接口 |
| **Token 认证** | ✅ 支持 | Token-Code + Token-User |
| **用户名密码** | ✅ 支持 | 支持加密传输 |
| **代理用户** | ✅ 支持 | ProxyUser 功能 |
| **Session 管理** | ✅ 支持 | 内存/Redis 模式 |
| **跨域 Cookie** | ✅ 支持 | Cookie 域名配置 |
| **CAS 集成** | ⚠️ 需扩展 | 需实现 SSOInterceptor |
| **OAuth2 集成** | ⚠️ 需扩展 | 需实现 SSOInterceptor |
| **SAML 集成** | ⚠️ 需扩展 | 需实现 SSOInterceptor |
| **LDAP 集成** | ⚠️ 需扩展 | 需实现 SSOInterceptor |

### 核心优势

1. **灵活扩展**: 支持任意 SSO 协议
2. **多种认证**: Token/SSO/用户名密码并存
3. **分布式**: Redis Session 支持
4. **安全性**: SSL、加密、IP白名单

### 开发建议

- **标准场景**: 使用 Token 认证即可
- **企业 SSO**: 扩展 SSOInterceptor 实现 CAS/OAuth2
- **高可用**: 启用 Redis Session
- **安全**: 启用 SSL + IP白名单

---

**文档版本**: 1.0  
**最后更新**: 2026-06-22  
**适用版本**: Linkis 1.3.0+  
**作者**: v-kkhuang
