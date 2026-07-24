# Keytab Principal Host 自动获取 - 设计文档

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本（基于已落地实现规范化） |
| 1.1 | 2026-07-24 | DevSyncAgent | 简化重构：复用 `host.enabled` 单开关，删除 `host.auto`，`host`/`host.map` 降级为声明兼容，hostname 取完整值原样认证，异常返回 null |

---

## 一、设计概述

### 1.1 设计目标

为 Linkis keytab(Kerberos) 认证提供 principal host 自动获取能力：复用既有 `wds.linkis.keytab.host.enabled` 开关，开启后 principal 的 host 部分自动取本机主机名（`InetAddress.getLocalHost.getHostName`，完整值原样使用），对接集群 `kinit -kt hadoop.keytab hadoop/${hostname}` 认证方式，免去多机部署逐机配置 host 的运维负担。

| 目标项 | 说明 |
|:------|:-----|
| 自动获取 hostname | principal host 部分取 `InetAddress.getLocalHost.getHostName`（完整值，不截取） |
| 向后兼容 | 开关默认 `false`，关闭时不拼 host，与改造前 `host.enabled=false` 行为一致 |
| 异常安全 | `InetAddress` 抛 `UnknownHostException` 时返回 null（不拼 host）并打 warn，不中断认证主链路 |
| 最小改动 | 仅复用一个既有开关，删除本轮新增的 `host.auto`；既有 `host`/`host.map` 保留声明兼容、不再驱动逻辑 |

### 1.2 设计原则

- **复用既有开关（不新增配置）**：`KEYTAB_HOST_ENABLED` 原本的静态 host 拼装路径在生产从不通（默认 `127.0.0.1` 匹配不上任何 keytab），属于死配置，故直接复用其语义为"自动取本机主机名"，无需再引入 `host.auto`
- **异常降级（CLAUDE.md §4.2）**：`resolveKeytabHost` 整体用 `Utils.tryCatch` 包裹，异常返回 null（不拼 host），认证主链路零风险
- **最小改动**：仅重构 `getKerberosUser` 内部逻辑，抽取 `resolveKeytabHost` / `localHostname` 私有方法，不改方法签名、不改调用方
- **正交设计**：host 拼装（`getKerberosUser`）与 AES keytab 加密（`getLinkisUserKeytabFile`）是两个独立方法，可任意组合

---

## 二、整体架构

### 2.1 认证调用链位置图

`getKerberosUser` 位于 Kerberos 认证主链路的 principal 组装环节，被 `getUserGroupInformation` 调用：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          HDFSUtils (object)                               │
├──────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────┐   │
│  │               getUserGroupInformation(userName, label)             │   │
│  │                     [认证主入口]                                    │   │
│  ├───────────────────────────────────────────────────────────────────┤   │
│  │   isKerberosEnabled(label) ──> true                                 │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   getLinkisUserKeytabFile(userName, label)   [keytab 文件路径]      │   │
│  │        │  path                                                      │   │
│  │        ▼                                                            │   │
│  │   getKerberosUser(userName, label)            ◄── 本次改动           │   │
│  │     └──> resolveKeytabHost()  [单开关]                              │   │
│  │           ├── 读 KEYTAB_HOST_ENABLED                                │   │
│  │           └── localHostname()        [本机主机名]                    │   │
│  │   [principal 拼装，受 host.enabled 控制]                            │   │
│  │        │  user (= "hadoop/hostname" 或 "hadoop")                    │   │
│  │        ▼                                                            │   │
│  │   UserGroupInformation.loginUserFromKeytabAndReturnUGI(user, path)  │   │
│  │   [实际 Kerberos 登录]                                               │   │
│  └───────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 改动范围

| 文件 | 改动类型 | 说明 |
|:-----|:-------|:-----|
| `HadoopConf.scala` | 删除配置项 | `KEYTAB_HOST_AUTO`（本轮 v1.0 新增的开关）及其 scaladoc 移除 |
| `HDFSUtils.scala` `getKerberosUser` | 重构 | 委托 `resolveKeytabHost()`（无 label），`isNotBlank` 判空后拼 principal |
| `HDFSUtils.scala` `resolveKeytabHost` | 重构 | 单开关决策：`host.enabled` true→`localHostname()`，false→null；异常→null |
| `HDFSUtils.scala` `localHostname` | scaladoc 更新 | 仍为 `InetAddress.getLocalHost.getHostName`，措辞改为"完整主机名原样使用" |

