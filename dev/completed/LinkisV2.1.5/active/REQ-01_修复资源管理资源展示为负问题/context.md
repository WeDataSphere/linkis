---
contextSchemaVersion: "1.0"
contextRevision: 22
checkpointId: "LinkisV2.1.5:REQ-01:9:2026-10-19T09:30:00Z"
checkpointState: "clean"
currentStage: "9"
lastCompletedStage: "9"
testResult: "test_passed"
cycleDecision: "proceed_to_stage_10"
sdlResult:
  status: "not_applicable"
  conclusion: "需求内容无效——DPMS 判定需求内容过短或过于简单，无法识别并生成安全需求，自动标记为不执行 SDL"
  source: "change_order_silent_refresh"
  updatedAt: "2026-10-19T10:45:00Z"
stageHistory: ["0", "1", "1.1", "2", "2.1", "2.2", "3", "4", "5", "5.5", "6", "6.1", "7", "8", "9"]
segmentProgress:
  P1:
    status: "completed"
  S1:
    status: "completed"
  P2:
    status: "completed"
  S2:
    status: "completed"
  D:
    status: "completed"
  V:
    status: "pending"
skipDecisions: {"2.2": "executed", "6.1": "skipped_no_regression_sync_artifact_recovered"}
rollbackHistory: []
taskName: "REQ-01_修复资源管理资源展示为负问题"
requirementType: "OPTIMIZE"
executionMode: "快速模式"
interactionConfig: "standard_development"
loopDecisionMode: "ask"
inputSource: "version_mode"
reqIndex: "01"
reqPrefix: "REQ-01"
versionName: "LinkisV2.1.5"
versionContextPath: "dev/completed/LinkisV2.1.5/version-context.md"
dpmsStoryId: "552808"
testSetId: "322336"
regressionTestSetId: "322337"
releasePlanId: "181593"
productId: "100484"
productName: "BDP_Linkis"
subsystemId: "5435"
subsystemName: "WDSL-UJES"
subsystemVersionId: "2.1.5"
branchName: "release_v2.1.5"
pipelineId: null
pipelineVersion: null
reqSubsystemId: "5435"
repositoryId: "git@code.weoa.com:Internal/incubator-linkis.git"
functionAttributes: "后端,前端"
frontendType: "前端+后台web API"
selectedDevAgents: ["java-code-developer", "frontend-code-developer"]
knowledgeBaseStatus: "built"
moduleIndexPath: "docs/project-knowledge/module-index.json"
stageOutputs:
  stage2:
    designDocumentPath: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    designDocumentSha256: "123f610eb6e53a939ecbd78a8e68934767fc7f5923264050b771b027b260228f"
    generatedAt: "2026-10-12T10:30:00Z"
    agentType: "des-optimize"
    wordExportMode: "deferred"
  stage2_1:
    designDocumentPath: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    designDocumentSha256: "123f610eb6e53a939ecbd78a8e68934767fc7f5923264050b771b027b260228f"
    reviewedAt: "2026-10-12T11:00:00Z"
    recheckScore: 82
    optimizationsApplied: 6
    qualityVerdict: "符合标准"
    postReviewCorrections: "用户反馈修正：O1接口定位修正为EMRestfulApi.java:593（非RMMonitorRest.scala）；O4前端按钮设计修正为原重置按钮改名归零+新增重置按钮调用reset-resource传入creator"
    codeSyncUpdate: "2026-10-14T20:38:33Z 按实际代码同步：第1批需求变更后范围收敛为O3+OC1，清除文档中O1/O2/O4/reset-resource/creator/username空指针/前端按钮等已回退内容；3.2.3 normalizeNegativeResource签名修正为返回util.Map[String,Any]（链式调用）；3.1.7 O3伪代码补充RM_NEGATIVE_RESOURCE_DISCARD_ENABLE开关读取、usedResource空值保护、tryLockOneLabel降级(waitForDealResourceLabels)等实际实现细节；第4-12章异常/回滚/兼容/降级/监控/数据/效果/风险响应统一改写为OC1语义"
  stage3:
    devMode: "direct"
    completedAt: "2026-10-14T10:30:00Z"
    optimizationPoints:
      O3:
        description: "引擎退出消息负值丢弃，新增配置开关linkis.manager.engine.exit.negative.resource.discard.enable(默认true)"
        files:
          - "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/service/impl/DefaultResourceManager.scala"
          - "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/utils/RMUtils.scala"
      OC1:
        description: "资源管理界面查询(allUserResource)负数归零：若查询回来的已使用资源存在负数强制改为0，剩余资源存在负数也改为0"
        files:
          - "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/utils/RMUtils.scala"
          - "linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/scala/org/apache/linkis/manager/rm/restful/RMMonitorRest.scala"
  stage4:
    commitOnly: true
    commitHash: "b8df1c64"
    branchName: "release_v2.1.5"
    completedAt: "2026-10-14T11:55:00Z"
    filesCommitted: 17
    originalCommitHash: "20a5a296b"
    originalBranch: "release_v2.1.5-bak"
    branchSwitchNote: "2026-10-14 11:55 外部将2.1.5发布基线由webank线切换为hadoop3线(原release_v2.1.5改名release_v2.1.5-bak保留,dev-2.1.4-hadoop3改名release_v2.1.5)。已通过3-way merge(merge-tree,无冲突)将REQ-01提交重放至新基线,生成commit b8df1c64并快进推送到远程release_v2.1.5。原提交20a5a296b完整保留于-bak分支。"
  "5":
      "status": "completed",
      "summary": "部署确认完成：代码已推送至远程 release_v2.1.5（commit b8df1c64），用户确认 CI/CD 部署成功。pipelineId 为空，无法自动校验流水线状态，按用户判断信任继续。",
      "artifacts": [],
      "decisions": [
        "deploy_confirmed_by_user",
        "pipeline_id_absent_trust_user"
      ],
      "risks": [],
      "contextUpdates": {
        "deploymentStatus": "deployed",
        "deploymentDecision": "confirmed_proceed"
      },
      "completedAt": "2026-10-16T10:18:30Z"
  "5.5":
      "status": "completed",
      "summary": "recovered_from_artifacts：测试用例澄清已在前序会话完成——三维机制（O3/OC1/OC1增强）的测试范围、环境前置与管理员鉴权要求已落入 Feature 文件与测试用例文档 1.2/1.3 节；未单独落盘澄清纪要文件，按实际产物反推确认澄清结论有效",
      "artifacts": [
        "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/features/REQ-01_修复资源管理资源展示为负问题.feature"
      ],
      "decisions": [
        "recovered_from_artifacts",
        "no_separate_minutes_scope_embedded_in_feature_and_testcase_docs"
      ],
      "risks": [
        "5.5澄清纪要未单独落盘，测试范围以Feature文件与测试用例文档为准"
      ],
      "contextUpdates": {},
      "completedAt": "2026-10-19T03:29:10Z"
  "6":
      "status": "completed",
      "summary": "recovered_from_artifacts：测试用例文档已生成（TC001-TC019，覆盖 O3/OC1/OC1增强 三维机制 12 个 Scenario，标注单测/远程API/开关翻转三类执行方式）",
      "artifacts": [
        "docs/LinkisV2.1.5/testing/REQ-01_修复资源管理资源展示为负问题_测试用例.md",
        "docs/LinkisV2.1.5/testing/wemind/REQ-01_修复资源管理资源展示为负问题_wemind导入.json"
      ],
      "decisions": [
        "recovered_from_artifacts",
        "test_case_doc_and_wemind_import_recovered"
      ],
      "risks": [],
      "contextUpdates": {
        "testCaseArtifacts": ["docs/LinkisV2.1.5/testing/REQ-01_修复资源管理资源展示为负问题_测试用例.md"],
        "featureArtifacts": ["dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/features/REQ-01_修复资源管理资源展示为负问题.feature"]
      },
      "completedAt": "2026-10-19T03:29:30Z"
  "6.1":
      "status": "skipped",
      "summary": "recovered_from_artifacts：回归测试同步（module-testing-manager）无产物，按可跳过阶段处理，恢复期不补执行",
      "artifacts": [],
      "decisions": [
        "recovered_from_artifacts",
        "skipped_no_regression_sync_artifact"
      ],
      "risks": [],
      "contextUpdates": {},
      "completedAt": "2026-10-19T03:29:50Z"
  "7":
      "status": "completed",
      "summary": "recovered_from_artifacts：测试执行已完成——单测 13/13 通过（RMUtilsTest，surefire 真实执行，含 BUG 修复后新增 2 个嵌套回归单测），UAT 已部署环境远程 API 5/5 通过（TC013a/b、TC014、TC015、TC016，hadoop 管理员鉴权），allUserResource 多页 0 负值、56/56 字段 left=max-used 精确匹配，UAT 复测 leftResource 嵌套结构确认 pass；TC002/TC003/TC005/TC006（O3/OC1/OC1增强 开关关闭态回退）未执行，整体 event_type=not_executed（如实记录，不虚报 test_passed）",
      "artifacts": [
        "docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md",
        "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/test-status.json"
      ],
      "decisions": [
        "recovered_from_artifacts",
        "unit_and_remote_api_real_execution_evidence",
        "switch_flip_cases_deferred_to_stage9",
        "event_type_kept_not_executed"
      ],
      "risks": [
        "TC002/TC003/TC005/TC006 开关关闭回退用例未实测（需在已部署环境翻转运行时 CommonVars 开关），整体 event_type=not_executed，留待 Stage 9 循环决策处理"
      ],
      "contextUpdates": {
        "eventType": "not_executed",
        "testExecutionArtifacts": ["docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md", "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/test-status.json"]
      },
      "completedAt": "2026-10-19T03:30:10Z"
  "8":
      "status": "completed",
      "summary": "recovered_from_artifacts：测试报告已产出（测试执行报告含三因子门控结论：16/19 真实执行全部通过、0 失败；event_type=not_executed 未虚报；测试期发现并修复 2 个 BUG：DriverAndYarn 复合结构 leftResource 被覆盖为空（P1，RMUtils 递归重算修复）、剩余资源 cores 为 0 显示错误（P2，前端 index.vue 修复），修复已提交并推送 release_v2.1.5）",
      "artifacts": [
        "docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md"
      ],
      "decisions": [
        "recovered_from_artifacts",
        "test_report_recovered_with_no_false_pass"
      ],
      "risks": [
        "event_type=not_executed：开关翻转用例未实跑，Stage 9 需决策补执行或风险豁免"
      ],
      "contextUpdates": {
        "testReportArtifacts": ["docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md"],
        "testReportSummary": "16/19 通过（单测13+远程API5，0失败），TC002/TC003/TC005/TC006 开关关闭回退未执行，event_type=not_executed"
      },
      "completedAt": "2026-10-19T03:30:30Z"
  "9":
      "status": "completed",
      "summary": "开关翻转关闭态回退实测完成（tests_passed）：运维将 O3/OC1/OC1增强 3 类运行时开关在 UAT 落盘 false 后，远程实测 TC002/TC003/TC005/TC006 关闭态回退 4/4 通过——全量 41 记录 0 负值、41/41 字段 left=max-used 精确匹配、非负值原样保留（DB 快照透传）、跨轮真实引擎退出（hadoop/hive used 15G/15cores/15instances→0）扣减正确。单测 13 + 远程 API 9 共 22 次真实执行 0 失败，event_type 由 not_executed 升级 test_passed，R-TEST-001 关闭，全量门禁达成，推进 Stage 10 版本级回归",
      "artifacts": [
        "test-results/remote-api/TC002_o3_rollback_used.json",
        "test-results/remote-api/TC003_auth_read_ok.json",
        "test-results/remote-api/TC005_oc1_negative_zero.json",
        "test-results/remote-api/TC005_TC006_allUserResource_full.json",
        "test-results/remote-api/TC005_TC006_allUserResource_s1_p15.json",
        "test-results/remote-api/TC006_nonnegative_preserved_rollback.json",
        "test-results/remote-api/switch_flip_rollback_observation.json",
        "docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md",
        "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/test-status.json"
      ],
      "decisions": [
        "tests_passed",
        "proceed_to_stage_10",
        "r_test_001_resolved_by_flip_execution"
      ],
      "risks": [],
      "contextUpdates": {
        "cycleDecision": "proceed_to_stage_10",
        "loopDecisionMode": "ask",
        "testResult": "test_passed"
      },
      "completedAt": "2026-10-19T09:30:00Z"
