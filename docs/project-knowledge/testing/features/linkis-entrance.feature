# language: zh-CN
@regression @linkis-entrance
Feature: linkis-entrance 模块回归测试

  作为系统，需要保证 linkis-entrance 模块的核心功能稳定
  以便确保任务入口的 Spark3 强制切换配置项管理迁移正确

  # 配置项管理迁移场景（来源：spark3-coercion-creator.feature Rule: 配置项管理迁移）

  @regression @migration @smoke
  Scenario: RPC 成功读配置项管理名单后命中切换
    Given Spark3 强制切换总开关已开启
    And 配置项管理已注册应用级名单 "appM"（三张表 config_key/key_engine_relation/config_value 完整）
    And linkis-ps-configuration 服务正常
    When 用户 "userM" 从应用 "appM" 提交 Spark2 任务
    Then entrance 应通过 fetchSpark3CoercionConfig 发 RPC 拉取配置项管理的名单
    And 任务应该被强制切换到 Spark3
    And 引擎版本应该为 3.4.4
    And 日志应该记录 creator 级命中

  @regression @migration @negative @resilience
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

  @regression @migration @negative
  Scenario: DB 未注册 key（SQL 未执行）时行为与迁移前一致
    Given Spark3 强制切换总开关已开启
    And 配置项管理未注册 spark.version.coercion.* 的 key
    When 用户 "userO" 从应用 "appO" 提交 Spark2 任务
    Then RPC 返回的 map 不应该包含 coercion 相关 key
    And getValue 应走 properties 默认值
    And 任务应该使用 Spark2 引擎执行
    And 行为应该与迁移前完全一致

  @regression @migration @negative
  Scenario: 三张表漏 config_value 时 queryConfig 查不到 key
    Given Spark3 强制切换总开关已开启
    And config_key 和 key_engine_relation 已注册
    And 但 config_value 表无对应记录
    When 用户 "userP" 从应用 "appP" 提交 Spark2 任务
    Then queryConfigWithGlobalConfig 不应该返回 coercion key
    And 等效于 fallback 到 properties 默认值
    And 任务应该使用 Spark2 引擎执行

  @regression @migration @config
  Scenario: switch 保留 properties 不迁移到配置项管理
    Given 配置项管理未注册 spark.version.coercion.switch
    And properties 中 spark.version.coercion.switch=true
    And 应用级名单在配置项管理中包含 "appQ"
    When 用户 "userQ" 从应用 "appQ" 提交 Spark2 任务
    Then RPC 返回的 map 不应该包含 switch key
    And getValue(map) 对缺失的 switch 应走 properties 默认值 true
    And 任务应该被强制切换到 Spark3

  @regression @migration @config
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

  @regression @migration @config @known-limitation
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
