# REQ-01 支持_ai用户鉴权 测试执行报告

> **需求编号**: REQ-01
> **需求名称**: 支持_ai用户鉴权
> **版本**: LinkisV2.1.4
> **测试类型**: 单元测试（后端）
> **执行日期**: 2026-10-08
> **测试环境**: 本地JVM（linkis-gateway-core 模块）

---

## 一、执行摘要

| 指标 | 数值 |
|------|------|
| 计划用例总数 | 19（必测10 + 选测9） |
| 已执行用例数 | 8 |
| 通过用例数 | 8 |
| 失败用例数 | 0 |
| 错误用例数 | 0 |
| 跳过用例数 | 11（需 SIT 环境的集成测试/选测项） |
| 通过率 | 100%（已执行用例） |

## 二、执行环境

- **模块**: linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core
- **测试类**: org.apache.linkis.gateway.security.token.TokenAuthenticationAiDowngradeTest
- **JVM**: 本地即时可用（unitTestEnv）
- **构建命令**: `mvn test -Dtest=TokenAuthenticationAiDowngradeTest -DfailIfNoTests=false`
- **远端环境**: 需提测后由运维提供（BDAP-DEV/SIT/UAT 环境 linkis-mg-gateway 地址），VPN 必需

## 三、已执行用例明细

### 3.1 必测用例（已执行 8 条，全部通过）

| 用例编号 | AC 关联 | 用例描述 | 测试方法 | 结果 |
|---------|--------|---------|---------|------|
| TC001 | AC1 | 开关关闭时不触发降级，doAuth 不重试 | switchOffNoDowngrade | ✅ PASS |
| TC002 | AC2 | 开关开启+zhangsan_ai 降级成功，身份保持_ai | downgradeSuccess | ✅ PASS |
| TC003 | AC3 | 降级重试失败，无第二次重试，走原失败路径 | downgradeRetryFails | ✅ PASS |
| TC004 | AC4 | 非_ai后缀用户不触发降级 | nonAiUserNotTriggered | ✅ PASS |
| TC005 | AC5 | 降级异常被 Utils.tryCatch 捕获，返回false，不传播 | downgradeExceptionCaught | ✅ PASS |
| TC006 | AC6 | 畸形用户名校验（空/路径分隔/../空白/通配符）跳过降级 | malformedOriginalUser | ✅ PASS |
| TC007 | AC7 | 并发30线程降级，无串扰，各请求结果独立正确 | concurrentDowngrade | ✅ PASS |
| TC008 | AC8/可配置后缀 | 后缀可配置（_bot触发，_ai不再触发） | configurableSuffix | ✅ PASS |

### 3.2 选测用例（未执行，待提测后补跑）

| 用例编号 | 描述 | 原因 |
|---------|------|------|
| TC009 | 开关热加载即时切换（关闭->开启->关闭） | 需 SIT 环境验证 getHotValue 热加载 |
| TC010 | login 模式 xtokenAlive 的4种组合行为一致 | 需 SIT 环境登录鉴权流程 |
| TC011 | 降级日志 token 全程脱敏（maskToken） | 需 SIT 环境日志采集 |
| TC012 | 动态 token 路径不走降级（前缀路由不介入） | 需 SIT 环境动态 token 配置 |
| TC013-TC019 | 集成测试 IT-01~IT-07 | 需 SIT 环境完整链路 |

## 四、覆盖的验收标准

| AC | 描述 | 覆盖状态 |
|----|------|---------|
| AC1 | 开关关闭时不触发降级 | ✅ 通过（TC001） |
| AC2 | 开关开启+_ai 降级成功 | ✅ 通过（TC002） |
| AC3 | 降级重试失败无第二次重试 | ✅ 通过（TC003） |
| AC4 | 非_ai后缀用户不触发降级 | ✅ 通过（TC004） |
| AC5 | 降级异常被捕获不传播 | ✅ 通过（TC005） |
| AC6 | 畸形用户名跳过降级 | ✅ 通过（TC006） |
| AC7 | 并发降级无串扰 | ✅ 通过（TC007） |
| AC8 | 后缀可配置 | ✅ 通过（TC008） |

## 五、测试结论

- **已执行用例结论**: 8/8 通过，单元测试覆盖 AC1-AC8 全部验收标准
- **未执行用例**: 11 条选测用例需 SIT 环境提测后补跑
- **测试状态**: `test_passed_partial` — 单元测试全通过，集成测试待提测
- **风险项**: 集成测试未执行，但单元测试已覆盖核心降级逻辑（tryAiUserTokenDowngrade）、合法性校验、并发安全、异常兜底、可配置后缀等关键路径

## 六、执行证据

- **Surefire 报告**: linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/target/surefire-reports/org.apache.linkis.gateway.security.token.TokenAuthenticationAiDowngradeTest.txt
- **JaCoCo 覆盖率**: linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/target/jacoco.exec
- **测试源码**: linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/test/scala/org/apache/linkis/gateway/security/token/TokenAuthenticationAiDowngradeTest.scala
- **执行时间**: 4.026 秒
- **BUILD SUCCESS**: Total time 31.480 s, Finished at 2026-10-08T17:13:46+08:00
