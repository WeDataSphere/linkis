# 支持_ai用户鉴权 测试澄清纪要

> **需求编号**: REQ-01
> **需求名称**: 支持_ai用户鉴权
> **版本**: LinkisV2.1.4
> **澄清日期**: 2026-10-08
> **就绪度**: 中
> **澄清轮次**: 4（降级推导，非交互式环境）
> **完成度**: 0.88
> **门控通过**: 是（P0 覆盖率 >= 85%）

---

## 一、被测对象清单

### 1.1 接口/方法清单

| 编号 | 被测对象 | 类型 | 所在文件 | 关键逻辑 |
|------|---------|------|---------|---------|
| O-1 | `TokenAuthentication.tryAiUserTokenDowngrade` | 私有方法（package-private） | `TokenAuthentication.scala:214-255` | 降级主逻辑：开关检查 -> 后缀检查 -> 剥离 -> 合法性校验 -> 重试 doAuth |
| O-2 | `TokenAuthentication.isLegalDowngradeUser` | 私有方法 | `TokenAuthentication.scala:262-267` | 用户名合法性校验（非空/长度<=64/路径穿越/空白/非法字符） |
| O-3 | `TokenAuthentication.tokenAuth`（降级分支） | 公开方法 | `TokenAuthentication.scala:129-140` | doAuth 失败后插入降级调用，成功走原路径，失败走原失败路径 |
| O-4 | `GatewayConfiguration.AI_USER_TOKEN_DOWNGRADE_ENABLE` | 配置项 | `GatewayConfiguration.scala:157-158` | 降级开关，getHotValue 热加载，默认 false |
| O-5 | `GatewayConfiguration.AI_USER_SUFFIX` | 配置项 | `GatewayConfiguration.scala:164` | AI 后缀配置，默认 _ai，getHotValue 热加载 |

### 1.2 功能点清单

| 编号 | 功能点 | 验收标准 |
|------|--------|---------|
| F-1 | 降级开关 | AC1（关闭时不触发）、AC10（热加载即时切换） |
| F-2 | AI 后缀配置 | AC11（后缀可配置为 _bot） |
| F-3 | 降级触发判定 | AC1、AC2、AC4（非 _ai 后缀不触发） |
| F-4 | 后缀剥离与合法性校验 | AC12（畸形用户名不触发降级） |
| F-5 | 降级重试 doAuth | AC2（成功）、AC3（失败无第二次重试） |
| F-6 | 降级成功后放行身份 | AC2、AC9（身份保持 _ai 用户） |
| F-7 | 降级失败兜底 | AC3（走现网原失败路径） |
| F-8 | 降级全程异常保护 | AC5（异常被捕获不传播） |
| F-9 | 降级日志审计 | AC6（token 全程脱敏） |
| F-10 | 动态 token 路径不降级 | AC8（动态 token 不介入降级） |
| F-11 | 并发独立无串扰 | AC7（30 线程并发） |
| F-12 | login 模式与 tokenAlive 各分支 | AC9（4 种组合行为一致） |

### 1.3 Scenario 清单（来自需求文档 AC1-AC12）

| AC 编号 | 场景 | 单测覆盖 | 集成测试覆盖 |
|---------|------|:--------:|:----------:|
| AC1 | 开关关闭时不触发降级 | switchOffNoDowngrade | IT-01 |
| AC2 | 开关开启 + _ai 用户降级成功 | downgradeSuccess | IT-02 |
| AC3 | 降级重试失败无第二次重试 | downgradeRetryFails | IT-03 |
| AC4 | 非 _ai 后缀用户不触发降级 | nonAiUserNotTriggered | IT-04 |
| AC5 | 降级过程异常被捕获不传播 | downgradeExceptionCaught | - |
| AC6 | 日志 token 全程脱敏 | -（需人工/日志验证） | - |
| AC7 | 并发请求独立无串扰 | concurrentDowngrade | - |
| AC8 | 动态 token 路径不降级 | - | IT-05 |
| AC9 | login 模式与 tokenAlive 各分支行为一致 | - | IT-07 |
| AC10 | 开关热加载即时切换 | - | IT-06 |
| AC11 | 后缀可配置 | configurableSuffix | - |
| AC12 | 畸形用户名不触发降级 | malformedOriginalUser | - |

