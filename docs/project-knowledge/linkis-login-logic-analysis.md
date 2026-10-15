# Apache Linkis 登录逻辑分析

## 概述

Linkis 采用了多层认证机制，支持多种登录方式，包括用户名密码登录、Token 认证、SSO 单点登录等。本文档详细分析了 Linkis 的登录逻辑架构和实现细节。

## 核心架构组件

### 1. 核心文件位置

| 组件 | 文件路径 | 说明 |
|------|---------|------|
| 安全过滤器 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/SecurityFilter.scala` | 请求拦截和认证的核心入口 |
| 用户REST接口 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala` | 处理登录、登出等用户操作 |
| Token认证 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/token/TokenAuthentication.scala` | Token方式认证 |
| SSO工具 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/GatewaySSOUtils.scala` | 网关层SSO工具 |
| SSO基础 | `linkis-commons/linkis-module/src/main/scala/org/apache/linkis/server/security/SSOUtils.scala` | 服务器层SSO工具 |
| LDAP认证 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/LDAPUserRestful.scala` | LDAP方式认证 |
| 网关配置 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/config/GatewayConfiguration.scala` | 网关配置参数 |

## 登录流程详解

### 1. 请求拦截流程（SecurityFilter.doFilter）

所有请求都会经过 `SecurityFilter.doFilter()` 方法进行认证检查：

```scala
def doFilter(gatewayContext: GatewayContext): Boolean
```

**流程步骤：**

#### 步骤1：添加响应头
```scala
addAccessHeaders(gatewayContext)  // 添加 CORS 等响应头
```

#### 步骤2：IP白名单检查
```scala
if (ENABLE_GATEWAY_AUTH.getValue) {
  if (!ipSet.contains(host)) {
    // 拒绝非白名单IP的访问
  }
}
```

**配置项：** `wds.linkis.enable.gateway.auth` (默认 false)
**白名单文件：** `auth.txt`

#### 步骤3：Referer检查
```scala
if (refererValidate) {
  // 检查 Referer 头，防止CSRF攻击
  // 检查 HTTP 方法，防止动词篡改
}
```

#### 步骤4：登录请求处理
```scala
if (gatewayContext.getRequest.getRequestURI.startsWith(ServerConfiguration.BDP_SERVER_USER_URI.getValue)) {
  userRestful.doUserRequest(gatewayContext)
  false
}
```

**用户URI路径包括：**
- `/api/user/login` - 用户登录
- `/api/user/logout` - 用户登出
- `/api/user/register` - 用户注册
- `/api/user/userInfo` - 获取用户信息
- `/api/user/token-login` - Token登录
- `/api/user/sso-login` - SSO登录
- `/api/user/proxy` - 代理用户登录

#### 步骤5：Token认证检查
```scala
else if (TokenAuthentication.isTokenRequest(gatewayContext)) {
  TokenAuthentication.tokenAuth(gatewayContext)
}
```

#### 步骤6：SSO/普通登录检查
```scala
else {
  val userName = GatewaySSOUtils.getLoginUser(gatewayContext)
  // 根据配置决定是否启用SSO
}
```

### 2. 用户名密码登录流程

#### 2.1 核心方法
```scala
UserPwdAbstractUserRestful.tryLogin(gatewayContext: GatewayContext): Message
```

#### 2.2 登录步骤

**步骤1：获取用户名和密码**
```scala
private def getUserNameAndPWD(gatewayContext: GatewayContext): (String, String)
```

- 支持从 QueryParams 或 RequestBody 获取参数
- 支持 RSA 加密密码解密（如果启用了 `LOGIN_ENCRYPT_ENABLE`）
- 参数名：`userName`、`password` 或 `passwdEncrypt`

**步骤2：系统用户禁止检查**
```scala
if (GatewayConfiguration.PROHIBIT_LOGIN_SWITCH.getValue && webLogin) {
  PROHIBIT_LOGIN_PREFIX.split(",").foreach { prefix =>
    if (userName.toLowerCase().startsWith(prefix)) {
      return Message.error("System users are prohibited from logging in!")
    }
  }
}
```

**配置项：**
- `linkis.system.user.prohibit.login.switch` (默认 false)
- `linkis.system.user.prohibit.login.prefix` (默认 "hduser,shduser,hadoop")

**步骤3：管理员登录检查**
```scala
if (GatewayConfiguration.ADMIN_USER.getValue.equals(userName) && 
    GatewayConfiguration.ADMIN_PASSWORD.getValue.equals(password)) {
  GatewaySSOUtils.setLoginUser(gatewayContext, userName)
  return Message.ok("login successful!").data("isAdmin", true)
}
```

**配置项：**
- `wds.linkis.admin.user` (默认 "hadoop")
- `wds.linkis.admin.password` (默认与管理员用户相同)

