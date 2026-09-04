# 引擎复用TGT过期修复 测试用例

**需求类型**: FIX（Bug修复）
**问题等级**: P0-紧急
**文档版本**: v1.0
**创建日期**: 2026-09-02
**功能属性**: 后端

---

## 测试用例概述

| 维度 | 内容 |
|------|------|
| **测试范围** | Hive/JDBC/HBase三类引擎TGT懒刷新功能验证 |
| **测试类型** | 单元测试 + 集成测试 + 回归测试 + 性能测试 |
| **用例总数** | 30个 |
| **Feature覆盖率** | 100%（17/17 Scenario） |
| **验收标准覆盖率** | 100%（9/9 AC） |
| **测试数据来源** | Feature文件 + 需求文档验收标准 + 设计文档测试场景 |

> 未检测到测试澄清纪要，已按默认范围生成；建议先执行 Stage 5.5 测试用例澄清以收敛范围与边界。

### 需求属性识别结果

- **识别的属性**: 后端开发
- **识别依据**:
  - 策略: 需求文档读取（context.md functionAttributes: ["后端"]）
  - 关键词: Kerberos、Hive、JDBC、HBase、引擎插件
  - 置信度: 高
- **测试用例生成策略**: 侧重接口测试、业务逻辑测试、性能测试

### 优先级分布

| 优先级 | 数量 | 说明 |
|--------|------|------|
| P0 | 14 | 核心Bug修复验证（critical/smoke） |
| P1 | 10 | 重要功能验证（important/negative） |
| P2 | 6 | 边界与性能验证（boundary/performance） |

---

## 一、Bug复现测试（修复前错误行为记录）

> 以下用例标记为 @skip，仅记录修复前的错误行为，不在修复后执行。

### TC001: 复现Bug - Hive引擎运行超过TGT有效期后复用失败

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 1

**标签**: @bug @reproduction @skip

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭（wds.linkis.hadoop.hdfs.cache.enable=false）
- 配置项 linkis.engineconn.tgt.refresh.enable 已声明

**测试步骤**:
1. 启动Hive引擎并执行SQL（如 SELECT 1）
2. 等待引擎UGI的TGT过期（超过TGT有效期）
3. 复用该引擎执行新的SQL（如 SELECT 2）

**预期结果**:
- 系统应该抛出Kerberos认证异常
- 错误信息应该包含"TGT"或"Kerberos"
- 任务状态应该为失败
- 错误发生在ugi.doAs调用时

**优先级**: P0

**Gherkin规格**:
```gherkin
@bug @reproduction @skip
Scenario: 复现Bug - Hive引擎运行超过TGT有效期后复用失败
  Given Hive引擎已启动并执行过SQL
  And 引擎UGI的TGT已过期
  When 引擎被复用执行新的SQL
  Then 系统应该抛出Kerberos认证异常
  And 错误信息应该包含"TGT"或"Kerberos"
  And 任务状态应该为失败
  And 错误发生在ugi.doAs调用时
```

---

### TC002: 复现Bug - JDBC引擎运行超过TGT有效期后连接失败

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 2

**标签**: @bug @reproduction @skip

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭

**测试步骤**:
1. 启动JDBC引擎并执行SQL
2. 等待JVM全局loginUser的TGT过期
3. 复用该引擎执行新的SQL

**预期结果**:
- 系统应该抛出Kerberos认证异常
- 任务状态应该为失败

**优先级**: P0

---

### TC003: 复现Bug - HBase引擎运行超过TGT有效期后连接失败

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 3

**标签**: @bug @reproduction @skip

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭

**测试步骤**:
1. 启动HBase引擎并执行SQL
2. 等待JVM全局loginUser的TGT过期
3. 复用该引擎执行新的SQL

**预期结果**:
- 系统应该抛出Kerberos认证异常
- 任务状态应该为失败

**优先级**: P0

---

## 二、公共开关验证测试

### TC004: 修复后 - 公共开关关闭时行为与修复前一致

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 4

