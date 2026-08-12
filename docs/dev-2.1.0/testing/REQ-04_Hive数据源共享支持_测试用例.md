# 测试用例：REQ-04 Hive数据源共享支持（功能开关控制）

> 需求：【WDSL-UJES】【中风险】【运维优化】【BDP】【datasource】【hive】【后端】Hive数据源共享支持（功能开关控制）
> 版本：dev-2.1.0
> DPMS Story ID：539155
> 创建日期：2026-08-11

---

## 一、测试范围

| 测试类型 | 覆盖内容 |
|---------|---------|
| 单元测试 | isHiveShareEnabled方法、principal解析、本地keytab路径构造、keytab文件存在性检查 |
| 接口测试 | reqToGetDataSourceInfo权限校验、queryDataSourceInfoByNameAndEnvId权限校验、HiveMetaService.getConnection |
| 功能测试 | 开关开启/关闭时的完整访问流程、管理员/创建者/非创建者权限 |
| 兼容性测试 | 开关关闭时原有行为不变、非Hive数据源不受影响、非Kerberos连接不受影响 |
| 配置测试 | 配置项默认值、配置文件声明、keytab路径自定义 |

---

## 二、代码变更分析

### 变更文件

| 文件路径 | 变更类型 | 新增方法 | 修改方法 |
|---------|:--------:|:-------:|:-------:|
| MdmConfiguration.java | MODIFIED | 2（配置常量） | 0 |
| MetadataQueryServiceImpl.java | MODIFIED | 1（isHiveShareEnabled） | 2（reqToGetDataSourceInfo, queryDataSourceInfoByNameAndEnvId） |
| HiveMetaService.java | MODIFIED | 0 | 1（getConnection） |
| linkis-ps-publicservice.properties | MODIFIED | 2（配置项） | 0 |

### 新增/修改方法详情

#### MdmConfiguration.java
**HIVE_DATASOURCE_SHARE_ENABLE**：`CommonVars<Boolean>`，key=`linkis.datasource.hive.share.enable`，默认`false`
**HIVE_DATASOURCE_SHARE_KEYTAB_PATH**：`CommonVars<String>`，key=`linkis.datasource.hive.share.keytab.path`，默认`/mnt/bdap/keytab/`

#### MetadataQueryServiceImpl.java - isHiveShareEnabled(String dsType)
**变更类型**：NEW
**方法签名**：`private boolean isHiveShareEnabled(String dsType)`
**逻辑**：读取HIVE_DATASOURCE_SHARE_ENABLE配置，若为true且dsType为"hive"（忽略大小写）则返回true，否则返回false

#### MetadataQueryServiceImpl.java - reqToGetDataSourceInfo / queryDataSourceInfoByNameAndEnvId
**变更类型**：MODIFIED
**改动**：hasPermission判断条件新增 `|| isHiveShareEnabled(response.getDsType())`

#### HiveMetaService.java - getConnection
**变更类型**：MODIFIED
**改动**：当开关开启且principle非空时，使用本地keytab路径（`{keytabDir}{principalPrimary}.keytab`）替代BML下载；若本地文件不存在则抛出MetaRuntimeException

### 影响范围评估
- 直接影响：MetadataQueryServiceImpl（权限校验）、HiveMetaService（keytab获取）
- 间接影响：HiveConnection（接收keytab路径，不关心来源）
- 不影响：其他数据源类型、BML服务、引擎侧、非Kerberos连接

---

## 三、控制流分析

### isHiveShareEnabled 控制流

```
输入: dsType
├─ HIVE_DATASOURCE_SHARE_ENABLE == false → return false
└─ HIVE_DATASOURCE_SHARE_ENABLE == true
   ├─ dsType == null → "hive".equalsIgnoreCase(null) == false → return false
   ├─ "hive".equalsIgnoreCase(dsType) == false → return false
   └─ "hive".equalsIgnoreCase(dsType) == true → log info, return true
```

### HiveMetaService.getConnection keytab分支 控制流

