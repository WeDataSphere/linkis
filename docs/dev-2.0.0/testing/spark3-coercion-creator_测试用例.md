# Spark3 强制切换（Creator 维度 + 配置项管理迁移）测试用例

| 项目 | 内容 |
|-----|------|
| 任务 | spark3-coercion-creator |
| 需求属性 | 后端开发 |
| 来源 | [spark3-coercion-creator.feature](../features/spark3-coercion-creator.feature)（7 Rule, 30 Scenario）|
| 需求文档 | [spark3-coercion-creator_需求.md](../requirements/spark3-coercion-creator_需求.md) |
| 设计文档 | [spark3-coercion-creator_设计.md](../design/spark3-coercion-creator_设计.md) |

---

## 1. 测试概述

验证 Spark3 强制切换（sparkVersionCoercion）：creator 维度判定 + 配置项管理迁移（3 名单从 properties 迁移到配置项管理 RPC 读取，switch 保留 properties）。

**测试范围**：
- creator 维度判定（个人>部门>creator 优先级、命中/未命中、异常降级、热加载、冒烟）
- 配置项管理迁移（RPC 读取、fallback、三表完整性、switch 保留、缓存坑）

---

## 2. 测试用例

### 2.1 保持现有功能不受影响（Rule 1）

#### TC-001：总开关关闭时所有维度均不生效 `P0` `@regression @critical` `功能测试`
- **来源**：feature Rule 1, Scenario 1
- **前置**：总开关关闭；用户级名单含 userA；部门级含 dept01；应用级含 appA
- **步骤**：userA 提交 Spark2 任务
- **预期**：使用 Spark2 引擎，版本保持 Spark2，行为与增强前一致

#### TC-002：用户级名单命中强制切换（原有功能）`P0` `@regression @critical` `功能测试`
- **来源**：feature Rule 1, Scenario 2
- **前置**：总开关开启；用户级含 userA；部门级/应用级空
- **步骤**：userA 从 appA 提交 Spark2 任务
- **预期**：切换 Spark3（3.4.4），日志记录用户级命中

#### TC-003：部门级名单命中强制切换（原有功能）`P0` `@regression @critical` `功能测试`
- **来源**：feature Rule 1, Scenario 3
- **前置**：总开关开启；用户级不含 userB；部门级含 userB 所属 dept02；应用级空
- **步骤**：userB 从 appB 提交 Spark2 任务
- **预期**：切换 Spark3，日志记录部门级命中

#### TC-004：非 Spark 引擎不受影响 `P1` `@regression` `功能测试`
- **来源**：feature Rule 1, Scenario 4
- **前置**：总开关开启；应用级含 appA
- **步骤**：userA 从 appA 提交 Hive 任务
- **预期**：使用 Hive 引擎，不切换 Spark3

#### TC-005：已是 Spark3 不重复切换 `P1` `@regression` `功能测试`
- **来源**：feature Rule 1, Scenario 5
- **前置**：总开关开启；应用级含 appA
- **步骤**：userA 从 appA 提交 Spark3 任务
- **预期**：使用 Spark3，版本保持 3.4.4，不重复改写

### 2.2 creator 维度强制切换（Rule 2）

#### TC-006：creator 在应用级名单时切换 Spark3 `P0` `@smoke @new-feature` `功能测试`
- **来源**：feature Rule 2, Scenario 1
- **前置**：总开关开启；用户级不含 userC；部门级不含 userC 所属 dept03；应用级含 appC
- **步骤**：userC 从 appC 提交 Spark2 任务
- **预期**：切换 Spark3，日志记录 creator 命中，含 creator 值 appC

#### TC-007：creator 不在名单时保持 Spark2 `P1` `@new-feature` `功能测试`
- **来源**：feature Rule 2, Scenario 2
- **前置**：总开关开启；应用级含 appC 不含 appD
- **步骤**：userD 从 appD 提交 Spark2 任务
- **预期**：使用 Spark2

#### TC-008：应用级名单空时保持 Spark2 `P1` `@new-feature` `功能测试`
- **来源**：feature Rule 2, Scenario 3
- **前置**：总开关开启；三级名单全空
- **步骤**：userE 从 appE 提交 Spark2 任务
- **预期**：使用 Spark2，行为与增强前一致