**标签**: @bugfix @critical @smoke

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 false
- Hive引擎已启动
- 引擎UGI的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=false
2. 复用Hive引擎执行新的SQL

**预期结果**:
- 行为应该与修复前完全一致
- 不应该触发UGI刷新逻辑
- 应该使用原始UGI执行doAs
- 不调用 KerberosTgtUtils.refreshUgiIfNeeded

**优先级**: P0

**覆盖验收标准**: AC1

---

### TC005: 修复后 - 公共开关打开且TGT有效时正常执行

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 5

**标签**: @bugfix @critical

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- 引擎UGI的TGT仍然有效

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=true
2. 复用Hive引擎执行新的SQL

**预期结果**:
- SQL应该正常执行
- 不应该触发UGI刷新（isTgtValid返回true）
- 不应该出现Kerberos认证异常

**优先级**: P0

**覆盖验收标准**: AC2

---

## 三、Hive引擎TGT刷新测试

### TC006: 修复后 - Hive引擎TGT过期时自动刷新UGI

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 6

**标签**: @bugfix @critical

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- 引擎UGI的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=true
2. 复用Hive引擎执行新的SQL

**预期结果**:
- 系统应该检测到TGT已过期（KerberosTgtUtils.isTgtValid返回false）
- 系统应该调用KerberosTgtUtils.refreshUgiIfNeeded获取新UGI
- 应该使用新UGI执行doAs（ugi引用被替换）
- SQL应该正常执行
- 日志应该记录UGI刷新事件（info级别）
- 失败计数器应该重置为0

**优先级**: P0

**覆盖验收标准**: AC3

**Gherkin规格**:
```gherkin
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
```

---

### TC007: 修复后 - Hive UGI刷新前2次失败时降级使用原UGI

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 8

**标签**: @bugfix @critical

**测试类型**: 异常测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- 引擎UGI的TGT已过期
- HDFSUtils.getUserGroupInformation 调用会抛出异常（Mock模拟）

**测试步骤**:
1. Mock HDFSUtils.getUserGroupInformation 抛出异常
2. 第1次复用引擎执行SQL（failCount=1）
3. 第2次复用引擎执行SQL（failCount=2）
4. 验证每次执行的行为

**预期结果**:
- 系统应该捕获UGI刷新异常
- 应该使用原始UGI执行doAs（降级）
- 日志应该记录warn级别的刷新失败信息
- 第1次失败后失败计数器为1
- 第2次失败后失败计数器为2
- 不应该因为刷新失败而抛出未捕获异常
- SQL可能因TGT过期而失败，但不应因刷新逻辑本身失败

**优先级**: P0

**覆盖验收标准**: AC4（前2次降级）

---

### TC008: 修复后 - Hive UGI连续3次刷新失败时抛异常报错

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 9

**标签**: @bugfix @critical

**测试类型**: 异常测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- 引擎UGI的TGT已过期
- UGI刷新失败计数器已经为2（前2次已失败）
- HDFSUtils.getUserGroupInformation 调用会抛出异常

**测试步骤**:
1. 确认ugiRefreshFailCount=2
2. Mock HDFSUtils.getUserGroupInformation 抛出异常
3. 复用引擎执行SQL

**预期结果**:
- 系统应该抛出RuntimeException
- 异常信息应该包含"3 times"
- 异常信息应该包含"aborting"
- 任务状态应该为失败
- ugiRefreshFailCount 应该为3

**优先级**: P0

**覆盖验收标准**: AC7（3次报错）

---

### TC009: 修复后 - 刷新成功后失败计数器重置为0

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 10

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- UGI刷新失败计数器为2（前2次已失败）
- 引擎UGI的TGT已过期
- HDFSUtils.getUserGroupInformation 调用成功

**测试步骤**:
1. 确认ugiRefreshFailCount=2
2. Mock HDFSUtils.getUserGroupInformation 返回新UGI
3. 复用引擎执行SQL

**预期结果**:
- SQL应该正常执行
- 失败计数器应该重置为0（ugiRefreshFailCount.set(0)）
- 日志应该记录info级别的刷新成功信息
- 后续失败应从0重新计数