**保留不动（声明兼容）**：`KEYTAB_HOST`、`KEYTAB_HOST_MAP` 仍声明于 `HadoopConf`，供存量 properties 文件识别 key，但 `resolveKeytabHost` 不再读取它们。
**不改动**：`getUserGroupInformation` 签名、调用方代码、`getLinkisUserKeytabFile`、`kerberosValueMapParser`（仍被其他方法使用）、其余 `HDFSUtils` 方法。

---

## 三、核心方法设计

### 3.1 resolveKeytabHost 单开关逻辑

`resolveKeytabHost()` 是 host 解析的唯一决策点，整体被 `Utils.tryCatch` 包裹。

```
resolveKeytabHost()
│
├── [TRY]
│   │
│   └── KEYTAB_HOST_ENABLED.getValue ?
│       │
│       ├── true  ──> localHostname()          ← [分支A] 自动取本机主机名
│       │
│       └── false ──> null                     ← [分支B] 默认，不拼 host（向后兼容）
│
├── [CATCH] (任何 Throwable)
│   │
│   ├── logger.warn("Resolve keytab host failed, no host will be appended ...")
│   │
│   └── null                                   ← [分支C] 异常不拼 host
│
└── 返回 host 字符串（可能为 null / 本机主机名）
```

3 个终态分支：

| 分支 | 条件 | 返回值 | 说明 |
|:----:|:-----|:-------|:-----|
| A | `host.enabled=true` | `localHostname()` | principal = `userName/<hostname>` |
| B | `host.enabled=false`（默认） | `null` | principal = `userName`（向后兼容） |
| C | try 块抛出任何异常 | `null` | 不拼 host + warn 日志 |

> 相比 v1.0 的 7 分支决策树（`label` × `host.enabled` × `host.auto` × `host.map`），简化为单开关 3 分支。`label` 不再参与 host 解析（本机主机名与集群 label 无关）。

### 3.2 getKerberosUser 重构

**职责**：拼装 Kerberos principal（`userName/host` 或 `userName`）。

```scala
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  val host = resolveKeytabHost()             // 委托给单开关方法（label 不再参与）
  if (StringUtils.isNotBlank(host)) {         // null 安全检查
    user = user + "/" + host
  }
  user
}
```

**要点**：
- 方法签名 `def getKerberosUser(userName: String, label: String): String` 保持不变（2 处调用方零影响）
- `label` 参数保留但不再用于 host 解析
- `StringUtils.isNotBlank(host)` 保证 null/空串都不拼 `/`

### 3.3 localHostname

```scala
private def localHostname(): String = InetAddress.getLocalHost.getHostName
```

| 要点 | 说明 |
|:-----|:-----|
| 取值口径 | `InetAddress.getLocalHost.getHostName` 的**完整返回值，原样使用，不截取** |
| 使用场景 | principal 形如 `hadoop/<hostname>`（hostname 必须与 keytab 注册值一致） |
| 异常处理 | 由 `resolveKeytabHost` 的 `Utils.tryCatch` 统一兜底 |

> 部署前需核对：`InetAddress.getLocalHost.getHostName` 的输出必须与 keytab 注册的 principal host 完全一致（参见 §10 R-01）。

### 3.4 完整伪代码

```scala
// resolveKeytabHost（HDFSUtils.scala）
private def resolveKeytabHost(): String = Utils.tryCatch {
  if (KEYTAB_HOST_ENABLED.getValue) localHostname() else null
} { t: Throwable =>
  logger.warn("Resolve keytab host failed, no host will be appended to principal", t)
  null
}

// localHostname（HDFSUtils.scala）
private def localHostname(): String = InetAddress.getLocalHost.getHostName

// getKerberosUser（HDFSUtils.scala）
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  val host = resolveKeytabHost()
  if (StringUtils.isNotBlank(host)) {
    user = user + "/" + host
  }
  user
}
```

---

## 四、向后兼容性分析

### 4.1 默认开关下行为对比

当 `host.enabled=false`（默认值）时：

| 维度 | 改造前 | 改造后 |
|:-----|:-------|:-------|
| 代码路径 | `KEYTAB_HOST_ENABLED.getValue` false → 不拼 host | `resolveKeytabHost` → `host.enabled` false → 返回 null → 不拼 |
| 输出 principal | `hadoop` | `hadoop` |
| **一致** | **是** | |

### 4.2 改变行为的路径（均为原死路径）