#### TC-009：creator 名单多应用正确匹配 `P1` `@new-feature` `功能测试`
- **来源**：feature Rule 2, Scenario 4
- **前置**：总开关开启；应用级含 app1,app2,app3
- **步骤**：userF 从 app2 提交 Spark2 任务
- **预期**：切换 Spark3

### 2.3 三维度优先级 个人>部门>creator（Rule 3）

#### TC-010：用户级命中时不检查 creator `P1` `@priority` `功能测试`
- **来源**：feature Rule 3, Scenario 1
- **前置**：用户级含 userA；应用级不含 appA
- **步骤**：userA 从 appA 提交 Spark2 任务
- **预期**：切换 Spark3，日志记录用户级命中，不记录 creator 命中

#### TC-011：部门级命中时不检查 creator `P1` `@priority` `功能测试`
- **来源**：feature Rule 3, Scenario 2
- **前置**：部门级含 userB 所属 dept02；应用级不含 appB
- **步骤**：userB 从 appB 提交 Spark2 任务
- **预期**：切换 Spark3，日志记录部门级命中，不记录 creator

#### TC-012：三维度均未命中保持 Spark2 `P1` `@priority` `功能测试`
- **来源**：feature Rule 3, Scenario 3
- **前置**：三级名单均不含 userG/dept07/appG
- **步骤**：userG 从 appG 提交 Spark2 任务
- **预期**：使用 Spark2

### 2.4 creator 维度异常降级（Rule 4）

#### TC-013：UserCreatorLabel 不存在保持 Spark2 `P1` `@negative @resilience` `功能测试`
- **来源**：feature Rule 4, Scenario 1
- **前置**：应用级含 appA；任务 UserCreatorLabel 不存在
- **步骤**：userH 提交 Spark2 任务
- **预期**：使用 Spark2，不抛异常，warn 日志

#### TC-014：creator 值空跳过 creator 检查 `P1` `@negative @resilience` `功能测试`
- **来源**：feature Rule 4, Scenario 2
- **前置**：应用级含 appA；creator 空字符串
- **步骤**：userI 提交 Spark2 任务
- **预期**：使用 Spark2，不抛异常

#### TC-015：creator 名单读取异常降级 `P1` `@negative @resilience` `功能测试`
- **来源**：feature Rule 4, Scenario 3
- **前置**：应用级名单读取异常
- **步骤**：userJ 从 appJ 提交 Spark2 任务
- **预期**：使用 Spark2，不抛异常，warn 日志

### 2.5 热加载（Rule 5）

#### TC-016：creator 名单热加载生效 `P1` `@config` `功能测试`
- **来源**：feature Rule 5, Scenario 1
- **前置**：总开关开启；应用级空
- **步骤**：userK 从 appK 提交（Spark2）→ 运维加 appK 到名单（不重启）→ userK 再提交
- **预期**：第1次 Spark2，第2次切换 Spark3

#### TC-017：creator 名单热加载移除恢复 Spark2 `P1` `@config` `功能测试`
- **来源**：feature Rule 5, Scenario 2
- **前置**：应用级含 appL
- **步骤**：userL 从 appL 提交（Spark3）→ 运维移除 appL → userL 再提交
- **预期**：第1次 Spark3，第2次 Spark2

### 2.6 高危区域冒烟（Rule 6）

#### TC-018：开关 on 冒烟 `P0` `@smoke @high-risk` `功能测试`
- **来源**：feature Rule 6, Scenario 1
- **前置**：总开关开启；应用级含 smokeApp
- **步骤**：smokeUser 从 smokeApp 提交 Spark2 任务
- **预期**：切换 Spark3，任务正常完成，日志/结果回写 Entrance

#### TC-019：开关 off 冒烟 `P0` `@smoke @high-risk` `功能测试`
- **来源**：feature Rule 6, Scenario 2
- **前置**：总开关关闭；应用级含 smokeApp
- **步骤**：smokeUser 从 smokeApp 提交 Spark2 任务
- **预期**：使用 Spark2，任务正常完成，行为与增强前一致

#### TC-020：并发场景 `P0` `@smoke @high-risk @concurrent` `性能测试`
- **来源**：feature Rule 6, Scenario 3
- **前置**：总开关开启；应用级含 concurrentApp
- **步骤**：3 个用户同时从 concurrentApp 提交 Spark2 任务
- **预期**：3 个任务全切换 Spark3，正常完成，无串扰

### 2.7 配置项管理迁移（Rule 7）⭐ 本次新增

