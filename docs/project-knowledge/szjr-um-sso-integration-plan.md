# 深圳金融监管局UM统一登录系统SSO集成方案

## 概述

本方案为 Apache Linkis 与深圳金融监管局UM统一登录系统的SSO集成设计文档，基于Linkis的SSO拦截器机制实现单点登录功能。

## UM系统接口分析

### 核心接口

| 接口名称 | HTTP方法 | 地址 | 说明 |
|---------|---------|------|------|
| 用户登录 | POST | http://ip:port/um/login | 用户名密码登录 |
| SSO验证 | POST | http://ip:port/sso/oauth | SSO Token验证 |
| 获取用户信息 | POST | http://ip:port/um/user | 获取用户详细信息 |
| 获取组织信息 | POST | http://ip:port/um/org | 获取组织架构信息 |
| 修改用户信息 | POST | http://ip:port/um/user/update | 更新用户信息 |
| 修改密码 | POST | http://ip:port/um/uPwd | 修改用户密码 |
| 服务权限校验 | POST | http://ip:port/um/service/check | 验证服务访问权限 |

### SSO登录流程

```
用户 → UM系统获取Token → Linkis网关(sso/login?ssoToken=xxx) → 验证Token → 登录成功
```

### Token格式

- **统一拼装格式**: `/sso/login?ssoToken={tokenValue}`
- **验证接口**: `POST http://ip:port/sso/oauth`

## 技术架构设计

### 1. 实现UM SSO拦截器

**文件位置**: `linkis-gateway-ujes/src/main/java/org/apache/linkis/gateway/ujes/security/sso/SZJRUMSSOInterceptor.java`

