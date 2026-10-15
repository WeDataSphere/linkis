# JMX 测试数据文件目录

## 用途

本目录存放 JMX 测试计划运行时需要从**本地文件系统读取**的测试数据文件。

JMX 文件通过 `${file_path}` 变量引用本目录，变量值在 `../jmx-env-config.properties` 中统一配置。

## 目录结构

```
testdata/
├── README.md          # 本文件
├── validator/         # linkis-validator-tctp.jmx 的测试数据
├── scriptis/          # linkis-scriptis-xc.jmx 的测试数据
├── udf/               # UDF 测试的 JAR/PY/SCALA 资源
└── production/        # linkis-生产问题回归-xc.jmx 的测试数据
```

## 文件清单

### validator/

| 文件 | 用途 | 来源 |
|------|------|------|
| `row_501sql.txt` | 501 行 SQL 测试数据 | `__FileToString()` 读取 |
| `row_sql501.txt` | 501 行 SQL 测试数据（另一组） | `__FileToString()` 读取 |
| `499_1empty.txt` | 499 行+1 空行测试数据 | `__FileToString()` 读取 |
| `499_1remark.txt` | 499 行+1 备注测试数据 | `__FileToString()` 读取 |

### scriptis/

| 文件 | 用途 |
|------|------|
| `upload1.csv` | CSV 上传测试文件 |
| `upload2.xlsx` | XLSX 上传测试文件 |
| `BOM_UTF8.csv` | 带 BOM 的 UTF-8 CSV 文件 |
| `db_to_hdfs.csv` | DB 到 HDFS 导出测试 CSV |
| `xls_test.xls` | XLS 上传测试文件 |
| `initdata5010.txt` | 5010 行初始化数据 |
| `31M.txt` | 31MB 大文件上传测试 |
| `0M.txt` | 0MB 空文件测试 |
| `file_203M.zip` | 203MB 大文件上传测试 |

### udf/

| 文件 | 用途 |
|------|------|
| `wds_functions.jar` | UDF 函数 JAR 包 |
| `scala-jar-test-1.1.10-wedatasphere.jar` | Scala JAR 测试包 |
| `qml_algo.jar` | QML 算法 JAR 包 |
| `testudfhello.py` | Python UDF 脚本 |

### production/

| 文件 | 用途 |
|------|------|
| `0902-scriptis-autotest.txt` | Scriptis 自动化测试数据 |
| `BOM_UTF8.csv` | 带 BOM 的 UTF-8 CSV 文件 |
| `many_sheets.xlsx` | 多 Sheet XLSX 测试文件 |
| `upload3.txt` | TXT 上传测试文件 |

## 引用方式

JMX 文件中使用 `${file_path}` 变量引用本目录：

```
${__FileToString(${file_path}/validator/row_501sql.txt,,)}
${file_path}/scriptis/upload1.csv
${file_path}/udf/wds_functions.jar
```

变量配置在 `../jmx-env-config.properties`：

```properties
file_path=docs/project-knowledge/testing/jmx/testdata
```

## 注意事项

1. **HDFS 路径不变**：HDFS 路径（如 `hdfs:///apps-data/...`）是集群侧资源，通过 Linkis API 访问，不需要移到本地
2. **Linux 服务器路径不变**：`file:///mnt/bdap/...` 是部署服务器上的文件，通过 Linkis API 访问，保持原样
3. **Linkis 配置路径不变**：`${LINKIS_HOME}/conf/...` 是部署环境的配置文件，保持原样
4. **文件占位**：本目录中的文件需要用户手动从原路径（如 `D:\大数据\自动化\...`）复制过来，或使用测试数据生成工具创建
5. **大文件**：`31M.txt`、`file_203M.zip` 等大文件不建议提交到 Git，可在 `.gitignore` 中排除