artifactIndex:
  designDocument:
    path: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    sha256: "4907b44468528f09d7936a2ec0e6fe3bfabdd77ff601cb1b1c4d929f46a40888"
    stage: "2.1"
    status: "reviewed"
  featureFile:
    path: "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/features/REQ-01_修复资源管理资源展示为负问题.feature"
    sha256: "dfaaa12ed08c73e681cae1a461ef519f7fbbec9f3efd85b22d33fef8cb61cc78"
    stage: "5.5"
    status: "generated"
  testCases:
    path: "docs/LinkisV2.1.5/testing/REQ-01_修复资源管理资源展示为负问题_测试用例.md"
    sha256: "d67ace3d079b6053cb2db59242304be09d38970f45e44885295b7323cc4066cc"
    stage: "6"
    status: "generated"
  wemindImport:
    path: "docs/LinkisV2.1.5/testing/wemind/REQ-01_修复资源管理资源展示为负问题_wemind导入.json"
    sha256: "716450ef7701afaeed2879da061a5d5f1d9053a52dff7158e63db39652b10237"
    stage: "6"
    status: "generated"
  testExecutionReport:
    path: "docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md"
    sha256: "cc14ca94f89cc9a020b5b7693049471860e457a4776a5a37e1396b70aa4c179b"
    stage: "7"
    status: "generated"
  testStatus:
    path: "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/test-status.json"
    sha256: "3f60f068bf7564e743861e1ce699532e5e1b3af9575b92fda10445a3802bd466"
    stage: "7"
    status: "generated"
  switchFlipEvidence:
    path: "test-results/remote-api/switch_flip_rollback_observation.json"
    sha256: "5927e6ff8c24510b1be5afdfa07e75021e328b217e04129e955146068a760574"
    stage: "9"
    status: "generated"
