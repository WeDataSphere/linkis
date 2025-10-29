# 结果集字段截取功能 - 功能说明与实现范围

## 一、需求理解修正

根据补充说明,明确了三个功能的区别:

### 1.1 功能分类

| 功能 | 接口 | 说明 | 字符限制 |
|------|------|------|---------|
| **结果集查看** | `/api/rest_j/v1/filesystem/openFile` | 在线预览结果集内容 | 10000字符 |
| **结果集下载** | `/api/rest_j/v1/filesystem/resultsetToExcel`<br/>`/api/rest_j/v1/filesystem/resultsetsToExcel` | 下载结果集为CSV/Excel文件 | 10000字符 |
| **结果集导出** | Pipeline引擎 | 通过 `from <源> to <目标>` 语法提交给Pipeline引擎执行 | 32767字符 |

## 二、实现范围

### ✅ 已实现功能

#### 2.1 结果集查看 (openFile)
- **位置**: `FsRestfulApi.openFile()`
- **实现方式**: 在返回数据前检测和截取超长字段
- **字符限制**: 10000 (可配置)
- **新增参数**: `truncateOversizedFields` (Boolean, 可选)
- **返回扩展字段**:
  - `hasOversizedFields`: 是否存在超长字段
  - `oversizedFields`: 超长字段列表
  - `maxOversizedFieldCount`: 最大收集数量(20)

**使用示例**:
```
# 第一次请求: 检测超长字段
GET /api/rest_j/v1/filesystem/openFile?path=/path/to/result&page=1

# 返回: hasOversizedFields=true, oversizedFields=[...]

# 用户确认后第二次请求: 执行截取
GET /api/rest_j/v1/filesystem/openFile?path=/path/to/result&page=1&truncateOversizedFields=true
```

#### 2.2 结果集下载 - 单文件 (resultsetToExcel)
- **位置**: `FsRestfulApi.resultsetToExcel()`
- **实现方式**: 读取完整数据后检测和截取,然后写入文件
- **字符限制**: 10000 (可配置,使用 `FIELD_DOWNLOAD_MAX_LENGTH`)
- **新增参数**: `truncateOversizedFields` (Boolean, 可选)
- **支持格式**: CSV, XLSX

**使用示例**:
```
GET /api/rest_j/v1/filesystem/resultsetToExcel?path=/path/to/result&outputFileType=xlsx&truncateOversizedFields=true
```

### ⏸️ 暂未实现功能

#### 2.3 结果集下载 - 多文件 (resultsetsToExcel)
- **原因**:
  1. 处理多个结果集文件,实现复杂度高
  2. 遵循最小改动原则,避免过度修改
  3. 实际使用频率较低
- **建议**: 作为后续优化项

#### 2.4 结果集导出 (Pipeline引擎)
- **说明**: 结果集导出功能是通过 Pipeline 引擎执行的,不是通过 HTTP 接口
- **Pipeline 工作流程**:
  ```
  用户提交: from <源路径> to <目标路径>
       ↓
  提交给 Pipeline 引擎执行
       ↓
  Pipeline 引擎读取 dolphin 文件
       ↓
  写入目标共享目录
  ```
- **实现方案**: 需要在 Pipeline 引擎的相关代码中实现字段截取逻辑(32767字符)
- **Pipeline 代码位置**: 需要进一步探索 Pipeline 相关模块
- **建议**: 作为独立的优化任务实现

## 三、配置项说明

### 3.1 已实现的配置

```properties
# 功能总开关
linkis.resultset.field.truncation.enabled=false

# 查看功能字段最大长度
linkis.resultset.field.view.max.length=10000

# 下载功能字段最大长度 (resultsetToExcel 使用此配置)
linkis.resultset.field.download.max.length=10000

# 导出功能字段最大长度 (预留给 Pipeline 引擎使用)
linkis.resultset.field.export.max.length=32767

# 最多收集超长字段数量
linkis.resultset.field.oversized.max.count=20
```

