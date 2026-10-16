---
contextSchemaVersion: "2.1"
contextRevision: 1
versionName: "LinkisV2.1.4"
taskName: "REQ-04_XC改造排除jetty和tomcat包"
reqIndex: 4
reqPrefix: "REQ-04"
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
  surefireReportDir: "linkis-commons/linkis-module/target/surefire-reports/"
  jacocoExec: "linkis-commons/linkis-module/target/jacoco.exec"
  testsRun: 9
  testsPassed: 9
  testsFailed: 0
  testsErrored: 0
  testsSkipped: 14
  buildResult: "BUILD SUCCESS"
  executedAt: "2026-10-10T18:00:00+08:00"
  enterpriseEnv: "DSS-DVS(5902,bes) 已于 SIT 318884 实测通过补录"
testExecutionArtifacts:
  testExecutionReport: "docs/LinkisV2.1.4/testing/reports/REQ-04_BES容器适配改造_测试执行报告.md"
  compileValidation: "13 changed files all compiled successfully"
testReportArtifacts:
  testReport: "docs/LinkisV2.1.4/testing/reports/REQ-04_BES容器适配改造_测试执行报告.md"
  testReportSummary: "9/9 模块测试通过，13个变更文件全部编译通过，企业版14条用例已于SIT 318884实测通过（补录）"
  testResultFinal: "test_passed"
testClarificationPath: "docs/LinkisV2.1.4/testing/REQ-04_BES容器适配改造_测试澄清.md"
testScope:
  mustTest:
    - title: "开源版（保留Jetty）服务启动 + REST API 响应验证"
      ac: "AC1"
      priority: "P0"
      source: "requirement"
    - title: "开源版（保留Jetty）WebSocket 连接 + 消息收发验证"
      ac: "AC1"
      priority: "P0"
      source: "requirement"
    - title: "@ConditionalOnClass 条件加载验证：Jetty在classpath时JettyServerCustomizerConfiguration加载"
      ac: "AC1"
      priority: "P0"
      source: "requirement"
    - title: "企业版（排除Jetty+引入BES）服务启动 + REST API 响应验证"
      ac: "AC2"
      priority: "P0"
      source: "requirement"
    - title: "企业版（排除Jetty+引入BES）WebSocket 连接 + 消息收发验证"
      ac: "AC2"
      priority: "P0"
      source: "requirement"
    - title: "@ConditionalOnClass 条件加载验证：Jetty不在classpath时JettyServerCustomizerConfiguration不加载"
      ac: "AC2"
      priority: "P0"
      source: "requirement"
    - title: "SecurityFilter.filterResponse 中文响应无乱码（getWriter.write）"
      ac: "AC3"
      priority: "P0"
      source: "requirement"
    - title: "EC classpath 追加 BES_PATH 路径验证（BES_PATH 非空时）"
      ac: "AC4"
      priority: "P0"
      source: "requirement"
    - title: "BDPJettyServerHelper.gson/jacksonJson/getControllerServer/addServerEventService API 兼容验证"
      ac: "AC5"
      priority: "P0"
      source: "requirement"
    - title: "TestContextHAManager 编译通过"
      ac: "AC6"
      priority: "P1"
      source: "requirement"
    - title: "BES_PATH 未设置时 EC classpath 不追加，行为不变"
      ac: "AC4"
      priority: "P1"
      source: "requirement"
    - title: "LinkisServletContextInitializer REST DispatcherServlet + SecurityFilter 注册正确性"
      ac: "AC1,AC2"
      priority: "P0"
      source: "code_analysis"
    - title: "LinkisWebSocketEndpoint onOpen/onMessage/onClose/onError 生命周期"
      ac: "AC1,AC2"
      priority: "P0"
      source: "code_analysis"
    - title: "HttpRequestCaptureConfigurator 握手期 HandshakeRequest 捕获"
      ac: "AC1,AC2"
      priority: "P1"
      source: "code_analysis"
    - title: "ControllerServer 去除 Jetty WS 依赖后标准 Session 交互"
      ac: "AC1,AC2"
      priority: "P1"
      source: "code_analysis"
    - title: "ServerSocket 改用 javax.websocket.Session 标准 API"
      ac: "AC1,AC2"
      priority: "P1"
      source: "code_analysis"
    - title: "BDPServerHelper controllerServer/EventBus/services 状态管理"
      ac: "AC5"
      priority: "P1"
      source: "code_analysis"
    - title: "DataWorkCloudApplication 移除 Jetty 专属 main 逻辑后启动正常"
      ac: "AC1,AC2"
      priority: "P0"
      source: "code_analysis"
  optionalTest:
    - title: "BES 容器 WebSocket 大消息传输（接近 maxTextMessageSize）"
      source: "code_analysis"
    - title: "WebSocket 并发连接（多客户端同时连接）"
      source: "code_analysis"
    - title: "WebSocket 断线重连"
      source: "code_analysis"
    - title: "SecurityFilter 非 UTF-8 字符集响应"
      source: "code_analysis"
    - title: "回归测试集联动（现有测试用例全量回归）"
      source: "user_decision"
  notTest:
    - title: "linkis-module pom.xml jetty-websocket 子依赖的 Maven 解析"
      reason: "属于构建验证，非功能测试"
      source: "user_decision"
    - title: "BES 中间件本身的 Servlet/WS 实现正确性"
      reason: "BES 为第三方产品，非本项目被测对象"
      source: "user_decision"
    - title: "Spring Boot 自动配置机制本身"
      reason: "Spring Boot 框架保证，非本项目代码"
      source: "user_decision"