```java
package org.apache.linkis.gateway.ujes.security.sso;

import org.apache.linkis.gateway.http.GatewayHttpRequest;
import org.apache.linkis.gateway.security.sso.SSOInterceptor;
import org.apache.linkis.server.security.SSOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 深圳金融监管局UM统一登录系统SSO拦截器
 */
public class SZJRUMSSOInterceptor implements SSOInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SZJRUMSSOInterceptor.class);

    // UM系统配置
    private String umHost = "127.0.0.1"; // UM系统IP
    private String umPort = "8080";      // UM系统端口
    private String appId = "linkis";     // Linkis在UM系统注册的应用ID
    private String appToken = "linkis_token"; // Linkis在UM系统的Token
    
    // SSO配置
    private String umSSOLoginUrl = "/sso/login"; // UM系统SSO登录页面
    private String umSSOVerifyUrl = "/sso/oauth"; // UM系统SSO验证接口
    
    public SZJRUMSSOInterceptor() {
        init();
    }
    
    private void init() {
        // 从配置文件读取配置
        this.umHost = getConfiguration("wds.linkis.szjr.um.host", umHost);
        this.umPort = getConfiguration("wds.linkis.szjr.um.port", umPort);
        this.appId = getConfiguration("wds.linkis.szjr.um.appid", appId);
        this.appToken = getConfiguration("wds.linkis.szjr.um.apptoken", appToken);
        this.umSSOLoginUrl = getConfiguration("wds.linkis.szjr.um.sso.login.url", umSSOLoginUrl);
        this.umSSOVerifyUrl = getConfiguration("wds.linkis.szjr.um.sso.verify.url", umSSOVerifyUrl);
        
        logger.info("SZJRUMSSOInterceptor initialized with host: {}, port: {}, appId: {}", 
                    umHost, umPort, appId);
    }
    
    /**
     * 从SSO Token中获取用户信息
     * 调用UM系统的sso/oauth接口验证Token并获取用户ID
     */
    @Override
    public String getUser(GatewayHttpRequest request) {
        try {
            // 从请求参数中获取ssoToken
            String ssoToken = request.getQueryParams().get("ssoToken");
            if (ssoToken == null || ssoToken.isEmpty()) {
                logger.warn("ssoToken not found in request");
                return null;
            }
            
            // 获取客户端IP
            String clientIp = request.getRequestRealIpAddr();
            
            // 调用UM系统验证SSO Token
            String userId = verifySSOToken(ssoToken, clientIp);
            if (userId != null && !userId.isEmpty()) {
                logger.info("SSO authentication successful for user: {}", userId);
                return userId;
            }
            
            logger.warn("SSO authentication failed for token: {}", maskToken(ssoToken));
            return null;
        } catch (Exception e) {
            logger.error("Error during SSO authentication", e);
            return null;
        }
    }
    
    /**
     * 生成重定向URL，指向UM系统的SSO登录页面
     */
    @Override
    public String redirectTo(URI requestUri) {
        try {
            // 构建回调地址（当前请求地址）
            String callbackUrl = requestUri.toString();
            
            // 构建UM系统登录页面URL
            String umLoginUrl = String.format("http://%s:%s%s",
                                             umHost, umPort, umSSOLoginUrl);
            
            // 添加回调参数（登录成功后跳转回Linkis）
            String redirectUrl = umLoginUrl + "?callback=" + 
                               URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
            
            logger.info("Redirecting to UM login page: {}", redirectUrl);
            return redirectUrl;
        } catch (Exception e) {
            logger.error("Error building redirect URL", e);
            return null;
        }
    }
    
    /**
     * SSO登出处理
     * 清除Linkis会话并重定向到UM系统登出页面
     */
    @Override
    public void logout(GatewayHttpRequest request) {
        try {
            // 获取当前用户信息
            String userId = SSOUtils.getLoginUsername(request.getCookies());
            
            // 清除Linkis的登录Cookie
            SSOUtils.removeLoginUser(request.getCookies());
            
            // 重定向到UM系统登出页面
            String umLogoutUrl = String.format("http://%s:%s/sso/logout",
                                             umHost, umPort);
            
            logger.info("User {} logged out, redirecting to UM logout page: {}", 
                       userId, umLogoutUrl);
            
            // 可以选择性地调用UM系统的登出接口
            // notifyUMLogout(userId);
            
        } catch (Exception e) {
            logger.error("Error during SSO logout", e);
        }
    }
    
    /**
     * 验证SSO Token
     * 调用UM系统的/sso/oauth接口
     * 
     * @param ssoToken SSO Token
     * @param clientIp 客户端IP
     * @return 用户ID，验证失败返回null
     */
    private String verifySSOToken(String ssoToken, String clientIp) {
        HttpURLConnection conn = null;
        try {
            // 构建请求URL
            String urlStr = String.format("http://%s:%s%s", umHost, umPort, umSSOVerifyUrl);
            URL url = new URL(urlStr);
            
            // 建立连接
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            // 构建请求参数
            String params = String.format("ssoToken=%s&appId=%s&clientIp=%s",
                                        URLEncoder.encode(ssoToken, StandardCharsets.UTF_8.toString()),
                                        URLEncoder.encode(appId, StandardCharsets.UTF_8.toString()),
                                        URLEncoder.encode(clientIp != null ? clientIp : "", 
                                                        StandardCharsets.UTF_8.toString()));
            
            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // 解析响应（假设返回格式为JSON）
                String responseBody = response.toString();
                logger.debug("UM system response: {}", responseBody);
                
                // 解析用户ID（需要根据实际响应格式调整）
                return parseUserIdFromResponse(responseBody);
            } else {
                logger.warn("UM system returned error code: {}", responseCode);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error verifying SSO token with UM system", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 从UM系统响应中解析用户ID
     * 根据实际响应格式实现解析逻辑
     * 
     * @param response 响应内容
     * @return 用户ID
     */
    private String parseUserIdFromResponse(String response) {
        // 根据UM系统实际响应格式解析用户ID
        // 假设返回格式：{"code":0,"message":"success","data":"userId"}
        // 需要根据实际情况调整
        
        try {
            if (response.contains("\"code\":0") && response.contains("\"data\":")) {
                int dataStart = response.indexOf("\"data\":\"") + 8;
                int dataEnd = response.indexOf("\"", dataStart);
                if (dataStart > 0 && dataEnd > dataStart) {
                    return response.substring(dataStart, dataEnd);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error parsing user ID from response: {}", response, e);
            return null;
        }
    }
    
    /**
     * 通知UM系统用户登出（可选）
     */
    private void notifyUMLogout(String userId) {
        // 如果需要通知UM系统用户已登出，可以实现此方法
        logger.info("Notifying UM system that user {} has logged out", userId);
    }
    
    /**
     * 脱敏处理Token（用于日志输出）
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 6) {
            return "***";
        }
        return token.substring(0, 3) + "***" + token.substring(token.length() - 3);
    }
    
    /**
     * 获取配置项（简化版）
     */
    private String getConfiguration(String key, String defaultValue) {
        // 实际应从配置系统获取
        String value = System.getProperty(key);
        return value != null ? value : defaultValue;
    }
}
```

### 2. 配置文件

**文件位置**: `linkis-gateway-ujes/src/main/resources/linkis.properties`

