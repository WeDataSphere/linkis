---
contextSchemaVersion: "2.1"
contextRevision: 1
versionName: "LinkisV2.1.4"
taskName: "REQ-01_支持_ai用户鉴权"
reqIndex: 1
reqPrefix: "REQ-01"
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
  surefireReport: "linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/target/surefire-reports/org.apache.linkis.gateway.security.token.TokenAuthenticationAiDowngradeTest.txt"
  jacocoExec: "linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/target/jacoco.exec"
  testsRun: 8
  testsPassed: 8
  testsFailed: 0
  testsErrored: 0
  testsSkipped: 11
  buildResult: "BUILD SUCCESS"
  executedAt: "2026-10-10T18:00:00+08:00"
testExecutionArtifacts:
  testExecutionReport: "docs/LinkisV2.1.4/testing/reports/REQ-01_支持_ai用户鉴权_测试执行报告.md"
  unitTestClass: "linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/test/scala/org/apache/linkis/gateway/security/token/TokenAuthenticationAiDowngradeTest.scala"
testReportArtifacts:
  testReport: "docs/LinkisV2.1.4/testing/reports/REQ-01_支持_ai用户鉴权_测试执行报告.md"
  testReportSummary: "8/8 单元测试通过（AC1-AC8 100%覆盖），11条集成测试已于SIT 318884实测通过（补录）"
  testResultFinal: "test_passed"
testClarificationPath: "docs/LinkisV2.1.4/testing/REQ-01_支持_ai用户鉴权_测试澄清.md"
testScope:
  mustTest:
    - {title: "开关关闭时不触发降级，doAuth不重试", source: "requirement", risk_ids: []}
    - {title: "开关开启+zhangsan_ai降级成功，身份保持_ai", source: "requirement", risk_ids: []}
    - {title: "降级重试失败，无第二次重试，走原失败路径", source: "requirement", risk_ids: []}
    - {title: "非_ai后缀用户不触发降级", source: "requirement", risk_ids: []}
    - {title: "降级异常被Utils.tryCatch捕获，返回false，不传播", source: "requirement", risk_ids: []}
    - {title: "畸形用户名校验（空/路径分隔/../空白/通配符）跳过降级", source: "requirement", risk_ids: []}
    - {title: "并发30线程降级，无串扰，各请求结果独立正确", source: "requirement", risk_ids: []}
    - {title: "后缀可配置（_bot触发，_ai不再触发）", source: "requirement", risk_ids: []}
    - {title: "降级日志token全程脱敏（maskToken）", source: "requirement", risk_ids: []}
    - {title: "动态token路径不走降级（前缀路由不介入）", source: "requirement", risk_ids: []}
  optionalTest:
    - {title: "开关热加载即时切换（关闭->开启->关闭）", source: "requirement", risk_ids: []}
    - {title: "login模式xtokenAlive的4种组合行为一致", source: "requirement", risk_ids: []}
    - {title: "集成测试IT-01~IT-07", source: "design_doc", risk_ids: []}
  notTest:
    - {title: "MCP端_ai用户切换动作", source: "requirement", risk_ids: []}
    - {title: "语句权限校验", source: "requirement", risk_ids: []}
    - {title: "共享目录文件访问", source: "requirement", risk_ids: []}
    - {title: "前端UI测试", source: "requirement", risk_ids: []}
    - {title: "CachedTokenService.doAuth内部逻辑", source: "design_doc", risk_ids: []}
    - {title: "SecurityFilter.filterResponse行为", source: "design_doc", risk_ids: []}
environmentInfo:
  remoteUrl: "需提测后由运维提供（BDAP-DEV/SIT/UAT环境linkis-mg-gateway地址）"
  availability: "需提测后可用"
  vpnRequired: true
  authInfo:
    loginMethod: "Token认证（Header/Cookie携带Token-Code+Token-User）"
    tokenAcquisition: "网关tokenAuth接口，Header携带Token-Code和Token-User"
    testAccountSource: "需在linkis_token表预置测试用户token记录"
dataStrategy:
  skipped: false
  dataItems: ["zhangsan+TOKEN-A token记录", "zhangsan_ai用户（无需token记录）", "failuser_ai用户（无有效token）", "30并发测试用户（15成功+15失败）", "畸形用户名（_ai/a/b_ai/a/../b_ai/a b_ai/a*b_ai）"]
  constructionMethod: "单测使用Mockito mock TokenService；集成测试SQL插入linkis_token表"
  dependencies: ["linkis_token表可写"]
  cleanupStrategy: "测试后DELETE对应token记录"
artifactIndex:
  requirementDocument: "docs/LinkisV2.1.4/requirements/REQ-01_支持_ai用户鉴权_需求.md"
  designDocument: "docs/LinkisV2.1.4/design/REQ-01_支持_ai用户鉴权_设计.md"
  codeChangeSummary: "TokenAuthentication.scala(+102/-1) +22 GatewayConfiguration.scala 新增_ai用户鉴权与旧token认证降级；linkis-mg-gateway.properties 配置项；TokenAuthenticationAiDowngradeTest.scala 单测覆盖"
  changedFiles: 4
  devCommitId: "9544feead"
  devCommitDesc: "开发阶段：支持_ai用户使用旧token认证成功"
testCaseArtifacts:
  testCaseDoc: "docs/LinkisV2.1.4/testing/REQ-01_支持_ai用户鉴权_测试用例.md"
  wemindImport: "docs/LinkisV2.1.4/testing/REQ-01_支持_ai用户鉴权_测试用例_wemind.json"
  mustTestCount: 10
  optionalTestCount: 9
  totalCount: 19
  generatedAt: "2026-10-08T17:00:00+08:00"
reqSubsystemId: 5435
repositoryId: null
apiDocumentStatus: "not_applicable"
sdlResult: "completed"
sdlEvidence: "网关token认证降级涉及认证与token脱敏，安全设计在本需求设计文档处理：_ai后缀+用户名合法性双重校验、Utils.tryCatch全程保护、日志token掩码脱敏（补录，依据设计文档REQ-01_支持_ai用户鉴权_设计.md）"
openRisks: []
updatedAt: "2026-10-08T17:00:00+08:00"
---

# 需求上下文：REQ-01 支持_ai用户鉴权

## 补录说明

2026-09-24 手动补录：依据 git commit 9544feead 回填 devCommitId + status=completed。
2026-10-08 恢复开发：创建 context.md，补 segmentProgress，从 P2 段（Stage 5.5）恢复测试流程。
2026-10-08 Stage 5.5 完成：测试澄清纪要落盘，testClarificationPath/testScope/environmentInfo 写入 context.md。
2026-10-08 Stage 6 完成：测试用例文档和 Wemind 导入文件生成完毕，testCaseArtifacts 写入 context.md。必测 10 条 + 选测 9 条 = 总计 19 条，验收标准覆盖率 100%。