environmentInfo:
  remoteUrl: "开源版: dev-2.1.4-webank 分支构建部署; 企业版: DSS-DVS(5902,bes) 需确认地址"
  availability: "开源版已部署(Stage5完成); 企业版BES环境需确认"
  vpnRequired: "开源版: 否; 企业版: 需确认"
  besPath: "示例 /opt/bes, 实际路径需确认"
artifactIndex:
  requirementDocument: "docs/LinkisV2.1.4/requirements/REQ-04_BES容器适配改造_需求.md"
  designDocument: "docs/LinkisV2.1.4/design/REQ-04_BES容器适配改造_设计.md"
  testClarificationDocument: "docs/LinkisV2.1.4/testing/REQ-04_BES容器适配改造_测试澄清.md"
  codeChangeSummary: "linkis-module BDPJettyServerHelper 重构(-177) + BDPServerHelper(+104) + JettyServerCustomizerConfiguration(+141) + LinkisServletContextInitializer(+119) + LinkisWebSocketEndpoint(+331) + EnvConfiguration 配置 + JavaProcessEngineConnLaunchBuilder 引擎启动脚本适配"
  changedFiles: 13
  devCommitId: "050b7aa55"
  devCommitDesc: "开发阶段：引擎bes改造"
reqSubsystemId: 5902
repositoryId: null
apiDocumentStatus: "not_applicable"
sdlResult: "not_applicable"
sdlEvidence: "XC改造排除jetty/tomcat依赖，BES容器适配，无新增接口、无安全敏感处理（补录）"
openRisks: []
stabilityRisks: []
stabilityRiskTestRequirements: []
updatedAt: "2026-10-08T17:30:00+08:00"
testCaseArtifacts:
  testCaseDoc: "docs/LinkisV2.1.4/testing/REQ-04_BES容器适配改造_测试用例.md"
  wemindImport: "docs/LinkisV2.1.4/testing/REQ-04_BES容器适配改造_测试用例_wemind.json"
  testCaseCount: 23
  mustTestCount: 18
  optionalTestCount: 5
  p0Count: 12
  p1Count: 6
  p2Count: 3
  p3Count: 1
  acCoverage:
    AC1: {testCases: ["TC-001","TC-002","TC-003","TC-012","TC-013","TC-018"], coverage: "100%"}
    AC2: {testCases: ["TC-004","TC-005","TC-006","TC-012","TC-013","TC-018"], coverage: "100%"}
    AC3: {testCases: ["TC-007","TC-022"], coverage: "100%"}
    AC4: {testCases: ["TC-008","TC-011"], coverage: "100%"}
    AC5: {testCases: ["TC-009","TC-017"], coverage: "100%"}
    AC6: {testCases: ["TC-010"], coverage: "100%"}
  generatedAt: "2026-10-08T17:30:00+08:00"
---

# 需求上下文：REQ-04 XC改造排除jetty和tomcat包

## 补录说明

2026-09-24 手动补录：依据 git commit 050b7aa55 回填 devCommitId + status=completed。
2026-10-08 恢复开发：创建 context.md，补 segmentProgress，从 P2 段（Stage 5.5）恢复测试流程。