| 路径 | 改造前 | 改造后 | 影响评估 |
|:-----|:-------|:-------|:--------|
| `host.enabled=true` | 拼**静态** `KEYTAB_HOST`（默认 `127.0.0.1`）→ principal `hadoop/127.0.0.1` 从不通 | 拼**本机主机名** → principal `hadoop/<hostname>` 可正常登录 | 原 `127.0.0.1` principal 永远匹配不上 keytab，属死路径；无生效部署受影响 |
| `label != null`（多集群） | 走 `host.map` 静态映射 | 忽略 label，统一走 `host.enabled` | `host.map` 默认值 `cluster1=127.0.0.2,...` 是无效测试值；且全仓仅本方法读取，无生效部署 |

### 4.3 兼容性结论

| 检查项 | 结果 |
|:------|:----:|
| `host.enabled=false`（默认）行为与改造前一致 | ✓ |
| 方法签名未变 | ✓ |
| 调用方代码未变 | ✓ |
| 删除的是本轮新增的 `host.auto`，既有 key（`host`/`host.map`）保留声明 | ✓ |
| 现有部署（默认配置）无需改动 | ✓ |

---

## 五、组合矩阵

### 5.1 正交性

`host.enabled`（本功能）与 `linkis.keytab.switch`（AES keytab 加密）作用于两个独立方法：

```
getUserGroupInformation(userName, label)
    │
    ├── getLinkisUserKeytabFile(userName, label)   ← 受 keytab.switch 控制
    │     [返回: path]
    │
    ├── getKerberosUser(userName, label)           ← 受 host.enabled 控制
    │     [返回: user = "hadoop/hostname" 或 "hadoop"]
    │
    └── loginUserFromKeytabAndReturnUGI(user, path)
          user 和 path 独立产出，互不影响
```

### 5.2 四组合矩阵

| 组合 | host.enabled | keytab.switch | principal host | keytab 文件 | 可用性 |
|:----:|:------------:|:-------------:|:--------------|:-----------|:------:|
| 1 | `false` | `false` | 不加 host | 源文件路径 | ✓ |
| 2 | `false` | `true` | 不加 host | AES 解密临时文件 | ✓ |
| 3 | `true` | `false` | 本机主机名 | 源文件路径 | ✓ |
| 4 | `true` | `true` | 本机主机名 | AES 解密临时文件 | ✓ |

四种组合均能完成 Kerberos 登录。

---

## 六、异常处理设计

### 6.1 UnknownHostException 路径

`localHostname()` 调用 `InetAddress.getLocalHost`，本机 `/etc/hosts` 未正确配置时可能抛 `UnknownHostException`，由 `resolveKeytabHost` 的 `Utils.tryCatch` 兜底。

```
resolveKeytabHost()
│
├── [TRY] host.enabled=true → localHostname() → InetAddress.getLocalHost
│       │
│       ├── 成功 ──> getHostName ──> 返回主机名
│       │
│       └── 抛出 UnknownHostException
│               │
│               ▼
├── [CATCH] Utils.tryCatch 捕获
│       │
│       ├── logger.warn("Resolve keytab host failed, no host will be appended ...", t)
│       │
│       └── 返回 null
│           │
│           ▼
└── getKerberosUser 拿到 null → 不拼 host → principal = userName
        │
        ▼
    loginUserFromKeytabAndReturnUGI 正常执行（认证主链路不中断）
```

### 6.2 为什么异常返回 null 而非退化到静态值

| 方案 | principal | 结果 |
|:-----|:---------|:-----|
| 返回 null（采用） | `hadoop` | 登录可能失败（取决于 keytab 是否含无 host 的 principal），但 warn 日志清晰指出"hostname 解析失败" |
| 退化静态 `127.0.0.1`（v1.0 做法，已废弃） | `hadoop/127.0.0.1` | 必然登录失败，且错误信息指向 Kerberos principal 不匹配，误导排查 |

两者在 keytab 不含对应 principal 时登录都会失败，但返回 null 的失败原因更易定位，且不引入一个"看似兜底实则无效"的假安全。

### 6.3 与 CLAUDE.md §4 铁律的对应

| 铁律条目 | 本设计落地 |
|:--------|:----------|
| §4.1 复用既有开关，不裸写新逻辑 | 复用 `KEYTAB_HOST_ENABLED`，删除 `host.auto` |
| §4.2 Utils.tryCatch 包裹 | `resolveKeytabHost` 整体 `Utils.tryCatch` 包裹 |
| §4.2 catch 不中断主链路 | 返回 null，`loginUserFromKeytabAndReturnUGI` 照常执行 |
| §4.2 日志用项目 logger | `logger.warn(...)`（`HDFSUtils extends Logging`） |

