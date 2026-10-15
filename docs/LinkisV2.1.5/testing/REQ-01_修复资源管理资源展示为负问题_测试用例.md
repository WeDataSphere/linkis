# 修复资源管理资源展示为负问题 测试用例

> 版本：LinkisV2.1.5 ｜ 需求：REQ-01 ｜ 子系统：WDSL-UJES ｜ 生成时间：2026-10-15
> 需求类型：OPTIMIZE（快速模式） ｜ 功能属性：后端 + 前端+后台 Web API（纯后端项目，Web API 测试归本测试案例文档）

## 一、测试概述

### 1.1 测试目标

验证「资源管理资源展示为负问题」修复在 3 个机制维度上的正确性，确保资源管理界面展示的非负性：

1. **O3 - 引擎退出消息负值丢弃**：`linkis.manager.engine.exit.negative.resource.discard.enable` 开关开启时，引擎退出消息若使已用资源变为负值，丢弃该扣减操作。
2. **OC1 - 资源管理界面查询负数归零**：`linkis.manager.rm.user.resource.negative.normalize.enable` 开关开启时，`getAllUserResource` 查询结果中 usedResource/leftResource 负值强制归零。
3. **OC1增强 - 剩余资源重算（left = max - used）**：`linkis.manager.rm.user.resource.left.recompute.enable` 开关开启时，在接口最后将剩余资源重算为「最大资源 - 已用资源」，使展示口径更准确。

### 1.2 测试范围与输入

| 输入项 | 来源 | 说明 |
|-------|------|------|
| Feature 文件 | `dev/versions/LinkisV2.1.5/active/REQ-01_.../features/REQ-01_...feature` | 含 O3/OC1/OC1增强 三维 12 个 Scenario |
| 接口定义 | `RMMonitorRest.scala` | GET `/linkisManager/rm/allUserResource`（改造点）、GET `/linkisManager/rm/get-user-resource`（原状）|
| 代码实现 | `RMUtils.scala`、`RMMonitorRest.scala` | `recomputeUserResourceLeftNegative`、`normalizeUserResourceVoNegative`、`normalizeNegativeResource` 及 3 个 CommonVars 开关 |
| 设计风险要求 | 设计文档 12.4 | RTR-001（O3）、RTR-002（OC1）、RTR-003（引擎退出）|

### 1.3 测试前置条件（通用 Background）

- Linkis 计算治理平台已启动，资源管理器（RM）与应用管理器（AM）服务正常
- 资源管理监控已启用
- 测试账号具备管理员权限（`getAllUserResource` 接口要求管理员，`ONLY_ADMIN_READ`）
- 测试环境已部署 release_v2.1.5 分支代码

## 二、功能测试用例

### 2.1 规则一：O3-引擎退出消息负值丢弃+参数开关

### TC001：引擎退出消息导致负值时丢弃回收操作（开关开启）

**来源**：Feature 文件 - REQ-01 feature, O3 规则 Scenario 1

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：O3-引擎退出消息负值丢弃+参数开关

**标签**：@optimization @important @P1

**前置条件（来自Given）**：
- 参数开关 `linkis.manager.engine.exit.negative.resource.discard.enable` 为 true（默认开启）
- 当前用户资源 usedResource 为 0（已重置）
- 引擎正常注册且资源标签已落地

**测试步骤（来自When）**：
1. linkis-manager 收到某引擎退出消息
2. 该引擎退出时回收资源量使「当前资源 - 回收资源」为负值

**预期结果（来自Then）**：
- 丢弃该引擎资源扣减操作（不执行 currentUsed - releasedResource）
- 引擎状态清理（EC 记录删除、标签清理）仍正常执行
- 打印 warn 级别日志记录丢弃事件，日志包含引擎信息、标签信息、原始资源值、回收资源值
- usedResource 保持为 0，不变为负值

**优先级**：P1 ｜ **测试类型**：功能测试 ｜ **覆盖场景**：正向场景 - 开关开启负值丢弃