```properties
# 深圳金融监管局UM系统配置
wds.linkis.szjr.um.host=127.0.0.1
wds.linkis.szjr.um.port=8080
wds.linkis.szjr.um.appid=linkis
wds.linkis.szjr.um.apptoken=linkis_secret_token_2024
wds.linkis.szjr.um.sso.login.url=/sso/login
wds.linkis.szjr.um.sso.verify.url=/sso/oauth

# 启用SSO登录
wds.linkis.gateway.conf.enable.sso=true
wds.linkis.gateway.conf.sso.interceptor=org.apache.linkis.gateway.ujes.security.sso.SZJRUMSSOInterceptor

# 禁用用户名密码登录（可选，强制使用SSO）
wds.linkis.gateway.usercontrol_switch_on=false
```

### 3. Spring Bean配置

**文件位置**: `linkis-gateway-ujes/src/main/resources/spring/application.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 深圳金融监管局UM SSO拦截器Bean -->
    <bean id="szjrUMSSOInterceptor" 
          class="org.apache.linkis.gateway.ujes.security.sso.SZJRUMSSOInterceptor">
    </bean>

</beans>
```

## 集成流程

### SSO登录流程

```
┌─────────┐        ┌──────────┐        ┌──────────┐        ┌─────────┐
│  用户   │ ───1──>│ UM系统   │ ───2──>│Linkis网关│ ───3──>│UM系统   │
└─────────┘        └──────────┘        └──────────┘        └─────────┘
    │                  │                     │                    │
    │                  │                     │                    │
    ├─ 访问Linkis      │                     │                    │
    │                  │                     │                    │
    │                  ├─ 未登录，重定向      │                    │
    │                  │  到UM登录页面       │                    │
    │                  │                     │                    │
    │                  │<────────────────────┤                    │
    │                  │                     │                    │
    ├─ 输入用户名密码   │                     │                    │
    │                  │                     │                    │
    ├─ UM系统验证      │                     │                    │
    │                  │                     │                    │
    │                  │                     │                    │
    │                  ├─ 生成SSO Token      │                    │
    │                  │                     │                    │
    │                  ├─ 重定向回Linkis     │                    │
    │                  │  携带ssoToken      │                    │
    │                  │                     │                    │
    │<─────────────────┤                     │                    │
    │                  │                     │                    │
    ├─ 携带Token访问   │                     │                    │
    │  Linkis (带token)│                     │                    │
    │                  │                     │                    │
    │                  │                     ├─ 提取ssoToken      │
    │                  │                     │                    │
    │                  │                     ├─ 调用UM验证接口     │
    │                  │                     │                    │
    │                  │                     │<───────────────────┤
    │                  │                     │                    │
    │                  │                     ├─ 获取userId       │
    │                  │                     │                    │
    │                  │                     ├─ 设置Linkis会话    │
    │                  │                     │                    │
    │                  │                     ├─ 返回业务页面       │
    │                  │                     │                    │
    │<─────────────────┤                     │                    │
│  登录成功，使用系统  │                     │                    │
└─────────┘        └──────────┘        └──────────┘        └─────────┘
```

### 详细步骤说明

1. **用户访问Linkis系统**
   - URL: `http://linkis-server:8080/`

2. **Linkis检测未登录**
   - `SecurityFilter` 检测到用户未登录
   - 判断是否启用SSO (`wds.linkis.gateway.conf.enable.sso=true`)

3. **重定向到UM登录页面**
   - 调用 `SSOInterceptor.redirectTo()`
   - 生成URL: `http://um-server:8080/sso/login?callback=http://linkis-server:8080/`

4. **用户在UM系统登录**
   - 用户输入用户名和密码
   - UM系统验证用户身份

5. **UM系统生成SSO Token**
   - UM系统验证成功后生成 `ssoToken`
   - 重定向回Linkis: `http://linkis-server:8080/sso/login?ssoToken=abc123...`

6. **Linkis验证SSO Token**
   - 调用 `SSOInterceptor.getUser()`
   - 提取 `ssoToken` 参数
   - 调用UM验证接口: `POST http://um-server:8080/sso/oauth`

7. **UM系统返回用户ID**
   - 验证成功返回: `{"code":0,"message":"success","data":"userId"}`
   - Linkis获取到用户ID

8. **设置Linkis会话**
   - 调用 `GatewaySSOUtils.setLoginUser()`
   - 设置登录Cookie
   - 用户成功登录Linkis系统

## 关键配置说明

### 网关配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `wds.linkis.gateway.conf.enable.sso` | 启用SSO功能 | false |
| `wds.linkis.gateway.conf.sso.interceptor` | SSO拦截器类名 | 空 |
| `wds.linkis.szjr.um.host` | UM系统IP地址 | 127.0.0.1 |
| `wds.linkis.szjr.um.port` | UM系统端口 | 8080 |
| `wds.linkis.szjr.um.appid` | 应用ID | linkis |
| `wds.linkis.szjr.um.apptoken` | 应用Token | linkis_token |

### 环境变量配置（可选）

