# JMX 回归测试用例

## 概述

本目录存放 JMX (JMeter) 回归测试用例，用于 dev-flow 工作流的性能回归测试。JMX 文件在此目录下按模块组织，与 `regression/` 和 `features/` 平级，版本无关（全分支共享）。

## 目录结构

```
docs/project-knowledge/testing/jmx/
├── README.md                                    # 本文件
├── jmx-global.properties                        # 全局阈值配置（推荐，配这一个就够）
├── generated/                                   # test-code-generator 自动生成的 jmx（见下文「自动生成 jmx」）
├── {moduleId}/                                  # 按模块分子目录（人工维护）
│   ├── {moduleId}.properties                    # 模块级阈值配置（可选，覆盖全局）
│   ├── {moduleId}_{场景描述}.jmx                # JMeter 测试计划文件
│   └── {moduleId}_{场景描述}.properties         # 单文件阈值配置（可选，覆盖模块级）
```

## 命名规范

| 文件类型 | 命名规则 | 示例 | 作用域 |
|---------|---------|------|--------|
| 全局配置 | `jmx-global.properties` | `jmx-global.properties` | 所有 JMX |
| 模块级配置 | `{moduleId}.properties` | `linkis-entrance.properties` | 模块内所有 JMX |
| JMX 文件 | `{moduleId}_{场景描述}.jmx` | `linkis-entrance_task-submit.jmx` | 单个 |
| 单文件配置 | `{moduleId}_{场景描述}.properties` | `linkis-entrance_task-submit.properties` | 单个 JMX |

**约束**：
- `moduleId` 与 `module-index.json` 中的模块 ID 对应
- 场景描述使用英文 kebab-case
- 单文件配置与 JMX 文件必须同名（仅扩展名不同）

## 分层阈值配置（三层，配一个全局即可跑全部）

采用**三层配置机制**，避免为每个 JMX 重复配置：

| 层级 | 文件 | 作用域 | 必需性 | 典型场景 |
|------|------|--------|--------|---------|
| **全局** | `jmx-global.properties` | 所有 JMX | 推荐 | 统一阈值标准 |
| **模块级** | `{moduleId}/{moduleId}.properties` | 模块内所有 JMX | 可选 | 某模块阈值不同 |
| **单文件** | `{moduleId}/{name}.properties` | 单个 JMX | 可选 | 特定 JMX 需特殊阈值 |

**优先级**：单文件 > 模块级 > 全局 > 脚本默认值

**加载逻辑**：脚本按优先级从低到高依次加载，后者覆盖前者。无需为每个 JMX 都配 `.properties`。

### 最简配置（推荐）

只需配一个全局文件 `jmx/jmx-global.properties`：

```properties
# ============================================
# JMX 全局阈值配置
# 作用于所有 JMX 文件，配这一个就够跑全部测试
# ============================================

# --- 功能开关（改为 true 启用全部 JMX）---
linkis.testing.jmx.enable=true

# --- 响应时间阈值（单位：毫秒）---
linkis.testing.jmx.threshold.responseTime.p95=2000
linkis.testing.jmx.threshold.responseTime.p99=5000
linkis.testing.jmx.threshold.responseTime.avg=1000

# --- 错误率阈值（百分比，0-100）---
linkis.testing.jmx.threshold.errorRate=1.0

# --- 吞吐量阈值（TPS，每秒请求数）---
linkis.testing.jmx.threshold.throughput=50.0

# --- 断言配置 ---
linkis.testing.jmx.useJmxAssertion=true
```

### 覆盖示例

假设全局配置 P95=2000ms，但 `linkis-entrance_task-query` 接口要求更严（P95=1500ms）：

```
docs/project-knowledge/testing/jmx/
├── jmx-global.properties              # P95=2000（全局）
└── linkis-entrance/
    ├── linkis-entrance_task-submit.jmx
    ├── linkis-entrance_task-query.jmx
    └── linkis-entrance_task-query.properties   # 仅覆盖 P95=1500（其余继承全局）
```

`linkis-entrance_task-query.properties`（只写需要覆盖的字段）：
```properties
linkis.testing.jmx.threshold.responseTime.p95=1500
linkis.testing.jmx.threshold.responseTime.p99=3000
```

## 阈值配置字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `linkis.testing.jmx.enable` | boolean | false | 功能开关，false 时跳过执行 |
| `linkis.testing.jmx.threshold.responseTime.p95` | int | 2000 | P95 响应时间阈值(ms) |
| `linkis.testing.jmx.threshold.responseTime.p99` | int | 5000 | P99 响应时间阈值(ms) |
| `linkis.testing.jmx.threshold.responseTime.avg` | int | 1000 | 平均响应时间阈值(ms) |
| `linkis.testing.jmx.threshold.errorRate` | float | 1.0 | 错误率上限(%) |
| `linkis.testing.jmx.threshold.throughput` | float | 50.0 | 最小吞吐量(TPS) |
| `linkis.testing.jmx.useJmxAssertion` | boolean | true | 是否结合 JMX 内置断言 |
| `linkis.testing.jmx.users` | int | - | 并发用户数（覆盖 JMX 设置） |
| `linkis.testing.jmx.loops` | int | - | 循环次数 |
| `linkis.testing.jmx.duration` | int | - | 持续时间(秒) |
| `linkis.testing.jmx.tags` | string | @regression,@performance | 标签列表 |

