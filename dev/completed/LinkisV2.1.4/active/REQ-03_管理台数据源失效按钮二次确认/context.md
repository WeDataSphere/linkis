---
contextSchemaVersion: "2.1"
contextRevision: 1
versionName: "LinkisV2.1.4"
taskName: "REQ-03_管理台数据源失效按钮二次确认"
reqIndex: 3
reqPrefix: "REQ-03"
versionContextPath: "dev/completed/LinkisV2.1.4/version-context.md"
checkpointId: null
checkpointState: "clean"
currentStage: "10"
lastCompletedStage: "10"
stageHistory: ["0","1","1.1","1.2","1.6","2","2.1","2.2","3","3.1","3.2","3.5","4","5","5.5","6","7","8","9","10"]
skipDecisions:
  "6.1": "skipped"
rollbackHistory: []
segmentProgress:
  P1: {status: "completed", stages: ["0","1","1.1","1.2","1.6","2","2.1","2.2"]}
  S1: {status: "completed", stages: ["3","3.1","3.2","3.5","4","5"]}
  P2: {status: "completed", stages: ["5.5","6","6.1"]}
  S2: {status: "completed", stages: ["7","8"]}
  D:  {status: "completed", stages: ["9"]}
  V:  {status: "completed", stages: ["10"], note: "Stage 10 regression 已通过，报告 320339（补录恢复）"}
executionProfile: "default"
testResult: "test_passed"
executionEvidence:
  blockedReason: "已于 SIT 前端管理台实测通过（补录）"
  remoteUrl: "http://dpms.weoa.com/#/product/100484/test-plan/detail/318884"
  vpnRequired: true
  executedAt: "2026-10-10T18:00:00+08:00"
testExecutionArtifacts:
  testExecutionReport: "docs/LinkisV2.1.4/testing/reports/REQ-03_数据源过期二次确认_测试执行报告.md"
  codeReviewStatus: "passed"
testReportArtifacts:
  testReport: "docs/LinkisV2.1.4/testing/reports/REQ-03_数据源过期二次确认_测试执行报告.md"
  testReportSummary: "纯前端需求14条用例已于 SIT 前端管理台 318884 实测通过（补录）；代码审查通过"
  testResultFinal: "test_passed"
artifactIndex:
  requirementDocument: "docs/LinkisV2.1.4/requirements/REQ-03_数据源过期二次确认_需求.md"
  designDocument: "docs/LinkisV2.1.4/design/REQ-03_数据源过期二次确认_设计.md"
  codeChangeSummary: "datasource/index.vue(+28/-4) 二次确认弹框 + zh/en.json i18n + EnvConfiguration 配置项 + TestContextHAManager 调整"
  changedFiles: 5
  devCommitId: "62662aed5"
  devCommitDesc: "开发阶段：数据源过期按钮新增弹框确认"
reqSubsystemId: 5436
repositoryId: null
apiDocumentStatus: "not_applicable"
sdlResult: "not_applicable"
sdlEvidence: "纯前端弹框二次确认 + i18n 文案，无后端逻辑、无新增接口、无安全敏感处理（补录）"
openRisks: []
testClarificationPath: "docs/LinkisV2.1.4/testing/REQ-03_数据源过期二次确认_测试澄清.md"
testScope:
  mustTest:
    - title: "点击过期按钮弹出二次确认 Modal"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "Modal 内容包含数据源名称和不可恢复提示"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "Modal 包含确认和取消两个按钮"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "点击取消关闭 Modal 不调用 expire 接口"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "点击确认关闭 Modal 并调用 expire 接口"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "expire 成功后提示数据源已过期并刷新列表"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "中文环境 Modal 文案正确显示"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "英文环境 Modal 文案正确显示"
      risk_ids: []
      requirement_ids: []
      source: "acceptance_criteria"
    - title: "overdueConfirm 状态对象正确管理 show/id/name"
      risk_ids: []
      requirement_ids: []
      source: "feature_scenario"
    - title: "overdue 方法不再直接调用 expire"
      risk_ids: []
      requirement_ids: []
      source: "feature_scenario"
  optionalTest:
    - title: "数据源名称为空时 Modal 内容处理"
      risk_ids: []
      source: "user_decision"
    - title: "expire 接口失败时错误提示"
      risk_ids: []
      source: "user_decision"
    - title: "连续快速点击过期按钮防竞态"
      risk_ids: []
      source: "user_decision"
    - title: "Modal 关闭后 overdueConfirm 状态重置"
      risk_ids: []
      source: "user_decision"
  notTest:
    - title: "后端 expire 接口逻辑"
      source: "user_decision"
    - title: "EnvConfiguration.scala 变更"
      source: "user_decision"
    - title: "TestContextHAManager.java 变更"
      source: "user_decision"
    - title: "数据源列表分页搜索创建等其他功能"
      source: "user_decision"
environmentInfo:
  remoteUrl: "需提测后确认 SIT 环境前端管理台地址"
  availability: "需提测后可用"
  vpnRequired: true
  frontendType: "pure_frontend"
  subsystemId: 5436
  subsystemName: "WDSL-RM"
authInfo:
  loginMethod: "账密登录"
  tokenAcquisition: "登录成功后通过 cookie/token 管理会话"
  testAccountSource: "测试环境运维账号"
  permissionRequired: "数据源管理模块查看和操作权限"
dataStrategy:
  skipped: false
  dataItems:
    - "至少一条有效数据源"
    - "数据源名称含特殊字符（可选）"
    - "空名称数据源（选测）"
  constructionMethod: "通过管理台创建数据源功能或 DB 直接插入"
  dependencies:
    - "数据源类型已配置"
  cleanupStrategy: "测试结束后删除或过期创建的测试数据源"
testCaseArtifacts:
  testCaseDoc: "docs/LinkisV2.1.4/testing/REQ-03_数据源过期二次确认_测试用例.md"
  wemindImportFile: "docs/LinkisV2.1.4/testing/REQ-03_数据源过期二次确认_测试用例_wemind.json"
  testCaseCount: 14
  mustTestCount: 10
  optionalTestCount: 4
  generatedAt: "2026-10-08T17:00:00+08:00"
updatedAt: "2026-10-08T17:00:00+08:00"
---

# 需求上下文：REQ-03 管理台数据源失效按钮二次确认

## 补录说明

2026-09-24 手动补录：依据 git commit 62662aed5 回填 devCommitId + status=completed。
2026-10-08 恢复开发：创建 context.md，补 segmentProgress，从 P2 段（Stage 5.5）恢复测试流程。
