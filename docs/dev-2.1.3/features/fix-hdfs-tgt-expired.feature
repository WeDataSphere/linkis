Feature: 修复引擎复用TGT过期问题（Hive/JDBC/HBase公共修复）
  修复开启Kerberos keytab但未开启HDFS缓存时，引擎长时间运行后复用报TGT过期错误的Bug
  公共开关 linkis.engineconn.tgt.refresh.enable 同时控制 Hive/JDBC/HBase 三个引擎
  连续3次刷新失败抛 RuntimeException 报错提示

  Background:
    Given 系统已启动
    And Kerberos认证已开启
    And HDFS缓存已关闭
    And 配置项"linkis.engineconn.tgt.refresh.enable"已声明

  Rule: Bug复现 - 记录修复前的错误行为

    @bug @reproduction @skip
    Scenario: 复现Bug - Hive引擎运行超过TGT有效期后复用失败
      Given Hive引擎已启动并执行过SQL
      And 引擎UGI的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该抛出Kerberos认证异常
      And 错误信息应该包含"TGT"或"Kerberos"
      And 任务状态应该为失败
      And 错误发生在ugi.doAs调用时

    @bug @reproduction @skip
    Scenario: 复现Bug - JDBC引擎运行超过TGT有效期后连接失败
      Given JDBC引擎已启动并执行过SQL
      And JVM全局loginUser的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该抛出Kerberos认证异常
      And 任务状态应该为失败

    @bug @reproduction @skip
    Scenario: 复现Bug - HBase引擎运行超过TGT有效期后连接失败
      Given HBase引擎已启动并执行过SQL
      And JVM全局loginUser的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该抛出Kerberos认证异常
      And 任务状态应该为失败

  Rule: Bug修复 - 验证修复后的正确行为（公共开关）

    @bugfix @critical @smoke
    Scenario: 修复后 - 公共开关关闭时行为与修复前一致
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"false"
      And Hive引擎已启动
      And 引擎UGI的TGT已过期
      When 引擎被复用执行新的SQL
      Then 行为应该与修复前完全一致
      And 不应该触发UGI刷新逻辑
      And 应该使用原始UGI执行doAs

    @bugfix @critical
    Scenario: 修复后 - 公共开关打开且TGT有效时正常执行
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And 引擎UGI的TGT仍然有效
      When 引擎被复用执行新的SQL
      Then SQL应该正常执行
      And 不应该触发UGI刷新
      And 不应该出现Kerberos认证异常

    @bugfix @critical
    Scenario: 修复后 - Hive引擎TGT过期时自动刷新UGI
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And 引擎UGI的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该检测到TGT已过期
      And 系统应该调用KerberosTgtUtils.refreshUgiIfNeeded获取新UGI
      And 应该使用新UGI执行doAs
      And SQL应该正常执行
      And 日志应该记录UGI刷新事件
      And 失败计数器应该重置为0

    @bugfix @critical
    Scenario: 修复后 - JDBC引擎TGT过期时自动刷新loginUser
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And JDBC引擎已启动
      And JVM全局loginUser的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该调用KerberosTgtUtils.refreshLoginUserTgtIfNeeded
      And loginUser的TGT应该被刷新
      And SQL应该正常执行
      And 失败计数器应该重置为0

    @bugfix @critical
    Scenario: 修复后 - HBase引擎TGT过期时自动刷新loginUser
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And HBase引擎已启动
      And JVM全局loginUser的TGT已过期
      When 引擎被复用执行新的SQL
      Then 系统应该调用KerberosTgtUtils.refreshLoginUserTgtIfNeeded
      And loginUser的TGT应该被刷新
      And SQL应该正常执行
      And 失败计数器应该重置为0

  Rule: Bug修复 - 验证重试与报错机制

    @bugfix @critical
    Scenario: 修复后 - Hive UGI刷新前2次失败时降级使用原UGI
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And 引擎UGI的TGT已过期
      And HDFSUtils.getUserGroupInformation调用会抛出异常
      When 引擎被复用执行新的SQL
      Then 系统应该捕获UGI刷新异常
      And 应该使用原始UGI执行doAs
      And 日志应该记录warn级别的刷新失败信息
      And 失败计数器应该为1或2
      And 不应该因为刷新失败而抛出未捕获异常

    @bugfix @critical
    Scenario: 修复后 - Hive UGI连续3次刷新失败时抛异常报错
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And 引擎UGI的TGT已过期
      And UGI刷新失败计数器已经为2
      And HDFSUtils.getUserGroupInformation调用会抛出异常
      When 引擎被复用执行新的SQL
      Then 系统应该抛出RuntimeException
      And 异常信息应该包含"3 times"
      And 异常信息应该包含"keytab"
      And 任务状态应该为失败

    @bugfix @critical
    Scenario: 修复后 - loginUser连续3次刷新失败时抛异常报错
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And JDBC引擎已启动
      And JVM全局loginUser的TGT已过期
      And loginUser刷新失败计数器已经为2
      And checkTGTAndReloginFromKeytab调用后TGT仍然过期
      When 引擎被复用执行新的SQL
      Then 系统应该抛出RuntimeException
      And 异常信息应该包含"3 times"
      And 任务状态应该为失败

    @bugfix
    Scenario: 修复后 - 刷新成功后失败计数器重置为0
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And UGI刷新失败计数器为2
      And 引擎UGI的TGT已过期
      And HDFSUtils.getUserGroupInformation调用成功
      When 引擎被复用执行新的SQL
      Then SQL应该正常执行
      And 失败计数器应该重置为0

  Rule: Bug修复 - 验证并发安全

    @bugfix @critical
    Scenario: 修复后 - 并发模式下UGI刷新线程安全
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive并发引擎(HiveEngineConcurrentConnExecutor)已启动
      And 引擎UGI的TGT已过期
      When 多个线程同时执行SQL
      Then UGI刷新应该是线程安全的
      And 不应该出现竞态条件
      And 所有线程应该使用刷新后的同一个新UGI
      And 所有SQL应该正常执行
      And synchronized同步锁应该保护UGI刷新

  Rule: Bug修复 - 验证close阶段兜底

    @bugfix
    Scenario: 修复后 - Hive引擎close阶段TGT过期不影响关闭
      Given Hive引擎已启动
      And 引擎UGI的TGT已过期
      When 引擎执行close操作
      Then super.close()应该被Utils.tryQuietly包裹
      And 不应该因为TGT过期而抛出未捕获异常
      And 引擎应该正常关闭

    @bugfix
    Scenario: 修复后 - Hive并发引擎close阶段TGT过期不影响关闭
      Given Hive并发引擎已启动
      And 引擎UGI的TGT已过期
      When 引擎执行close操作
      Then super.close()应该被Utils.tryQuietly包裹
      And 不应该因为TGT过期而抛出未捕获异常
      And 引擎应该正常关闭

  Rule: 回归验证 - 确保修复不影响其他场景

    @regression @critical
    Scenario: 非Kerberos场景下引擎复用正常
      Given Kerberos认证未开启
      And 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动并执行过SQL
      When 引擎被复用执行新的SQL
      Then SQL应该正常执行
      And KerberosTgtUtils.isTgtValid应该返回true
      And 不应该触发UGI刷新

    @regression @critical
    Scenario: Kerberos开启HDFS缓存的场景不受影响
      Given Kerberos认证已开启
      And HDFS缓存已开启
      And 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      When 引擎被复用执行新的SQL
      Then SQL应该正常执行
      And 行为应该与修复前一致

    @regression
    Scenario: 引擎首次启动执行SQL正常
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎刚启动
      When 执行第一条SQL
      Then SQL应该正常执行
      And 不应该出现任何异常

    @regression
    Scenario: 引擎复用TGT未过期时SQL正常执行
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      And 引擎UGI的TGT仍然有效
      When 引擎被复用执行新的SQL
      Then SQL应该正常执行
      And 不应该触发UGI刷新
      And 行为应该与开关关闭时一致

    @regression @performance
    Scenario: TGT检查性能开销可忽略
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
      And Hive引擎已启动
      When 连续执行100条SQL
      Then 每条SQL的TGT检查耗时应该小于1毫秒
      And 整体执行性能不应该有明显下降

    @regression
    Scenario Outline: 不同TGT状态的引擎复用场景
      Given 配置项"linkis.engineconn.tgt.refresh.enable"的值为"<switch>"
      And Hive引擎已启动
      And 引擎UGI的TGT状态为"<tgt_status>"
      When 引擎被复用执行新的SQL
      Then 执行结果应该为"<result>"
      And UGI刷新行为应该为"<refresh_behavior>"

      Examples:
        | switch | tgt_status | result | refresh_behavior |
        | false  | valid     | 成功   | 不刷新           |
        | false  | expired   | 失败   | 不刷新           |
        | true   | valid     | 成功   | 不刷新           |
        | true   | expired   | 成功   | 刷新UGI          |
