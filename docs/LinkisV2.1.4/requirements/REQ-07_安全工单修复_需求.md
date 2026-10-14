> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/1d3aVl1GBohBYpqg)

# 安全工单修复 需求文档

**需求类型**: FIX（安全工单/依赖升级修复）
**风险等级**: 中风险（涉及 hive 引擎版本默认值变更 + 依赖版本升级）
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 后端（不涉及前端）
**关联 commit**: `9aedec081`

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | 安全工单修复：① hive 引擎默认版本 1.2.1→2.3.3；② commons-beanutils 1.9.4→1.11.0（CVE 修复）；③ JUnit 4.12→4.13.2（测试依赖升级） |
| **改动服务** | linkis-computation-governance-common（hive 版本）+ linkis-manager-persistence + linkis-ps-common-lock（beanutils）+ root pom + linkis-dist（DML/NOTICE）+ linkis-label-common 测试 |
| **改动范围** | 7 文件，10 行变更 |
| **开关方式** | hive 版本走 `wds.linkis.hive.engine.version` 配置（默认值变更），运行时可覆盖回 1.2.1 |

---

## 1. 需求背景

### 1.1 业务背景

安全工单扫描发现以下问题需修复：

1. **hive 引擎版本过低**：原默认 `wds.linkis.hive.engine.version=1.2.1`，生产环境已升级到 hive 2.3.3，默认值与生产环境不一致，新建 hive 引擎实例可能拉起错误版本；
2. **commons-beanutils 1.9.4 存在 CVE**：`commons-beanutils` 1.9.4 版本存在已知安全漏洞（CVE-2014-0114 类相关问题 + 反序列化风险），需升级到修复版 1.11.0；
3. **JUnit 4.12 版本过低**：测试依赖 JUnit 4.12 存在多个已修复 bug，安全工单要求升级到 4.13.2。

### 1.2 目标

1. hive 引擎默认版本对齐生产环境（2.3.3）；
2. commons-beanutils 升级到 1.11.0 消除 CVE；
3. JUnit 升级到 4.13.2；
4. DML 初始化脚本注释对齐 hive-2.3.3；
5. NOTICE 文件同步更新 JUnit 版本声明。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**安全管理员**，我希望 commons-beanutils 升级到修复 CVE 的版本，消除安全扫描告警 |
| US-2 | 作为**平台运维**，希望 hive 引擎默认版本与生产环境一致（2.3.3），避免新建引擎拉起错误版本 |
| US-3 | 作为**开发**，希望 JUnit 升级到 4.13.2，避免测试框架已知 bug |

---

## 3. 功能描述

### 3.1 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | hive 引擎版本默认值升级 | `GovernanceCommonConf.HIVE_ENGINE_VERSION` 默认值 `1.2.1` → `2.3.3` |
| F-2 | beanutils 版本升级 | `linkis-manager-persistence/pom.xml` 和 `linkis-ps-common-lock/pom.xml` 中 `commons-beanutils` 版本 `1.9.4` → `1.11.0` |
| F-3 | JUnit 版本升级 | root `pom.xml` 新增 `<junit.version>4.13.2</junit.version>` 属性 |
| F-4 | DML 脚本注释对齐 | `linkis_dml.sql` 中 `hive-1.2.1`/`hive1.2.1` 注释改为 `hive-2.3.3`/`hive2.3.3` |
| F-5 | NOTICE 同步 | `linkis-dist/release-docs/NOTICE` 中 3 处 `JUnit (4.12)` 改为 `JUnit (4.13.2)` |
| F-6 | 测试用例适配 | `TestLabelBuilder.java` 中 `engineType` label 从 `hive-1.2.1` 改为 `hive-2.3.3` |

### 3.2 关键设计决策

| 编号 | 问题 | 决策 |
|:----:|------|------|
| Q1 | beanutils 1.11.0 是否兼容 1.9.4？ | 1.11.0 是 1.9.x 的 minor 升级，API 向后兼容，主要修复反序列化漏洞；但需验证依赖冲突（1.11.0 的传递依赖可能与现有 jackson/guava 冲突） |
| Q2 | hive 版本默认值变更是否影响已部署环境？ | 已部署环境若显式配置 `wds.linkis.hive.engine.version`，配置优先；未显式配置的环境升级后默认值变为 2.3.3，新建 hive 引擎会拉起 2.3.3——若生产 hive 环境仍为 1.2.1 会导致引擎启动失败，需确认生产环境 hive 版本 |
| Q3 | JUnit 4.13.2 是否兼容 4.12？ | 4.13.x 是 4.12 的 patch 升级，API 完全兼容，仅 bug 修复 |

---

## 4. 验收标准

### AC1: hive 引擎版本默认值为 2.3.3

**操作步骤**:
1. 不配置 `wds.linkis.hive.engine.version`
2. 读取 `GovernanceCommonConf.HIVE_ENGINE_VERSION.getValue`

**预期结果**:
- 返回 `2.3.3`

### AC2: hive 引擎任务正常执行

**前置条件**: 生产环境 hive 版本为 2.3.3

**操作步骤**:
1. 提交 hive SQL 任务
2. 观察引擎拉起与执行

**预期结果**:
- hive 引擎以 2.3.3 版本拉起
- 任务正常执行

### AC3: beanutils 依赖升级无冲突

