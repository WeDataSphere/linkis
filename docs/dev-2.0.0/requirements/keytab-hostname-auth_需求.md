# Keytab Principal Host 自动获取 - 需求文档

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本 |

---

## 一、功能概述

### 1.1 功能名称
Keytab Principal Host 自动获取（对接 `kinit -kt hadoop.keytab hadoop/${hostname}` 认证方式）

### 1.2 一句话描述
为 Linkis keytab(Kerberos) 认证新增 `wds.linkis.keytab.host.auto` 开关，开启后 principal 的 host 部分自动取本机短名（等价 shell `hostname`），免去多机部署逐机配置 `wds.linkis.keytab.host` 的负担。

### 1.3 功能类型
功能增强 (ENHANCE)

### 1.4 优先级
P2（内部基础设施改进，开关默认关闭可灰度，非紧急）

---

## 二、背景与现状分析

### 2.1 背景
集群机器统一改用 `kinit -kt hadoop.keytab hadoop/${hostname}` 方式认证：每台机器以超级用户 `hadoop` 的 keytab 登录，principal 形如 `hadoop/${hostname}`，其中 `${hostname}` 是机器实际短主机名（shell `hostname` 输出）。Linkis 需对接此认证方式。

### 2.2 现状（改造前）

| 分析维度 | 详情 |
|:--------|------|
| **相关文件** | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala`、`.../conf/HadoopConf.scala` |
| **相关方法** | `getKerberosUser(userName, label)` — 拼 principal 的 host 部分 |
| **host 来源** | 静态配置 `wds.linkis.keytab.host`（默认 `127.0.0.1`），多集群走 `linkis.keytab.host.map` |
| **痛点** | 多机部署时每台机器 hostname 不同，但 `wds.linkis.keytab.host` 是全局静态值，需逐机配置且无法适应动态 hostname |

### 2.3 根因

| 根因编号 | 根因描述 | 影响 |
|:--------:|:--------|:-----|
| RC-1 | principal 的 host 部分只能来自静态配置项，无自动获取机器 hostname 的能力 | 多机部署运维负担重，hostname 变更即失效 |
| RC-2 | 默认值 `127.0.0.1` 对 Kerberos 无意义 | 开启 `host.enabled` 后必须手动配置，否则登录失败 |
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

---

## 三、解决方案设计

### 3.1 核心方案
新增开关 `wds.linkis.keytab.host.auto`（默认 `false`）。开启后，principal 的 host 部分自动取本机短名 `InetAddress.getLocalHost.getHostName`（等价 shell `hostname`）。关闭时行为与改造前完全一致（向后兼容）。

### 3.2 关键设计

#### 3.2.1 host 解析优先级
- `label == null`：`host.enabled=true` 时，`host.auto=true` 用本机短名，否则用静态 `KEYTAB_HOST`；`host.enabled=false` 不加 host。
- `label != null`：优先 `linkis.keytab.host.map` 中该 label 的值；未配置时，`host.auto=true` 回退本机短名，否则不加 host。

#### 3.2.2 异常降级
`InetAddress.getLocalHost` 可能抛 `UnknownHostException`，用 `Utils.tryCatch` 包裹，异常时降级回静态 `KEYTAB_HOST` 值，绝不中断 Kerberos 登录主链路（符合 CLAUDE.md §4 铁律）。

#### 3.2.3 与 `linkis.keytab.switch` 正交
host 自动获取（`getKerberosUser`，拼 principal）与 AES 加密 keytab 处理（`getLinkisUserKeytabFile`，处理文件）是两个独立方法，可任意组合（4 种组合均可用）。

### 3.3 约束条件

| 约束类型 | 要求 |
|:--------|:-----|
| **向后兼容** | 开关默认 `false`，关闭时 `getKerberosUser` 行为逐场景与改造前一致 |
| **生产稳定** | 异常降级，不破坏认证主链路 |
| **配置规范** | 开关命名遵循 `wds.linkis.keytab.*` 前缀，默认 `false` |
| **代码规范** | ASF License 头、scalastyle（≤200 字符/2 空格缩进）、复用 `Utils.tryCatch` |

---

## 四、功能需求

### 4.1 核心功能 (P0)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-01 | principal host 自动获取 | `host.auto=true` + `host.enabled=true` 时，principal host = 本机短名（`InetAddress.getLocalHost.getHostName`） |
| F-02 | 向后兼容 | `host.auto=false`（默认）时，`getKerberosUser` 输出与改造前完全一致 |
| F-03 | 异常降级 | `InetAddress` 抛 `UnknownHostException` 时，降级回静态 `KEYTAB_HOST` 值，不抛出未捕获异常 |
| F-04 | 多集群支持 | `label != null` 且 `host.map` 未配置该 label 时，`host.auto=true` 回退本机短名 |

### 4.2 重要功能 (P1)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-05 | 与 `keytab.switch` 正交 | `host.auto` 与 `keytab.switch` 四种组合（on/off × on/off）均能正常 Kerberos 登录 |
| F-06 | host.map 优先级 | `label != null` 且 `host.map` 含该 label 时，`host.map` 值优先于 `host.auto` |

### 4.3 辅助功能 (P2)

| ID | 功能描述 | 验收标准 |
|:--:|:--------|:--------|
| F-07 | 配置可观测 | `linkis.properties` 与 `docs/configuration/linkis-hadoop-common.md` 同步新增 `host.auto` 配置项说明 |

---

## 五、非功能需求

### 5.1 可靠性需求

| 需求 | 说明 |
|:----|:-----|
| 认证主链路保护 | 自动获取失败时降级，不影响任务提交与引擎拉起 |
| principal 一致性 | 自动获取的 hostname 必须与 keytab 注册的 principal host 完全一致，否则登录失败（部署前需核对） |

### 5.2 可维护性需求

| 需求 | 说明 |
|:----|:-----|
| 代码可读性 | 抽取 `resolveKeytabHost` / `localHostname` 私有方法，注释说明各分支语义 |
| 日志完善 | 降级时打 warn 日志，便于线上排查 |

---

## 六、验收标准

### 6.1 功能验收
- [ ] F-01: `host.auto=true` 时 principal 正确带本机短名
- [ ] F-02: `host.auto=false` 时与改造前逐场景一致（label null/非null、host.enabled、host.map 命中/未命中）
- [ ] F-03: `UnknownHostException` 场景降级到静态值
- [ ] F-04: 多集群 label 未配 host.map 时自动回退本机短名
- [ ] F-05: `host.auto` × `keytab.switch` 四组合均可用
- [ ] F-06: host.map 命中时优先于 host.auto

### 6.2 兼容性验收
- [ ] 开关默认 `false`，现有部署无需改动即保持原行为
- [ ] 编译通过（`linkis-hadoop-common BUILD SUCCESS`）

### 6.3 配置验收
- [ ] `linkis.properties` 含 `wds.linkis.keytab.host.auto` 注释行
- [ ] `docs/configuration/linkis-hadoop-common.md` 含新配置项说明

---

## 七、风险与预案

| 风险 | 影响 | 概率 | 应对措施 |
|:----|:----|:----:|:--------|
| R-01 | 自动获取的 hostname 与 keytab 注册的 principal 不一致（短名 vs FQDN） | 高 | 中 | 部署前 `hostname` 与 `klist -k hadoop.keytab` 核对；选用 `getHostName`（短名）匹配 shell `hostname` |
| R-02 | `InetAddress.getLocalHost` 抛 `UnknownHostException` | 中 | 低 | `Utils.tryCatch` 降级回静态 `KEYTAB_HOST` |
| R-03 | 机器 `/etc/hosts` 未正确配置本机名导致 `getLocalHost` 返回非预期值 | 中 | 中 | 部署文档强调 `/etc/hosts` 配置；降级兜底 |

---

## 八、待确认问题

| 问题ID | 问题描述 | 优先级 | 状态 |
|:------|:--------|:------:|:----:|
| Q-01 | 是否需要支持 FQDN（`getCanonicalHostName`）作为可选模式？ | P2 | 已确认：本期仅短名，FQDN 作为后续可扩展点 |
| Q-02 | JDBC 引擎独立 Kerberos 分支（`ConnectionManager`）是否同步改造？ | P2 | 已确认：本期不覆盖，单独评估 |

---

## 九、参考文档

1. 现有代码：`HDFSUtils.scala` `getUserGroupInformation` / `getKerberosUser`
2. 配置：`HadoopConf.scala` `KEYTAB_HOST` / `KEYTAB_HOST_ENABLED` / `KEYTAB_HOST_MAP`
3. CLAUDE.md §4 新功能开发铁律（开关 + tryCatch 降级）
4. Hadoop Kerberos 认证文档（principal 格式 `service/host@REALM`）