```bash
export UM_SYSTEM_HOST="192.168.1.100"
export UM_SYSTEM_PORT="8080"
export LINKIS_APP_ID="linkis"
export LINKIS_APP_TOKEN="secret_token_2024"
```

## 测试验证

### 1. 单元测试

```java
@Test
public void testSSOInterceptor() {
    SZJRUMSSOInterceptor interceptor = new SZJRUMSSOInterceptor();
    
    // 测试getUser方法
    String token = "valid_sso_token_12345";
    String userId = interceptor.getUser(mockRequest);
    
    assertNotNull(userId);
    assertEquals("testuser", userId);
}

@Test
public void testRedirectTo() {
    SZJRUMSSOInterceptor interceptor = new SZJRUMSSOInterceptor();
    URI requestUri = URI.create("http://linkis-server:8080/home");
    
    String redirectUrl = interceptor.redirectTo(requestUri);
    
    assertTrue(redirectUrl.contains("sso/login"));
    assertTrue(redirectUrl.contains("callback"));
}
```

### 2. 集成测试

1. **启动Linkis网关**
   ```bash
   sh linkis-gateway-ujes/sbin/linkis-daemon.sh start cg-gateway
   ```

2. **访问系统**
   - 浏览器访问: `http://linkis-server:8080/`
   - 应自动重定向到: `http://um-server:8080/sso/login?callback=...`

3. **完成登录**
   - 在UM系统输入用户名密码
   - 登录成功后应重定向回Linkis
   - 验证Linkis会话已建立

4. **验证登录状态**
   - 检查Cookie是否设置
   - 调用 `/api/user/userInfo` 接口验证

## 异常处理

### 常见问题

1. **Token验证失败**
   - 检查UM系统是否正常运行
   - 检查网络连接
   - 验证appId和appToken配置

2. **重定向失败**
   - 检查回调URL编码
   - 验证UM登录页面URL正确性

3. **会话丢失**
   - 检查Cookie域配置
   - 验证会话超时设置

### 日志监控

```bash
# 查看SSO相关日志
tail -f logs/linkis-gateway.log | grep "SSO"

# 查看错误日志
tail -f logs/linkis-gateway.log | grep "ERROR"
```

## 部署清单

### 生产环境部署

1. **配置检查清单**
   - [ ] UM系统地址配置正确
   - [ ] appId和appToken已获取
   - [ ] SSO拦截器已注册
   - [ ] Cookie域配置正确

2. **网络配置**
   - [ ] Linkis到UM系统网络畅通
   - [ ] 防火墙规则已配置
   - [ ] DNS解析正常

3. **安全配置**
   - [ ] HTTPS证书配置
   - [ ] Token加密传输
   - [ ] 日志脱敏处理

### 回滚方案

如果SSO集成出现问题，可通过以下方式回滚：

1. 修改配置禁用SSO:
```properties
wds.linkis.gateway.conf.enable.sso=false
```

2. 重启网关服务

## 附录

### A. UM系统接口详细说明

#### SSO验证接口

**接口**: `POST http://ip:port/sso/oauth`

**请求参数**:
- `ssoToken`: SSO登录令牌
- `appId`: 应用ID
- `clientIp`: 客户端IP地址

**响应格式**:
```json
{
  "code": 0,
  "message": "success",
  "data": "userId"
}
```

#### 用户信息接口

**接口**: `POST http://ip:port/um/user`

**请求参数**:
- `appId`: 应用ID
- `appToken`: 应用令牌

**响应格式**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "user123",
    "userName": "张三",
    "userDept": "深圳金融监管局",
    "orgType": "SAFE"
  }
}
```

### B. 配置文件模板

```properties
# UM系统基础配置
wds.linkis.szjr.um.host=${UM_HOST:127.0.0.1}
wds.linkis.szjr.um.port=${UM_PORT:8080}
wds.linkis.szjr.um.appid=${UM_APPID:linkis}
wds.linkis.szjr.um.apptoken=${UM_APPTOKEN:linkis_token}

# SSO配置
wds.linkis.gateway.conf.enable.sso=true
wds.linkis.gateway.conf.sso.interceptor=org.apache.linkis.gateway.ujes.security.sso.SZJRUMSSOInterceptor

# 超时配置（毫秒）
wds.linkis.szjr.um.connect.timeout=5000
wds.linkis.szjr.um.read.timeout=5000

# 重试配置
wds.linkis.szjr.um.retry.times=3
wds.linkis.szjr.um.retry.interval=1000
```

---

**文档版本**: 1.0
**创建日期**: 2024-06-15
**适用版本**: Linkis 1.18.0+
**维护团队**: Apache Linkis社区
