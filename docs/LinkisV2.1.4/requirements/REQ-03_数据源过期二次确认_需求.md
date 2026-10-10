# 管理台数据源过期按钮二次确认 需求文档

**需求类型**: ENHANCE（功能增强）
**风险等级**: 低风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 前端
**关联 commit**: `62662aed5`

---

## 📋 需求速览

| 维度 | 内容 |
|-----|------|
| **一句话概述** | 管理台数据源列表"过期"按钮点击后，先弹出二次确认 Modal，确认后才调用 expire 接口，避免误操作 |
| **改动服务** | linkis-web（前端单模块 `apps/linkis/module/datasource`） |
| **改动范围** | `datasource/index.vue` + i18n `zh.json`/`en.json` |
| **后端联动** | 无（复用已有 `expire` 接口） |
| **开关** | 无（纯交互增强，默认生效） |

---

## 1. 需求背景

### 1.1 业务背景

管理台数据源列表页提供"过期"按钮，点击即调用 `expire(id)` 接口使数据源失效。现网反馈该操作**无二次确认**，运维误点击导致数据源被误过期，影响下游任务。需增加 Modal 二次确认，降低误操作风险。

### 1.2 目标

1. 点击"过期"按钮不直接调接口，先弹 Modal 确认；
2. Modal 展示数据源名称，提示"此操作不可恢复"；
3. 确认按钮调用 expire，取消按钮关闭 Modal；
4. 成功后提示"数据源已过期"并刷新列表。

---

## 2. 用户故事

| 编号 | 用户故事 |
|------|---------|
| US-1 | 作为**平台运维**，我希望点击"过期"按钮后先弹出确认框，避免误点击导致数据源被误过期 |
| US-2 | 作为**平台运维**，希望确认框显示数据源名称，明确知道我在过期哪个数据源 |

---

## 3. 功能描述

### 3.1 功能点详述

| 编号 | 功能点 | 描述 |
|------|--------|------|
| F-1 | 二次确认 Modal | `datasource/index.vue` 新增 `<Modal v-model="overdueConfirm.show">`，标题 `overdueConfirmTitle`，内容 `overdueConfirmContent`（含数据源名称占位 `{0}`） |
| F-2 | 状态管理 | 新增 `overdueConfirm: { show, id, name }` 响应式对象 |
| F-3 | overdue 方法改造 | 点击"过期"按钮时不再直接调 `expire`，改为设置 `overdueConfirm` 并显示 Modal |
| F-4 | confirmOverdue 方法 | 确认按钮触发：关闭 Modal → 调 `expire(overdueConfirm.id)` → 成功后 `$Message.success` → 刷新列表 |
| F-5 | i18n 双语 | zh.json/en.json 同步新增 `overdueConfirmTitle/overdueConfirmContent/overdueConfirmOk/overdueConfirmCancel/overdueSuccess` |

### 3.2 关键代码锚点

| 文件 | 行号 | 说明 |
|------|------|------|
| `datasource/index.vue` | template | 新增 Modal 组件 |
| `datasource/index.vue` | data | 新增 `overdueConfirm` 对象 |
| `datasource/index.vue` | methods.overdue | 改为设置 confirm 状态 |
| `datasource/index.vue` | methods.confirmOverdue | 新增确认处理方法 |
| `zh.json` | datasource 节 | 新增 5 个 i18n key |
| `en.json` | datasource 节 | 新增 5 个 i18n key |

---

## 4. 验收标准

### AC1: 点击过期按钮弹出确认框

**前置条件**: 数据源列表有数据

**操作步骤**:
1. 点击某数据源的"过期"按钮

**预期结果**:
- 弹出 Modal，标题"确认过期数据源"
- 内容"是否过期数据源：{数据源名称}？此操作不可恢复!"
- 按钮："确认" + "取消"

### AC2: 取消不调接口

**操作步骤**:
1. 点击"过期"按钮 → 弹出确认框
2. 点击"取消"

**预期结果**:
- Modal 关闭
- 不调用 `expire` 接口
- 数据源列表不变

### AC3: 确认调用 expire 并刷新

**操作步骤**:
1. 点击"过期"按钮 → 弹出确认框
2. 点击"确认"

**预期结果**:
- Modal 关闭
- 调用 `expire(id)` 接口
- 成功后提示"数据源已过期"
- 列表刷新，该数据源状态变为过期

### AC4: 中英文切换

**操作步骤**:
1. 切换语言为英文
2. 点击"过期"按钮

**预期结果**:
- Modal 标题"Confirm Expire Datasource"
- 内容 `Expire datasource "{name}"? This cannot be undone!`
- 按钮："Confirm" + "Cancel"

---

## 5. 配置说明

无新增配置。纯前端交互增强。

---

## 6. 风险识别

| 风险 | 等级 | 说明 |
|------|:----:|------|
| 误操作仍可能发生 | 🟢 | 二次确认后仍可能误确认，属用户责任 |
| i18n 遗漏 | 🟢 | 已同步 zh/en，husky lint 会校验 |

---

## 7. 关联影响分析

| 维度 | 影响 |
|------|------|
| 功能模块 | 🟢 仅前端 datasource module |
| 后端 | 🟢 无改动，复用已有 `expire` 接口 |
| 数据模型 | 🟢 无 |
| 性能 | 🟢 无 |

---

## 附录

### A.1 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `linkis-web/src/apps/linkis/module/datasource/index.vue` | 修改 | 新增 Modal + overdueConfirm 状态 + confirmOverdue 方法 + overdue 方法改造 |
| `linkis-web/src/apps/linkis/i18n/common/zh.json` | 修改 | 新增 5 个 i18n key |
| `linkis-web/src/apps/linkis/i18n/common/en.json` | 修改 | 新增 5 个 i18n key |

### A.2 i18n key 清单

| Key | 中文 | 英文 |
|-----|------|------|
| `overdueConfirmTitle` | 确认过期数据源 | Confirm Expire Datasource |
| `overdueConfirmContent` | 是否过期数据源：{0}？此操作不可恢复! | Expire datasource "{0}"? This cannot be undone! |
| `overdueConfirmOk` | 确认 | Confirm |
| `overdueConfirmCancel` | 取消 | Cancel |
| `overdueSuccess` | 数据源已过期 | Datasource expired |

### A.3 关联文档

- 设计文档: [REQ-03_数据源过期二次确认_设计.md](../design/REQ-03_数据源过期二次确认_设计.md)
