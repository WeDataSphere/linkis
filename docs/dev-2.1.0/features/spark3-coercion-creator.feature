Feature: Spark3 强制切换增强（Creator 维度）
  在现有 Spark3 强制切换机制中新增"应用(creator)"维度判定，
  支持按应用粒度灰度迁移 Spark2 到 Spark3

  作为运维管理员
  我希望能够按应用(creator)维度强制切换 Spark3
  以便更精细地控制 Spark2 到 Spark3 的灰度迁移

  Background:
    Given 系统已启动
    And linkis-entrance 服务正常运行
    And 任务解析器 CommonEntranceParser 已加载

  Rule: 必须保持现有功能不受影响

    @regression @critical
    Scenario: 总开关关闭时所有维度均不生效
      Given Spark3 强制切换总开关已关闭
      And 用户级名单包含用户 "userA"
      And 部门级名单包含部门 "dept01"
      And 应用级名单包含应用 "appA"
      When 用户 "userA" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 引擎版本应该保持为 Spark2
      And 行为应该与增强前完全一致

    @regression @critical
    Scenario: 用户级名单命中时强制切换（原有功能）
      Given Spark3 强制切换总开关已开启
      And 用户级名单包含执行用户 "userA"
      And 部门级名单为空
      And 应用级名单为空
      When 用户 "userA" 从应用 "appA" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 日志应该记录用户级命中

    @regression @critical
    Scenario: 部门级名单命中时强制切换（原有功能）
      Given Spark3 强制切换总开关已开启
      And 用户级名单不包含执行用户 "userB"
      And 部门级名单包含用户 "userB" 所属部门 "dept02"
      And 应用级名单为空
      When 用户 "userB" 从应用 "appB" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 日志应该记录部门级命中

    @regression
    Scenario: 非 Spark 引擎任务不受强制切换影响
      Given Spark3 强制切换总开关已开启
      And 应用级名单包含应用 "appA"
      When 用户 "userA" 从应用 "appA" 提交 Hive 任务
      Then 任务应该使用 Hive 引擎执行
      And 不应该被切换到 Spark3

    @regression
    Scenario: 已是 Spark3 的任务不重复切换
      Given Spark3 强制切换总开关已开启
      And 应用级名单包含应用 "appA"
      When 用户 "userA" 从应用 "appA" 提交 Spark3 任务
      Then 任务应该使用 Spark3 引擎执行
      And 引擎版本应该保持为 3.4.4
      And 不应该被重复改写

  Rule: 新增 creator 维度强制切换

    @smoke @new-feature
    Scenario: creator 在应用级名单时强制切换 Spark3
      Given Spark3 强制切换总开关已开启
      And 用户级名单不包含执行用户 "userC"
      And 部门级名单不包含用户 "userC" 所属部门 "dept03"
      And 应用级名单包含应用 "appC"
      When 用户 "userC" 从应用 "appC" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 日志应该记录 creator 级命中
      And 日志应该包含 creator 值 "appC"

    @new-feature
    Scenario: creator 不在应用级名单时保持 Spark2
      Given Spark3 强制切换总开关已开启
      And 用户级名单不包含执行用户 "userD"
      And 部门级名单不包含用户 "userD" 所属部门 "dept04"
      And 应用级名单包含应用 "appC" 但不包含 "appD"
      When 用户 "userD" 从应用 "appD" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 引擎版本应该保持为 Spark2

    @new-feature
    Scenario: 应用级名单为空时保持 Spark2
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单为空
      When 用户 "userE" 从应用 "appE" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 行为应该与增强前完全一致

    @new-feature
    Scenario: creator 名单包含多个应用时正确匹配
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单包含应用 "app1,app2,app3"
      When 用户 "userF" 从应用 "app2" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4

  Rule: 三维度优先级为 个人 > 部门 > 应用(creator)

    @priority
    Scenario: 用户级命中时不检查 creator 维度
      Given Spark3 强制切换总开关已开启
      And 用户级名单包含执行用户 "userA"
      And 部门级名单为空
      And 应用级名单不包含应用 "appA"
      When 用户 "userA" 从应用 "appA" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 日志应该记录用户级命中
      And 日志不应该记录 creator 级命中

    @priority
    Scenario: 部门级命中时不检查 creator 维度
      Given Spark3 强制切换总开关已开启
      And 用户级名单不包含执行用户 "userB"
      And 部门级名单包含用户 "userB" 所属部门 "dept02"
      And 应用级名单不包含应用 "appB"
      When 用户 "userB" 从应用 "appB" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 日志应该记录部门级命中
      And 日志不应该记录 creator 级命中

    @priority
    Scenario: 三维度均未命中时保持 Spark2
      Given Spark3 强制切换总开关已开启
      And 用户级名单包含用户 "otherUser" 但不包含 "userG"
      And 部门级名单包含部门 "otherDept" 但不包含 "dept07"
      And 应用级名单包含应用 "otherApp" 但不包含 "appG"
      When 用户 "userG" 从应用 "appG" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 引擎版本应该保持为 Spark2

  Rule: creator 维度异常时降级不影响任务

    @negative @resilience
    Scenario: UserCreatorLabel 不存在时保持 Spark2
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单包含应用 "appA"
      And 任务的 UserCreatorLabel 不存在
      When 用户 "userH" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 不应该抛出异常
      And 应该记录 warn 级别日志

    @negative @resilience
    Scenario: creator 值为空时跳过 creator 维度检查
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单包含应用 "appA"
      And 任务的 creator 值为空字符串
      When 用户 "userI" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 不应该抛出异常

    @negative @resilience
    Scenario: creator 名单读取异常时降级
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单配置读取异常
      When 用户 "userJ" 从应用 "appJ" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 不应该抛出异常
      And 应该记录 warn 级别日志

  Rule: 热加载支持

    @config
    Scenario: creator 名单热加载生效
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单为空
      When 用户 "userK" 从应用 "appK" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 运维在不重启服务的情况下将应用 "appK" 加入应用级名单
      When 用户 "userK" 再次从应用 "appK" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4

    @config
    Scenario: creator 名单热加载移除后恢复 Spark2
      Given Spark3 强制切换总开关已开启
      And 用户级名单为空
      And 部门级名单为空
      And 应用级名单包含应用 "appL"
      When 用户 "userL" 从应用 "appL" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 运维在不重启服务的情况下将应用 "appL" 从应用级名单移除
      When 用户 "userL" 再次从应用 "appL" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行

  Rule: 高危区域冒烟验证（§10.2 #2 parser/拦截器链）

    @smoke @high-risk
    Scenario: 开关 on 状态冒烟验证
      Given Spark3 强制切换总开关已开启
      And 应用级名单包含应用 "smokeApp"
      When 用户 "smokeUser" 从应用 "smokeApp" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 任务应该正常执行完成
      And 日志和结果集应该正确回写到 Entrance

    @smoke @high-risk
    Scenario: 开关 off 状态冒烟验证
      Given Spark3 强制切换总开关已关闭
      And 应用级名单包含应用 "smokeApp"
      When 用户 "smokeUser" 从应用 "smokeApp" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      And 任务应该正常执行完成
      And 行为应该与增强前完全一致

    @smoke @high-risk @concurrent
    Scenario: 并发场景验证
      Given Spark3 强制切换总开关已开启
      And 应用级名单包含应用 "concurrentApp"
      When 3 个用户同时从应用 "concurrentApp" 提交 Spark2 任务
      Then 所有 3 个任务都应该被强制切换到 Spark3
      And 所有任务应该正常执行完成
      And 不应该出现任务串扰或引擎路由错误

  Rule: 配置项管理迁移（RPC 读取 + fallback）
    背景：spark.version.coercion.{users,department.id,creators} 三个名单已迁移到配置项管理，
    entrance 通过 RPC（RequestQueryEngineConfigWithGlobalConfig）读取；
    switch 保留 properties。RPC 失败时 fallback 到本地 properties 默认值，不阻断任务。

    @migration @smoke
    Scenario: RPC 成功读配置项管理名单后命中切换
      Given Spark3 强制切换总开关已开启
      And 配置项管理已注册应用级名单 "appM"（三张表 config_key/key_engine_relation/config_value 完整）
      And linkis-ps-configuration 服务正常
      When 用户 "userM" 从应用 "appM" 提交 Spark2 任务
      Then entrance 应通过 fetchSpark3CoercionConfig 发 RPC 拉取配置项管理的名单
      And 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 日志应该记录 creator 级命中

    @migration @negative @resilience
    Scenario: RPC 失败时 fallback 到本地 properties 默认值
      Given Spark3 强制切换总开关在 properties 中已开启
      And 应用级名单在配置项管理中配置为 "appN"
      And linkis-ps-configuration 服务不可用
      When 用户 "userN" 从应用 "appN" 提交 Spark2 任务
      Then fetchSpark3CoercionConfig 的 RPC 应失败但不抛出异常
      And keyAndValue 应为 null
      And CommonVars.getValue(null) 应走本地 properties 默认值
      And 任务应该使用 Spark2 引擎执行
      And 不应该阻断任务执行
      And 应该记录 warn 级别日志

    @migration @negative
    Scenario: DB 未注册 key（SQL 未执行）时行为与迁移前一致
      Given Spark3 强制切换总开关已开启
      And 配置项管理未注册 spark.version.coercion.* 的 key
      When 用户 "userO" 从应用 "appO" 提交 Spark2 任务
      Then RPC 返回的 map 不应该包含 coercion 相关 key
      And getValue 应走 properties 默认值
      And 任务应该使用 Spark2 引擎执行
      And 行为应该与迁移前完全一致

    @migration @negative
    Scenario: 三张表漏 config_value 时 queryConfig 查不到 key
      Given Spark3 强制切换总开关已开启
      And config_key 和 key_engine_relation 已注册
      And 但 config_value 表无对应记录
      When 用户 "userP" 从应用 "appP" 提交 Spark2 任务
      Then queryConfigWithGlobalConfig 不应该返回 coercion key
      And 等效于 fallback 到 properties 默认值
      And 任务应该使用 Spark2 引擎执行

    @migration @config
    Scenario: switch 保留 properties 不迁移到配置项管理
      Given 配置项管理未注册 spark.version.coercion.switch
      And properties 中 spark.version.coercion.switch=true
      And 应用级名单在配置项管理中包含 "appQ"
      When 用户 "userQ" 从应用 "appQ" 提交 Spark2 任务
      Then RPC 返回的 map 不应该包含 switch key
      And getValue(map) 对缺失的 switch 应走 properties 默认值 true
      And 任务应该被强制切换到 Spark3

    @migration @config
    Scenario: 前端 saveFullTree 改名单后广播清 AM 缓存
      Given Spark3 强制切换总开关已开启
      And 应用级名单为空
      When 用户 "userR" 从应用 "appR" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行
      When 运维在前端 setting 页面将 "appR" 加入应用级名单并保存
      Then saveFullTree 应广播 RemoveCacheConfRequest 清除 AM 缓存
      When 用户 "userR" 再次从应用 "appR" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 下个任务应该读到新的名单值

    @migration @config @known-limitation
    Scenario: 直接执行 SQL 后需重启 linkis-cg-entrance 清 RPC 缓存
      Given linkis-entrance 的 RPC 缓存策略为 expireAfterAccess 120000ms
      And 配置项管理已注册应用级名单 "appS"
      When 运维直接执行 SQL 注册新 key（未走前端 saveFullTree）
      Then entrance 侧 CacheableRPCInterceptor 缓存不会被清除
      When 用户 "userS" 从应用 "appS" 提交 Spark2 任务（未重启 entrance）
      Then 任务可能仍读到旧缓存值
      But 重启 linkis-cg-entrance 后
      When 用户 "userS" 再次从应用 "appS" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3

  Rule: user+creator 组合细粒度控制
    背景：在个人级未命中后，检查 (user, creator) 组合是否在名单。
    名单格式 "user:creator"，逗号分隔（如 "userA:IDE,userB:Schedulis"）。
    优先级：个人 > user+creator组合 > 部门 > creator。
    组合维度用 split 精确匹配（避免 contains 子串误命中）。

    @combo @smoke
    Scenario: user+creator 组合命中切换 Spark3
      Given Spark3 强制切换总开关已开启
      And user+creator 组合名单为 "userA:IDE"
      And 用户级名单为空
      And 应用级名单为空
      When 用户 "userA" 从应用 "IDE" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 引擎版本应该为 3.4.4
      And 日志应该记录 user+creator 组合命中

    @combo @negative
    Scenario: user+creator 组合 creator 不匹配保持 Spark2
      Given Spark3 强制切换总开关已开启
      And user+creator 组合名单为 "userA:IDE"
      And 用户级名单为空
      And 应用级名单为空
      When 用户 "userA" 从应用 "Schedulis" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行

    @combo @negative
    Scenario: user+creator 组合 user 不匹配保持 Spark2
      Given Spark3 强制切换总开关已开启
      And user+creator 组合名单为 "otherUser:IDE"
      And 用户级名单为空
      And 应用级名单为空
      When 用户 "userA" 从应用 "IDE" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行

    @combo
    Scenario: user+creator 组合精确匹配（子串安全）
      Given Spark3 强制切换总开关已开启
      And user+creator 组合名单为 "userA:IDE"
      When 用户 "userA" 从应用 "ID" 提交 Spark2 任务
      Then 任务应该使用 Spark2 引擎执行

    @combo @priority
    Scenario: 用户级命中时不检查组合
      Given Spark3 强制切换总开关已开启
      And 用户级名单包含 "userA"
      And user+creator 组合名单为 "otherUser:IDE"
      When 用户 "userA" 从应用 "IDE" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 日志应该记录用户级命中

    @combo @priority
    Scenario: 组合命中时不检查 creator 维度
      Given Spark3 强制切换总开关已开启
      And user+creator 组合名单为 "userA:IDE"
      And 应用级名单为 "Schedulis"
      When 用户 "userA" 从应用 "IDE" 提交 Spark2 任务
      Then 任务应该被强制切换到 Spark3
      And 日志应该记录 user+creator 组合命中