```
输入: principle, keytabResourceId, hiveShareEnabled
├─ StringUtils.isNotBlank(principle) == false → 非Kerberos连接
└─ StringUtils.isNotBlank(principle) == true
   ├─ hiveShareEnabled == true
   │  ├─ 提取 principalPrimary = principle.split("/")[0].split("@")[0]
   │  ├─ 构造 keytabFilePath = keytabDir + principalPrimary + ".keytab"
   │  ├─ keytabFile.exists() == false → throw MetaRuntimeException("Local keytab file not found")
   │  └─ keytabFile.exists() == true → 使用本地keytab
   └─ hiveShareEnabled == false
      ├─ StringUtils.isNotBlank(keytabResourceId) == true → BML下载（原逻辑）
      ├─ StringUtils.isNotBlank(keytabResourceId) == false → throw MetaRuntimeException("Cannot find keytab file")
      └─ 下载失败 → throw MetaRuntimeException("Fail to download resource")
```

### 关键路径汇总

| 路径编号 | 描述 | 条件 | 预期结果 |
|---------|------|------|---------|
| P1 | 开关关闭+非创建者+非管理员+Hive | share=false, isAdmin=false, isCreator=false, dsType=hive | hasPermission=false, 拦截 |
| P2 | 开关开启+非创建者+非管理员+Hive | share=true, isAdmin:Admin=false, isCreator=false, dsType=hive | hasPermission=true, 放行 |
| P3 | 开关开启+非创建者+非管理员+非Hive | share=true, is:Admin=false, isCreator=false, dsType=spark | hasPermission=false, 拦截 |
| P4 | 管理员+任意开关+任意类型 | isAdmin=true | hasPermission=true, 放行 |
| P5 | 创建者+任意开关+Hive | isCreator=true | hasPermission=true, 放行 |
| P6 | 开关开启+本地keytab存在 | share=true, keytabFile.exists()=true | 使用本地keytab |
| P7 | 开关开启+本地keytab不存在 | share=true, keytabFile.exists()=false | 抛出MetaRuntimeException |
| P8 | 开关关闭+BML下载 | share=false, keytabResourceId非空 | 走BML下载 |
| P9 | principal格式hadoop@REALM | principle="hadoop@REALM" | principalPrimary="hadoop" |
| P10 | principal格式hadoop/host@REALM | principle="hadoop/host@REALM" | principalPrimary="hadoop" |

---

## 四、测试用例

### 4.1 权限校验测试（MetadataQueryServiceImpl）

#### TC-REQ04-001：开关关闭时非创建者非管理员访问Hive数据源被拦截

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-001 |
| **测试名称** | 开关关闭时非创建者非管理员访问Hive数据源被拦截 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, reqToGetDataSourceInfo方法, 路径P1 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；当前用户既非管理员也非数据源创建者；数据源类型为Hive |
| **测试步骤** | 1. 以非创建者非管理员用户调用`reqToGetDataSourceInfo(dataSourceId, system, userName)`<br>2. 验证hasPermission计算结果<br>3. 验证是否抛出ErrorException |
| **预期结果** | hasPermission=false；抛出ErrorException，消息为"Don't have query permission for data source [没有数据源的查询权限]" |
| **测试数据** | userName="zhangsan", creator="hadoop", dsType="hive", isAdmin=false |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 负向场景 - 开关关闭保持原有行为 |

---

#### TC-REQ04-002：开关开启时非创建者非管理员访问Hive数据源放行

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-002 |
| **测试名称** | 开关开启时非创建者非管理员访问Hive数据源放行 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, reqToGetDataSourceInfo方法, 路径P2 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；当前用户既非管理员也非数据源创建者；数据源类型为Hive |
| **测试步骤** | 1. 以非创建者非管理员用户调用`reqToGetDataSourceInfo(dataSourceId, system, userName)`<br>2. 验证hasPermission计算结果<br>3. 验证日志输出包含"Hive datasource share is enabled" |
| **预期结果** | hasPermission=true；正常返回DsInfoResponse；日志包含"Hive datasource share is enabled, allowing access for non-creator user to hive datasource" |
| **测试数据** | userName="zhangsan", creator="hadoop", dsType="hive", isAdmin=false |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 开关开启核心功能 |

