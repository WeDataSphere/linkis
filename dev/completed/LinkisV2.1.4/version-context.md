---
contextSchemaVersion: "1.0"
contextRevision: 3
versionName: "LinkisV2.1.4"
versionStatus: completed
productId: 100484
productName: "BDP_Linkis"
subsystemId: 5435
subsystemName: "WDSL-UJES"
subsystemVersionId: "2.1.4"
testSetId: 318884
regressionTestSetId: 318890
isNeedGrayRelease: 1
grayReleaseDate: "2026-10-15"
branchName: "dev-2.1.4-webank"
pipelineId: null
pipelineVersion: null
requirementContextIndex:
  REQ-01:
    contextPath: "dev/completed/LinkisV2.1.4/active/REQ-01_支持_ai用户鉴权/context.md"
    status: "development"
    devCommitId: "9544feead"
    reqSubsystemId: 5435
    currentStage: "10"
    lastCompletedStage: 10
    testResult: "test_passed"
    segmentProgress:
      P1: "completed"
      S1: "completed"
      P2: "completed"
      S2: "completed"
      D: "completed"
      V: "completed"
  REQ-02:
    contextPath: "dev/completed/LinkisV2.1.4/active/REQ-02_ContextService日志优化/context.md"
    status: "development"
    devCommitId: "38e156050"
    reqSubsystemId: 5435
    currentStage: "10"
    lastCompletedStage: 10
    testResult: "test_passed"
    segmentProgress:
      P1: "completed"
      S1: "completed"
      P2: "completed"
      S2: "completed"
      D: "completed"
      V: "completed"
  REQ-03:
    contextPath: "dev/completed/LinkisV2.1.4/active/REQ-03_管理台数据源失效按钮二次确认/context.md"
    status: "development"
    devCommitId: "62662aed5"
    reqSubsystemId: 5436
    currentStage: "10"
    lastCompletedStage: 10
    testResult: "test_passed"
    segmentProgress:
      P1: "completed"
      S1: "completed"
      P2: "completed"
      S2: "completed"
      D: "completed"
      V: "completed"
  REQ-04:
    contextPath: "dev/completed/LinkisV2.1.4/active/REQ-04_XC改造排除jetty和tomcat包/context.md"
    status: "development"
    devCommitId: "050b7aa55"
    reqSubsystemId: 5902
    currentStage: "10"
    lastCompletedStage: 10
    testResult: "test_passed"
    segmentProgress:
      P1: "completed"
      S1: "completed"
      P2: "completed"
      S2: "completed"
      D: "completed"
      V: "completed"
openRisks: []
stage10Result:
  status: "passed"
  reason: "回归测试集 318890 已执行通过，报告 320339（补录恢复）"
  completedAt: "2026-10-08T18:30:00+08:00"
releaseSchedule:
  version_name: "LinkisV2.1.4"
  timezone: "Asia/Shanghai"
  confirmed: true
  items:
    - {environment: "准生产环境", cluster: "", batch: 1, release_at: "2026-10-20 22:00", id: 1}
    - {environment: "容灾环境", cluster: "", batch: 2, release_at: "2026-10-20 22:00", id: 2}
    - {environment: "生产环境", cluster: "", batch: 3, release_at: "2026-10-21 00:00", id: 3}
versionSummaryPath: "docs/LinkisV2.1.4/版本总结报告.md"
updatedAt: "2026-10-08T18:38:00+08:00"
---

# 版本上下文

本文件由 dev-flow version 主会话维护。禁止 Agent/Skill 直接修改控制字段。

## 补录说明

2026-09-24 手动补录：4 条需求均已开发完成，依据 git commit 历史回填 devCommitId + status=completed。
- REQ-01 支持_ai用户鉴权 → commit 9544feead（mg-gateway）
- REQ-02 ContextService日志优化 → commit 38e156050（ps-cs）
- REQ-03 管理台数据源失效按钮二次确认 → commit 62662aed5（ps-datasource 前端）
- REQ-04 XC改造排除jetty和tomcat包 → commit 050b7aa55（bes）