**Gherkin规格**：
```gherkin
@optimization @important @P1
Scenario: 引擎退出消息导致负值时丢弃回收操作（开关开启）
  Given 参数开关 linkis.manager.engine.exit.negative.resource.discard.enable 为 true（默认开启）
  And 当前用户资源usedResource为0（已重置）
  When linkis-manager收到引擎退出消息
  And 当前资源减去引擎回收资源后为负值
  Then 丢弃该引擎资源扣减操作（不执行currentUsed - releasedResource）
  And 引擎状态清理（如EC记录删除、标签清理）仍正常执行
  And 打印warn级别日志记录丢弃事件
  And 日志包含引擎信息、标签信息、原始资源值、回收资源值
  And usedResource保持为0不变为负值
```

**关联风险**：RTR-003-DOWNSTREAM-LINKAGE

---

### TC002：开关关闭时保持原有行为（向后兼容）

**来源**：Feature 文件 - REQ-01 feature, O3 规则 Scenario 2

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：O3-引擎退出消息负值丢弃+参数开关

**标签**：@optimization @compatibility @P1

**前置条件（来自Given）**：
- 参数开关 `linkis.manager.engine.exit.negative.resource.discard.enable` 为 false
- 当前用户资源 usedResource 为 0（已重置）

**测试步骤（来自When）**：
1. linkis-manager 收到引擎退出消息
2. 当前资源减去引擎回收资源后为负值

**预期结果（来自Then）**：
- 按原有逻辑执行资源扣减
- usedResource 变为负值
- 不打印丢弃 warn 日志
- 可能打印 resourceCheck 的 "found error resource" 告警日志

**优先级**：P1 ｜ **测试类型**：功能测试 ｜ **覆盖场景**：边界场景 - 开关关闭回退

**Gherkin规格**：
```gherkin
@optimization @compatibility @P1
Scenario: 开关关闭时保持原有行为（向后兼容）
  Given 参数开关 linkis.manager.engine.exit.negative.resource.discard.enable 为 false
  And 当前用户资源usedResource为0（已重置）
  When linkis-manager收到引擎退出消息
  And 当前资源减去引擎回收资源后为负值
  Then 按原有逻辑执行资源扣减
  And usedResource变为负值
  And 不打印丢弃warn日志
  But 可能打印resourceCheck的"found error resource"告警日志
```

**关联风险**：RTR-001-CONFIG-ROLLBACK

---

### TC003：正常引擎退出不受丢弃逻辑影响

**来源**：Feature 文件 - REQ-01 feature, O3 规则 Scenario 3

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：O3-引擎退出消息负值丢弃+参数开关

**标签**：@optimization @regression @P1

**前置条件（来自Given）**：
- 参数开关 `linkis.manager.engine.exit.negative.resource.discard.enable` 为 true
- 当前用户资源 usedResource 为正值

**测试步骤（来自When）**：
1. linkis-manager 收到引擎退出消息
2. 当前资源减去引擎回收资源后为非负值

**预期结果（来自Then）**：
- 正常执行资源回收操作
- usedResource 正确扣减
- 不触发丢弃逻辑
- 不打印丢弃 warn 日志

**优先级**：P1 ｜ **测试类型**：功能测试 ｜ **覆盖场景**：正向场景 - 正常退出

**Gherkin规格**：
```gherkin
@optimization @regression @P1
Scenario: 正常引擎退出不受丢弃逻辑影响
  Given 参数开关 linkis.manager.engine.exit.negative.resource.discard.enable 为 true
  And 当前用户资源usedResource为正值
  When linkis-manager收到引擎退出消息
  And 当前资源减去引擎回收资源后为非负值
  Then 正常执行资源回收操作
  And usedResource正确扣减
  And 不触发丢弃逻辑
  And 不打印丢弃warn日志
```

---

### 2.2 规则二：OC1-资源管理界面查询负数归零

### TC004：查询返回的 usedResource 存在负值时展示为 0

**来源**：Feature 文件 - REQ-01 feature, OC1 规则 Scenario 1

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-资源管理界面查询负数归零

**标签**：@optimization @critical @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource
- 接口描述：getAllUserResource 获取所有用户资源列表（管理台资源总览）

