# 项目骨架与服务清单

> 本文件是 modules/ 详细文档的"目录索引"——列出 13 个微服务的 **主类入口 + 模块路径**。
> 完整领域概念词典见 [`../CLAUDE.md`](../CLAUDE.md) §9.1；可复用代码模板见 [snippets.md](./snippets.md)。
>
> 适用版本：Apache Linkis `dev-2.0.0`（JDK 1.8、Scala 2.11.12 默认 / spark-3 profile 切 2.12.18、Spring Boot 2.7.12）。

---

## 1. 服务交互图

```
上层应用 → Gateway → Entrance → LinkisManager → ECM → EngineConn → 底层引擎
              ↓
         公共增强服务（BML、DataSource、Configuration、CS、JobHistory 等）
```

详细任务执行主线流程见 [`../CLAUDE.md`](../CLAUDE.md) §10.1。

---

## 2. 13 个微服务清单

### 2.1 微服务治理（基础设施层）

| 服务 | 主类入口 | 模块路径 |
| --- | --- | --- |
| Gateway | `org.apache.linkis.gateway.springcloud.LinkisGatewayApplication` | `linkis-spring-cloud-services/linkis-service-gateway/linkis-spring-cloud-gateway` |
| Eureka | `org.apache.linkis.eureka.SpringCloudEurekaApplication` | `linkis-spring-cloud-services/linkis-service-discovery/linkis-eureka` |

详细文档：[gateway.md](./modules/microservice-governance/gateway.md)、[eureka.md](./modules/microservice-governance/eureka.md)、[monitor.md](./modules/microservice-governance/monitor.md)。

### 2.2 计算治理（核心业务层）

| 服务 | 主类入口 | 模块路径 |
| --- | --- | --- |
| Entrance | `org.apache.linkis.entrance.LinkisEntranceApplication` | `linkis-computation-governance/linkis-entrance` |
| LinkisManager | `org.apache.linkis.manager.LinkisManagerApplication` | `linkis-computation-governance/linkis-manager/linkis-application-manager` |
| ECM | `org.apache.linkis.ecm.server.LinkisECMApplication` | `linkis-computation-governance/linkis-engineconn-manager/linkis-engineconn-manager-server` |
| EngineConn | `org.apache.linkis.engineconn.LinkisEngineConnApplication` | `linkis-computation-governance/linkis-engineconn` |
| JobHistory | `org.apache.linkis.jobhistory.LinkisJobHistoryApp` | `linkis-public-enhancements/linkis-jobhistory` |

详细文档：[entrance.md](./modules/computation-governance/entrance.md)、[manager.md](./modules/computation-governance/manager.md)、[ecm.md](./modules/computation-governance/ecm.md)、[computation-governance/jobhistory.md](./modules/computation-governance/jobhistory.md)。

### 2.3 公共增强（支撑服务层）

| 服务 | 主类入口 | 模块路径 |
| --- | --- | --- |
| PublicService | `org.apache.linkis.filesystem.LinkisPublicServiceApp` | `linkis-public-enhancements/linkis-pes-publicservice` |
| BML | `org.apache.linkis.bml.LinkisBMLApplication` | `linkis-public-enhancements/linkis-bml-server` |
| DataSource | `org.apache.linkis.metadata.LinkisDataSourceApplication` | `linkis-public-enhancements/linkis-datasource` |
| Configuration | `org.apache.linkis.configuration.LinkisConfigurationApp` | `linkis-public-enhancements/linkis-configuration` |
| ContextServer | `org.apache.linkis.cs.server.LinkisCSApplication` | `linkis-public-enhancements/linkis-cs-server` |

详细文档：[publicservice.md](./modules/public-enhancements/publicservice.md)、[bml.md](./modules/public-enhancements/bml.md)、[datasource.md](./modules/public-enhancements/datasource.md)、[configuration.md](./modules/public-enhancements/configuration.md)、[context.md](./modules/public-enhancements/context.md)、[public-enhancements/jobhistory.md](./modules/public-enhancements/jobhistory.md)。

---

## 3. Java / Scala 选型建议

| 场景 | 语言 |
| --- | --- |
| REST API、Service、Entity、Mapper、配置类 | **Java** |
| RPC（Sender/Receiver）、计算逻辑、复杂业务、配置对象（`*Configuration.scala`） | **Scala** |

混合开发，同模块可同时含 `.java` 和 `.scala`。

---

## 4. 统一约定速查

| 类 | 用途 | 包名 |
| --- | --- | --- |
| `Message` | REST 统一返回体 | `org.apache.linkis.server.Message` |
| `LinkisException` 及子类 | 业务异常（带 errCode、desc） | `org.apache.linkis.common.exception` |
| `CommonVars` | 配置项声明 | `org.apache.linkis.common.conf.CommonVars` |
| `ModuleUserUtils` | 取当前操作用户 | `org.apache.linkis.server.utils.ModuleUserUtils` |
| `Utils` | tryCatch / classForName / defaultScheduler | `org.apache.linkis.common.utils.Utils` |

详细用法见 [snippets.md](./snippets.md) 对应章节。
