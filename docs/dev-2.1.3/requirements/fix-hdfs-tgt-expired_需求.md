# 引擎复用TGT过期修复 需求文档

**需求类型**: FIX（Bug修复）
**问题等级**: P0-紧急
**文档版本**: v2.0
**创建日期**: 2026-09-01
**更新日期**: 2026-09-02
**需求来源**: DPMS Story #544181
**功能属性**: 后端
**前端开发类型**: 不涉及前端

---

## 📋 问题速览

| 维度 | 内容 |
|-----|------|
| **问题概述** | 引擎复用时TGT过期，导致Hive/JDBC/HBase三类引擎任务执行失败 |
| **紧急程度** | P0-紧急 |
| **影响范围** | 所有开启Kerberos keytab但未开启HDFS缓存的Hive/JDBC/HBase引擎实例 |
| **问题模块** | linkis-engineconn-plugins/hive、linkis-engineconn-plugins/jdbc、linkis-engineconn-plugins/hbase |
| **复现概率** | 必现（引擎运行超过TGT有效期后复用） |
| **发现环境** | 生产（金监局环境） |

> 💡 **阅读指引**：速览问题看本卡片 -> 复现步骤看第一章 -> 根因分析看第二章 -> 修复方案看第三章

---

## 1. 问题定位【核心】

### 1.1 问题描述

金监局使用 Linkis 开启 keytab（Kerberos 认证），但未开启 HDFS 缓存（`wds.linkis.hadoop.hdfs.cache.enable=false`，`wds.linkis.fs.hdfs.impl.disable.cache=false`）。外部 keytab 刷新频率为一小时一次。在引擎使用超过一天的场景下，Hive、JDBC、HBase 三类引擎复用会报 TGT 过期错误，导致任务执行失败。

三类引擎的问题表现一致，但根因路径不同：
- **Hive 引擎**：独立 UGI 对象在引擎启动时创建后永不更新，TGT 过期后 `ugi.doAs` 失败
- **JDBC 引擎**：使用 JVM 全局 loginUser，TGT 过期后 Kerberos 认证连接失败
- **HBase 引擎**：同样使用 JVM 全局 loginUser，TGT 过期后 HBase 连接创建失败

### 1.2 影响范围评估

| 维度 | 评估 |
|------|------|
| **受影响用户** | 金监局等所有开启Kerberos但未开启HDFS缓存的租户 |
| **受影响功能** | Hive/JDBC/HBase引擎复用模式下的所有SQL/数据操作执行 |
| **业务影响** | 长时间运行的引擎复用时任务必现失败，影响数据分析业务连续性 |
| **数据影响** | 无数据损坏风险，但任务无法完成执行 |

### 1.3 紧急程度判定

**P0-紧急**：生产环境中必现的Bug，直接导致任务失败，需要尽快修复。

### 1.4 复现步骤

| 步骤 | 操作 | 预期结果 | 实际结果 |
|:----:|------|---------|---------|
| 1 | 配置 Linkis 开启 Kerberos keytab 认证 | 正常启动 | 正常启动 |
| 2 | 设置 `wds.linkis.hadoop.hdfs.cache.enable=false` | 关闭HDFS缓存 | 关闭HDFS缓存 |
| 3 | 设置 `wds.linkis.fs.hdfs.impl.disable.cache=false` | 关闭FS缓存 | 关闭FS缓存 |
| 4 | 启动 Hive/JDBC/HBase 引擎并执行操作 | 正常执行 | 正常执行 |
| 5 | 等待超过 TGT 有效期（默认约10小时，keytab刷新频率1小时） | 引擎保持空闲 | 引擎保持空闲 |
| 6 | 复用该引擎执行新操作 | 正常执行 | TGT过期错误，任务失败 |

### 1.5 错误现象

- 引擎复用时抛出 Kerberos 认证异常
- 错误信息包含 TGT 过期相关内容
- 任务状态标记为失败
- 引擎心跳正常（因为引擎复用检查只看心跳状态，不检查TGT）

---

## 2. 根因分析【核心】

### 2.1 问题代码定位

