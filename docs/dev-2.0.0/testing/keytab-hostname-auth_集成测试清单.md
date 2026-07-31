# Keytab Principal Host 自动获取 - 集成测试清单

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本 |

> 本清单用于在**真实 Kerberos 集群**验证 `wds.linkis.keytab.host.auto` 改造。单测受 `CommonVars.getValue` 的 `val` 缓存限制无法覆盖 `auto=true` 分支，此处通过端到端任务执行验证。

---

## 一、测试目标

验证以下单测未覆盖项在真实环境的正确性：

| 验证项 | 对应单测缺口 | 集成验证方式 |
|:---|:---|:---|
| `host.auto=true` 时 principal 自动带本机短名 | TC-03/TC-05 | 提交 HDFS 任务，登录成功即证明 principal 拼装正确 |
| `host.auto` × `keytab.switch` 四组合 | CB-01~04 | 分别配置四组合，各提交一次任务 |
| 代理模式 + auto（对接 `kinit -kt hadoop.keytab hadoop/${hostname}`） | IT-02 | 代理模式配置 + auto，提交任务 |
| 异常降级（hostname 不一致） | TC-07 | 构造不一致场景，确认降级或报错符合预期 |
| 向后兼容（auto=false 默认） | BC-01~04 | 不改配置，任务行为与改造前一致 |

---

## 二、前置环境

### 2.1 Kerberos 集群
- KDC 已部署，REALM 例 `EXAMPLE.COM`
- 已创建 principal `hadoop/<hostname>@EXAMPLE.COM`（`<hostname>` 为各 Linkis 服务机的**短名**）
- keytab 文件 `hadoop.keytab` 已分发到每台 Linkis 服务机

### 2.2 Linkis 服务机准备（每台）

```bash
# 1. 确认本机短名（必须与 keytab principal 的 host 完全一致）
hostname                          # 期望输出：如 node1（短名，不含域）

# 2. 确认 keytab 内 principal
klist -kt /appcom/keytab/hadoop.keytab
# 期望看到：hadoop/node1@EXAMPLE.COM（host 部分与上一步 hostname 输出一致）

# 3. 确认 /etc/hosts 含本机名解析（避免 InetAddress.getLocalHost 抛 UnknownHostException）
cat /etc/hosts | grep $(hostname)

# 4. 用 kinit 手工验证（与机器上既定认证方式一致）
kinit -kt /appcom/keytab/hadoop.keytab hadoop/$(hostname)
klist                             # 期望看到 ticket
```

> ⚠️ **关键**：`hostname` 输出必须与 keytab 里 principal 的 host **逐字符一致**。若 keytab 注册的是 FQDN（`node1.example.com`），则 `getHostName` 返回短名会不匹配 → 登录失败，需改用 `getCanonicalHostName`（本期未实现，见需求 Q-01）。

### 2.3 Linkis 服务
- 已部署 `linkis-entrance`、`linkis-cg-engineconnmanager`、`linkis-ps-publicservice`（涉及 HDFS 操作的服务）
- 涉及服务进程的 OS 用户对 `hadoop.keytab` 有读权限

---

## 三、测试场景

### 场景 1：回归 — `host.auto=false`（默认，向后兼容）

**目的**：确认改造未破坏现有部署。

**配置**（`linkis.properties`，保持默认或显式关闭）：
```properties
wds.linkis.keytab.enable=true
wds.linkis.keytab.proxyuser.enable=true
wds.linkis.keytab.proxyuser.superuser=hadoop
wds.linkis.keytab.host.enabled=true
wds.linkis.keytab.host.auto=false
wds.linkis.keytab.host=<本机短名>       # 老方式：静态配置
wds.linkis.keytab.file=/appcom/keytab/
```

**操作**：重启 Linkis 服务 → 提交一个 HDFS 任务（如读取/写入文件）。

**预期**：
- 任务成功，HDFS 操作正常
- 日志无 Kerberos 错误
- 行为与改造前一致

---

### 场景 2：核心 — `host.auto=true` + 代理模式（对接 kinit 认证）

**目的**：验证 principal host 自动取本机短名，免逐机配置。

**配置**：
```properties
wds.linkis.keytab.enable=true
wds.linkis.keytab.proxyuser.enable=true
wds.linkis.keytab.proxyuser.superuser=hadoop
wds.linkis.keytab.host.enabled=true
wds.linkis.keytab.host.auto=true          # ★ 新开关
wds.linkis.keytab.file=/appcom/keytab/
# 注意：不再需要 wds.linkis.keytab.host 静态配置
```

**操作**：
1. 重启 Linkis 服务
2. 提交 HDFS 任务（linkis-cli 或 REST）：
   ```bash
   ./linkis-cli -engineType spark-2.4.3 -code "val fs = org.apache.hadoop.fs.FileSystem.get(new org.apache.hadoop.conf.Configuration()); fs.listStatus(new org.apache.hadoop.fs.Path(\"/tmp\"))" -codeType scala
   ```
   或提交一个 Hive 查询读取 HDFS 数据。

**预期**：
- 任务成功
- Linkis 日志（entrance/ecm）出现：
  ```
  Getting UserGroupInformation - user: ..., authMethod: kerberos
  Performing Kerberos login with proxy user - user: ..., superUser: hadoop, ...
  ```
- **无** `KrbException` / `Principal not found` / `host not matched` 错误
- 多台服务机各自用本机 hostname 登录成功（无需逐机改配置）

**判定**：任务成功 = principal `hadoop/<本机hostname>` 拼装正确 = `auto=true` 分支工作 ✅

---

### 场景 3：组合 — `host.auto=true` + `keytab.switch=true`（AES 加密 keytab）

