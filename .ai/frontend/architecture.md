# 前端架构速查（linkis-web/）

> 本文件解决"前端需求评估不准"的根因——AI 没有 linkis-web 的整体心智模型。
> 通用约定（构建命令、husky、不升级大版本）见 [`../../CLAUDE.md`](../../CLAUDE.md) §3.2。
>
> 完整模块模板见 [module-template.md](./module-template.md)；按 App 拆分的模块清单见
> [apps-linkis-modules.md](./apps-linkis-modules.md)、[apps-urm-modules.md](./apps-urm-modules.md)。

---

## 1. 技术栈与目录

```
linkis-web/
├── src/
│   ├── apps/                  # ★ 子应用（每个 = 一组前端页面）
│   │   ├── linkis/            # 主管理后台（30 个 module）
│   │   ├── URM/               # UDF 与函数管理（2 个 module）
│   │   ├── scriptis/          # 脚本编辑（本分支仅 webSocket/）
│   │   └── PythonModule/      # Python 引擎前端（独立 React 子项目，非 Vue）
│   ├── common/                # 公共代码
│   │   ├── service/api.js     # ★ 统一 axios 实例 + api.fetch
│   │   ├── i18n/              # 全局国际化（zh.json/en.json）
│   │   └── util.js
│   ├── components/            # 公共组件（20+，见 §4）
│   ├── router.js              # 顶层路由（聚合各 App router.js）
│   └── apps/<app>/router.js   # App 内部路由
└── package.json
```

技术栈：Vue `2.6.12` + iview `3.5.4` + vue-router `3.4.8` + vue-i18n `8.22.1` + axios `1.12.2`。

---

## 2. ★ 4 个子应用对照

> **关键**：前端按"业务域"拆 App，**不同 App 对应不同后端服务**。评估需求时先定位 App，再定位 module。

