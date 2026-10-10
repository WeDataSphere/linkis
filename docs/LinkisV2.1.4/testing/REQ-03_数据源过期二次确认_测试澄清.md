# REQ-03 管理台数据源过期按钮二次确认 测试澄清纪要

**需求编号**: REQ-03
**需求名称**: 管理台数据源失效按钮增加二次确认，防误触
**功能属性**: 前端
**前端类型**: 纯前端（复用已有 expire 接口，无后端改动）
**澄清日期**: 2026-10-08
**就绪度**: 中
**完成度**: 0.85
**轮次**: 4
**门控通过**: 是

---

## 一、被测对象清单

### 1.1 功能点清单

| 编号 | 功能点 | 验收标准 | 代码位置 |
|------|--------|----------|----------|
| F-1 | 二次确认 Modal | AC1 | `datasource/index.vue` template 新增 `<Modal v-model="overdueConfirm.show">` |
| F-2 | 状态管理 `overdueConfirm` | AC1 | `datasource/index.vue` data 新增 `{ show, id, name }` 响应式对象 |
| F-3 | `overdue` 方法改造 | AC1/AC2 | `datasource/index.vue` methods.overdue 改为设置 confirm 状态，不直接调 expire |
| F-4 | `confirmOverdue` 方法 | AC3 | `datasource/index.vue` methods.confirmOverdue 关闭 Modal -> 调 expire -> 成功提示 -> 刷新列表 |
| F-5 | i18n 双语 | AC4 | `zh.json`/`en.json` 新增 5 个 i18n key |

### 1.2 验收标准清单

| 编号 | 验收标准 | 前置条件 |
|------|----------|----------|
| AC1 | 点击过期按钮弹出确认框，含数据源名称、"此操作不可恢复"提示、确认+取消按钮 | 数据源列表有数据 |
| AC2 | 点击取消，Modal 关闭，不调用 expire 接口，列表不变 | AC1 前置 |
| AC3 | 点击确认，Modal 关闭，调用 expire(id)，成功后提示"数据源已过期"并刷新列表 | AC1 前置 |
| AC4 | 中英文切换后 Modal 文案正确显示 | 管理台支持中英文切换 |

### 1.3 接口清单

| 接口 | 方法 | 说明 | 变更状态 |
|------|------|------|----------|
| `expire(id)` | API 调用 | 使数据源失效（过期） | 未变更，复用已有接口 |

### 1.4 Scenario 清单（从需求文档推导）

| Scenario | 对应 AC | 描述 |
|----------|---------|------|
| S-1 | AC1 | Given 数据源列表有数据，When 点击"过期"按钮，Then 弹出 Modal 显示数据源名称和不可恢复提示 |
| S-2 | AC2 | Given 确认框已弹出，When 点击"取消"，Then Modal 关闭且不调用 expire |
| S-3 | AC3 | Given 确认框已弹出，When 点击"确认"，Then 调用 expire 接口并刷新列表 |
| S-4 | AC4 | Given 切换语言为英文，When 点击"过期"按钮，Then Modal 显示英文文案 |

### 1.5 代码变更清单

| 文件 | 修改类型 | 行数变化 | 说明 |
|------|:--------:|----------|------|
| `linkis-web/src/apps/linkis/module/datasource/index.vue` | 修改 | +28/-4 | Modal + overdueConfirm + confirmOverdue + overdue 改造 |
| `linkis-web/src/apps/linkis/i18n/common/zh.json` | 修改 | +7/-1 | 5 个 i18n key（含格式修正） |
| `linkis-web/src/apps/linkis/i18n/common/en.json` | 修改 | +7/-1 | 5 个 i18n key（含格式修正） |
| `EnvConfiguration.scala` | 修改 | +1 | 空行（无逻辑变更） |
| `TestContextHAManager.java` | 修改 | +5/-8 | Jetty 配置重构注释（无逻辑变更） |

> 注：EnvConfiguration.scala 和 TestContextHAManager.java 的变更与本需求核心功能无关，为 commit 中的附带修改。测试范围聚焦 datasource/index.vue 和 i18n 文件。

---

## 二、被测范围（testScope）

### 必测项（mustTest）

