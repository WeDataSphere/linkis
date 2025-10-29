# 结果集字段截取功能设计文档

## 1. 设计概述

### 1.1 设计目标
在不破坏现有功能的前提下,为结果集查看、下载、导出接口增加超长字段检测和截取能力。

### 1.2 设计原则
- **最小改动原则**: 仅在必要位置增加检测和截取逻辑
- **功能可配置原则**: 所有功能通过开关控制,默认关闭
- **向下兼容原则**: 不修改现有接口签名,仅扩展返回数据结构

## 2. 架构设计

### 2.1 现有架构
```
REST API Layer (FsRestfulApi)
    ↓
Service Layer (FileSource)
    ↓
Storage Layer (ResultSetReader)
```

### 2.2 改造架构
```
FsRestfulApi
  ↓
FieldTruncationHelper (新增工具类)
  ↓
原有逻辑
```

## 3. 详细设计

### 3.1 配置类设计
**位置**: `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/conf/FileSystemConfiguration.java`

新增配置项:
- `FIELD_TRUNCATION_ENABLED`: 功能开关,默认false
- `FIELD_VIEW_MAX_LENGTH`: 查看最大长度,默认10000
- `FIELD_DOWNLOAD_MAX_LENGTH`: 下载最大长度,默认10000
- `FIELD_EXPORT_MAX_LENGTH`: 导出最大长度,默认32767
- `OVERSIZED_FIELD_MAX_COUNT`: 最多收集数量,默认20

### 3.2 实体类设计

#### OversizedFieldInfo
```java
public class OversizedFieldInfo {
    private String fieldName;      // 字段名
    private Integer rowIndex;      // 行号
    private Integer actualLength;  // 实际长度
    private Integer maxLength;     // 最大长度
}
```

#### FieldTruncationResult
```java
public class FieldTruncationResult {
    private boolean hasOversizedFields;
    private List<OversizedFieldInfo> oversizedFields;
    private Integer maxOversizedFieldCount;
    private Object data;
}
```

### 3.3 工具类设计
**位置**: `linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/filesystem/util/FieldTruncationHelper.java`

核心方法:
- `detectAndHandle()`: 检测并处理超长字段
- `detectOversizedFields()`: 检测超长字段
- `truncateFields()`: 截取超长字段

### 3.4 API改造

#### openFile() 改造
1. 添加参数 `truncateOversizedFields`
2. 调用检测逻辑
3. 扩展返回结果

#### resultsetToExcel() 改造
类似 openFile(),在导出前检测和截取

#### download() 改造
可选实现,评估复杂度后决定

## 4. 前后端交互流程

```
前端 -> GET /openFile (truncate=false)
后端 -> 检测超长字段,返回字段列表
前端 -> 展示提示,用户确认
前端 -> GET /openFile (truncate=true)
后端 -> 执行截取,返回数据
```

## 5. 实施步骤

1. 创建配置类,定义配置项
2. 创建实体类
3. 创建工具类,实现核心逻辑
4. 改造 openFile() 方法
5. 改造 resultsetToExcel() 方法
6. 编写单元测试

## 6. 风险与应对

### 6.1 性能风险
- 添加功能开关,默认关闭
- 最多收集20个字段
- 高效的字符串长度检测

### 6.2 兼容性风险
- 不修改现有接口签名
- 新增字段可选,不影响老版本

## 7. 测试计划

- 单元测试: 工具类各方法
- 集成测试: API功能测试
- 性能测试: 大结果集测试