---

## 二、被测范围（testScope）

### 必测项（mustTest）

| 编号 | 测试项 | 验收标准 | 来源 |
|------|--------|---------|------|
| MT-01 | 开关关闭时不触发降级，doAuth 不重试 | AC1 | requirement |
| MT-02 | 开关开启 + zhangsan_ai 降级成功，身份保持 _ai | AC2 | requirement |
| MT-03 | 降级重试失败，无第二次重试，走原失败路径 | AC3 | requirement |
| MT-04 | 非 _ai 后缀用户不触发降级 | AC4 | requirement |
| MT-05 | 降级异常被 Utils.tryCatch 捕获，返回 false，不传播 | AC5 | requirement |
| MT-06 | 畸形用户名校验（空/路径分隔/.. /空白/通配符）跳过降级 | AC12 | requirement |
| MT-07 | 并发 30 线程降级，无串扰，各请求结果独立正确 | AC7 | requirement |
| MT-08 | 后缀可配置（_bot 触发，_ai 不再触发） | AC11 | requirement |
| MT-09 | 降级日志 token 全程脱敏（maskToken） | AC6 | requirement |
| MT-10 | 动态 token 路径不走降级（前缀路由不介入） | AC8 | requirement |

### 选测项（optionalTest）

| 编号 | 测试项 | 验收标准 | 来源 |
|------|--------|---------|------|
| OT-01 | 开关热加载即时切换（关闭->开启->关闭） | AC10 | requirement |
| OT-02 | login 模式 x tokenAlive 的 4 种组合行为一致 | AC9 | requirement |
| OT-03 | 集成测试 IT-01: 开关关闭 + _ai 用户请求 | AC1 | design_doc |
| OT-04 | 集成测试 IT-02: 开关开启 + zhangsan_ai 降级成功 | AC2 | design_doc |
| OT-05 | 集成测试 IT-03: 降级失败 + 原用户 token 也无效 | AC3 | design_doc |
| OT-06 | 集成测试 IT-04: 非 _ai 用户认证失败 | AC4 | design_doc |
| OT-07 | 集成测试 IT-05: 动态 token + _ai 用户 | AC8 | design_doc |
| OT-08 | 集成测试 IT-06: 开关运行时切换 | AC10 | design_doc |
| OT-09 | 集成测试 IT-07: login 模式 + tokenAlive | AC9 | design_doc |

### 不测项（notTest）

| 编号 | 测试项 | 原因 |
|------|--------|------|
| NT-01 | MCP 端 _ai 用户切换动作 | 需求范围外，MCP 端独立仓库实现 |
| NT-02 | 语句权限校验 | 需求范围外，由 MCP 端处理 |
| NT-03 | 共享目录文件访问 | 本版本不做 |
| NT-04 | 前端 UI 测试 | 功能属性为后端，不涉及前端 |
| NT-05 | CachedTokenService.doAuth 内部逻辑 | 不修改此层，对外行为保持不变 |
| NT-06 | SecurityFilter.filterResponse 行为 | 不修改，走现网原路径 |

---

## 三、远端环境信息（environmentInfo）

| 环境 | 地址 | 可用性 | VPN 要求 |
|------|------|--------|---------|
| BDAP-DEV（测试） | linkis-mg-gateway 服务地址（需部署后确认，参考 `wds.linkis.gateway.address` 配置） | 需提测后可用 | 需内网/VPN 访问 |
| SIT/UAT | 同上 SIT/UAT 环境地址 | 需提测后可用 | 需内网/VPN 访问 |
| 生产 | 生产环境 linkis-mg-gateway 地址 | 灰度发布时可用 | 需内网/VPN 访问 |

