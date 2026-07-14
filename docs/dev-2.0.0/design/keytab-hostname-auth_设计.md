# Keytab Principal Host 自动获取 - 设计文档

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本（基于已落地实现规范化） |

---

## 一、设计概述

### 1.1 设计目标

为 Linkis keytab(Kerberos) 认证新增 `wds.linkis.keytab.host.auto` 开关。开启后，principal 的 host 部分自动取本机短名（等价 shell `hostname`），对接集群 `kinit -kt hadoop.keytab hadoop/${hostname}` 认证方式，免去多机部署逐机配置 `wds.linkis.keytab.host` 的运维负担。

| 目标项 | 说明 |
|:------|:-----|
| 自动获取 hostname | principal host 部分自动取 `InetAddress.getLocalHost.getHostName`（短名） |
| 向后兼容 | 开关默认 `false`，关闭时 `getKerberosUser` 逐场景与改造前完全一致 |
| 异常降级 | `InetAddress` 抛 `UnknownHostException` 时降级回静态 `KEYTAB_HOST` 值，不中断认证主链路 |
| 多集群支持 | `label != null` 且 `host.map` 未命中时，`host.auto=true` 回退本机短名 |

### 1.2 设计原则

- **开关先行（CLAUDE.md §4.1）**：新增开关默认 `false`，灰度验证后再开启，出问题可一键回退
- **异常降级（CLAUDE.md §4.2）**：`resolveKeytabHost` 整体用 `Utils.tryCatch` 包裹，任何异常均降级回静态值，认证主链路零风险
- **最小改动**：仅重构 `getKerberosUser` 方法内部逻辑，抽取 `resolveKeytabHost` / `localHostname` 私有方法，不改方法签名、不改调用方
- **正交设计**：host 自动获取（`getKerberosUser`）与 AES keytab 加密（`getLinkisUserKeytabFile`）是两个独立方法，可任意组合

---

## 二、整体架构

### 2.1 认证调用链位置图

`getKerberosUser` 位于 Kerberos 认证主链路的 principal 组装环节，被 `getUserGroupInformation` 调用：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          HDFSUtils (object)                               │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌───────────────────────────────────────────────────────────────────┐   │
│  │               getUserGroupInformation(userName, label)             │   │
│  │                     [认证主入口]                                    │   │
│  ├───────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │   isKerberosEnabled(label) ──> true                                 │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────┐          │   │
│  │   │  getLinkisUserKeytabFile(userName, label)            │          │   │
│  │   │  [keytab 文件路径，受 keytab.switch 控制]             │          │   │
│  │   └─────────────────────────────────────────────────────┘          │   │
│  │        │  path                                                      │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   ┌─────────────────────────────────────────────────────┐          │   │
│  │   │  getKerberosUser(userName, label)                    │ ◄── 本次改动 │
│  │   │    │                                                 │          │   │
│  │   │    └──> resolveKeytabHost(label)  [新增]             │          │   │
│  │   │          ├── 读 KEYTAB_HOST_ENABLED                  │          │   │
│  │   │          ├── 读 KEYTAB_HOST_AUTO     [新增开关]       │          │   │
│  │   │          ├── 读 KEYTAB_HOST_MAP                      │          │   │
│  │   │          └── localHostname()         [新增]           │          │   │
│  │   │  [principal 拼装，受 host.auto 控制]                  │          │   │
│  │   └─────────────────────────────────────────────────────┘          │   │
│  │        │  user (= "hadoop/hostname" 或 "hadoop")                     │   │
│  │        │                                                            │   │
│  │        ▼                                                            │   │
│  │   UserGroupInformation.loginUserFromKeytabAndReturnUGI(user, path)  │   │
│  │   [实际 Kerberos 登录]                                               │   │
│  └───────────────────────────────────────────────────────────────────┘   │
│                                                                            │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.2 改动范围