| 文件 | 位置 | 问题描述 |
|------|------|---------|
| `HiveEngineConnFactory.scala` | 第112-118行 `doCreateHiveSession` | UGI只在引擎启动时创建一次，之后整个生命周期不变 |
| `HiveEngineConnFactory.scala` | 第104-110行 `doCreateHiveConcurrentSession` | 同上，并发模式下UGI也只创建一次 |
| `HiveEngineConnExecutor.scala` | 第222-252行 `executeLine` | 使用固定UGI执行`ugi.doAs`，不检查TGT有效性 |
| `HiveEngineConcurrentConnExecutor.scala` | 第183行 `executeLine` | 同上，并发执行器使用固定UGI |
| `ConnectionManager.java` | KERBEROS case `createKerberosSecureConfiguration` | JDBC引擎使用JVM全局loginUser，不检查TGT有效性 |
| `HBaseConnectionManager.java` | `doKerberosLogin` / `getConnection` | HBase引擎使用JVM全局loginUser，不检查TGT有效性 |

### 2.2 5Why分析

| 层级 | 问题 | 答案 |
|:----:|------|------|
| Why 1 | 为什么引擎复用时任务执行失败？ | 因为UGI/loginUser中的TGT已过期，Kerberos认证抛出异常 |
| Why 2 | 为什么TGT会过期？ | 因为外部keytab刷新频率为1小时一次，TGT有效期有限（通常约10小时），引擎运行超过一天后TGT必然过期 |
| Why 3 | 为什么TGT过期后没有自动刷新？ | Hive EC的UGI对象在`doCreateHiveSession`中创建后永不更新；JDBC/HBase EC使用JVM全局loginUser，没有在连接前检查TGT有效性 |
| Why 4 | 为什么引擎复用时不检查TGT有效性？ | 因为`DefaultEngineNodeManager.reuseEngine`只检查引擎心跳状态，不检查Kerberos凭证有效性 |
| Why 5 | **根本原因是什么？** | **引擎复用流程缺少TGT有效性检查机制，Hive/JDBC/HBase三类引擎均缺少TGT懒刷新能力，在Kerberos+无缓存场景下凭证是"一次性"的，无法应对长时间运行的引擎复用** |

### 2.3 根因确认

- **直接原因**: 引擎的UGI/loginUser创建或登录后永不重新刷新，TGT过期后Kerberos认证抛出异常
- **根本原因**: 引擎复用流程缺少TGT有效性检查机制，三类引擎均缺少TGT懒刷新能力
- **相关代码**:
  - `HiveEngineConnFactory.doCreateHiveSession` - Hive UGI创建点（第114行）
  - `HiveEngineConnExecutor.executeLine` - Hive UGI使用点（第222-252行）
  - `HiveEngineConcurrentConnExecutor.executeLine` - Hive UGI使用点（第183行）
  - `ConnectionManager.java` KERBEROS case - JDBC loginUser使用点
  - `HBaseConnectionManager.java` doKerberosLogin - HBase loginUser使用点
  - `KerberosTgtUtils.java` - TGT检查/刷新工具（已存在，本次扩展）
  - `HadoopConf.scala` - Hadoop公共配置（本次扩展）
  - `HDFSUtils.getUserGroupInformation` - UGI获取工具（已存在）

### 2.4 问题链路分析

**Hive 引擎（独立 UGI 对象）**:
```
引擎启动
  +-- HiveEngineConnFactory.doCreateHiveSession
      +-- HDFSUtils.getUserGroupInformation(Utils.getJvmUser)  <-- UGI创建（仅一次）
          +-- UserGroupInformation.loginUserFromKeytabAndReturnUGI  <-- Kerberos登录
              +-- UGI存入HiveSession.ugi  <-- 固定引用

引擎复用（TGT已过期）
  +-- HiveEngineConnExecutor.executeLine
      +-- ugi.doAs(...)  <-- 使用过期TGT
          +-- Kerberos认证失败  <-- 抛出异常
              +-- 任务执行失败
```

**JDBC/HBase 引擎（JVM 全局 loginUser）**:
```
引擎启动
  +-- ConnectionManager / HBaseConnectionManager
      +-- UserGroupInformation.loginUserFromKeytab  <-- 全局loginUser登录（仅一次）

引擎复用（TGT已过期）
  +-- getConnection / doKerberosLogin
      +-- UserGroupInformation.getLoginUser()  <-- 使用过期TGT的全局loginUser
          +-- Kerberos认证失败  <-- 抛出异常
              +-- 连接创建失败 -> 任务执行失败
```

