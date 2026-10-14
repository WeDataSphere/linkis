> 石墨文档链接：[点击查看](http://docs.weoa.com/docs/1lq7MwW1YVud0vAe)

# 管理台数据源过期按钮二次确认 设计文档

**需求类型**: ENHANCE（功能增强）
**风险等级**: 低风险
**文档版本**: v1.0
**创建日期**: 2026-09-24
**所属版本**: Linkis 2.1.4（分支 `dev-2.1.4-webank`）
**功能属性**: 前端
**关联需求**: [REQ-03_数据源过期二次确认_需求.md](../requirements/REQ-03_数据源过期二次确认_需求.md)
**关联 commit**: `62662aed5`

---

## 0. 执行摘要

| 维度 | 内容 |
|------|------|
| **改造范围** | linkis-web 单模块（apps/linkis/module/datasource） |
| **核心机制** | iview `<Modal>` + `v-model` 双向绑定 + `overdueConfirm` 响应式状态对象 |
| **后端联动** | 无，复用已有 `expire(id)` API |
| **关键风险** | 🟢 低，纯交互增强，不影响业务链路 |

---

## 1. 设计背景

管理台数据源列表"过期"按钮原行为是点击即调 `expire(id)`，无确认环节，误点击导致数据源被误过期。需增加 Modal 二次确认。

详见需求文档 §1。

---

## 2. 设计方案

### 2.1 组件结构

```vue
<template>
  <!-- 原有列表/分页/表单 -->
  <Modal v-model="overdueConfirm.show"
         :title="$t('message.linkis.datasource.overdueConfirmTitle')">
    <p style="text-align: center">
      {{ $t('message.linkis.datasource.overdueConfirmContent').format(overdueConfirm.name) }}
    </p>
    <div slot="footer">
      <Button type="primary" @click="confirmOverdue">
        {{ $t('message.linkis.datasource.overdueConfirmOk') }}
      </Button>
      <Button @click="overdueConfirm.show = false">
        {{ $t('message.linkis.datasource.overdueConfirmCancel') }}
      </Button>
    </div>
  </Modal>
</template>
```

### 2.2 状态对象

```js
data() {
  return {
    // ... 原有状态
    overdueConfirm: {
      show: false,
      id: null,
      name: '',
    },
  }
}
```

### 2.3 方法改造

**改造前**：
```js
overdue(data) {
  expire(data.id).then(() => {
    this.searchList()
  })
}
```

**改造后**：
```js
overdue(data) {
  // 不直接调接口，先弹确认框
  this.overdueConfirm = {
    show: true,
    id: data.id,
    name: data.dataSourceName,
  }
},

confirmOverdue() {
  this.overdueConfirm.show = false
  expire(this.overdueConfirm.id).then(() => {
    this.$Message.success(this.$t('message.linkis.datasource.overdueSuccess'))
    this.searchList()
  })
},
```

### 2.4 i18n 内容占位符

`overdueConfirmContent` 使用 `{0}` 占位数据源名称，通过 iview/vue 的 `String.prototype.format` 填充：
- zh: `是否过期数据源：{0}？此操作不可恢复!`
- en: `Expire datasource "{0}"? This cannot be undone!`

调用方式：`$t('...').format(overdueConfirm.name)`

---

## 3. 关键代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `datasource/index.vue` | template 末尾 | 新增 Modal 组件 |
| `datasource/index.vue` | data | 新增 `overdueConfirm` |
| `datasource/index.vue` | methods.overdue | 改为设置 confirm 状态 |
| `datasource/index.vue` | methods.confirmOverdue | 新增确认方法 |
| `zh.json` | datasource 节 | 新增 5 个 key |
| `en.json` | datasource 节 | 新增 5 个 key |

---

## 4. 异常与降级

| 场景 | 处理 |
|------|------|
| `expire` 接口失败 | axios 拦截器统一处理错误提示，Modal 已关闭，列表不刷新 |
| 数据源名称为空 | Modal 内容仍显示，占位符替换为空字符串，不影响确认流程 |

---

## 5. 兼容性与影响

| 维度 | 影响 |
|------|------|
| 后端 API | 🟢 无改动 |
| 路由 | 🟢 无新增 |
| i18n | 🟡 新增 5 个 key，zh/en 同步 |
| husky lint | 🟢 代码符合规范 |

---

## 6. 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|:--------:|------|
| `datasource/index.vue` | 修改 | Modal + overdueConfirm + confirmOverdue + overdue 改造 |
| `zh.json` | 修改 | 5 个 i18n key |
| `en.json` | 修改 | 5 个 i18n key |

---

## 7. 测试要点

1. **AC1** 点击过期按钮弹 Modal，含数据源名称；
2. **AC2** 取消不调接口；
3. **AC3** 确认调 expire + 成功提示 + 刷新列表；
4. **AC4** 中英文切换正常显示。