| 文件 | 改动类型 | 说明 |
|:-----|:-------|:-----|
| `HadoopConf.scala` (line 52) | 新增配置项 | `KEYTAB_HOST_AUTO = CommonVars("wds.linkis.keytab.host.auto", false)` |
| `HDFSUtils.scala` `getKerberosUser` (line 456-463) | 重构 | 从内联逻辑改为调用 `resolveKeytabHost(label)`，`isNotBlank` 判空后再拼 principal |
| `HDFSUtils.scala` `resolveKeytabHost` (line 479-499) | 新增方法 | host 解析决策树，含 `label null/非null` × `host.enabled` × `host.auto` × `host.map` 全分支 |
| `HDFSUtils.scala` `localHostname` (line 505) | 新增方法 | `InetAddress.getLocalHost.getHostName`（等价 shell `hostname`） |

**不改动**：`getUserGroupInformation` 签名、调用方代码、`getLinkisUserKeytabFile`、其余 `HDFSUtils` 方法。

---

## 三、核心方法设计

### 3.1 resolveKeytabHost 完整决策树

`resolveKeytabHost(label)` 是本次改造的核心，所有 host 解析逻辑集中在此方法。整体被 `Utils.tryCatch` 包裹。

```
resolveKeytabHost(label)
│
├── [TRY]
│   │
│   ├── label == null ?
│   │   │
│   │   ├── YES ──> KEYTAB_HOST_ENABLED.getValue ?
│   │   │           │
│   │   │           ├── true ──> KEYTAB_HOST_AUTO.getValue ?
│   │   │           │           │
│   │   │           │           ├── true  ──> localHostname()          ← [分支A] F-01 自动获取
│   │   │           │           │
│   │   │           │           └── false ──> KEYTAB_HOST.getValue     ← [分支B] 向后兼容（静态值）
│   │   │           │
│   │   │           └── false ──> null                                 ← [分支C] 不加 host
│   │   │
│   │   └── NO (label != null)
│   │       │
│   │       ├── hostMap.contains(label) ?
│   │       │   │
│   │       │   ├── YES ──> hostMap(label)                             ← [分支D] F-06 host.map 优先
│   │       │   │
│   │       │   └── NO ──> KEYTAB_HOST_AUTO.getValue ?
│   │       │           │
│   │       │           ├── true  ──> localHostname()                  ← [分支E] F-04 未命中自动获取
│   │       │           │
│   │       │           └── false ──> null                             ← [分支F] 向后兼容（不加 host）
│   │
├── [CATCH] (任何 Throwable)
│   │
│   └── KEYTAB_HOST.getValue                                           ← [分支G] F-03 异常降级
│       logger.warn("Resolve keytab host failed, fallback ...")
│
└── 返回 host 字符串（可能为 null / 短名 / 静态IP）
```

7 个终态分支汇总：

| 分支 | 条件 | 返回值 | 对应功能 |
|:----:|:-----|:-------|:--------|
| A | `label==null` ∧ `host.enabled=true` ∧ `host.auto=true` | `localHostname()` | F-01 自动获取 |
| B | `label==null` ∧ `host.enabled=true` ∧ `host.auto=false` | `KEYTAB_HOST` 静态值 | F-02 向后兼容 |
| C | `label==null` ∧ `host.enabled=false` | `null`（不加 host） | F-02 向后兼容 |
| D | `label!=null` ∧ `host.map` 命中 | `hostMap(label)` | F-06 map 优先 |
| E | `label!=null` ∧ `host.map` 未命中 ∧ `host.auto=true` | `localHostname()` | F-04 自动获取 |
| F | `label!=null` ∧ `host.map` 未命中 ∧ `host.auto=false` | `null`（不加 host） | F-02 向后兼容 |
| G | try 块抛出任何异常 | `KEYTAB_HOST` 静态值 | F-03 异常降级 |

### 3.2 getKerberosUser 重构

**职责**：拼装 Kerberos principal（`userName/host` 或 `userName`）。

**改造前 → 改造后对比**：

