# 支持_ai用户鉴权 测试用例

> **需求编号**: REQ-01
> **需求名称**: 支持_ai用户鉴权
> **版本**: LinkisV2.1.4
> **功能属性**: 后端（不涉及前端）
> **生成日期**: 2026-10-08
> **来源**: 测试澄清纪要 + 需求文档 + 设计文档 + 代码变更分析

---

## 一、测试范围概述

### 1.1 被测对象

| 编号 | 被测对象 | 类型 | 所在文件 | 关键逻辑 |
|------|---------|------|---------|---------|
| O-1 | `TokenAuthentication.tryAiUserTokenDowngrade` | 私有方法（package-private） | `TokenAuthentication.scala:214-255` | 降级主逻辑：开关检查 -> 后缀检查 -> 剥离 -> 合法性校验 -> 重试 doAuth |
| O-2 | `TokenAuthentication.isLegalDowngradeUser` | 私有方法 | `TokenAuthentication.scala:262-267` | 用户名合法性校验（非空/长度<=64/路径穿越/空白/非法字符） |
| O-3 | `TokenAuthentication.tokenAuth`（降级分支） | 公开方法 | `TokenAuthentication.scala:129-140` | doAuth 失败后插入降级调用，成功走原路径，失败走原失败路径 |
| O-4 | `GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE` | 配置项 | `GatewayConfiguration.scala:157-158` | 降级开关，getHotValue 热加载，默认 false |
| O-5 | `GatewayConfiguration.AI_USER_SUFFIX` | 配置项 | `GatewayConfiguration.scala:164` | AI 后缀配置，默认 _ai，getHotValue 热加载 |

### 1.2 testScope 消费摘要

| 范围 | 数量 | 来源 |
|------|------|------|
| 必测项（mustTest） | 10 | 需求文档 AC1-AC8+AC11+AC12 |
| 选测项（optionalTest） | 9 | 需求文档 AC9-AC10 + 设计文档 IT-01~IT-07 |
| 不测项（notTest） | 6 | 需求范围外/不修改层 |

### 1.3 前置数据策略

> 造数策略来源：测试澄清纪要第五章 dataStrategy（非跳过，已确认数据项）

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|---------|------|---------|
| zhangsan 用户 + TOKEN-A token 记录 | SQL 插入 linkis_token 表 | linkis_token 表可写 | 测试后 DELETE 对应 token 记录 |
| zhangsan_ai 用户（无需 token 记录） | 无需构造（表无记录是降级前提） | - | - |
| failuser + failuser_ai 用户（无有效 token） | 确保表中无记录或 token 已过期 | - | - |
| 30 个并发测试用户（15 成功 + 15 失败） | SQL 批量插入 15 个成功用户的 token 记录 | linkis_token 表可写 | 测试后批量 DELETE |
| 畸形用户名（_ai、a/b_ai、a/../b_ai、a b_ai、a*b_ai） | 无需构造数据（降级校验在 doAuth 之前） | - | - |
| 动态 token（dyn- 前缀） | 需配置 DynamicTokenService | DynamicTokenService 已配置 | - |
| 后缀 _bot 配置 | System.setProperty 或修改 linkis-mg-gateway.properties | - | 测试后恢复默认 _ai |

**单测数据策略**: 单元测试使用 Mockito mock TokenService，不依赖数据库。开关和后缀通过 System.setProperty 控制（getHotValue 读 Java 系统属性），@BeforeEach/@AfterEach 清理。

**集成测试数据策略**: 需在 linkis_token 表预置 token 记录，通过 SQL 脚本构造。

---

## 二、必测用例（mustTest）

### TC001：开关关闭时不触发降级，doAuth 不重试

**来源**: 需求文档 AC1 / testScope MT-01

**测试类型**: 单元测试

**关联验收标准**: AC1

