> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/m5kv9LgO82IPwmqX)

# 网关 _ai 用户 token 降级鉴权 需求文档

**需求类型**: NEW（新增功能）
**风险等级**: 低风险（DPMS 评定）
**文档版本**: v4.0（标准格式稿，Q1-Q6 全部关闭）
**创建日期**: 2026-09-21
**更新日期**: 2026-09-24
**需求来源**: DPMS Story #548689（产品 100484 / BDP_Linkis）——本文档仅覆盖其中 **Linkis 网关认证缺口** 子集
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**提出部门**: 企业风险部（企金）
**开发/测试**: alexyang(杨永坤) / stacyyan(严晓斌)
**功能属性**: 后端（不涉及前端）

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | 网关新增降级开关：开启后，`_ai` 用户 token 认证失败时，剥离后缀用原 user 的 authtoken 重试鉴权一次，解决 `_ai` 用户无 token 记录被拒之门外的问题 |
| **唯一改动服务** | **linkis-mg-gateway**（linkis-gateway-core，单模块单切入点） |
| **开关** | 默认**不开启**（`wds.linkis.gateway.ai.user.token.downgrade.enable=false`）；仅 `_ai` 后缀用户可触发；仅认证失败路径生效；支持热加载（`getHotValue`）不重启关停 |
| **放行身份** | 降级成功后请求身份保持 `_ai` 用户（仅凭证降级，身份不改写） |
| **范围外** | MCP 用户切换动作、语句权限校验（均 MCP 端处理）；共享目录文件访问（本版本不做）；mcp server 侧降级（独立仓库）；前端 |

> 💡 **阅读指引**：需求背景看 §1 → 功能描述看 §3 → 验收标准看 §4 → 配置说明看 §5 → 关键代码位置看 §6
> ⚠️ 标注 ✅ 的代码锚点已在 `dev-2.1.4-webank` 分支核实（2026-09-24），本需求全部改动集中在 `TokenAuthentication.scala` 一条链路，**代码已实现完毕**。

---

## 1. 需求背景【核心】

### 1.1 业务背景

企金 MCP 通道将调用用户统一切换为独立身份 `<user>_ai`（实名/系统用户均切换，切换动作在 MCP 端完成）。切换后的 `_ai` 用户到达 Linkis 网关时，因 token 体系（linkis_token 表）中**没有 `<user>_ai` 的记录**，token 认证（`doAuth`）直接失败，请求被拒——这是 `_ai` 通道接入 Linkis 的**第一道阻塞点**。

本需求在网关侧提供降级能力：`_ai` 用户认证失败时，剥离后缀改用**原用户自己的 token** 重试鉴权，让 `_ai` 用户无需独立 token 记录即可通过网关。mcp server 侧同步支持降级（独立仓库，范围外）。

其余两个缺口（语句权限、共享目录）按需求方最新界定分别由 MCP 端处理和本版本不做，见文档头部范围界定。

### 1.2 认证链路现状（已核实 ✅）

当前 `tokenAuth` 完整流程（`TokenAuthentication.scala`）：

```
tokenAuth(gatewayContext, login)                          :55
 ├─ ENABLE_TOKEN_AUTHENTICATION 检查（生产 enable.token.auth=true 已启用）  :56
 ├─ 从 Header / Cookie 提取 Token-Code + Token-User        :62-93
 ├─ tokenAlive 参数处理                                     :94-109
 ├─ 动态 token 前缀路由 → dynamicTokenAuth（不走静态 doAuth） :110-113
 ├─ 静态 token 认证：tokenService.doAuth(token, tokenUser, host)   :118
 │    ├─ 失败 → 构造 noLogin 报错                           :115-123
 │    │         └─ 【新增降级分支】tryAiUserTokenDowngrade  :129-140
 │    │         └─ filterResponse 返回认证失败               :164
 │    └─ 成功 →                                            :124
 │         ├─ 动态 token 签发请求分支                        :130-137
 │         ├─ login 模式：setLoginUser(tokenUser)           :139-149
 │         └─ 常规模式：GatewaySSOUtils.setLoginUser(       :150-157
 │              request, tokenUser, tokenAlive)
 │              ★ 放行身份 = tokenUser（保持 _ai 用户，见 §3.3 Q1）
 └─ 日志：入口 :68-76 / 成功 :125-128 / 失败 :160-163（token 均已 maskToken 脱敏）
```