---

#### TC-REQ04-003：开关开启时非创建者访问非Hive数据源仍被拦截

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-003 |
| **测试名称** | 开关开启时非创建者访问非Hive数据源仍被拦截 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, isHiveShareEnabled方法, 路径P3 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；当前用户既非管理员也非数据源创建者；数据源类型为Spark |
| **测试步骤** | 1. 以非创建者非管理员用户调用`reqToGetDataSourceInfo`，数据源类型为"spark"<br>2. 验证isHiveShareEnabled("spark")返回false<br>3. 验证hasPermission计算结果 |
| **预期结果** | isHiveShareEnabled("spark")=false；hasPermission=false；抛出权限异常 |
| **测试数据** | userName="zhangsan", creator="hadoop", dsType="spark", isAdmin=false |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 边界场景 - 开关仅对Hive生效 |

---

#### TC-REQ04-004：开关关闭时管理员访问Hive数据源正常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-004 |
| **测试名称** | 开关关闭时管理员访问Hive数据源正常 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, 路径P4 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；当前用户为管理员 |
| **测试步骤** | 1. 以管理员用户调用`reqToGetDataSourceInfo`<br>2. 验证hasPermission计算结果 |
| **预期结果** | hasPermission=true（因isAdmin=true）；正常返回DsInfoResponse |
| **测试数据** | userName="hadoop"(管理员), creator="otherUser", dsType="hive", isAdmin=true |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 管理员不受开关影响 |

---

#### TC-REQ04-005：开关开启时管理员访问Hive数据源正常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-005 |
| **测试名称** | 开关开启时管理员访问Hive数据源正常 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, 路径P4 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；当前用户为管理员 |
| **测试步骤** | 1. 以管理员用户调用`reqToGetDataSourceInfo`<br>2. 验证hasPermission计算结果 |
| **预期结果** | hasPermission=true；正常返回DsInfoResponse |
| **测试数据** | userName="hadoop"(管理员), creator="otherUser", dsType="hive", isAdmin=true |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 管理员不受开关影响 |

---

#### TC-REQ04-006：开关关闭时创建者访问Hive数据源正常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-006 |
| **测试名称** | 开关关闭时创建者访问Hive数据源正常 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, 路径P5 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；当前用户为数据源创建者 |
| **测试步骤** | 1. 以创建者用户调用`reqToGetDataSourceInfo`<br>2. 验证hasPermission计算结果 |
| **预期结果** | hasPermission=true（因userName.equals(creator)）；正常返回DsInfoResponse |
| **测试数据** | userName="hadoop", creator="hadoop", dsType="hive", isAdmin=false |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 创建者不受开关影响 |

---

#### TC-REQ04-007：开关开启时创建者访问Hive数据源正常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-007 |
| **测试名称** | 开关开启时创建者访问Hive数据源正常 |
| **来源** | 代码变更分析 - MetadataQueryServiceImpl.java, 路径P5 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；当前用户为数据源创建者 |
| **测试步骤** | 1. 以创建者用户调用`reqToGetDataSourceInfo`<br>2. 验证hasPermission计算结果 |
| **预期结果** | hasPermission=true；正常返回DsInfoResponse |
| **测试数据** | userName="hadoop", creator="hadoop", dsType="hive", isAdmin=false |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 创建者不受开关影响 |

---

#### TC-REQ04-008：isHiveShareEnabled - dsType为null

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-008 |
| **测试名称** | isHiveShareEnabled传入null时返回false |
| **来源** | 静态代码分析 - isHiveShareEnabled方法, dsType参数边界 |
| **前置条件** | `linkis.datasource.hive.share.enable=true` |
| **测试步骤** | 1. 调用`isHiveShareEnabled(null)` |
| **预期结果** | 返回false（"hive".equalsIgnoreCase(null)为false） |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 边界场景 - null参数 |