**前置条件**:
- 降级开关 `wds.linkis.gateway.ai.user.token.downgrade.enable=false`（默认关闭）
- Token 认证已启用（`enable.token.auth=true`）
- tokenService 使用 Mockito mock

**测试步骤**:
1. 不设置开关系统属性（保持默认关闭）
2. 调用 `TokenAuthentication.tryAiUserTokenDowngrade("TOKEN-A", "zhangsan_ai", "127.0.0.1", gatewayContext)`
3. 验证返回值
4. 验证 tokenService.doAuth 调用次数

**预期结果**:
- 返回值为 `false`
- `tokenService.doAuth` 从未被调用（`verify(tokenService, never()).doAuth(any, any, any)`）
- 降级代码零生效，行为与现网完全一致

**测试数据**:
```json
{
  "token": "TOKEN-A",
  "tokenUser": "zhangsan_ai",
  "host": "127.0.0.1"
}
```

**优先级**: P0

**覆盖场景**: 正向场景 - 开关关闭时不触发降级

---

### TC002：开关开启 + zhangsan_ai 降级成功，身份保持 _ai

**来源**: 需求文档 AC2 / testScope MT-02

**测试类型**: 单元测试

**关联验收标准**: AC2

**前置条件**:
- 降级开关 `wds.linkis.gateway.ai.user.token.downgrade.enable=true`
- AI 后缀为默认 `_ai`
- `zhangsan_ai` 在 linkis_token 表无记录
- `zhangsan` 有有效 token 记录，token `TOKEN-A` 绑定 `zhangsan`
- tokenService mock：对 `zhangsan_ai` 返回 false（首次失败），对 `zhangsan` 返回 true（降级成功）

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. Mock tokenService.doAuth 对任意参数返回 true
3. 调用 `TokenAuthentication.tryAiUserTokenDowngrade("TOKEN-A", "zhangsan_ai", "127.0.0.1", gatewayContext)`
4. 验证返回值
5. 验证 tokenService.doAuth 调用参数和次数

**预期结果**:
- 返回值为 `true`（降级成功）
- `tokenService.doAuth("TOKEN-A", "zhangsan", "127.0.0.1")` 被调用恰好一次
- 降级成功后放行身份保持 `zhangsan_ai`（setLoginUser 使用原始 tokenUser，不改写）

**测试数据**:
```json
{
  "token": "TOKEN-A",
  "tokenUser": "zhangsan_ai",
  "originalUser": "zhangsan",
  "host": "127.0.0.1"
}
```

**优先级**: P0

**覆盖场景**: 正向场景 - 降级成功流程

---

### TC003：降级重试失败，无第二次重试，走原失败路径

**来源**: 需求文档 AC3 / testScope MT-03

**测试类型**: 单元测试

**关联验收标准**: AC3

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- `zhangsan_ai` 和 `zhangsan` 均无有效 token 记录
- tokenService mock：doAuth 对任意参数返回 false

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. Mock tokenService.doAuth 对任意参数返回 false
3. 调用 `TokenAuthentication.tryAiUserTokenDowngrade("TOKEN-A", "zhangsan_ai", "127.0.0.1", gatewayContext)`
4. 验证返回值
5. 验证 tokenService.doAuth 调用次数

**预期结果**:
- 返回值为 `false`（降级失败）
- `tokenService.doAuth` 被调用恰好一次（降级重试一次，无第二次重试）
- 走现网原失败路径（noLogin + filterResponse）
- 不产生新的错误形态

**测试数据**:
```json
{
  "token": "TOKEN-A",
  "tokenUser": "zhangsan_ai",
  "originalUser": "zhangsan",
  "host": "127.0.0.1"
}
```

**优先级**: P0

**覆盖场景**: 负向场景 - 降级失败兜底

---

### TC004：非 _ai 后缀用户不触发降级

**来源**: 需求文档 AC4 / testScope MT-04

**测试类型**: 单元测试