| # | 现状要点 | 锚点 | 对本需求的意义 |
|---|---------|------|--------------|
| 1 | doAuth 失败即拒绝，无任何降级路径 | `TokenAuthentication.scala:118-123` + `:164` | ✅ 降级逻辑的唯一切入点 |
| 2 | token-user 不匹配抛 `TokenAuthException(ILLEGAL_TOKENUSER)` | `CachedTokenService.scala:226-231` | ✅ `_ai` 用户必然命中此异常（表无记录/用户不匹配） |
| 3 | **认证成功后放行身份 = tokenUser** | `TokenAuthentication.scala:154-156`（`setLoginUser`） | ✅ 降级后请求身份保持 `_ai`（Q1 已定，见 §3.3） |
| 4 | 动态 token（前缀路由）不走静态 doAuth | `TokenAuthentication.scala:110-113` | ✅ 降级范围边界：仅静态 token 路径（§3） |
| 5 | token 可来自 Header 或 Cookie | `TokenAuthentication.scala:62-93` | ✅ 降级判断在提取之后，两种来源均覆盖 |
| 6 | 既有 token 脱敏日志规范 | `TokenSensitiveUtils.maskToken`（linkis-common） | ✅ 降级日志直接复用 |
| 7 | 网关既有用户映射机制（方向相反可参考） | `ProxyUserUtils` + `GatewayConfiguration.ENABLE_PROXY_USER`（`:25`，`wds.linkis.gateway.conf.enable.proxy.user`） | ✅ 开关命名与实现形态参考 |
| 8 | 网关开关命名惯例 | `GatewayConfiguration.scala`（`wds.linkis.gateway.conf.enable.token.auth`、`wds.linkis.gateway.auth.token.alive.enable` 等） | ✅ 新开关命名对齐（§5） |

### 1.3 需求目标

1. 降级开关开启后，`_ai` 用户复用原用户 token 通过网关认证，无需为每个 `_ai` 用户维护 token 记录；
2. 开关默认关闭，关闭时网关行为与现网完全一致；
3. 降级不扩大权限面：触发条件严格限定（`_ai` 后缀 + 认证失败），重试至多一次，凭据仍是原用户自己的 token；
4. 全链路可审计：降级事件有日志，token 全程脱敏。

---

## 2. 用户故事【核心】

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台管理员**，我希望开启降级开关后，`<user>_ai` 用户即使没有独立 token 也能复用原用户 token 通过网关鉴权，以便 `_ai` 通道顺畅接入且无需批量维护 token 记录。 |
| US-2 | 作为**安全管理员**，我希望降级仅对 `_ai` 后缀用户、仅在认证失败路径生效、至多重试一次，以便降级不成为认证旁路。 |
| US-3 | 作为**平台运维**，我希望开关默认关闭且支持快速关停（不重启，通过 `getHotValue` 热加载），以便生产出现认证异常时一键回退现网行为。 |
| US-4 | 作为**审计人员**，我希望每次降级鉴权都有可检索的日志记录（谁触发、降级到谁、成功与否），token 脱敏，以便审计 AI 通道认证行为。 |

---

## 3. 功能描述【核心】

### 3.1 功能概述

在网关 `TokenAuthentication.tokenAuth` 的静态 token 认证失败分支（`doAuth` 返回 false 或抛异常后）新增 **AI 用户 token 降级鉴权**逻辑：当开关开启且 tokenUser 以配置的 AI 后缀（默认 `_ai`）结尾时，剥离后缀得到原用户，用同一 token 对原用户重试 `doAuth` 一次。成功则按原成功路径继续（含动态 token 签发、login 模式、setLoginUser 各分支），失败则走现网原失败路径。

### 3.2 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | 降级开关 | 新增 `wds.linkis.gateway.ai.user.token.downgrade.enable`（默认 `false`），使用 `getHotValue` 读取，支持不重启关停 |
| F-2 | AI 后缀配置 | 新增 `wds.linkis.gateway.ai.user.suffix`（默认 `_ai`），使用 `getHotValue` 读取，支持后缀可配置 |
| F-3 | 降级触发判定 | 三个条件同时满足才触发：① 开关开启；② tokenUser 以配置后缀结尾；③ 该用户 token 认证失败（含 `ILLEGAL_TOKENUSER` 等异常） |
| F-4 | 后缀剥离与合法性校验 | 剥离后缀得到原用户，校验：非空、长度 ≤64、不含 `..`、不含路径分隔符（`\/:*?"<>|`）和空白字符；不合法则跳过降级 |
| F-5 | 降级重试 doAuth | 用同一 token 对原用户重试 `doAuth` 一次（至多一次，禁止二次降级/循环重试）；重试异常用 `Utils.tryCatch` 包裹，异常时回退 false |
| F-6 | 降级成功后放行身份 | 降级成功后请求身份保持 `_ai` 用户（`setLoginUser` 仍写 tokenUser），**身份不改写为原用户**（仅凭证降级） |
| F-7 | 降级失败兜底 | 原用户重试也失败 → 走现网原失败路径（noLogin + filterResponse），报错行为与现网一致 |
| F-8 | 降级全程异常保护 | 降级调用本身用 `Utils.tryCatch` 包裹（`:129-140`），任何异常回退现网原报错路径，不产生新错误形态 |
| F-9 | 降级日志审计 | warn 级别记录降级触发（触发用户、降级目标、URI、脱敏 token），info 级别记录降级成功；日志关键字 `token downgrade` 可检索 |
| F-10 | 动态 token 路径不降级 | 动态 token（前缀路由 `:110-113`）不走静态 doAuth，降级逻辑不介入，行为不变 |