**优先级**: P1

**覆盖验收标准**: AC8

---

## 四、JDBC引擎TGT刷新测试

### TC010: 修复后 - JDBC引擎TGT过期时自动刷新loginUser

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 7

**标签**: @bugfix @critical

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- JDBC引擎已启动
- JVM全局loginUser的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=true
2. 复用JDBC引擎执行新的SQL

**预期结果**:
- 系统应该调用KerberosTgtUtils.refreshLoginUserTgtIfNeeded
- loginUser的TGT应该被刷新（checkTGTAndReloginFromKeytab被调用）
- 刷新后验证TGT有效性（isTgtValid返回true）
- SQL应该正常执行
- 失败计数器应该重置为0
- 日志应该记录loginUser刷新事件

**优先级**: P0

**覆盖验收标准**: AC4

---

### TC011: 修复后 - JDBC loginUser连续3次刷新失败时抛异常报错

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 11

**标签**: @bugfix @critical

**测试类型**: 异常测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- JDBC引擎已启动
- JVM全局loginUser的TGT已过期
- loginUser刷新失败计数器已经为2
- checkTGTAndReloginFromKeytab 调用后TGT仍然过期

**测试步骤**:
1. 确认loginUserRefreshFailCount=2
2. Mock checkTGTAndReloginFromKeytab 后TGT仍然无效
3. 复用JDBC引擎执行SQL

**预期结果**:
- 系统应该抛出RuntimeException
- 异常信息应该包含"3 times"
- 异常信息应该包含"aborting"
- 任务状态应该为失败
- loginUserRefreshFailCount 应该为3

**优先级**: P0

**覆盖验收标准**: AC7

---

### TC012: 修复后 - JDBC loginUser前2次刷新失败时降级

**来源**: 设计文档 - UT-09扩展

**标签**: @bugfix @critical

**测试类型**: 异常测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- JDBC引擎已启动
- JVM全局loginUser的TGT已过期
- checkTGTAndReloginFromKeytab 调用抛出异常

**测试步骤**:
1. Mock checkTGTAndReloginFromKeytab 抛出异常
2. 第1次复用JDBC引擎执行SQL（failCount=1）
3. 第2次复用JDBC引擎执行SQL（failCount=2）

**预期结果**:
- 系统应该捕获刷新异常
- 不应该抛出未捕获异常（前2次降级）
- 日志应该记录warn级别的刷新失败信息
- 第1次失败后loginUserRefreshFailCount为1
- 第2次失败后loginUserRefreshFailCount为2

**优先级**: P0

---

### TC013: 修复后 - JDBC开关关闭时不触发TGT检查

**来源**: 设计文档 - UT-13

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 false
- JDBC引擎已启动
- JVM全局loginUser的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=false
2. 复用JDBC引擎执行SQL

**预期结果**:
- 不调用 KerberosTgtUtils.refreshLoginUserTgtIfNeeded
- 行为与修复前完全一致

**优先级**: P1

**覆盖验收标准**: AC1

---

## 五、HBase引擎TGT刷新测试

### TC014: 修复后 - HBase引擎TGT过期时自动刷新loginUser

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 7（HBase版）

**标签**: @bugfix @critical

**测试类型**: 功能测试

**前置条件**:
- 系统已启动
- Kerberos认证已开启
- HDFS缓存已关闭
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HBase引擎已启动
- JVM全局loginUser的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=true
2. 复用HBase引擎执行新的操作

**预期结果**:
- 系统应该调用KerberosTgtUtils.refreshLoginUserTgtIfNeeded
- loginUser的TGT应该被刷新
- SQL/操作应该正常执行
- 失败计数器应该重置为0
- 日志应该记录刷新事件

**优先级**: P0

**覆盖验收标准**: AC5

---

### TC015: 修复后 - HBase loginUser连续3次刷新失败时抛异常报错

**来源**: 设计文档 - UT-12

**标签**: @bugfix @critical