**步骤4：用户控制模块登录或标准登录**
```scala
if (GatewayConfiguration.USERCONTROL_SWITCH_ON.getValue) {
  message = userControlLogin(userName, password, gatewayContext)
} else {
  message = login(lowerCaseUserName, password)
}
```

##### 4.1 用户控制模式
通过 RPC 调用 UserControl 服务进行认证：

```scala
val requestLogin = new RequestLogin()
requestLogin.setUserName(userName).setPassword(password)
sender.ask(requestLogin) match {
  case r: ResponseLogin =>
    if (r.getStatus == 0) {
      val proxyUser = getRandomProxyUser()
      GatewaySSOUtils.setLoginUser(gatewayContext, proxyUser)
    }
}
```

**配置项：** `wds.linkis.gateway.usercontrol_switch_on` (默认 false)

##### 4.2 标准登录模式
直接调用具体的登录实现：

```scala
protected def login(userName: String, password: String): Message
```

**LDAP 实现：**
```scala
LDAPUtils.login(userName, password)
```

**步骤5：设置登录用户**
```scala
GatewaySSOUtils.setLoginUser(gatewayContext, lowerCaseUserName)
```

### 3. Token认证流程

#### 3.1 Token请求识别
```scala
def isTokenRequest(gatewayContext: GatewayContext): Boolean = {
  (gatewayContext.getRequest.getHeaders.containsKey(TOKEN_KEY) &&
    gatewayContext.getRequest.getHeaders.containsKey(TOKEN_USER_KEY)) ||
  (gatewayContext.getRequest.getCookies.containsKey(TOKEN_KEY) &&
    gatewayContext.getRequest.getCookies.containsKey(TOKEN_USER_KEY))
}
```

**Token Header/Cookie 键名：**
- `Token-Code`: Token值
- `Token-User`: 用户名
- `Token-Alive`: 是否保持Token活跃

#### 3.2 Token认证执行
```scala
def tokenAuth(gatewayContext: GatewayContext, login: Boolean = false): Boolean
```

**认证步骤：**

1. **检查Token认证开关**
```scala
if (!ENABLE_TOKEN_AUTHENTICATION.getValue) {
  return Message.noLogin("Gateway未启用token认证!")
}
```

2. **获取Token和用户**
```scala
val token = gatewayContext.getRequest.getHeaders.get(TOKEN_KEY).head
val tokenUser = gatewayContext.getRequest.getHeaders.get(TOKEN_USER_KEY).head
// 或从Cookie获取
```

3. **执行Token验证**
```scala
val ok = tokenService.doAuth(token, tokenUser, host)
```

4. **设置登录状态**
```scala
if (ok) {
  if (ENABLE_TOEKN_AUTHENTICATION_ALIVE.getValue || tokenAlive) {
    GatewaySSOUtils.setLoginUser(gatewayContext.getRequest, tokenUser, true)
  } else {
    GatewaySSOUtils.setLoginUser(gatewayContext.getRequest, tokenUser, false)
  }
}
```

**配置项：**
- `wds.linkis.gateway.conf.enable.token.auth` (默认 false)
- `wds.linkis.gateway.auth.token.alive.enable` (默认 false)

### 4. SSO单点登录流程

#### 4.1 SSO拦截器接口
```scala
trait SSOInterceptor {
  def getUser(gatewayContext: GatewayContext): String
  def redirectTo(requestUrl: URI): String
  def logout(gatewayContext: GatewayContext): Unit
}
```

#### 4.2 SSO登录流程
```scala
if (GatewayConfiguration.ENABLE_SSO_LOGIN.getValue) {
  val user = SSOInterceptor.getSSOInterceptor.getUser(gatewayContext)
  if (StringUtils.isNotBlank(user)) {
    GatewaySSOUtils.setLoginUser(gatewayContext.getRequest, user)
    true
  } else {
    // 重定向到SSO登录页面
    gatewayContext.getResponse.redirectTo(
      SSOInterceptor.getSSOInterceptor.redirectTo(gatewayContext.getRequest.getURI)
    )
  }
}
```

**配置项：**
- `wds.linkis.gateway.conf.enable.sso` (默认 false)
- `wds.linkis.gateway.conf.sso.interceptor` (SSO拦截器实现类)

### 5. 会话管理（SSOUtils）

#### 5.1 登录用户设置
```scala
def setLoginUser(addCookie: Cookie => Unit, username: String, updateSession: Boolean = true)
```

**实现细节：**
1. 生成用户Ticket：`username + "," + System.currentTimeMillis`
2. 加密Ticket（通过 ServerConfiguration.getTicketByUsername）
3. 存储到 Session Map：`userTicketIdToLastAccessTime`
4. 设置 Cookie

#### 5.2 登录用户获取
```scala
def getLoginUser(getCookies: => Array[Cookie]): Option[String]
```

**实现细节：**
1. 从Cookie中获取 `userTicketId`
2. 检查是否超时
3. 解析Ticket获取用户名

