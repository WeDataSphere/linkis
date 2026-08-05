# Keytab Principal Host 自动获取 - 测试用例文档

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本 |

---

## 一、测试概述

### 1.1 测试目标
验证 `resolveKeytabHost` / `getKerberosUser` 在 `host.auto` 开关 on/off、`host.enabled`、`label`、`host.map` 命中情况、`InetAddress` 异常等各分支下，principal 拼装结果正确且向后兼容。

### 1.2 测试范围

| 测试类型 | 测试内容 |
|:--------|:--------|
| 单元测试 | `resolveKeytabHost` 7 分支决策、`getKerberosUser` 拼装、`localHostname` |
| 向后兼容测试 | `host.auto=false`（默认）时与改造前逐场景等价 |
| 异常降级测试 | `UnknownHostException` 降级回静态 `KEYTAB_HOST` |
| 组合测试 | `host.auto` × `keytab.switch` 四组合 |
| 集成测试 | 与 `getUserGroupInformation` 调用链集成 |

### 1.3 测试方法
- 反射调用私有方法 `resolveKeytabHost`（参考现有 `HDFSUtilsKeytabCacheTest`）
- 用 `System.setProperty` 覆盖 `CommonVars` 开关值（`wds.linkis.keytab.host.enabled` / `wds.linkis.keytab.host.auto` / `wds.linkis.keytab.host` / `linkis.keytab.host.map`）
- `InetAddress` 异常场景：构造无法解析的主机环境或 mock

---

## 二、单元测试用例 — resolveKeytabHost 决策树

### 2.1 label=null 分支（单集群）

| 用例ID | 测试场景 | 输入(label/host.enabled/host.auto) | 预期 host | 对应需求 | 优先级 |
|:------:|:--------|:---------|:---------|:---------|:------:|
| TC-01 | host.enabled=false（不加 host） | null / false / * | null（principal=userName） | F-02 | P0 |
| TC-02 | host.enabled=true, auto=false（静态值） | null / true / false | `KEYTAB_HOST` 配置值 | F-02 | P0 |
| TC-03 | host.enabled=true, auto=true（自动短名） | null / true / true | `InetAddress.getLocalHost.getHostName` | F-01 | P0 |

### 2.2 label≠null 分支（多集群）

| 用例ID | 测试场景 | 输入(label/host.map/host.auto) | 预期 host | 对应需求 | 优先级 |
|:------:|:--------|:---------|:---------|:---------|:------:|
| TC-04 | host.map 命中（map 优先） | "cluster1" / 命中 / * | `host.map[cluster1]` | F-06 | P0 |
| TC-05 | host.map 未命中, auto=true（回退短名） | "clusterX" / 未命中 / true | 本机短名 | F-04 | P0 |
| TC-06 | host.map 未命中, auto=false（不加 host） | "clusterX" / 未命中 / false | null | F-02 | P0 |

### 2.3 异常降级

| 用例ID | 测试场景 | 输入 | 预期 host | 对应需求 | 优先级 |
|:------:|:--------|:---------|:---------|:---------|:------:|
| TC-07 | InetAddress 抛 UnknownHostException | auto=true 触发异常 | 降级回 `KEYTAB_HOST` 静态值，不抛异常 | F-03 | P0 |

---

## 三、向后兼容测试（host.auto=false 默认）

| 用例ID | 场景 | 改造前预期 | 改造后实际（auto=false） | 一致性 | 优先级 |
|:------:|:--------|:---------|:---------|:------:|:------:|
| BC-01 | label=null, enabled=false | userName（不加） | userName | ✓ | P0 |
| BC-02 | label=null, enabled=true | userName/KEYTAB_HOST | userName/KEYTAB_HOST | ✓ | P0 |
| BC-03 | label=X, map 命中 | userName/map(X) | userName/map(X) | ✓ | P0 |
| BC-04 | label=X, map 未命中 | userName（不加） | userName（不加） | ✓ | P0 |

---

## 四、组合测试（host.auto × keytab.switch 正交性）

| 用例ID | keytab.switch | host.auto | keytab 来源 | principal | 预期 | 优先级 |
|:------:|:------:|:------:|:---------|:---------|:-----|:------:|
| CB-01 | false | false | 明文 keytab | userName/静态host | 登录成功（原行为） | P1 |
| CB-02 | false | true | 明文 keytab | userName/本机短名 | 登录成功 | P1 |
| CB-03 | true | true | AES 解密 keytab | userName/本机短名 | 登录成功 | P1 |
| CB-04 | true | false | AES 解密 keytab | userName/静态host | 登录成功 | P1 |

> 注：CB-03/CB-04 需真实 Kerberos 环境验证，单测仅验证 principal 字符串与 keytab 路径独立产出。

---

## 五、集成测试用例

| 用例ID | 测试场景 | 测试方法 | 预期结果 | 优先级 |
|:------:|:--------|:---------|:---------|:------:|
| IT-01 | 完整 principal 拼装链 | getKerberosUser("hadoop", null) + auto=true | principal=`hadoop/${本机短名}` | P0 |
| IT-02 | 代理模式 + auto | getKeytabSuperUser + getKerberosUser(superUser) | superUser principal 带本机短名 | P0 |
| IT-03 | 非 Kerberos 场景不受影响 | isKerberosEnabled=false | 走 createRemoteUser，不触发 resolveKeytabHost | P1 |

---

## 六、边界条件测试

| 用例ID | 测试场景 | 输入 | 预期结果 | 优先级 |
|:------:|:--------|:---------|:---------|:------:|
| EG-01 | host.map 配置为空字符串 | linkis.keytab.host.map="" | 当作空 map，按未命中处理 | P2 |
| EG-02 | host.map 格式异常 | "cluster1"（无 = value） | 解析跳过异常项，不抛异常 | P2 |
| EG-03 | label 为空字符串 | label="" | 视配置走 label=null 或非null 分支，不抛异常 | P2 |

---

## 七、测试通过准则

- [ ] TC-01 ~ TC-07：7 分支决策树全部通过
- [ ] BC-01 ~ BC-04：向后兼容性全部一致
- [ ] CB-01 ~ CB-04：组合矩阵单测层通过（principal/路径独立）
- [ ] IT-01 ~ IT-03：集成调用链正确
- [ ] 编译通过（`linkis-hadoop-common` mvn test 编译期）