```scala
// ===== BEFORE（改造前）=====
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  if (label == null) {
    if (KEYTAB_HOST_ENABLED.getValue) {
      user = user + "/" + KEYTAB_HOST.getValue   // 仅静态值
    }
  } else {
    val hostMap = kerberosValueMapParser(KEYTAB_HOST_MAP.getValue)
    if (hostMap.contains(label)) {
      user = user + "/" + hostMap(label)          // 仅 map 静态值
    }
  }
  user
}

// ===== AFTER（改造后）=====
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  val host = resolveKeytabHost(label)             // ← 委托给新方法
  if (StringUtils.isNotBlank(host)) {              // ← null 安全检查
    user = user + "/" + host
  }
  user
}
```

**重构要点**：
- host 解析逻辑全部下沉到 `resolveKeytabHost`，`getKerberosUser` 仅负责"拿到 host 后拼 principal"
- 用 `StringUtils.isNotBlank(host)` 替代改造前的内联 `if` 判断，`null` 和空字符串都不拼 `/`
- 方法签名 `def getKerberosUser(userName: String, label: String): String` 保持不变

### 3.3 localHostname 新增方法

**职责**：获取本机短主机名（等价 shell `hostname` 输出）。

```scala
private def localHostname(): String = InetAddress.getLocalHost.getHostName
```

| 要点 | 说明 |
|:-----|:-----|
| `getLocalHost` | 从 `/etc/hosts` 或 DNS 解析本机地址，可能抛 `UnknownHostException` |
| `getHostName` | 返回短名（非 FQDN），与 shell `hostname` 一致 |
| 使用场景 | principal 形如 `hadoop/worker01`（短名匹配 keytab 注册的 host） |
| 异常处理 | 由调用方 `resolveKeytabHost` 的 `Utils.tryCatch` 统一兜底 |

### 3.4 完整伪代码

```scala
// 配置开关声明（HadoopConf.scala）
val KEYTAB_HOST_AUTO = CommonVars("wds.linkis.keytab.host.auto", false)

// resolveKeytabHost（HDFSUtils.scala）
private def resolveKeytabHost(label: String): String = Utils.tryCatch {
  if (label == null) {
    if (KEYTAB_HOST_ENABLED.getValue) {
      if (KEYTAB_HOST_AUTO.getValue) localHostname() else KEYTAB_HOST.getValue
    } else {
      null
    }
  } else {
    val hostMap = kerberosValueMapParser(KEYTAB_HOST_MAP.getValue)
    if (hostMap.contains(label)) {
      hostMap(label)
    } else if (KEYTAB_HOST_AUTO.getValue) {
      localHostname()
    } else {
      null
    }
  }
} { case t: Throwable =>
  logger.warn(
    s"Resolve keytab host failed, fallback to static value: ${KEYTAB_HOST.getValue}",
    t
  )
  KEYTAB_HOST.getValue
}

// localHostname（HDFSUtils.scala）
private def localHostname(): String = InetAddress.getLocalHost.getHostName

// getKerberosUser（HDFSUtils.scala）
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  val host = resolveKeytabHost(label)
  if (StringUtils.isNotBlank(host)) {
    user = user + "/" + host
  }
  user
}
```

---

## 四、向后兼容性分析

### 4.1 host.auto=false 时逐场景对比

当 `host.auto=false`（默认值）时，逐场景验证改造前后行为一致。

#### 场景 1：label == null，host.enabled = false

| 维度 | 改造前 | 改造后 (host.auto=false) |
|:-----|:-------|:------------------------|
| 代码路径 | `if (KEYTAB_HOST_ENABLED.getValue)` → false → 跳过 | `resolveKeytabHost` → `host.enabled` false → 返回 null → `isNotBlank(null)` → 不拼 |
| 输出 principal | `hadoop` | `hadoop` |
| **一致** | **是** | |

#### 场景 2：label == null，host.enabled = true