**关联验收标准**: AC4

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- 测试用户：`zhangsan`（无后缀）、`zhangsan_ai2`（非配置后缀）

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. 调用 `tryAiUserTokenDowngrade("T", "zhangsan", "h", gatewayContext)`
3. 调用 `tryAiUserTokenDowngrade("T", "zhangsan_ai2", "h", gatewayContext)`
4. 验证两次调用的返回值
5. 验证 tokenService.doAuth 调用次数

**预期结果**:
- 两次调用均返回 `false`
- `tokenService.doAuth` 从未被调用
- 非配置后缀用户不触发降级逻辑
- 行为与现网一致（认证失败）

**测试数据**:
```json
[
  {"token": "T", "tokenUser": "zhangsan", "host": "h"},
  {"token": "T", "tokenUser": "zhangsan_ai2", "host": "h"}
]
```

**优先级**: P0

**覆盖场景**: 边界场景 - 后缀匹配检查

---

### TC005：降级异常被 Utils.tryCatch 捕获，返回 false，不传播

**来源**: 需求文档 AC5 / testScope MT-05

**测试类型**: 单元测试

**关联验收标准**: AC5

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- tokenService mock：doAuth 抛出 `RuntimeException`

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. Mock tokenService.doAuth 抛出 `new RuntimeException("simulated doAuth failure")`
3. 调用 `TokenAuthentication.tryAiUserTokenDowngrade("TOKEN-A", "zhangsan_ai", "127.0.0.1", gatewayContext)`
4. 验证返回值
5. 验证无异常传播
6. 验证 tokenService.doAuth 调用次数

**预期结果**:
- 返回值为 `false`
- 无异常抛出（异常被 `Utils.tryCatch` 捕获）
- `tokenService.doAuth` 被调用恰好一次
- 异常被记录（warn 级别，含 `token downgrade` 关键字和异常堆栈）
- 回退现网原报错路径，不产生新错误形态

**测试数据**:
```json
{
  "token": "TOKEN-A",
  "tokenUser": "zhangsan_ai",
  "host": "127.0.0.1",
  "mockException": "RuntimeException(simulated doAuth failure)"
}
```

**优先级**: P0

**覆盖场景**: 异常场景 - 降级过程异常保护

---

### TC006：畸形用户名校验（空/路径分隔/../空白/通配符）跳过降级

**来源**: 需求文档 AC12 / testScope MT-06

**测试类型**: 单元测试

**关联验收标准**: AC12

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- tokenService mock：doAuth 对任意参数返回 true（如果被调用的话）

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. 依次调用 `tryAiUserTokenDowngrade` 传入以下畸形用户名：
   - `_ai`（后缀 only，剥离后原用户为空）
   - `a/b_ai`（含路径分隔符 `/`）
   - `a/../b_ai`（含 `..` 路径穿越）
   - `a b_ai`（含空白字符）
   - `a*b_ai`（含通配符 `*`）
3. 验证每次调用返回值
4. 验证 tokenService.doAuth 调用次数

**预期结果**:
- 所有畸形用户名调用均返回 `false`
- `tokenService.doAuth` 从未被调用
- `isLegalDowngradeUser` 校验拦截所有畸形用户名
- 行为与现网一致（认证失败）
- 日志记录跳过降级原因（warn: illegal original user）

**测试数据**:
```json
[
  {"tokenUser": "_ai", "expectedOriginal": ""},
  {"tokenUser": "a/b_ai", "expectedOriginal": "a/b"},
  {"tokenUser": "a/../b_ai", "expectedOriginal": "a/../b"},
  {"tokenUser": "a b_ai", "expectedOriginal": "a b"},
  {"tokenUser": "a*b_ai", "expectedOriginal": "a*b"}
]
```

**优先级**: P0

**覆盖场景**: 安全场景 - 畸形用户名防构造绕过

---

### TC007：并发 30 线程降级，无串扰，各请求结果独立正确