| 序号 | 测试项 | 对应 AC | 来源 | 测试类型 |
|------|--------|---------|------|----------|
| 1 | 点击"过期"按钮弹出二次确认 Modal | AC1 | acceptance_criteria | 前端 UI 交互 |
| 2 | Modal 内容包含数据源名称和"此操作不可恢复"提示 | AC1 | acceptance_criteria | 前端 UI 断言 |
| 3 | Modal 包含"确认"和"取消"两个按钮 | AC1 | acceptance_criteria | 前端 UI 断言 |
| 4 | 点击"取消"按钮关闭 Modal，不调用 expire 接口 | AC2 | acceptance_criteria | 前端 UI 交互 + 网络断言 |
| 5 | 点击"确认"按钮关闭 Modal 并调用 expire(id) 接口 | AC3 | acceptance_criteria | 前端 UI 交互 + 网络断言 |
| 6 | expire 成功后提示"数据源已过期"并刷新列表 | AC3 | acceptance_criteria | 前端 UI 断言 |
| 7 | 中文环境下 Modal 文案正确显示 | AC4 | acceptance_criteria | 前端 i18n 断言 |
| 8 | 英文环境下 Modal 文案正确显示 | AC4 | acceptance_criteria | 前端 i18n 断言 |
| 9 | `overdueConfirm` 状态对象正确管理 show/id/name | F-2 | feature_scenario | 前端状态断言 |
| 10 | `overdue` 方法不再直接调用 expire | F-3 | feature_scenario | 前端逻辑断言 |

### 选测项（optionalTest）

| 序号 | 测试项 | 说明 | 来源 |
|------|--------|------|------|
| 1 | 数据源名称为空时 Modal 内容处理 | 设计文档 §4 异常与降级：占位符替换为空字符串 | user_decision |
| 2 | expire 接口失败时错误提示 | 设计文档 §4：axios 拦截器统一处理，Modal 已关闭 | user_decision |
| 3 | 连续快速点击"过期"按钮多次 | 防止多次弹出 Modal 的竞态 | user_decision |
| 4 | Modal 关闭后 overdueConfirm 状态重置 | 确认状态不残留 | user_decision |

### 不测项（notTest）

| 序号 | 排除项 | 原因 |
|------|--------|------|
| 1 | 后端 expire 接口逻辑 | 后端无改动，复用已有接口 |
| 2 | EnvConfiguration.scala 变更 | 仅空行变更，无逻辑影响 |
| 3 | TestContextHAManager.java 变更 | Jetty 配置重构注释，与本需求无关 |
| 4 | 数据源列表分页、搜索、创建等其他功能 | 不在本次改动范围 |

---

## 三、远端环境信息（environmentInfo）

| 环境 | 地址 | 可用性 | VPN 要求 |
|------|------|--------|----------|
| 前端管理台 (WDSL-RM 5436) | 需部署 linkis-web 前端至测试环境（SIT/UAT） | 需提测后可用 | 需内网访问 |
| 后端 API (linkis-server) | 与前端同环境部署 | 需提测后可用 | 需内网访问 |

> 环境说明：前端管理台需与后端 linkis-server 部署在同一环境。测试时需确认 SIT 环境的前端管理台地址。

---

## 四、登录与鉴权（authInfo）

| 维度 | 内容 |
|------|------|
| 登录方式 | 管理台账密登录（Linkis 管理台标准登录流程） |
| Token 获取 | 登录成功后通过 cookie/token 管理会话 |
| 测试账号来源 | 使用测试环境提供的运维账号（需有数据源管理权限） |
| 权限要求 | 测试账号需具备数据源管理模块的查看和操作权限 |

---

## 五、测试数据策略（dataStrategy）

| 数据项 | 构造方法 | 依赖 | 清理策略 |
|--------|----------|------|----------|
| 至少一条有效数据源 | 通过管理台"创建数据源"功能或 DB 直接插入 | 数据源类型已配置 | 测试结束后删除或过期创建的测试数据源 |
| 数据源名称含特殊字符（可选） | 创建数据源时使用特殊字符名称 | 无 | 同上 |
| 空名称数据源（选测） | DB 直接插入 name 为空的数据源记录 | 无 | 测试后删除 |

> 数据构造说明：本需求测试核心是 Modal 交互逻辑，数据量需求小。至少需要 1 条数据源记录用于触发过期按钮。数据源名称用于 Modal 内容显示验证。