apiDocumentStatus: {}
documentArchive: {}
requirementWordExports: {}
designWordExports:
  docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md:
    markdownSha256: "4907b44468528f09d7936a2ec0e6fe3bfabdd77ff601cb1b1c4d929f46a40888"
    outputPath: "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/word_format/design.docx"
    status: "exported"
    exportedAt: "2026-10-12T11:10:00Z"
    sourceStage: "2.1"
stage3DesignReclosure: {}
staleArtifacts: []
openRisks: []
    sourceStage: "7"
    summary: "TC002/TC003/TC005/TC006（O3/OC1/OC1增强 开关关闭态回退）未在部署环境实测，整体 event_type=not_executed，待 Stage 9 循环决策处理"
    evidenceRefs: ["docs/LinkisV2.1.5/testing/reports/REQ-01_修复资源管理资源展示为负问题_测试执行报告.md"]
    status: "open"

decisionLog:
  - stage: "3-change"
    timestamp: "2026-10-15T00:00:00Z"
    decision: "第1批重大需求变更：去除O1(reset-resource接口增强)/O2(username空指针修复)/O4(前端归零/重置按钮改造)，新增allUserResource查询负数归零改造"
    rationale: "用户反馈需求范围调整：O1/O2/O4均去除。O1来源文件(EMRestfulApi.java/EMInfoService.scala/DefaultEMInfoService.scala)、O2来源文件(EMRestfulApi.java)、O4来源文件(index.vue/zh.json/en.json)已git checkout回退至701caf557父基线。O3保留(引擎退出负值丢弃开关)。新增OC1：资源管理界面查询走allUserResource接口，将usedResource(已使用资源)与leftResource(剩余资源)中的负数强制归零——在RMUtils.scala新增normalizeNegativeResource/normalizeUserResourceVoNegative工具方法，RMMonitorRest.scala.getAllUserResource中调用。"
    skipDecisions:
      - stage: "3.1"
        reason: "S1段module-code-manager模块代码管理:REQ-01为既有模块增量修改,无新增模块,跳过模块登记"
      - stage: "3.2"
        reason: "S1段java-code-review代码评审:Stage3已按设计完成O3+OC1并自检,提交后由Stage5部署+测试段验证,本阶段跳过独立评审"
  - stage: "4"
    timestamp: "2026-10-14T12:05:00Z"
    decision: "提交重放修复分支基线切换异常"
    rationale: "Stage4提交后,外部将2.1.5发布基线由webank线(dev-2.1.4-webank)切换为hadoop3线(dev-2.1.4-hadoop3),原提交20a5a296b被留在release_v2.1.5-bak,git push误推旧基线到新建远程分支。经merge-tree验证3-way合并无冲突,生成重放提交b8df1c64于新基线0339713e5之上,快进推送至远程release_v2.1.5。-bak原样保留作备份,主工作区未受影响。"
    skipDecisions:
      - stage: "3.1"
        reason: "S1段module-code-manager模块代码管理:REQ-01为既有模块增量修改,无新增模块,跳过模块登记"
      - stage: "3.2"
        reason: "S1段java-code-review代码评审:Stage3已按设计完成O1-O4并自检,提交后由Stage5部署+测试段验证,本阶段跳过独立评审"
  - stage: "recovery-align"
    timestamp: "2026-10-19T03:30:50Z"
    decision: "控制面对齐恢复（recovered_from_artifacts）：Stage 5.5/6/7/8 补记 completed、6.1 补记 skipped，lastCompletedStage 推进至 8，进入 Stage 9 循环决策；TC002/003/005/006 未执行用例留待 Stage 9 处理"
    rationale: "实际产物（Feature/测试用例/测试执行报告/test-status.json）已达 Stage 7/8 而控制面停留在 Stage 5；用户明确选择对齐控制面到 Stage 8，未执行用例在 Stage 9 循环决策时处理"
    skipDecisions:
      - stage: "6.1"
        reason: "P2段module-testing-manager回归测试同步:恢复期未发现产物,按可跳过阶段补记跳过"
  - stage: "9-loop-decision"
    timestamp: "2026-10-19T03:45:00Z"
    decision: "Stage 9 循环决策=保留现场（pause_for_manual_intervention）：用户将安排运维在 UAT 翻转 3 类运行时开关后通知续跑，补执行 TC002/TC003/TC005/TC006"
    rationale: "测试 16/19 真实通过、0 失败；未执行项为开关关闭态回退路径，需已部署环境翻转 CommonVars 方能实测。用户选择保留现场而非风险豁免，恢复时不回退、不重跑 Stage 3-8"
  - stage: "9"
    timestamp: "2026-10-19T09:30:00Z"
    decision: "Stage 9 循环决策：测试通过收口（tests_passed），推进 Stage 10 版本级回归"
    rationale: "运维将 3 类运行时开关（linkis.manager.engine.exit.negative.resource.discard.enable / linkis.manager.rm.user.resource.negative.normalize.enable / linkis.manager.rm.user.resource.left.recompute.enable）在 UAT 落盘 false（getValue JVM 启动快照语义，随服务重启生效），远程实测 TC002/TC003/TC005/TC006 关闭态回退 4/4 通过：全量 41 记录 0 负值、41/41 left=max-used、非负值原样保留、跨轮真实引擎退出扣减正确。单测 13 + 远程 API 9 共 22 次真实执行 0 失败，event_type 升级 test_passed，R-TEST-001 关闭。验证口径逐例登记于测试执行报告第八章（over-release 异常分支按守卫语义+关闭态全量观测判定，未构造 E2E 事件，如实披露）"
