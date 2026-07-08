# apps/URM/module/ 模块清单（2 个核心 + header）

> URM（UDF & Function Resource）App。架构与映射见 [architecture.md](./architecture.md)。
>
> **关键**：URM 一个后端（linkis-udf-service）支撑两个前端页面，靠 `udfType` 字段区分。

---

## 1. 两个核心 module

### 1.1 udfManagement/ —— UDF 管理（终端用户）

| 文件 | 用途 |
| --- | --- |
| `index.vue` | 主页面，搜索/列表/操作（含 udfType 筛选） |
| `index.js` | 模块注册（name: `UdfManagemet`） |
| `vlist.vue` | 列表组件（iview Table） |
| `addFunctionModal.vue` | 新增/编辑 UDF 弹窗 |
| `jarPreview.vue` / `pyPreview.vue` / `scalaPreview.vue` / `usePreview.vue` | 4 种 UDF 类型预览 |
| `index.scss` | 样式 |

**后端接口**（统一前缀 `/udf/*`，对应 linkis-udf-service）：
- `GET /udf/managerPages` —— 分页查询
- `POST /udf/add` —— 新增
- `PUT /udf/update` —— 修改
- `POST /udf/delete/{id}` —— 删除
- `POST /udf/handover` —— 转交
- `POST /udf/setExpire` —— 设置过期
- `POST /udf/shareUDF` + `POST /udf/getSharedUsers` —— 共享

**udfType 取值**（在 v1 前端代码里可见）：
- `0`：通用（common）
- `1`：Spark
- `2`：（其他引擎，前端按 `engineType: [1,2].includes(...) ? 'spark' : '*'` 判断）

### 1.2 functionManagement/ —— 函数管理（终端用户）

文件结构与 udfManagement **完全平行**（addFunctionModal.vue / vlist.vue / 4 个 Preview / index.vue）。

**关键差异**：
- 复用同一组 `/udf/*` 接口，但传 `udfType=3`（python）或 `udfType=4`（scala）；
- 列表只展示函数（不展示 UDF），靠后端按 udfType 过滤；
- 前端列表展示 `it.udfTypeText = it.udfType === 3 ? 'python' : 'scala'`。

### 1.3 header/ —— URM 顶栏

普通导航组件，无后端调用。

---

## 2. ★ udfType 完整取值表（**评估需求时必看**）

> 决定"UDF 管理"和"函数管理"如何分别扩展功能。

| udfType | 含义 | 所属前端 | 业务概念 |
| --- | --- | --- | --- |
| 0 | 通用 UDF | udfManagement | 通用 UDF（common） |
| 1 | Spark UDF | udfManagement | Spark 引擎 UDF |
| 2 | 其他引擎 UDF | udfManagement | 其他引擎（前端归到 spark 引擎判断） |
| 3 | Python 函数 | functionManagement | Python 函数 |
| 4 | Scala 函数 | functionManagement | Scala 函数 |

**两个 module 共享一个后端、共享一组接口**——给 URM 加功能（如"管理员查看"）几乎一定**两个 module 都要改**。

---

## 3. 典型需求评估

| 需求 | 影响范围 |
| --- | --- |
| 给 UDF 加"管理员查看"入口 | udfManagement + functionManagement（同步改），可能还要改 udfManager（linkis app，管理员视角已有） |
| UDF/函数加"按用户筛选" | 两个 module 的 index.vue 同时改 searchBar + 后端 `/udf/managerPages` 加 user 参数 |
| 新增 UDF 类型 | 改 udfType 枚举（前后端）+ addFunctionModal.vue + 后端 linkis-udf-service |
| 共享逻辑调整 | 两个 module 的 vlist.vue 共享同一段代码（注意同步） |

---

## 4. 参考实现位置

- 前端共享 API：`apps/URM/module/{udfManagement,functionManagement}/index.vue`（直接 `api.fetch('/udf/...')`，未抽 service.js）
- 后端入口：`linkis-udf/linkis-udf-service/src/main/java/org/apache/linkis/udf/...`（按 udfType 路由）
- udfType 枚举：后端 `UdfType.java` 或类似常量类