---

## 3. 修复方案【核心】

### 3.1 修复架构

采用**公共开关 + 公共工具方法 + 三引擎分别调用**的架构：

- **公共开关**：在 `HadoopConf.scala` 中新增 `ENGINE_TGT_REFRESH_ENABLE`，配置 key 为 `linkis.engineconn.tgt.refresh.enable`（默认 false），Hive/JDBC/HBase 三个引擎共用
- **公共工具方法**：在 `KerberosTgtUtils.java` 中新增两个方法：
  - `refreshUgiIfNeeded(ugi, userName)` - 给 Hive 用（独立 UGI 对象）
  - `refreshLoginUserTgtIfNeeded()` - 给 JDBC/HBase 用（JVM 全局 loginUser）
- **三引擎分别调用**：Hive/JDBC/HBase 各自在执行/连接前调用对应方法
- **重试与报错机制**：用 AtomicInteger 计数器记录连续失败次数，前2次降级，第3次抛 RuntimeException

### 3.2 临时方案（Hot Fix）

- **方案描述**: 在引擎执行/连接前增加TGT有效性检查，过期则刷新凭证
- **实施步骤**:
  1. 在 `HadoopConf` 中添加公共功能开关 `linkis.engineconn.tgt.refresh.enable`（默认 `false`）
  2. 在 `KerberosTgtUtils` 中新增 `refreshUgiIfNeeded` 和 `refreshLoginUserTgtIfNeeded` 两个公共方法
  3. Hive 引擎在 `executeLine` 的 `ugi.doAs` 前，调用 `refreshUgiIfNeeded` 检查并刷新独立 UGI
  4. JDBC 引擎在 `ConnectionManager` 的 KERBEROS 分支，调用 `refreshLoginUserTgtIfNeeded` 检查并刷新全局 loginUser
  5. HBase 引擎在 `HBaseConnectionManager` 的 `getConnection` 中，调用 `refreshLoginUserTgtIfNeeded` 检查并刷新全局 loginUser
  6. 连续3次刷新失败时抛出 RuntimeException，避免无限静默降级
- **预计恢复时间**: 修复部署后立即生效
- **副作用**: 开关关闭时行为完全不变；开关打开时每次执行增加一次TGT检查（开销极小）

### 3.3 根本方案

#### 3.3.1 公共开关设计（HadoopConf.scala）

- **修复内容**: 在 `HadoopConf` 中新增 `ENGINE_TGT_REFRESH_ENABLE` 公共开关
- **涉及文件**:
  - [x] `HadoopConf.scala` - 新增公共开关
- **修复思路**:
  1. 使用 `CommonVars[java.lang.Boolean]` 声明，key 为 `linkis.engineconn.tgt.refresh.enable`
  2. 默认值 `false`，三引擎共用
  3. 移除各引擎独立开关（HiveEngineConfiguration、JDBCConfiguration、HBaseConfiguration 中的独立开关）

#### 3.3.2 公共工具方法设计（KerberosTgtUtils.java）

- **修复内容**: 新增两个公共方法 + AtomicInteger 重试计数器
- **涉及文件**:
  - [x] `KerberosTgtUtils.java` - 新增 `refreshUgiIfNeeded` 和 `refreshLoginUserTgtIfNeeded`
- **修复思路**:
  1. 新增 `MAX_REFRESH_FAILURES = 3` 常量
  2. 新增 `AtomicInteger ugiRefreshFailCount` 和 `loginUserRefreshFailCount` 计数器
  3. `refreshUgiIfNeeded(ugi, userName)`: 检查TGT -> 过期则 `HDFSUtils.getUserGroupInformation` 获取新UGI -> 成功重置计数器 -> 失败 incrementAndGet -> 达到3次抛 RuntimeException
  4. `refreshLoginUserTgtIfNeeded()`: 检查 loginUser TGT -> 过期则 `checkTGTAndReloginFromKeytab` -> 验证刷新结果 -> 失败计数 -> 达到3次抛 RuntimeException

#### 3.3.3 Hive 引擎修复

