# Spark3 强制切换 配置项管理迁移 测试报告

| 项目 | 内容 |
|-----|------|
| 任务 | spark3-coercion-creator（配置项管理迁移） |
| 测试时间 | 2026-07-24 |
| 测试范围 | 配置项管理迁移代码变更（properties → 配置项管理 RPC 读取） |
| 关联需求 | [spark3-coercion-creator_需求.md](../requirements/spark3-coercion-creator_需求.md) 第10章 |
| 关联设计 | [spark3-coercion-creator_设计.md](../design/spark3-coercion-creator_设计.md) Part 4 |

---

## 1. 测试概述

本次测试验证「Spark3 强制切换配置项管理迁移」：将 3 个名单（users/department.id/creators）从 `linkis-cg-entrance.properties` 迁移到 Linkis 配置项管理，entrance 通过 RPC 读取；switch 保留 properties。

**核心验证点**：
- RPC 读取配置项管理值，命中切换 Spark3
- RPC 失败 fallback 到 properties 默认值，不阻断任务
- 三张表（config_key/key_engine_relation/config_value）完整性
- switch 保留 properties（getValue(map) 对缺失 key 自动 fallback）
- 判定逻辑（个人>部门>creator）与迁移前一致

---

## 2. 测试用例统计

| 测试类型 | 用例数 | 状态 | 说明 |
|---------|:------:|:----:|------|
| 单元测试 | 11 | ✅ 全过 | CommonEntranceParserSpark3CoercionTest |
| 功能测试（feature 迁移场景）| 7 | 📋 用例就绪 | docs/dev-2.0.0/features/spark3-coercion-creator.feature（Rule: 配置项管理迁移）|
| 模块回归集（沉淀）| 7 | 📋 已沉淀 | docs/project-knowledge/testing/features/linkis-entrance.feature |
| **合计** | **25** | — | — |

---

## 3. 单元测试执行结果

**测试类**：[CommonEntranceParserSpark3CoercionTest.scala](../../linkis-computation-governance/linkis-entrance/src/test/scala/org/apache/linkis/entrance/parser/CommonEntranceParserSpark3CoercionTest.scala)

**结果**：`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` ✅ BUILD SUCCESS

| 用例 | 覆盖点 | 结果 |
|------|--------|:----:|
| testCreatorHit | creator 命中名单 → 切换 Spark3 | ✅ |
| testCreatorNotHit | creator 未命中 → 保持 Spark2 | ✅ |
| testCreatorListEmpty | 名单空 → 保持 Spark2（与迁移前一致）| ✅ |
| testUserCreatorLabelNotExists | UserCreatorLabel 缺失 → 不抛异常 | ✅ |
| testCreatorBlank | creator null/空白 → 不命中 | ✅ |
| testUserPriorityOverCreator | 用户级优先于 creator | ✅ |
| testDepartmentPriorityOverCreator | 部门级优先于 creator | ✅ |
| testSwitchOff | 总开关关 → 不切换 | ✅ |
| testNonSparkEngine | 非 Spark 引擎 → 不切换 | ✅ |
| **testRpcFallbackToNull** | **RPC 返回 null → fallback properties 默认** | ✅ |
| testExceptionDegradation | asInstanceOf 异常 → tryAndWarnMsg 降级 | ✅ |

**测试方式**：匿名子类 override `fetchSpark3CoercionConfig`（返回 configMap）+ `fetchUserDepartmentId`（返回 mockDeptId），绕过真实 RPC 与单例反射（JDK21 限制）。

---

## 4. 覆盖率分析

### 迁移核心逻辑覆盖（单测）
- ✅ `fetchSpark3CoercionConfig` seam + RPC fallback（testRpcFallbackToNull）
- ✅ `getValue(keyAndValue)` 读法（configMap 注入值）
- ✅ `fetchUserDepartmentId` seam（部门级判定）
- ✅ switch 走 configMap/properties（testSwitchOff）
- ✅ 优先级 个人>部门>creator（testUserPriorityOverCreator/testDepartmentPriorityOverCreator）
- ✅ 异常降级（Utils.tryAndWarnMsg，testExceptionDegradation）

### 功能测试覆盖（feature 7 场景，用例就绪）
- RPC 成功读配置项管理名单命中切换
- RPC 失败 fallback 到 properties 默认值
- DB 未注册 key 行为与迁移前一致
- 三张表漏 config_value 时 queryConfig 查不到
- switch 保留 properties 不迁移
- 前端 saveFullTree 改名单清 AM 缓存
- 直接执行 SQL 后需重启 entrance 清 RPC 缓存

---

## 5. 已知限制与待集成验证

| 项 | 说明 | 风险 |
|---|------|:----:|
| RPC 缓存坑 | 直接执行 SQL 后需重启 linkis-cg-entrance（120s expireAfterAccess）| 🟡 运维需注意 |
| 名单注入 EC | AM 独立拉配置注入 EC（冗余但无害，EC 不读）| 🟢 接受 |
| 三表完整性 | 缺 config_value 则 queryConfig 查不到 | 🟡 SQL 脚本已含第③步 |
| 端到端冒烟 | 前端配名单 → 任务切换 → 日志/结果回写 | 🟡 待部署环境验证 |

---

## 6. 兼容性验证

| 场景 | 结果 |
|------|:----:|
| SQL 未执行（DB 未注册 key）| ✅ getValue 走 properties 默认，行为与迁移前一致 |
| RPC 失败（服务不可用）| ✅ getValue(null) 走 properties 默认，不阻断 |
| switch 保留 properties | ✅ map 无 switch → getValue 走 properties |
| 判定逻辑/优先级 | ✅ 与迁移前完全一致 |

---

## 7. 测试结论

✅ **配置项管理迁移单元测试全部通过（11/11）**，核心逻辑（RPC 读取、fallback、判定、降级）验证充分。

📋 功能测试用例（7 场景）+ 模块回归集（linkis-entrance，7 用例）已就绪。

**建议**：部署后执行端到端冒烟（前端 setting 配名单 + switch 开启 → Spark2 任务切换 Spark3 → 日志/结果回写正常），并验证 RPC 缓存重启场景。

**测试状态**：通过（单元测试），功能/集成测试用例就绪待环境执行。