**来源**: 需求文档 AC7 / testScope MT-07

**测试类型**: 单元测试

**关联验收标准**: AC7

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- 使用线程安全的 `RecordingTokenService`：15 个成功用户（user1~user15）+ 15 个失败用户（failuser1~failuser15）
- 30 个 `_ai` 后缀用户并发请求

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. 创建 `RecordingTokenService`，设置 15 个成功原用户集合
3. 构造 30 个 `_ai` 用户（15 成功 + 15 失败）
4. 使用 `CountDownLatch` 同步启动 30 个线程
5. 每个线程调用 `tryAiUserTokenDowngrade("T", aiUser, "h", gatewayContext)`
6. 等待全部完成（超时 30 秒）
7. 检查各请求认证结果
8. 验证 doAuth 总调用次数

**预期结果**:
- 各请求认证结果独立正确（15 个成功返回 true，15 个失败返回 false）
- 无串扰，降级逻辑无共享可变状态
- `doAuth` 总调用次数 = 30（每请求降级重试 1 次）
- 无并发异常

**测试数据**:
```json
{
  "successUsers": ["user1_ai", "user2_ai", ..., "user15_ai"],
  "failUsers": ["failuser1_ai", "failuser2_ai", ..., "failuser15_ai"],
  "totalThreads": 30,
  "timeout": "30 seconds"
}
```

**优先级**: P0

**覆盖场景**: 性能场景 - 并发独立无串扰

---

### TC008：后缀可配置（_bot 触发，_ai 不再触发）

**来源**: 需求文档 AC11 / testScope MT-08

**测试类型**: 单元测试

**关联验收标准**: AC11

**前置条件**:
- 降级开关开启
- AI 后缀配置为 `_bot`（而非默认 `_ai`）
- tokenService mock：doAuth 对任意参数返回 true

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. 通过 `System.setProperty` 设置后缀为 `_bot`
3. 调用 `tryAiUserTokenDowngrade("T", "zhangsan_bot", "h", gatewayContext)`
4. 验证返回值和 doAuth 调用参数
5. 调用 `tryAiUserTokenDowngrade("T", "zhangsan_ai", "h", gatewayContext)`
6. 验证返回值

**预期结果**:
- `zhangsan_bot` 触发降级，返回 `true`
- `tokenService.doAuth("T", "zhangsan", "h")` 被调用（剥离 `_bot` 后缀）
- `zhangsan_ai` 不再触发降级（后缀不匹配配置的 `_bot`），返回 `false`
- 后缀配置化生效

**测试数据**:
```json
[
  {"token": "T", "tokenUser": "zhangsan_bot", "expectedOriginal": "zhangsan", "host": "h"},
  {"token": "T", "tokenUser": "zhangsan_ai", "expectedOriginal": null, "host": "h"}
]
```

**优先级**: P0

**覆盖场景**: 参数配置场景 - 后缀可配置

---

### TC009：降级日志 token 全程脱敏（maskToken）

**来源**: 需求文档 AC6 / testScope MT-09

**测试类型**: 安全测试（日志验证）

**关联验收标准**: AC6

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- 触发降级各场景（成功/失败/异常）

**测试步骤**:
1. 触发降级成功场景（zhangsan_ai -> zhangsan 成功）
2. 触发降级失败场景（failuser_ai -> failuser 失败）
3. 触发降级异常场景（doAuth 抛 RuntimeException）
4. 收集全部降级相关日志输出
5. 检查日志中所有 token 字段

**预期结果**:
- 全部日志中 token 均为 `TokenSensitiveUtils.maskToken` 脱敏形态
- 无明文 token 输出
- 日志包含触发用户（_ai 用户）、降级目标用户（原用户）、URI、结果
- 日志关键字 `token downgrade` 可检索

**测试数据**:
```json
{
  "scenarios": ["降级成功", "降级失败", "降级异常"],
  "token": "TOKEN-A-SENSITIVE",
  "expectedMasked": "TOK*****NSE***"
}
```