**测试类型**: 异常测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HBase引擎已启动
- JVM全局loginUser的TGT已过期
- loginUser刷新失败计数器已经为2
- checkTGTAndReloginFromKeytab 调用后TGT仍然过期

**测试步骤**:
1. 确认loginUserRefreshFailCount=2
2. Mock checkTGTAndReloginFromKeytab 后TGT仍然无效
3. 复用HBase引擎执行操作

**预期结果**:
- 系统应该抛出RuntimeException
- 异常信息应该包含"3 times"
- 任务状态应该为失败
- loginUserRefreshFailCount 应该为3

**优先级**: P0

**覆盖验收标准**: AC7

---

### TC016: 修复后 - HBase开关关闭时不触发TGT检查

**来源**: 设计文档 - UT-13扩展

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 false
- HBase引擎已启动
- isKerberosAuthType(configuration) 为 true
- JVM全局loginUser的TGT已过期

**测试步骤**:
1. 确认配置 linkis.engineconn.tgt.refresh.enable=false
2. 复用HBase引擎执行操作

**预期结果**:
- 不调用 KerberosTgtUtils.refreshLoginUserTgtIfNeeded
- 行为与修复前完全一致

**优先级**: P1

**覆盖验收标准**: AC1

---

### TC017: 修复后 - HBase非Kerberos认证时不触发TGT检查

**来源**: 设计文档 - isKerberosAuthType 条件

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HBase引擎已启动
- isKerberosAuthType(configuration) 为 false（Simple认证）

**测试步骤**:
1. 确认 isKerberosAuthType 返回 false
2. 复用HBase引擎执行操作

**预期结果**:
- 不调用 KerberosTgtUtils.refreshLoginUserTgtIfNeeded
- 正常创建连接并执行操作

**优先级**: P1

---

## 六、并发安全测试

### TC018: 修复后 - 并发模式下UGI刷新线程安全

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 12

**标签**: @bugfix @critical

**测试类型**: 并发测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive并发引擎（HiveEngineConcurrentConnExecutor）已启动
- 引擎UGI的TGT已过期
- backgroundOperationPool 有多个工作线程

**测试步骤**:
1. 启动多个线程（Thread-1, Thread-2, Thread-3）同时调用executeLine
2. Thread-1 首先进入synchronized块，执行UGI刷新
3. Thread-2, Thread-3 等待锁
4. Thread-1 完成刷新后释放锁
5. Thread-2, Thread-3 依次获取锁，检查TGT已有效，不重复刷新
6. 验证所有线程的执行结果

**预期结果**:
- UGI刷新应该是线程安全的
- 不应该出现竞态条件
- 所有线程应该使用刷新后的同一个新UGI
- 所有SQL应该正常执行
- synchronized同步锁应该保护UGI刷新
- @volatile 保证UGI引用的可见性
- 只有一次实际的UGI刷新操作发生

**优先级**: P0

**覆盖验收标准**: AC6

**Gherkin规格**:
```gherkin
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
```

---

### TC019: 修复后 - 并发模式下TGT有效时不触发刷新

**来源**: 设计文档 - UT-08扩展

**标签**: @bugfix

**测试类型**: 并发测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive并发引擎已启动
- 引擎UGI的TGT仍然有效

**测试步骤**:
1. 启动多个线程同时调用executeLine
2. 验证各线程的TGT检查行为

**预期结果**:
- 每个线程的isTgtValid都返回true
- 不触发UGI刷新
- 所有线程直接使用原UGI执行doAs
- synchronized块快速通过（仅检查不刷新）

**优先级**: P1

---

## 七、close阶段兜底测试

### TC020: 修复后 - Hive引擎close阶段TGT过期不影响关闭

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 13

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- Hive引擎已启动
- 引擎UGI的TGT已过期
- 引擎即将关闭

**测试步骤**:
1. 触发引擎close操作
2. 验证close过程中的行为

