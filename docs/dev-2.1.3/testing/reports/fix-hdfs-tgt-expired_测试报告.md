# 引擎复用TGT过期修复 测试报告

**需求类型**: FIX（Bug修复）
**问题等级**: P0-紧急
**文档版本**: v1.0
**创建日期**: 2026-09-02
**功能属性**: 后端
**DPMS Story ID**: 544181

---

## 1 测试概述

### 1.1 测试目标

验证修复方案是否正确解决 Hive/JDBC/HBase 三类引擎在 Kerberos + keytab + 关闭 HDFS 缓存场景下，引擎长时间运行后复用报 TGT 过期错误的问题。

### 1.2 测试范围

| 维度 | 内容 |
|------|------|
| **引擎覆盖** | Hive（普通+并发）、JDBC、HBase |
| **功能点** | 公共开关、TGT懒刷新、3次失败报错、并发安全、close兜底、回归兼容 |
| **测试类型** | 编译验证 + 单元测试 + 集成测试（需Kerberos环境）+ 回归测试 |
| **用例总数** | 33个（3个@skip复现用例 + 30个修复/回归用例） |

### 1.3 测试结论

| 结论项 | 状态 |
|--------|------|
| **编译验证** | PASS（4个模块 BUILD SUCCESS） |
| **现有单元测试** | PASS（5个测试，0失败） |
| **代码审查** | PASS（开关默认false + tryCatch兜底 + 3次报错机制） |
| **集成测试** | 待执行（需Kerberos环境，建议在SIT/UAT环境验证） |
| **总体结论** | 代码改动已通过编译验证和现有单元测试，可进入SIT环境集成测试 |

---

## 2 编译验证

### 2.1 编译环境

| 项 | 值          |
|----|------------|
| JDK | 1.8        |
| Maven | 3.8.2      |
| Scala | 2.12       |
| Hadoop Profile | hadoop-3.3 |
| 分支 | dev-2.1.3  |

### 2.2 编译结果

| 模块 | 模块路径 | 编译结果 | 说明 |
|------|----------|----------|------|
| linkis-hadoop-common | linkis-commons/linkis-hadoop-common | BUILD SUCCESS | 公共开关 + KerberosTgtUtils 新方法 |
| linkis-engineplugin-hive | linkis-engineconn-plugins/hive | BUILD SUCCESS | Hive引擎TGT刷新 + close兜底 |
| linkis-engineplugin-jdbc | linkis-engineconn-plugins/jdbc | BUILD SUCCESS | JDBC引擎TGT刷新 |
| linkis-engineconn-plugin-hbase | linkis-engineconn-plugins/hbase/hbase-core | BUILD SUCCESS | HBase引擎TGT刷新 |

### 2.3 编译顺序说明

由于 Hive/JDBC/HBase 引擎依赖 `linkis-hadoop-common` 中的 `KerberosTgtUtils` 和 `HadoopConf`，编译顺序为：
1. 先 `mvn install` linkis-hadoop-common（将新方法装入本地Maven仓库）
2. 再编译三个引擎模块

---

## 3 单元测试执行

### 3.1 现有单元测试执行结果

| 测试类 | 模块 | 测试数 | 通过 | 失败 | 跳过 | 结果 |
|--------|------|--------|------|------|------|------|
| ConnectionManagerTest | jdbc | 2 | 2 | 0 | 0 | PASS |
| JdbcParamUtilsTest | jdbc | 2 | 2 | 0 | 0 | PASS |
| ProgressMonitorTest | jdbc | 1 | 1 | 0 | 0 | PASS |
| **合计** | - | **5** | **5** | **0** | **0** | **PASS** |

### 3.2 单元测试覆盖说明

现有单元测试验证了 JDBC 引擎的 `ConnectionManager`、`JdbcParamUtils`、`ProgressMonitor` 在**非Kerberos场景**下的正常行为，确保代码改动**未破坏现有功能**。

`KerberosTgtUtils` 的专项单元测试（TC031-TC033）需要 Mock `UserGroupInformation` 和 `HDFSUtils`，属于新增测试，建议在后续迭代中补充。

---

## 4 代码审查验证

### 4.1 开关规范验证

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 开关已声明 | PASS | `HadoopConf.ENGINE_TGT_REFRESH_ENABLE` |
| 开关命名规范 | PASS | `linkis.engineconn.tgt.refresh.enable`（符合 `linkis.[模块].[功能].[属性]` 规范） |
| 默认值 false | PASS | `CommonVars[java.lang.Boolean](..., false)` |
| 公共位置 | PASS | 放在 `HadoopConf`（Hive/JDBC/HBase共用） |
| properties文件声明 | PASS | 三个引擎的 linkis-engineconn.properties 均已添加注释 |

