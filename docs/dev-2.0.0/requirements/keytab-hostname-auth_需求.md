# Keytab Principal Host 自动获取 - 需求文档

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本 |
| 1.1 | 2026-07-24 | DevSyncAgent | 简化：复用 `host.enabled` 单开关；删除本轮新增的 `host.auto`，`host.map`（既有）保留声明不再驱动逻辑；hostname 取完整值原样认证 |

---

## 一、功能概述

### 1.1 功能名称
Keytab Principal Host 自动获取（对接 `kinit -kt hadoop.keytab hadoop/${hostname}` 认证方式）

### 1.2 一句话描述
复用既有 `wds.linkis.keytab.host.enabled` 开关，开启后 principal 的 host 部分自动取本机主机名（`InetAddress.getLocalHost.getHostName`，原样使用），免去多机部署逐机配置 host 的负担。

### 1.3 功能类型
功能增强 (ENHANCE)

### 1.4 优先级
P2（内部基础设施改进，开关默认关闭可灰度，非紧急）

---

## 二、背景与现状分析

### 2.1 背景
集群机器统一改用 `kinit -kt hadoop.keytab hadoop/${hostname}` 方式认证：每台机器以超级用户 `hadoop` 的 keytab 登录，principal 形如 `hadoop/${hostname}`，其中 `${hostname}` 是机器实际主机名。Linkis 需对接此认证方式。

### 2.2 现状（改造前）