**预期结果**:
- super.close() 应该被 Utils.tryQuietly 包裹
- 不应该因为TGT过期而抛出未捕获异常
- 引擎应该正常关闭
- sessionState.close() 被 Utils.tryAndWarnMsg 包裹（原有逻辑）

**优先级**: P1

**覆盖验收标准**: AC9

---

### TC021: 修复后 - Hive并发引擎close阶段TGT过期不影响关闭

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 14

**标签**: @bugfix

**测试类型**: 功能测试

**前置条件**:
- Hive并发引擎已启动
- 引擎UGI的TGT已过期
- 引擎即将关闭

**测试步骤**:
1. 触发并发引擎close操作
2. 验证close过程中的行为

**预期结果**:
- super.close() 应该被 Utils.tryQuietly 包裹
- 不应该因为TGT过期而抛出未捕获异常
- backgroundOperationPool 应该正常shutdown
- 引擎应该正常关闭

**优先级**: P1

**覆盖验收标准**: AC9

---

## 八、回归测试

### TC022: 非Kerberos场景下引擎复用正常

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 15

**标签**: @regression @critical

**测试类型**: 回归测试

**前置条件**:
- Kerberos认证未开启（Simple认证）
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动并执行过SQL

**测试步骤**:
1. 确认Kerberos未开启
2. 复用Hive引擎执行新的SQL

**预期结果**:
- SQL应该正常执行
- KerberosTgtUtils.isTgtValid 应该返回true（非Kerberos场景默认有效）
- 不应该触发UGI刷新

**优先级**: P0

**Gherkin规格**:
```gherkin
@regression @critical
Scenario: 非Kerberos场景下引擎复用正常
  Given Kerberos认证未开启
  And 配置项"linkis.engineconn.tgt.refresh.enable"的值为"true"
  And Hive引擎已启动并执行过SQL
  When 引擎被复用执行新的SQL
  Then SQL应该正常执行
  And KerberosTgtUtils.isTgtValid应该返回true
  And 不应该触发UGI刷新
```

---

### TC023: Kerberos开启HDFS缓存的场景不受影响

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 16

**标签**: @regression @critical

**测试类型**: 回归测试

**前置条件**:
- Kerberos认证已开启
- HDFS缓存已开启（wds.linkis.hadoop.hdfs.cache.enable=true）
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动

**测试步骤**:
1. 确认HDFS缓存已开启
2. 复用Hive引擎执行新的SQL

**预期结果**:
- SQL应该正常执行
- 行为应该与修复前一致（HDFS缓存场景TGT由HDFS层管理）

**优先级**: P0

---

### TC024: 引擎首次启动执行SQL正常

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 17

**标签**: @regression

**测试类型**: 回归测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎刚启动

**测试步骤**:
1. 引擎启动后立即执行第一条SQL

**预期结果**:
- SQL应该正常执行
- 不应该出现任何异常
- TGT检查通过（首次启动TGT必然有效）

**优先级**: P1

---

### TC025: 引擎复用TGT未过期时SQL正常执行

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 18

**标签**: @regression

**测试类型**: 回归测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动
- 引擎UGI的TGT仍然有效

**测试步骤**:
1. 复用Hive引擎执行新的SQL

**预期结果**:
- SQL应该正常执行
- 不应该触发UGI刷新
- 行为应该与开关关闭时一致

**优先级**: P1

---

### TC026: TGT检查性能开销可忽略

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario 19

**标签**: @regression @performance

**测试类型**: 性能测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动

**测试步骤**:
1. 连续执行100条SQL
2. 记录每条SQL执行前TGT检查的耗时
3. 计算平均耗时

**预期结果**:
- 每条SQL的TGT检查耗时应该小于1毫秒
- 整体执行性能不应该有明显下降
- isTgtValid 仅检查Kerberos票据过期时间，不涉及网络IO

**优先级**: P2