#### 5.3 会话超时机制
```scala
private def isTimeoutOrNot(userTicketId: String): Unit
```

**超时检查：**
```scala
if (System.currentTimeMillis - lastAccessTime > sessionTimeout) {
  userTicketIdToLastAccessTime.remove(userTicketId)
  throw new LoginExpireException("登录已过期，请重新登录！")
}
```

**配置项：** `wds.linkis.server.web.session.timeout`（会话超时时间，单位毫秒）

**定时清理：**
```scala
Utils.defaultScheduler.scheduleAtFixedRate(
  // 清理超时的会话
  runnable,
  sessionTimeout,
  sessionTimeout / 10,
  TimeUnit.MILLISECONDS
)
```

## 安全特性

### 1. IP白名单
- 配置文件：`auth.txt`
- 配置开关：`wds.linkis.enable.gateway.auth`

### 2. Referer验证
- 防止CSRF攻击
- 配置开关：`wds.linkis.server.security.referer.validate`
- 允许的Referer：`wds.linkis.server.address`

### 3. 密码加密传输
- 支持RSA加密
- 配置开关：`wds.linkis.login_encrypt.enable`
- 公钥获取接口：`/api/user/publicKey`

### 4. Cookie域控制
- 域级别配置：`wds.linkis.gateway.domain.level` (默认 3)
- 开关：`wds.linkis.gateway.cookie.domain.setup.switch`

### 5. 系统用户保护
- 禁止系统用户通过Web登录
- 配置前缀：`linkis.system.user.prohibit.login.prefix`

## 配置参数汇总

| 配置项 | 默认值 | 说明 |
|-------|-------|------|
| `wds.linkis.gateway.conf.enable.token.auth` | false | 启用Token认证 |
| `wds.linkis.gateway.auth.token.alive.enable` | false | Token保持活跃 |
| `wds.linkis.gateway.conf.enable.sso` | false | 启用SSO单点登录 |
| `wds.linkis.gateway.usercontrol_switch_on` | false | 启用用户控制模块 |
| `wds.linkis.admin.user` | hadoop | 管理员用户名 |
| `wds.linkis.admin.password` | hadoop | 管理员密码 |
| `wds.linkis.login_encrypt.enable` | false | 启用登录密码加密 |
| `wds.linkis.enable.gateway.auth` | false | 启用网关IP认证 |
| `linkis.system.user.prohibit.login.switch` | false | 禁止系统用户登录 |
| `wds.linkis.server.web.session.timeout` | - | 会话超时时间(毫秒) |
| `wds.linkis.gateway.url.pass.auth` | /dws/ | 无需认证的URL前缀 |

## 登录流程图

```
用户请求
   ↓
SecurityFilter.doFilter
   ↓
添加CORS头 → IP白名单检查 → Referer验证
   ↓
是否为用户相关请求？
   ├─ 是 → UserRestful.doUserRequest
   │         ├─ login → 用户名密码登录
   │         ├─ logout → 登出
   │         ├─ token-login → Token登录
   │         └─ sso-login → SSO登录
   │
   ├─ 是否为Token请求？
   │   ├─ 是 → TokenAuthentication.tokenAuth
   │   └─ 否 → 检查SSO/普通登录
   │
   └─ 获取登录用户
        ├─ SSO模式？
        │   ├─ 是 → SSOInterceptor.getUser
        │   └─ 否 → GatewaySSOUtils.getLoginUser
        │             ↓
        │           SSOUtils.getLoginUser (Cookie → Ticket → 用户名)
        │             ↓
        │           检查会话超时
        │
        └─ 认证成功？
            ├─ 是 → 放行请求
            └─ 否 → 返回401未授权
```

## 扩展登录方式

### 自定义登录实现

可以通过继承 `UserPwdAbstractUserRestful` 来实现自定义的登录方式：

```scala
class CustomUserRestful extends UserPwdAbstractUserRestful {
  override def login(userName: String, password: String): Message = {
    // 自定义登录逻辑
  }
}
```

### 自定义SSO实现

通过实现 `SSOInterceptor` 接口：

```scala
class CustomSSOInterceptor extends SSOInterceptor {
  override def getUser(gatewayContext: GatewayContext): String = {
    // 从SSO系统获取用户
  }
  
  override def redirectTo(requestUrl: URI): String = {
    // 返回SSO登录页面URL
  }
  
  override def logout(gatewayContext: GatewayContext): Unit = {
    // SSO登出逻辑
  }
}
```

## 总结

Linkis 的登录系统具有以下特点：

1. **多层防护**：IP白名单 + Referer验证 + 会话管理
2. **多种认证方式**：用户名密码、Token、SSO、LDAP
3. **灵活配置**：支持多种认证模式切换
4. **安全增强**：密码加密传输、系统用户保护
5. **可扩展性**：支持自定义登录和SSO实现

通过理解这个登录逻辑，可以更好地集成 Linkis 到现有的认证体系中。