| 分析维度 | 详情 |
|:--------|------|
| **相关文件** | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala`、`.../conf/HadoopConf.scala` |
| **相关方法** | `getKerberosUser(userName, label)` — 拼 principal 的 host 部分 |
| **host 来源** | 静态配置 `wds.linkis.keytab.host`（默认 `127.0.0.1`），多集群走 `linkis.keytab.host.map` |
| **痛点** | 多机部署时每台机器 hostname 不同，但 host 是全局静态值，需逐机配置且无法适应动态 hostname |

### 2.3 根因

| 根因编号 | 根因描述 | 影响 |
|:--------:|:--------|:-----|
| RC-1 | principal 的 host 部分只能来自静态配置项，无自动获取机器 hostname 的能力 | 多机部署运维负担重，hostname 变更即失效 |
| RC-2 | 默认值 `127.0.0.1` 对 Kerberos 无意义 | 即使开启 `host.enabled`，拼出的 `hadoop/127.0.0.1` 也匹配不上 keytab，等于死配置 |
| RC-3 | ECM 等每机一实例的服务，全局 host 配置无法满足每机不同 | 机器级认证场景无法直接对接 |

### 2.4 改造前代码

```scala
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  if (label == null) {
    if (KEYTAB_HOST_ENABLED.getValue) {
      user = user + "/" + KEYTAB_HOST.getValue   // 仅静态值，无自动获取
    }
  } else {
    val hostMap = kerberosValueMapParser(KEYTAB_HOST_MAP.getValue)
    if (hostMap.contains(label)) {
      user = user + "/" + hostMap(label)          // 仅 map 静态值
    }
  }
  user
}
```

> 说明：改造前 `KEYTAB_HOST_ENABLED` / `KEYTAB_HOST_MAP` / `KEYTAB_HOST` 三项配置**仅被本方法消费**，全仓无其他依赖；且 `host.enabled=true` 配合默认 `127.0.0.1` 产出的 principal 从不通，属于死路径。因此可安全地复用 `host.enabled` 语义；`host`/`host.map` 仅保留 key 声明兼容存量 properties，不再驱动 principal 拼装。

---

## 三、解决方案设计

### 3.1 核心方案
**复用既有开关 `wds.linkis.keytab.host.enabled`（默认 `false`）**。开启后，principal 的 host 部分自动取本机主机名 `InetAddress.getLocalHost.getHostName`（**完整值原样使用，不截取**）。关闭时不拼 host（与改造前 `host.enabled=false` 行为一致，向后兼容）。

**删除**本轮新增的 `wds.linkis.keytab.host.auto`；既有 `wds.linkis.keytab.host` 与 `linkis.keytab.host.map` 仅作 key 声明兼容（存量 properties 不报未知 key），不再驱动 principal 拼装。

### 3.2 关键设计

#### 3.2.1 单开关语义
- `host.enabled=false`（默认）：principal 不带 host（= `userName`）。
- `host.enabled=true`：principal = `userName/<本机主机名>`，本机主机名取 `InetAddress.getLocalHost.getHostName` 原值。
- `label` 参数不再参与 host 解析（本机主机名与集群 label 无关）。

#### 3.2.2 异常处理
`InetAddress.getLocalHost` 可能抛 `UnknownHostException`，用 `Utils.tryCatch` 包裹。**异常时返回 `null`（即不拼 host）并打 warn 日志**——而不是退化成静态 `127.0.0.1`（后者同样匹配不上 keytab，还会误导排查）。符合 CLAUDE.md §4 铁律：主链路不抛未捕获异常。

#### 3.2.3 与 `linkis.keytab.switch` 正交
host 自动获取（`getKerberosUser`，拼 principal）与 AES 加密 keytab 处理（`getLinkisUserKeytabFile`，处理文件）是两个独立方法，可任意组合。

### 3.3 约束条件

| 约束类型 | 要求 |
|:--------|:-----|
| **向后兼容** | 开关默认 `false`，关闭时 `getKerberosUser` 不拼 host，与改造前 `host.enabled=false` 一致 |
| **生产稳定** | 异常返回 null，不破坏认证主链路 |
| **配置规范** | 复用既有 `wds.linkis.keytab.host.enabled`，不新增开关 |
| **代码规范** | ASF License 头、scalastyle（≤200 字符/2 空格缩进）、复用 `Utils.tryCatch` |

---

## 四、功能需求

### 4.1 核心功能 (P0)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-01 | principal host 自动获取 | `host.enabled=true` 时，principal host = 本机主机名（`InetAddress.getLocalHost.getHostName` 原值，不截取） |
| F-02 | 向后兼容 | `host.enabled=false`（默认）时，principal 不带 host |
| F-03 | 异常处理 | `InetAddress` 抛 `UnknownHostException` 时返回 null（不拼 host）并打 warn，不抛未捕获异常 |

### 4.2 重要功能 (P1)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-04 | 与 `keytab.switch` 正交 | `host.enabled` 与 `keytab.switch` 任意组合均能正常 Kerberos 登录 |

### 4.3 辅助功能 (P2)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-05 | 配置可观测 | `linkis.properties` 与 `docs/configuration/linkis-hadoop-common.md` 同步：删除 `host.auto` 行，更新 `host.enabled` 语义说明 |

---

## 五、非功能需求

### 5.1 可靠性需求

| 需求 | 说明 |
|:----|:-----|
| 认证主链路保护 | 自动获取失败时返回 null（不拼 host），不影响任务提交与引擎拉起 |
| principal 一致性 | 自动获取的主机名必须与 keytab 注册的 principal host 完全一致，否则登录失败（部署前需核对） |

### 5.2 可维护性需求

| 需求 | 说明 |
|:----|:-----|
| 代码可读性 | host 解析集中到 `resolveKeytabHost`，`localHostname` 注释说明取值口径 |
| 日志完善 | 异常时打 warn 日志，便于线上排查 |

---

## 六、验收标准

### 6.1 功能验收
- [x] F-01: `host.enabled=true` 时 principal 正确带本机主机名（集成测试覆盖）
- [x] F-02: `host.enabled=false`（默认）时 principal 不带 host（单测 `testGetKerberosUserNoHostByDefault`）
- [x] F-03: 异常返回 null（代码审查 + 集成测试）
- [ ] F-04: `host.enabled` × `keytab.switch` 组合可用（集成测试）

### 6.2 兼容性验收
- [x] 开关默认 `false`，现有部署无需改动即保持原行为
- [x] 编译通过（`linkis-hadoop-common BUILD SUCCESS`）

### 6.3 配置验收
- [x] `linkis.properties` 已删除 `host.auto` 注释行，更新 `host.enabled` 注释
- [x] `docs/configuration/linkis-hadoop-common.md` 已删除 `host.auto` 行，更新 `host.enabled` 描述

---

## 七、风险与预案

| 风险 | 影响 | 概率 | 应对措施 |
|:----|:----|:----:|:--------|
| R-01 | 自动获取的主机名与 keytab 注册的 principal 不一致 | 高 | 中 | 部署前核对 `InetAddress.getLocalHost.getHostName` 输出与 `klist -k hadoop.keytab` 注册值完全一致 |
| R-02 | `InetAddress.getLocalHost` 抛 `UnknownHostException` | 中 | 低 | `Utils.tryCatch` 返回 null（不拼 host）+ warn 日志 |
| R-03 | 机器 `/etc/hosts` 未正确配置本机名导致返回非预期值 | 中 | 中 | 部署文档强调 `/etc/hosts` 配置 |

---

## 八、待确认问题

| 问题ID | 问题描述 | 优先级 | 状态 |
|:------|:--------|:------:|:----:|
| Q-01 | hostname 取短名还是完整值？ | P2 | 已确认：取 `getHostName` 完整值原样认证，不截取 |
| Q-02 | JDBC 引擎独立 Kerberos 分支（`ConnectionManager`）是否同步改造？ | P2 | 已确认：本期不覆盖，单独评估 |

---

## 九、参考文档

1. 现有代码：`HDFSUtils.scala` `getUserGroupInformation` / `getKerberosUser`
2. 配置：`HadoopConf.scala` `KEYTAB_HOST_ENABLED`（生效开关）；`KEYTAB_HOST` / `KEYTAB_HOST_MAP`（仅声明兼容，不再读取）
3. CLAUDE.md §4 新功能开发铁律（开关 + tryCatch 降级）
4. Hadoop Kerberos 认证文档（principal 格式 `service/host@REALM`）