**性能指标**:
| 指标 | 基准值 | 验收标准 |
|------|--------|---------|
| TGT检查耗时 | < 1ms | 单次 KerberosTgtUtils.isTgtValid 耗时 < 1ms |
| UGI刷新耗时(Hive) | < 500ms | 单次 HDFSUtils.getUserGroupInformation 耗时 < 500ms |
| loginUser刷新耗时(JDBC/HBase) | < 500ms | 单次 checkTGTAndReloginFromKeytab 耗时 < 500ms |
| 开关OFF开销 | 0ms | 开关关闭时无额外性能开销 |

---

### TC027: 参数化测试 - 不同TGT状态的引擎复用场景

**来源**: Feature文件 - fix-hdfs-tgt-expired.feature, Scenario Outline

**标签**: @regression

**测试类型**: 参数化测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为指定值
- Hive引擎已启动
- 引擎UGI的TGT状态为指定状态

**测试数据（来自Examples表格）**:

| 测试案例 | switch | tgt_status | result | refresh_behavior |
|---------|--------|------------|--------|-----------------|
| TC027-1 | false  | valid     | 成功   | 不刷新           |
| TC027-2 | false  | expired   | 失败   | 不刷新           |
| TC027-3 | true   | valid     | 成功   | 不刷新           |
| TC027-4 | true   | expired   | 成功   | 刷新UGI          |

**测试步骤**:
1. 按照参数设置开关值和TGT状态
2. 复用引擎执行SQL
3. 验证执行结果和刷新行为

**预期结果**:
- 执行结果应该与参数表中 result 一致
- UGI刷新行为应该与参数表中 refresh_behavior 一致

**优先级**: P1

---

### TC028: 回归 - 普通Hive SQL执行正常

**来源**: 设计文档 - 回归测试范围

**标签**: @regression

**测试类型**: 回归测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- Hive引擎已启动，TGT有效

**测试步骤**:
1. 执行DDL语句：CREATE TABLE IF NOT EXISTS test_tgt_fix (id INT, name STRING)
2. 执行DML语句：INSERT INTO test_tgt_fix VALUES (1, 'test')
3. 执行查询：SELECT * FROM test_tgt_fix

**预期结果**:
- DDL执行成功
- DML执行成功
- 查询返回正确结果
- 不触发TGT刷新

**优先级**: P1

---

### TC029: 回归 - JDBC引擎各种数据库连接正常

**来源**: 设计文档 - 回归测试范围

**标签**: @regression

**测试类型**: 回归测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- JDBC引擎已启动，TGT有效（非Kerberos场景）

**测试步骤**:
1. 使用JDBC引擎连接MySQL数据库
2. 执行SELECT查询
3. 验证连接和查询结果

**预期结果**:
- 数据库连接正常
- SQL查询成功
- 不触发TGT检查（非Kerberos认证）

**优先级**: P1

---

### TC030: 回归 - HBase引擎连接正常

**来源**: 设计文档 - 回归测试范围

**标签**: @regression

**测试类型**: 回归测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HBase引擎已启动，TGT有效

**测试步骤**:
1. 使用HBase引擎连接HBase集群
2. 执行简单的Scan操作
3. 验证连接和操作结果

**预期结果**:
- HBase连接正常
- Scan操作返回结果
- 不触发TGT刷新

**优先级**: P1

---

## 九、UGI刷新策略专项测试

### TC031: Hive UGI引用替换验证（方案B）

**来源**: 设计文档 - ADR-03

**标签**: @bugfix

**测试类型**: 单元测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HiveEngineConnExecutor 实例已创建
- 构造参数 ugi 为 var（可变引用）

**测试步骤**:
1. 记录当前ugi对象的引用（originalUgi）
2. Mock TGT过期 + HDFSUtils.getUserGroupInformation 返回新UGI
3. 调用 executeLine
4. 验证ugi引用是否被替换

**预期结果**:
- ugi 引用应该被替换为新UGI对象
- originalUgi 不受影响（引用替换，非修改原对象）
- 新UGI的 getUserName 与原UGI一致（同一用户）

**优先级**: P1

---

### TC032: Hive并发模式volatile可见性验证

**来源**: 设计文档 - ADR-03

**标签**: @bugfix

