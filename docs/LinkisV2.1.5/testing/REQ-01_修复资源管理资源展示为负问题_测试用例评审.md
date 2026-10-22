# 修复资源管理资源展示为负问题 测试用例内容评审记录

> 版本：LinkisV2.1.5 ｜ 需求：REQ-01 ｜ 评审时间：2026-10-20 ｜ 评审方式：既有用例内容评审（变更单生成前置，final-readiness 约定）
> 评审对象：`docs/LinkisV2.1.5/testing/REQ-01_修复资源管理资源展示为负问题_测试用例.md`（TC001-TC019）
> 评审依据：设计文档（§3 O3/OC1 设计、§4 异常、§5 回滚、§12.4 风险测试要求）、Feature 文件（12 Scenario）、RTR-001~RTR-003 共 8 项风险测试要求

## 一、评审范围与方法

逐例核查三个维度：①结构完整性（前置条件/测试步骤/预期结果/优先级/Gherkin 或接口规格齐全）；②需求与设计一致性（场景、预期与 O3/OC1/OC1增强 三维机制及 RTR 验证点对应）；③可执行性（环境前置、数据策略、与实际执行方式标注一致）。另核对用例与风险测试要求（RTR）映射的完整性。

## 二、逐例评审结论

| 用例 | 覆盖要点 | 结构 | 一致性 | 结论 |
|------|---------|------|--------|------|
| TC001 | O3 引擎退出负值丢弃（开关开启） | 完整（Given/When/Then+Gherkin） | 与设计 3.1 丢弃逻辑及 RTR-001-CONFIG-EFFECT/RTR-003-DOWNSTREAM-LINKAGE 一致 | 通过 |
| TC002 | 开关关闭保持原有行为（向后兼容） | 完整 | 与设计 5.2 回滚及 RTR-001-CONFIG-ROLLBACK 一致 | 通过 |
| TC003 | 正常引擎退出不受丢弃逻辑影响 | 完整 | 与 RTR-003 下游联动一致 | 通过 |
| TC004 | usedResource 负值归零 | 完整 | 与 OC1 归零及 RTR-002-DOWNSTREAM-LINKAGE 一致 | 通过 |
| TC005 | leftResource 负值归零 | 完整 | 与 OC1 归零及 RTR-002-DOWNSTREAM-LINKAGE 一致 | 通过 |
| TC006 | 非负值保持原值不变 | 完整 | 与 OC1 非篡改约束及 RTR-001-CONFIG-BOUNDARY 一致 | 通过 |
| TC007 | left 按 max-used 重算（默认开启） | 完整 | 与 OC1增强 3.2.6 及 RTR-002-DOWNSTREAM-LINKAGE 一致 | 通过 |
| TC008 | 重算开关关闭保留 DB 快照 | 完整 | 与回滚设计 5.1 一致 | 通过 |
| TC009 | max/used 缺失字段规则 | 完整 | 与 RTR-002-CROSS-SYSTEM-FAILURE 容错要求一致 | 通过 |
| TC010 | 单用户接口不受重算影响 | 完整 | 与 OC1 范围约束（仅 allUserResource）一致 | 通过 |
| TC011 | O3+OC1 协同保证非负 | 完整 | 与两层机制架构 2.1 及 RTR-003-CASCADE-RECOVERY 一致 | 通过 |
| TC012 | O3 关闭时 OC1 兜底 | 完整 | 与 RTR-001-CONFIG-BOUNDARY 单层失效兜底一致 | 通过 |
| TC013a | allUserResource 管理员鉴权访问 | 完整（HTTP 方法/路径） | 与 RMMonitorRest 管理员鉴权要求一致 | 通过 |
| TC013b | allUserResource 非管理员（无 token）拒绝 | 完整（HTTP 方法/路径） | 与 RMMonitorRest ONLY_ADMIN_READ 一致 | 通过 |
| TC014 | 分页返回资源列表 | 完整 | 与接口改造点一致 | 通过 |
| TC015 | get-user-resource 单用户查询原样 | 完整 | 与 OC1 不影响单用户口径一致 | 通过 |
| TC016 | O3 开关非标准布尔值视为 false | 完整 | 与 RTR-001-CONFIG-BOUNDARY 越界值要求一致 | 通过 |
| TC017 | OC1 归零遇 null/非数值容错 | 完整 | 与 RTR-002-CROSS-SYSTEM-FAILURE 一致 | 通过 |
| TC018 | OC1 归零异常时接口正常返回（降级） | 完整 | 与 RTR-002-CASCADE-RECOVERY 降级要求一致 | 通过 |
| TC019 | 丢弃逻辑不影响引擎状态清理 | 完整 | 与 RTR-003-CASCADE-RECOVERY 一致 | 通过 |

## 三、评审发现（记录修正建议，不阻断用例有效性）

1. **优先级统计表遗漏 TC011**：用例文档 §七 优先级分布仅列 18 例（P0 9 例 + P1 9 例），遗漏 TC011（P0，集成测试）。修正后应为 P0 10 例、P1 9 例、总计 19 例。不影响用例本体与执行结论。
2. **执行报告单测子表用例编号错位**：测试执行报告 §三 单测子表中 TC008/TC009/TC010/TC018 的场景描述与用例文档同名用例定义不一致（报告 TC008=「used 为负数先归零」、TC009=「used>max 归零」、TC010=「字段缺失防御」、TC018=「Double 边界」，分别对应单测方法 `zeroesUsedBeforeSub`/`negativeResultZeroed`/`missingFieldIsSafe`/`doubleTypes`，而用例文档同名编号为回退/缺失规则/单用户/降级场景）。单测方法本身真实且全部通过，但建议后续核对两文档的编号映射，避免证据引用歧义。
3. **TC016 执行口径与定义不一致**：用例文档 TC016 定义为「O3 开关非标准布尔值视为 false」，实际执行报告将该编号用于「不存在的 engineType 边界」并实测通过；开关非标准布尔值场景未单独实测（CommonVars 框架层行为，由 true/false 两态翻转实测旁证）。建议后续版本补测或注明口径。

## 四、评审结论

- **逐例结论**：19/19 用例内容评审通过（结构完整、与设计及风险测试要求一致、可执行）。
- **RTR 覆盖**：8/8 风险测试要求均有对应用例（用例文档 §5.3 映射 100%）。
- **整体结论**：**评审通过**；上述 3 项为记录层面的修正建议（非阻断），建议在下一版本迭代中修正文档。