**优先级**: P0

**覆盖场景**: 安全场景 - token 全链路脱敏

---

### TC010：动态 token 路径不走降级（前缀路由不介入）

**来源**: 需求文档 AC8 / testScope MT-10

**测试类型**: 单元测试

**关联验收标准**: AC8

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- 动态 token 前缀路由已配置（`DYNAMIC_TOKEN_PREFIX`）
- DynamicTokenService 已配置

**测试步骤**:
1. 通过 `System.setProperty` 开启降级开关
2. 构造以动态 token 前缀（`dyn-`）开头的 token
3. 调用 `TokenAuthentication.tokenAuth` 方法
4. 验证请求走 `dynamicTokenAuth` 路径
5. 验证降级逻辑不介入

**预期结果**:
- 走 `dynamicTokenAuth` 原路径
- 降级逻辑不介入（`tryAiUserTokenDowngrade` 不被调用）
- 行为与降级开关无关
- 动态 token 前缀路由在 doAuth 之前执行（`TokenAuthentication.scala:110-113`）

**测试数据**:
```json
{
  "token": "dyn-abc123",
  "tokenUser": "zhangsan_ai",
  "host": "127.0.0.1"
}
```

**优先级**: P0

**覆盖场景**: 边界场景 - 动态 token 路径不降级

---

## 三、选测用例（optionalTest）

### TC011：开关热加载即时切换（关闭 -> 开启 -> 关闭）

**来源**: 需求文档 AC10 / testScope OT-01

**测试类型**: 参数配置测试

**关联验收标准**: AC10

**前置条件**:
- 网关运行中，降级开关当前为关闭状态
- `zhangsan_ai` 在 linkis_token 表无记录
- `zhangsan` 有有效 token 记录

**测试步骤**:
1. 开关关闭时发起 `zhangsan_ai` 请求 -> 验证认证失败
2. 不重启网关，通过修改配置将开关设为 `true`
3. 再次发起 `zhangsan_ai` 请求 -> 验证降级成功
4. 不重启网关，将开关改回 `false`
5. 再次发起 `zhangsan_ai` 请求 -> 验证认证失败

**预期结果**:
- 行为随开关即时正确切换（不重启）
- 开关关闭 -> 认证失败（无降级）
- 开关开启 -> 降级成功（请求放行）
- 开关再次关闭 -> 认证失败（降级停止）
- `getHotValue` 热加载生效

**优先级**: P1

**覆盖场景**: 参数配置场景 - 热加载即时切换

---

### TC012：login 模式 x tokenAlive 的 4 种组合行为一致

**来源**: 需求文档 AC9 / testScope OT-02

**测试类型**: 功能测试