### 3.3 关键设计决策（Q1-Q6 全部关闭）

| 编号 | 问题 | 决策结论 | 代码锚点 |
|:----:|------|---------|---------|
| **Q1** | 降级成功后，请求放行身份是谁？ | **选项 A：仅凭证降级**——doAuth 用原用户，`setLoginUser` 仍写 `_ai` 用户。身份与隔离目的对齐，jobhistory 记 `_ai`，引擎以 `<user>_ai` 拉起 | `TokenAuthentication.scala:200` 注释明确 "The login identity is NOT rewritten"；`:161` setLoginUser(tokenUser) |
| **Q2** | `_ai` 后缀是否配置化？ | **配置化**，新增 `wds.linkis.gateway.ai.user.suffix`（默认 `_ai`），用 `getHotValue` 读取 | `GatewayConfiguration.scala:152-157` AI_USER_SUFFIX |
| **Q3** | 开关是否要求热加载？ | **要求热加载**（`getHotValue`），鉴权链路出问题时可不重启网关一键关停 | `GatewayConfiguration.scala:144-149` AI_USER_TOKEN_DOWNGRADE_ENABLE + `:220` getHotValue |
| **Q4** | 降级审计口径？ | **仅日志**（与现网 token 认证审计口径一致），不额外落库/上报 | `TokenAuthentication.scala:235-253` warn/info 日志 |
| **Q5** | 原用户 token 已过期/禁用场景是否降级？ | **统一触发，不做异常分支过滤**——降级重试 doAuth 时由 `CachedTokenService` 自行判断 token 有效性。若 token 已过期/禁用，doAuth 返回 false，降级失败走原失败路径。不在降级层做异常类型区分 | `TokenAuthentication.scala:240` 直接调用 doAuth，不区分异常类型 |
| **Q6** | `_ai` 用户未来拥有独立 token 记录时的先后顺序？ | **天然兼容**——doAuth `_ai` 成功则不降级；失败才触发降级。无需额外处理 | `TokenAuthentication.scala:129` if (!ok) 才触发 |

---

## 4. 验收标准（三段式）【核心】

### AC1: 开关关闭时不触发降级

**前置条件**:
- 降级开关 `wds.linkis.gateway.ai.user.token.downgrade.enable=false`
- 网关正常运行，token 认证已启用（`enable.token.auth=true`）

**操作步骤**:
1. 使用任意用户（含 `_ai` 后缀用户，如 `zhangsan_ai`）携带有效但绑定用户不匹配的 token 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 认证失败报错行为与现网**完全一致**（noLogin + filterResponse）
- 降级代码零生效，`doAuth` 不被额外调用
- 日志中无 `token downgrade` 关键字

### AC2: 开关开启 + _ai 用户降级成功

**前置条件**:
- 降级开关 `wds.linkis.gateway.ai.user.token.downgrade.enable=true`
- `zhangsan_ai` 在 linkis_token 表无记录
- `zhangsan` 有有效 token 记录，token `TOKEN-A` 绑定 `zhangsan`

**操作步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带 `TOKEN-A` 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 首次 `doAuth(TOKEN-A, zhangsan_ai, host)` 失败
- 剥离后缀以 `zhangsan` 重试 `doAuth(TOKEN-A, zhangsan, host)` 一次且成功
- 请求按原成功路径放行，`setLoginUser` 写入 `zhangsan_ai`（身份保持 _ai）
- 日志记录降级事件（warn: 触发用户 `zhangsan_ai`、降级目标 `zhangsan`、URI、脱敏 token；info: 降级成功）

### AC3: 降级重试失败无第二次重试

**前置条件**:
- 降级开关开启
- `zhangsan_ai` 和 `zhangsan` 均无有效 token 记录