---

#### TC-REQ04-009：isHiveShareEnabled - dsType大小写混合

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-009 |
| **测试名称** | isHiveShareEnabled对dsType忽略大小写 |
| **来源** | 静态代码分析 - isHiveShareEnabled方法, equalsIgnoreCase语义 |
| **前置条件** | `linkis.datasource.hive.share.enable=true` |
| **测试步骤** | 1. 分别调用`isHiveShareEnabled("Hive")`、`isHiveShareEnabled("HIVE")`、`isHiveShareEnabled("hive")` |
| **预期结果** | 三次调用均返回true |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 边界场景 - 大小写兼容 |

---

#### TC-REQ04-010：queryDataSourceInfoByNameAndEnvId权限校验行为与reqToGetDataSourceInfo一致

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-010 |
| **测试名称** | queryDataSourceInfoByNameAndEnvId权限校验与reqToGetDataSourceInfo一致 |
| **来源** | 代码变更分析 - 两个方法使用相同的hasPermission逻辑 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；非创建者非管理员用户 |
| **测试步骤** | 1. 以相同条件调用`queryDataSourceInfoByNameAndEnvId`<br>2. 验证hasPermission判断逻辑与reqToGetDataSourceInfo一致 |
| **预期结果** | Hive数据源放行，非Hive数据源拦截，行为与reqToGetDataSourceInfo完全一致 |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 两个入口方法行为一致性 |

---

### 4.2 Keytab获取逻辑测试（HiveMetaService）

#### TC-REQ04-011：开关开启时使用本地keytab文件建立连接

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-011 |
| **测试名称** | 开关开启+本地keytab存在时使用本地keytab |
| **来源** | 代码变更分析 - HiveMetaService.java, getConnection方法, 路径P6 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；`linkis.datasource.hive.share.keytab.path=/mnt/bdap/keytab/`；principle非空；本地keytab文件`/mnt/bdap/keytab/hadoop.keytab`存在 |
| **测试步骤** | 1. 调用`getConnection(operator, params)`，params中包含principle="hadoop@BDAP.COM"<br>2. 验证不调用BML下载<br>3. 验证日志包含"using local keytab path"<br>4. 验证HiveConnection使用的keytab路径为本地路径 |
| **预期结果** | 不调用BML下载；keytabFilePath="/mnt/bdap/keytab/hadoop.keytab"；HiveConnection成功建立 |
| **测试数据** | principle="hadoop@BDAP.COM", keytabDir="/mnt/bdap/keytab/", 本地文件/mnt/bdap/keytab/hadoop.keytab存在 |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 开关开启核心路径 |

---

#### TC-REQ04-012：开关开启时本地keytab文件不存在抛出明确异常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-012 |
| **测试名称** | 开关开启+本地keytab不存在时抛出MetaRuntimeException |
| **来源** | 代码变更分析 - HiveMetaService.java, getConnection方法, 路径P7 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；principle非空；本地keytab文件不存在 |
| **测试步骤** | 1. 调用`getConnection(operator, params)`，params中包含principle="hadoop@BDAP.COM"<br>2. 验证抛出MetaRuntimeException<br>3. 验证异常消息包含"Local keytab file not found"和具体文件路径 |
| **预期结果** | 抛出MetaRuntimeException，消息为"Local keytab file not found:[/mnt/bdap/keytab/hadoop.keytab], please ensure the keytab file is placed correctly" |
| **测试数据** | principle="hadoop@BDAP.COM", 本地文件/mnt/bdap/keytab/hadoop.keytab不存在 |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 异常场景 - keytab文件缺失 |

---

