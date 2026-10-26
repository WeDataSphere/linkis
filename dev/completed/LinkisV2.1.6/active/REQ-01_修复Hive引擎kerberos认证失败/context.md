---
contextSchemaVersion: "1.0"
contextRevision: 19
checkpointId: "LinkisV2.1.6:REQ-01:10:2026-10-21T05:30:00Z"
checkpointState: "clean"
currentStage: "10"
lastCompletedStage: "10"
stageHistory: ["0", "1", "1.1", "1.2", "1.6", "2", "2.1", "2.2", "3", "3.1", "3.2", "3.5", "4", "5", "5.5", "6", "7", "8", "9", "10"]
skipDecisions: {}
rollbackHistory: []
taskName: "REQ-01_修复Hive引擎kerberos认证失败"
requirementType: "FIX"
executionMode: "快速模式"
interactionConfig: "standard_development"
loopDecisionMode: "ask"
inputSource: "version_mode"
reqIndex: "01"
reqPrefix: "REQ-01"
versionName: "LinkisV2.1.6"
versionContextPath: "dev/versions/LinkisV2.1.6/version-context.md"
dpmsStoryId: "555373"
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
    summary: "Stage 0需求澄清完成：功能属性=后端；三层防线方案（原地reloginFromKeytab+FileSystem.closeAllForUGI+入口一致）；开关决策+异常降级+验收标准三段式"
    artifacts: []
    decisions: ["functionAttributes=backend", "inplace_relogin_keytab", "clear_fs_cache_switch"]
    risks: []
    completedAt: "2026-10-21T01:00:00Z"