### 4.2 异常降级验证

| 检查项 | 结果 | 说明 |
|--------|------|------|
| Hive executeLine 刷新逻辑 | PASS | 开关关闭时不触发；3次失败抛RuntimeException |
| Hive close() 兜底 | PASS | `Utils.tryQuietly { super.close() }` |
| JDBC getConnection 刷新逻辑 | PASS | 开关关闭时不触发；Kerberos分支才触发 |
| HBase getConnection 刷新逻辑 | PASS | 开关关闭时不触发；`isKerberosAuthType` 才触发 |
| 前2次失败降级 | PASS | 返回原UGI，warn日志，计数器+1 |
| 3次失败报错 | PASS | 抛RuntimeException，信息含"3 times"和"keytab" |

### 4.3 并发安全验证

| 检查项 | 结果 | 说明 |
|--------|------|------|
| ugi 字段可见性 | PASS | `@volatile private var ugi` |
| synchronized 保护 | PASS | synchronized 块包裹刷新逻辑 |
| double-check | PASS | synchronized 外层先检查 `isTgtValid`，内层通过 `refreshUgiIfNeeded` 再次检查 |
| 计数器线程安全 | PASS | `AtomicInteger` 保证原子操作 |

### 4.4 代码规范验证

| 检查项 | 结果 | 说明 |
|--------|------|------|
| License 头 | PASS | 修改的文件均已有License头 |
| 日志规范 | PASS | 使用 `extends Logging` 的 logger |
| 最小改动原则 | PASS | 未修改公共接口签名，仅新增方法和配置 |
| 无新依赖引入 | PASS | 使用已有的 `HDFSUtils`、`UserGroupInformation` |

---

## 5 功能测试用例覆盖

### 5.1 Feature 场景覆盖

| Feature文件 | Scenario总数 | 已覆盖 | 覆盖率 |
|-------------|-------------|--------|--------|
| fix-hdfs-tgt-expired.feature | 17 | 17 | 100% |

### 5.2 验收标准覆盖

| 验收标准 | 测试用例 | 状态 |
|----------|---------|------|
| AC1: 开关关闭时行为与修复前一致 | TC004, TC013, TC016 | 已覆盖 |
| AC2: 开关打开+TGT有效时正常执行 | TC005 | 已覆盖 |
| AC3: Hive TGT过期自动刷新UGI | TC006 | 已覆盖 |
| AC4: JDBC TGT过期自动刷新loginUser | TC010 | 已覆盖 |
| AC5: HBase TGT过期自动刷新loginUser | TC014 | 已覆盖 |
| AC6: 并发模式线程安全 | TC018 | 已覆盖 |
| AC7: 3次刷新失败抛RuntimeException | TC008, TC011, TC015 | 已覆盖 |
| AC8: 刷新成功后计数器重置 | TC009 | 已覆盖 |
| AC9: close()阶段TGT过期不抛异常 | TC020, TC021 | 已覆盖 |

**验收标准覆盖率: 9/9 (100%)**

### 5.3 改动文件验证矩阵

| 改动文件 | 编译通过 | 单元测试 | 代码审查 | 集成测试 |
|----------|----------|----------|----------|----------|
| HadoopConf.scala | PASS | - | PASS | 待执行 |
| KerberosTgtUtils.java | PASS | - | PASS | 待执行 |
| HiveEngineConnExecutor.scala | PASS | - | PASS | 待执行 |
| HiveEngineConcurrentConnExecutor.scala | PASS | - | PASS | 待执行 |
| HiveEngineConfiguration.scala | PASS | - | PASS | - |
| ConnectionManager.java | PASS | PASS | PASS | 待执行 |
| JDBCConfiguration.scala | PASS | - | PASS | - |
| HBaseConnectionManager.java | PASS | - | PASS | 待执行 |
| HBaseConfiguration.scala | PASS | - | PASS | - |
| 3个 linkis-engineconn.properties | - | - | PASS | 待执行 |

---

## 6 集成测试计划

### 6.1 测试环境要求

集成测试需在 Kerberos 环境下执行，本地开发环境无法模拟。建议在以下环境执行：

| 环境 | 用途 | 说明 |
|------|------|------|
| GZ-BDAP-DEV | 开发环境验证 | 3个bdap测试环境之一 |
| GZ-BDAP-SIT | SIT环境验证 | 正式SIT测试 |
| GZ-BDAP-UAT | UAT环境验证 | 上线前UAT验证 |