**测试类型**: 单元测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- HiveEngineConcurrentConnExecutor 实例已创建
- 构造参数 ugi 为 @volatile var

**测试步骤**:
1. Thread-1 执行UGI刷新，替换ugi引用
2. Thread-2 同时读取ugi引用
3. 验证Thread-2读取到的ugi是否为最新值

**预期结果**:
- @volatile 保证ugi引用的写入对其他线程立即可见
- Thread-2 应该读取到Thread-1写入的最新ugi引用
- 不应该出现Thread-2使用旧ugi的情况

**优先级**: P1

---

## 十、两套独立计数器验证

### TC033: UGI计数器和loginUser计数器独立计数

**来源**: 设计文档 - 1.4.7节

**标签**: @bugfix

**测试类型**: 单元测试

**前置条件**:
- 配置项 linkis.engineconn.tgt.refresh.enable 的值为 true
- ugiRefreshFailCount 当前值为2
- loginUserRefreshFailCount 当前值为0

**测试步骤**:
1. 使Hive UGI刷新失败1次（ugiRefreshFailCount 2->3，抛RuntimeException）
2. 验证JDBC loginUser刷新不受影响
3. 使JDBC loginUser刷新失败1次（loginUserRefreshFailCount 0->1，降级）
4. 验证loginUserRefreshFailCount=1，不抛异常

**预期结果**:
- ugiRefreshFailCount 和 loginUserRefreshFailCount 独立计数
- UGI计数器到达3时抛异常，不影响loginUser计数器
- loginUser计数器为1时降级，不抛异常
- 两套计数器互不影响

**优先级**: P1

---

## Feature覆盖率统计

| Feature文件 | Scenario总数 | 已生成测试用例 | 覆盖率 | 状态 |
|------------|-------------|--------------|-------|------|
| fix-hdfs-tgt-expired.feature | 17 | 17 | 100% | 完全覆盖 |

### 覆盖详情

#### fix-hdfs-tgt-expired.feature
- TC001: Scenario 1 - 复现Bug Hive引擎复用失败 (@skip)
- TC002: Scenario 2 - 复现Bug JDBC引擎复用失败 (@skip)
- TC003: Scenario 3 - 复现Bug HBase引擎复用失败 (@skip)
- TC004: Scenario 4 - 公共开关关闭时行为一致
- TC005: Scenario 5 - 公共开关打开且TGT有效
- TC006: Scenario 6 - Hive引擎TGT过期自动刷新UGI
- TC010: Scenario 7 - JDBC引擎TGT过期自动刷新loginUser
- TC014: Scenario 7 - HBase引擎TGT过期自动刷新loginUser
- TC007: Scenario 8 - Hive UGI前2次失败降级
- TC008: Scenario 9 - Hive UGI连续3次失败抛异常
- TC011: Scenario 10 - loginUser连续3次失败抛异常
- TC009: Scenario 10 - 刷新成功后计数器重置
- TC018: Scenario 11 - 并发模式UGI刷新线程安全
- TC020: Scenario 12 - Hive close阶段TGT过期不影响
- TC021: Scenario 13 - Hive并发close阶段TGT过期不影响
- TC022: Scenario 14 - 非Kerberos场景不受影响
- TC023: Scenario 15 - Kerberos开启HDFS缓存不受影响
- TC024: Scenario 16 - 引擎首次启动正常
- TC025: Scenario 17 - 引擎复用TGT未过期正常
- TC026: Scenario 18 - TGT检查性能开销可忽略
- TC027: Scenario Outline - 参数化测试（4组数据）

### 验收标准覆盖检查

