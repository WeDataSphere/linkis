# AI 协作深度参考库

> 本目录是 CLAUDE.md 的深度参考补充。**主入口仍是仓库根目录的 [`../CLAUDE.md`](../CLAUDE.md)**——AI 协作前应先读 CLAUDE.md。
>
> 适用版本：Apache Linkis `dev-2.0.0`（`<revision>2.0.0</revision>`）。

---

## 何时读哪个文件

| 我要做什么 | 读哪个文件 |
| --- | --- |
| 上手本项目开发 | [`../CLAUDE.md`](../CLAUDE.md)（必读） |
| 找可复用代码模板（CommonVars、Utils.tryCatch、Label、RPC、拦截器、EngineConnPlugin、Restful、MyBatis、**前端模板**） | [snippets.md](./snippets.md) |
| 看常见踩坑与反例（字符编码、开关、表结构、异常、日志、REST 返回、SQL 注入、事务） | [common-pitfalls.md](./common-pitfalls.md) |
| 看数据库修改 / 配置管理专属规则 | [rules.md](./rules.md) |
| 看某个微服务的 API / 表结构 / RPC 协议 | [modules/](./modules/) 下对应文档 |
| **评估 / 新增前端需求**（4 个 App 架构、模块清单、复制模板） | [frontend/](./frontend/) 下文档 |

---

## 文档索引

### 核心参考
| 文档 | 用途 |
| --- | --- |
| [snippets.md](./snippets.md) | 12 类可复用代码模板（配置开关、异常降级、日志、任务参数、Label、RPC、拦截器、EngineConnPlugin、REST、MyBatis、用户角色、任务关键信息） |
| [common-pitfalls.md](./common-pitfalls.md) | 8 个高频踩坑（字符编码 / 功能开关 / 表结构变更 / 异常处理 / 日志 / REST 返回体 / SQL 注入 / 事务） |
| [rules.md](./rules.md) | 数据库修改原则、配置管理规则、代码边界约束 |

### 模块文档（参考用，**以源码为最终准**）
> ⚠️ 模块文档为 AI 辅助生成的快照，可能滞后于源码。遇到不一致以源码为准。

#### 计算治理（核心业务层）
- [entrance.md](./modules/computation-governance/entrance.md) — 任务提交入口
- [manager.md](./modules/computation-governance/manager.md) — 资源/应用管理（AM/RM/LM）
- [ecm.md](./modules/computation-governance/ecm.md) — 引擎连接管理
- [jobhistory.md](./modules/computation-governance/jobhistory.md) — 任务历史

#### 微服务治理（基础设施层）
- [gateway.md](./modules/microservice-governance/gateway.md) — API 网关
- [eureka.md](./modules/microservice-governance/eureka.md) — 服务注册发现
- [monitor.md](./modules/microservice-governance/monitor.md) — 监控

#### 公共增强（支撑服务层）
- [publicservice.md](./modules/public-enhancements/publicservice.md) — 公共服务
- [configuration.md](./modules/public-enhancements/configuration.md) — 配置管理
- [bml.md](./modules/public-enhancements/bml.md) — 物料库
- [datasource.md](./modules/public-enhancements/datasource.md) — 数据源
- [context.md](./modules/public-enhancements/context.md) — 上下文服务
- [jobhistory.md](./modules/public-enhancements/jobhistory.md) — 任务历史（pes 视角）

#### 前端（linkis-web/）
- [frontend/architecture.md](./frontend/architecture.md) — 4 个 App 架构 + 前后端 URL 映射 + 公共组件
- [frontend/module-template.md](./frontend/module-template.md) — 管理页面标准模板（可直接复制）
- [frontend/apps-linkis-modules.md](./frontend/apps-linkis-modules.md) — 30 个 linkis module 清单
- [frontend/apps-urm-modules.md](./frontend/apps-urm-modules.md) — UDF/函数管理详解（含 udfType 取值表）

---

## 维护原则

1. 本目录**不重复** CLAUDE.md 的内容——核心规则、依赖约束、铁律都在 CLAUDE.md。
2. 代码模板进 `snippets.md`；踩坑反例进 `common-pitfalls.md`；领域专属规则进 `rules.md`。
3. 修改任何文件时同步更新本索引。