**操作步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带任意 token 发起请求
2. 观察网关认证行为和日志

**预期结果**:
- 首次 `doAuth(TOKEN-A, zhangsan_ai, host)` 失败
- 剥离后缀以 `zhangsan` 重试 `doAuth(TOKEN-A, zhangsan, host)` 一次且失败
- 按现网原失败路径返回认证失败（noLogin + filterResponse）
- **无第二次重试**，`doAuth` 总调用次数 = 2（首次 + 降级重试）

### AC4: 非 _ai 后缀用户不触发降级

**前置条件**:
- 降级开关开启
- 测试用户：`zhangsan`（无后缀）、`zhangsan_ai2`（非配置后缀）

**操作步骤**:
1. 以 `zhangsan` 为 Token-User，携带无效 token 发起请求
2. 以 `zhangsan_ai2` 为 Token-User，携带无效 token 发起请求
3. 观察网关认证行为和日志

**预期结果**:
- 两个请求均认证失败，行为与现网一致
- 降级逻辑不触发，`doAuth` 不被额外调用（仅首次调用）
- 日志中无 `token downgrade` 关键字

### AC5: 降级过程异常被捕获不传播

**前置条件**:
- 降级开关开启
- `zhangsan_ai` 在 linkis_token 表无记录

**操作步骤**:
1. 模拟降级重试 `doAuth` 抛出 `RuntimeException`
2. 以 `zhangsan_ai` 为 Token-User 发起请求
3. 观察网关认证行为和日志

**预期结果**:
- 降级异常被 `Utils.tryCatch` 捕获，返回 false
- 回退现网原报错路径，不产生新错误形态
- 异常被记录（warn 级别，含 `token downgrade` 关键字和异常堆栈）

### AC6: 日志 token 全程脱敏

**前置条件**:
- 降级开关开启
- 任意降级场景（成功/失败/异常）

**操作步骤**:
1. 触发降级各场景
2. 检查全部相关日志输出

**预期结果**:
- 全部日志中 token 均为 `TokenSensitiveUtils.maskToken` 脱敏形态
- 无明文 token 输出
- 日志包含触发用户、降级目标用户、URI、结果

### AC7: 并发请求独立无串扰

**前置条件**:
- 降级开关开启
- 30 个 `_ai` 用户（15 个降级成功、15 个降级失败）并发发起请求

**操作步骤**:
1. 30 个线程同时发起 `_ai` 用户请求（混合成功/失败凭据）
2. 等待全部完成（超时 30 秒）
3. 检查各请求认证结果

**预期结果**:
- 各请求认证结果独立正确（15 个成功、15 个失败）
- 无串扰，降级逻辑无共享可变状态
- `doAuth` 总调用次数 = 60（每请求 2 次：首次 + 降级重试）
- 无并发异常

### AC8: 动态 token 路径不降级

**前置条件**:
- 降级开关开启
- 动态 token 前缀路由已配置

**操作步骤**:
1. 以 `zhangsan_ai` 为 Token-User，携带动态 token（`dyn-` 前缀）发起请求
2. 观察网关认证行为

**预期结果**:
- 走 `dynamicTokenAuth` 原路径
- 降级逻辑不介入
- 行为与降级开关无关

### AC9: login 模式与 tokenAlive 各分支行为一致

**前置条件**:
- 降级开关开启
- 测试 login 模式（true/false）与 tokenAlive（true/false）的不同组合

**操作步骤**:
1. 降级开关开启，分别测试 login=true/false × tokenAlive=true/false 的 4 种组合
2. 降级成功后观察各分支行为

**预期结果**:
- 成功后各分支（setLoginUser 的 alive/非 alive、login 响应）行为与非降级成功路径一致
- 身份均为 `_ai` 用户（tokenUser）

### AC10: 开关热加载即时切换

**前置条件**:
- 网关运行中，降级开关当前为关闭状态

**操作步骤**:
1. 开关关闭时发起 `zhangsan_ai` 请求 → 认证失败
2. 不重启网关，通过修改配置将开关设为 `true`
3. 再次发起 `zhangsan_ai` 请求 → 降级成功
4. 不重启网关，将开关改回 `false`
5. 再次发起 `zhangsan_ai` 请求 → 认证失败

**预期结果**:
- 行为随开关即时正确切换（不重启）
- 开关关闭→开启→关闭各状态行为正确

### AC11: 后缀可配置

**前置条件**:
- 降级开关开启
- AI 后缀配置为 `_bot`（而非默认 `_ai`）

**操作步骤**:
1. 以 `zhangsan_bot` 为 Token-User 发起请求
2. 以 `zhangsan_ai` 为 Token-User 发起请求
3. 观察网关认证行为

