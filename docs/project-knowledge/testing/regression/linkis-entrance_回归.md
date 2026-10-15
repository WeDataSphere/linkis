# linkis-entrance 模块回归测试

## 模块信息

| 项目 | 内容 |
|-----|------|
| 模块ID | linkis-entrance |
| 模块名称 | 任务入口服务 |
| 关键级别 | critical |
| 最后更新 | 2026-07-24 |

## 测试覆盖统计

| 测试类型 | 用例数量 |
|---------|:-------:|
| 功能测试（配置项管理迁移）| 7 |
| **总计** | **7** |

## 涉及的需求

| 需求名称 | 来源分支 | 来源文档 |
|---------|---------|---------|
| spark3-coercion-creator（配置项管理迁移）| dev-2.0.0 | docs/dev-2.0.0/features/spark3-coercion-creator.feature |

## 回归测试用例（配置项管理迁移 — 7 个 @migration 场景）

> 来源：spark3-coercion-creator.feature 的 Rule: 配置项管理迁移。覆盖 RPC 读取 / fallback / 三表 / switch 保留 / 缓存坑。

### TC-MIG-001：RPC 成功读配置项管理名单命中切换 `P0` `@smoke`
- **前置**：总开关开启；三张表（config_key/key_engine_relation/config_value）完整；linkis-ps-configuration 正常
- **步骤**：用户从名单内应用提交 Spark2 任务；fetchSpark3CoercionConfig 发 RPC 拉名单；getValue(keyAndValue) 读取
- **预期**：切换 Spark3 3.4.4，日志记录 creator 命中

### TC-MIG-002：RPC 失败 fallback 到 properties 默认值 `P1` `@negative @resilience`
- **前置**：linkis-ps-configuration 不可用
- **步骤**：提交任务；RPC 失败被 tryAndWarnMsg 捕获；keyAndValue=null；getValue(null) 走 properties 默认
- **预期**：保持 Spark2，不阻断任务，warn 日志

### TC-MIG-003：DB 未注册 key 行为与迁移前一致 `P1` `@negative`
- **前置**：配置项管理未注册 spark.version.coercion.* key（SQL 未执行）
- **步骤**：提交任务；RPC map 无 key；getValue 走 properties 默认
- **预期**：保持 Spark2，行为与迁移前完全一致

### TC-MIG-004：三张表漏 config_value 时 queryConfig 查不到 `P1` `@negative`
- **前置**：config_key + key_engine_relation 已注册，config_value 无记录
- **步骤**：提交任务；queryConfigWithGlobalConfig 不返回 key；等效 fallback
- **预期**：保持 Spark2

### TC-MIG-005：switch 保留 properties 不迁移 `P2` `@config`
- **前置**：配置项管理未注册 switch；properties switch=true；名单含目标应用
- **步骤**：提交任务；map 无 switch；getValue(map) 走 properties 默认 true
- **预期**：切换 Spark3，代码统一 getValue(keyAndValue) 无需分叉

### TC-MIG-006：前端 saveFullTree 改名单清 AM 缓存 `P2` `@config`
- **前置**：名单初始空
- **步骤**：提交任务→Spark2；前端 saveFullTree 改名单（广播 RemoveCacheConfRequest）；再提交
- **预期**：清 AM 缓存，下个任务切换 Spark3，无需重启

### TC-MIG-007：直接执行 SQL 后需重启 entrance 清 RPC 缓存 `P2` `@config @known-limitation`
- **前置**：RPC 缓存 expireAfterAccess 120000ms
- **步骤**：直接执行 SQL 注册 key（未走前端）；未重启提交→可能读旧缓存；重启后再提交
- **预期**：重启前可能旧值；重启后 RPC 缓存清空，正确切换 Spark3

## 变更历史

| 时间 | 操作 | 来源 | 用例数 | 说明 |
|------|------|------|:------:|------|
| 2026-07-24 | promote | spark3-coercion-creator.feature | 7 | 配置项管理迁移场景沉淀（阶段6.1，用户确认）|