---

## 七、配置项设计

### 7.1 生效配置项

| 配置项 | 默认值 | 说明 |
|:------|:------|:-----|
| `wds.linkis.keytab.host.enabled` | `false` | true → principal 拼本机主机名（`hadoop/${hostname}`）；false（默认）→ 不拼 host |

### 7.2 保留声明兼容（不再读取）

| 配置项 | 默认值 | 状态 |
|:------|:------|:-----|
| `wds.linkis.keytab.host` | `127.0.0.1` | 声明保留，存量 properties 不报未知 key；不再驱动 principal 拼装 |
| `linkis.keytab.host.map` | `cluster1=127.0.0.2,cluster2=127.0.0.3` | 同上 |

### 7.3 已删除

| 配置项 | 说明 |
|:------|:-----|
| `wds.linkis.keytab.host.auto` | v1.0 新增的开关，v1.1 删除（语义合并进 `host.enabled`） |

### 7.4 典型部署配置示例

```properties
# ===== 集群统一 kinit -kt hadoop.keytab hadoop/${hostname} 方式 =====
wds.linkis.keytab.enable=true
wds.linkis.keytab.host.enabled=true        # principal host 自动取本机主机名

# 不再需要逐机配置（被 host.enabled 取代）：
# wds.linkis.keytab.host=worker01          # 旧方式：每台机器不同，已废弃读取
```

---

## 八、验收标准对应

| 验收标准 | 对应设计分支 | 验证方法 |
|:--------|:-----------|:--------|
| F-01: `host.enabled=true` 时 principal 带本机主机名 | 分支 A | 集成测试：`host.enabled=true` → principal = `hadoop/<hostname>` |
| F-02: `host.enabled=false` 时 principal 不带 host | 分支 B | 单测 `testGetKerberosUserNoHostByDefault` + `testResolveDefaultNoHost` |
| F-03: `UnknownHostException` 不拼 host | 分支 C | mock `InetAddress.getLocalHost` 抛异常 → principal = `hadoop` |
| F-04: `host.enabled` × `keytab.switch` 组合可用 | §5.2 | 四组合各跑一次 Kerberos 登录冒烟 |

---

## 九、回滚方案

### 9.1 回滚条件
- 自动获取的主机名与 keytab 注册的 principal 不匹配导致登录失败

### 9.2 回滚方式
**零代码改动**：将 `wds.linkis.keytab.host.enabled` 设为 `false`，重启服务。principal 不再带 host，行为回退。

### 9.3 回滚验证
- [ ] `host.enabled=false` 后 principal 不含本机主机名
- [ ] 现有任务提交流程不受影响

---

## 十、风险与预案

| 风险 | 影响 | 概率 | 应对措施 |
|:-----|:-----|:----:|:--------|
| R-01 自动获取的主机名与 keytab 注册 principal 不一致 | Kerberos 登录失败 | 中 | 部署前核对 `InetAddress.getLocalHost.getHostName` 输出与 `klist -k hadoop.keytab` 注册值完全一致 |
| R-02 `InetAddress.getLocalHost` 抛 `UnknownHostException` | 不拼 host（可能登录失败） | 低 | `Utils.tryCatch` 返回 null + warn 日志 |
| R-03 机器 `/etc/hosts` 未正确配置本机名 | `getLocalHost` 返回非预期值 | 中 | 部署文档强调 `/etc/hosts` 配置 |

---

## 十一、附录

### 11.1 改动文件清单

| 文件 | 路径 |
|:-----|:-----|
| HadoopConf.scala | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala` |
| HDFSUtils.scala | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala` |
| HDFSUtilsKeytabHostTest.scala | `linkis-commons/linkis-hadoop-common/src/test/scala/.../HDFSUtilsKeytabHostTest.scala` |
| linkis.properties | `linkis-dist/package/conf/linkis.properties` |
| 配置文档 | `docs/configuration/linkis-hadoop-common.md` |

### 11.2 参考文档

1. 需求文档：`docs/dev-2.0.0/requirements/keytab-hostname-auth_需求.md`
2. CLAUDE.md §4 新功能开发铁律（开关 + tryCatch 降级）
3. CLAUDE.md §5 代码规范（ASF License、scalastyle）
4. Hadoop Kerberos 认证文档（principal 格式 `service/host@REALM`）
