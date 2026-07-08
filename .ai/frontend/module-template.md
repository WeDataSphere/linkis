# 前端模块标准模板（直接复制）

> 评估或新增前端管理页面时，按此模板对照。架构背景见 [architecture.md](./architecture.md)。

---

## 1. 标准目录结构

新增一个管理页面（如 `xxxManagement`），目录与文件命名**严格对齐既有 module**：

```
src/apps/<app>/module/<feature>/
├── index.js               # ① 模块注册（暴露 name/component/dispatchs）
├── index.vue              # ② 主页面（搜索条件 + 表格 + 操作按钮）
├── index.scss             # ③ 样式（可选，简单页面可内联）
├── vlist.vue              # ④ 列表组件（iview Table + 列定义）—— 简单页面可并入 index.vue
├── addXxxModal.vue        # ⑤ 新增/编辑弹窗（可选，按需）
├── service.js             # ⑥ API 调用（推荐抽出，便于复用）
└── EditForm/              # ⑦ 复杂表单子组件目录（可选）
```

**已有 module 全部遵循此结构**，参考：
- `apps/linkis/module/udfManager/`（含 EditForm/）
- `apps/linkis/module/globalHistoryManagement/`（含 viewHistory.vue/log.vue）
- `apps/URM/module/udfManagement/`（含 addFunctionModal.vue + 多 Preview 组件）

---

## 2. 模板代码（直接复制改字段名）

### 2.1 `index.js` —— 模块注册

```js
/*! ASF License 头（参考任一现有 .js/.vue 文件） */

export default {
  name: 'XxxManagement',
  dispatchs: {
    // 如果要通知其他模块（如 Workbench），写在这里；不需要就删掉
    // Workbench: ['add'],
  },
  component: () => import('./index.vue'),
};
```

### 2.2 `service.js` —— API 调用

```js
/*! ASF License 头 */

import api from '@/common/service/api';

const baseUrl = '/basedata-manager/xxx';   // URL 前缀对照 architecture.md §3

export const getList = (params) => api.fetch(baseUrl, params, 'get');
export const add = (data) => api.fetch(baseUrl, data, 'post');
export const edit = (data) => api.fetch(baseUrl, data, 'put');
export const del = (id) => api.fetch(`${baseUrl}/${id}`, 'delete');
export const getById = (id) => api.fetch(`${baseUrl}/${id}`, 'get');
```

### 2.3 `index.vue` —— 主页面骨架

```vue
<!-- ASF License 头 -->
<template>
  <div class="xxx-management">
    <SearchBox v-model="searchBar" :columns="searchColumns" @search="search" @reset="reset">
      <Button type="primary" @click="openAdd">{{ $t('message.linkis.add') }}</Button>
    </SearchBox>
    <vlist
      ref="vlist"
      :data="list"
      :loading="loading"
      :total="total"
      :page-size="pageSize"
      :current="page"
      @edit="openEdit"
      @delete="confirmDelete"
      @page-change="handlePageChange"
    />
    <addXxxModal ref="addModal" @success="search" />
  </div>
</template>

<script>
import vlist from './vlist.vue';
import addXxxModal from './addXxxModal.vue';
import { getList } from './service';

export default {
  name: 'XxxManagement',
  components: { vlist, addXxxModal },
  data() {
    return {
      searchBar: { name: '', user: '' },
      searchColumns: [
        { label: this.$t('message.linkis.xxx.name'), key: 'name' },
        { label: this.$t('message.linkis.xxx.user'), key: 'user' },
      ],
      list: [],
      loading: false,
      page: 1,
      pageSize: 20,
      total: 0,
    };
  },
  mounted() {
    this.search();
  },
  methods: {
    async search() {
      this.loading = true;
      try {
        const res = await getList({
          ...this.searchBar,
          page: this.page,
          pageSize: this.pageSize,
        });
        this.list = res.list || [];
        this.total = res.total || 0;
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.searchBar = { name: '', user: '' };
      this.page = 1;
      this.search();
    },
    handlePageChange(page) {
      this.page = page;
      this.search();
    },
    openAdd() {
      this.$refs.addModal.open();
    },
    openEdit(row) {
      this.$refs.addModal.open(row);
    },
    confirmDelete(row) {
      this.$Modal.confirm({
        title: this.$t('message.linkis.deleteConfirm'),
        onOk: async () => {
          await del(row.id);
          this.$Message.success(this.$t('message.linkis.deleteSuccess'));
          this.search();
        },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import './index.scss';
</style>
```

