# apps/linkis/module/ 模块清单（30 个）

> 主管理后台的所有前端模块。架构与映射见 [architecture.md](./architecture.md)。
> **如需新增管理页面，先在这里查重名，再决定路径**。

---

## 数据源 / 元数据域

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `datasource/` | `/datasource/*` | linkis-datasource | 数据源 CRUD + 元数据查询 |
| `datasourceAccess/` | `/basedata-manager/datasource-access` | linkis-basedata-manager | 数据源访问权限 |
| `datasourceEnv/` | `/basedata-manager/datasource-env` | linkis-basedata-manager | 数据源环境配置 |
| `datasourceType/` | `/basedata-manager/datasource-type` | linkis-basedata-manager | 数据源类型字典 |
| `datasourceTypeKey/` | `/basedata-manager/datasource-type-key` | linkis-basedata-manager | 类型对应 key 字典 |

## 历史与任务域

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `globalHistoryManagement/` | `/jobhistory/*`、`/configuration/engineType` | linkis-jobhistory | 全局任务历史 + 详情 + 日志（viewHistory.vue/log.vue） |
| `codeQuery/` | — | linkis-jobhistory | 代码查询 |

## 资源与引擎治理域

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `resourceManagement/` | `/linkisManager/rm/*` | linkis-resource-manager | 用户/引擎资源 |
| `ECM/` | `/linkisManager/listAllEMs` 等 | linkis-application-manager | ECM 列表 + 健康状态 |
| `eurekaService/` | Eureka REST | linkis-eureka（直连） | 服务实例列表 |
| `microServiceManagement/` | Eureka REST | linkis-eureka | 微服务管理 |
| `EnginePluginManagement/` | `/linkisManager/listEngineTypePlugins` 等 | linkis-application-manager | 引擎插件管理 |
| `engineConfigurationTemplate/` | `/basedata-manager/configuration-template` | linkis-basedata-manager | 引擎配置模板 |
| `pythonModule/` | `/basedata-manager/*` | linkis-basedata-manager | Python 模块管理 |

## UDF / 函数域（管理员视角）

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `udfManager/` | `/basedata-manager/udf-manager` | linkis-basedata-manager | **管理员视角**的 UDF 管理（≠ URM 的 udfManagement） |
| `udfTree/` | `/basedata-manager/udf-tree` | linkis-basedata-manager | UDF 分类树 |

## 配置与标签域

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `configManagement/` | `/configuration/*` | linkis-configuration | 全局配置 |
| `userConfig/` | `/configuration/keyvalue` 等 | linkis-configuration | 用户级配置 |
| `setting/` | `/configuration/*` | linkis-configuration | 系统设置 |
| `globalValiable/` | `/configuration/globalValiable` | linkis-configuration | 全局变量 |
| `tenantTagManagement/` | `/basedata-manager/*` | linkis-basedata-manager | 租户标签 |
| `departmentTagManagement/` | `/basedata-manager/*` | linkis-basedata-manager | 部门标签 |
| `ipListManagement/` | `/basedata-manager/*` | linkis-basedata-manager | IP 白名单/黑名单 |
| `acrossClusterRule/` | `/basedata-manager/*` | linkis-basedata-manager | 跨集群规则 |

## 网关与基础

| 模块 | 后端 URL | 后端模块 | 备注 |
| --- | --- | --- | --- |
| `gatewayAuthToken/` | `/basedata-manager/gateway-auth-token` | linkis-basedata-manager | **Token 管理**（涉及动态 token 需求时看这里） |
| `errorCode/` | `/basedata-manager/error-code` | linkis-basedata-manager | 错误码字典 |
| `rmExternalResourceProvider/` | `/basedata-manager/rm-external-resource-provider` | linkis-basedata-manager | 外部资源提供方（YARN/K8s） |
| `FAQ/` | — | 静态 | FAQ 页面 |
| `header/` | — | 公共 | 顶栏 |

---

## 评估提示

- 接到"管理员视角 UDF/函数管理"需求 → 改 `udfManager/`（基于 linkis-basedata-manager）
- 接到"终端用户 UDF/函数管理"需求 → 改 `apps/URM/module/{udfManagement,functionManagement}/`（基于 linkis-udf-service）
- 接到"任务详情扩展字段"需求 → 改 `globalHistoryManagement/{index.vue,viewHistory.vue,log.vue}` + 后端 linkis-jobhistory
- 接到"网关 token 管理"需求 → 改 `gatewayAuthToken/` + 后端 linkis-basedata-manager