- **修复内容**: `executeLine` 方法中 `ugi.doAs` 前增加TGT懒刷新逻辑
- **涉及文件**:
  - [x] `HiveEngineConnExecutor.scala` - `executeLine`中增加TGT检查和UGI刷新 + `close()`兜底
  - [x] `HiveEngineConcurrentConnExecutor.scala` - 同上，含并发安全保护 + `close()`兜底
- **修复思路**:
  1. 调用公共开关 `HadoopConf.ENGINE_TGT_REFRESH_ENABLE`
  2. 若开关打开，调用 `KerberosTgtUtils.refreshUgiIfNeeded(ugi, Utils.getJvmUser)`
  3. 方法内部检查TGT，过期则 `HDFSUtils.getUserGroupInformation` 获取新UGI
  4. 使用新UGI执行 `doAs`
  5. `close()` 中 `super.close()` 用 `Utils.tryQuietly` 包裹

#### 3.3.4 JDBC 引擎修复

- **修复内容**: `ConnectionManager` 的 KERBEROS 分支增加TGT懒刷新逻辑
- **涉及文件**:
  - [x] `ConnectionManager.java` - KERBEROS case 中增加TGT检查
- **修复思路**:
  1. 在 `createKerberosSecureConfiguration` 之后调用 `KerberosTgtUtils.refreshLoginUserTgtIfNeeded()`
  2. 方法内部检查 loginUser TGT，过期则 `checkTGTAndReloginFromKeytab`
  3. 连续3次失败抛 RuntimeException

#### 3.3.5 HBase 引擎修复

- **修复内容**: `HBaseConnectionManager` 的 `getConnection` 中增加TGT懒刷新逻辑
- **涉及文件**:
  - [x] `HBaseConnectionManager.java` - `doKerberosLogin` 之后增加TGT检查
- **修复思路**:
  1. 在 `doKerberosLogin` 之后、`getLoginUser` 之前调用 `KerberosTgtUtils.refreshLoginUserTgtIfNeeded()`
  2. 仅在 `isKerberosAuthType(configuration)` 为 true 时执行
  3. 连续3次失败抛 RuntimeException

#### 3.3.6 重试与报错机制

- **设计**: 使用 AtomicInteger 计数器记录连续失败次数
- **行为**:
  - 第1次失败：降级执行，logger.warn 记录
  - 第2次失败：降级执行，logger.warn 记录
  - 第3次失败：抛出 RuntimeException，不再静默降级
- **计数器重置**: 刷新成功后立即重置计数器为0
- **两套独立计数器**: UGI刷新（Hive用）和 loginUser刷新（JDBC/HBase用）各自独立计数

#### 3.3.7 UGI刷新策略

采用**方案B - 重新调用`HDFSUtils.getUserGroupInformation`获取新UGI对象**（Hive引擎）：
- 不修改原UGI对象，避免并发修改风险
- 获取全新的UGI对象后替换引用
- `HiveEngineConnExecutor`：直接替换（单线程执行）
- `HiveEngineConcurrentConnExecutor`：`volatile` + `synchronized`保护替换

对于 JDBC/HBase 引擎，使用 `UserGroupInformation.checkTGTAndReloginFromKeytab()` 刷新 JVM 全局 loginUser。

---

## 4. 测试验证计划【核心】

### 4.1 单元测试

- [ ] 开关关闭时，三类引擎行为与修改前完全一致
- [ ] 开关打开 + TGT有效时，正常执行（Hive/JDBC/HBase）
- [ ] 开关打开 + TGT过期时，自动刷新凭证后正常执行（Hive/JDBC/HBase）
- [ ] 凭证刷新失败时，前2次降级不抛出异常（Hive/JDBC/HBase）
- [ ] 连续3次刷新失败时，抛出 RuntimeException（Hive/JDBC/HBase）
- [ ] 刷新成功后计数器重置为0
- [ ] `close()` 阶段TGT过期不抛出未捕获异常（Hive）

### 4.2 集成测试