> **降级说明**: 远端环境具体地址和端口需在提测后由运维提供。单元测试不依赖远端环境（使用 Mockito mock TokenService）。集成测试 IT-01~IT-07 需要实际的 linkis-mg-gateway 服务实例。

---

## 四、登录与鉴权

| 维度 | 内容 |
|------|------|
| **登录方式** | Token 认证（Header/Cookie 携带 Token-Code + Token-User） |
| **token 获取接口** | 网关 tokenAuth 接口，Header 携带 `Token-Code` 和 `Token-User` |
| **token 有效期** | 由 linkis_token 表配置，CachedTokenService 校验 |
| **测试账号来源** | 需在 linkis_token 表中预置测试用户（如 zhangsan）的 token 记录 |
| **_ai 用户特殊** | zhangsan_ai 不需要在 linkis_token 表中有记录，降级后复用 zhangsan 的 token |
| **账号权限矩阵** | 测试用户需具备基本的网关访问权限；_ai 用户身份保持，引擎以 _ai 用户拉起（需 Linux/HDFS/Kerberos 就绪，属联调验收前置，非本需求开发范围） |
| **token 刷新机制** | 静态 token 无自动刷新；getHotValue 读取开关支持运行时关停 |

---

## 五、测试数据策略（dataStrategy）

> **澄清状态**: 降级推导（非交互式环境，基于需求文档和设计文档自动推导）

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|---------|------|---------|
| zhangsan 用户 + TOKEN-A token 记录 | SQL 插入 linkis_token 表（INSERT INTO linkis_token...） | linkis_token 表可写 | 测试后 DELETE 对应 token 记录 |
| zhangsan_ai 用户（无需 token 记录） | 无需构造（表无记录是降级前提） | - | - |
| failuser + failuser_ai 用户（无有效 token） | 确保表中无记录或 token 已过期 | - | - |
| 30 个并发测试用户（15 成功 + 15 失败） | SQL 批量插入 15 个成功用户的 token 记录 | linkis_token 表可写 | 测试后批量 DELETE |
| 畸形用户名（_ai、a/b_ai、a/../b_ai、a b_ai、a*b_ai） | 无需构造数据（降级校验在 doAuth 之前） | - | - |
| 动态 token（dyn- 前缀） | 需配置 DynamicTokenService | DynamicTokenService 已配置 | - |
| 后缀 _bot 配置 | System.setProperty 或修改 linkis-mg-gateway.properties | - | 测试后恢复默认 _ai |

**单测数据策略**: 单元测试使用 Mockito mock TokenService，不依赖数据库。开关和后缀通过 System.setProperty 控制（getHotValue 读 Java 系统属性），@BeforeEach/@AfterEach 清理。

**集成测试数据策略**: 需在 linkis_token 表预置 token 记录，通过 SQL 脚本构造。

---

## 六、边界与异常约定

| 边界/异常 | 约定 | 验收标准 |
|----------|------|---------|
| 空值/null | token 或 tokenUser 为空时走现网原失败路径（noLogin） | 既有逻辑覆盖 |
| _ai 后缀 only（剥离后空） | isLegalDowngradeUser 返回 false，跳过降级 | AC12 |
| 路径分隔符（\/:*?"<>\|） | isLegalDowngradeUser 返回 false，跳过降级 | AC12 |
| 路径穿越（..） | isLegalDowngradeUser 返回 false，跳过降级 | AC12 |
| 空白字符 | isLegalDowngradeUser 返回 false，跳过降级 | AC12 |
| 用户名超长（>64） | isLegalDowngradeUser 返回 false，跳过降级 | AC12 |
| 非 _ai 后缀用户 | endsWith 检查失败，返回 false，不触发降级 | AC4 |
| doAuth 抛 RuntimeException | Utils.tryCatch 捕获，返回 false，不传播 | AC5 |
| 降级逻辑自身异常 | 外层 Utils.tryCatch 捕获，ok=false，走原失败路径 | AC5 |
| 原用户 token 过期/禁用 | 统一触发降级，重试自然失败（Q5 决策） | AC3 |
| 并发请求 | 无共享可变状态，各请求独立 | AC7 |
| 降级重试失败 | 无第二次重试，doAuth 总调用=2 | AC3 |