**前置条件**：
- 以管理员身份登录测试环境
- 存在至少一条用户资源记录，且其 DB 快照 usedResource 含负值字段

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/allUserResource`（带管理员鉴权 Headers）
2. 确保返回结果中某用户的 usedResource map 含负值数值字段
3. 观察接口返回值

**预期结果**：
- RMUtils.normalizeUserResourceVoNegative 将 usedResource 中的负值字段归零
- 界面/接口展示的 usedResource 为 0（非负数）

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：正向场景 - usedResource 负值归零

**关联风险**：RTR-002-DOWNSTREAM-LINKAGE

---

### TC005：查询返回的 leftResource 存在负值时展示为 0

**来源**：Feature 文件 - REQ-01 feature, OC1 规则 Scenario 2

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-资源管理界面查询负数归零

**标签**：@optimization @critical @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 管理员权限已配置
- 存在用户资源记录，其 DB 快照 leftResource 含负值字段（或经 addUserResource 写入负值）

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/allUserResource`
2. 确保返回某用户记录中 leftResource map 含负值数值字段
3. 观察接口返回值

**预期结果**：
- RMUtils.normalizeUserResourceVoNegative 将 leftResource 中的负值字段归零
- 界面展示的 leftResource 为 0（非负数）

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：正向场景 - leftResource 负值归零

**关联风险**：RTR-002-DOWNSTREAM-LINKAGE

---

### TC006：usedResource 和 leftResource 均为非负时保持原值不变

**来源**：Feature 文件 - REQ-01 feature, OC1 规则 Scenario 3

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-资源管理界面查询负数归零

**标签**：@optimization @regression @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 管理员权限已配置
- 存在用户资源记录，usedResource 与 leftResource 均为非负值

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/allUserResource`
2. 返回结果中某用户 usedResource 和 leftResource 均为非负
3. 观察接口返回值

**预期结果**：
- 不执行归零处理
- 界面展示原始资源值不变

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：回归场景 - 非负值不被篡改

**关联风险**：RTR-002-DOWNSTREAM-LINKAGE

---

### 2.3 规则三：OC1-增强-剩余资源重算（left = max - used）+参数开关

### TC007：剩余资源重算开关开启时 left 按 max - used 计算（默认开启）

**来源**：Feature 文件 - REQ-01 feature, OC1增强 Rule Scenario 1

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-增强-剩余资源重算（left = max - used）+参数开关

**标签**：@optimization @critical @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource
- 接口描述：改造点，接口最后追加 `recomputeUserResourceLeftNegative`

**前置条件**：
- 参数开关 `linkis.manager.rm.user.resource.left.recompute.enable` 为 true（默认开启）
- 管理员权限已配置

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/allUserResource`
2. 系统返回某用户 maxResource 与 usedResource 均为非负且 leftResource（DB 快照）与「max-used」不一致
3. 观察接口返回值中该用户的 leftResource

**预期结果**：
- 先对 usedResource 中的负值字段强制归零
- 逐数值字段计算 left = max - used
- 重算结果若为负（used > max）则归零
- 将重算结果写入 leftResource 并展示
- 不改动底层 DB 数据（仅展示层生效）

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：关键路径 - left=max-used 重算

**关联风险**：RTR-002-DOWNSTREAM-LINKAGE

---

### TC008：剩余资源重算开关关闭时保留 DB 快照 left（回退原有行为）

**来源**：Feature 文件 - REQ-01 feature, OC1增强 Rule Scenario 2

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-增强-剩余资源重算（left = max - used）+参数开关

**标签**：@optimization @critical @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 参数开关 `linkis.manager.rm.user.resource.left.recompute.enable` 为 false
- 管理员权限已配置

**测试步骤**：
1. 关闭剩余资源重算开关（配置中心或配置文件置 false）
2. 构造请求：`GET /linkisManager/rm/allUserResource`
3. 观察接口返回值中某用户的 leftResource

**预期结果**：
- 不执行 left = max - used 重算
- 界面直接展示 DB 快照中的 leftResource 原值

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：边界场景 - 开关关闭回退

---

### TC009：max 与 used 中缺失字段时剩余资源按字段规则计算

**来源**：Feature 文件 - REQ-01 feature, OC1增强 Rule Scenario 3

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-增强-剩余资源重算（left = max - used）+参数开关