## 自动生成 jmx（generated/ 子目录）

`generated/` 子目录存放由 **test-code-generator 第4.3步**自动生成的 jmx，与人工维护的 `{moduleId}/` 子目录（如 `回归/`）物理隔离。

**生成规则**：
- 来源：测试用例文档（`_测试用例.md` / `_后端测试案例.md`）里的 HTTP 接口用例（三策略提取：结构化接口信息 → 行内 `POST /api/...` → curl）
- 命名：`{需求名}.jmx`（同需求重跑覆盖，幂等）
- 环境变量：通过 `${__P(var,默认值)}` 引用上层 `jmx-env-config.properties`（testenv / token_code / token_user 等）
- 阈值：继承全局 `jmx-global.properties`；如需单独阈值可在 `generated/` 下放 `generated.properties`

**执行**：test-executor 步骤3.5 的 `find` 递归扫描自动覆盖 `generated/`，无需额外配置；全局 `linkis.testing.jmx.enable=true` 开启后即随阶段7一起跑。

**无接口跳过**：若测试用例文档未识别到任何 HTTP 接口，test-code-generator 不产出空 jmx（脚本返回 SKIP，退出码 0）。

## 执行方式

### 1. dev-flow 阶段7自动执行

在 dev-flow 工作流的阶段7（测试执行）中，test-executor 会自动扫描本目录并执行所有 `enable=true` 的 JMX 文件。

### 2. 独立命令手动触发

```bash
# 执行全部 JMX 文件（遵守 enable 开关）
/jmx-test --all

# 执行指定目录
/jmx-test --dir docs/project-knowledge/testing/jmx/linkis-entrance

# 执行单个文件（强制执行，忽略 enable 开关）
/jmx-test --file docs/project-knowledge/testing/jmx/linkis-entrance/test.jmx

# 查看最近执行结果
/jmx-test status
```

## 结果判定规则

JMX 执行结果采用**双重判定**机制：

1. **JMX 内置断言**（当 `useJmxAssertion=true`）：JMX 文件中定义的 Response Assertion、Duration Assertion 等断言结果
2. **外部阈值配置**：`.properties` 文件中定义的 P95/P99/avg 响应时间、错误率、吞吐量阈值

任一判定失败，则该 JMX 测试用例整体判定为 FAIL。

## 执行结果输出

执行结果输出到 `test-results/jmx/{timestamp}/` 目录：

| 文件 | 说明 |
|------|------|
| `{name}.jtl` | JMeter 原始结果文件（CSV 或 XML 格式） |
| `{name}.log` | JMeter 执行日志 |
| `individual-results.jsonl` | 每个 JMX 的执行结果（JSON Lines 格式） |
| `jmx-summary.json` | 所有 JMX 的汇总结果 |
| `latest/` | 指向最新执行结果的软链接 |

## 回归沉淀

JMX 测试用例可沉淀到 module-testing-manager 的 performance 模块回归集：

- **Feature 文件**：生成 `{moduleId}_jmx.feature`，带 `@regression @performance @jmx` 标签
- **Markdown 文档**：在 `性能模块_回归.md` 中追加 JMX 性能测试用例表格

沉淀规则配置在 `.claude/config/testing/regression/沉淀规则.json.template` 中，`JMX性能测试` 类型默认自动沉淀。

## 示例

### 最简用法：一个全局配置跑所有 JMX

```
docs/project-knowledge/testing/jmx/
├── jmx-global.properties              # 唯一的配置文件
├── linkis-entrance/
│   ├── linkis-entrance_task-submit.jmx
│   └── linkis-entrance_task-query.jmx
└── linkis-datasource/
    └── linkis-datasource_query-metadata.jmx
```

`jmx-global.properties`：
```properties
linkis.testing.jmx.enable=true
linkis.testing.jmx.threshold.responseTime.p95=2000
linkis.testing.jmx.threshold.responseTime.p99=5000
linkis.testing.jmx.threshold.errorRate=1.0
linkis.testing.jmx.threshold.throughput=50.0
linkis.testing.jmx.useJmxAssertion=true
```

执行 `/jmx-test --all` 即可跑通全部 JMX。

### 进阶用法：模块级覆盖

当某模块阈值与全局不同时，添加模块级配置：

```
docs/project-knowledge/testing/jmx/
├── jmx-global.properties              # P95=2000（全局）
└── linkis-datasource/
    ├── linkis-datasource.properties   # 模块级：P95=1000（更严）
    └── linkis-datasource_query-metadata.jmx
```

`linkis-datasource.properties`（只写差异部分）：
```properties
linkis.testing.jmx.threshold.responseTime.p95=1000
linkis.testing.jmx.threshold.responseTime.p99=2000
```