stabilityRisks:
  - id: R-001
    risk_level: 高
    risk_type: 参数配置
    affected_scope: linkis-resource-manager, linkis-manager（O3开关配置 linkis.manager.engine.exit.negative.resource.discard.enable）
    test_case_requirements: 配置生效验证：验证开关默认开启时引擎退出消息负值丢弃逻辑生效；配置回滚验证：验证关闭开关后恢复原有行为；配置边界值验证：覆盖开关true/false边界
    evidence: "需求事实：O3新增配置参数开关 linkis.manager.engine.exit.negative.resource.discard.enable（默认true），需求第四章4.2节O3明确通过配置参数开关控制。影响依据：linkis-manager为计算治理核心模块，资产风险画像为空按KNOWLEDGE-UNKNOWN-001保守判高。风险判断：新增配置开关涉及引擎退出消息处理的核心行为变更，开关配置错误可能导致资源负值问题无法防御或正常退出消息被错误丢弃。缺失信息：需求未提供配置生效范围（哪些节点/实例）、配置变更的具体生效方式和压测验证安排。处理建议：设计阶段补充配置生效范围、生效方式说明和配置回滚步骤。"
    status: 已覆盖
    source_stage: "1.1"
  - id: R-002
    risk_level: 高
    risk_type: 模块链路
    affected_scope: linkis-resource-manager, linkis-manager, 前端资源管理页面（reset-resource接口参数扩展+username空指针修复+前端按钮改造）
    test_case_requirements: 下游联动验证：验证reset-resource接口creator参数传入时仅重置对应资源；跨系统调用异常验证：验证username为空时返回友好错误而非空指针；级联失败恢复验证：验证前端归零/重置操作失败后的状态恢复
    evidence: "需求事实：O1扩展reset-resource接口增加creator参数，O2修复username空指针，O4前端归零按钮+重置按钮改造调用reset-resource接口，需求第四章4.2节明确接口变更和前端行为变更。影响依据：linkis为计算中间件核心系统，资产风险画像为空按KNOWLEDGE-UNKNOWN-001保守判高。风险判断：接口参数扩展和前端调用链变更影响资源重置全链路，接口参数传递错误或前端交互异常可能导致资源重置失败或误操作。缺失信息：需求未说明前端调用reset-resource接口的参数映射关系和异常处理流程。处理建议：设计阶段细化前端与后端的接口调用契约、参数映射和异常处理流程。"
    status: 已覆盖
    source_stage: "1.1"
  - id: R-003
    risk_level: 高
    risk_type: 模块链路
    affected_scope: linkis-manager（引擎退出消息处理逻辑增强，丢弃负值资源扣减操作）
    test_case_requirements: 下游联动验证：验证引擎退出消息处理逻辑变更对资源记账的影响；级联失败恢复验证：验证丢弃负值资源后引擎状态清理仍正常执行；可用性测试：验证开关开启时正常EC退出不受影响
    evidence: "需求事实：O3在linkis-manager引擎退出消息处理中增加负值丢弃逻辑，当前资源减去回收资源后为负时丢弃该引擎资源回收操作，需求第四章4.2节O3明确丢弃范围说明。影响依据：linkis-manager为计算治理核心模块，资产风险画像为空按KNOWLEDGE-UNKNOWN-001保守判高。风险判断：引擎退出消息处理逻辑变更影响资源记账核心链路，丢弃逻辑异常可能导致资源泄漏或正常资源扣减被错误丢弃。缺失信息：需求未提供高可用降级方案和开关关闭时的回滚验证。处理建议：设计阶段补充丢弃逻辑的边界条件、引擎状态清理与资源扣减的分离设计和开关回滚验证方案。"
    status: 已覆盖
    source_stage: "1.1"
stabilityRiskAssessmentTrace:
  R-001:
    rules_version: "1.1"
    catalog_version: "2.0"
    scoring_provisional: true
    final_level: 高
    deterministic_level: 高
    llm_proposed_level: 中
    knowledge_status: unknown
    matched_rule_ids:
      - KNOWLEDGE-UNKNOWN-001
      - CROSS-SYSTEM-001
      - CHANGE-TYPE-UNKNOWN-001
      - INPUT-VALUE-UNKNOWN-001
    missing_knowledge:
      - asset_catalog_entry
    degraded: true
    blocking: false
  R-002:
    rules_version: "1.1"
    catalog_version: "2.0"
    scoring_provisional: true
    final_level: 高
    deterministic_level: 高
    llm_proposed_level: 中
    knowledge_status: unknown
    matched_rule_ids:
      - KNOWLEDGE-UNKNOWN-001
      - CROSS-SYSTEM-001
      - CHANGE-TYPE-UNKNOWN-001
      - INPUT-VALUE-UNKNOWN-001
    missing_knowledge:
      - asset_catalog_entry
    degraded: true
    blocking: false
  R-003:
    rules_version: "1.1"
    catalog_version: "2.0"
    scoring_provisional: true
    final_level: 高
    deterministic_level: 高
    llm_proposed_level: 中
    knowledge_status: unknown
    matched_rule_ids:
      - KNOWLEDGE-UNKNOWN-001
      - CHANGE-TYPE-UNKNOWN-001
      - INPUT-VALUE-UNKNOWN-001
    missing_knowledge:
      - asset_catalog_entry
    degraded: true
    blocking: false
stabilityRiskSourceEvidence:
  - evidence_id: EVID-R-001-001
    kind: requirement_fact
    value: "O3新增配置参数开关 linkis.manager.engine.exit.negative.resource.discard.enable（默认true）"
    state: present
    source_refs:
      - "requirement-analysis.md#4.2 O3"
    source_excerpt: "此功能通过配置参数开关控制（配置项名：linkis.manager.engine.exit.negative.resource.discard.enable，默认值：true，即默认开启防御逻辑）"
    resolver_id: requirement_document_literal_resolver
  - evidence_id: EVID-R-002-001
    kind: requirement_fact
    value: "O1扩展reset-resource接口增加creator参数，O4前端重置按钮改为调用reset-resource接口"
    state: present
    source_refs:
      - "requirement-analysis.md#4.2 O1"
      - "requirement-analysis.md#4.2 O4"
    source_excerpt: "在 reset-resource 接口中增加 creator 参数，支持按 creator 进行细粒度资源重置"
    resolver_id: requirement_document_literal_resolver
  - evidence_id: EVID-R-003-001
    kind: requirement_fact
    value: "O3在引擎退出消息处理中增加负值丢弃逻辑"
    state: present
    source_refs:
      - "requirement-analysis.md#4.2 O3"
    source_excerpt: "当前资源减去引擎回收资源后为负时，丢弃该引擎资源回收操作（识别为重置时未kill释放的残留EC）"
    resolver_id: requirement_document_literal_resolver
stabilityRiskRequirementMappings:
  - risk_id: R-001
    requirement_items:
      - item_id: O3
        item_type: optimization
        title: 引擎退出消息负值丢弃 + warn日志 + 参数开关
        source_ref: "requirement-analysis.md#4.2 O3"
  - risk_id: R-002
    requirement_items:
      - item_id: O1
        item_type: optimization
        title: reset-resource接口增强（增加creator参数，支持细粒度资源重置）
        source_ref: "requirement-analysis.md#4.2 O1"
      - item_id: O2
        item_type: optimization
        title: 修复传入username空指针问题
        source_ref: "requirement-analysis.md#4.2 O2"
      - item_id: O4
        item_type: optimization
        title: 前端归零按钮+重置按钮改造（二次确认+调用reset-resource接口）
        source_ref: "requirement-analysis.md#4.2 O4"
  - risk_id: R-003
    requirement_items:
      - item_id: O3
        item_type: optimization
        title: 引擎退出消息负值丢弃 + warn日志 + 参数开关
        source_ref: "requirement-analysis.md#4.2 O3"