| 维度 | 改造前 | 改造后 (host.auto=false) |
|:-----|:-------|:------------------------|
| 代码路径 | `host.enabled` true → `user + "/" + KEYTAB_HOST.getValue` | `resolveKeytabHost` → `host.enabled` true → `host.auto` false → 返回 `KEYTAB_HOST.getValue` → `isNotBlank` → 拼 `/` |
| 输出 principal | `hadoop/127.0.0.1` | `hadoop/127.0.0.1` |
| **一致** | **是** | |

#### 场景 3：label != null，host.map 命中

| 维度 | 改造前 | 改造后 (host.auto=false) |
|:-----|:-------|:------------------------|
| 代码路径 | `hostMap.contains(label)` true → `user + "/" + hostMap(label)` | `resolveKeytabHost` → `hostMap.contains(label)` true → 返回 `hostMap(label)` → 拼 `/` |
| 输出 principal | `hadoop/127.0.0.2` | `hadoop/127.0.0.2` |
| **一致** | **是** | |

#### 场景 4：label != null，host.map 未命中

| 维度 | 改造前 | 改造后 (host.auto=false) |
|:-----|:-------|:------------------------|
| 代码路径 | `hostMap.contains(label)` false → 不拼 host | `resolveKeytabHost` → `hostMap.contains(label)` false → `host.auto` false → 返回 null → `isNotBlank(null)` → 不拼 |
| 输出 principal | `hadoop` | `hadoop` |
| **一致** | **是** | |

### 4.2 兼容性结论

| 检查项 | 结果 |
|:------|:----:|
| host.auto=false 时 label==null 场景等价 | ✓ |
| host.auto=false 时 label!=null 场景等价 | ✓ |
| host.enabled=false 时不加 host | ✓ |
| host.map 命中时优先使用 map 值 | ✓ |
| 方法签名未变 | ✓ |
| 调用方代码未变 | ✓ |
| 新增开关默认 false | ✓ |
| 现有部署无需改动 | ✓ |

> **证明**：改造后所有"旧路径"分支（B/C/D/F）在 `host.auto=false` 时，`resolveKeytabHost` 返回值与改造前 `getKerberosUser` 内联逻辑的 host 来源完全一致，`getKerberosUser` 的拼装行为不变。

---

## 五、组合矩阵

### 5.1 正交性证明

`host.auto`（本功能）与 `keytab.switch`（AES keytab 加密）作用于两个独立方法：

```
getUserGroupInformation(userName, label)
    │
    ├── getLinkisUserKeytabFile(userName, label)   ← 受 keytab.switch 控制
    │     keytab.switch=true:  读 AES 加密文件 → 解密 → 临时文件路径
    │     keytab.switch=false: 直接返回源文件路径
    │     [返回: path]
    │
    ├── getKerberosUser(userName, label)           ← 受 host.auto 控制
    │     resolveKeytabHost(label)
    │       host.auto=true:  本机短名
    │       host.auto=false: 静态值 / null
    │     [返回: user = "hadoop/hostname" 或 "hadoop"]
    │
    └── loginUserFromKeytabAndReturnUGI(user, path)
          user 和 path 独立产出，互不影响
```

`path`（文件）和 `user`（principal）是两个独立变量，分别由不同方法、不同开关控制，在 `loginUserFromKeytabAndReturnUGI(user, path)` 时才汇合。因此 `host.auto` 与 `keytab.switch` 完全正交。

### 5.2 四组合矩阵

| 组合 | host.auto | keytab.switch | principal host | keytab 文件 | 可用性 | 说明 |
|:----:|:---------:|:-------------:|:--------------|:-----------|:------:|:-----|
| 1 | `false` | `false` | 静态值/不加 host | 源文件路径 | ✓ | 默认场景，完全向后兼容 |
| 2 | `false` | `true` | 静态值/不加 host | AES 解密临时文件 | ✓ | 原有 AES 加密模式 |
| 3 | `true` | `false` | 本机短名 | 源文件路径 | ✓ | 自动 host + 明文 keytab |
| 4 | `true` | `true` | 本机短名 | AES 解密临时文件 | ✓ | 自动 host + AES 加密 |

四种组合均能正常完成 Kerberos 登录（F-05 验收标准）。