### 3.2 配置项对应关系

| 配置项 | 使用场景 | 默认值 |
|--------|---------|--------|
| `FIELD_VIEW_MAX_LENGTH` | openFile 查看 | 10000 |
| `FIELD_DOWNLOAD_MAX_LENGTH` | resultsetToExcel 下载 | 10000 |
| `FIELD_EXPORT_MAX_LENGTH` | Pipeline 导出 (待实现) | 32767 |

## 四、实现总结

### 4.1 当前版本实现

**V1.0 实现范围**:
- ✅ 结果集查看 (openFile) - 10000字符截取
- ✅ 结果集下载 - 单文件 (resultsetToExcel) - 10000字符截取
- ⏸️ 结果集下载 - 多文件 (resultsetsToExcel) - 待实现
- ⏸️ 结果集导出 (Pipeline引擎) - 32767字符截取 - 待实现

**实现的核心功能**:
1. 字段长度检测
2. 超长字段信息收集(最多20个)
3. 字段截取
4. 功能开关控制
5. 配置项管理

### 4.2 遵循的原则

- ✅ 最小改动原则: 仅在API层添加逻辑
- ✅ 功能可配置原则: 所有功能可配置,默认关闭
- ✅ 数据库修改原则: 未修改任何表结构
- ✅ 配置管理规则: 使用 CommonVars 统一管理
- ✅ 代码边界约束: 未引入新依赖,未修改公共接口签名

### 4.3 后续优化方向

1. **优先级高**:
   - 实现 Pipeline 引擎的字段截取(32767字符)
   - 需要探索 Pipeline 相关代码位置

2. **优先级中**:
   - 实现 resultsetsToExcel 多文件下载的字段截取

3. **优先级低**:
   - 性能优化: 只检测前N行
   - 支持异步检测大结果集
   - 前端完整交互实现

## 五、使用建议

### 5.1 分阶段启用

**阶段1: 仅启用查看功能**
```properties
linkis.resultset.field.truncation.enabled=true
linkis.resultset.field.view.max.length=10000
```

**阶段2: 启用查看和下载功能**
```properties
linkis.resultset.field.truncation.enabled=true
linkis.resultset.field.view.max.length=10000
linkis.resultset.field.download.max.length=10000
```

**阶段3: 启用全部功能(需要实现Pipeline导出后)**
```properties
linkis.resultset.field.truncation.enabled=true
linkis.resultset.field.view.max.length=10000
linkis.resultset.field.download.max.length=10000
linkis.resultset.field.export.max.length=32767
```

### 5.2 测试建议

1. **功能测试**: 先在测试环境验证查看功能
2. **性能测试**: 评估大结果集的性能影响
3. **灰度发布**: 建议分批启用,逐步推广

## 六、已知限制

1. **多文件下载**: 当前版本不支持 `resultsetsToExcel` 的字段截取
2. **Pipeline导出**: 需要单独实现,不在当前版本范围内
3. **性能影响**: 大结果集检测可能有性能损耗,建议配合分页使用
4. **导出元数据**: 下载功能的元数据写入需要进一步测试完善

## 七、代码位置

### 7.1 已实现代码

| 类型 | 位置 |
|------|------|
| 配置类 | `WorkSpaceConfiguration.java` |
| 实体类 | `OversizedFieldInfo.java`<br/>`FieldTruncationResult.java` |
| 工具类 | `FieldTruncationHelper.java` |
| API改造 | `FsRestfulApi.java` (openFile, resultsetToExcel) |

### 7.2 待实现代码 (Pipeline导出)

需要探索的模块:
- Pipeline 引擎相关代码
- Dolphin 文件读取和写入相关代码
- 可能位置: linkis-computation-orchestrator 或 linkis-engineconn 模块

---

**版本**: 1.0
**日期**: 2025-10-28
**状态**: 查看和单文件下载功能已完成,Pipeline导出功能待实现