**关联验收标准**: AC9

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`
- `zhangsan_ai` 降级可成功

**测试步骤**:
1. 降级开关开启，测试 login=true x tokenAlive=true 组合
2. 降级开关开启，测试 login=true x tokenAlive=false 组合
3. 降级开关开启，测试 login=false x tokenAlive=true 组合
4. 降级开关开启，测试 login=false x tokenAlive=false 组合
5. 降级成功后观察各分支行为

**预期结果**:
- 成功后各分支（setLoginUser 的 alive/非 alive、login 响应）行为与非降级成功路径一致
- 身份均为 `_ai` 用户（tokenUser）
- 4 种组合行为一致，降级成功后走原成功路径对应分支

**优先级**: P1

**覆盖场景**: 功能场景 - login 模式与 tokenAlive 分支

---

### TC013：集成测试 IT-01 - 开关关闭 + _ai 用户请求

**来源**: 设计文档 IT-01 / testScope OT-03

**测试类型**: 集成测试

**关联验收标准**: AC1

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关 `wds.linkis.gateway.ai.user.token.downgrade.enable=false`
- `zhangsan_ai` 在 linkis_token 表无记录

**测试步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带有效但绑定用户不匹配的 token 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 认证失败，行为与现网完全一致（noLogin + filterResponse）
- 降级代码零生效
- 日志中无 `token downgrade` 关键字

**优先级**: P1

**覆盖场景**: 集成测试 - 开关关闭行为不变

---

### TC014：集成测试 IT-02 - 开关开启 + zhangsan_ai 降级成功

**来源**: 设计文档 IT-02 / testScope OT-04

**测试类型**: 集成测试

**关联验收标准**: AC2

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关开启
- `zhangsan_ai` 在 linkis_token 表无记录
- `zhangsan` 有有效 token 记录，token `TOKEN-A` 绑定 `zhangsan`

**测试步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带 `TOKEN-A` 发起请求
2. 观察网关认证行为和日志
3. 验证放行身份

**预期结果**:
- 首次 doAuth(TOKEN-A, zhangsan_ai, host) 失败
- 剥离后缀以 `zhangsan` 重试 doAuth 成功
- 请求放行，身份 = `zhangsan_ai`
- 日志记录降级事件

**优先级**: P1

**覆盖场景**: 集成测试 - 降级成功端到端

---

### TC015：集成测试 IT-03 - 降级失败 + 原用户 token 也无效

**来源**: 设计文档 IT-03 / testScope OT-05

**测试类型**: 集成测试

**关联验收标准**: AC3

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关开启
- `zhangsan_ai` 和 `zhangsan` 均无有效 token 记录

**测试步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带任意 token 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 首次 doAuth 失败
- 降级重试 doAuth 也失败
- 按现网原失败路径返回认证失败
- 无第二次重试

**优先级**: P1

**覆盖场景**: 集成测试 - 降级失败兜底

---

### TC016：集成测试 IT-04 - 非 _ai 用户认证失败

**来源**: 设计文档 IT-04 / testScope OT-06

**测试类型**: 集成测试

**关联验收标准**: AC4

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关开启
- 测试用户：`zhangsan`（无后缀）

**测试步骤**:
1. 以 `zhangsan` 为 Token-User，携带无效 token 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 认证失败，行为与现网一致
- 降级逻辑不触发
- 日志中无 `token downgrade` 关键字

**优先级**: P1

**覆盖场景**: 集成测试 - 非 _ai 用户不降级

---

### TC017：集成测试 IT-05 - 动态 token + _ai 用户

**来源**: 设计文档 IT-05 / testScope OT-07

**测试类型**: 集成测试

**关联验收标准**: AC8

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关开启
- 动态 token 前缀路由已配置

**测试步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带动态 token（`dyn-` 前缀）发起请求
2. 观察网关认证行为

**预期结果**:
- 走 `dynamicTokenAuth` 原路径
- 降级逻辑不介入
- 行为与降级开关无关

**优先级**: P1

**覆盖场景**: 集成测试 - 动态 token 不降级

---

### TC018：集成测试 IT-06 - 开关运行时切换

**来源**: 设计文档 IT-06 / testScope OT-08

**测试类型**: 集成测试

**关联验收标准**: AC10

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关当前为关闭状态

**测试步骤**:
1. 开关关闭时发起 `zhangsan_ai` 请求 -> 认证失败
2. 不重启网关，将开关改为 `true`
3. 再次发起 `zhangsan_ai` 请求 -> 降级成功
4. 不重启网关，将开关改回 `false`
5. 再次发起 `zhangsan_ai` 请求 -> 认证失败

**预期结果**:
- 行为随开关即时正确切换
- 热加载（`getHotValue`）生效

**优先级**: P1

**覆盖场景**: 集成测试 - 热加载运行时切换

---

### TC019：集成测试 IT-07 - login 模式 + tokenAlive

**来源**: 设计文档 IT-07 / testScope OT-09

**测试类型**: 集成测试

**关联验收标准**: AC9

**前置条件**:
- linkis-mg-gateway 服务已部署
- 降级开关开启
- 测试 login 模式（true/false）与 tokenAlive（true/false）的不同组合

**测试步骤**:
1. 降级开关开启，分别测试 login=true/false x tokenAlive=true/false 的 4 种组合
2. 降级成功后观察各分支行为

**预期结果**:
- 成功后各分支行为与非降级成功路径一致
- 身份均为 `_ai` 用户

**优先级**: P1

**覆盖场景**: 集成测试 - login 模式与 tokenAlive 分支

---

## 四、验收标准覆盖检查

| AC 编号 | 场景 | 对应用例 | 覆盖状态 |
|---------|------|---------|:--------:|
| AC1 | 开关关闭时不触发降级 | TC001, TC013 | 已覆盖 |
| AC2 | 开关开启 + _ai 用户降级成功 | TC002, TC014 | 已覆盖 |
| AC3 | 降级重试失败无第二次重试 | TC003, TC015 | 已覆盖 |
| AC4 | 非 _ai 后缀用户不触发降级 | TC004, TC016 | 已覆盖 |
| AC5 | 降级过程异常被捕获不传播 | TC005 | 已覆盖 |
| AC6 | 日志 token 全程脱敏 | TC009 | 已覆盖 |
| AC7 | 并发请求独立无串扰 | TC007 | 已覆盖 |
| AC8 | 动态 token 路径不降级 | TC010, TC017 | 已覆盖 |
| AC9 | login 模式与 tokenAlive 各分支行为一致 | TC012, TC019 | 已覆盖 |
| AC10 | 开关热加载即时切换 | TC011, TC018 | 已覆盖 |
| AC11 | 后缀可配置 | TC008 | 已覆盖 |
| AC12 | 畸形用户名不触发降级 | TC006 | 已覆盖 |

**验收标准覆盖率**: 12/12 (100%)

---

## 五、测试用例统计

| 维度 | 数量 |
|------|------|
| 必测用例（mustTest） | 10 |
| 选测用例（optionalTest） | 9 |
| 总计 | 19 |

### 按优先级分布

| 优先级 | 数量 | 用例编号 |
|--------|------|---------|
| P0 | 10 | TC001-TC010 |
| P1 | 9 | TC011-TC019 |

### 按测试类型分布

| 测试类型 | 数量 |
|---------|------|
| 单元测试 | 8 |
| 安全测试（日志验证） | 1 |
| 参数配置测试 | 1 |
| 功能测试 | 1 |
| 集成测试 | 8 |

### 按分类映射（Wemind）

| Wemind 分类 | 用例数 | 适用场景 |
|------------|--------|---------|
| 功能案例 | 5 | 业务功能验证（降级成功/失败/非_ai用户/后缀可配置/login模式） |
| 安全用例 | 3 | 权限验证、数据安全（畸形用户名/日志脱敏/异常保护） |
| 性能案例 | 1 | 并发测试 |
| 参数配置 | 3 | 配置项测试（开关关闭/热加载/后缀配置） |
| 接口案例 | 2 | 动态token路径/集成测试 |
| 流程案例 | 5 | 集成测试端到端流程 |

---

## 六、不测项说明

| 编号 | 不测项 | 原因 |
|------|--------|------|
| NT-01 | MCP 端 _ai 用户切换动作 | 需求范围外，MCP 端独立仓库实现 |
| NT-02 | 语句权限校验 | 需求范围外，由 MCP 端处理 |
| NT-03 | 共享目录文件访问 | 本版本不做 |
| NT-04 | 前端 UI 测试 | 功能属性为后端，不涉及前端 |
| NT-05 | CachedTokenService.doAuth 内部逻辑 | 不修改此层，对外行为保持不变 |
| NT-06 | SecurityFilter.filterResponse 行为 | 不修改，走现网原路径 |