**预期结果**:
- `zhangsan_bot` 触发降级，剥离后缀以 `zhangsan` 重试 doAuth
- `zhangsan_ai` 不触发降级（后缀不匹配配置的 `_bot`）
- 后缀配置化生效

### AC12: 畸形用户名不触发降级

**前置条件**:
- 降级开关开启
- AI 后缀为默认 `_ai`

**操作步骤**:
1. 以 `_ai`（后缀 only，剥离后原用户为空）为 Token-User 发起请求
2. 以 `a/b_ai`（含路径分隔符）为 Token-User 发起请求
3. 以 `a/../b_ai`（含 `..`）为 Token-User 发起请求
4. 以 `a b_ai`（含空白字符）为 Token-User 发起请求
5. 以 `a*b_ai`（含通配符）为 Token-User 发起请求
6. 观察网关认证行为

**预期结果**:
- 所有畸形用户名均不触发降级，`doAuth` 不被额外调用
- 行为与现网一致（认证失败）
- 日志记录跳过降级原因（warn: illegal original user）

---

## 5. 配置说明

### 5.1 新增配置项

| 配置Key | 默认值 | 说明 |
|---------|--------|------|
| `wds.linkis.gateway.ai.user.token.downgrade.enable` | `false` | `_ai` 用户 token 降级鉴权开关。开启后，`_ai` 后缀用户 token 认证失败时，剥离后缀用原用户 token 重试鉴权一次。使用 `getHotValue` 读取，支持不重启关停 |
| `wds.linkis.gateway.ai.user.suffix` | `_ai` | AI 用户后缀配置。可配置化，避免后缀变更时改代码。使用 `getHotValue` 读取 |

### 5.2 配置位置

- 模块配置类: `GatewayConfiguration.scala`（`linkis-gateway-core`）
- 部署配置文件: `linkis-mg-gateway.properties` 添加注释行

### 5.3 使用说明

```properties
# _ai 用户 token 降级鉴权开关（默认关闭）
# 开启后，_ai 后缀用户 token 认证失败时，剥离后缀用原用户 token 重试鉴权一次
# 适用于企金 MCP 通道 _ai 用户接入场景
# wds.linkis.gateway.ai.user.token.downgrade.enable=false

# AI 用户后缀配置（默认 _ai）
# 若 MCP 端切换使用其他后缀，可在此配置
# wds.linkis.gateway.ai.user.suffix=_ai
```

### 5.4 技术约束

| 项 | 内容 |
|----|------|
| 改动模块 | `linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core`（单模块） |
| 切入点 | `TokenAuthentication.tokenAuth` 的 doAuth 失败分支（`:118-123`）之后、filterResponse（`:164`）之前 |
| 高危等级 | 🔴 **网关鉴权主链路**（`enable.token.auth=true` 生产已启用，影响所有 token 请求）——按 CLAUDE.md §4 铁律执行：开关默认关 + `Utils.tryCatch` 保护 + 开关 on/off 双态冒烟 |
| 开关读取方式 | `getHotValue`（热加载，不重启关停） |
| 降级日志 | warn 级别记录降级触发和失败，info 级别记录降级成功；token 走 `TokenSensitiveUtils.maskToken` 脱敏 |
| 不触地雷区 | 不改 protocol、不改 `ENABLE_TOKEN_AUTHENTICATION`/`enable.token.auth` 语义、不改 `CachedTokenService.doAuth` 签名与抛错行为、不动 `SecurityFilter.filterResponse` |

### 5.5 可复用既有机制

| 机制 | 位置 | 用途 |
|------|------|------|
| `ProxyUserUtils` | linkis-gateway-core | 用户映射的既有实现形态参考（方向相反：proxy 是 um→proxy，降级是 `_ai`→原用户，纯后缀剥离更简） |
| `TokenSensitiveUtils.maskToken` | linkis-common | 降级日志脱敏（含单测） |
| `Utils.tryCatch` | linkis-common | 降级分支异常保护 |
| `CommonVars` / `getHotValue` | linkis-common | 开关声明与热加载读取 |

---

## 6. 关键代码位置参考

### 6.1 网关核心模块