---

## 六、异常处理设计

### 6.1 UnknownHostException 降级路径

`localHostname()` 调用 `InetAddress.getLocalHost.getHostName`，可能抛 `UnknownHostException`（本机 `/etc/hosts` 未正确配置时）。该异常被 `resolveKeytabHost` 的 `Utils.tryCatch` 兜底。

```
resolveKeytabHost(label)
│
├── [TRY] 分支 A/E 调用 localHostname()
│   │
│   └── InetAddress.getLocalHost
│       │
│       ├── 成功 ──> getHostName ──> 返回短名（如 "worker01"）
│       │
│       └── 抛出 UnknownHostException
│               │
│               ▼
├── [CATCH] Utils.tryCatch 捕获
│   │
│   ├── logger.warn("Resolve keytab host failed, fallback ...", t)
│   │
│   └── 返回 KEYTAB_HOST.getValue（静态值，如 "127.0.0.1"）
│           │
│           ▼
└── getKerberosUser 拿到静态值，拼 principal "hadoop/127.0.0.1"
        │
        ▼
    loginUserFromKeytabAndReturnUGI 正常执行
    [认证主链路不中断]
```

### 6.2 异常场景与处理策略

| 场景 | 异常类型 | 触发条件 | 处理策略 | 日志级别 |
|:-----|:--------|:--------|:--------|:--------:|
| 本机名解析失败 | `UnknownHostException` | `/etc/hosts` 未配置本机名 | 降级回静态 `KEYTAB_HOST` 值 | WARN |
| host.map 解析异常 | `RuntimeException` | `KEYTAB_HOST_MAP` 格式异常 | 同上（tryCatch 统一兜底） | WARN |
| 其他未预料异常 | `Throwable` | 未知 | 同上 | WARN |

### 6.3 日志设计

| 级别 | 场景 | 日志内容 |
|:----:|:-----|:--------|
| WARN | 自动获取失败降级 | `Resolve keytab host failed, fallback to static value: {KEYTAB_HOST}` + 异常堆栈 |
| INFO | Kerberos 登录 | `Performing Kerberos login with keytab - user: {user}, label: {label}`（既有日志） |

**设计要点**：降级日志包含静态值，运维人员可据此判断降级后的 principal 是否有效。

### 6.4 与 CLAUDE.md §4 铁律的对应

| 铁律条目 | 本设计落地 |
|:--------|:----------|
| §4.1 开关默认 false | `KEYTAB_HOST_AUTO` 默认 `false` |
| §4.2 Utils.tryCatch 包裹 | `resolveKeytabHost` 整体 `Utils.tryCatch` 包裹 |
| §4.2 catch 分支降级 | 降级回 `KEYTAB_HOST.getValue`，认证主链路不中断 |
| §4.2 日志用项目 logger | `logger.warn(...)`（`HDFSUtils extends Logging`） |

---

## 七、配置项设计

### 7.1 新增配置项

| 配置项 | 默认值 | 说明 |
|:------|:------|:-----|
| `wds.linkis.keytab.host.auto` | `false` | 当 `host.enabled=true` 时，是否自动取本机短名作为 principal host。`false` 时用静态 `KEYTAB_HOST` 值（向后兼容） |

**配置声明**（`HadoopConf.scala` line 52）：

```scala
/**
 * When host.enabled=true, whether to automatically resolve the principal host from the local
 * machine's short hostname (equivalent to shell `hostname`, e.g. hadoop/${hostname}). Default
 * false to stay backward compatible; when disabled the static KEYTAB_HOST value is used. When
 * enabled, the resolved hostname must exactly match the one registered in the keytab, otherwise
 * Kerberos login will fail.
 */
val KEYTAB_HOST_AUTO = CommonVars("wds.linkis.keytab.host.auto", false)
```

### 7.2 既有相关配置项