#### TC-REQ04-013：开关关闭时走BML下载keytab

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-013 |
| **测试名称** | 开关关闭时走BML下载keytab（原逻辑） |
| **来源** | 代码变更分析 - HiveMetaService.java, getConnection方法, 路径P8 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；principle非空；keytabResourceId非空 |
| **测试步骤** | 1. 调用`getConnection(operator, params)`<br>2. 验证调用BML的downloadResource方法<br>3. 验证keytab文件路径为TMP_FILE_STORE_LOCATION下的随机文件名 |
| **预期结果** | 调用downloadResource(keytabResourceId, operator, keytabFilePath)；keytabFilePath为临时目录下随机UUID.keytab |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 开关关闭保持原有行为 |

---

#### TC-REQ04-014：开关关闭且keytabResourceId为空时抛出异常

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-014 |
| **测试名称** | 开关关闭+keytabResourceId为空时抛出MetaRuntimeException |
| **来源** | 静态代码分析 - HiveMetaService.java, getConnection方法原有异常分支 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；principle非空；keytabResourceId为空 |
| **测试步骤** | 1. 调用`getConnection(operator, params)`，params中keytabResourceId为空字符串<br>2. 验证抛出MetaRuntimeException |
| **预期结果** | 抛出MetaRuntimeException，消息为"Cannot find the keytab file in connect parameters" |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 异常场景 - 参数缺失 |

---

#### TC-REQ04-015：principle为空时建立非Kerberos连接

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-015 |
| **测试名称** | principle为空时建立非Kerberos连接（不受开关影响） |
| **来源** | 代码变更分析 - HiveMetaService.java, getConnection方法 |
| **前置条件** | params中不包含principle或principle为空 |
| **测试步骤** | 1. 调用`getConnection(operator, params)`，params中principle为空<br>2. 验证进入非Kerberos分支 |
| **预期结果** | 创建HiveConnection(uris, extraHadoopConf)（无principle和keytab）；不受开关影响 |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 非Kerberos连接不受影响 |

---

### 4.3 Principal解析测试

#### TC-REQ04-016：principal格式"hadoop@REALM"解析为"hadoop"

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-016 |
| **测试名称** | principal格式user@REALM正确提取primary |
| **来源** | 代码变更分析 - HiveMetaService.java, principal解析逻辑, 路径P9 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；本地keytab文件存在 |
| **测试步骤** | 1. 调用`getConnection`，params中principle="hadoop@BDAP.COM"<br>2. 验证principalPrimary解析结果<br>3. 验证keytabFilePath构造结果 |
| **预期结果** | principalPrimary="hadoop"；keytabFilePath="/mnt/bdap/keytab/hadoop.keytab" |
| **测试数据** | principle="hadoop@BDAP.COM" |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 标准principal格式 |

---

#### TC-REQ04-017：principal格式"hadoop/host@REALM"解析为"hadoop"

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-017 |
| **测试名称** | principal格式user/host@REALM正确提取primary |
| **来源** | 代码变更分析 - HiveMetaService.java, principal解析逻辑, 路径P10 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；本地keytab文件存在 |
| **测试步骤** | 1. 调用`getConnection`，params中principle="hadoop/node1.bdap.com@BDAP.COM"<br>2. 验证principalPrimary解析结果<br>3. 验证keytabFilePath构造结果 |
| **预期结果** | principalPrimary="hadoop"；keytabFilePath="/mnt/bdap/keytab/hadoop.keytab" |
| **测试数据** | principle="hadoop/node1.bdap.com@BDAP.COM" |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - host-based principal格式 |

---

#### TC-REQ04-018：principal仅包含用户名"hadoop"解析为"hadoop"

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-018 |
| **测试名称** | principal无REALM时正确提取primary |
| **来源** | 静态代码分析 - principal解析边界，split("/")和split("@")的容错 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；本地keytab文件存在 |
| **测试步骤** | 1. 调用`getConnection`，params中principle="hadoop"（无@REALM）<br>2. 验证principalPrimary解析结果 |
| **预期结果** | principalPrimary="hadoop"；keytabFilePath="/mnt/bdap/keytab/hadoop.keytab" |
| **测试数据** | principle="hadoop" |
| **优先级** | P2 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 边界场景 - 非标准principal格式 |