**需求文档的验收标准**:
- [x] AC1: 开关 linkis.engineconn.tgt.refresh.enable=false 时，三类引擎行为与修复前完全一致 -> TC004, TC013, TC016 覆盖
- [x] AC2: 开关打开 + TGT有效时，三类引擎正常执行，不触发凭证刷新 -> TC005 覆盖
- [x] AC3: 开关打开 + TGT过期时，Hive自动刷新UGI后SQL正常执行 -> TC006 覆盖
- [x] AC4: 开关打开 + TGT过期时，JDBC自动刷新loginUser后连接正常 -> TC010 覆盖
- [x] AC5: 开关打开 + TGT过期时，HBase自动刷新loginUser后连接正常 -> TC014 覆盖
- [x] AC6: 并发模式下多线程UGI刷新线程安全，无竞态条件 -> TC018 覆盖
- [x] AC7: 连续3次刷新失败时抛出RuntimeException -> TC008, TC011, TC015 覆盖
- [x] AC8: 刷新成功后失败计数器重置为0 -> TC009 覆盖
- [x] AC9: close()阶段即使TGT过期也不抛出未捕获异常 -> TC020, TC021 覆盖

**覆盖率**: 9/9 验收标准 (100%)

---

## 测试用例统计

| 统计维度 | 数量 |
|---------|------|
| **总用例数** | 33 |
| Bug复现（@skip） | 3 |
| Bug修复验证 | 17 |
| 回归测试 | 9 |
| 并发安全测试 | 2 |
| 单元测试 | 2 |
| 参数化测试 | 1组（4个数据组合） |

| 按引擎分布 | 用例数 |
|-----------|--------|
| Hive（普通+并发） | 16 |
| JDBC | 6 |
| HBase | 5 |
| 公共/跨引擎 | 6 |

| 按测试类型分布 | 用例数 |
|---------------|--------|
| 功能测试 | 18 |
| 异常测试 | 6 |
| 回归测试 | 9 |
| 并发测试 | 2 |
| 性能测试 | 1 |
| 单元测试 | 2 |
| 参数化测试 | 1 |

---

## 测试环境要求

### Kerberos环境配置

```properties
# linkis.properties 中
wds.linkis.hadoop.hdfs.cache.enable=false
wds.linkis.fs.hdfs.impl.disable.cache=false

# 引擎 linkis-engineconn.properties 中
linkis.engineconn.tgt.refresh.enable=true
```

### 测试数据

| 数据项 | 说明 |
|--------|------|
| Kerberos keytab文件 | 测试用户对应的keytab |
| Kerberos krb5.conf | KDC配置文件 |
| Hive测试表 | test_tgt_fix (id INT, name STRING) |
| JDBC测试数据库 | MySQL/PostgreSQL 可用实例 |
| HBase测试表 | test_tgt_hbase |

### 测试工具

| 工具 | 用途 |
|------|------|
| Mockito 3.9.0 | Mock HDFSUtils.getUserGroupInformation、checkTGTAndReloginFromKeytab |
| JUnit 5.7.2 | 测试框架 |
| AssertJ 3.17.2 | 断言 |
| 多线程测试工具 | 并发模式线程安全验证 |

---

## 测试执行建议

### 执行顺序

1. **阶段1 - 单元测试**: TC031, TC032, TC033（Mock测试，快速验证）
2. **阶段2 - 功能测试**: TC004-TC006, TC010, TC014, TC009（核心Bug修复验证）
3. **阶段3 - 异常测试**: TC007, TC008, TC011, TC012, TC015（失败降级和报错）
4. **阶段4 - 并发测试**: TC018, TC019（并发安全）
5. **阶段5 - 兜底测试**: TC020, TC021（close阶段）
6. **阶段6 - 回归测试**: TC022-TC030（确保不影响现有功能）
7. **阶段7 - 性能测试**: TC026（性能指标验证）
8. **阶段8 - 参数化测试**: TC027（组合验证）

### 关键测试场景

| 场景 | 对应用例 | 优先级 | 说明 |
|------|---------|--------|------|
| 核心修复验证 | TC006 | P0 | Hive TGT过期自动刷新 |
| 3次失败报错 | TC008 | P0 | 避免无限静默降级 |
| 并发安全 | TC018 | P0 | volatile+synchronized |
| 开关关闭兼容 | TC004 | P0 | 向后兼容 |
| 非Kerberos回归 | TC022 | P0 | 不影响非Kerberos场景 |