| App | 路径 | 用途 | module 数 | 后端服务 | 路由前缀 |
| --- | --- | --- | --- | --- | --- |
| **linkis/** | `src/apps/linkis/` | 主管理后台（资源、历史、引擎、数据源、各类管理员页） | 30 | 多后端混用 | `/console/...` |
| **URM/** | `src/apps/URM/` | UDF 管理 + 函数管理 | 2 | linkis-udf-service（基于 udfType 区分） | `/urm/...` |
| **scriptis/** | `src/apps/scriptis/` | 脚本编辑、工作台（本分支仅 webSocket 模块） | 1 | entrance | — |
| **PythonModule/** | `src/apps/PythonModule/` | Python 引擎前端（**独立 React 项目**，与主 Vue 体系不共享 iview/i18n） | — | engineconn-python | — |

**最容易踩坑**：
- `apps/linkis/module/udfManager/` 与 `apps/URM/module/udfManagement/` **是两个完全不同的页面**：
  - 前者走 `/basedata-manager/udf-manager/*`（基于 linkis-basedata-manager，管理员视角）
  - 后者走 `/udf/*`（基于 linkis-udf-service，终端用户视角，按 `udfType` 区分 UDF/函数）
- URM 一个后端（linkis-udf-service）支撑 **两个前端页面**（udfManagement + functionManagement），靠 `udfType` 字段区分（0/1/2=UDF；3/4=函数）。

---

## 3. ★ 前后端 URL 映射表

> 看到前端 service.js 里的 baseURL，立即知道打到哪个后端。

| 前端 URL 前缀 | 后端模块 | 后端 Restful 入口包 |
| --- | --- | --- |
| `/basedata-manager/*` | linkis-basedata-manager | `linkis-basedata-manager/src/main/java/.../app/` |
| `/udf/*` | linkis-udf-service | `linkis-udf/src/main/java/.../udf/`（按 udfType 路由） |
| `/configuration/*` | linkis-configuration | `linkis-configuration/.../restful/` |
| `/jobhistory/*` | linkis-jobhistory | `linkis-jobhistory/.../restful/` |
| `/linkisManager/*` | linkis-application-manager | `linkis-manager/linkis-application-manager/.../restful/` |
| `/linkisManager/rm/*` | linkis-resource-manager | 同上 |
| `/datasource/*` | linkis-datasource | `linkis-datasource/.../restful/` |
| `/filesystem/*` | linkis-pes-publicservice | `linkis-pes-publicservice/.../filesystem/` |
| `/dss/framework/workspace/*` | DSS 项目（外部，不在本仓库） | — |
| `/api/rest_j/v1/<...>` | 经 Gateway 路由 | `linkis-mg-gateway` |

**axios baseURL 默认**：`${origin}/api/rest_j/v1/`（见 `src/common/service/api.js`），所以 `service.js` 里写 `/udf/...` 实际打到 `/api/rest_j/v1/udf/...`，由 Gateway 转给 linkis-udf-service。

---

## 4. 公共组件清单（src/components/）

> 优先复用，不要重复造轮子。

| 组件 | 用途 |
| --- | --- |
| `table/`、`virtualTable/`、`virtualList/` | 列表/虚拟滚动（大数据量用 virtual*） |
| `tree/`、`virtualTree/` | 树形结构（UDF 分类、目录） |
| `editor/` | monaco-editor 封装（代码编辑） |
| `log/` | 日志流式展示（接 logDB） |
| `deleteDialog/`、`directoryDialog/`、`panel/` | 通用弹窗 |
| `tag/`、`svgIcon/`、`circleProgress/` | UI 元件 |
| `navbar/`、`menu/` | 导航 |
| `watermark/` | 水印 |
| `webSocket/`（apps/scriptis） | WebSocket 客户端封装 |

---

## 5. 路由约定

```js
// src/apps/<app>/router.js
export const subAppRoutes = { path: '', name: 'layout', component: () => import('./view/layout.vue') }
export default [
  {
    path: '<url>',
    name: '<Name>',
    component: () => import('./module/<feature>/index.vue'),
    meta: { title: '...', publicPage: true }   // publicPage: true 表示免鉴权
  }
]
```

- 顶层 `src/router.js` 聚合所有 `apps/*/router.js`；
- 新增页面**必须**在对应 App 的 `router.js` 注册，否则前端访问不到；
- `meta.publicPage: true` 的路径会被 Gateway 自动放行（见后端 `wds.linkis.server.user.restful.uri.pass.auth`）。

---

## 6. i18n 三层结构

```
src/common/i18n/zh.json           # 全局通用（log/refresh/submit 等）
src/apps/linkis/i18n/common/zh.json  # App 级（linkis 专属）
src/apps/<app>/i18n/common/en.json  # 对应英文
```

调用方式：`$t('message.linkis.udfName')`，键路径必以 `message.<app>.<key>` 开头。

---

## 7. state 管理（dispatchs）

`module/<feature>/index.js` 可声明 `dispatchs`，让其他模块监听本模块动作：

```js
export default {
  name: 'GlobalHistory',
  dispatchs: { Workbench: ['add'] },   // 通知 Workbench 模块执行 add
  component: () => import('./index.vue'),
}
```

新增模块如需与现有模块联动（如"任务详情页"要在"全局历史"打开），通过 `dispatchs` 解耦。

---

## 8. 典型需求评估清单

接到前端需求时**先回答这 5 个问题**：

1. **改哪个 App？** —— linkis / URM / scriptis / PythonModule
2. **改哪个 module？** —— `apps/<app>/module/<feature>/`，先查 [apps-linkis-modules.md](./apps-linkis-modules.md) 看是否已存在
3. **是新增还是扩展？** —— 扩展直接改 index.vue/vlist.vue；新增走 [module-template.md](./module-template.md)
4. **需要新后端接口吗？** —— 看对应后端 URL 前缀（§3），是否要改 RestfulApi / Service / Mapper
5. **是否动 i18n？** —— 新增字段必须同步 zh.json + en.json