---

## 七、接口契约约定

| 维度 | 约定 |
|------|------|
| **鉴权方式** | Token 认证（Header: Token-Code + Token-User；或 Cookie 同名） |
| **限流/熔断** | 网关既有机制，本需求不涉及 |
| **幂等性** | 降级逻辑无副作用，纯 Boolean 返回 |
| **版本兼容** | 开关默认关闭时与现网行为逐字节一致；不修改任何公开 API 签名 |
| **字段必填** | Token-Code 和 Token-User 必须同时存在（Header 或 Cookie） |
| **动态 token 前缀** | dyn- 前缀走 dynamicTokenAuth，不走静态 doAuth 降级 |
| **配置热加载** | getHotValue 读取，支持运行时关停不重启 |
| **token 脱敏** | 全部日志中 token 均经 TokenSensitiveUtils.maskToken 脱敏 |
| **日志关键字** | `token downgrade`（warn: 触发/失败/异常/非法用户；info: 成功） |

---

## 八、澄清问答记录（clarificationLog）

> **降级说明**: 本纪要在非交互式环境中生成，以下问答基于需求文档和设计文档已有信息自动推导。

| 轮次 | 维度 | 问题 | 回答（推导） |
|:----:|------|------|------------|
| 1 | 远端环境 | BDAP-DEV/SIT 环境的网关地址和端口？是否需要 VPN？ | 需求文档发布策略明确 BDAP-DEV -> SIT/UAT -> 生产灰度。具体地址需提测后由运维提供。需内网/VPN 访问。单元测试不依赖远端环境。 |
| 2 | 登录/鉴权 | 测试账号来源？token 记录如何插入？ | Token 认证方式，Header 携带 Token-Code + Token-User。测试用户（如 zhangsan）需在 linkis_token 表预置 token 记录。_ai 用户无需表记录（降级前提）。 |
| 3 | 测试数据 | 需要哪些用户和 token 记录？造数方式？清理策略？ | 单测使用 Mockito mock，不依赖数据库。集成测试需 SQL 插入 linkis_token 表。需构造：zhangsan+TOKEN-A（成功）、failuser_ai（失败）、30 并发用户、畸形用户名（无需数据）。测试后 DELETE 清理。 |
| 4 | 被测范围 | 集成测试是否纳入？回归范围？ | 必测项 MT-01~MT-10 对应 AC1-AC8+AC11+AC12。选测项 OT-01~OT-09 对应 AC9-AC10 和集成测试 IT-01~IT-07。不测项 NT-01~NT-06 为需求范围外内容。回归范围：非 _ai 用户 token 认证、动态 token 认证、Cookie 来源 token、开关 on/off 双态全量回归。 |

---

## 九、待补充项（qualityWarnings）

| 编号 | 待补充项 | 影响 | 建议处理 |
|------|---------|------|---------|
| W-01 | 远端环境具体地址和端口未确认 | 集成测试 IT-01~IT-07 需实际网关服务 | 提测后由运维提供地址，在集成测试前补入 |
| W-02 | 测试账号的 token 记录具体值未确认 | 集成测试需预置 linkis_token 表数据 | 测试前通过 SQL 脚本构造，需 DBA 配合 |
| W-03 | AC6（日志 token 脱敏）和 AC9（login/tokenAlive 分支）未被单测覆盖 | 日志脱敏需人工或日志断言验证；AC9 需集成测试 | 建议在集成测试中补充日志断言和分支测试 |
| W-04 | AC10（开关热加载即时切换）未被单测覆盖 | 热加载行为需实际运行验证 | 建议在集成测试 IT-06 中验证 |
| W-05 | 造数策略为降级推导，未经用户确认 | 集成测试数据构造方法可能与实际环境不符 | 建议集成测试前与 DBA/运维确认造数方式 |
| W-06 | 并发测试（AC7）在单测中使用 RecordingTokenService mock，未验证真实 doAuth 并发安全性 | 单测验证降级逻辑无共享状态，但不覆盖 CachedTokenService 并发 | 可接受（CachedTokenService 非本需求改动范围） |