---

## 六、边界与异常约定

| 场景 | 预期行为 | 测试类型 |
|------|----------|----------|
| 数据源名称为空 | Modal 内容占位符替换为空字符串，确认流程不受影响 | 选测 |
| expire 接口返回失败 | axios 拦截器统一错误提示，Modal 已关闭，列表不刷新 | 选测 |
| 连续快速点击"过期"按钮 | overdueConfirm 状态被覆盖，仅显示最后一次的 Modal | 选测 |
| 数据源已被过期（重复操作） | 列表中已过期数据源是否仍显示"过期"按钮取决于原有逻辑 | 回归 |

---

## 七、接口契约约定

| 维度 | 内容 |
|------|------|
| 鉴权方式 | 管理台标准会话鉴权（cookie/token） |
| 限流/熔断 | 无特殊限流（前端单次调用） |
| 幂等性 | expire 接口对同一数据源重复调用应安全（幂等或返回已过期状态） |
| 版本兼容 | 无版本兼容问题（接口未变更） |

---

## 八、澄清问答记录（clarificationLog）

| 轮次 | 维度 | 问题 | 回答 |
|------|------|------|------|
| 1 | 远端环境 | 前端管理台测试环境地址是什么？是否需要提测后才有环境？ | 需提测后使用 SIT 环境部署的前端管理台，需内网访问 |
| 1 | 远端环境 | 是否需要 VPN 或特殊网络配置？ | 需内网访问，测试环境部署在内部网络 |
| 2 | 登录/鉴权 | 管理台登录方式是什么？ | 账密登录，Linkis 管理台标准流程 |
| 2 | 登录/鉴权 | 测试账号来源和权限要求？ | 使用测试运维账号，需数据源管理权限 |
| 3 | 测试数据 | 需要构造哪些测试数据？ | 至少 1 条有效数据源记录，可通过管理台创建或 DB 插入 |
| 3 | 测试数据 | 数据清理策略？ | 测试结束后删除或过期测试数据源 |
| 4 | 被测范围 | 必测项有哪些？ | AC1-AC4 全部为必测项，核心交互路径全覆盖 |
| 4 | 被测范围 | 不测项有哪些？ | 后端 expire 接口逻辑、EnvConfiguration/TestContextHAManager 附带变更、非本次改动的数据源功能 |

---

## 九、待补充项（qualityWarnings）

| 序号 | 待补充项 | 影响 | 建议处理 |
|------|----------|------|----------|
| 1 | SIT 环境具体前端管理台地址未确认 | 需提测后获取 | Stage 6 生成用例时使用占位符，提测后填充 |
| 2 | 测试账号具体用户名/密码未确认 | 需测试环境管理员提供 | 提测时同步提供 |
| 3 | expire 接口幂等性行为未完全确认 | 选测项依赖此信息 | 回归测试时观察接口行为 |
| 4 | 数据源名称特殊字符的边界场景 | 选测项 | 视测试时间补充 |

---

## 十、风险测试增量

本需求无 `stabilityRisks` 和 `stabilityRiskTestRequirements`（context.md 中未声明稳定性风险）。版本级 `openRisks` 为空列表。

- **stabilityRiskTestFlow.scope**: 无（不适用）
- **风险来源必测项数量**: 0
- **降级状态**: 不适用（无风险约束，正常澄清流程完成）

---

## 十一、完成度评估

| 维度 | 权重 | 得分 | 说明 |
|------|------|------|------|
| P0 覆盖率 | 0.4 | 0.36 | 4/4 P0 维度均 covered（远端环境/登录鉴权/测试数据/被测范围） |
| 回答质量 | 0.3 | 0.25 | 回答具体，环境地址待提测后确认 |
| 边界明确度 | 0.2 | 0.14 | 边界场景明确，部分为选测 |
| 约束清晰度 | 0.1 | 0.10 | 不测项明确，被测范围清晰 |
| **总计** | **1.0** | **0.85** | 门控通过（>=85%） |

---

## 十二、GEP 召回记录

- **召回状态**: 超时/不可用（冷启动期）
- **Gene 策略**: 无（空 Gene，正常执行不阻塞）
- **回流状态**: 待执行（纪要落盘后执行）
