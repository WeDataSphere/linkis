# 安全工单修复 设计文档

**需求类型**: FIX（安全工单/依赖升级修复）
**风险等级**: 中风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端
**关联需求**: [REQ-07_安全工单修复_需求.md](../requirements/REQ-07_安全工单修复_需求.md)
**关联 commit**: `9aedec081`

---

## 0. 执行摘要

| 维度 | 内容 |
|------|------|
| **改造点** | 3 项依赖升级：hive 引擎版本默认值（1.2.1→2.3.3）、commons-beanutils（1.9.4→1.11.0）、JUnit（4.12→4.13.2） |
| **机制** | ① hive 版本走 `CommonVars` 默认值变更，运行时可配置覆盖；② beanutils/JUnit 走 pom.xml 版本号变更 |
| **关键风险** | 🔴 hive 版本默认值变更可能导致生产 hive 1.2.1 环境引擎启动失败；🔴 `GovernanceCommonConfTest` 未同步修改会失败 |
| **回退** | hive 版本可配置覆盖回 1.2.1；beanutils/JUnit 需回滚 pom.xml |

---

## 1. 设计背景

安全工单要求修复以下依赖问题，详见需求文档 §1。

---

## 2. 设计方案

### 2.1 hive 引擎版本默认值升级

```scala
// GovernanceCommonConf.scala
val HIVE_ENGINE_VERSION = CommonVars("wds.linkis.hive.engine.version", "2.3.3")  // 原 "1.2.1"
```

**影响链路**：
```
HIVE_ENGINE_VERSION
  ├─ EngineTypeLabelCreator.java:55  → 创建 hive engineType label 时使用
  ├─ LabelParameterParser.scala      → 参数解析
  ├─ linkis.properties               → 部署配置（生产可覆盖）
  └─ linkis_dml.sql                  → DML 初始化脚本注释（对齐）
```

**配置优先级**：
1. `linkis.properties` 显式配置 `wds.linkis.hive.engine.version` > 默认值；
2. 未显式配置时，升级后默认值为 2.3.3。

### 2.2 commons-beanutils 版本升级

```xml
<!-- linkis-manager-persistence/pom.xml + linkis-ps-common-lock/pom.xml -->
<dependency>
  <groupId>commons-beanutils</groupId>
  <artifactId>commons-beanutils</artifactId>
  <version>1.11.0</version>  <!-- 原 1.9.4 -->
</dependency>
```

**1.9.4 → 1.11.0 变更要点**：
- 修复反序列化漏洞（CVE-2014-0114 类相关）；
- API 向后兼容（minor 升级）；
- 传递依赖变更：commons-collections 3.x → 4.x（需验证依赖冲突）。

**影响模块**：
| 模块 | 使用场景 |
|------|---------|
| linkis-manager-persistence | Bean 属性操作 |
| linkis-ps-common-lock | 分布式锁相关 Bean 操作 |

### 2.3 JUnit 版本升级

```xml
<!-- root pom.xml -->
<properties>
  <junit5.version>5.7.2</junit5.version>
  <junit.version>4.13.2</junit.version>  <!-- 新增 -->
  ...
</properties>
```

JUnit 4.12 → 4.13.2 为 patch 升级，API 完全兼容，仅 bug 修复。NOTICE 文件同步更新 3 处声明。

### 2.4 测试用例适配

```java
// TestLabelBuilder.java
Label<?> engineType = labelBuilderFactory.createLabel("engineType", "hive-2.3.3");  // 原 "hive-1.2.1"
```

---

## 3. 关键代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `GovernaceCommonConf.scala` | `:30` | HIVE_ENGINE_VERSION 默认值 |
| `linkis-manager-persistence/pom.xml` | `:56-59` | commons-beanutils 版本 |
| `linkis-ps-common-lock/pom.xml` | `:37-40` | commons-beanutils 版本 |
| `pom.xml` | `:174` | junit.version 属性 |
| `linkis_dml.sql` | `:203, :326` | hive 注释 |
| `NOTICE` | `:1506, :1761, :1767` | JUnit 声明 |
| `TestLabelBuilder.java` | `:30` | engineType label |

---

## 4. 遗漏与风险

### 4.1 GovernanceCommonConfTest 遗漏（🔴 必须同步修改）

`GovernanceCommonConfTest.scala:46` 仍断言：
```scala
Assertions.assertEquals("1.2.1", hiveengineversion)
```

升级默认值后此断言会失败。**建议同步修改为**：
```scala
Assertions.assertEquals("2.3.3", hiveengineversion)
```

本 commit 未修改此文件，属遗漏。

### 4.2 生产环境 hive 版本一致性

| 生产 hive 版本 | 升级后行为 | 处理 |
|:--------------:|-----------|------|
| 2.3.3 | ✅ 默认值匹配，正常拉起 | 无需处理 |
| 1.2.1 | 🔴 默认值不匹配，引擎启动失败 | 需显式配置 `wds.linkis.hive.engine.version=1.2.1` |

---

## 5. 兼容性与影响

| 维度 | 影响 |
|------|------|
| API | 🟢 无签名变更 |
| 依赖 | 🟡 beanutils 1.11.0 传递依赖可能冲突 |
| 测试 | 🔴 GovernanceCommonConfTest 需同步 |
| 部署 | 🟡 需确认生产 hive 版本 |
| 兼容性 | 🟡 hive 版本可覆盖，beanutils/junit 向后兼容 |

---

## 6. 修改文件清单

详见需求文档附录 A.1（7 文件修改 + 1 遗漏文件需补改）。

---

## 7. 测试要点

1. **AC1** 默认值返回 2.3.3；
2. **AC2** hive 引擎任务正常（需生产 hive=2.3.3）；
3. **AC3** beanutils 依赖无冲突（mvn dependency:tree）；
4. **AC4** JUnit 单测全量通过（⚠️ 需先修复 GovernanceCommonConfTest）；
5. **AC5** DML 注释对齐；
6. **AC6** NOTICE 声明对齐。