stabilityRiskBusinessConclusions:
  - risk_id: R-001
    change_fact: "新增配置开关控制引擎退出消息负值丢弃逻辑"
    business_impact: "资源负值防御机制失效或正常退出消息被错误丢弃"
    conclusion: "新增配置开关控制引擎退出消息负值丢弃逻辑可能导致资源负值防御机制失效或正常退出消息被错误丢弃"
  - risk_id: R-002
    change_fact: "reset-resource接口参数扩展和前端调用链变更"
    business_impact: "资源重置操作失败或误操作影响用户资源数据"
    conclusion: "reset-resource接口参数扩展和前端调用链变更可能导致资源重置操作失败或误操作影响用户资源数据"
  - risk_id: R-003
    change_fact: "引擎退出消息处理逻辑增强增加负值丢弃"
    business_impact: "资源记账异常或正常资源扣减被错误丢弃"
    conclusion: "引擎退出消息处理逻辑增强增加负值丢弃可能导致资源记账异常或正常资源扣减被错误丢弃"
stabilityRiskTestRequirements:
  - requirement_id: RTR-001-CONFIG-EFFECT
    risk_id: R-001
    policy_rule_ids:
      - RTP-CONFIGURATION-001
    definition_id: RTEST-CONFIG-EFFECT-001
    policy_version: "1.0"
    test_type: configuration_effect
    title: 配置变更生效验证
    business_context: "验证开关linkis.manager.engine.exit.negative.resource.discard.enable默认true时引擎退出消息负值丢弃逻辑生效"
    display_text: 配置变更生效验证：验证开关默认开启时引擎退出消息负值丢弃逻辑生效
    verification_points:
      - 验证目标配置值在声明范围内生效
      - 验证生效后的业务行为符合预期
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=参数配置"
  - requirement_id: RTR-001-CONFIG-ROLLBACK
    risk_id: R-001
    policy_rule_ids:
      - RTP-CONFIGURATION-001
    definition_id: RTEST-CONFIG-ROLLBACK-001
    policy_version: "1.0"
    test_type: configuration_rollback
    title: 配置回滚验证
    business_context: "验证关闭开关linkis.manager.engine.exit.negative.resource.discard.enable=false后恢复原有退出消息处理行为"
    display_text: 配置回滚验证：验证关闭开关后恢复原有退出消息处理行为
    verification_points:
      - 验证配置可恢复至变更前状态
      - 验证回滚后的业务行为和数据状态
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=参数配置"
  - requirement_id: RTR-001-CONFIG-BOUNDARY
    risk_id: R-001
    policy_rule_ids:
      - RTP-CONFIGURATION-001
    definition_id: RTEST-CONFIG-BOUNDARY-001
    policy_version: "1.0"
    test_type: configuration_boundary
    title: 配置边界值验证
    business_context: "覆盖开关true/false边界值，验证非法配置的拒绝或保护行为"
    display_text: 配置边界值验证：覆盖开关true/false边界值
    verification_points:
      - 覆盖最小值、最大值和越界值
      - 验证非法配置的拒绝或保护行为
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=参数配置"
  - requirement_id: RTR-002-DOWNSTREAM-LINKAGE
    risk_id: R-002
    policy_rule_ids:
      - RTP-MODULE-LINK-001
    definition_id: RTEST-DOWNSTREAM-LINKAGE-001
    policy_version: "1.0"
    test_type: downstream_linkage
    title: 下游联动验证
    business_context: "验证reset-resource接口creator参数传入时仅重置对应资源，前端归零/重置按钮调用链路正确"
    display_text: 下游联动验证：验证reset-resource接口creator参数和前端按钮调用链路
    verification_points:
      - 覆盖已确认的下游调用链
      - 验证上下游输入输出和状态传递
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=模块链路"
  - requirement_id: RTR-002-CROSS-SYSTEM-FAILURE
    risk_id: R-002
    policy_rule_ids:
      - RTP-MODULE-LINK-001
    definition_id: RTEST-CROSS-SYSTEM-FAILURE-001
    policy_version: "1.0"
    test_type: cross_system_failure
    title: 跨系统调用异常验证
    business_context: "验证username为空时返回友好错误而非空指针异常，前端调用reset-resource接口异常时的错误处理"
    display_text: 跨系统调用异常验证：验证username空指针修复和前端异常处理
    verification_points:
      - 覆盖跨系统调用失败或异常返回
      - 验证调用方错误处理和业务状态
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=模块链路"
  - requirement_id: RTR-002-CASCADE-RECOVERY
    risk_id: R-002
    policy_rule_ids:
      - RTP-MODULE-LINK-001
    definition_id: RTEST-CASCADE-RECOVERY-001
    policy_version: "1.0"
    test_type: cascade_recovery
    title: 级联失败恢复验证
    business_context: "验证前端归零/重置操作失败后的状态恢复和二次确认机制"
    display_text: 级联失败恢复验证：验证前端操作失败后的状态恢复和二次确认机制
    verification_points:
      - 覆盖局部故障向下游传播的场景
      - 验证隔离、恢复和业务状态收敛
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=模块链路"
  - requirement_id: RTR-003-DOWNSTREAM-LINKAGE
    risk_id: R-003
    policy_rule_ids:
      - RTP-MODULE-LINK-001
    definition_id: RTEST-DOWNSTREAM-LINKAGE-001
    policy_version: "1.0"
    test_type: downstream_linkage
    title: 下游联动验证
    business_context: "验证引擎退出消息处理逻辑变更对资源记账的影响，丢弃负值资源扣减后引擎状态清理仍正常执行"
    display_text: 下游联动验证：验证引擎退出消息处理变更对资源记账的影响
    verification_points:
      - 覆盖已确认的下游调用链
      - 验证上下游输入输出和状态传递
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=模块链路"
  - requirement_id: RTR-003-CASCADE-RECOVERY
    risk_id: R-003
    policy_rule_ids:
      - RTP-MODULE-LINK-001
    definition_id: RTEST-CASCADE-RECOVERY-001
    policy_version: "1.0"
    test_type: cascade_recovery
    title: 级联失败恢复验证
    business_context: "验证丢弃负值资源后引擎状态清理仍正常执行，开关关闭时恢复原有行为"
    display_text: 级联失败恢复验证：验证丢弃逻辑不影响引擎状态清理
    verification_points:
      - 覆盖局部故障向下游传播的场景
      - 验证隔离、恢复和业务状态收敛
    evidence_requirements:
      - test_case_generated
      - review_passed
      - execution_passed
    required: true
    applicability_basis:
      - "risk.risk_type=模块链路"
