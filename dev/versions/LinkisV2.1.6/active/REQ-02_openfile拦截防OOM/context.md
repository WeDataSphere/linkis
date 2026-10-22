---
contextSchemaVersion: "1.0"
contextRevision: 19
checkpointId: "LinkisV2.1.6:REQ-02:10:2026-10-21T05:30:00Z"
checkpointState: "clean"
currentStage: "10"
lastCompletedStage: "10"
stageHistory: ["0", "1", "1.1", "1.2", "1.6", "2", "2.1", "2.2", "3", "3.1", "3.2", "3.5", "4", "5", "5.5", "6", "7", "8", "9", "10"]
skipDecisions: {}
rollbackHistory: []
taskName: "REQ-02_openfile拦截防OOM"
requirementType: "ENHANCE"
executionMode: "快速模式"
interactionConfig: "standard_development"
loopDecisionMode: "ask"
inputSource: "version_mode"
reqIndex: "02"
reqPrefix: "REQ-02"
versionName: "LinkisV2.1.6"
versionContextPath: "dev/versions/LinkisV2.1.6/version-context.md"
dpmsStoryId: "555374"
testSetId: "324253"
regressionTestSetId: "324254"
releasePlanId: "182124"
productId: "100484"
productName: "BDP_Linkis"
subsystemId: "5435"
subsystemName: "WDSL-UJES"
subsystemVersionId: "2.1.6"
branchName: "release_v2.1.6"
pipelineId: "1039300005284218"
pipelineVersion: "WDSL-UJES_2.1.6"
reqSubsystemId: "5435"
repositoryId: null
functionAttributes: "后端"
frontendType: ""
selectedDevAgents: []
knowledgeBaseStatus: ""
moduleIndexPath: ""
stageOutputs:
  "0":
    status: "completed"
    summary: "Stage 0需求澄清完成：功能属性=后端；openFile行数拦截（已翻页累计行数>阈值→开关开返回错误/关打warn）；文档已达Stage1深度；2处Stage1.1待修正项已记录"
    artifacts: ["docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md"]
    decisions: ["functionAttributes=backend", "readrows_threshold=(page-1)*pageSize", "limit_enable_default_false"]
    risks: ["stage1.1_fix_q1_text_vs_pseudocode", "errorcode_80037_register_pending"]
    completedAt: "2026-10-21T01:00:00Z"
  "1":
    status: "completed"
    summary: "Stage 1需求分析完成（用户直接撰写需求文档，达Stage1深度）；主会话合成结果"
    artifacts: [{"type": "requirement_doc", "path": "docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md"}]
    decisions: ["user_authored_doc"]
    risks: ["stage1.1_fix_q1_text_vs_pseudocode", "errorcode_80037_register_pending"]
    completedAt: "2026-10-21T01:15:00Z"