### 2.4 `vlist.vue` —— 列表组件骨架

```vue
<!-- ASF License 头 -->
<template>
  <div class="xxx-vlist">
    <Table :data="data" :columns="columns" :loading="loading" border />
    <Page
      :total="total"
      :page-size="pageSize"
      :current="current"
      show-total
      show-elevator
      @on-change="(p) => $emit('page-change', p)"
    />
  </div>
</template>

<script>
export default {
  name: 'XxxVlist',
  props: {
    data: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    total: { type: Number, default: 0 },
    pageSize: { type: Number, default: 20 },
    current: { type: Number, default: 1 },
  },
  data() {
    return {
      columns: [
        { title: this.$t('message.linkis.xxx.name'), key: 'name' },
        { title: this.$t('message.linkis.xxx.user'), key: 'user' },
        { title: this.$t('message.linkis.createTime'), key: 'createTime' },
        {
          title: this.$t('message.linkis.action'),
          key: 'action',
          render: (h, params) => (
            <div>
              <Button size="small" on-click={() => this.$emit('edit', params.row)}>
                {this.$t('message.linkis.edit')}
              </Button>
              <Button size="small" type="error" on-click={() => this.$emit('delete', params.row)}>
                {this.$t('message.linkis.delete')}
              </Button>
            </div>
          ),
        },
      ],
    };
  },
};
</script>
```

### 2.5 `addXxxModal.vue` —— 弹窗骨架

```vue
<!-- ASF License 头 -->
<template>
  <Modal
    v-model="visible"
    :title="isEdit ? $t('message.linkis.edit') : $t('message.linkis.add')"
    :loading="submitting"
    @on-ok="submit"
    @on-cancel="cancel"
  >
    <Form ref="form" :model="form" :rules="rules" :label-width="100">
      <FormItem :label="$t('message.linkis.xxx.name')" prop="name">
        <Input v-model="form.name" />
      </FormItem>
      <!-- 其他字段 -->
    </Form>
  </Modal>
</template>

<script>
import { add, edit } from './service';

export default {
  name: 'AddXxxModal',
  data() {
    return {
      visible: false,
      submitting: false,
      isEdit: false,
      form: { id: null, name: '' },
      rules: {
        name: [{ required: true, message: this.$t('message.linkis.xxx.nameRequired'), trigger: 'blur' }],
      },
    };
  },
  methods: {
    open(row) {
      this.isEdit = !!row;
      this.form = row ? { ...row } : { id: null, name: '' };
      this.visible = true;
      this.$nextTick(() => this.$refs.form.resetFields());
    },
    async submit() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) {
          this.submitting = false;
          return;
        }
        try {
          if (this.isEdit) {
            await edit(this.form);
          } else {
            await add(this.form);
          }
          this.$Message.success(this.$t('message.linkis.saveSuccess'));
          this.$emit('success');
          this.visible = false;
        } finally {
          this.submitting = false;
        }
      });
    },
    cancel() {
      this.visible = false;
    },
  },
};
</script>
```

---

## 3. 注册路由（必做！）

新增 module **必须**在 `src/apps/<app>/router.js` 注册：

```js
export default [
  // ... existing routes
  {
    name: 'xxxManagement',
    path: 'xxxManagement',
    component: () => import('./module/xxxManagement/index.vue'),
    meta: {
      title: 'xxxManagement',
      publicPage: true,
    },
  },
]
```

---

## 4. 加 i18n（必做！）

在 `src/apps/<app>/i18n/common/zh.json` 和 `en.json` 同步加：

```json
{
  "message": {
    "linkis": {
      "xxx": {
        "name": "名称",
        "nameRequired": "名称不能为空",
        "user": "用户",
        "add": "新增",
        "edit": "编辑",
        "delete": "删除",
        "deleteConfirm": "确认删除？",
        "deleteSuccess": "删除成功",
        "saveSuccess": "保存成功"
      }
    }
  }
}
```

en.json 加同名 key 的英文翻译。

---

## 5. 自检清单

提交前对照：

- [ ] 目录结构与 §1 一致；
- [ ] `index.js` 有 ASF License 头；
- [ ] URL 前缀对照 [architecture.md §3](./architecture.md) 选对后端；
- [ ] 路由已在 `apps/<app>/router.js` 注册；
- [ ] zh.json + en.json 同步更新（**两份都要！**）；
- [ ] 没有引入新的 npm 依赖（升级依赖需 PR 显式说明）；
- [ ] husky lint-staged 跑过（`npm run lint` 无报错）。
