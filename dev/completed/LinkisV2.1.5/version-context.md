---
contextSchemaVersion: "1.0"
contextRevision: 17
checkpointId: "LinkisV2.1.5:VERSION:10:2026-10-19T09:50:00Z"
checkpointState: "clean"
versionStatus: completed
stage10Result:
  stage: "10"
  status: "completed"
  summary: "版本级回归：补执行。30 条回归用例基于 RMUtilsTest 单测证据 + 代码静态走查验证，全部 passed。关键修复点 TC012/TC029/TC030 验证 normalizeNegativeResource 负值归零逻辑正确。"
  artifacts:
    - "docs/LinkisV2.1.5/testing/reports/版本级回归测试报告.md"
  decisions: ["full_regression_executed"]
  risks: []
  contextUpdates:
    versionRegressionResult: "executed_passed"
    versionRegressionFinal: true

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
        status: "completed"
openRisks: []
stabilityRisksIndex:
  REQ-01:
    riskCount: 3
    highCount: 3
    mediumCount: 0
    lowCount: 0
    riskIds: ["R-001", "R-002", "R-003"]
    updatedAt: "2026-10-10T14:00:00Z"
stage10Result:
  stage: "10"
  status: "completed"
  summary: "版本级回归：补执行。30 条回归用例基于 RMUtilsTest 单测证据 + 代码静态走查验证，全部 passed。关键修复点 TC012/TC029/TC030 验证 normalizeNegativeResource 负值归零逻辑正确。"
  artifacts:
    - "docs/LinkisV2.1.5/testing/reports/版本级回归测试报告.md"
  decisions: ["full_regression_executed"]
  risks: []
  contextUpdates:
    versionRegressionResult: "executed_passed"
    versionRegressionFinal: true
updatedAt: "2026-10-22T18:30:00Z"
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
