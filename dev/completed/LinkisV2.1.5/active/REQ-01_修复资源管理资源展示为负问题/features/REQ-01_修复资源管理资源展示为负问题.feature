Feature: 修复资源管理资源展示为负问题
  消除资源管理模块资源展示为负数的异常，通过 O3引擎退出消息负值丢弃 + OC1界面查询负数归零 两层机制确保资源非负性

  Background:
    Given Linkis 计算治理平台已启动
    And 资源管理器（RM）和应用管理器（AM）服务正常
    And 资源管理监控已启用

  Rule: O3-引擎退出消息负值丢弃+参数开关

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

  Rule: OC1-资源管理界面查询负数归零

    @optimization @critical @P0
    Scenario: 查询返回的usedResource存在负值时展示为0
      Given 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      And 返回结果中usedResource包含负值字段
      Then RMUtils.normalizeUserResourceVoNegative 将usedResource中的负值字段归零
      And 界面展示的usedResource为0（非负数）

    @optimization @critical @P0
    Scenario: 查询返回的leftResource存在负值时展示为0
      Given 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      And 返回结果中leftResource包含负值字段
      Then RMUtils.normalizeUserResourceVoNegative 将leftResource中的负值字段归零
      And 界面展示的leftResource为0（非负数）

    @optimization @regression @P0
    Scenario: usedResource和leftResource均为非负时保持原值不变
      Given 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      And 返回结果中usedResource和leftResource均为非负值
      Then 不执行归零处理
      And 界面展示原始资源值不变

  Rule: 综合验证-O3+OC1两层机制确保资源非负

    @optimization @integration @P0
    Scenario: O3丢弃防御与OC1归零展示协同保证界面无负值
      Given O3引擎退出消息负值丢弃已生效且开关开启
      And OC1资源管理界面查询负数归零已生效
      When 引擎退出产生负值资源时
      Then O3丢弃负值扣减操作（防御层）
      And 界面查询allUserResource接口时OC1将usedResource/leftResource负值归零（展示层）
      And 资源管理界面始终显示非负数资源值

    @optimization @integration @P1
    Scenario: O3开关关闭时OC1仍保证界面展示非负
      Given O3参数开关 linkis.manager.engine.exit.negative.resource.discard.enable 为 false
      And OC1资源管理界面查询负数归零已生效
      When 引擎退出产生负值资源且开关关闭导致usedResource变为负值
      Then O3不执行丢弃操作（保持原有行为）
      But 界面查询allUserResource接口时OC1仍将usedResource/leftResource负值归零
      And 资源管理界面始终显示非负数资源值

  Rule: OC1-增强-剩余资源重算（left = max - used）+参数开关

    @optimization @critical @P0
    Scenario: 剩余资源重算开关开启时left按 max - used 计算（默认开启）
      Given 参数开关 linkis.manager.rm.user.resource.left.recompute.enable 为 true（默认开启）
      And 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      And RMUtils.recomputeUserResourceLeftNegative 读取已用资源 usedResource 与最大资源 maxResource
      Then 先对 usedResource 中的负值字段强制归零
      And 逐数值字段计算 left = max - used 并对负值结果归零（used > max 时）
      And 将重算结果写入 leftResource 并展示
      And 不改动底层DB数据（仅展示层生效）

    @optimization @critical @P0
    Scenario: 剩余资源重算开关关闭时保留DB快照left（回退原有行为）
      Given 参数开关 linkis.manager.rm.user.resource.left.recompute.enable 为 false
      And 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      Then 不执行 left = max - used 重算
      And 界面直接展示DB快照中的 leftResource 原值

    @optimization @regression @P0
    Scenario: max 与 used 中缺失字段时剩余资源按字段规则计算
      Given 参数开关 linkis.manager.rm.user.resource.left.recompute.enable 为 true
      And 资源管理界面发起资源查询请求
      When 系统调用 RMMonitorRest.getAllUserResource 接口
      And maxResource 含某字段而 usedResource 缺该字段
      Then 该字段剩余资源取 maxResource 原值（负值则归零）
      And maxResource 中非数值字段被忽略不进入重算

    @optimization @compatibility @P1
    Scenario: 单用户接口不受剩余资源重算影响
      Given 参数开关 linkis.manager.rm.user.resource.left.recompute.enable 为 true
      When 调用 getUserResourceByLabel 单用户资源查询接口
      Then 不执行 left = max - used 重算（仅 allUserResource 总览列表生效）
      And 返回值保持 RMUtils.toUserResourceVo 原样输出