| 文件 | 行号 | 说明 |
|------|------|------|
| `TokenAuthentication.scala` | `:55-184` | `tokenAuth` 主流程，降级逻辑切入点 |
| `TokenAuthentication.scala` | `:118-123` | doAuth 失败分支（降级切入点） |
| `TokenAuthentication.scala` | `:129-140` | **新增降级调用**，`Utils.tryCatch` 包裹 `tryAiUserTokenDowngrade` |
| `TokenAuthentication.scala` | `:186-255` | **新增 `tryAiUserTokenDowngrade` 方法**：开关检查、后缀剥离、合法性校验、重试 doAuth |
| `TokenAuthentication.scala` | `:186-187` | `DOWNGRADE_USER_MAX_LENGTH = 64` 常量 |
| `TokenAuthentication.scala` | `:189-190` | `ILLEGAL_DOWNGRADE_USER_CHARS = "\\/:*?\"<>|"` 常量 |
| `TokenAuthentication.scala` | `:262-267` | **新增 `isLegalDowngradeUser` 方法**：合法性校验（非空、≤64、不含 `..`、不含路径分隔符和空白） |
| `GatewayConfiguration.scala` | `:144-149` | **新增 `AI_USER_TOKEN_DOWNGRADE_ENABLE`** 开关（`getHotValue`） |
| `GatewayConfiguration.scala` | `:152-157` | **新增 `AI_USER_SUFFIX`** 后缀配置（`getHotValue`） |

### 6.2 关联代码

| 文件 | 行号 | 说明 |
|------|------|------|
| `CachedTokenService.scala` | `:215-241` | `doAuth` 方法（不修改，保持对外抛错语义不变） |
| `CachedTokenService.scala` | `:226-231` | `ILLEGAL_TOKENUSER` 抛出点（`_ai` 用户必命中） |
| `TokenSensitiveUtils.java` | 全文件 | token 脱敏工具（直接复用） |
| `ProxyUserUtils.scala` | 全文件 | 用户映射参考（方向相反） |

### 6.3 单元测试

| 文件 | 说明 |
|------|------|
| `TokenAuthenticationAiDowngradeTest.scala` | 8 个测试方法，覆盖 AC1-AC7 + AC11（后缀可配置）+ AC12（畸形用户名） |

| 测试方法 | 覆盖验收标准 | 说明 |
|---------|------------|------|
| `switchOffNoDowngrade` | AC1 | 开关关闭 → 不降级，doAuth 不重试 |
| `downgradeSuccess` | AC2 | 开关开启 + `zhangsan_ai` → 重试 `zhangsan` 成功 |
| `downgradeRetryFails` | AC3 | 重试失败 → false，单次重试无第二次 |
| `nonAiUserNotTriggered` | AC4 | 非 `_ai` 后缀用户不触发降级 |
| `downgradeExceptionCaught` | AC5 | doAuth 抛异常 → 捕获返回 false，不传播 |
| `malformedOriginalUser` | AC12 | 畸形用户名（空、路径分隔符、`..`、空白、通配符）跳过降级 |
| `concurrentDowngrade` | AC7 | 30 线程并发降级，无串扰 |
| `configurableSuffix` | AC11 | 后缀可配置为 `_bot`，`_ai` 不再触发 |

---

## 7. 风险识别【参考】

### 7.1 实现风险

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| 网关鉴权主链路改动，影响面=所有 token 请求 | 🔴 | 开关默认关；降级仅在"失败路径 + `_ai` 后缀"窄口触发；AC1/AC5/AC7 兜底；上线前开关 on/off 双态全量回归 token 认证 |
| 放行身份语义选错 → 隔离失效或下游错乱 | 🔴 | Q1 已定：身份保持 `_ai`（选项 A），代码注释 `:200` 明确 "login identity is NOT rewritten" |
| token 失效场景降级误放行 | 🟡 | Q5 已定：统一触发，不做异常分支过滤；由 `CachedTokenService.doAuth` 自行判断 token 有效性 |

### 7.2 发布风险

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| 与 MCP 端开关灰度顺序 | 🟡 | 建议网关降级开关先开（或同时），MCP 侧切换后开——避免 `_ai` 请求到达而降级未就绪导致全部认证失败（发布计划对齐，实现各自独立） |
| 回滚方案 | 🟢 | 开关热加载（`getHotValue`），设置 `enable=false` 即可即时回退，无需重启 |

### 7.3 业务风险

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| `_ai` 任务整体可用性依赖账号开通 | 🟡 | 引擎以 `<user>_ai` 拉起需 Linux 代理/HDFS/Kerberos 就绪。**非本需求开发范围**（属原 story 整体/MCP 端配套），但联调验收前置，在此留痕提醒 |

---

## 8. 关联影响分析【参考】

### 8.1 功能模块影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 在 linkis-gateway-core 的 `TokenAuthentication` 中新增降级逻辑，在 `GatewayConfiguration` 中新增 2 个配置项，不影响其他模块 |