- [ ] Kerberos + 无缓存场景：Hive引擎运行超过TGT有效期后复用成功
- [ ] Kerberos + 无缓存场景：JDBC引擎运行超过TGT有效期后复用成功
- [ ] Kerberos + 无缓存场景：HBase引擎运行超过TGT有效期后复用成功
- [ ] Kerberos + 有缓存场景：功能不受影响（三类引擎）
- [ ] 非Kerberos场景：功能不受影响（三类引擎）
- [ ] 并发模式（HiveEngineConcurrentConnExecutor）：多线程同时执行SQL时UGI刷新正确
- [ ] 连续3次失败报错场景验证（三类引擎）

### 4.3 回归测试

- [ ] 普通Hive/JDBC/HBase SQL执行正常
- [ ] 引擎首次启动执行正常
- [ ] 引擎复用执行正常（TGT未过期场景）
- [ ] Hive引擎并发模式开关正常
- [ ] JDBC引擎各种数据库连接正常
- [ ] HBase引擎连接正常

### 4.4 性能测试

- [ ] TGT检查开销：单次`KerberosTgtUtils.isTgtValid`耗时 < 1ms
- [ ] UGI刷新开销：单次`HDFSUtils.getUserGroupInformation`耗时 < 500ms
- [ ] loginUser刷新开销：单次`checkTGTAndReloginFromKeytab`耗时 < 500ms
- [ ] 开关关闭时无额外性能开销

---

## 5. 发布和回滚方案【重要】

### 5.1 发布策略

- **发布方式**: 灰度发布
- **灰度步骤**:
  1. 先在测试环境（BDAP-DEV）部署并验证
  2. 在SIT/UAT环境验证长时间运行场景
  3. 生产环境先灰度金监局环境，开关默认`false`
  4. 手动设置`linkis.engineconn.tgt.refresh.enable=true`开启修复
- **监控指标**:
  - Hive/JDBC/HBase引擎复用成功率
  - TGT刷新次数和成功率
  - TGT连续失败次数（达3次时报错）
  - SQL执行错误率
- **发布窗口**: 非业务高峰期

### 5.2 回滚方案

- **回滚条件**: 开启修复后出现SQL执行异常率上升或其他回归问题
- **回滚步骤**:
  1. 设置`linkis.engineconn.tgt.refresh.enable=false`
  2. 重启引擎
  3. 行为回退到修复前状态
- **回滚时间**: 配置修改 + 引擎重启，约5分钟
- **回滚预案**: 无需代码回滚，仅通过配置开关即可降级

---

## 6. 防范措施【重要】

### 6.1 测试改进

- [ ] 补充测试用例：Kerberos + 长时间运行 + 引擎复用场景（Hive/JDBC/HBase）
- [ ] 增加边界测试：TGT刚好过期时刻的引擎复用
- [ ] 增加并发测试：并发模式下UGI刷新的线程安全性
- [ ] 增加3次失败报错场景测试

### 6.2 监控改进

- [ ] 增加监控指标：三类引擎TGT过期次数
- [ ] 增加告警规则：TGT刷新失败告警、连续3次失败严重告警
- [ ] 增加日志输出：UGI/loginUser刷新关键步骤的info日志

### 6.3 流程改进

- [ ] 代码规范强化：Kerberos相关代码必须检查TGT有效性
- [ ] Code Review检查点：涉及UGI使用的代码必须考虑TGT过期场景
- [ ] 发布流程优化：Kerberos相关改动需在SIT环境验证超过24小时

---

## 7. 验收标准（三段式）

| 验证阶段 | 验收条件 |
|:--------:|---------|
| 【输入验证】 | AC1: 开关`linkis.engineconn.tgt.refresh.enable=false`时，Hive/JDBC/HBase三类引擎行为与修复前完全一致 |
| 【处理验证】 | AC2: 开关打开 + TGT有效时，三类引擎正常执行，不触发凭证刷新 |
| 【处理验证】 | AC3: 开关打开 + TGT过期时，Hive自动刷新UGI后SQL正常执行，日志记录刷新事件 |
| 【处理验证】 | AC4: 开关打开 + TGT过期时，JDBC自动刷新loginUser后连接正常，日志记录刷新事件 |
| 【处理验证】 | AC5: 开关打开 + TGT过期时，HBase自动刷新loginUser后连接正常，日志记录刷新事件 |
| 【处理验证】 | AC6: 并发模式下（HiveEngineConcurrentConnExecutor），多线程UGI刷新线程安全，无竞态条件 |
| 【处理验证】 | AC7: 连续3次刷新失败时，抛出RuntimeException，不再静默降级（三类引擎） |
| 【处理验证】 | AC8: 刷新成功后失败计数器重置为0 |
| 【输出验证】 | AC9: `close()`阶段即使TGT过期也不抛出未捕获异常，引擎正常关闭（Hive） |