stabilityDesignObligations:
  - obligation_id: R-001-handling_strategy
    risk_id: R-001
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    fact_fields: ["处理对象", "实施动作", "预期结果"]
    target_section_roles: ["architecture", "configuration", "data"]
  - obligation_id: R-001-exception_path
    risk_id: R-001
    concern_type: exception_path
    concern: 异常路径
    required: true
    fact_fields: ["触发条件", "处理动作", "业务结果"]
    target_section_roles: ["exception", "degradation", "architecture"]
  - obligation_id: R-001-recovery_strategy
    risk_id: R-001
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    fact_fields: ["恢复触发", "恢复动作", "成功标准"]
    target_section_roles: ["recovery", "rollback", "exception"]
  - obligation_id: R-001-rollback
    risk_id: R-001
    concern_type: rollback
    concern: 回滚与止损
    required: true
    fact_fields: ["回滚触发", "回滚动作", "验证标准"]
    target_section_roles: ["rollback", "recovery", "configuration"]
  - obligation_id: R-001-observability
    risk_id: R-001
    concern_type: observability
    concern: 监控与告警
    required: true
    fact_fields: ["监控指标", "告警条件"]
    target_section_roles: ["monitoring", "exception"]
  - obligation_id: R-002-handling_strategy
    risk_id: R-002
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    fact_fields: ["处理对象", "实施动作", "预期结果"]
    target_section_roles: ["architecture", "configuration", "data"]
  - obligation_id: R-002-exception_path
    risk_id: R-002
    concern_type: exception_path
    concern: 异常路径
    required: true
    fact_fields: ["触发条件", "处理动作", "业务结果"]
    target_section_roles: ["exception", "degradation", "architecture"]
  - obligation_id: R-002-recovery_strategy
    risk_id: R-002
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    fact_fields: ["恢复触发", "恢复动作", "成功标准"]
    target_section_roles: ["recovery", "rollback", "exception"]
  - obligation_id: R-002-compatibility
    risk_id: R-002
    concern_type: compatibility
    concern: 兼容性
    required: true
    fact_fields: ["兼容对象", "兼容策略"]
    target_section_roles: ["compatibility", "architecture"]
  - obligation_id: R-002-degradation
    risk_id: R-002
    concern_type: degradation
    concern: 降级与熔断
    required: true
    fact_fields: ["降级触发", "降级动作"]
    target_section_roles: ["degradation", "exception", "recovery"]
  - obligation_id: R-002-observability
    risk_id: R-002
    concern_type: observability
    concern: 监控与告警
    required: true
    fact_fields: ["监控指标", "告警条件"]
    target_section_roles: ["monitoring", "exception"]
  - obligation_id: R-003-handling_strategy
    risk_id: R-003
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    fact_fields: ["处理对象", "实施动作", "预期结果"]
    target_section_roles: ["architecture", "configuration", "data"]
  - obligation_id: R-003-exception_path
    risk_id: R-003
    concern_type: exception_path
    concern: 异常路径
    required: true
    fact_fields: ["触发条件", "处理动作", "业务结果"]
    target_section_roles: ["exception", "degradation", "architecture"]
  - obligation_id: R-003-recovery_strategy
    risk_id: R-003
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    fact_fields: ["恢复触发", "恢复动作", "成功标准"]
    target_section_roles: ["recovery", "rollback", "exception"]
  - obligation_id: R-003-compatibility
    risk_id: R-003
    concern_type: compatibility
    concern: 兼容性
    required: true
    fact_fields: ["兼容对象", "兼容策略"]
    target_section_roles: ["compatibility", "architecture"]
  - obligation_id: R-003-degradation
    risk_id: R-003
    concern_type: degradation
    concern: 降级与熔断
    required: true
    fact_fields: ["降级触发", "降级动作"]
    target_section_roles: ["degradation", "exception", "recovery"]
  - obligation_id: R-003-observability
    risk_id: R-003
    concern_type: observability
    concern: 监控与告警
    required: true
    fact_fields: ["监控指标", "告警条件"]
    target_section_roles: ["monitoring", "exception"]
