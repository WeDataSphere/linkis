# Jackson 超长字符串处理 测试报告

| 项 | 内容 |
|---|---|
| 需求名称 | Jackson 超长字符串处理（修复 Spark EventLog StreamConstraintsException） |
| DPMS | 534335 |
| 模块 | linkis-engineconn-plugins/spark |
| 测试日期 | 2026-07-27 |
| 测试人 | v-kkhuang |
| 关联用例文档 | [Jackson超长字符串处理_测试用例.md](./Jackson超长字符串处理_测试用例.md) |
| 关联用例 JSON | [wemind/Jackson超长字符串处理_wemind导入.json](./wemind/Jackson超长字符串处理_wemind导入.json) |

---

## 一、测试结论

| 维度 | 结果 |
|---|---|
| 自动化用例 | **7/7 通过**（6 个测试方法，覆盖 7 个逻辑用例） |
| 集成用例 | **TC-W-002 通过**（端到端，部署环境验证） |
| 总体结论 | **通过**（自动化全绿 + 端到端通过；仅反射兜底为静态审查，未发现缺陷） |
| 质量风险 | 低 — 核心行为（反射生效 + 全局传播）已被自动化证明 |

---

## 二、自动化执行证据

**命令**：
```bash
./mvnw -pl linkis-engineconn-plugins/spark test \
  -Dtest=TestSparkJacksonConfiguration,TestSparkEngineConnFactoryJacksonConstraints \
  -DfailIfNoTests=false -Dscalastyle.skip=true
```

**Surefire 输出**：
```
Running org.apache.linkis.engineplugin.spark.config.TestSparkJacksonConfiguration
Running org.apache.linkis.engineplugin.spark.factory.TestSparkEngineConnFactoryJacksonConstraints
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.018 s - in ...TestSparkJacksonConfiguration
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.848 s - in ...TestSparkEngineConnFactoryJacksonConstraints
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**测试类**：
- `linkis-engineconn-plugins/spark/src/test/scala/org/apache/linkis/engineplugin/spark/factory/TestSparkEngineConnFactoryJacksonConstraints.scala`（反射生效 + 传播 + 基线 + 还原，4 个方法）
- `linkis-engineconn-plugins/spark/src/test/scala/org/apache/linkis/engineplugin/spark/config/TestSparkJacksonConfiguration.scala`（配置默认值，2 个方法）

---

## 三、用例执行明细

| 用例 | 分类 | 优先级 | 执行方式 | 结果 | 说明 |
|---|---|:---:|---|:---:|---|
| TC-F-001 maxStringLength 放宽到 10000000 | 功能 | P0 | 自动化 `testOverrideRelaxesGlobalDefault` | ✅ 通过 | 反射后 DEFAULT == 10000000，且确有变化 |
| TC-F-002 maxNestingDepth 放宽到 2000 | 功能 | P0 | 自动化 `testOverrideRelaxesGlobalDefault` | ✅ 通过 | 反射后 DEFAULT == 2000 |
| TC-F-003 传播到 new JsonFactory | 功能 | P0 | 自动化 `testOverridePropagatesToNewJsonFactory` | ✅ 通过 | json4s 路径生效的根本证明 |
| TC-F-004 基线=Jackson 文档常量 | 功能 | P1 | 自动化 `testOriginalDefaultMatchesJacksonConstant` | ✅ 通过 | 捕获原值一致 |
| TC-E-002 还原机制不污染套件 | 功能 | P1 | 自动化 `testRestoreLeavesDefaultUnchangedAcrossTests` | ✅ 通过 | DEFAULT 恢复原值 |
| TC-P-001 maxStringLength 默认值 | 参数配置 | P0 | 自动化 `testJacksonMaxStringLengthDefault` | ✅ 通过 | 默认 10000000 |
| TC-P-002 maxNestingDepth 默认值 | 参数配置 | P0 | 自动化 `testJacksonMaxNestingDepthDefault` | ✅ 通过 | 默认 2000 |
| TC-P-003 配置项可被 -D 覆盖 | 参数配置 | P1 | 手工/配置 | ⏸ 待执行 | 需带 -D 启动验证 |
| TC-E-001 反射失败兜底 | 功能-负向 | P1 | 白盒/静态审查 | ✅ 审查通过 | catch 分支确认；JDK11 失败时降级默认，不阻断 |
| TC-W-001 调用顺序在 SparkContext 前 | 流程 | P0 | 白盒/静态审查 | ✅ 审查通过 | createEngineConnSession 开头即调用 |
| TC-W-002 端到端超长字符串 | 流程 | P0 | 集成 | ✅ 通过 | 部署环境验证通过 |

---

## 四、统计

| 状态 | 数量 |
|---|:---:|
| ✅ 通过（自动化） | 7 |
| ✅ 通过（静态审查） | 2 |
| ✅ 通过（集成） | 1 |
| ⏸ 待执行 | 1 |
| ❌ 失败 | 0 |
| 合计 | 11 |

- 自动化覆盖率（自动化通过 / 可自动化用例）：7/7 = 100%
- P0 用例：7（自动化 5 + 静态审查 1 + 集成 1）
- 发现缺陷：0

---

## 五、测试环境

| 项 | 值 |
|---|---|
| JDK | 1.8 |
| Scala | 2.11.12 |
| Jackson | 2.15.0 |
| Spark profile | 默认（spark-2.4.3） |
| 构建分支 | dev-2.1.0-webank（revision 2.1.0） |
| 测试框架 | JUnit 5 (Jupiter) + Maven Surefire 3.0.0-M7 |

---

## 六、未覆盖项与风险

| 项 | 风险 | 缓解 |
|---|---|---|
| TC-E-001 反射失败兜底 | JDK 11+ 未加 `--add-opens` 时反射可能失败 | 静态审查确认 try/catch 兜底；建议 JDK11 部署时在引擎启动脚本加 `--add-opens java.base/java.lang.reflect=ALL-UNNAMED` |
| 全局静态副作用 | 改的是 JVM 全局 DEFAULT，影响整个 EC 进程 | 方向为放宽（安全）；单测有 save/restore 隔离 |
| TC-P-003 配置 -D 覆盖 | 未自动化（CommonVars 启动期缓存，单测难干净覆盖） | 手工/部署时带 `-D` 验证 |

> TC-W-002（端到端）已部署环境验证通过；TC-P-001/002 配置默认值已补单测通过。

---

## 七、循环决策（Stage 9）

- 自动化用例 7/7 通过 + 端到端 TC-W-002 通过，无失败、无缺陷 → **不进入修复循环**
- 该需求测试收尾，仅余 TC-P-003（-D 覆盖）建议在部署联调时顺手验证

---

## 八、产物清单

| 产物 | 路径 |
|---|---|
| 测试用例文档 | `docs/dev-2.1.0/testing/Jackson超长字符串处理_测试用例.md` |
| Wemind 用例 JSON | `docs/dev-2.1.0/testing/wemind/Jackson超长字符串处理_wemind导入.json` |
| 测试报告 | `docs/dev-2.1.0/testing/Jackson超长字符串处理_测试报告.md` |
| 自动化测试代码 | `.../factory/TestSparkEngineConnFactoryJacksonConstraints.scala`（4 方法）+ `.../config/TestSparkJacksonConfiguration.scala`（2 方法） |
| 流程状态 | `dev/active/jackson-overlong/context.md` |