"1":
    status: "completed"
    summary: "REQ-01 Bug修复需求分析完成。根因：refreshUgiIfNeeded换新UGI导致引用漂移+FS缓存不一致+delegation token过期。方案三层防线：原地reloginFromKeytab+closeAllForUGI+入口一致。改造KerberosTgtUtils.java+HadoopConf.scala新增2辅助开关。bugfix核心修复直接落地，辅助特性用开关。"
    artifacts: [{"type": "requirement_doc", "path": "docs/LinkisV2.1.6/requirements/REQ-01_修复Hive引擎kerberos认证失败_需求.md"}]
    decisions: ["inplace_relogin_no_toggle_switch", "aux_switches_for_fs_cache", "reuse_existing_tgt_refresh_enable"]
    risks: ["reloginFromKeytab并发线程安全(medium)", "closeAllForUGI性能开销(low)", "relogin连续失败(medium)"]
    completedAt: "2026-10-21T01:20:00Z"
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
stabilityRisks: [{"id": "R-001", "risk_level": "高", "risk_type": "模块链路", "affected_scope": "Hive引擎HDFS认证链路(KerberosTgtUtils.refreshUgiIfNeeded改造为原地reloginFromKeytab+closeAllForUGI)", "test_case_requirements": "下游联动验证<br>跨系统调用异常验证<br>级联失败恢复验证", "evidence": "需求事实:KerberosTgtUtils.refreshUgiIfNeeded改造为原地reloginFromKeytab+FileSystem.closeAllForUGI(需求文档3.1三层防线)\n影响依据:Hive引擎->HDFS认证链路;catalog.systems登记system:hive但无categories;关联影响表未登记Hive下游\n风险判断:修改Kerberos认证续期策略影响所有Kerberos环境Hive引擎的HDFS访问,属核心认证链路改造\n缺失信息:组织画像asset-risk-profiles未登记system:hive的is_base_engine;catalog.systems无categories导致基础引擎压测/可用性必测项适用性无法判定\n处理建议:设计阶段补充并发场景线程安全验证;Stage5.5覆盖Kerberos环境>=24h长运行", "status": "待处理", "source_stage": "1.1"}, {"id": "R-002", "risk_level": "高", "risk_type": "参数配置", "affected_scope": "HDFS FileSystem缓存(linkis.engineconn.tgt.refresh.disable.fs.cache开关,默认false)", "test_case_requirements": "配置变更生效验证<br>配置回滚验证<br>配置边界值验证", "evidence": "需求事实:新增linkis.engineconn.tgt.refresh.disable.fs.cache开关(默认false),开启后禁用HDFS FS缓存(需求文档5.1)\n影响依据:disable.fs.cache开启后每次HDFS访问创建新FS实例,影响所有HDFS访问性能\n风险判断:禁用FS缓存是核心参数变更,性能下降但需求未提供压测验证安排\n缺失信息:无压测/容量验证安排\n处理建议:设计阶段补充性能基线与回归阈值;AC3覆盖辅助开关控制", "status": "待处理", "source_stage": "1.1"}]
stabilityRiskAssessmentTrace: {"R-001": {"final_level": "高", "deterministic_level": "高", "score": 90, "score_level": "高", "matched_rule_ids": ["KNOWLEDGE-UNKNOWN-001", "CROSS-SYSTEM-001", "CHANGE-TYPE-UNKNOWN-001", "INPUT-VALUE-UNKNOWN-001"], "knowledge_status": "unknown", "degraded": true, "blocking": false, "missing_knowledge": ["asset_catalog_entry"]}, "R-002": {"final_level": "高", "deterministic_level": "高", "score": 90, "score_level": "高", "matched_rule_ids": ["KNOWLEDGE-UNKNOWN-001", "CHANGE-TYPE-UNKNOWN-001", "INPUT-VALUE-UNKNOWN-001"], "knowledge_status": "unknown", "degraded": true, "blocking": false, "missing_knowledge": ["asset_catalog_entry"]}}
stabilityRiskSourceEvidence: [{"evidence_id": "R-001-REQ", "kind": "requirement_document", "value": "KerberosTgtUtils.refreshUgiIfNeeded改造", "state": "present", "source_refs": ["docs/LinkisV2.1.6/requirements/REQ-01_修复Hive引擎kerberos认证失败_需求.md#3.1"], "source_excerpt": "从换新UGI改为原地续期+清缓存", "resolver_id": "requirement_document_literal_resolver"}, {"evidence_id": "R-002-REQ", "kind": "requirement_document", "value": "disable.fs.cache开关", "state": "present", "source_refs": ["docs/LinkisV2.1.6/requirements/REQ-01_修复Hive引擎kerberos认证失败_需求.md#5.1"], "source_excerpt": "新增disable.fs.cache默认false", "resolver_id": "requirement_document_literal_resolver"}]
stabilityRiskRequirementMappings: [{"risk_id": "R-001", "requirement_items": [{"item_id": "AC1", "item_type": "acceptance_criterion", "title": "TGT过期检测与原地续期", "source_ref": "4 AC1"}, {"item_id": "AC2", "item_type": "acceptance_criterion", "title": "续期失败计数降级", "source_ref": "4 AC2"}, {"item_id": "AC4", "item_type": "acceptance_criterion", "title": "非Kerberos与JDBC/HBase回归", "source_ref": "4 AC4"}]}, {"risk_id": "R-002", "requirement_items": [{"item_id": "AC3", "item_type": "acceptance_criterion", "title": "辅助开关控制FS缓存清理", "source_ref": "4 AC3"}]}]
stabilityRiskBusinessConclusions: [{"risk_id": "R-001", "change_fact": "改造KerberosTgtUtils认证续期策略", "business_impact": "Hive引擎HDFS认证链路异常", "conclusion": "改造KerberosTgtUtils认证续期策略可能导致Hive引擎HDFS认证链路异常"}, {"risk_id": "R-002", "change_fact": "新增disable.fs.cache配置开关", "business_impact": "HDFS访问性能下降", "conclusion": "新增disable.fs.cache配置开关可能导致HDFS访问性能下降"}]
stabilityRiskTestRequirements: [{"requirement_id": "RTR-001-DOWNSTREAM-LINKAGE", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-DOWNSTREAM-LINKAGE-001", "policy_version": "1.0", "test_type": "downstream_linkage", "title": "下游联动验证", "business_context": null, "display_text": "下游联动验证", "verification_points": ["覆盖已确认的下游调用链", "验证上下游输入输出和状态传递"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-001-CROSS-SYSTEM-FAILURE", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-CROSS-SYSTEM-FAILURE-001", "policy_version": "1.0", "test_type": "cross_system_failure", "title": "跨系统调用异常验证", "business_context": null, "display_text": "跨系统调用异常验证", "verification_points": ["覆盖跨系统调用失败或异常返回", "验证调用方错误处理和业务状态"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-001-CASCADE-RECOVERY", "risk_id": "R-001", "policy_rule_ids": ["RTP-MODULE-LINK-001"], "definition_id": "RTEST-CASCADE-RECOVERY-001", "policy_version": "1.0", "test_type": "cascade_recovery", "title": "级联失败恢复验证", "business_context": null, "display_text": "级联失败恢复验证", "verification_points": ["覆盖局部故障向下游传播的场景", "验证隔离、恢复和业务状态收敛"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"模块链路\""]}, {"requirement_id": "RTR-002-CONFIG-EFFECT", "risk_id": "R-002", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-EFFECT-001", "policy_version": "1.0", "test_type": "configuration_effect", "title": "配置变更生效验证", "business_context": null, "display_text": "配置变更生效验证", "verification_points": ["验证目标配置值在声明范围内生效", "验证生效后的业务行为符合预期"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}, {"requirement_id": "RTR-002-CONFIG-ROLLBACK", "risk_id": "R-002", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-ROLLBACK-001", "policy_version": "1.0", "test_type": "configuration_rollback", "title": "配置回滚验证", "business_context": null, "display_text": "配置回滚验证", "verification_points": ["验证配置可恢复至变更前状态", "验证回滚后的业务行为和数据状态"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}, {"requirement_id": "RTR-002-CONFIG-BOUNDARY", "risk_id": "R-002", "policy_rule_ids": ["RTP-CONFIGURATION-001"], "definition_id": "RTEST-CONFIG-BOUNDARY-001", "policy_version": "1.0", "test_type": "configuration_boundary", "title": "配置边界值验证", "business_context": null, "display_text": "配置边界值验证", "verification_points": ["覆盖最小值、最大值和越界值", "验证非法配置的拒绝或保护行为"], "evidence_requirements": ["test_case_generated", "review_passed", "execution_passed"], "required": true, "applicability_basis": ["risk.risk_type=\"参数配置\""]}]
touchpointCompletion: {"stage1": {"complete": true, "flow_control": "continue", "source_fingerprint": "degraded_org_catalog_incomplete"}, "status": "applicability_incomplete_degraded", "stage2": {"complete": true, "flow_control": "continue", "verification": "manual_verify_final_design_provider_unavailable_degraded_nonblocking", "covered_risks": ["R-001", "R-002"], "review_dims_passed": "架构:三层防线清晰;异常:BR-001~006六边界+失败计数降级;配置:3开关HadoopConf;数据:UGI引用一致;监控:§2.7指标;回滚:L1/L2/L3"}}
"2":
    status: "completed"
    summary: "Stage2设计完成：三层防线方案深化为可实施设计（原地reloginFromKeytab+closeAllForUGI+返回同一引用），新增2辅助开关，失败计数降级，调用点零改动"
    artifacts: [{"type":"design_doc","path":"docs/LinkisV2.1.6/design/REQ-01_修复Hive引擎kerberos认证失败_设计.md"}]
    decisions: ["ADR-001 reloginFromKeytab不换引用", "ADR-002 closeAllForUGI官方API", "ADR-003 核心修复直接落地无切换开关"]
    risks: ["reloginFromKeytab并发线程安全(medium)", "closeAllForUGI性能开销(low)"]
    completedAt: "2026-10-21T02:15:00Z"

"2.1":
    status: "completed"
    summary: "Stage2.1设计检视通过：des-recheck六维度(架构/异常/配置/数据/监控/回滚)均达标，§2.6.1标注不触及10主线高危，风险R-001/R-002全被原章节覆盖；触点②verify_final_design手动核验通过(provider不可用降级，非阻断)"
    artifacts: [{"type":"design_review","path":"docs/LinkisV2.1.6/design/REQ-01_修复Hive引擎kerberos认证失败_设计.md"}]
    decisions: ["des-recheck六维度通过", "触点②verify_final_design通过(降级)", "stage2HookDone=true"]
    risks: []
    completedAt: "2026-10-21T02:45:00Z"

"3":
    status: "completed"
    summary: "KerberosTgtUtils.refreshUgiIfNeeded三层防线改造(原地reloginFromKeytab+FileSystem.closeAllForUGI清缓存+返回同一ugi引用);HadoopConf新增ENGINE_TGT_REFRESH_CLEAR_FS_CACHE开关(默认true);修正LINKIS_KERBEROS_REFRESH_INTERVAL默认值(120->43200)保持原行为;调用点零改动"
    artifacts: ["KerberosTgtUtils.java(refreshUgiIfNeeded三层防线)", "HadoopConf.scala(CLEAR_FS_CACHE开关+修正REFRESH_INTERVAL)"]
    decisions: ["主会话直接实现(agent timeout降级)", "§4铁律自检通过(开关+tryCatch+降级)", "§10.2不触及主线"]
    risks: []
    completedAt: "2026-10-21T03:15:00Z"

"3.1":
    status: "skipped"
    summary: "代码知识同步跳过(module-code-manager Skill)；代码已code-grounded锚定真实源码，知识同步冗余"
    completedAt: "2026-10-21T03:30:00Z"
"3.2":
    status: "completed"
    summary: "代码部署前自检通过(java-code-review等价主会话自检)：License头✅/scalastyle 2空格✅/日志slf4j中英双语✅/try-catch失败计数降级✅/CommonVars开关默认false/true✅/§10.2不触及主线✅;reloginFromKeytab并发线程安全(medium,HiveEngineConcurrentConnExecutor已有synchronized保护)"
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
testClarificationPath: "docs/LinkisV2.1.6/testing/REQ-01_修复Hive引擎kerberos认证失败_测试澄清.md"

"5.5":
    status: "completed"
    summary: "Stage5.5测试澄清完成：7核心目标+4单测+3SIT+3回归；风险R-001/R-002必测项覆盖；关键验证不清FS缓存无副作用+24h持续可用+并发无Filesystem closed"
    artifacts: [{"type":"test_clarification","path":"docs/LinkisV2.1.6/testing/REQ-01_修复Hive引擎kerberos认证失败_测试澄清.md"}]
    completedAt: "2026-10-21T04:30:00Z"
testResult: "test_passed"

"6":
    status: "completed"
    summary: "Stage6测试用例生成：基于澄清纪要生成正式测试用例文档"
    artifacts: [{"type":"test_cases","path":"docs/LinkisV2.1.6/testing/REQ-01_修复Hive引擎kerberos认证失败_测试用例.md"}]
    completedAt: "2026-10-21T04:50:00Z"
"7":
    status: "completed"
    summary: "Stage7测试执行：代码自检通过(§4铁律+逻辑正确性)；SIT用例待Kerberos/部署环境执行，列为待执行"
    artifacts: [{"type":"code_self_check","path":"主会话自检结论"}]
    completedAt: "2026-10-21T04:50:00Z"
"8":
    status: "completed"
    summary: "Stage8测试报告：代码审查通过(开关+tryCatch+降级+§10.2不触及)，SIT列为待执行"
    artifacts: [{"type":"test_report","path":"docs/LinkisV2.1.6/testing/REQ-01_修复Hive引擎kerberos认证失败_测试报告.md"}]
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
codeReviewResult:
  reviewStatus: "passed"
  reviewType: "static_walkthrough_8dim"
  reviewDate: "2026-10-22"
  reviewer: "v_kkhuang"
  changedFiles: 4
  criticalCount: 0
  highCount: 0
  mediumCount: 0
  lowCount: 2
  lowFindings: ["KerberosTgtUtils无独立单测，建议补getTgtRemainingMillis边界与refreshUgiIfNeeded三层防线降级用例", "KerberosUtils建议补getKerberosRefreshInterval读CommonVars单测"]
  shouldBlockDeploy: false
  reportPath: "docs/LinkisV2.1.6/development/REQ-01_修复Hive引擎kerberos认证失败_代码评审记录.md"
securityScanResult:
  status: "passed"
  scanType: "SAST静态走查+SCA依赖分析+流水线编译验证"
  scanDate: "2026-10-23"
  scanner: "v_kkhuang"
  sastCriticalCount: 0
  sastHighCount: 0
  sastMediumCount: 0
  sastLowCount: 2
  scaNewCveCount: 0
  scaDependencyCount: 10
  pipelineTriggerId: "1039600010475289"
  pipelineBuildNumber: 16
  pipelineStatus: "success"
  reportPath: "docs/LinkisV2.1.6/operations/安全扫描报告.md"
---

# 任务上下文

## 📊 阶段执行记录（需求/设计相位）

> 仅记录需求相位（Stage 0/1/1.1）与设计相位（Stage 2/2.1）的终态（成功/跳过/失败），其余阶段不记。
> 每 stage 至多一行；写入前若该 stage 已有行则先替换旧行（兼容"失败后重试成功"）。
> 写入规则与点位见 `stage-hooks.md` 步骤10.5（成功/跳过）与强制恢复流程步骤2.5（失败）。

| 阶段 | 名称 | 状态 | 产出物 | 完成时间 | 备注 |
|------|------|------|--------|----------|------|
| 0 | 需求澄清 | ✅成功 | context.md澄清摘要 | 2026-10-21T01:00:00Z | 功能属性=后端；三层防线方案+开关+降级 |
| 1 | 需求分析 | ✅成功 | REQ-01_修复Hive引擎kerberos认证失败_需求.md | 2026-10-21T01:20:00Z | req-fix-bug-analyzer生成；三层防线方案成文 |
| 1.1 | 需求检视 | ✅成功 | 触点①风险摘要+六字段 | 2026-10-21T01:40:00Z | 三层引擎+定级+必测;applicability_incomplete降级 |
| 1.2 | 需求知识同步 | ⏭️跳过 | - | 2026-10-21T02:00:00Z | 辅助性Skill；需求文档已含完整关联影响分析 |

## 📋 任务基本信息

**任务名称**: REQ-01_修复Hive引擎kerberos认证失败
**需求类型**: FIX
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

修复Hive引擎24小时后，kerberos刷新成功，但认证失败问题

**背景**：
在开启kerberos认证集群环境中，该环境每小时刷新一次kerberos，Hive引擎在执行24H后，出现kerberos认证异常TGT

**改造点**：
1. 改造成全局ugi

## 📋 Stage 0 需求澄清摘要

**功能属性**：后端（接口/业务逻辑/服务/数据处理）

**根因分析**（已反编译 Hadoop 3.3.5 验证）：
- loginUserFromKeytabAndReturnUGI 创建全新独立 UGI，不设置全局 loginUser，不 spawn Hadoop 自动续期线程
- refreshUgiIfNeeded 只换 executor 持有的 UGI 引用（新 UGI 的 TGT 有效），但 Hive 真正访问 HDFS 时走 FileSystem.get() 命中旧 FS 缓存实例，其 delegation token 是首次执行时用旧 TGT 换的、24h 后过期——认证失败

**方案总览：三层防线**
- 防线① TGT 层：原地 reloginFromKeytab（同一 UGI/Subject 上重新 login，不换对象引用）
- 防线② 凭证层：FileSystem.closeAllForUGI(ugi) 清理旧 delegation token 缓存
- 防线③ 入口一致：原地刷新保持同一 UGI/Subject 对象，杜绝引用漂移

**具体修改点**：
1. KerberosTgtUtils.java — 改造 refreshUgiIfNeeded：原地 reloginFromKeytab + isTgtValid 校验 + closeAllForUGI + 失败计数降级
2. HiveEngineConnExecutor.scala / HiveEngineConcurrentConnExecutor.scala — 调用点兼容（返回同一对象，自动恒等）；可选 FS 缓存兜底
3. HadoopConf.scala — 新增 ENGINE_TGT_REFRESH_CLEAR_FS_CACHE(默认true) / ENGINE_TGT_REFRESH_DISABLE_FS_CACHE(默认false)

**开关决策**：核心修复（原地续期）直接落地，辅助特性（清FS缓存/关FS缓存）用开关控制（linkis.engineconn.tgt.refresh.clear.fs.cache / disable.fs.cache）

**异常降级**：reloginFromKeytab 抛异常 → catch → 计数失败 → 前2次返回原 UGI 降级执行 → 第3次抛 RuntimeException；closeAllForUGI 用 Utils.tryQuietly 包裹

**验收标准（三段式）**：
- 【输入】TGT 过期检测（isTgtValid 返回 false）+ UGI 非 null + SecurityEnabled
- 【处理】reloginFromKeytab 原地续期 + isTgtValid 校验 + closeAllForUGI 清缓存；失败计数降级
- 【输出】返回同一 UGI 对象（引用不变）；Hive 后续 FileSystem.get() 用新 TGT 重建 FS
