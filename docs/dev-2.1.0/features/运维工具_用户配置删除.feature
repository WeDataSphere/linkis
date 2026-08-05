Feature: 运维工具-用户配置删除功能
  为用户配置管理模块增加删除能力，支持管理员通过界面删除多余的配置项

  作为系统管理员
  我希望能够通过界面删除用户配置
  以便快速清理多余或错误的配置，提升运维效率

  Background:
    Given 系统已启动
    And 用户已登录
    And 配置管理功能正常

  Rule: 必须保持现有配置查看和编辑功能不受影响

    @regression @critical
    Scenario: 增强后配置查看功能正常
      Given 配置数据库中存在用户配置
      And 普通用户"user1"已登录
      When 用户访问配置管理页面
      And 用户查询自己的配置列表
      Then 应该显示用户的配置列表
      And 行为应该与增强前完全一致
      And 不应该显示删除按钮

    @regression @critical
    Scenario: 增强后配置编辑功能正常
      Given 配置数据库中存在用户配置
      And 普通用户"user1"已登录
      When 用户访问配置管理页面
      And 用户点击编辑某个配置
      Then 应该打开编辑对话框
      And 用户可以修改配置值
      And 保存后配置应该更新成功

  Rule: 删除功能仅对管理员可用

    @smoke @authorization
    Scenario: 管理员可以看到删除按钮
      Given 配置数据库中存在用户配置
      And 管理员"admin"已登录
      When 管理员访问配置管理页面
      Then 每个配置项的操作列应该显示"删除"按钮

    @authorization
    Scenario: 普通用户看不到删除按钮
      Given 配置数据库中存在用户配置
      And 普通用户"user1"已登录
      When 用户访问配置管理页面
      Then 操作列不应该显示"删除"按钮
      And 只应该显示"编辑"按钮

  Rule: 支持管理员删除用户配置

    @smoke @new-feature
    Scenario: 成功删除用户配置
      Given 配置数据库中存在配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.test.key | hive | 2.3.0 | IDE | test-value |
      And 管理员"admin"已登录
      When 管理员点击配置"wds.linkis.test.key"的"删除"按钮
      And 管理员在确认对话框中点击"确认"
      Then 应该显示"删除成功"提示
      And 配置列表应该自动刷新
      And 配置"wds.linkis.test.key"不应该再显示在列表中
      And 数据库中该配置应该被删除

    @negative
    Scenario: 用户取消删除操作
      Given 配置数据库中存在配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.test.key | hive | 2.3.0 | IDE | test-value |
      And 管理员"admin"已登录
      When 管理员点击配置"wds.linkis.test.key"的"删除"按钮
      And 管理员在确认对话框中点击"取消"
      Then 确认对话框应该关闭
      And 配置列表应该保持不变
      And 配置"wds.linkis.test.key"应该仍然显示在列表中
      And 数据库中该配置应该仍然存在

    @boundary
    Scenario: 删除配置时显示完整配置信息
      Given 配置数据库中存在配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.test.key | hive | 2.3.0 | IDE | test-value |
      And 管理员"admin"已登录
      When 管理员点击配置"wds.linkis.test.key"的"删除"按钮
      Then 确认对话框应该显示:
        | 配置键 | 引擎类型 | 版本 | 创建者 |
        | wds.linkis.test.key | hive | 2.3.0 | IDE |

    @negative
    Scenario: 删除失败时显示错误信息
      Given 配置数据库中存在配置
      And 管理员"admin"已登录
      And 后端服务不可用或返回错误
      When 管理员点击某个配置的"删除"按钮
      And 管理员在确认对话框中点击"确认"
      Then 应该显示删除失败的错误提示
      And 确认对话框应该保持打开
      And 配置列表应该保持不变

    @negative
    Scenario: 删除不存在的配置
      Given 配置数据库中不存在某个配置
      And 管理员"admin"已登录
      When 管理员尝试删除该不存在的配置
      Then 应该显示"配置不存在"的错误提示
      And 配置列表应该保持不变

    @boundary
    Scenario: 删除正在使用的重要配置
      Given 配置数据库中存在关键配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.rm.yarnqueue.memory.max | * | * | * | 100G |
      And 管理员"admin"已登录
      When 管理员点击该配置的"删除"按钮
      Then 确认对话框应该显示警告:"此操作可能影响正在运行的作业"
      And 管理员仍然可以执行删除操作

  Rule: 删除操作的安全性验证

    @security @authorization
    Scenario: 非管理员用户无法直接调用删除接口
      Given 普通用户"user1"已登录
      When 用户尝试直接调用删除接口 DELETE /configuration/keyvalue
      Then 应该返回权限不足错误
      And HTTP状态码应该是403或401
      And 配置不应该被删除

    @security
    Scenario: 删除接口需要管理员权限
      Given 后端删除接口已启用管理员权限检查
      And 普通用户"user1"已登录
      When 用户尝试通过界面删除配置
      Then 删除按钮不应该显示
      And 用户无法触发删除操作

  Rule: 删除操作不影响配置定义

    @regression
    Scenario: 删除用户配置后配置定义仍然存在
      Given 配置数据库中存在配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.test.key | hive | 2.3.0 | IDE | test-value |
      And 配置"wds.linkis.test.key"的配置定义已存在
      And 管理员"admin"已登录
      When 管理员删除该配置值
      Then 配置值应该被删除
      And 配置定义应该仍然存在
      And 用户可以重新创建该配置

  Rule: 支持删除不同类型的配置

    @boundary
    Scenario: 删除全局配置
      Given 配置数据库中存在全局配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.global.config | * | * | * | global-value |
      And 管理员"admin"已登录
      When 管理员删除该全局配置
      Then 全局配置应该被成功删除

    @boundary
    Scenario: 删除引擎特定配置
      Given 配置数据库中存在引擎特定配置:
        | configKey | engineType | version | creator | configValue |
        | wds.linkis.spark.executor.memory | spark | 2.4.3 | IDE | 4g |
      And 管理员"admin"已登录
      When 管理员删除该引擎配置
      Then 引擎配置应该被成功删除
      And 不应该影响其他引擎的相同配置

    @boundary
    Scenario: 删除用户特定配置
      Given 配置数据库中存在用户特定配置:
        | configKey | engineType | version | creator | user | configValue |
        | wds.linkis.user.queue | hive | 2.3.0 | IDE | user1 | queueA |
      And 管理员"admin"已登录
      When 管理员删除该用户配置
      Then 用户配置应该被成功删除
      And 不应该影响其他用户的相同配置