---

## 8. 风险识别【参考】

### 8.1 修复风险

- **低风险**: 刷新逻辑用`Utils.tryCatch`包裹，前2次异常时降级，不影响任务执行
- **低风险**: 开关默认`false`，需要显式开启
- **中风险**: 第3次失败抛 RuntimeException 会导致任务失败，但这是预期行为（避免无限静默降级掩盖问题）

### 8.2 发布风险

- **中风险**: 需要在Kerberos环境验证，测试环境配置可能不完整
- **低风险**: 回滚方案简单（关闭开关即可）

### 8.3 业务风险

- **低风险**: 不修改任何公开接口签名，不影响上下游依赖
- **低风险**: 公共开关统一控制三类引擎，配置简洁

---

## 9. 关联影响分析【参考】

### 9.1 功能模块影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 在Hive/JDBC/HBase引擎插件内部增加TGT检查逻辑，在公共模块HadoopConf和KerberosTgtUtils中新增配置和方法，不影响其他模块 |

### 9.2 数据模型影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 不涉及数据库表结构变更 |

### 9.3 安全与权限影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 不涉及权限变更，凭证刷新使用与原创建相同的Kerberos凭证 |

### 9.4 用户体验与文案影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 无前端变更，用户无感知（仅日志中增加TGT刷新记录） |

### 9.5 上下游与三方依赖影响

| 影响等级 | 说明 |
|:--------:|------|
| 🟢 轻微影响 | 不影响上下游系统，不涉及第三方服务变更 |

---

## 10. 配置说明

### 10.1 新增配置项

| 配置Key | 默认值 | 说明 |
|---------|--------|------|
| `linkis.engineconn.tgt.refresh.enable` | `false` | 引擎TGT懒刷新公共开关（Hive/JDBC/HBase共用）。开启后在引擎执行/连接前检查TGT有效性，过期则刷新凭证 |

### 10.2 配置位置

- 模块配置类: `HadoopConf.scala`（`linkis-hadoop-common` 公共模块）
- 部署配置文件: 三个引擎的 `linkis-engineconn.properties` 均添加注释行

### 10.3 使用说明

```properties
# 引擎TGT懒刷新公共开关（默认关闭）
# 开启Kerberos认证且未开启HDFS缓存时建议开启
# 适用于Hive/JDBC/HBase三类引擎
# linkis.engineconn.tgt.refresh.enable=false
```

---

## 11. 关键代码位置参考

### 11.1 公共模块

| 文件 | 行号 | 说明 |
|------|------|------|
| `HadoopConf.scala` | 全文件 | 公共配置类，新增`ENGINE_TGT_REFRESH_ENABLE`公共开关 |
| `KerberosTgtUtils.java` | 第77-98行 | `isTgtValid`方法 - TGT检查工具（已存在） |
| `KerberosTgtUtils.java` | 新增 | `refreshUgiIfNeeded`方法 - UGI刷新（Hive用） |
| `KerberosTgtUtils.java` | 新增 | `refreshLoginUserTgtIfNeeded`方法 - loginUser刷新（JDBC/HBase用） |
| `KerberosTgtUtils.java` | 新增 | `MAX_REFRESH_FAILURES = 3` 常量 + AtomicInteger计数器 |
| `HDFSUtils.scala` | 第452-499行 | `getUserGroupInformation`方法 - UGI获取工具（已存在） |

### 11.2 Hive 引擎