---

### 4.4 配置测试

#### TC-REQ04-019：配置项默认值验证

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-019 |
| **测试名称** | 验证配置项默认值正确 |
| **来源** | 代码变更分析 - MdmConfiguration.java |
| **前置条件** | 无（不设置任何配置值） |
| **测试步骤** | 1. 验证`MdmConfiguration.HIVE_DATASOURCE_SHARE_ENABLE.getValue()`返回false<br>2. 验证`MdmConfiguration.HIVE_DATASOURCE_SHARE_KEYTAB_PATH.getValue()`返回"/mnt/bdap/keytab/" |
| **预期结果** | HIVE_DATASOURCE_SHARE_ENABLE默认值为false；HIVE_DATASOURCE_SHARE_KEYTAB_PATH默认值为"/mnt/bdap/keytab/" |
| **优先级** | P0 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 正向场景 - 默认配置验证 |

---

#### TC-REQ04-020：配置文件属性项存在性验证

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-020 |
| **测试名称** | 验证linkis-ps-publicservice.properties包含新增配置项 |
| **来源** | 代码变更分析 - linkis-ps-publicservice.properties |
| **前置条件** | 配置文件存在 |
| **测试步骤** | 1. 读取`linkis-ps-publicservice.properties`<br>2. 验证包含`linkis.datasource.hive.share.enable=false`<br>3. 验证包含`linkis.datasource.hive.share.keytab.path=/mnt/bdap/keytab/` |
| **预期结果** | 配置文件包含两个新增配置项，默认值与MdmConfiguration声明一致 |
| **优先级** | P1 |
| **测试类型** | 参数配置 |
| **覆盖场景** | 正向场景 - 配置文件完整性 |

---

#### TC-REQ04-021：自定义keytab路径配置生效

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-021 |
| **测试名称** | 自定义keytab路径配置生效 |
| **来源** | 代码变更分析 - HiveMetaService.java, keytabDir来源 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；`linkis.datasource.hive.share.keytab.path=/data/keytabs/`；对应路径下keytab文件存在 |
| **测试步骤** | 1. 设置自定义keytab路径为`/data/keytabs/`<br>2. 调用`getConnection`<br>3. 验证keytabFilePath使用自定义路径 |
| **预期结果** | keytabFilePath="/data/keytabs/hadoop.keytab"（使用自定义路径而非默认路径） |
| **测试数据** | keytabDir="/data/keytabs/", principle="hadoop@BDAP.COM" |
| **优先级** | P1 |
| **测试类型** | 参数配置 |
| **覆盖场景** | 正向场景 - 自定义配置 |

---

#### TC-REQ04-022：keytab路径不以斜杠结尾时的拼接行为

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-022 |
| **测试名称** | keytab路径不以/结尾时拼接结果 |
| **来源** | 静态代码分析 - HiveMetaService.java, keytabFilePath = keytabDir + principalPrimary + ".keytab" |
| **前置条件** | `linkis.datasource.hive.share.keytab.path=/mnt/bdap/keytab`（无末尾/） |
| **测试步骤** | 1. 设置keytab路径为`/mnt/bdap/keytab`（无末尾斜杠）<br>2. 调用`getConnection`<br>3. 验证keytabFilePath拼接结果 |
| **预期结果** | keytabFilePath="/mnt/bdap/keytabhadoop.keytab"（直接拼接，路径可能不正确。建议配置时以/结尾） |
| **优先级** | P2 |
| **测试类型** | 参数配置 |
| **覆盖场景** | 边界场景 - 路径拼接边界 |

---

### 4.5 集成/端到端测试