### 6.2 集成测试关键场景

以下为必须在Kerberos环境执行的核心集成测试场景：

| 优先级 | 场景 | 对应用例 | 验证目标 |
|--------|------|---------|----------|
| P0 | Hive TGT过期自动刷新 | TC006 | 引擎运行>10h后复用不再报TGT错误 |
| P0 | JDBC TGT过期自动刷新 | TC010 | JDBC引擎复用正常 |
| P0 | HBase TGT过期自动刷新 | TC014 | HBase引擎复用正常 |
| P0 | 3次刷新失败报错 | TC008, TC011, TC015 | keytab文件删除后第3次报错 |
| P0 | 开关关闭兼容性 | TC004 | 开关关闭时行为与修复前一致 |
| P0 | 并发模式安全 | TC018 | 并发Hive引擎复用正常 |
| P0 | 非Kerberos回归 | TC022 | 非Kerberos场景不受影响 |
| P1 | close()兜底 | TC020, TC021 | 引擎关闭不因TGT过期报错 |
| P1 | 性能验证 | TC026 | TGT检查耗时<1ms |

### 6.3 集成测试配置

```properties
# linkis.properties
wds.linkis.keytab.enable=true
wds.linkis.hadoop.hdfs.cache.enable=false
wds.linkis.fs.hdfs.impl.disable.cache=false

# 引擎 linkis-engineconn.properties
linkis.engineconn.tgt.refresh.enable=true
```

---

## 7 风险评估

### 7.1 已识别风险

| 风险项 | 风险等级 | 缓解措施 | 残留风险 |
|--------|----------|----------|----------|
| Kerberos环境差异 | 中 | SIT/UAT环境充分验证 | 不同KDC版本行为差异 |
| keytab文件刷新频率 | 低 | 外部1h刷新 + 引擎懒刷新双保险 | 极端场景：keytab文件被删 |
| 并发竞态 | 低 | @volatile + synchronized + double-check | 理论安全，需并发测试确认 |
| HDFS缓存场景影响 | 低 | 开关默认false，HDFS缓存场景不受影响 | 无 |

### 7.2 回退方案

如生产环境出现问题，可通过以下方式即时回退：

```properties
# 关闭TGT刷新功能，回退到修复前行为
linkis.engineconn.tgt.refresh.enable=false
```

开关关闭后：
- Hive 引擎使用原始 UGI 执行 doAs（不刷新）
- JDBC/HBase 引擎使用 JVM 全局 loginUser（不刷新）
- 行为与修复前完全一致

---

## 8 测试总结

### 8.1 已完成测试

| 测试类型 | 结果 | 说明 |
|----------|------|------|
| 编译验证 | PASS | 4个模块全部 BUILD SUCCESS |
| 现有单元测试 | PASS | 5个测试全部通过，无回归 |
| 代码审查 | PASS | 开关规范、异常降级、并发安全、代码规范均符合 |
| 测试用例生成 | PASS | 33个用例，覆盖17个Feature场景和9个验收标准 |

### 8.2 待执行测试

| 测试类型 | 说明 | 建议执行环境 |
|----------|------|--------------|
| 集成测试-核心修复验证 | TC006, TC010, TC014 | GZ-BDAP-SIT |
| 集成测试-3次失败报错 | TC008, TC011, TC015 | GZ-BDAP-SIT |
| 集成测试-并发安全 | TC018 | GZ-BDAP-SIT |
| 集成测试-回归验证 | TC022-TC030 | GZ-BDAP-SIT |
| 性能测试 | TC026 | GZ-BDAP-SIT |
| UAT验证 | 全量回归 | GZ-BDAP-UAT |

### 8.3 发布建议

**结论**: 代码改动已通过编译验证和现有单元测试，代码审查确认符合项目规范（开关默认false、异常降级、并发安全、最小改动），可进入SIT环境集成测试。

**前置条件**:
1. 在SIT环境配置 Kerberos + keytab + 关闭HDFS缓存
2. 开启 `linkis.engineconn.tgt.refresh.enable=true`
3. 执行核心集成测试场景（TC006/TC010/TC014/TC008/TC018/TC022）

**上线建议**:
- SIT + UAT 双环境验证通过后可灰度上线
- 生产环境建议先在 bdap 灰度环境开启开关，观察1-2天后再全量开启
- 保留 `linkis.engineconn.tgt.refresh.enable=false` 作为即时回退手段