**标签**：@optimization @regression @P0

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 参数开关 `linkis.manager.rm.user.resource.left.recompute.enable` 为 true
- 管理员权限已配置
- 存在用户记录：maxResource 含某数值字段而 usedResource 缺该字段

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/allUserResource`
2. 观察返回中该用户的 leftResource 计算

**预期结果**：
- 该字段剩余资源取 maxResource 原值（若为负则归零）
- maxResource 中非数值字段被忽略，不进入重算

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：边界场景 - 缺失字段

**关联风险**：RTR-002-CROSS-SYSTEM-FAILURE

---

### TC010：单用户接口不受剩余资源重算影响

**来源**：Feature 文件 - REQ-01 feature, OC1增强 Rule Scenario 4

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：OC1-增强-剩余资源重算（left = max - used）+参数开关

**标签**：@optimization @compatibility @P1

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/get-user-resource
- 接口描述：getUserResourceByLabel 单用户资源查询，保持原状

**前置条件**：
- 参数开关 `linkis.manager.rm.user.resource.left.recompute.enable` 为 true（即使开启也不影响此接口）
- 存在指定用户/creator/engineType 的资源记录

**测试步骤**：
1. 构造请求：`GET /linkisManager/rm/get-user-resource?username=xxx&creator=xxx&engineType=xxx`
2. 观察该接口返回值

**预期结果**：
- 不执行 left = max - used 重算（仅 allUserResource 总览列表生效）
- 返回值保持 RMUtils.toUserResourceVo 原样输出

**优先级**：P1 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：回归场景 - 单用户接口不受影响

---

### 2.4 规则四：综合验证-O3+OC1 两层机制确保资源非负

### TC011：O3 丢弃防御与 OC1 归零展示协同保证界面无负值

**来源**：Feature 文件 - REQ-01 feature, 综合验证 Rule Scenario 1

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：综合验证-O3+OC1 两层机制确保资源非负

**标签**：@optimization @integration @P0

**前置条件**：
- O3 引擎退出消息负值丢弃已生效且开关开启
- OC1 资源管理界面查询负数归零已生效

**测试步骤**：
1. 触发引擎退出场景，模拟产生负值资源
2. 调用 allUserResource 查询接口
3. 观察资源管理界面展示

**预期结果**：
- O3 丢弃负值扣减操作（防御层），usedResource 不落负值
- 界面查询 allUserResource 接口时 OC1 将 any usedResource/leftResource 负值归零（展示层兜底）
- 资源管理界面始终显示非负数资源值

**优先级**：P0 ｜ **测试类型**：流程测试 ｜ **覆盖场景**：集成场景 - 两层协同

**关联风险**：RTR-003-CASCADE-RECOVERY

---

### TC012：O3 开关关闭时 OC1 仍保证界面展示非负

**来源**：Feature 文件 - REQ-01 feature, 综合验证 Rule Scenario 2

**Feature上下文**：
- Feature：修复资源管理资源展示为负问题
- Rule：综合验证-O3+OC1 两层机制确保资源非负

**标签**：@optimization @integration @P1

**前置条件**：
- O3 参数开关 `linkis.manager.engine.exit.negative.resource.discard.enable` 为 false
- OC1 资源管理界面查询负数归零已生效

**测试步骤**：
1. 触发引擎退出场景，O3 关闭导致 usedResource 可能为负
2. 调用 allUserResource 查询接口
3. 观察资源管理界面展示

**预期结果**：
- O3 不执行丢弃操作（保持原有行为）
- 界面查询 allUserResource 接口时 OC1 仍将 usedResource/leftResource 负值归零
- 资源管理界面始终显示非负数资源值

**优先级**：P1 ｜ **测试类型**：流程测试 ｜ **覆盖场景**：集成场景 - 单层失效兜底

**关联风险**：RTR-001-CONFIG-BOUNDARY

## 三、接口测试用例（Web API）

### TC013：GET /linkisManager/rm/allUserResource - 非管理员访问被拒绝

**来源**：接口定义解析 - RMMonitorRest.scala getAllUserResource

**测试类型**：接口测试

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource
- 接口描述：仅管理员可访问（Configuration.isNotAdmin 校验）

**前置条件**：
- 以非管理员普通用户身份登录

**测试步骤**：
1. 以普通用户身份发送 `GET /linkisManager/rm/allUserResource`
2. 观察响应

**预期结果**：
- 抛出 RMErrorException（ONLY_ADMIN_READ），返回权限不足错误
- 不返回任何用户资源数据

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：异常场景 - 权限不足

---

### TC014：GET /linkisManager/rm/allUserResource - 正常分页返回资源列表

**来源**：接口定义解析 - RMMonitorRest.scala getAllUserResource

**测试类型**：接口测试

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource
- 接口描述：支持 username/creator/engineType 过滤 + page/size 分页

**前置条件**：
- 管理员权限已配置
- 存在多条不同用户/creator/engineType 的资源记录

**测试步骤**：
1. 管理员发送 `GET /linkisManager/rm/allUserResource?page=1&size=20`（可选带 username/creator/engineType 过滤）
2. 观察响应

**预期结果**：
- 返回 code=200 的 Message
- data.resources 为 UserResourceVo 列表，字段含 maxResource/usedResource/leftResource/username/creator/engineType
- data.total 返回总条数
- 各资源经过 recomputeUserResourceLeftNegative(normalizeUserResourceVoNegative(toUserResourceVo(...))) 处理

**优先级**：P0 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：正向场景 - 列表查询

---

### TC015：GET /linkisManager/rm/get-user-resource - 单用户资源查询保持原样

**来源**：接口定义解析 - RMMonitorRest.scala getUserResourceByLabel

**测试类型**：接口测试

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/get-user-resource
- 接口描述：按 username/creator/engineType 查询单用户资源，不参与 left 重算与归零增强

**前置条件**：
- 存在指定 username/creator/engineType 的资源记录

**测试步骤**：
1. 发送 `GET /linkisManager/rm/get-user-resource?username=xxx&creator=xxx&engineType=xxx`
2. 观察响应

**预期结果**：
- 返回 code=200 的 Message
- data.resources 为该用户资源列表
- 值保持 RMUtils.toUserResourceVo 原样输出（不重算、不归零）

**优先级**：P1 ｜ **测试类型**：接口测试 ｜ **覆盖场景**：回归场景 - 单用户接口

## 四、边界与异常场景

### TC016：O3 开关非标准布尔值视为 false

**来源**：设计文档 12.4 - RTR-001-CONFIG-BOUNDARY

**测试类型**：边界测试

**前置条件**：
- 将参数 `linkis.manager.engine.exit.negative.resource.discard.enable` 配置为非标准布尔值（如 "abc"、""、数值）

**测试步骤**：
1. 重启或刷新相关服务使配置生效
2. 触发引擎退出消息且满足负值条件
3. 观察行为

**预期结果**：
- 非标准布尔值被视为 false
- 按原有逻辑执行资源扣减，不触发丢弃
- 行为与开关显式设为 false 一致

**优先级**：P1 ｜ **测试类型**：边界测试 ｜ **覆盖场景**：边界场景 - 非法开关值

---

### TC017：OC1 归零遇 null/非数值字段容错

**来源**：设计文档 12.4 - RTR-002-CROSS-SYSTEM-FAILURE

**测试类型**：异常测试

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 管理员权限已配置
- 构造某用户资源记录，使其 usedResource/leftResource 含 null 值或字符串等非数值字段

**测试步骤**：
1. 发送 `GET /linkisManager/rm/allUserResource`
2. 观察接口是否抛出异常

**预期结果**：
- normalizeNegativeResource 遇 null 返回原值不抛异常
- 非数值字段被忽略，不参与归零与重算
- 接口正常返回，底层数据不变

**优先级**：P0 ｜ **测试类型**：异常测试 ｜ **覆盖场景**：异常场景 - null/非数值容错

**关联风险**：RTR-002-CROSS-SYSTEM-FAILURE

---

### TC018：OC1 归零异常时接口仍正常返回（降级）

**来源**：设计文档 12.4 - RTR-002-CASCADE-RECOVERY

**测试类型**：异常测试

**接口信息**：
- HTTP方法：GET
- 接口路径：/linkisManager/rm/allUserResource

**前置条件**：
- 管理员权限已配置
- 模拟归零/重算过程中某字段处理异常（如极端类型）

**测试步骤**：
1. 发送 `GET /linkisManager/rm/allUserResource`
2. 观察接口整体行为

**预期结果**：
- 归零/重算异常时跳过该字段处理
- 接口整体正常返回，不再抛异常打断
- 底层 DB 数据未被改写

**优先级**：P1 ｜ **测试类型**：异常测试 ｜ **覆盖场景**：降级场景 - 展示层兜底

**关联风险**：RTR-002-CASCADE-RECOVERY

---

### TC019：丢弃逻辑不影响引擎状态清理

**来源**：设计文档 12.4 - RTR-003-CASCADE-RECOVERY

**测试类型**：流程测试

**前置条件**：
- O3 开关开启（默认）
- 触发引擎退出且资源扣减为负值

**测试步骤**：
1. 发生引擎退出消息且命中丢弃条件
2. 观察引擎状态清理执行情况

**预期结果**：
- 丢弃仅跳过资源扣减
- 引擎状态清理（EC 记录删除、标签清理）正常执行
- 不因丢弃而遗漏状态清理

**优先级**：P1 ｜ **测试类型**：流程测试 ｜ **覆盖场景**：级联场景 - 状态清理不受影响

**关联风险**：RTR-003-CASCADE-RECOVERY

## 五、Feature 覆盖率统计

### 5.1 Scenario 覆盖率

| Feature 文件 | Scenario 总数 | 已生成测试用例 | 覆盖率 | 状态 |
|------------|-------------|--------------|-------|------|
| REQ-01_修复资源管理资源展示为负问题.feature | 12 | 12 | 100% | ✅ 完全覆盖 |

### 5.2 覆盖详情

| Feature Scenario | 对应测试用例 |
|-----------------|-------------|
| O3-S1 引擎退出消息负值丢弃（开关开启） | TC001 |
| O3-S2 开关关闭保持原有行为 | TC002 |
| O3-S3 正常引擎退出不受影响 | TC003 |
| OC1-S1 usedResource 负值归零 | TC004 |
| OC1-S2 leftResource 负值归零 | TC005 |
| OC1-S3 非负值保持原值 | TC006 |
| OC1增强-S1 left 按 max-used 计算 | TC007 |
| OC1增强-S2 开关关闭保留 DB 快照 | TC008 |
| OC1增强-S3 max/used 缺失字段规则 | TC009 |
| OC1增强-S4 单用户接口不受影响 | TC010 |
| 综合-S1 O3+OC1 协同保证非负 | TC011 |
| 综合-S2 O3 关闭时 OC1 兜底 | TC012 |

### 5.3 验收标准覆盖检查

**设计文档 12.4 风险测试要求**：

| 测试要求 ID | 测试内容 | 对应用例 | 状态 |
|-----------|---------|---------|------|
| RTR-001-CONFIG-EFFECT | 开关默认开启丢弃逻辑生效 | TC001 | ✅ |
| RTR-001-CONFIG-ROLLBACK | 关闭开关恢复原有行为 | TC002 | ✅ |
| RTR-001-CONFIG-BOUNDARY | 开关 true/false 边界值 | TC016 | ✅ |
| RTR-002-DOWNSTREAM-LINKAGE | OC1 归零对 allUserResource 影响 | TC004/TC005/TC006/TC007 | ✅ |
| RTR-002-CROSS-SYSTEM-FAILURE | OC1 归零 null/非数值容错 | TC009/TC017 | ✅ |
| RTR-002-CASCADE-RECOVERY | OC1 异常时接口仍正常返回 | TC018 | ✅ |
| RTR-003-DOWNSTREAM-LINKAGE | 引擎退出处理变更对资源记账影响 | TC001/TC011 | ✅ |
| RTR-003-CASCADE-RECOVERY | 丢弃逻辑不影响引擎状态清理 | TC019 | ✅ |

**覆盖率**：8/8 风险测试要求 (100%)

## 六、测试数据策略说明

- **接口测试（TC004-TC015）**：通过测试环境的 resource 标签与资源表构造不同用户/资源记录；可采用调整配置开关后重启/刷新服务验证回退行为
- **DB 数据构造**：输入已用资源负值、剩余资源负值、非数值字段等，用于验证归零与重算逻辑
- **数据清理**：测试结束后清理构造的标签与资源记录，避免影响后续测试

## 七、测试优先级分布

| 优先级 | 数量 | 用例 |
|-------|------|------|
| P0 | 9 | TC004-TC009、TC013、TC014、TC017 |
| P1 | 9 | TC001-TC003、TC010、TC012、TC015、TC016、TC018、TC019 |
| **总计** | **18** | — |

## 八、测试执行建议

1. **正向路径**：先验证 TC007（left=max-used 重算核心）、TC004/TC005（归零），再补充边界与异常
2. **回退验证**：验证两个功能开关（O3 discard、left recompute）关闭时的回退行为（TC002/TC008）
3. **单用户接口回归**：TC010/TC015 确保新增增强不影响既有单用户查询
4. **权限验证**：TC013 确保 getAllUserResource 仍受管理员权限约束