### 8.2 数据模型影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 不涉及数据库表结构变更，`linkis_token` 表无改动 |

### 8.3 安全与权限影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 降级不是认证旁路：触发条件三重限定（开关+后缀+失败）、单次重试、凭据仍属原用户；畸形用户名校验防构造绕过；token 日志全链路脱敏 |

### 8.4 用户体验与文案影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 无前端变更，用户无感知（仅日志中增加降级记录） |

### 8.5 上下游与三方依赖影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 不影响上下游系统。MCP 端独立实现降级，网关侧改动自包含 |

---

## 9. 发布和回滚方案【重要】

### 9.1 发布策略

- **发布方式**: 灰度发布
- **灰度步骤**:
  1. 先在测试环境（BDAP-DEV）部署并验证 AC1-AC12 全部
  2. 在 SIT/UAT 环境验证 `_ai` 通道端到端
  3. 生产环境灰度企金环境，开关默认 `false`
  4. 手动设置 `wds.linkis.gateway.ai.user.token.downgrade.enable=true` 开启降级
- **监控指标**:
  - 网关 token 认证成功率
  - 降级触发次数和成功率（日志关键字 `token downgrade`）
  - 降级失败次数
  - 非降级请求认证成功率（确认无影响）
- **发布窗口**: 非业务高峰期

### 9.2 回滚方案

- **回滚条件**: 开启降级后出现认证异常率上升或其他回归问题
- **回滚步骤**:
  1. 设置 `wds.linkis.gateway.ai.user.token.downgrade.enable=false`
  2. **无需重启网关**（`getHotValue` 热加载即时生效）
  3. 行为回退到降级前状态
- **回滚时间**: 配置修改即时生效，约 1 分钟
- **回滚预案**: 无需代码回滚，仅通过配置开关即可降级

---

## 10. 防范措施【重要】

### 10.1 测试改进

- [x] 补充单元测试：`TokenAuthenticationAiDowngradeTest.scala`（8 个测试方法，覆盖 AC1-AC7 + AC11 + AC12）
- [x] 增加边界测试：畸形用户名（空、路径分隔符、`..`、空白、通配符）跳过降级
- [x] 增加并发测试：30 线程并发降级，无串扰
- [ ] 补充集成测试：MCP 端到端 `_ai` 通道验证

### 10.2 监控改进

- [ ] 增加监控指标：降级触发次数和成功率
- [ ] 增加告警规则：降级失败率超阈值告警
- [x] 增加日志输出：降级触发（warn）、降级成功（info）、降级失败（warn）、畸形用户名跳过（warn）

### 10.3 流程改进

- [x] 代码规范强化：降级逻辑用 `Utils.tryCatch` 包裹，开关默认 `false`，符合 CLAUDE.md §4 铁律
- [x] Code Review 检查点：网关鉴权链路改动需 on/off 双态冒烟
- [ ] 发布流程优化：与 MCP 端开关灰度顺序对齐

---

## 11. 非功能性需求【重要】

| 类别 | 要求 |
|------|------|
| **安全** | ① 降级不是认证旁路：触发条件三重限定（开关+后缀+失败）、单次重试、凭据仍属原用户；② 剥离后缀后校验原用户名合法性（非空、≤64、不含 `..`、不含路径分隔符 `\/:*?"<>|` 和空白字符），防畸形用户名构造；③ token 日志全链路脱敏；④ 不打印其他用户敏感信息 |
| **性能** | 新增逻辑位于**认证失败路径**（正常流量不经过）；单次后缀判断 + 至多一次 DB 缓存查询重试，O(1)；对现网正常请求零开销 |
| **兼容性** | 开关默认关闭时与现网行为逐字节一致；非 `_ai` 用户零影响；动态 token 路径零影响；不修改任何公开 API 签名、protocol、`CachedTokenService` 对外行为 |
| **可运维** | 开关注释行随包发布；支持热加载（`getHotValue`）不重启关停；降级成功/失败/异常均有独立可检索日志关键字（`token downgrade`） |
| **审计** | 降级事件四要素可查：触发用户（`_ai`）、降级目标用户、结果、URI |

---

## 附录

### A.1 已核实代码锚点清单（2026-09-24，分支 dev-2.1.4-webank）