---

## 十、testScope 结构化摘要

```json
{
  "mustTest": [
    {"title": "开关关闭时不触发降级，doAuth不重试", "source": "requirement", "risk_ids": []},
    {"title": "开关开启+zhangsan_ai降级成功，身份保持_ai", "source": "requirement", "risk_ids": []},
    {"title": "降级重试失败，无第二次重试，走原失败路径", "source": "requirement", "risk_ids": []},
    {"title": "非_ai后缀用户不触发降级", "source": "requirement", "risk_ids": []},
    {"title": "降级异常被Utils.tryCatch捕获，返回false，不传播", "source": "requirement", "risk_ids": []},
    {"title": "畸形用户名校验（空/路径分隔/../空白/通配符）跳过降级", "source": "requirement", "risk_ids": []},
    {"title": "并发30线程降级，无串扰，各请求结果独立正确", "source": "requirement", "risk_ids": []},
    {"title": "后缀可配置（_bot触发，_ai不再触发）", "source": "requirement", "risk_ids": []},
    {"title": "降级日志token全程脱敏（maskToken）", "source": "requirement", "risk_ids": []},
    {"title": "动态token路径不走降级（前缀路由不介入）", "source": "requirement", "risk_ids": []}
  ],
  "optionalTest": [
    {"title": "开关热加载即时切换（关闭->开启->关闭）", "source": "requirement", "risk_ids": []},
    {"title": "login模式xtokenAlive的4种组合行为一致", "source": "requirement", "risk_ids": []},
    {"title": "集成测试IT-01~IT-07", "source": "design_doc", "risk_ids": []}
  ],
  "notTest": [
    {"title": "MCP端_ai用户切换动作", "source": "requirement", "risk_ids": []},
    {"title": "语句权限校验", "source": "requirement", "risk_ids": []},
    {"title": "共享目录文件访问", "source": "requirement", "risk_ids": []},
    {"title": "前端UI测试", "source": "requirement", "risk_ids": []},
    {"title": "CachedTokenService.doAuth内部逻辑", "source": "design_doc", "risk_ids": []},
    {"title": "SecurityFilter.filterResponse行为", "source": "design_doc", "risk_ids": []}
  ]
}
```

---

## 十一、environmentInfo 结构化摘要

```json
{
  "remoteUrl": "需提测后由运维提供（BDAP-DEV/SIT/UAT环境linkis-mg-gateway地址）",
  "availability": "需提测后可用",
  "vpnRequired": true
}
```

---

## 十二、降级推导说明

本纪要在非交互式环境中生成（AskUserQuestion 工具不可用），基于以下高置信度来源自动推导：

1. **需求文档**（v4.0 标准格式稿，Q1-Q6 全部关闭）：AC1-AC12 全覆盖，测试策略、发布策略、回滚方案明确
2. **设计文档**（v1.0）：单测场景 AC1-AC7+Q2 覆盖矩阵、集成测试 IT-01~IT-07 场景、回归测试范围
3. **代码变更**（commit 9544feead）：4 个文件，+389/-1 行，单测 8 个方法已实现
4. **context.md**：无 stabilityRisks，无风险测试增量

推导置信度：高（需求文档和设计文档详尽，代码已实现完毕，单测已覆盖 8/12 AC）

用户可在后续阶段对以下项进行人工校准：
- 远端环境地址和端口（W-01）
- 测试账号和 token 记录具体值（W-02）
- 造数方式确认（W-05）