#### TC-REQ04-023：开关开启完整流程-非创建者使用Hive数据源执行查询

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-023 |
| **测试名称** | 开关开启完整流程-非创建者使用Hive数据源执行查询 |
| **来源** | 需求描述 - 完整业务流程 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；Hive数据源由hadoop用户创建；本地keytab文件`/mnt/bdap/keytab/hadoop.keytab`存在；用户zhangsan既非管理员也非创建者 |
| **测试步骤** | 1. 以zhangsan身份查询Hive数据源元数据<br>2. 验证权限校验通过<br>3. 验证使用本地keytab建立Hive连接<br>4. 执行Hive SQL查询验证连接可用 |
| **预期结果** | zhangsan成功查询Hive数据源元数据；Hive连接使用本地keytab而非BML下载；SQL查询正常返回结果 |
| **优先级** | P0 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 正向场景 - 完整业务流程 |

---

#### TC-REQ04-024：开关关闭完整流程-非创建者使用Hive数据源被拦截

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-024 |
| **测试名称** | 开关关闭完整流程-非创建者使用Hive数据源被拦截 |
| **来源** | 需求描述 - 兼容性验证 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；Hive数据源由hadoop用户创建；用户zhangsan既非管理员也非创建者 |
| **测试步骤** | 1. 以zhangsan身份查询Hive数据源元数据<br>2. 验证权限校验不通过<br>3. 验证返回权限错误 |
| **预期结果** | 返回权限错误"Don't have query permission for data source [没有数据源的查询权限]" |
| **优先级** | P0 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 负向场景 - 开关关闭保持原有行为 |

---

#### TC-REQ04-025：开关开启时其他类型数据源仍受限

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-025 |
| **测试名称** | 开关开启时非创建者使用Spark数据源仍被拦截 |
| **来源** | 需求描述 - 影响范围限定 |
| **前置条件** | `linkis.datasource.hive.share.enable=true`；Spark数据源由hadoop用户创建；用户zhangsan既非管理员也非创建者 |
| **测试步骤** | 1. 以zhangsan身份查询Spark数据源元数据<br>2. 验证权限校验不通过 |
| **预期结果** | 返回权限错误，Spark数据源不受开关影响 |
| **优先级** | P1 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 边界场景 - 开关仅对Hive生效 |

---

### 4.6 回归测试

#### TC-REQ04-026：创建者用户无论开关状态均能正常使用

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-026 |
| **测试名称** | 创建者用户无论开关状态均能正常使用Hive数据源 |
| **来源** | 回归测试 - 创建者权限 |
| **前置条件** | 当前用户为数据源创建者 |
| **测试步骤** | 1. 开关关闭时以创建者身份查询Hive数据源<br>2. 开关开启时以创建者身份查询Hive数据源 |
| **预期结果** | 两种情况下均正常通过权限校验 |
| **优先级** | P0 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 回归场景 - 创建者权限不变 |

---

#### TC-REQ04-027：非Kerberos Hive数据源不受开关影响

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-027 |
| **测试名称** | 非Kerberos Hive数据源连接不受开关影响 |
| **来源** | 回归测试 - 非Kerberos场景 |
| **前置条件** | Hive数据源不使用Kerberos认证（principle为空） |
| **测试步骤** | 1. 开关关闭时连接非Kerberos Hive数据源<br>2. 开关开启时连接非Kerberos Hive数据源 |
| **预期结果** | 两种情况下均使用非Kerberos方式建立连接，行为一致 |
| **优先级** | P1 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 回归场景 - 非Kerberos不受影响 |

---

#### TC-REQ04-028：BML下载失败时的错误信息与开关关闭时一致

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-028 |
| **测试名称** | 开关关闭+BML下载失败时错误信息不变 |
| **来源** | 回归测试 - 原有异常行为 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；BML下载失败 |
| **测试步骤** | 1. Mock BML下载返回失败<br>2. 调用`getConnection` |
| **预期结果** | 抛出MetaRuntimeException，消息为"Fail to download resource i:[xxx]"（与原逻辑一致） |
| **优先级** | P1 |
| **测试类型** | 单元测试 |
| **覆盖场景** | 回归场景 - 原有异常行为不变 |

---