| 锚点 | 位置 | 核实结论 |
|------|------|---------|
| tokenAuth 主流程 | `linkis-gateway-core/.../security/token/TokenAuthentication.scala:55-184` | 完整流程见 §1.2 流程图 |
| doAuth 失败分支（降级切入点） | `TokenAuthentication.scala:118-123` + `:164` | 失败即 noLogin + filterResponse，无降级路径 |
| **降级调用**（新增） | `TokenAuthentication.scala:129-140` | `Utils.tryCatch` 包裹 `tryAiUserTokenDowngrade` |
| **tryAiUserTokenDowngrade 方法**（新增） | `TokenAuthentication.scala:186-255` | 开关检查、后缀剥离、合法性校验、重试 doAuth |
| **isLegalDowngradeUser 方法**（新增） | `TokenAuthentication.scala:262-267` | 合法性校验（非空、≤64、不含 `..`、不含路径分隔符和空白） |
| 放行身份语义 | `TokenAuthentication.scala:150-157` | `GatewaySSOUtils.setLoginUser(request, tokenUser)`——身份=tokenUser（保持 _ai，Q1 已定） |
| 动态 token 路由 | `TokenAuthentication.scala:110-113` | 前缀路由不走静态 doAuth（AC8 依据） |
| token 双来源（Header/Cookie） | `TokenAuthentication.scala:62-93` | 降级判断在提取之后，两来源均覆盖 |
| `ILLEGAL_TOKENUSER` 抛出点 | `linkis-gateway-authentication/.../CachedTokenService.scala:226-231` | `_ai` 用户直查必命中；不改此层 |
| **降级开关配置**（新增） | `linkis-gateway-core/.../config/GatewayConfiguration.scala:144-149` | `AI_USER_TOKEN_DOWNGRADE_ENABLE`，`getHotValue` 读取 |
| **AI 后缀配置**（新增） | `linkis-gateway-core/.../config/GatewayConfiguration.scala:152-157` | `AI_USER_SUFFIX`，默认 `_ai`，`getHotValue` 读取 |
| 开关声明与命名惯例 | `linkis-gateway-core/.../config/GatewayConfiguration.scala:25/37/40...` | `wds.linkis.gateway.*.enable` 惯例（ENABLE_PROXY_USER 等） |
| 用户映射参考 | `linkis-gateway-core/.../security/ProxyUserUtils.scala` | 反方向映射机制，实现形态可参考 |
| token 脱敏工具 | `linkis-commons/linkis-common/.../utils/TokenSensitiveUtils.java` | 直接复用（含单测） |
| **单元测试**（新增） | `linkis-gateway-core/src/test/.../token/TokenAuthenticationAiDowngradeTest.scala` | 8 个测试方法，覆盖 AC1-AC7 + AC11 + AC12 |

### A.2 修改文件清单

| 序号 | 文件路径 | 修改类型 | 说明 |
|:----:|---------|:--------:|------|
| 1 | `linkis-gateway-core/.../security/token/TokenAuthentication.scala` | 修改 | 新增 `tryAiUserTokenDowngrade` + `isLegalDowngradeUser` 方法 + doAuth 失败分支降级调用 |
| 2 | `linkis-gateway-core/.../config/GatewayConfiguration.scala` | 修改 | 新增 `AI_USER_TOKEN_DOWNGRADE_ENABLE` + `AI_USER_SUFFIX` 配置项 |
| 3 | `linkis-gateway-core/src/test/.../token/TokenAuthenticationAiDowngradeTest.scala` | 新增 | 单元测试，8 个测试方法 |
| 4 | `linkis-dist/package/conf/linkis-mg-gateway.properties` | 修改 | 添加降级开关注释行 + AI 后缀配置注释行 |

### A.3 版本演进说明

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-09-21 | 初稿：覆盖 MCP 切换 + 语句校验 + 网关降级 + 文件访问全量（`mcp-ai-user-switch_需求.md`，已删） |
| v2.0 | 2026-09-21 | 排除 MCP 部分，保留 Linkis 侧三 FR（`linkis-ai-user-support_需求.md`，已删） |
| v3.0 | 2026-09-21 | 语句校验划归 MCP 端、文件访问本版本不做——收敛为网关 token 降级单 FR（含 6 个开放问题 Q1-Q6） |
| v4.0 | 2026-09-24 | **标准格式稿**：Q1-Q6 全部关闭，代码锚点已核实实现完毕，验收标准改为三段式，补充配置说明和关键代码位置参考 |

### A.4 关联文档

- DPMS Story #548689（需求原文；本文档为其 Linkis 网关认证子集）
- CLAUDE.md §4（新功能铁律：开关默认关 + tryCatch + 双态冒烟）
- `docs/LinkisV2.1.4/design/REQ-01_支持_ai用户鉴权_设计.md`（设计文档）
- `docs/dev-2.1.3/requirements/fix-hdfs-tgt-expired_需求.md`（文档格式参照）