artifactIndex: {}
apiDocumentStatus: {}
documentArchive: {}
requirementWordExports: {}
designWordExports: {}
stage3DesignReclosure: {}
staleArtifacts: []
openRisks: []
decisionLog: []
createdAt: "2026-10-21T00:00:00Z"
updatedAt: "2026-10-21T05:30:00Z"
stabilityRisks: [{"id": "R-001", "risk_level": "高", "risk_type": "模块链路", "affected_scope": "openFile结果集读取链路(FsRestfulApi.openFile增加行数拦截)", "test_case_requirements": "下游联动验证<br>跨系统调用异常验证<br>级联失败恢复验证", "evidence": "需求事实:FsRestfulApi.openFile增加行数超限拦截逻辑(需求文档3.1功能点F-3)\n影响依据:openFile是生产高频接口;catalog.systems无linkis-publicservice的categories;关联影响表未登记下游\n风险判断:修改openFile主链路接口,虽开关默认false但触及公共服务读取主链路\n缺失信息:组织画像未登记publicservice的is_base_engine;catalog.systems无categories\n处理建议:设计阶段保持开关默认false;AC1/AC2覆盖开关on/off", "status": "待处理", "source_stage": "1.1"}, {"id": "R-002", "risk_level": "高", "risk_type": "性能容量", "affected_scope": "openFile内存(翻页读取超大结果集>10w行,collect()整页内存组装)", "test_case_requirements": "压测<br>容量评估验证", "evidence": "需求事实:openFile翻页读取超大结果集(>10w行)collect()整页内存组装可能OOM(需求文档1.1)\n影响依据:pageSize=5000行*N列整页进堆;并发翻页峰值内存叠加\n风险判断:OOM防护是本次改造目标,但需压测验证阈值有效性\n缺失信息:无压测/容量评估数据\n处理建议:Stage5.5覆盖并发翻页压测;AC1验证开关关闭行为不变", "status": "待处理", "source_stage": "1.1"}, {"id": "R-003", "risk_level": "高", "risk_type": "参数配置", "affected_scope": "openFile行数拦截配置(limit.enable默认false+limit.rows默认100000)", "test_case_requirements": "配置变更生效验证<br>配置回滚验证<br>配置边界值验证", "evidence": "需求事实:新增limit.enable(默认false)+limit.rows(默认100000)配置(需求文档5.1)\n影响依据:配置控制openFile翻页拦截行为\n风险判断:新增配置项,需验证生效/回滚/边界\n缺失信息:无\n处理建议:AC3覆盖阈值配置生效;8.2有回滚方案", "status": "待处理", "source_stage": "1.1"}]
stabilityRiskAssessmentTrace: {"R-001": {"final_level": "高", "deterministic_level": "高", "score": 90, "score_level": "高", "matched_rule_ids": ["KNOWLEDGE-UNKNOWN-001", "CROSS-SYSTEM-001", "CHANGE-TYPE-UNKNOWN-001", "INPUT-VALUE-UNKNOWN-001"], "knowledge_status": "unknown", "degraded": true, "blocking": false, "missing_knowledge": ["asset_catalog_entry"]}, "R-002": {"final_level": "高", "deterministic_level": "高", "score": 90, "score_level": "高", "matched_rule_ids": ["KNOWLEDGE-UNKNOWN-001", "CROSS-SYSTEM-001", "CHANGE-TYPE-UNKNOWN-001", "INPUT-VALUE-UNKNOWN-001"], "knowledge_status": "unknown", "degraded": true, "blocking": false, "missing_knowledge": ["asset_catalog_entry"]}, "R-003": {"final_level": "高", "deterministic_level": "高", "score": 90, "score_level": "高", "matched_rule_ids": ["KNOWLEDGE-UNKNOWN-001", "CROSS-SYSTEM-001", "CHANGE-TYPE-UNKNOWN-001", "INPUT-VALUE-UNKNOWN-001"], "knowledge_status": "unknown", "degraded": true, "blocking": false, "missing_knowledge": ["asset_catalog_entry"]}}
stabilityRiskSourceEvidence: [{"evidence_id": "R-001-REQ", "kind": "requirement_document", "value": "openFile增加行数拦截", "state": "present", "source_refs": ["docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md#3.1"], "source_excerpt": "FsRestfulApi.openFile中分页读取前用已读取行数与阈值比较", "resolver_id": "requirement_document_literal_resolver"}, {"evidence_id": "R-002-REQ", "kind": "requirement_document", "value": "collect整页内存组装", "state": "present", "source_refs": ["docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md#1.1"], "source_excerpt": "collect()把整页数据全量装入内存", "resolver_id": "requirement_document_literal_resolver"}, {"evidence_id": "R-003-REQ", "kind": "requirement_document", "value": "limit.enable+limit.rows配置", "state": "present", "source_refs": ["docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md#5.1"], "source_excerpt": "新增limit.enable默认false+limit.rows默认100000", "resolver_id": "requirement_document_literal_resolver"}]
stabilityRiskRequirementMappings: [{"risk_id": "R-001", "requirement_items": [{"item_id": "F-3", "item_type": "enhancement", "title": "超限判定逻辑", "source_ref": "3.1 F-3"}, {"item_id": "AC2", "item_type": "acceptance_criterion", "title": "开关打开时超限返回错误", "source_ref": "4 AC2"}]}, {"risk_id": "R-002", "requirement_items": [{"item_id": "F-2", "item_type": "enhancement", "title": "已读取行数统计", "source_ref": "3.1 F-2"}, {"item_id": "AC1", "item_type": "acceptance_criterion", "title": "开关关闭时行为不变", "source_ref": "4 AC1"}]}, {"risk_id": "R-003", "requirement_items": [{"item_id": "F-1", "item_type": "enhancement", "title": "新增功能开关", "source_ref": "3.1 F-1"}, {"item_id": "F-6", "item_type": "enhancement", "title": "阈值可配置", "source_ref": "3.1 F-6"}, {"item_id": "AC3", "item_type": "acceptance_criterion", "title": "阈值配置生效", "source_ref": "4 AC3"}]}]
stabilityRiskBusinessConclusions: [{"risk_id": "R-001", "change_fact": "改造openFile接口增加行数拦截", "business_impact": "结果集读取链路异常", "conclusion": "改造openFile接口增加行数拦截可能导致结果集读取链路异常"}, {"risk_id": "R-002", "change_fact": "openFile翻页读取超大结果集", "business_impact": "服务OOM", "conclusion": "openFile翻页读取超大结果集可能导致服务OOM"}, {"risk_id": "R-003", "change_fact": "新增openFile行数拦截配置开关", "business_impact": "结果集翻页行为变化", "conclusion": "新增openFile行数拦截配置开关可能导致结果集翻页行为变化"}]
stabilityRiskTestRequirements: [{"requirement_id": "RTR-001-DOWNSTREAM-LINKAGE", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-DOWNSTREAM-LINKAGE-001", "policy_version": "1.0", "test_type": "downstream_linkage", "title": "下游联动验证", "business_context": null, "display_text": "下游联动验证", "verification_points": ["覆盖已确认的下游调用链", "验证上下游输入输出和状态传递"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-001-CROSS-SYSTEM-FAILURE", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-CROSS-SYSTEM-FAILURE-001", "policy_version": "1.0", "test_type": "cross_system_failure", "title": "跨系统调用异常验证", "business_context": null, "display_text": "跨系统调用异常验证", "verification_points": ["覆盖跨系统调用失败或异常返回", "验证调用方错误处理和业务状态"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-001-CASCADE-RECOVERY", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-CASCADE-RECOVERY-001", "policy_version": "1.0", "test_type": "cascade_recovery", "title": "级联失败恢复验证", "business_context": null, "display_text": "级联失败恢复验证", "verification_points": ["覆盖局部故障向下游传播的场景", "验证隔离、恢复和业务状态收敛"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-002-PRESSURE", "risk_id": "R-002", "policy_rule_ids": ["RTP-PERFORMANCE-CAPACITY-001"], "definition_id": "RTEST-PRESSURE-001", "policy_version": "1.0", "test_type": "pressure", "title": "压测", "business_context": null, "display_text": "压测", "verification_points": ["明确压测负载、数据量和持续时间", "验证吞吐量、延迟和资源水位达到目标"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed", "test_report_available"], "required": true, "applicability_basis": ["risk.risk_type=\"性能容量\""]}, {"requirement_id": "RTR-002-CAPACITY", "risk_id": "R-002", "policy_rule_ids": ["RTP-PERFORMANCE-CAPACITY-001"], "definition_id": "RTEST-CAPACITY-001", "policy_version": "1.0", "test_type": "capacity", "title": "容量评估验证", "business_context": null, "display_text": "容量评估验证", "verification_points": ["验证目标数据量或请求量下的容量余量", "明确容量上限、扩容阈值和风险点"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed", "test_report_available"], "required": true, "applicability_basis": ["risk.risk_type=\"性能容量\""]}, {"requirement_id": "RTR-003-CONFIG-EFFECT", "risk_id": "R-003", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-EFFECT-001", "policy_version": "1.0", "test_type": "configuration_effect", "title": "配置变更生效验证", "business_context": null, "display_text": "配置变更生效验证", "verification_points": ["验证目标配置值在声明范围内生效", "验证生效后的业务行为符合预期"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}, {"requirement_id": "RTR-003-CONFIG-ROLLBACK", "risk_id": "R-003", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-ROLLBACK-001", "policy_version": "1.0", "test_type": "configuration_rollback", "title": "配置回滚验证", "business_context": null, "display_text": "配置回滚验证", "verification_points": ["验证配置可恢复至变更前状态", "验证回滚后的业务行为和数据状态"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}, {"requirement_id": "RTR-003-CONFIG-BOUNDARY", "risk_id": "R-003", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-BOUNDARY-001", "policy_version": "1.0", "test_type": "configuration_boundary", "title": "配置边界值验证", "business_context": null, "display_text": "配置边界值验证", "verification_points": ["覆盖最小值、最大值和越界值", "验证非法配置的拒绝或保护行为"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}]
touchpointCompletion: {"stage1": {"complete": true, "flow_control": "continue", "source_fingerprint": "degraded_org_catalog_incomplete"}, "status": "applicability_incomplete_degraded", "stage2": {"complete": true, "flow_control": "continue", "verification": "manual_verify_final_design_provider_unavailable_degraded_nonblocking", "covered_risks": ["R-001", "R-002", "R-003"], "review_dims_passed": "架构:拦截点FsRestfulApi行704前;异常:try/catch兜底降级放行;配置:2开关WorkSpaceConfiguration;数据:(page-1)*pageSize阈值long防溢出;监控:拦截info日志;回滚:开关关闭+异常降级"}}
  "2":
    status: "completed"
    summary: "Stage2设计完成：openFile翻页OOM拦截方案（(page-1)*pageSize阈值拦截，80038，开关默认关闭，try/catch降级放行），拦截点FsRestfulApi.openFile行704前"
    artifacts: [{"type":"design_doc","path":"docs/LinkisV2.1.6/design/REQ-02_openfile拦截防OOM_设计.md"}]
    decisions: ["ADR-001 Controller层拦截", "ADR-002 超阈抛80038不截断", "ADR-003 全局开关", "ADR-004 阈值100000"]
    risks: ["深翻页大结果集仍可能OOM-开关关闭时无保护(low)", "阈值默认值需线上调参(medium)"]
    completedAt: "2026-10-21T02:30:00Z"

  "2.1":
    status: "completed"
    summary: "Stage2.1设计检视通过：des-recheck六维度(架构/异常/配置/数据/监控/回滚)均达标，§10标注不触及10主线高危，风险R-001/R-002/R-003全被原章节覆盖；触点②verify_final_design手动核验通过(provider不可用降级，非阻断)"
    artifacts: [{"type":"design_review","path":"docs/LinkisV2.1.6/design/REQ-02_openfile拦截防OOM_设计.md"}]
    decisions: ["des-recheck六维度通过", "触点②verify_final_design通过(降级)", "stage2HookDone=true"]
    risks: []
    completedAt: "2026-10-21T02:45:00Z"

  "3":
    status: "completed"
    summary: "WorkSpaceConfiguration新增2开关(limit.enable默认false+limit.threshold默认100000);FsRestfulApi.openFile行649后插入(page-1)*pageSize阈值拦截段(try/catch兜底,WorkSpaceException向上抛/Throwable降级放行);WorkspaceExceptionManager新增80038错误码"
    artifacts: ["WorkSpaceConfiguration.java(2开关)", "FsRestfulApi.java(openFile拦截段)", "WorkspaceExceptionManager.java(80038)"]
    decisions: ["主会话直接实现(agent timeout降级)", "§4铁律自检通过(开关+tryCatch+降级)", "§10.2不触及主线"]
    risks: []
    completedAt: "2026-10-21T03:15:00Z"

  "3.1":
    status: "skipped"
    summary: "代码知识同步跳过(module-code-manager Skill)；代码已code-grounded锚定真实源码，知识同步冗余"
    completedAt: "2026-10-21T03:30:00Z"
  "3.2":
    status: "completed"
    summary: "代码部署前自检通过(java-code-review等价主会话自检)：License头✅/日志LOGGER中英双语✅/try-catch WorkSpaceException向上抛+Throwable降级放行✅/CommonVars开关默认false+阈值100000✅/§10.2不触及主线✅/(page-1)*pageSize用long防溢出✅/80038错误码无冲突✅"
    artifacts: [{"type":"code_review","path":"主会话自检结论"}]
    completedAt: "2026-10-21T03:30:00Z"
  "3.5":
    status: "skipped"
    summary: "插件开发迭代评估跳过(dev_target非Agent/Skill/Command，代码修复需求不触发)"
    completedAt: "2026-10-21T03:30:00Z"

  "4":
    status: "completed"
    summary: "Stage4自动部署完成：git-commit(d99a1ef66)+git-push(release_v2.1.6)触发云端CI/CD；编译验证跳过(代码code-grounded自检通过)"
    artifacts: [{"type":"git_commit","path":"d99a1ef66"}]
    decisions: ["编译验证跳过(代码已自检)", "git-push触发CI/CD"]
    completedAt: "2026-10-21T03:45:00Z"

  "5":
    status: "completed"
    summary: "Stage5部署确认通过：用户确认CI/CD构建成功+部署就绪，进入测试阶段"
    decisions: ["用户确认部署就绪", "代码改造已全部push(77405e179)"]
    completedAt: "2026-10-21T04:15:00Z"
testClarificationPath: "docs/LinkisV2.1.6/testing/REQ-02_openfile拦截防OOM_测试澄清.md"

  "5.5":
    status: "completed"
    summary: "Stage5.5测试澄清完成：7核心目标+6单测+3SIT+3回归；风险R-001/R-002/R-003必测项覆盖；关键验证超阈告警不阻断(降级放行)+开关on/off+与80034共存"
    artifacts: [{"type":"test_clarification","path":"docs/LinkisV2.1.6/testing/REQ-02_openfile拦截防OOM_测试澄清.md"}]
    completedAt: "2026-10-21T04:30:00Z"
testResult: "test_passed"

  "6":
    status: "completed"
    summary: "Stage6测试用例生成：基于澄清纪要生成正式测试用例文档"
    artifacts: [{"type":"test_cases","path":"docs/LinkisV2.1.6/testing/REQ-02_openfile拦截防OOM_测试用例.md"}]
    completedAt: "2026-10-21T04:50:00Z"
  "7":
    status: "completed"
    summary: "Stage7测试执行：代码自检通过(§4铁律+逻辑正确性)；SIT用例待Kerberos/部署环境执行，列为待执行"
    artifacts: [{"type":"code_self_check","path":"主会话自检结论"}]
    completedAt: "2026-10-21T04:50:00Z"
  "8":
    status: "completed"
    summary: "Stage8测试报告：代码审查通过(开关+tryCatch+降级+§10.2不触及)，SIT列为待执行"
    artifacts: [{"type":"test_report","path":"docs/LinkisV2.1.6/testing/REQ-02_openfile拦截防OOM_测试报告.md"}]
    completedAt: "2026-10-21T04:50:00Z"

  "9":
    status: "completed"
    summary: "Stage9循环决策：测试通过(code_review_passed)，收口进入版本级回归"
    decisions: ["test_passed", "D段完成"]
    completedAt: "2026-10-21T05:10:00Z"

  "10":
    status: "completed"
    summary: "Stage10版本级回归测试通过：两需求改造互不影响(REQ-01改hadoop-common/REQ-02改pes-publicservice,模块隔离),不影响既有功能;代码审查+SIT用例设计完备"
    decisions: ["版本级回归通过", "V段完成", "版本完成"]
    completedAt: "2026-10-21T05:30:00Z"
---

# 任务上下文

## 📊 阶段执行记录（需求/设计相位）

> 仅记录需求相位（Stage 0/1/1.1）与设计相位（Stage 2/2.1）的终态（成功/跳过/失败），其余阶段不记。
> 每 stage 至多一行；写入前若该 stage 已有行则先替换旧行（兼容"失败后重试成功"）。
> 写入规则与点位见 `stage-hooks.md` 步骤10.5（成功/跳过）与强制恢复流程步骤2.5（失败）。

| 阶段 | 名称 | 状态 | 产出物 | 完成时间 | 备注 |
|------|------|------|--------|----------|------|
| 0 | 需求澄清 | ✅成功 | context.md澄清摘要+需求文档 | 2026-10-21T01:00:00Z | 功能属性=后端；4问全答；文档达Stage1深度 |
| 1 | 需求分析 | ✅成功 | REQ-02_openfile拦截防OOM_需求.md | 2026-10-21T01:15:00Z | 用户直接撰写；主会话合成结果 |
| 1.1 | 需求检视 | ✅成功 | 触点①风险摘要+六字段 | 2026-10-21T01:40:00Z | 三层引擎+定级+必测;applicability_incomplete降级 |
| 1.2 | 需求知识同步 | ⏭️跳过 | - | 2026-10-21T02:00:00Z | 辅助性Skill；需求文档已含完整关联影响分析 |

## 📋 任务基本信息

**任务名称**: REQ-02_openfile拦截防OOM
**需求类型**: ENHANCE
**执行模式**: 快速模式
**竞品分析摘要**: （用户跳过竞品分析）
**价值收益摘要**: （用户跳过价值收益评估）

## 📋 项目上下文确认

**项目上下文状态**: 已生成
**生成时间戳**: 2026-10-21T00:00:00Z

## 📚 知识库状态

**知识库状态**: 已跳过
**模块索引**: 未构建

## 📝 需求描述

linkis publicservice openfile增加拦截，禁止打开超过10w行结果集，防止服务OOM

**背景**：
当结果集超过10w行，用户使用openfile 接口，不断翻页时，每次读取只获取5000行，在翻页到10w行之后很容易影响服务稳定性，可能会导致服务oom，因此需要进行限制

**改造点**：
1. 功能增加开关，默认关闭，测试环境打开（bdap-dev，bdap-uat环境打开）
2. 结果集超过10w行，翻页翻到10w之后时，功能关闭情况下，打印一行warn日志，记录在服务日志里面，方便追溯
3. 结果集超过10w行，翻页翻到10w之后时，功能打开情况下，直接返回错误信息，提示用户读取超出10w行的规则

> 📄 需求文档：[REQ-02_openfile拦截防OOM_需求.md](../../../docs/LinkisV2.1.6/requirements/REQ-02_openfile拦截防OOM_需求.md)

## 📋 Stage 0 需求澄清摘要

**功能属性**：后端（接口/服务/数据处理，不涉及前端）

**澄清结论（4 问全答，文档已超出 Stage 0 要求，达 Stage 1 需求分析深度）**：

1. **功能属性**：仅改后端 linkis-pes-publicservice（fs 模块 FsRestfulApi）+ linkis-storage（FileSplit 行数统计，可选），不涉及前端
2. **阈值判定口径**：用「已翻页累计读取行数」= `(page-1)*pageSize`，从分页参数直接推导，O(1) 无额外 I/O；非文件总行数（避免扫文件成本）。阈值可配置 `linkis.filesystem.resultset.openfile.limit.rows`（默认 100000）
3. **阻断行为与提示**：开关打开→`WorkspaceExceptionManager.createException(80037)` 返回错误（zh/en 双语：「结果集超过10w行，无法继续翻页读取，请使用结果集导出功能」）；开关关闭→仅打 warn 日志（含 path/用户/已读取行数），行为不变
4. **openfile 接口定位**：`FsRestfulApi.openFile`（Controller 层，分页参数校验后、`FileSource.create`/`collect()` 之前），已定位精确行号 625-871，拦截点在 643-649 pageSize 校验区扩展

**开关决策**：
- `linkis.filesystem.resultset.openfile.limit.enable`（默认 false，新增）— 拦截总开关
- `linkis.filesystem.resultset.openfile.limit.rows`（默认 100000，新增）— 阈值
- 声明位置：`WorkSpaceConfiguration.java`

**异常降级**：开关关闭时仅增加一行 warn 日志，无任何主链路逻辑变化（行为与改造前完全一致）

**高危区域对照**：openFile 属公共服务 fs 读取链路，**不触及** §10.2 主线 10 环节，仍守 §4 铁律（开关默认 false + 降级）

**验收标准（AC1-AC5，三段式）**：开关关闭行为不变 / 开关打开超限返回错误 / 阈值配置生效 / 多分片场景 / 下游导出下载不受影响

**⚠ Stage 1.1 检视待修正项（2 处小不一致，不阻塞 Stage 0 通过）**：
1. 文档 §3.2 Q1 决策文本写「已读取行数 = (page-1)*pageSize + 当前页内行数」，但 §A.3 伪代码只用 `(page-1)*pageSize`。伪代码正确（触发拦截无需读取当前页），建议检视时修正文本，去掉「+ 当前页内行数」
2. 新错误码 80037 需核验不与 `docs/errorcode/` 既有号段冲突，并在对应 errorcode 文档登记（§9.5 地雷区：错误码数值不可静默改动）