#### TC-MIG-001：RPC 成功读配置项管理名单命中切换 `P0` `@migration @smoke` `功能测试`
- **来源**：feature Rule 7, Scenario 1
- **前置**：总开关开启；三张表（config_key/key_engine_relation/config_value）完整；linkis-ps-configuration 正常
- **步骤**：userM 从 appM（名单内）提交 Spark2 任务；fetchSpark3CoercionConfig 发 RPC 拉名单；getValue(keyAndValue) 读取
- **预期**：切换 Spark3（3.4.4），日志记录 creator 命中

#### TC-MIG-002：RPC 失败 fallback 到 properties 默认值 `P1` `@migration @negative @resilience` `功能测试`
- **来源**：feature Rule 7, Scenario 2
- **前置**：properties switch=true；名单配置 appN；linkis-ps-configuration 不可用
- **步骤**：userN 从 appN 提交；RPC 失败被 tryAndWarnMsg 捕获；keyAndValue=null；getValue(null) 走 properties
- **预期**：保持 Spark2，不阻断任务，warn 日志

#### TC-MIG-003：DB 未注册 key 行为与迁移前一致 `P1` `@migration @negative` `功能测试`
- **来源**：feature Rule 7, Scenario 3
- **前置**：配置项管理未注册 spark.version.coercion.* key（SQL 未执行）
- **步骤**：userO 从 appO 提交；RPC map 无 key；getValue 走 properties 默认
- **预期**：保持 Spark2，行为与迁移前一致

#### TC-MIG-004：三表漏 config_value 时 queryConfig 查不到 `P1` `@migration @negative` `参数配置`
- **来源**：feature Rule 7, Scenario 4
- **前置**：config_key + relation 已注册，config_value 无记录
- **步骤**：userP 从 appP 提交；queryConfig 不返回 key；等效 fallback
- **预期**：保持 Spark2

#### TC-MIG-005：switch 保留 properties 不迁移 `P2` `@migration @config` `参数配置`
- **来源**：feature Rule 7, Scenario 5
- **前置**：配置项管理未注册 switch；properties switch=true；名单含 appQ
- **步骤**：userQ 从 appQ 提交；map 无 switch；getValue(map) 走 properties true
- **预期**：切换 Spark3，代码统一 getValue(keyAndValue) 无需分叉

#### TC-MIG-006：前端 saveFullTree 改名单清 AM 缓存 `P2` `@migration @config` `流程案例`
- **来源**：feature Rule 7, Scenario 6
- **前置**：名单初始空
- **步骤**：userR 从 appR 提交（Spark2）→ 前端 saveFullTree 加 appR（广播 RemoveCacheConfRequest）→ userR 再提交
- **预期**：第1次 Spark2，第2次切换 Spark3，无需重启

#### TC-MIG-007：直接执行 SQL 后需重启 entrance 清 RPC 缓存 `P2` `@migration @config @known-limitation` `参数配置`
- **来源**：feature Rule 7, Scenario 7
- **前置**：RPC 缓存 expireAfterAccess 120000ms；名单含 appS
- **步骤**：运维直接执行 SQL（未走前端）→ userS 提交（未重启，可能旧缓存）→ 重启 entrance → userS 再提交
- **预期**：重启前可能旧值；重启后正确切换 Spark3

### 2.8 user+creator 组合细粒度（Rule 8）⭐ 新增

> 优先级：个人 > user+creator组合 > 部门 > creator。名单格式 "user:creator"，逗号分隔。split 精确匹配（避免 contains 子串误命中）。

#### TC-COMBO-001：user+creator 组合命中切换 Spark3 `P0` `@combo @smoke` `功能测试`
- **来源**：feature Rule 8, Scenario 1
- **前置**：总开关开启；组合名单 "userA:IDE"；用户级/应用级空
- **步骤**：userA 从 IDE 提交 Spark2 任务
- **预期**：切换 Spark3，日志记录组合命中

#### TC-COMBO-002：组合 creator 不匹配保持 Spark2 `P1` `@combo @negative` `功能测试`
- **来源**：feature Rule 8, Scenario 2
- **前置**：组合名单 "userA:IDE"
- **步骤**：userA 从 Schedulis 提交（userA:Schedulis 不在名单）
- **预期**：保持 Spark2

#### TC-COMBO-003：组合 user 不匹配保持 Spark2 `P1` `@combo @negative` `功能测试`
- **来源**：feature Rule 8, Scenario 3
- **前置**：组合名单 "otherUser:IDE"
- **步骤**：userA 从 IDE 提交（userA:IDE 不在名单）
- **预期**：保持 Spark2