stabilityDesignConcerns:
  - concern_id: R-001-handling_strategy
    risk_id: R-001
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    design_response: "在linkis-manager引擎退出消息处理中增加负值丢弃逻辑，由开关linkis.manager.engine.exit.negative.resource.discard.enable控制；当前资源减去回收资源后为负时丢弃该引擎资源回收操作并打印warn日志"
    design_facts:
      处理对象: "引擎退出消息中的资源回收操作（usedResource扣减）"
      实施动作: "开关开启时检查当前资源减去回收资源是否为负，为负则丢弃该次回收操作并打印warn日志"
      预期结果: "资源展示为负数的异常状态被消除，资源数据保持非负"
    design_reference: "§3.3 O3引擎退出消息负值丢弃逻辑设计"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§3.3章节包含完整的处理方案描述，涵盖处理对象、实施动作和预期结果"
    status: 已覆盖
    status_source: provider
  - concern_id: R-001-exception_path
    risk_id: R-001
    concern_type: exception_path
    concern: 异常路径
    required: true
    design_response: "开关配置异常（如值非法）时视为false，回退到原有退出消息处理行为；开关配置正确但丢弃逻辑执行异常时记录error日志并回退到原有扣减逻辑"
    design_facts:
      触发条件: "开关配置值非法或丢弃逻辑执行异常"
      处理动作: "回退到原有退出消息处理行为，记录error日志"
      业务结果: "资源扣减按原有逻辑执行，不会因开关异常导致资源记账中断"
    design_reference: "§4.1 R-001异常路径"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§4.1章节明确了异常触发条件、处理动作和业务结果"
    status: 已覆盖
    status_source: provider
  - concern_id: R-001-recovery_strategy
    risk_id: R-001
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    design_response: "关闭开关linkis.manager.engine.exit.negative.resource.discard.enable即可恢复原有退出消息处理行为"
    design_facts:
      恢复触发: "运维人员发现丢弃逻辑异常或需要回退时"
      恢复动作: "将开关值设为false并重启相关服务"
      成功标准: "引擎退出消息按原有逻辑执行资源扣减，不再丢弃负值操作"
    design_reference: "§5.1 R-001恢复方式"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§5.1章节描述了恢复触发、恢复动作和成功标准"
    status: 已覆盖
    status_source: provider
  - concern_id: R-001-rollback
    risk_id: R-001
    concern_type: rollback
    concern: 回滚与止损
    required: true
    design_response: "通过配置中心修改开关值为false实现回滚，无需代码回退；验证标准为开关关闭后引擎退出消息按原有逻辑执行"
    design_facts:
      回滚触发: "丢弃逻辑导致正常资源扣减被错误丢弃或资源负值问题重现"
      回滚动作: "修改配置linkis.manager.engine.exit.negative.resource.discard.enable=false"
      验证标准: "开关关闭后引擎退出消息按原有逻辑执行资源扣减，资源数据恢复正常"
    design_reference: "§5.2 R-001回滚与止损 + §9.3 配置回滚步骤"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§5.2和§9.3章节提供了回滚触发、回滚动作和验证标准"
    status: 已覆盖
    status_source: provider
  - concern_id: R-001-observability
    risk_id: R-001
    concern_type: observability
    concern: 监控与告警
    required: true
    design_response: "新增丢弃次数计数指标和开关状态监控，配置resourceCheck告警规则"
    design_facts:
      监控指标: "引擎退出消息负值丢弃次数、开关当前状态值"
      告警条件: "丢弃次数异常增多或开关状态非预期变更时触发告警"
    design_reference: "§8.1 R-001监控指标"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§8.1章节定义了监控指标和告警条件"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-handling_strategy
    risk_id: R-002
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    design_response: "O1扩展reset-resource接口增加creator参数支持细粒度重置，O2修复username空指针，O4前端归零/重置按钮改造调用reset-resource接口"
    design_facts:
      处理对象: "reset-resource接口、username空指针、前端归零/重置按钮"
      实施动作: "接口增加creator参数、username空值检查、前端按钮二次确认和接口调用"
      预期结果: "资源重置可按creator细粒度执行，空指针异常消除，前端操作入口完善"
    design_reference: "§3.1 O1设计 + §3.2 O2设计 + §3.4 O4设计"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§3.1/§3.2/§3.4章节分别描述了O1/O2/O4的处理方案"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-exception_path
    risk_id: R-002
    concern_type: exception_path
    concern: 异常路径
    required: true
    design_response: "username为空时返回友好错误提示而非空指针异常；creator参数非法时返回参数校验错误；接口调用异常时前端显示错误提示"
    design_facts:
      触发条件: "username为空、creator参数非法、接口调用失败"
      处理动作: "空值返回友好错误、非法参数返回校验错误、前端捕获异常显示提示"
      业务结果: "用户收到明确的错误提示，不影响其他资源操作"
    design_reference: "§4.2 R-002异常路径"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§4.2章节覆盖了三种异常场景的触发条件、处理动作和业务结果"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-recovery_strategy
    risk_id: R-002
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    design_response: "前端不传creator参数时接口按原有逻辑执行全量重置；O2修复可通过git revert回退"
    design_facts:
      恢复触发: "细粒度重置功能异常或需要回退到全量重置"
      恢复动作: "前端不传creator参数，接口按原有逻辑执行；O2修复通过git revert回退"
      成功标准: "reset-resource接口恢复全量重置行为，空指针问题按需保留或回退"
    design_reference: "§5.3 R-002恢复方式"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§5.3章节描述了恢复触发、恢复动作和成功标准"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-compatibility
    risk_id: R-002
    concern_type: compatibility
    concern: 兼容性
    required: true
    design_response: "creator为可选参数，不传时按原有逻辑全量重置；username非空时行为不变；接口路径不变"
    design_facts:
      兼容对象: "现有reset-resource接口调用方和前端页面"
      兼容策略: "creator可选参数向后兼容，不传时保持原有全量重置行为"
    design_reference: "§6.1 R-002兼容性"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§6.1章节明确了兼容对象和兼容策略"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-degradation
    risk_id: R-002
    concern_type: degradation
    concern: 降级与熔断
    required: true
    design_response: "接口超时或异常时不自动重试，由用户手动重新操作；前端操作失败时不刷新页面，数据保持不变"
    design_facts:
      降级触发: "接口调用超时或返回异常"
      降级动作: "不自动重试，前端提示错误，用户手动重新操作"
    design_reference: "§7.1 R-002降级策略"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§7.1章节定义了降级触发条件和降级动作"
    status: 已覆盖
    status_source: provider
  - concern_id: R-002-observability
    risk_id: R-002
    concern_type: observability
    concern: 监控与告警
    required: true
    design_response: "新增接口调用次数、错误次数和空值触发次数监控指标"
    design_facts:
      监控指标: "reset-resource接口调用次数、错误次数、username空值触发次数"
      告警条件: "错误率超阈值或空值触发次数异常增多时告警"
    design_reference: "§8.2 R-002监控指标"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§8.2章节定义了监控指标和告警条件"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-handling_strategy
    risk_id: R-003
    concern_type: handling_strategy
    concern: 处理方案
    required: true
    design_response: "在引擎退出消息处理中增加负值丢弃逻辑，资源扣减与引擎状态清理分离设计，丢弃仅影响数值扣减不影响状态清理"
    design_facts:
      处理对象: "引擎退出消息处理中的资源扣减操作和引擎状态清理操作"
      实施动作: "当前资源减去回收资源后为负时丢弃扣减操作，状态清理正常执行"
      预期结果: "资源数据保持非负，引擎状态正常清理"
    design_reference: "§3.3 O3设计"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§3.3章节描述了处理对象、实施动作和预期结果"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-exception_path
    risk_id: R-003
    concern_type: exception_path
    concern: 异常路径
    required: true
    design_response: "丢弃逻辑执行异常时回退到原有扣减逻辑，记录error日志"
    design_facts:
      触发条件: "丢弃逻辑执行过程中发生异常"
      处理动作: "回退到原有扣减逻辑，记录error日志"
      业务结果: "资源扣减按原有逻辑执行，不影响引擎退出流程"
    design_reference: "§4.3 R-003异常路径"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§4.3章节明确了异常触发条件、处理动作和业务结果"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-recovery_strategy
    risk_id: R-003
    concern_type: recovery_strategy
    concern: 恢复方式
    required: true
    design_response: "关闭O3开关恢复原有引擎退出消息处理行为"
    design_facts:
      恢复触发: "丢弃逻辑异常或需要回退时"
      恢复动作: "关闭开关linkis.manager.engine.exit.negative.resource.discard.enable"
      成功标准: "引擎退出消息按原有逻辑执行，资源扣减和状态清理均正常"
    design_reference: "§5.4 R-003恢复方式"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§5.4章节描述了恢复触发、恢复动作和成功标准"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-compatibility
    risk_id: R-003
    concern_type: compatibility
    concern: 兼容性
    required: true
    design_response: "正常EC退出不受影响（资源减去回收值非负时不丢弃）；开关关闭时完全恢复原有行为；resourceCheck不变"
    design_facts:
      兼容对象: "正常EC退出流程、开关关闭场景、resourceCheck机制"
      兼容策略: "仅在资源为负时丢弃，正常退出不受影响；开关关闭完全恢复；resourceCheck逻辑不变"
    design_reference: "§6.2 R-003兼容性"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§6.2章节明确了兼容对象和兼容策略"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-degradation
    risk_id: R-003
    concern_type: degradation
    concern: 降级与熔断
    required: true
    design_response: "丢弃逻辑异常时回退到原有扣减逻辑，状态清理不受影响"
    design_facts:
      降级触发: "丢弃逻辑执行异常"
      降级动作: "回退到原有扣减逻辑，引擎状态清理继续按原有流程执行"
    design_reference: "§7.2 R-003降级策略"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§7.2章节定义了降级触发条件和降级动作"
    status: 已覆盖
    status_source: provider
  - concern_id: R-003-observability
    risk_id: R-003
    concern_type: observability
    concern: 监控与告警
    required: true
    design_response: "新增丢弃次数和状态清理失败次数监控指标"
    design_facts:
      监控指标: "引擎退出消息负值丢弃次数、引擎状态清理失败次数"
      告警条件: "丢弃次数异常增多或状态清理失败时触发告警"
    design_reference: "§8.3 R-003监控指标"
    response_origin: existing_design
    evidence_document: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    assessment_basis: "设计文档§8.3章节定义了监控指标和告警条件"
    status: 已覆盖
    status_source: provider