| 配置项 | 默认值 | 与本功能的关系 |
|:------|:------|:-------------|
| `wds.linkis.keytab.enable` | `false` | 总开关，控制是否走 Kerberos 认证 |
| `wds.linkis.keytab.host.enabled` | `false` | 控制 principal 是否拼 host 部分。`false` 时本功能不生效（分支 C/F 返回 null） |
| `wds.linkis.keytab.host` | `127.0.0.1` | 静态 host 值。`host.auto=false` 时使用；`host.auto=true` 异常降级时兜底 |
| `linkis.keytab.host.map` | `cluster1=127.0.0.2,cluster2=127.0.0.3` | 多集群 host 映射。`label!=null` 时优先于 `host.auto`（分支 D） |
| `linkis.copy.keytab.file.switch` | `false` | AES keytab 开关，与本功能正交 |

### 7.3 典型部署配置示例

```properties
# ===== 集群统一 kinit -kt hadoop.keytab hadoop/${hostname} 方式 =====
wds.linkis.keytab.enable=true
wds.linkis.keytab.host.enabled=true
wds.linkis.keytab.host.auto=true        # 新增：自动取本机短名

# 不再需要逐机配置（被 host.auto 取代）：
# wds.linkis.keytab.host=worker01       # 旧方式：每台机器不同，需逐机改
```

---

## 八、验收标准对应

### 8.1 功能验收映射

| 验收标准 | 对应设计分支 | 验证方法 |
|:--------|:-----------|:--------|
| F-01: `host.auto=true` 时 principal 带本机短名 | 分支 A | `label=null` + `host.enabled=true` + `host.auto=true` → principal = `hadoop/{hostname}` |
| F-02: `host.auto=false` 时与改造前一致 | 分支 B/C/D/F | 见 §4.1 四场景对比表 |
| F-03: `UnknownHostException` 降级到静态值 | 分支 G | mock `InetAddress.getLocalHost` 抛异常 → principal = `hadoop/127.0.0.1` |
| F-04: 多集群 label 未配 host.map 自动回退 | 分支 E | `label="cluster3"` + `host.map` 无 cluster3 + `host.auto=true` → principal = `hadoop/{hostname}` |
| F-05: `host.auto` × `keytab.switch` 四组合可用 | §5.2 四组合 | 四种组合各跑一次 Kerberos 登录冒烟 |
| F-06: host.map 命中时优先于 host.auto | 分支 D | `label="cluster1"` + `host.map` 有 cluster1 + `host.auto=true` → principal = `hadoop/{map值}` |

### 8.2 兼容性验收

| 验收标准 | 状态 |
|:--------|:----:|
| 开关默认 `false`，现有部署无需改动 | ✓（`CommonVars` 第二参数 `false`） |
| 编译通过 `linkis-hadoop-common BUILD SUCCESS` | ✓（已验证） |

### 8.3 配置验收

| 验收标准 | 状态 |
|:--------|:----:|
| `linkis.properties` 含 `wds.linkis.keytab.host.auto` 注释行 | 待同步 |
| `docs/configuration/linkis-hadoop-common.md` 含新配置项说明 | 待同步 |

---

## 九、回滚方案

### 9.1 回滚条件

- 自动获取的 hostname 与 keytab 注册的 principal 不匹配导致登录失败
- `InetAddress.getLocalHost` 在特定环境返回非预期值

### 9.2 回滚方式

**方式 1（推荐，零代码改动）**：将 `wds.linkis.keytab.host.auto` 设为 `false`，重启服务。行为完全回退到改造前。

**方式 2（代码级回滚）**：`git revert <commit>`，`getKerberosUser` 恢复为内联逻辑。

### 9.3 回滚验证

- [ ] `host.auto=false` 后 principal 不含本机短名
- [ ] Kerberos 登录恢复正常（使用静态 `KEYTAB_HOST`）
- [ ] 现有任务提交流程不受影响

---

## 十、风险与预案