#### TC-REQ04-029：开关关闭时非创建者使用JDBC数据源仍被拦截

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-REQ04-029 |
| **测试名称** | 开关关闭时非创建者使用JDBC数据源仍被拦截 |
| **来源** | 回归测试 - 其他数据源类型 |
| **前置条件** | `linkis.datasource.hive.share.enable=false`；JDBC数据源；非创建者非管理员用户 |
| **测试步骤** | 1. 以非创建者用户查询JDBC数据源元数据 |
| **预期结果** | 返回权限错误，JDBC数据源行为不变 |
| **优先级** | P2 |
| **测试类型** | 功能测试 |
| **覆盖场景** | 回归场景 - 其他数据源不受影响 |

---

## 五、测试用例统计

### 优先级分布

| 优先级 | 数量 | 用例编号 |
|:------:|:----:|---------|
| P0 | 13 | TC-REQ04-001, 002, 003, 004, 005, 006, 011, 012, 013, 016, 017, 019, 023, 024, 026 |
| P1 | 10 | TC-REQ04-007, 008, 009, 010, 014, 015, 020, 021, 025, 027, 028 |
| P2 | 3 | TC-REQ04-018, 022, 029 |

### 测试类型分布

| 测试类型 | 数量 |
|---------|:----:|
| 单元测试 | 18 |
| 功能测试 | 7 |
| 参数配置 | 3 |

### 场景覆盖分布

| 场景类型 | 数量 |
|---------|:----:|
| 正向场景 | 14 |
| 边界场景 | 5 |
| 负向场景 | 3 |
| 异常场景 | 2 |
| 回归场景 | 4 |

---

## 六、验收标准覆盖检查

| 验收标准 | 对应测试用例 | 覆盖状态 |
|---------|:-----------:|:-------:|
| 开关关闭时，非创建者用户使用Hive数据源应仍然被拦截 | TC-REQ04-001, TC-REQ04-024 | 已覆盖 |
| 开关开启时，非创建者用户使用Hive数据源（带keytab）应成功建立连接 | TC-REQ04-002, TC-REQ04-023 | 已覆盖 |
| 开关开启时，非创建者用户使用非Hive数据源应仍然被拦截 | TC-REQ04-003, TC-REQ04-025 | 已覆盖 |
| 本地keytab文件不存在时，应抛出明确异常 | TC-REQ04-012 | 已覆盖 |
| 管理员用户无论开关状态都应能正常使用所有数据源 | TC-REQ04-004, TC-REQ04-005 | 已覆盖 |
| principal格式兼容：hadoop@REALM和hadoop/host@REALM | TC-REQ04-016, TC-REQ04-017 | 已覆盖 |
| keytab路径配置可自定义 | TC-REQ04-021 | 已覆盖 |

**覆盖率**：7/7 验收标准 (100%)

---

## 七、回归测试关注点

| # | 回归点 | 说明 |
|:-:|--------|------|
| 1 | 开关关闭时权限校验行为与改造前完全一致 | 部署后先验证原有权限校验不受影响 |
| 2 | 开关关闭时BML下载keytab行为不变 | 验证HiveMetaService仍走BML路径 |
| 3 | 非Hive数据源权限校验不受影响 | 验证Spark/JDBC等数据源行为不变 |
| 4 | 管理员和创建者权限不受开关影响 | 验证原有特权用户不受影响 |
| 5 | 非Kerberos连接不受影响 | 验证不使用Kerberos的Hive数据源正常工作 |

---

## 八、环境要求

| 环境 | 配置要求 |
|------|---------|
| DEV测试 | `linkis.datasource.hive.share.enable=true` |
| | `linkis.datasource.hive.share.keytab.path=/mnt/bdap/keytab/` |
| SIT测试 | 同DEV配置，验证多用户场景 |
| 测试工具 | curl / linkis-cli / 单元测试(JUnit 5) |
| 前置条件 | 1. 至少一个Hive数据源（Kerberos认证）<br>2. 本地keytab文件已放置<br>3. 至少两个不同用户（创建者+非创建者） |
