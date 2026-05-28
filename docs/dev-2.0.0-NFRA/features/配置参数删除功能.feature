# language: zh-CN
功能: 配置参数删除功能
  作为 Linkis 管理员
  我想要删除冗余或过期的配置参数
  以便保持配置列表的整洁性和可维护性

  背景:
    Given 用户已登录 Linkis 系统
    And 用户拥有管理员权限（isLogAdmin 为 true）
    And 用户进入配置管理（Setting）页面
    And 当前显示至少一个配置参数列表

  场景: 管理员查看配置参数时显示删除按钮
    Given 用户是管理员
    And 配置参数列表中有配置项 "spark.executor.memory"
    When 用户查看配置参数列表
    Then 每个配置项右侧应显示删除按钮（红色垃圾桶图标）
    And 删除按钮仅在管理员权限下可见

  场景: 普通用户查看配置参数时不显示删除按钮
    Given 用户是普通用户（非管理员）
    And 配置参数列表中有配置项 "spark.executor.memory"
    When 用户查看配置参数列表
    Then 配置项右侧不显示删除按钮

  场景: 管理员成功删除配置参数
    Given 用户是管理员
    And 配置参数列表中有配置项 "spark.executor.memory"
    And 用户点击该配置项的删除按钮
    When 系统弹出确认对话框
    And 对话框标题为 "确认删除配置参数"
    And 对话框内容包含 "删除配置参数 spark.executor.memory 将会影响所有用户使用此配置，且不可恢复"
    And 用户点击 "确定" 按钮
    Then 该配置项从列表中移除
    And 系统显示成功提示 "删除配置参数成功"
    And 后端 API "/configuration/deleteConfigKey" 被正确调用
    And 请求参数包含 key="spark.executor.memory"

  场景: 管理员取消删除配置参数
    Given 用户是管理员
    And 配置参数列表中有配置项 "spark.executor.memory"
    And 用户点击该配置项的删除按钮
    When 系统弹出确认对话框
    And 用户点击 "取消" 按钮
    Then 该配置项保留在列表中
    And 对话框关闭
    And 列表状态无变化

  场景: 删除配置参数时后端返回错误
    Given 用户是管理员
    And 配置参数列表中有配置项 "wds.linkis.rm.yarnqueue.memory.max"
    And 该配置参数为系统核心参数不允许删除
    And 用户点击该配置项的删除按钮
    When 用户在确认对话框中点击 "确定" 按钮
    Then 后端返回错误信息 "该参数不允许删除"
    And 配置项保留在列表中
    And 系统显示错误提示 "删除配置参数失败：该参数不允许删除"

  场景: 删除配置参数时网络异常
    Given 用户是管理员
    And 配置参数列表中有配置项 "spark.executor.memory"
    And 用户点击该配置项的删除按钮
    When 用户在确认对话框中点击 "确定" 按钮
    And 网络请求失败或超时
    Then 配置项保留在列表中
    And 系统显示错误提示 "删除配置参数失败：网络异常"

  场景: 删除按钮交互效果
    Given 用户是管理员
    And 配置项右侧显示删除按钮
    When 用户鼠标悬停在删除按钮上
    Then 删除按钮图标放大 1.2 倍
    And 图标颜色变为 #f5222d
    When 用户点击删除按钮
    Then 删除按钮有按下效果（缩小至 0.95 倍）

  场景: 在全局设置中删除配置参数
    Given 用户是管理员
    And 用户切换到 "全局设置" 标签页
    And 全局设置中存在配置项 "bdp.dwc.yarnqueue.memory.max"
    When 用户点击该配置项的删除按钮
    And 用户在确认对话框中点击 "确定" 按钮
    Then 后端 API 调用时 engineType 参数为 null
    And creator 参数为 "全局设置" 或 "GlobalSettings"

  场景: 在引擎配置中删除配置参数
    Given 用户是管理员
    And 用户选择应用 "LINKISCLI"
    And 用户选择引擎 "hive-2.3.3"
    And 引擎配置中存在配置项 "hive.executor.instances"
    When 用户点击该配置项的删除按钮
    And 用户在确认对话框中点击 "确定" 按钮
    Then 后端 API 调用时 creator 参数为 "LINKISCLI"
    And engineType 参数为 "hive"

  场景: 删除配置参数后保存其他配置
    Given 用户是管理员
    And 用户删除了配置项 "spark.executor.memory"
    And 用户修改了配置项 "spark.driver.memory" 的值为 "1024m"
    When 用户点击 "保存" 按钮
    Then 系统保存修改后的配置
    And 已删除的配置项不会被保存
    And 系统提示 "保存成功"

  场景: 删除按钮与高级设置功能兼容
    Given 用户是管理员
    And 配置参数列表包含普通配置和高级配置
    And 高级设置默认处于隐藏状态
    When 用户点击 "显示高级设置" 按钮
    Then 高级配置项显示
    And 高级配置项右侧同样显示删除按钮
    When 用户点击 "隐藏高级设置" 按钮
    Then 高级配置项隐藏
    And 删除按钮同步隐藏

  场景: 非管理员用户尝试直接调用删除 API
    Given 用户是普通用户
    And 用户通过直接调用 API 方式尝试删除配置参数
    When 用户发送 POST 请求到 "/configuration/deleteConfigKey"
    And 请求参数为 {"key": "spark.executor.memory", "creator": "LINKISCLI"}
    Then 后端返回权限错误
    And 响应状态码为 401 或 403
    And 响应消息包含 "无权限" 或 "需要管理员权限"

  场景: 批量删除配置参数（不支持）
    Given 用户是管理员
    And 配置参数列表中有多个配置项
    When 用户查看配置参数列表
    Then 界面上不提供批量删除功能
    And 用户需要逐个点击删除按钮进行删除
