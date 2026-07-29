# Keytab Principal Host 自动获取 - 测试报告

| 版本 | 日期 | 作者 | 变更说明 |
|:----:|:----:|:----:|:--------|
| 1.0 | 2026-07-14 | DevSyncAgent | 初始版本 |
| 1.1 | 2026-07-24 | DevSyncAgent | 简化重构后：单开关 `host.enabled`，删除 host.auto/host.map 相关用例 |
| 1.2 | 2026-07-29 | DevSyncAgent | 收窄：resolveKeytabHost 加 superUser 判定，仅超级用户（默认 hadoop）拼 host；测试 3/3 通过 |

---

## 一、测试概述

### 1.1 测试对象
`HDFSUtils.resolveKeytabHost` / `getKerberosUser` / `localHostname`（keytab principal host 自动获取改造 v1.2，单开关 + superUser 收窄版）。

### 1.2 测试环境
| 项 | 值 |
|:---|:---|
| 模块 | `linkis-commons/linkis-hadoop-common` |
| 测试类 | `org.apache.linkis.hadoop.common.utils.HDFSUtilsKeytabHostTest`、`org.apache.linkis.hadoop.common.conf.HadoopConfTest` |
| 框架 | JUnit Jupiter 5.7.2 + 反射私有方法 |
| 执行命令 | `./mvnw -pl linkis-commons/linkis-hadoop-common -Dtest=HDFSUtilsKeytabHostTest,HadoopConfTest -Dsurefire.failIfNoSpecifiedTests=false test` |

---

## 二、执行结果

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

（`HadoopConfTest` 1 个 + `HDFSUtilsKeytabHostTest` 2 个）

**结论：✅ 全部通过。**

---

## 三、已通过用例明细

| 用例 | 覆盖 | 结果 |
|:-----|:-----|:----:|
| `HDFSUtilsKeytabHostTest.testResolveDefaultNoHost` | `host.enabled=false`（默认）→ `resolveKeytabHost()` 返回 null（不拼 host） | ✅ |
| `HDFSUtilsKeytabHostTest.testGetKerberosUserNoHostByDefault` | `getKerberosUser("hadoop", null)` 默认 → principal = `hadoop`（向后兼容） | ✅ |
| `HadoopConfTest.constTest` | `KEYTAB_HOST=127.0.0.1`、`KEYTAB_HOST_ENABLED=false` 等默认值断言 | ✅ |

**覆盖结论**：默认开关（`host.enabled=false`）下 principal 不拼 host，与改造前一致；`HadoopConf` 保留配置项默认值不变。

---

## 四、未单测覆盖项及替代验证

| 项 | 未覆盖原因 | 替代验证方式 |
|:----:|:--------|:--------|
| `host.enabled=true` 且 `userName==superUser`（拼本机主机名） | `CommonVars.getValue` 为 `val`（HadoopConf object 初始化期缓存），运行时 `System.setProperty` 覆盖不可靠 | **代码审查**（`resolveKeytabHost(userName,label)` 中 `userName == getKeytabSuperUser(label)` 判定）+ 真实 Kerberos 集群集成测试 |
| `userName != superUser`（不拼 host） | 同上缓存限制 | **代码审查**（同上，非 superUser 返回 null） |
| `UnknownHostException` 异常（分支 C，返回 null） | 需异常注入环境 | **代码审查**确认 `resolveKeytabHost` 整体被 `Utils.tryCatch` 包裹，catch 返回 null（HDFSUtils.scala，符合 CLAUDE.md §4.2） |
| `host.enabled` × `keytab.switch` 四组合 | 需真实 Kerberos 环境 | **设计文档组合矩阵**（§5.2）+ principal(`getKerberosUser`) 与 keytab 路径(`getLinkisUserKeytabFile`) 独立产出的正交性 |

> `localHostname` 为 Scala `private` 简单方法，可能被 scalac 内联、反射不可达；其正确性由 `resolveKeytabHost` 的 `host.enabled=true` 分支与代码审查间接保证。

---

## 五、编译验证

- **main 编译**：`linkis-hadoop-common BUILD SUCCESS`（含 HadoopConf.scala 删除 `KEYTAB_HOST_AUTO` + HDFSUtils.scala 单开关重构）
- **test 编译**：`HDFSUtilsKeytabHostTest.scala` 编译通过（scalastyle/spotless 无报错）
- **依赖编译**：`linkis-common` 同 reactor 编译成功

---

## 六、风险与建议

| 项 | 说明 |
|:---|:---|
| 集成测试 | 在真实 Kerberos 集群执行：`host.enabled=true` → principal `hadoop/<hostname>` 与 `klist -k hadoop.keytab` 注册值一致 |
| principal 一致性 | 部署前核对 `InetAddress.getLocalHost.getHostName` 输出与 keytab 注册的 principal host 完全一致 |
| `/etc/hosts` | 确保机器本机名可解析，避免 `InetAddress.getLocalHost` 抛 `UnknownHostException`（此时降级为不拼 host） |

---

## 七、最终结论

| 维度 | 结果 |
|:---|:---:|
| 单元测试 | ✅ 3/3 通过 |
| 编译 | ✅ 通过 |
| 向后兼容 | ✅ 默认开关下 principal 不拼 host |
| 代码审查覆盖剩余分支 | ✅ 单开关 3 分支 + 异常返回 null + 组合矩阵 |
| **总体** | **✅ 测试通过，可进入灰度部署** |