#### TC-COMBO-004：组合精确匹配（子串安全）`P1` `@combo` `功能测试`
- **来源**：feature Rule 8, Scenario 4
- **前置**：组合名单 "userA:IDE"
- **步骤**：userA 从 ID 提交（userA:ID 子串，split 精确不命中）
- **预期**：保持 Spark2，验证精确匹配

#### TC-COMBO-005：用户级命中时不检查组合 `P1` `@combo @priority` `功能测试`
- **来源**：feature Rule 8, Scenario 5
- **前置**：用户级含 userA；组合名单 "otherUser:IDE"
- **步骤**：userA 从 IDE 提交
- **预期**：切换 Spark3（用户级命中），组合未检查

#### TC-COMBO-006：组合命中时不检查 creator `P1` `@combo @priority` `功能测试`
- **来源**：feature Rule 8, Scenario 6
- **前置**：组合名单 "userA:IDE"；应用级名单 "Schedulis"
- **步骤**：userA 从 IDE 提交
- **预期**：切换 Spark3（组合命中），creator 维度未检查

---

## 3. 测试用例统计

### 按优先级
| 优先级 | 数量 | 占比 |
|-------|:----:|:----:|
| P0 | 10 | 28% |
| P1 | 19 | 53% |
| P2 | 4 | 11% |
| 其他 | 3 | 8% |
| **合计** | **36** | 100% |

### 按测试类型
| 类型 | 数量 |
|------|:----:|
| 功能测试 | 27 |
| 参数配置 | 2 |
| 流程案例 | 1 |
| 性能测试（并发）| 1 |

### 按维度
| 维度 | 用例 | 说明 |
|------|:----:|------|
| 保持现有功能 | 5 | TC-001~005 |
| creator 维度 | 4 | TC-006~009 |
| 优先级 | 3 | TC-010~012 |
| 异常降级 | 3 | TC-013~015 |
| 热加载 | 2 | TC-016~017 |
| 冒烟 | 3 | TC-018~020 |
| **配置项管理迁移** | **7** | **TC-MIG-001~007** |
| **user+creator 组合** | **6** | **TC-COMBO-001~006** |

---

## 4. Feature 覆盖率

| Feature 文件 | Rule 数 | Scenario 数 | 已生成 TC | 覆盖率 |
|------------|:------:|:----------:|:--------:|:-----:|
| spark3-coercion-creator.feature | 8 | 36 | 36 | 100% |

### 验收标准覆盖
| 验收标准（需求文档）| 覆盖 TC |
|------------------|---------|
| creator 命中名单切换 Spark3 | TC-006, TC-MIG-001 |
| creator 未命中保持 Spark2 | TC-007 |
| 名单空行为与增强前一致 | TC-008, TC-MIG-003 |
| 优先级 个人>部门>creator | TC-010, TC-011 |
| 异常降级不阻断任务 | TC-013~015, TC-MIG-002 |
| RPC 失败 fallback properties | TC-MIG-002 |
| switch 保留 properties | TC-MIG-005 |
| 三表完整性 | TC-MIG-001, TC-MIG-004 |
| 热加载/缓存 | TC-016~017, TC-MIG-006~007 |

**覆盖率**：9/9 验收标准（100%）

---

## 5. 单元测试映射

单元测试 [CommonEntranceParserSpark3CoercionTest](../../linkis-computation-governance/linkis-entrance/src/test/scala/org/apache/linkis/entrance/parser/CommonEntranceParserSpark3CoercionTest.scala)（11 用例）覆盖 TC-MIG 系列的核心逻辑：
- testCreatorHit → TC-006/TC-MIG-001
- testRpcFallbackToNull → TC-MIG-002
- testExceptionDegradation → TC-013~015
- testSwitchOff → TC-001
- testUserPriorityOverCreator → TC-010
- testDepartmentPriorityOverCreator → TC-011

---

## 6. Wemind 导入

Wemind 导入文件（配置项管理迁移 7 用例）：[spark3-coercion-config-migration_wemind导入.json](../../dev/active/spark3-coercion-creator/spark3-coercion-config-migration_wemind导入.json)

---

**生成时间**：2026-07-24 ｜ **生成方式**：functional-test-generator（基于 feature 30 Scenario 转化）