stabilityDesignQualityAssessment:
  scope: 运营稳定性
  evaluation_status: 已完成
  verdict: 符合标准
  risk_total: 3
  risk_covered: 3
  risk_coverage_rate: 100
  concern_total: 17
  concern_covered: 17
  concern_coverage_rate: 100
  unresolved_high_risk_count: 0
  basis: "Stage 2.1 设计检视完成，6项优化已全部执行；17项设计义务全部在正式设计章节承接，fact_fields 完整且可在响应中定位；3条风险全部聚合为已覆盖"
injection_risk:
  level: "N/A"
  reason: "本需求为Java/Scala后端代码优化和前端页面调整，不涉及Claude插件（Agent/Skill/Command）开发"
  identified_at: "2026-10-10T14:05:00Z"
diagramSyncChecks:
  requirement:
    documentPath: "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/requirement-analysis.md"
    documentSha256: "9fc793c5f556e0916337e1d974b0cff009515c7a6f97cd2aec0d81c834106222"
    status: "not_applicable"
    checkedAt: "2026-10-10T14:05:00Z"
  design:
    documentPath: "docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md"
    documentSha256: "4907b44468528f09d7936a2ec0e6fe3bfabdd77ff601cb1b1c4d929f46a40888"
    status: "passed"
    checkedAt: "2026-10-12T11:00:00Z"
    diagrams:
      - type: architecture
        location: "§2.1 四层防御体系架构图"
        status: "consistent"
      - type: sequence
        location: "§3.3.2 优化前流程"
        status: "consistent"
      - type: sequence
        location: "§3.3.3 优化后流程"
        status: "consistent"
requirementWordExports:
  dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/requirement-analysis.md:
    markdownSha256: "9fc793c5f556e0916337e1d974b0cff009515c7a6f97cd2aec0d81c834106222"
    outputPath: "dev/versions/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/word_format/requirement-analysis.docx"
    status: "exported"
    exportedAt: "2026-10-10T14:10:00Z"
    sourceStage: "1.1"
createdAt: "2026-10-10T00:00:00Z"
updatedAt: "2026-10-19T09:30:00Z"
---

# 任务上下文

## 📊 阶段执行记录（需求/设计相位）

> 仅记录需求相位（Stage 0/1/1.1）与设计相位（Stage 2/2.1）的终态（成功/跳过/失败），其余阶段不记。
> 每 stage 至多一行；写入前若该 stage 已有行则先替换旧行（兼容"失败后重试成功"）。
> 写入规则与点位见 `stage-hooks.md` 步骤10.5（成功/跳过）与强制恢复流程步骤2.5（失败）。

| 阶段 | 名称 | 状态 | 产出物 | 完成时间 | 备注 |
|------|------|------|--------|----------|------|
| 0 | 需求澄清 | 成功 | clarification-summary.md | 2026-10-10T11:30:00Z | 1轮完成，高明确度需求 |
| 1 | 需求分析 | 成功 | requirement-analysis.md, features/REQ-01_修复资源管理资源展示为负问题.feature | 2026-10-10T12:00:00Z | v1.0: 3个优化点，5人天 |
| 1.1 | 需求检视 | 成功 | requirement-analysis.md (v1.3 含需求风险摘要), features/*.feature (重写适配v1.1), word_format/requirement-analysis.docx | 2026-10-10T14:10:00Z | Stage1-Hook完成：触点①3条高风险+DPMS降级+图表N/A+Word导出 |
| 2 | 方案设计 | 成功 | docs/LinkisV2.1.5/design/REQ-01_修复资源管理资源展示为负问题_设计.md | 2026-10-12T10:30:00Z | 17项设计义务承接，触点②prepare_obligations+summarize_response完成，质量待核验 |
| 2.1 | 设计检视 | 成功 | 设计文档（6项优化已执行+用户反馈修正） | 2026-10-12T11:30:00Z | 六维检视82分，6项优化全部执行；用户反馈修正：O1接口定位修正为EMRestfulApi.java:593，O4前端按钮设计修正为原重置按钮改名归零+新增重置按钮调用reset-resource传入creator；verify_final_design：符合标准，3条风险全部已覆盖 |

## 📋 任务基本信息

**任务名称**: REQ-01_修复资源管理资源展示为负问题
**需求类型**: OPTIMIZE
**执行模式**: 快速模式
**竞品分析摘要**: （用户跳过竞品分析）
**价值收益摘要**: （用户跳过价值收益评估）

## 📋 项目上下文确认

**项目上下文状态**: 已确认
**生成时间戳**: 2026-10-10T00:00:00Z

## 📚 知识库状态

**知识库状态**: 已构建
**模块索引**: docs/project-knowledge/module-index.json

## 📝 需求描述

> 注：本段已按第1批需求变更（decisionLog stage "3-change"）更新为当前范围 O3+OC1；原 O1(reset-resource接口增强)/O2(username空指针修复)/O4(前端归零/重置按钮改造) 已去除。

1.背景：
在 Linkis 计算治理平台运行过程中，资源管理模块（linkis-resource-manager）存在用户资源展示为负数的异常问题。该问题通常发生在以下场景：
引擎异常退出：EngineConn（EC）因 OOM、超时等原因异常退出，资源释放消息与资源记账不同步
资源重置操作：管理员执行资源重置后，已有 EC 的退出消息仍会触发资源扣减
标签切换：用户切换引擎标签后，旧标签下残留 EC 的退出消息对新标签资源进行扣减
根本原因在于：资源释放消息与资源记账操作之间存在时序不一致，导致已占用资源变为负值、剩余资源超过上限。

2.目的：
修复资源负值问题：消除现有资源展示为负数的异常状态
防御性增强：通过引擎退出消息防御层（O3）+ 界面查询展示层归零（OC1）两层机制建立完整防御体系
向后兼容：O3 增强逻辑通过开关控制，关闭时保持原有行为；OC1 仅在展示层归零，不改变底层数据

输入：
后端：
1. linkis manager 收到引擎退出消息时，如果当前资源减去引擎回收资源后还是为负，则应该丢弃该引擎资源回收操作（识别到该引擎为重置时没有 kill 释放的资源，打印 warn 日志），这个功能加参数开关（linkis.manager.engine.exit.negative.resource.discard.enable，默认 true）—— O3
2. 资源管理界面查询时使用 allUserResource 接口（RMMonitorRest.getAllUserResource），如果查询回来的"已使用资源"(usedResource) 存在负数，就直接强制改成 0；如果"剩余资源"(leftResource) 里面存在负数也改成 0 —— OC1

输出：正确显示可用资源数量，非负数
