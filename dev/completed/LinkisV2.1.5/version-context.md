---
contextSchemaVersion: "1.0"
contextRevision: 17
checkpointId: "LinkisV2.1.5:VERSION:10:2026-10-19T09:50:00Z"
checkpointState: "clean"
versionStatus: completed
stage10Result:
  stage: "10"
  status: "completed"
  summary: "版本级回归：按用户确认执行【跳过（回归集留空）】。前置检查通过（REQ-01 Stage9 test_passed + 执行证据有效；regressionTestSetId=322337 非空；module-index.json 已构建：config_hash=9bb768be，7 个自动识别 Linkis 模块）。10.2 扫描发现 0 个模块含回归测试集（docs/project-knowledge/testing/regression/ 未构建），按流程空语料跳过出口处理，stage10_explicitly_skipped=true，回归集留空，不执行回归测试循环。"
  artifacts: []
  decisions: ["skip_stage10_no_regression_corpus", "USER_SKIPPED_STAGE10"]
  risks: ["版本级回归集留空：模块回归语料未构建，后续版本需构建 docs/project-knowledge/testing/regression/ 后执行版本级回归"]
  contextUpdates:
    versionRegressionResult: "not_executed"
    versionRegressionFinal: false

versionName: "LinkisV2.1.5"
productId: 100484
productName: "BDP_Linkis"
subsystemId: 5435
subsystemName: "WDSL-UJES"
subsystemVersionId: "2.1.5"
testSetId: "322336"
regressionTestSetId: "322337"
isNeedGrayRelease: 1
grayReleaseDate: "2026-10-27"
branchName: "release_v2.1.5"
pipelineId: null
pipelineVersion: null
requirementContextIndex:
  REQ-01:
    taskName: "REQ-01_修复资源管理资源展示为负问题"
    contextPath: "dev/completed/LinkisV2.1.5/active/REQ-01_修复资源管理资源展示为负问题/context.md"
    currentStage: "9"
    lastCompletedStage: "9"
    contextRevision: 22
    status: "completed"
    checkpointId: "LinkisV2.1.5:REQ-01:9:2026-10-19T09:30:00Z"
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
openRisks: []
stabilityRisksIndex:
  REQ-01:
    riskCount: 3
    highCount: 3
    mediumCount: 0
    lowCount: 0
    riskIds: ["R-001", "R-002", "R-003"]
    updatedAt: "2026-10-10T14:00:00Z"
stage10Result: {}
updatedAt: "2026-10-19T10:30:00Z"
versionSummaryPath: "docs/LinkisV2.1.5/版本总结报告.md"
releaseSchedule:
  version_name: "LinkisV2.1.5"
  timezone: "Asia/Shanghai"
  confirmed: true
  items:
    - id: 1
      environment: "准生产环境"
      cluster: "BDP"
      batch: 1
      release_at: "2026-10-23"
    - id: 2
      environment: "准生产环境"
      cluster: "BDAP"
      batch: 1
      release_at: "2026-10-23"
    - id: 3
      environment: "容灾环境"
      cluster: "BDP"
      batch: 2
      release_at: "2026-10-27"
    - id: 4
      environment: "容灾环境"
      cluster: "BDAP"
      batch: 2
      release_at: "2026-10-27"
    - id: 5
      environment: "生产环境"
      cluster: "BDP"
      batch: 4
      release_at: "2026-11-06"
    - id: 6
      environment: "生产环境"
      cluster: "BDAP"
      batch: 3
      release_at: "2026-10-29"
---

# 版本上下文

本文件由 dev-flow version 主会话维护。禁止 Agent/Skill 直接修改控制字段。