**操作步骤**:
1. `mvn dependency:tree` 检查 commons-beanutils 版本
2. 全量编译

**预期结果**:
- commons-beanutils 版本为 1.11.0
- 无依赖冲突
- 编译通过

### AC4: JUnit 单测全量通过

**操作步骤**:
1. `mvn test` 全量执行单测

**预期结果**:
- JUnit 4.13.2
- 单测全部通过（⚠️ 注意：`GovernanceCommonConfTest.scala:46` 仍断言 "1.2.1"，升级后此测试会失败，需同步修改为 "2.3.3"——见 §6 风险）

### AC5: DML 脚本注释对齐

**操作步骤**:
1. 检查 `linkis_dml.sql` 中 hive 相关注释

**预期结果**:
- 注释为 `hive-2.3.3` / `hive2.3.3`

### AC6: NOTICE 声明对齐

**操作步骤**:
1. 检查 `NOTICE` 文件 JUnit 声明

**预期结果**:
- 3 处均为 `JUnit (4.13.2)`

---

## 5. 配置说明

| Key | 默认值（改造后） | 说明 |
|-----|------------------|------|
| `wds.linkis.hive.engine.version` | `2.3.3`（原 `1.2.1`） | hive 引擎版本，运行时可覆盖 |

配置位置: `GovernanceCommonConf.scala`

> ⚠️ 若生产环境 hive 仍为 1.2.1，需显式配置 `wds.linkis.hive.engine.version=1.2.1` 回退。

---

## 6. 风险识别

| 风险 | 等级 | 说明与缓解 |
|------|:----:|-----------|
| hive 版本默认值变更导致引擎启动失败 | 🔴 | 若生产环境 hive 仍为 1.2.1，升级后新建 hive 引擎会拉起 2.3.3 失败。**上线前必须确认生产环境 hive 版本**；若仍为 1.2.1，需在 `linkis.properties` 显式配置 `wds.linkis.hive.engine.version=1.2.1` |
| `GovernanceCommonConfTest.scala` 单测失败 | 🔴 | 测试第 46 行 `Assertions.assertEquals("1.2.1", hiveengineversion)` 未同步修改，升级后会失败。**需同步改为 `"2.3.3"`**（本 commit 未修改此文件，属遗漏） |
| beanutils 1.11.0 依赖冲突 | 🟡 | 1.11.0 的传递依赖（如 commons-collections）可能与现有依赖冲突，需 `mvn dependency:tree` 验证 |
| JUnit 4.13.2 兼容性 | 🟢 | patch 升级，API 兼容，风险极低 |

---

## 7. 关联影响分析

### 7.1 影响清单

| 维度 | 影响 |
|------|------|
| hive 引擎 | 🔴 默认版本变更，影响新建 hive 引擎实例 |
| 依赖树 | 🟡 beanutils 升级，linkis-manager-persistence + linkis-ps-common-lock 模块 |
| 测试 | 🔴 GovernanceCommonConfTest 需同步修改（遗漏） |
| DML | 🟢 注释对齐，无逻辑影响 |
| NOTICE | 🟢 声明对齐，无逻辑影响 |
| 兼容性 | 🟡 hive 版本可配置覆盖，beanutils/junit 向后兼容 |

### 7.2 高危区域对照（CLAUDE.md §10.2）

不触及 §10.2 主流程 10 步中的任何环节（hive 版本仅影响引擎版本选择，不影响主链路逻辑）。

---

## 8. 发布和回滚方案

### 8.1 发布策略

1. 上线前确认生产环境 hive 版本；
2. 若 hive=2.3.3，直接发布；
3. 若 hive=1.2.1，发布前在 `linkis.properties` 显式配置 `wds.linkis.hive.engine.version=1.2.1`；
4. 同步修改 `GovernanceCommonConfTest.scala:46` 为 `"2.3.3"`。

### 8.2 回滚方案

- hive 版本：配置 `wds.linkis.hive.engine.version=1.2.1` 覆盖默认值，无需回滚代码；
- beanutils/junit：需回滚 pom.xml（依赖版本）。

---

## 附录

### A.1 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `GovernaceCommonConf.scala` | 修改 | HIVE_ENGINE_VERSION 默认值 1.2.1→2.3.3 |
| `linkis-manager-persistence/pom.xml` | 修改 | commons-beanutils 1.9.4→1.11.0 |
| `linkis-ps-common-lock/pom.xml` | 修改 | commons-beanutils 1.9.4→1.11.0 |
| `pom.xml` | 修改 | 新增 junit.version=4.13.2 |
| `linkis_dml.sql` | 修改 | 注释 hive-1.2.1→hive-2.3.3 |
| `NOTICE` | 修改 | JUnit 4.12→4.13.2（3 处） |
| `TestLabelBuilder.java` | 修改 | engineType label hive-1.2.1→hive-2.3.3 |

### A.2 遗漏文件（需同步修改）

| 文件 | 问题 | 建议修改 |
|------|------|---------|
| `GovernanceCommonConfTest.scala:46` | 仍断言 `"1.2.1"`，升级后会失败 | 改为 `Assertions.assertEquals("2.3.3", hiveengineversion)` |

### A.3 关联文档

- 设计文档: [REQ-07_安全工单修复_设计.md](../design/REQ-07_安全工单修复_设计.md)
- CLAUDE.md §3.1（依赖版本约束：不要随意升级依赖版本）