| 风险 | 影响 | 概率 | 应对措施 |
|:-----|:-----|:----:|:--------|
| R-01 自动获取的 hostname 与 keytab 注册的 principal 不一致（短名 vs FQDN） | Kerberos 登录失败 | 中 | 部署前 `hostname` 与 `klist -k hadoop.keytab` 核对；选用 `getHostName`（短名）匹配 shell `hostname` |
| R-02 `InetAddress.getLocalHost` 抛 `UnknownHostException` | 降级到静态值 | 低 | `Utils.tryCatch` 降级回 `KEYTAB_HOST` |
| R-03 机器 `/etc/hosts` 未正确配置本机名 | `getLocalHost` 返回非预期值 | 中 | 部署文档强调 `/etc/hosts` 配置；降级兜底 |

---

## 十一、附录

### 11.1 改动文件清单

| 文件 | 路径 |
|:-----|:-----|
| HadoopConf.scala | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala` |
| HDFSUtils.scala | `linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala` |

### 11.2 参考文档

1. 需求文档：`docs/dev-2.0.0/requirements/keytab-hostname-auth_需求.md`
2. CLAUDE.md §4 新功能开发铁律（开关 + tryCatch 降级）
3. CLAUDE.md §5 代码规范（ASF License、scalastyle）
4. Hadoop Kerberos 认证文档（principal 格式 `service/host@REALM`）

### 11.3 完整代码变更

<details>
<summary>HadoopConf.scala - 新增 KEYTAB_HOST_AUTO 开关</summary>

```scala
/**
 * When host.enabled=true, whether to automatically resolve the principal host from the local
 * machine's short hostname (equivalent to shell `hostname`, e.g. hadoop/${hostname}). Default
 * false to stay backward compatible; when disabled the static KEYTAB_HOST value is used. When
 * enabled, the resolved hostname must exactly match the one registered in the keytab, otherwise
 * Kerberos login will fail.
 */
val KEYTAB_HOST_AUTO = CommonVars("wds.linkis.keytab.host.auto", false)
```

</details>

<details>
<summary>HDFSUtils.scala - getKerberosUser 重构 + resolveKeytabHost + localHostname</summary>

```scala
def getKerberosUser(userName: String, label: String): String = {
  var user = userName
  val host = resolveKeytabHost(label)
  if (StringUtils.isNotBlank(host)) {
    user = user + "/" + host
  }
  user
}

/**
 * Resolve the host part of the kerberos principal.
 *
 *   - label == null: when host.enabled=true, uses the static KEYTAB_HOST value; if host.auto=true
 *     the local machine short hostname (equivalent to shell `hostname`) is used instead. When
 *     host.enabled=false, no host is appended.
 *   - label != null: prefers the value in linkis.keytab.host.map for that label; if absent and
 *     host.auto=true, falls back to the local machine short hostname; if absent and
 *     host.auto=false, no host is appended.
 *
 * Any failure (e.g. UnknownHostException) is caught and falls back to the static KEYTAB_HOST
 * value, so the kerberos login main path is never broken by the auto-resolution feature. Backward
 * compatible: with host.auto=false (default) the behavior is identical to before.
 */
private def resolveKeytabHost(label: String): String = Utils.tryCatch {
  if (label == null) {
    if (KEYTAB_HOST_ENABLED.getValue) {
      if (KEYTAB_HOST_AUTO.getValue) localHostname() else KEYTAB_HOST.getValue
    } else {
      null
    }
  } else {
    val hostMap = kerberosValueMapParser(KEYTAB_HOST_MAP.getValue)
    if (hostMap.contains(label)) {
      hostMap(label)
    } else if (KEYTAB_HOST_AUTO.getValue) {
      localHostname()
    } else {
      null
    }
  }
} { case t: Throwable =>
  logger.warn(s"Resolve keytab host failed, fallback to static value: ${KEYTAB_HOST.getValue}", t)
  KEYTAB_HOST.getValue
}

/**
 * Local machine short hostname, equivalent to shell `hostname`. Used to build principal like
 * `hadoop/${hostname}`. The returned value must exactly match the host registered in the keytab.
 */
private def localHostname(): String = InetAddress.getLocalHost.getHostName
```

</details>