**目的**：验证 host 自动获取与 AES 加密 keytab 正交（CB-03）。

**前置**：将 `hadoop.keytab` 用 `AESUtils.encrypt(明文, AESUtils.PASSWORD)` 加密后放 `linkis.copy.keytab.file` 目录。

**配置**：
```properties
wds.linkis.keytab.enable=true
wds.linkis.keytab.proxyuser.enable=true
wds.linkis.keytab.proxyuser.superuser=hadoop
wds.linkis.keytab.host.enabled=true
wds.linkis.keytab.host.auto=true
linkis.keytab.switch=true                 # ★ AES 加密 keytab
linkis.copy.keytab.file=/mnt/bdap/keytab/  # 加密 hadoop.keytab 所在目录
```

**操作**：重启服务 → 提交 HDFS 任务。

**预期**：
- 任务成功
- 日志出现 keytab 解密 + 临时文件创建（`Created and cached fixed keytab file: ...`）
- principal 带本机短名，Kerberos 登录成功

**判定**：`host.auto` × `keytab.switch` 双开可用 ✅

---

### 场景 4：多集群 — label 未配 host.map 时 auto 回退

**目的**：验证 `label != null` 且 `host.map` 未命中该 label 时，`auto=true` 回退本机短名（TC-05/F-04）。

**配置**：
```properties
wds.linkis.keytab.enable=true
wds.linkis.keytab.host.enabled=true
wds.linkis.keytab.host.auto=true
linkis.keytab.host.map=cluster1=host1.example.com   # 仅配 cluster1
# cluster2 不在 map 中 → 应回退本机短名
```

**操作**：提交一个带 `label=cluster2` 的任务（通过 creator/label 路由到 cluster2）。

**预期**：
- 任务成功，principal 用本机短名（非 host.map 值，因为 cluster2 未配）

---

### 场景 5：异常降级 — hostname 与 keytab 不一致

**目的**：观察 `host.auto=true` 但本机 hostname 与 keytab principal 不匹配时的行为。

**操作**：
1. 临时修改机器 hostname（或用一台 hostname 与 keytab 不符的机器）：
   ```bash
   hostname wrong-hostname
   ```
2. 保持 `host.auto=true`，提交任务。

**预期**：
- `InetAddress.getLocalHost.getHostName` 返回 `wrong-hostname`
- principal 拼成 `hadoop/wrong-hostname`，与 keytab 不符
- Kerberos 登录失败，日志报 `KrbException: Principal not found / Client not found in Kerberos database`
- **降级说明**：此场景是 hostname 错误（非代码 bug），`Utils.tryCatch` 只兜底 `InetAddress` 抛异常的情况，不兜底"hostname 值错误"——这是预期行为，需运维保证 hostname 正确

**恢复**：`hostname <正确短名>` 恢复。

---

## 四、验证 Checklist

执行完各场景后逐项确认：

- [ ] 场景1：`auto=false` 回归，任务成功，行为不变
- [ ] 场景2：`auto=true` + 代理模式，多机各自本机 hostname 登录成功
- [ ] 场景3：`auto=true` + `keytab.switch=true`，AES keytab + 自动 host 双开可用
- [ ] 场景4：label 未配 host.map 时 auto 回退本机短名
- [ ] 场景5：hostname 不一致时 Kerberos 登录失败（符合预期，非降级场景）
- [ ] 全程 Linkis 服务无 OOM / 无认证主链路中断
- [ ] TGT 自动续期线程正常（日志 `kerberos Refresh tread started`）

---

## 五、故障排查

| 现象 | 可能原因 | 排查 |
|:---|:---|:---|
| `Principal not found` / `Client not found in Kerberos database` | `hostname` 与 keytab principal 的 host 不一致 | `hostname` 与 `klist -kt hadoop.keytab` 比对 |
| `UnknownHostException` | `/etc/hosts` 未配本机名 | 检查 `/etc/hosts`；此异常会触发 `Utils.tryCatch` 降级回静态 `KEYTAB_HOST`，看日志 `Resolve keytab host failed, fallback to static value` |
| 登录用了静态 host 而非本机短名 | `host.auto` 未生效（可能 `host.enabled=false` 或开关未开） | 确认 `wds.linkis.keytab.host.enabled=true` + `host.auto=true` |
| 多机某台失败 | 该机 `/etc/hosts` 或 hostname 异常 | 单独排查该机 `hostname` + `klist` |
| `linkis.keytab.switch=true` 时解密失败 | keytab 未用 `AESUtils.encrypt` 加密，或放错目录 | 确认放 `linkis.copy.keytab.file` 目录且为加密版 |

---

## 六、回滚

任何场景异常，立即回滚：
```properties
wds.linkis.keytab.host.auto=false   # 关闭新开关，回退静态 host 配置
```
重启服务即恢复改造前行为（开关默认 false，向后兼容）。

---

## 七、测试结论模板

执行完成后填写：

| 场景 | 执行机 | 配置 | 任务结果 | 日志关键行 | 结论 |
|:---:|:---|:---|:---:|:---|:---:|
| 1 | node1 | auto=false | 成功/失败 | ... | ✅/❌ |
| 2 | node1 | auto=true | 成功/失败 | ... | ✅/❌ |
| 2 | node2 | auto=true | 成功/失败 | ... | ✅/❌ |
| 3 | node1 | auto+switch | 成功/失败 | ... | ✅/❌ |
| 4 | node1 | label=cluster2 | 成功/失败 | ... | ✅/❌ |
| 5 | node1 | hostname 错误 | 登录失败 | KrbException | ✅(符合预期) |

**总体结论**：______ （通过 / 不通过）
**测试人 / 日期**：______