| 文件 | 行号 | 说明 |
|------|------|------|
| `HiveEngineConnFactory.scala` | 第104-110行 | `doCreateHiveConcurrentSession` - UGI创建点（并发模式） |
| `HiveEngineConnFactory.scala` | 第112-118行 | `doCreateHiveSession` - UGI创建点（普通模式） |
| `HiveEngineConnExecutor.scala` | 第222-252行 | `executeLine`中`ugi.doAs` - UGI使用点 |
| `HiveEngineConcurrentConnExecutor.scala` | 第183行 | `executeLine`中`ugi.doAs` - UGI使用点（并发模式） |
| `HiveEngineConnExecutor.scala` | 第509-513行 | `close()`方法 - 需要兜底处理 |
| `HiveEngineConcurrentConnExecutor.scala` | 第445-461行 | `close()`方法 - 需要兜底处理 |
| `HiveEngineConfiguration.scala` | 全文件 | 配置类（独立开关已移除） |

### 11.3 JDBC 引擎

| 文件 | 行号 | 说明 |
|------|------|------|
| `ConnectionManager.java` | KERBEROS case | `createKerberosSecureConfiguration` - loginUser使用点 |
| `JDBCConfiguration.scala` | 全文件 | 配置类（独立开关已移除） |

### 11.4 HBase 引擎

| 文件 | 行号 | 说明 |
|------|------|------|
| `HBaseConnectionManager.java` | `doKerberosLogin` / `getConnection` | loginUser使用点 |
| `HBaseConfiguration.scala` | 全文件 | 配置类（独立开关已移除） |

---

## 附录

### A.1 修复涉及文件清单

| 序号 | 文件路径 | 修改类型 | 说明 |
|:----:|---------|:--------:|------|
| 1 | `linkis-hadoop-common/.../conf/HadoopConf.scala` | 修改 | 新增`ENGINE_TGT_REFRESH_ENABLE`公共配置开关 |
| 2 | `linkis-hadoop-common/.../utils/KerberosTgtUtils.java` | 修改 | 新增`refreshUgiIfNeeded`和`refreshLoginUserTgtIfNeeded`方法 + AtomicInteger重试计数器 |
| 3 | `linkis-engineconn-plugins/hive/.../executor/HiveEngineConnExecutor.scala` | 修改 | executeLine增加TGT检查（调用公共方法） + close兜底 |
| 4 | `linkis-engineconn-plugins/hive/.../executor/HiveEngineConcurrentConnExecutor.scala` | 修改 | executeLine增加TGT检查(含同步锁) + close兜底 |
| 5 | `linkis-engineconn-plugins/hive/.../conf/HiveEngineConfiguration.scala` | 修改 | 移除独立开关`TGT_REFRESH_ENABLE`（改用公共开关） |
| 6 | `linkis-engineconn-plugins/jdbc/.../ConnectionManager.java` | 修改 | KERBEROS分支增加TGT检查（调用公共方法） |
| 7 | `linkis-engineconn-plugins/jdbc/.../conf/JDBCConfiguration.scala` | 修改 | 移除独立开关`JDBC_TGT_REFRESH_ENABLE`（改用公共开关） |
| 8 | `linkis-engineconn-plugins/hbase/.../HBaseConnectionManager.java` | 修改 | getConnection增加TGT检查（调用公共方法） |
| 9 | `linkis-engineconn-plugins/hbase/.../conf/HBaseConfiguration.scala` | 修改 | 移除独立开关`HBASE_TGT_REFRESH_ENABLE`（改用公共开关） |
| 10 | 三个引擎的 `linkis-engineconn.properties` | 修改 | 添加公共开关注释行 |

### A.2 已有可复用工具

| 工具 | 位置 | 用途 |
|------|------|------|
| `KerberosTgtUtils.isTgtValid(ugi)` | `linkis-hadoop-common` | 检查UGI的TGT是否有效 |
| `HDFSUtils.getUserGroupInformation(userName, label)` | `linkis-hadoop-common` | 重新获取UGI（含Kerberos登录） |
| `UserGroupInformation.checkTGTAndReloginFromKeytab()` | Hadoop原生 | 刷新JVM全局loginUser的TGT |
| `Utils.tryCatch` / `Utils.tryQuietly` | `linkis-common` | 异常保护 |
| `CommonVars` | `linkis-common` | 配置开关声明 |

### A.3 编译验证结果

| 模块 | 编译结果 |
|------|---------|
| linkis-hadoop-common | BUILD SUCCESS |
| linkis-engineplugin-hive | BUILD SUCCESS |
| linkis-engineplugin-jdbc | BUILD SUCCESS |
| linkis-engineconn-plugin-hbase | BUILD SUCCESS |
