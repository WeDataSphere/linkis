
## 交互记录 (2026-05-12T02:40:01.747Z)

**用户**：
[{'type': 'text', 'text': '<ide_opened_file>The user opened the file g:\\kkhuang\\work\\linkis\\.asf.yaml in the IDE. This may or may not be related to the current task.</ide_opened_file>'}, {'type': 'text', 'text': '对比下1.18.2-webank分支，看下有哪些改动，1.18.2-webank是稳定版本，当前分支是合并了开源的代码，我感觉会有很多隐藏问题，分析下改动已经对稳定版本的影响'}]

**AI**：
[思考] 用户想要对比 `1.18.2-webank` 分支和当前的 `dev-2.0.0-bug-fix` 分支，分析合并开源代码后可能带来的改动和潜在问题。

我需要：
1. 首先确认两个分支的存在
2. 获取两个分支之间的差异
3. 分析改动的内容和影响

让我先检查这两个分支的差异。...


## 交互记录 (2026-05-12T02:40:18.088Z)

**用户**：
[{'tool_use_id': 'call_d682ed7ca5da4b208f135335', 'type': 'tool_result', 'content': "fatal: couldn't find remote ref 1.18.2-webank\n分支可能不在远程", 'is_error': False}]

**AI**：
我来帮你对比 `1.18.2-webank` 稳定版本和当前 `dev-2.0.0-bug-fix` 分支的改动。...


## 交互记录 (2026-05-12T02:40:20.815Z)

**用户**：
[{'tool_use_id': 'call_e6344d79fead4bfc82e517d7', 'type': 'tool_result', 'content': '  dev-2.0.0\n* dev-2.0.0-bug-fix\n  dev-2.0.0-code-format\n  dev-2.0.0-common-lang\n  dev-2.0.0-disable-code-log\n  dev-2.0.0-docs-update\n  dev-2.0.0-engine-log\n  dev-2.0.0-entrance-fix\n  dev-2.0.0-flink-version\n  dev-2.0.0-hadoop3\n  dev-2.0.0-hadoop3-sup\n  dev-2.0.0-hive-location\n  dev-2.0.0-hive-tag\n  dev-2.0.0-ketab-cache\n  dev-2.0.0-keytab-conf\n  dev-2.0.0-label-service\n  dev-2.0.0-log-mask\n  dev-2.0.0-log-update\n  dev-2.0.0-mask-token\n  dev-2.0.0-monitor-update\n  dev-2.0.0-resource-reset\n  dev-2.0.0-secondary-yarnqueue\n  dev-2.0.0-version-update\n  dev-2.0.0-week-variable\n  remotes/origin/dev-2.0.0\n  remotes/origin/dev-2.0.0-bug-fix\n  remotes/origin/dev-2.0.0-code-format\n  remotes/origin/dev-2.0.0-common-lang\n  remotes/origin/dev-2.0.0-disable-code-log\n  remotes/origin/dev-2.0.0-docs-update\n  remotes/origin/dev-2.0.0-engine-log\n  remotes/origin/dev-2.0.0-entrance-fix\n  remotes/origin/dev-2.0.0-flink-version\n  remotes/origin/dev-2.0.0-hadoop3\n  remotes/origin/dev-2.0.0-hadoop3-sup\n  remotes/origin/dev-2.0.0-hive-location\n  remotes/origin/dev-2.0.0-hive-tag\n  remotes/origin/dev-2.0.0-ketab-cache\n  remotes/origin/dev-2.0.0-keytab-conf\n  remotes/origin/dev-2.0.0-label-service\n  remotes/origin/dev-2.0.0-log-mask\n  remotes/origin/dev-2.0.0-log-update\n  remotes/origin/dev-2.0.0-mask-token\n  remotes/origin/dev-2.0.0-monitor-update\n  remotes/origin/dev-2.0.0-resource-reset\n  remotes/origin/dev-2.0.0-secondary-yarnqueue\n  remotes/origin/dev-2.0.0-version-update\n  remotes/origin/dev-2.0.0-week-variable', 'is_error': False}]

**AI**：
[思考] 看来 1.18.2-webank 分支不在远程仓库中，可能是本地分支。让我检查本地是否有这个分支。...


## 交互记录 (2026-05-12T02:40:24.456Z)

**用户**：
[{'tool_use_id': 'call_5697030b69614e30b86d4a9e', 'type': 'tool_result', 'content': '  dev-1.18.0-webank\n  dev-1.18.3-webank', 'is_error': False}]

**AI**：
[思考] 用户想对比的是 1.18.2-webank 分支，但我看到本地有 dev-1.18.0-webank 和 dev-1.18.3-webank 分支。让我确认一下是否有 1.18.2-webank 分支，同时检查当前分支相对于稳定分支的差异。...


## 交互记录 (2026-05-12T02:40:26.409Z)

**用户**：
[{'tool_use_id': 'call_baa345fd46f34cbaa9b7da27', 'type': 'tool_result', 'content': '* 6480e140f (HEAD -> dev-2.0.0-bug-fix, origin/dev-2.0.0-bug-fix) #AI commit# 开发阶段： 去除日志打印重复代码\n*   5e2cc8384 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n|\\  \n* | b43a8ff7f #AI commit# 开发阶段： 修复开源代码合并异常\n* |   6b10a7238 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n|\\ \\  \n* | | da0cb3eaa #AI commit# 开发阶段： 修复spark/hive引擎执行任务error异常\n* | |   e622406a2 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n|\\ \\ \\  \n* | | | 5c023b0f4 #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | | | a35da050b #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | | | 048bffde2 #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | | |   833f5c4d5 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n|\\ \\ \\ \\  \n* | | | | d34adc439 #AI commit# 开发阶段： 支持hadoop3引擎物料刷新\n* | | | | 105675f96 #AI commit# 开发阶段： 日志打印优化，spark init sql 异常优化\n* | | | | 994647ba1 #AI commit# 开发阶段： 优化2.0.0特性文档\n* | | | |   83ed972dd Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n|\\ \\ \\ \\ \\  \n* | | | | | 69fdf8449 #AI commit# 开发阶段： 修复依赖注入失败\n* | | | | | 45004df2a #AI commit# 开发阶段： 异常注释修复\n| | | | | | * fe3749604 (origin/dev-2.0.0-hadoop3, dev-2.0.0-hadoop3) #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | | * dd5a5bfa1 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | | * 50224d64b #AI commit# 开发阶段： 回退优化hadoop3依赖\n| | | | | | * 64d6c87b8 Revert "#AI commit# 开发阶段： 优化hadoop3依赖"\n| | | | | | * a124c236c Revert "#AI commit# 开发阶段： 优化hadoop3依赖"\n| | | | | | * df5c71ad7 Revert "#AI commit# 开发阶段： 优化hadoop3依赖"\n| | | | | | * 071d872e7 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | | * 24af83f09 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | | * 3259f4a43 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | | *   c025b9a6d Merge branch \'dev-2.0.0\' into dev-2.0.0-hadoop3\n| | | | | | |\\  \n| | | | | | |/  \n| | | | | |/|   \n| | | | | * | 04a953592 (origin/dev-2.0.0, dev-2.0.0) Dev 2.0.0 bug fix (#1026)\n| | | | |/ /  \n| | | | | * 6302a2609 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | * 9d0300037 #AI commit# 开发阶段： 优化hadoop3依赖\n| | | | | * 6f9db1caa #AI commit# 开发阶段： 修复hadoop3版本物料包丢失异常\n| | | | | * cb46eef1f #AI commit# 开发阶段： 修复hadoop3版本物料包丢失异常\n| | | | | * ad7774bd1 #AI commit# 开发阶段： 修复hadoop3版本物料包丢失异常\n| | | | | * e9693953b #AI commit# 开发阶段： 修复hadoop3版本物料包丢失异常\n| | | | | *   837a0ff3c Merge branch \'dev-2.0.0\' into dev-2.0.0-hadoop3\n| | | | | |\\  \n| | | | | |/  \n| | | | |/|   \n| | | | * | 39f0e377d [fix][EC][spark] fix spark and hive engine execution task error exception handling (#1023)\n| | | | * | 936168a4e [fix][EC][JDBC] Add configurable validation query for different database types (#984)\n| | | |/ /  \n| | | * | 9c20c54b7 #AI commit# 开发阶段： 支持keytab环境，启动命令行增安全参数 (#1020)\n| | | * | 67f0bef39 #AI commit# 开发阶段： 优化引擎日志输出用户执行code (#1021)\n| | | * | fa31f0d86 #AI commit# 开发阶段： 安全工单升级commons-lang移除，commons-lang3升级3.18.0 (#1019)\n| | | | * 26cfe995f #AI commit# 开发阶段： 支持keytab环境，启动命令行增安全参数 (#1020)\n| | | | * 5e7f4eb73 #AI commit# 开发阶段： 优化引擎日志输出用户执行code (#1021)\n| | | | * d9daf4d75 #AI commit# 开发阶段： 安全工单升级commons-lang移除，commons-lang3升级3.18.0 (#1019)\n| | | | * 39e8ccf4c #AI commit# 开发阶段： 升级hadoop版本\n| | | |/  \n| | | | * 76a9845b9 (origin/dev-2.0.0-log-mask, dev-2.0.0-log-mask) #AI commit# 开发阶段： 优化引擎日志输出用户执行code\n| | | |/  \n| | | | * a79e0827a (origin/dev-2.0.0-keytab-conf, dev-2.0.0-keytab-conf) #AI commit# 开发阶段： 支持keytab环境，启动命令行增安全参数\n| | | |/  \n| | | | * 81c8b7c07 (origin/dev-2.0.0-common-lang, dev-2.0.0-common-lang) #AI commit# 开发阶段： 安全工单升级commons-lang移除，commons-lang3升级3.18.0\n| | | |/  \n| | | * 88350524f [fix][ETS][et-monitor] fix log printing exception and optimize monitor cleanup process (#1015)\n| | | * cc92eeb16 #AI commit# 开发阶段： 增加label service 修改entrance 服务标签触发广播 (#1017)\n| | | * 53b4003bf #AI commit# 开发阶段： 同步flink版本 (#1013)\n| | | * a2ea93bdd #AI COMMIT# [fix][CGS] Add MDC jobId tracing for task-related logs (#1011)\n| | |/  \n| | | * d9caf359d (origin/dev-2.0.0-label-service, dev-2.0.0-label-service) #AI commit# 开发阶段： 增加label service 修改entrance 服务标签触发广播\n| | |/  \n| | | * 7cbc7f1c7 (origin/dev-2.0.0-entrance-fix, dev-2.0.0-entrance-fix) #AI commit# 开发阶段： 增加label service 修改entrance 服务标签触发广播\n| | | *   dde5f943b Merge branch \'dev-2.0.0\' into dev-2.0.0-entrance-fix\n| | | |\\  \n| | | |/  \n| | |/|   \n| | | * c432bd393 #AI commit# 开发阶段： * 修复异常场景，当Entrance实例offline后，ParallelGroup缓存未更新，导致用户任务并发数计算错误\n| | | * 0074036e4 #AI commit# 开发阶段： *  增加异常保护\n| | | * dd97b4e13 #AI commit# 开发阶段： *  修复异常场景，当Entrance实例offline后，ParallelGroup缓存未更新，导致用户任务并发数计算错误\n| | | * df0ddb62e #AI commit# 开发阶段： *  修复异常场景，当Entrance实例offline后，ParallelGroup缓存未更新，导致用户任务并发数计算错误\n| | | | * 5c4523f31 (origin/dev-2.0.0-flink-version, dev-2.0.0-flink-version) #AI commit# 开发阶段： 同步flink版本\n| | | |/  \n| | |/|   \n| | * | 1fc405ca2 #AI commit# 开发阶段： add permission check for secondary queue selection (#1010)\n| | * | a0ac6f5e8 #AI commit# 开发阶段： add week variable support for date calculation (#1008)\n| | * | 2e0772cd4 #AI commit# 开发阶段： spark scala version update (#1006)\n| | * | 49fbb659d [style][CGS][core] improve code formatting for unit tests and configuration files (#1004)\n| | * | 8086e269d [fix][EC][all-engines] mask user code in engine execution logs to prevent security leakage (#1003)\n| | * | a0c9a0fc9 #AI commit# 开发阶段： 加密日志打印token (#1002)\n| | | | * 1bd9e904b (origin/dev-2.0.0-secondary-yarnqueue, dev-2.0.0-secondary-yarnqueue) #AI commit# 开发阶段： add permission check for secondary queue selection\n| | | |/  \n| | |/|   \n| | | | * 31e2baa69 (origin/dev-2.0.0-week-variable, dev-2.0.0-week-variable) #AI commit# 开发阶段： add week variable support for date calculation\n| | | |/  \n| | |/|   \n| | | | * 5a14a3624 (origin/dev-2.0.0-version-update, dev-2.0.0-version-update) #AI commit# 开发阶段： spark scala version update\n| | | |/  \n| | |/|   \n| | | | * 127c84346 (origin/dev-2.0.0-code-format, dev-2.0.0-code-format) #AI commit# 开发阶段： 单元测试&配置文件优化\n| | | | *   f34dbf8fe Merge branch \'dev-2.0.0\' into dev-2.0.0-code-format\n| | | | |\\', 'is_error': False}]

**AI**：
我发现本地有 `dev-1.18.0-webank` 和 `dev-1.18.3-webank` 分支。让我检查是否有 `dev-1.18.2-webank` 分支，并对比当前分支与最近的稳定版本。...


## 交互记录 (2026-05-12T02:40:31.010Z)

**用户**：
[{'tool_use_id': 'call_801db74b1e234fada41c8bc4', 'type': 'tool_result', 'content': '  dev-1.18.0-webank\n  dev-1.18.3-webank\n  remotes/origin/dev-1.1.18-support-fb\n  remotes/origin/dev-1.1.18-support-fb-b3\n  remotes/origin/dev-1.1.18-support-fb-uat\n  remotes/origin/dev-1.1.18-vh-bank-new\n  remotes/origin/dev-1.1.18-webank\n  remotes/origin/dev-1.1.18-webank-acrosscluster\n  remotes/origin/dev-1.1.18-webank-hbase\n  remotes/origin/dev-1.18.0-bug-fix\n  remotes/origin/dev-1.18.0-disallow-hadoop\n  remotes/origin/dev-1.18.0-fe-fix\n  remotes/origin/dev-1.18.0-hadoop3-sup\n  remotes/origin/dev-1.18.0-hadoop3-sup-uat\n  remotes/origin/dev-1.18.0-safe-order\n  remotes/origin/dev-1.18.0-secondary-queue\n  remotes/origin/dev-1.18.0-var-week\n  remotes/origin/dev-1.18.0-webank\n  remotes/origin/dev-1.18.0-webank-bak\n  remotes/origin/dev-1.18.0-webank-bak-to-recover\n  remotes/origin/dev-1.18.1-jersey-update\n  remotes/origin/dev-1.18.3-hadoop-remix\n  remotes/origin/dev-1.18.3-sync\n  remotes/origin/dev-1.18.3-webank\n  remotes/origin/dev-1.4.0-webank-streamis-flink-1.18', 'is_error': False}]

**AI**：
[思考] 用户说的 1.18.2-webank 分支似乎不存在，但有 dev-1.18.0-webank 和 dev-1.18.3-webank。让我用 dev-1.18.3-webank（看起来是最新的 webank 稳定分支）来对比。

我需要分析两个方面的差异：
1. 当前 dev-2.0.0-bug-fix 分支相对于 dev-1.18.3-webank 分支的改动
2. 当前分支相对于 dev-2.0.0 主分支的改动

让我分析这些差异。...


## 交互记录 (2026-05-12T02:40:39.610Z)

**用户**：
[{'tool_use_id': 'call_5ec63cfafe3f461d922f392a', 'type': 'tool_result', 'content': '739', 'is_error': False}]

**AI**：
没有找到 `1.18.2-webank` 分支，但有 `dev-1.18.0-webank` 和 `dev-1.18.3-webank`。我将使用 `dev-1.18.3-webank`（看起来是最新的 webank 稳定分支）来对比。...


## 交互记录 (2026-05-12T02:40:52.215Z)

**用户**：
[{'tool_use_id': 'call_76d2cd8244b7479b86c9a7e5', 'type': 'tool_result', 'content': ' linkis-web-next/vite.config.ts                     |    45 +\n linkis-web/.env                                    |     5 +-\n linkis-web/package.json                            |    15 +-\n linkis-web/release-docs/NOTICE                     |     2 +-\n .../release-docs/licenses/LICENSE-autoprefixer.txt |    20 +\n .../release-docs/licenses/LICENSE-postcss.txt      |    20 +\n linkis-web/src/apps/PythonModule/.env              |     6 +-\n linkis-web/src/apps/PythonModule/package.json      |     4 +-\n .../src/apps/PythonModule/src/letgo/global.css     |     5 +-\n .../apps/linkis/module/datasourceAccess/index.vue  |     4 +-\n .../src/apps/linkis/module/eurekaService/index.js  |    11 +-\n .../linkis/module/statisticsDashboard/index.js     |    25 +\n .../linkis/module/statisticsDashboard/index.scss   |   157 +\n .../linkis/module/statisticsDashboard/index.vue    |   786 ++\n .../statisticsDashboard/statisticsDashboard.vue    |   441 +\n linkis-web/src/apps/linkis/router.js               |    11 +-\n linkis-web/src/apps/linkis/view/linkis/index.vue   |    25 +-\n linkis-web/src/common/i18n/en.json                 |     1 +\n linkis-web/src/common/i18n/zh.json                 |     1 +\n linkis-web/src/dss/router.js                       |    10 +\n linkis-web/src/dss/view/login/index.vue            |    26 +-\n linkis-web/src/dss/view/login/oauthCallback.vue    |    55 +\n linkis-web/vue.config.js                           |    17 +-\n pom.xml                                            |   247 +-\n quick-build.cmd                                    |   190 +\n quick-build.ps1                                    |   220 +\n quick-build.sh                                     |   386 +\n tool/dependencies/known-dependencies.txt           |   979 +-\n .../regenerate_konwn_dependencies_txt.sh           |     6 +-\n 1778 files changed, 202269 insertions(+), 9670 deletions(-)', 'is_error': False}]

**AI**：
[思考] 差异非常巨大！有 1778 个文件被修改，新增了 202269 行，删除了 9670 行。这是一个非常大的版本升级。

让我分析更详细的差异，特别是关键模块的改动。...


## 交互记录 (2026-05-12T02:40:56.708Z)

**用户**：
[{'tool_use_id': 'call_9b64f0f5229f4241a1dde5c7', 'type': 'tool_result', 'content': ' linkis-commons/linkis-common/pom.xml               |   1 -\n linkis-commons/linkis-hadoop-common/pom.xml        |  54 +-\n linkis-commons/linkis-mybatis/pom.xml              |   2 +-\n linkis-commons/linkis-rpc/pom.xml                  |   9 +\n linkis-commons/linkis-storage/pom.xml              |  84 +-\n .../linkis-engineconn-manager-server/pom.xml       |   6 +\n .../linkis-computation-engineconn/pom.xml          |   4 +\n .../linkis-entrance/pom.xml                        |   7 +\n .../linkis-manager-persistence/pom.xml             |   2 +-\n linkis-dist/pom.xml                                |  13 +-\n .../doris}/pom.xml                                 |  46 +-\n linkis-engineconn-plugins/elasticsearch/pom.xml    | 136 +++\n linkis-engineconn-plugins/flink/flink-core/pom.xml | 333 +++++++\n .../flink/flink-shims-1.12.2/pom.xml               | 253 ++++++\n .../flink/flink-shims-1.16.2/pom.xml               | 238 +++++\n .../flink/flink-shims/pom.xml                      |  66 ++\n linkis-engineconn-plugins/flink/pom.xml            | 541 +----------\n linkis-engineconn-plugins/hbase/hbase-core/pom.xml | 150 +++-\n .../hbase/hbase-shims-1.2.0/pom.xml                |  99 +++\n .../hbase/hbase-shims-1.2.1/pom.xml                |  42 +-\n .../hbase/hbase-shims-1.4.3/pom.xml                |  95 ++\n .../hbase/hbase-shims-2.2.6/pom.xml                | 114 +++\n .../hbase/hbase-shims-2.5.3/pom.xml                | 118 +++\n linkis-engineconn-plugins/hbase/pom.xml            |   3 +\n linkis-engineconn-plugins/hive/pom.xml             | 164 ++--\n linkis-engineconn-plugins/impala/pom.xml           | 143 +++\n linkis-engineconn-plugins/io_file/pom.xml          |   3 -\n linkis-engineconn-plugins/jdbc/pom.xml             |  28 +-\n linkis-engineconn-plugins/openlookeng/pom.xml      | 136 +++\n linkis-engineconn-plugins/pipeline/pom.xml         |   3 -\n linkis-engineconn-plugins/pom.xml                  |   5 +\n linkis-engineconn-plugins/presto/pom.xml           |   4 -\n linkis-engineconn-plugins/python/pom.xml           |   3 -\n linkis-engineconn-plugins/repl/pom.xml             | 127 +++\n linkis-engineconn-plugins/seatunnel/pom.xml        |  20 +-\n linkis-engineconn-plugins/shell/pom.xml            |   3 -\n linkis-engineconn-plugins/spark/pom.xml            | 108 ++-\n linkis-engineconn-plugins/spark/scala-2.12/pom.xml | 985 +++++++++++++++++++++\n linkis-engineconn-plugins/sqoop/pom.xml            |  54 +-\n linkis-engineconn-plugins/trino/pom.xml            |   4 +-\n linkis-extensions/linkis-et-monitor/pom.xml        |  14 +-\n linkis-hadoop-hdfs-client-shade/pom.xml            |   3 +-\n .../linkis-bml-server/pom.xml                      |   4 -\n .../linkis-configuration/pom.xml                   |   7 +\n .../linkis-cs-server/pom.xml                       |   1 -\n .../linkis-datasource-manager/server/pom.xml       |   5 +\n .../service/elasticsearch/pom.xml                  |   0\n .../service/hdfs/pom.xml                           |   0\n .../service/hive/pom.xml                           |   9 +-\n .../service/jdbc/pom.xml                           |   0', 'is_error': False}]

**AI**：
改动非常大！1778 个文件被修改。让我深入分析关键的改动：...


## 交互记录 (2026-05-12T02:40:56.760Z)

**用户**：
[{'tool_use_id': 'call_fb414df160884ac19f19a550', 'type': 'tool_result', 'content': '* 6480e140f #AI commit# 开发阶段： 去除日志打印重复代码\n*   5e2cc8384 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n| * 04a953592 Dev 2.0.0 bug fix (#1026)\n* | b43a8ff7f #AI commit# 开发阶段： 修复开源代码合并异常\n* | 6b10a7238 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n| * 39f0e377d [fix][EC][spark] fix spark and hive engine execution task error exception handling (#1023)\n| * 936168a4e [fix][EC][JDBC] Add configurable validation query for different database types (#984)\n* | da0cb3eaa #AI commit# 开发阶段： 修复spark/hive引擎执行任务error异常\n* | e622406a2 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n| * 9c20c54b7 #AI commit# 开发阶段： 支持keytab环境，启动命令行增安全参数 (#1020)\n| * 67f0bef39 #AI commit# 开发阶段： 优化引擎日志输出用户执行code (#1021)\n| * fa31f0d86 #AI commit# 开发阶段： 安全工单升级commons-lang移除，commons-lang3升级3.18.0 (#1019)\n| * 88350524f [fix][ETS][et-monitor] fix log printing exception and optimize monitor cleanup process (#1015)\n| * cc92eeb16 #AI commit# 开发阶段： 增加label service 修改entrance 服务标签触发广播 (#1017)\n| * 53b4003bf #AI commit# 开发阶段： 同步flink版本 (#1013)\n| * a2ea93bdd #AI COMMIT# [fix][CGS] Add MDC jobId tracing for task-related logs (#1011)\n* | 5c023b0f4 #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | a35da050b #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | 048bffde2 #AI commit# 开发阶段： 修复日志打印异常，优化monitor清理流程\n* | 833f5c4d5 Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n| * 1fc405ca2 #AI commit# 开发阶段： add permission check for secondary queue selection (#1010)\n| * a0ac6f5e8 #AI commit# 开发阶段： add week variable support for date calculation (#1008)\n| * 2e0772cd4 #AI commit# 开发阶段： spark scala version update (#1006)\n| * 8086e269d [fix][EC][all-engines] mask user code in engine execution logs to prevent security leakage (#1003)\n| * a0c9a0fc9 #AI commit# 开发阶段： 加密日志打印token (#1002)\n| * b0b5866da #AI commit# 开发阶段： 优化禁止引擎打印code隐私信息 (#1000)\n| * 9b2d2d8f3 #AI commit# 开发阶段： 代码格式化 (#998)\n| * dc3897e7a [feat][CGS][engineconn-manager] add support for hadoop3 engine material refresh (#996)\n| * 6ea5a7d85 [docs] reorganize 2.0.0 feature documentation structure (#994)\n* | d34adc439 #AI commit# 开发阶段： 支持hadoop3引擎物料刷新\n* | 105675f96 #AI commit# 开发阶段： 日志打印优化，spark init sql 异常优化\n* | 994647ba1 #AI commit# 开发阶段： 优化2.0.0特性文档\n* | 83ed972dd Merge branch \'dev-2.0.0\' into dev-2.0.0-bug-fix\n| * 23fe635b9 #AI commit# [fix][PES][datasource] fix DB2 schema query to use correct SQL instead of invalid command (#983)\n| * 340ae2986 #AI commit# fix: Restore linkis-io_file-client dependency in linkis-pes-publicservice\n| * 614b0f3d2 [fix][CGS][entrance] fix dependency injection failure in entrance configuration (#980)\n* | 69fdf8449 #AI commit# 开发阶段： 修复依赖注入失败\n* | 45004df2a #AI commit# 开发阶段： 异常注释修复\n* 56478dc0d #AI commit# 开发阶段： *  资源重置接口逻辑优化 (#978)\n* 297faf474 [feat][ETS][monitor] optimize monitor module with log cleanup and connection pool expansion (#976)\n* 31bb2577c [feat][COM][logging] optimize log printing and add hadoop client usage logs (#974)\n* 27f6b8805 [feat][EC][hive] add support for yarn tag and username in hive engine (#970)\n* 57e014c84 [feat][CGS][hive] add security control to prevent location clause usage in hive tasks (#968)\n* 66489063e [fix][CGS][entrance] parallelgroup cache not updated when entrance instance goes offline (#966)\n* 0f69c9ffd #AI commit# chore: Bump version from 1.9.0 to 2.0.0\n* bf1cb5bce [LINKIS-1.9.0] Feature toggles, dependency optimization and bug fixes for linkis-web and backend (#5400)\n* cb5d91a7e #AI commit# 新增功能 - spark引擎支持设置driver参数到driver端 (#5398)\n* 5fdf4a04f #AI commit# 开发阶段： 修复sr任务重试导致加载init_sql异常bug (#5399)\n* 6ead0303c #AI commit# 开发阶段：管理台支持展示引擎版本 (#5396)\n* ecde36710 [1.9.0 release] [PES][EC][publicservice][spark] feat: ResultSet field processing, engine switch, task diagnosis and more (#5356)\n* 6285de37d Master feature (#5293)\n* e3c84aa44 fix kafka datasource fixerror & code format (#5291)\n* 3a0e7420d fix hive concurrent mode load udf error & code foramt (#5289)\n* 1603bdfda Modify the SparkMeasure feature to be disabled by default. (#5274)\n* 7f3bf7e95 Add urlencode check for jdbc url in SecurityUtils.java and fix password leak in HiveUtils.java (#5261)\n*   7dec8837c Merge pull request #5258 from apache/dependabot/npm_and_yarn/linkis-web-next/axios-1.12.0\n* |   c613f7747 Merge pull request #5257 from apache/dependabot/maven/linkis-public-enhancements/linkis-datasource/linkis-datasource-manager/server/org.hibernate-hibernate-validator-6.2.0.Final\n* |   d9028be14 Merge pull request #5255 from apache/dependabot/npm_and_yarn/linkis-web-next/vite-6.3.6\n* 09f526a58 1.8.0 release feature (#5225)\n*   ebc08a089 Merge pull request #5204 from WeDataSphere/master-s3\n* 9b5a34cdc feat: add support for sparkmeasure (#5202)\n* 3f552070e [linkis-web-next]: Modules refactoring & Bug fixing (#5169)\n* 09f5f22f9 [linkis-web-next]: Modules refactoring & Bug fixing (#5151)\n* b6e929adc [bugfix] close method moved to finally (#5165)\n* 0d1c3f8a5 fix: resource output json error (#5160)\n* | da5492102 fix: Linkis-Gateway can not work with DELETE request without request (#5154)\n* 9a9b2105b [linkis-web-next]: Modules refactoring & Bug fixing (#5145)\n*   cac834d8c Merge pull request #5133 from WeDataSphere/master-1.6.0-fix-gateway\n| * 134cf8772 Fix Spring Cloud gateway cannot support fixed IP and port forwarding. closed #5132\n*   5cd2de33a Merge pull request #5126 from WeDataSphere/master-1.6.0\n* 3c8f2aded add connection pool keepalive feature\n* 0c186f9a3 fix cancelled task still executed (#5074)\n* e79b59d9d fix the HBase command reports an error, but the client returns success (#5065)\n* be7b82051 fix: To avoid converting null to "null", skip null passwords (#5057)\n*   f81179420 Merge pull request #5037 from WeDataSphere/master-remove-log\n*   03540ea9f Merge pull request #4996 from WeDataSphere/master-ec-conf\n* | ce0bf4353 fix db2 connect issue (#4995)\n* | d9240c07d fix web router (#4991)\n*   9e35c2b86 Merge pull request #4990 from WeDataSphere/master-manager-ec\n| *   79cad7155 Merge remote-tracking branch \'upstream/master\' into master-manager-ec\n* |   b704a2aff Merge pull request #4982 from WeDataSphere/master-1.5.0\n| * |   5c204b244 Merge remote-tracking branch \'upstream/master\' into master-1.5.0\n| * | | 39aa193bb add template conf feature\n| * | | 7c9d17a71 fix build issue\n* | | a8fb845f8 Check spark commands only when ENABLE_SPARK is true and fix the last print (#4984)\n* |   d7512fa9e Merge pull request #4905 from WeDataSphere/master-1.1.15-merge\n| * \\   bd15f865b Merge branch \'apache:master\' into master-1.1.15-merge\n| * | | | 9f4a9c881 bugfix `refresh maxAllowRunningJobs`\n| * | | | 5a5e5b37b bugfix NPE\n| * | | | 21df2de8a bugfix `head of empty list` again\n| * | | | fe9c0b2e0 rollback bugfix `head of empty list`\n| * | | | c97eacbec bugfix `head of empty list`\n| * | | |   7a473e5e6 Merge branch \'master\' of https://github.com/apache/linkis into dev-1.3.3-feature-ha-4181\n| * | | |   4571c4c9e Merge branch \'master\' of https://github.com/apache/linkis into dev-1.3.3-feature-ha-4181\n| * | | | | caeeab9fc sql optimize and bug fix\n| * | | | |   a23b73b60 Merge branch \'dev-1.4.0\' of https://github.com/apache/linkis into dev-1.3.3-feature-ha-4181\n| * | | | | | 170c0c984 [Bug-fix] gateway choose instance\n| * | | | | | d7eb30227 [bug-fix] failover logic\n* | | | | | | b6af5f0c3 [WIP]fix issues #4972 #4973 (#4972)\n* | | | | |   2b4701468 Merge pull request #4965 from apache/dependabot/npm_and_yarn/linkis-web/axios-1.6.0', 'is_error': False}]

**AI**：
[思考] 差异确实非常大，从 1.18.3-webank 到 2.0.0-bug-fix 是一个大版本的升级。让我分析核心模块的改动：

1. 引擎插件架构变化：
   - flink: 从单一结构拆分为 flink-core + shims (1.12.2, 1.16.2)
   - hbase: 同样拆分为 hbase-core + shims (1.2.0, 1.2.1, 1.4.3, 2.2.6, 2.5.3)
   - spark: 新增 scala-2.12 支持
   - 新增 impala, openlookeng, repl 引擎

2. 版本依赖变化：
   - commons-lang 移除，commons-lang3 升级到 3.18.0
   - 大量依赖更新

让我分析关键的 Java/Scala 代码改动。...


## 交互记录 (2026-05-12T02:41:05.146Z)

**用户**：
[{'tool_use_id': 'call_44491ea51c554be0b8361d02', 'type': 'tool_result', 'content': ' .../interactor/command/fitter/FitterUtils.java     |    2 +-\n .../application/LinkisClientApplicationTest.java   |    8 +-\n .../src/test/resources/conf/linkis-cli.properties  |    2 +-\n .../src/test/resources/linkis-cli.properties       |    2 +-\n .../computation/client/LinkisJobBuilder.scala      |    8 +-\n .../ujes/client/response/JobInfoResult.scala       |    2 +\n .../computation/client/InteractiveJobTest.java     |    3 +-\n .../computation/client/SeatunnelOnceJobTest.java   |   18 +-\n .../linkis/ujes/client/JobObserveActionTest.scala  |    7 +-\n .../governance/common/entity/job/JobRequest.java   |   11 +\n .../common/constant/job/JobRequestConstants.scala  |    4 +\n .../common/protocol/conf/SecondaryYarnConf.scala   |   33 +\n .../common/utils/EngineConnArguments.scala         |   30 +-\n .../enums/MappingPortStrategyName.java             |   39 +\n .../strategy/MappingPortContext.java               |   32 +\n .../strategy/MappingPortStrategy.java              |   24 +\n .../strategy/StaticMappingPortStrategy.java        |   79 +\n .../ecm/core/conf/ContainerizationConf.scala       |   44 +\n .../apache/linkis/ecm/core/conf/ECPCoreConf.scala  |    6 +\n .../core/launch/ProcessEngineCommandBuilder.scala  |   16 +\n .../ecm/core/launch/ProcessEngineConnLaunch.scala  |   55 +\n .../linkis-engineconn-manager-server/pom.xml       |    6 +\n .../src/main/assembly/distribution.xml             |    4 +-\n .../operator/EngineConnYarnLogOperator.scala       |    4 +-\n .../impl/AbstractEngineConnLaunchService.scala     |    4 +-\n .../linkis-computation-engineconn/pom.xml          |    4 +\n .../executor/execute/ComputationExecutor.scala     |   10 +-\n .../executor/execute/EngineExecutionContext.scala  |    6 +\n .../service/TaskExecutionServiceImpl.scala         |   30 +-\n .../executor/entity/KubernetesExecutor.scala       |    0\n .../common/conf/EnvConfiguration.scala             |   14 +-\n .../common/launch/process/Environment.scala        |    6 +-\n .../JavaProcessEngineConnLaunchBuilder.scala       |    4 +\n .../linkis-entrance/pom.xml                        |    7 +\n .../src/main/assembly/distribution.xml             |    4 +-\n .../entrance/conf/EntranceSpringConfiguration.java |   27 +-\n .../entrance/parser/AbstractEntranceParser.java    |    1 -\n .../entrance/restful/EntranceLabelRestfulApi.java  |   56 +\n .../entrance/restful/EntranceRestfulApi.java       |  557 ++++--\n .../entrance/server/EntranceFailoverJobServer.java |  180 ++', 'is_error': False}]

**AI**：
让我分析核心模块的代码改动：...


## 交互记录 (2026-05-12T02:41:09.445Z)

**用户**：
[{'tool_use_id': 'call_a646a9251c4f43f0932ea1ca', 'type': 'tool_result', 'content': '214', 'is_error': False}]

**AI**：
[思考] 有很多新增和删除的 Java/Scala 文件。让我进一步分析关键代码变更，特别是可能影响稳定性的改动。...


## 交互记录 (2026-05-12T02:41:15.909Z)

**用户**：
[{'tool_use_id': 'call_61a7be2715da4d679366f059', 'type': 'tool_result', 'content': '+  public String getUriString() {\n-  public FsPath getParent() throws IOException {\n+  public FsPath getParent() {\n+public class CodeUtils {\n+  private static final int DEFAULT_CODE_SNIPPET_LENGTH = 6;\n+  private static final String MSG_EMPTY_CODE = "[empty code]";\n+  public static String maskCode(String code) {\n+  public static String maskCode(String code, String codeType) {\n+  public static String getCodeSnippet(String code, int length) {\n+public class JobHistoryInfo {\n+  private String jobReqId;\n+  private String submitUser;\n+  private String executeUser;\n+  private String source;\n+  private String labels;\n+  private String params;\n+  private String progress;\n+  private String status;\n+  private String logPath;\n+  private Integer errorCode;\n+  private String errorDesc;\n+  private Date createdTime;\n+  private Date updatedTime;\n+  private String instances;\n+  private String metrics;\n+  private String engineType;\n+  private String executionCode;\n+  public String getJobReqId() {\n+  public void setJobReqId(String jobReqId) {\n+  public String getSubmitUser() {\n+  public void setSubmitUser(String submitUser) {\n+  public String getExecuteUser() {\n+  public void setExecuteUser(String executeUser) {\n+  public String getSource() {\n+  public void setSource(String source) {\n+  public String getLabels() {\n+  public void setLabels(String labels) {\n+  public String getParams() {\n+  public void setParams(String params) {\n+  public String getProgress() {\n+  public void setProgress(String progress) {\n+  public String getStatus() {\n+  public void setStatus(String status) {\n+  public String getLogPath() {\n+  public void setLogPath(String logPath) {\n+  public Integer getErrorCode() {\n+  public void setErrorCode(Integer errorCode) {\n+  public String getErrorDesc() {\n+  public void setErrorDesc(String errorDesc) {\n+  public Date getCreatedTime() {\n+  public void setCreatedTime(Date createdTime) {\n+  public Date getUpdatedTime() {\n+  public void setUpdatedTime(Date updatedTime) {\n+  public String getInstances() {\n+  public void setInstances(String instances) {\n+  public String getMetrics() {\n+  public void setMetrics(String metrics) {\n+  public String getEngineType() {\n+  public void setEngineType(String engineType) {\n+  public String getExecutionCode() {\n+  public void setExecutionCode(String executionCode) {\n+  public JobHistoryInfo(\n+  public JobHistoryInfo() {}\n+  private static final String BLACKLIST_REGEX =\n+  public static void checkUrlIsSafe(String url) {\n+public class TokenSensitiveUtils {\n+  public static String maskToken(String token) {\n+  private var registryTimestamp: Long = _\n+  private var mappingPorts: String = _\n+  private var mappingHost: String = _\n+  private val startupConfRegex =\n+class CustomWeekType(date: String, std: Boolean = true, isEnd: Boolean = false) {\n+case class WeekType(value: CustomWeekType) extends VariableType {\n+case class BigDecimalValue(value: BigDecimal) extends VariableType {\n+          s"BigDecimal class is not supported to use:$signal"\n+  private def formatResult(bd: BigDecimal): String = {\n+  private def bigDecimalOrLong(bd: BigDecimal): BigDecimal = {\n+class FsPathTest {\n+  public void testGetSchemaPath() {\n+  public void testGetUriString() {\n+class ShaUtilsTest {\n+public class TokenSensitiveUtilsTest {\n+  public void testMaskTokenShort() {\n+  public void testMaskTokenLong() {\n+  public void testMaskTokenNullOrEmpty() {\n+  public void testMaskTokenLengthDifferentiation() {\n+  public void testMaskTokenEdgeCases() {\n+  public void testMaskTokenSpecialCharacters() {\n+  public void testMaskTokenThreadSafe() {\n+  public void testJsonArrayFormat() throws VariableOperationFailedException {\n+  private val keytabTempFileCache: LoadingCache[String, String] = {\n+  private def createKeytabCacheKey(userName: String, label: String): String = {\n+  private def getKeytabTempDir(): java.nio.file.Path = {\n+  private def createNewKeytabFile(\n+public class KerberosUtilsTest {\n+  public void getKerberosRefreshIntervalTest() {\n+  public void kinitFailTimesThresholdTest() {\n+class HDFSUtilsTest {\n+class HDFSUtilsKeytabCacheTest {\n+  private def getCreateKeytabCacheKeyMethod = {\n+  public static final CommonVars<String> BDP_SERVER_MYBATIS_PAGEHELPER_DIALECT =\n-public interface DataSourceOpService {}\n+case class UserWithCreator(user: String, creator: String)\n+case class JobInstance(\n+case class EntranceGroupCacheClearBroadcast(instance: String, timestamp: Long)\n-  private val EXEC_ID = "exec_id"\n+public class LoadBalancerOptionsUtils {\n+  private static Options DEFAULT_OPTIONS = null;\n+  private static Object locker = new Object();\n+  public static Options getDefaultOptions() throws NoSuchFieldException, IllegalAccessException {\n-//  private val commonRetryHandler = new RPCRetryHandler\n-//  private def isCommonRetryHandler(retry: RetryableProtocol): Boolean = retry.maxPeriod == commonRetryHandler.getRetryMaxPeriod &&\n+  private var maxAllowRunningJobs: Int = 0\n-class PriorityLoopArrayQueueTest {\n-  public void testConcurrentPutAndTake() throws Exception {\n-  private void consume(PriorityLoopArrayQueue queue) {\n-  private void product(AtomicInteger counter, PriorityLoopArrayQueue queue) {\n-  private void printEvent(String opt, SchedulerEvent event) {\n-  private int getRandom(int bound) {\n-  private UserJob getJob(String name, int priority) {\n+public class BuildAzureBlobFileSystem implements BuildFactory {\n+  private static final Logger LOG = LoggerFactory.getLogger(BuildAzureBlobFileSystem.class);\n+  public Fs getFs(String user, String proxyUser) {\n+  public Fs getFs(String user, String proxyUser, String label) {\n+  public String fsName() {\n+public class BuildOSSSystem implements BuildFactory {\n+  private static final Logger LOG = LoggerFactory.getLogger(BuildOSSSystem.class);\n+  public Fs getFs(String user, String proxyUser) {\n+  public Fs getFs(String user, String proxyUser, String label) {\n+  public String fsName() {\n+public class BuildS3FileSystem implements BuildFactory {\n+  private static final Logger LOG = LoggerFactory.getLogger(BuildS3FileSystem.class);\n+  public Fs getFs(String user, String proxyUser) {\n+  public Fs getFs(String user, String proxyUser, String label) {\n+  public String fsName() {\n-  public abstract List<FsPath> getAllFilePaths(FsPath dest) throws IOException;\n+  public List<FsPath> getAllFilePaths(FsPath dest) throws IOException {\n+public class AzureBlobFileSystem extends FileSystem {\n+  private static final String SLASH = "/";\n+  public static class PahtInfo {\n+    private String schema = "http://"; // http\n+    private String domain; //\n+    private String container; // container name\n+    private String blobName; // blob name\n+    private String tail;\n+    public PahtInfo(String domain, String container, String blobName) {\n+    public String toFullName() {\n+    public String getSchema() {\n+    public String getDomain() {\n+    public String getContainer() {\n+    public String getBlobName() {\n+    public String getTail() {\n+    public String toString() {\n+  private BlobServiceClient serviceClient;\n+  private BlobContainerClient getBlobContainerClient(String containerName) {\n+  private PahtInfo azureLocation(String path) {\n+  private PahtInfo azureLocation(FsPath dest) {\n+  public void init(Map<String, String> properties) throws IOException {\n+  public String fsName() {\n+  public String rootUserName() {\n+  public FsPath get(String dest) throws IOException {\n+  public InputStream read(FsPath dest) {\n+  public OutputStream write(FsPath dest, boolean overwrite) {\n+  public boolean create(String dest) throws IOException {\n+  public List<FsPath> list(FsPath path) throws IOException {\n+  public boolean canRead(FsPath dest) throws IOException {\n+  public boolean canRead(FsPath dest, String user) throws IOException {\n+  public boolean canWrite(FsPath dest) throws IOException {\n+  public boolean exists(FsPath dest) throws IOException {\n+  public boolean delete(FsPath dest) throws IOException {\n+  public boolean copy(String origin, String dest) throws IOException {\n+  public boolean renameTo(FsPath oldDest, FsPath newDest) throws IOException {\n+  public boolean mkdir(FsPath dest) throws IOException {\n+  public boolean mkdirs(FsPath dest) throws IOException {\n+  public String listRoot() throws IOException {\n+  public long getTotalSpace(FsPath dest) throws IOException {\n+  public long getFreeSpace(FsPath dest) throws IOException {\n+  public long getUsableSpace(FsPath dest) throws IOException {\n+  public long getLength(FsPath dest) throws IOException {\n+  public String getChecksum(FsPath dest) throws IOException {\n+  public String getChecksumWithMD5(FsPath dest) throws IOException {\n+  public long getBlockSize(FsPath dest) throws IOException {\n+  public boolean canExecute(FsPath dest) throws IOException {\n+  public boolean setOwner(FsPath dest, String user, String group) throws IOException {\n+  public boolean setOwner(FsPath dest, String user) throws IOException {\n+  public boolean setGroup(FsPath dest, String group) throws IOException {\n+  public boolean setPermission(FsPath dest, String permission) throws IOException {\n+  public void close() throws IOException {}\n-  public String getChecksumWithMD5(FsPath dest) throws IOException {\n+  public String checkSum(FsPath dest) throws IOException {\n+  public String getChecksumWithMD5(FsPath dest) throws IOException {\n+public class OSSFileSystem extends FileSystem {\n+  public static final String OSS_PREFIX = "oss://";\n+  private org.apache.hadoop.fs.aliyun.oss.AliyunOSSFileSystem fs = null;\n+  private Configuration conf = null;\n+  private String label = null;\n+  private static final Logger logger = LoggerFactory.getLogger(OSSFileSystem.class);\n+  public String listRoot() throws IOException {\n+  public long getTotalSpace(FsPath dest) throws IOException {\n+  public long getFreeSpace(FsPath dest) throws IOException {', 'is_error': False}]

**AI**：
[思考] 现在我有了足够的信息来分析差异。让我总结一下主要的变化：

1. **大版本升级**：从 1.18.x 到 2.0.0，这是一个跨越多个小版本的大升级
2. **引擎插件架构重构**：
   - Flink 引擎拆分为核心 + shims 模式
   - HBase 引擎同样采用 shims 模式
   - Spark 新增 Scala 2.12 支持
   - 新增多个引擎支持

3. **安全增强**：
   - 日志脱敏（token、用户代码）
   - URL 安全检查
   - Keytab 环境支持

4. **依赖变化**：
   - commons-lang 移除，升级到 commons-lang3
   - 大量依赖版本升级

5. **核心模块改动**：
   - 入口服务修复（依赖注入、缓存更新）
   - 引擎连接管理优化
   - 监控模块优化

现在让我生成一份详细的分析报告。...


## 交互记录 (2026-05-12T02:41:17.662Z)

**用户**：
[{'tool_use_id': 'call_6ef2f2ef3fd449cf8abd2a80', 'type': 'tool_result', 'content': "5e2cc8384 Merge branch 'dev-2.0.0' into dev-2.0.0-bug-fix\n04a953592 Dev 2.0.0 bug fix (#1026)\n6b10a7238 Merge branch 'dev-2.0.0' into dev-2.0.0-bug-fix\n39f0e377d [fix][EC][spark] fix spark and hive engine execution task error exception handling (#1023)\n936168a4e [fix][EC][JDBC] Add configurable validation query for different database types (#984)\ne622406a2 Merge branch 'dev-2.0.0' into dev-2.0.0-bug-fix\n88350524f [fix][ETS][et-monitor] fix log printing exception and optimize monitor cleanup process (#1015)\na2ea93bdd #AI COMMIT# [fix][CGS] Add MDC jobId tracing for task-related logs (#1011)\n833f5c4d5 Merge branch 'dev-2.0.0' into dev-2.0.0-bug-fix\n8086e269d [fix][EC][all-engines] mask user code in engine execution logs to prevent security leakage (#1003)\n83ed972dd Merge branch 'dev-2.0.0' into dev-2.0.0-bug-fix\n23fe635b9 #AI commit# [fix][PES][datasource] fix DB2 schema query to use correct SQL instead of invalid command (#983)\n340ae2986 #AI commit# fix: Restore linkis-io_file-client dependency in linkis-pes-publicservice\n614b0f3d2 [fix][CGS][entrance] fix dependency injection failure in entrance configuration (#980)\n66489063e [fix][CGS][entrance] parallelgroup cache not updated when entrance instance goes offline (#966)\nbf1cb5bce [LINKIS-1.9.0] Feature toggles, dependency optimization and bug fixes for linkis-web and backend (#5400)\necde36710 [1.9.0 release] [PES][EC][publicservice][spark] feat: ResultSet field processing, engine switch, task diagnosis and more (#5356)\n6285de37d Master feature (#5293)\ne3c84aa44 fix kafka datasource fixerror & code format (#5291)\n3a0e7420d fix hive concurrent mode load udf error & code foramt (#5289)\n0cbe383b4 Release 1.8.0 rc1 (#5286)\n19961164d Release 1.8.0 code merge (#5269)\n6f77ab030 Fix split sql concatenated limit exceptions by semicolon (#5245)\n7f3bf7e95 Add urlencode check for jdbc url in SecurityUtils.java and fix password leak in HiveUtils.java (#5261)\nebc08a089 Merge pull request #5204 from WeDataSphere/master-s3\nd67847e86 Fix build\n6f3bb2294 Fix build\n14111f918 Fix ss3 and oss\nbca9fe22f Add a containerized mode to the ECM service (#5201)\n9b5a34cdc feat: add support for sparkmeasure (#5202)\n2fdf9d3e2 [EnginePlugin][Spark]Turn off use secure random by default (#5197)\n9036be666 Add a new management console to modify the engine status to UnHealthy and automatically kill the engine after task execution\n3f552070e [linkis-web-next]: Modules refactoring & Bug fixing (#5169)\nb8406e196 Support resultSet direct push for SparkSqlExecutor (#5170)\nd99f967b0 [Feature][Commons] Linkis Commons Function optimization (#5168)\n09f5f22f9 [linkis-web-next]: Modules refactoring & Bug fixing (#5151)\nb6e929adc [bugfix] close method moved to finally (#5165)\n0d1c3f8a5 fix: resource output json error (#5160)\nda5492102 fix: Linkis-Gateway can not work with DELETE request without request (#5154)\n9a9b2105b [linkis-web-next]: Modules refactoring & Bug fixing (#5145)\n2b55fe634 [LINKIS-5147][EC-JDBC] Add timeout configurations of datasource for jdbc engine (#5148)\n9be69de55 Upgrade version to 1.6.0 (#5142)\ncac834d8c Merge pull request #5133 from WeDataSphere/master-1.6.0-fix-gateway\n134cf8772 Fix Spring Cloud gateway cannot support fixed IP and port forwarding. closed #5132\n5831a19b1 Fix build issue\n6b1a82298 Fix log loss issue\n434e14b75 Fix engine exit hang bug\n43cb5b2e3 The interface of basic data management should be judged by the administrator. (#5108)\n562572ae8 Fix the spark could not start (#5105)\n15621240e Fix the eureka could not start (#5104)", 'is_error': False}]

**AI**：
让我再查看一些关键的配置和兼容性变更：...


## 交互记录 (2026-05-12T02:41:25.485Z)

**用户**：
[{'tool_use_id': 'call_4151ac647bb5457e9744e868', 'type': 'tool_result', 'content': 'diff --git a/linkis-commons/linkis-rpc/pom.xml b/linkis-commons/linkis-rpc/pom.xml\nindex e916107f5..2fc3ab3ba 100644\n--- a/linkis-commons/linkis-rpc/pom.xml\n+++ b/linkis-commons/linkis-rpc/pom.xml\n@@ -40,8 +40,17 @@\n           <groupId>org.springframework.cloud</groupId>\n           <artifactId>spring-cloud-commons</artifactId>\n         </exclusion>\n+        <exclusion>\n+          <groupId>org.springframework.cloud</groupId>\n+          <artifactId>spring-cloud-loadbalancer</artifactId>\n+        </exclusion>\n       </exclusions>\n     </dependency>\n+    <dependency>\n+      <groupId>org.springframework.cloud</groupId>\n+      <artifactId>spring-cloud-loadbalancer</artifactId>\n+      <version>${spring-cloud-common.version}</version>\n+    </dependency>\n     <dependency>\n       <groupId>org.springframework.cloud</groupId>\n       <artifactId>spring-cloud-starter-openfeign</artifactId>\ndiff --git a/linkis-commons/linkis-rpc/src/main/java/org/apache/linkis/rpc/message/utils/LoadBalancerOptionsUtils.java b/linkis-commons/linkis-rpc/src/main/java/org/apache/linkis/rpc/message/utils/LoadBalancerOptionsUtils.java\nnew file mode 100644\nindex 000000000..93762d1f3\n--- /dev/null\n+++ b/linkis-commons/linkis-rpc/src/main/java/org/apache/linkis/rpc/message/utils/LoadBalancerOptionsUtils.java\n@@ -0,0 +1,42 @@\n+/*\n+ * Licensed to the Apache Software Foundation (ASF) under one or more\n+ * contributor license agreements.  See the NOTICE file distributed with\n+ * this work for additional information regarding copyright ownership.\n+ * The ASF licenses this file to You under the Apache License, Version 2.0\n+ * (the "License"); you may not use this file except in compliance with\n+ * the License.  You may obtain a copy of the License at\n+ *\n+ *    http://www.apache.org/licenses/LICENSE-2.0\n+ *\n+ * Unless required by applicable law or agreed to in writing, software\n+ * distributed under the License is distributed on an "AS IS" BASIS,\n+ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n+ * See the License for the specific language governing permissions and\n+ * limitations under the License.\n+ */\n+\n+package org.apache.linkis.rpc.message.utils;\n+\n+import java.lang.reflect.Field;\n+\n+import feign.Request.Options;\n+\n+public class LoadBalancerOptionsUtils {\n+\n+  private static Options DEFAULT_OPTIONS = null;\n+\n+  private static Object locker = new Object();\n+\n+  public static Options getDefaultOptions() throws NoSuchFieldException, IllegalAccessException {\n+    if (null == DEFAULT_OPTIONS) {\n+      synchronized (locker) {\n+        Class<?> clazz = null;\n+        Field optionField = clazz.getDeclaredField("DEFAULT_OPTIONS");\n+        optionField.setAccessible(true);\n+        Object o = optionField.get(clazz);\n+        DEFAULT_OPTIONS = (Options) o;\n+      }\n+    }\n+    return DEFAULT_OPTIONS;\n+  }\n+}\ndiff --git a/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/RPCServerLoader.scala b/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/RPCServerLoader.scala\nindex a210a068e..e4259466b 100644\n--- a/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/RPCServerLoader.scala\n+++ b/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/RPCServerLoader.scala\n@@ -50,6 +50,7 @@ abstract class AbstractRPCServerLoader extends RPCServerLoader with Logging {\n       refreshed: => Boolean,\n       serviceInstance: ServiceInstance\n   ): Unit = {\n+\n     val instanceNotExists = new NoInstanceExistsException(\n       APPLICATION_IS_NOT_EXISTS.getErrorCode,\n       MessageFormat.format(\ndiff --git a/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/common/RetryableRPCInterceptor.scala b/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/common/RetryableRPCInterceptor.scala\nindex 6c6e9592f..0fc2a3910 100644\n--- a/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/common/RetryableRPCInterceptor.scala\n+++ b/linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/interceptor/common/RetryableRPCInterceptor.scala\n@@ -43,25 +43,15 @@ import feign.RetryableException\n class RetryableRPCInterceptor extends RPCInterceptor {\n   override val order: Int = 20\n \n-//  private val commonRetryHandler = new RPCRetryHandler\n-//  commonRetryHandler.setRetryInfo(new RetryableProtocol{})\n-//\n-//  private def isCommonRetryHandler(retry: RetryableProtocol): Boolean = retry.maxPeriod == commonRetryHandler.getRetryMaxPeriod &&\n-//    retry.period == commonRetryHandler.getRetryPeriod && retry.retryNum == commonRetryHandler.getRetryNum &&\n-//    (retry.retryExceptions.isEmpty || commonRetryHandler.getRetryExceptions.containsSlice(retry.retryExceptions))\n-\n   override def intercept(\n       interceptorExchange: RPCInterceptorExchange,\n       chain: RPCInterceptorChain', 'is_error': False}]

**AI**：
[思考] 让我继续分析更多细节，然后生成一份完整的风险分析报告。...


## 交互记录 (2026-05-12T02:41:27.068Z)

**用户**：
[{'tool_use_id': 'call_5275d08d44ab4c6691e85f47', 'type': 'tool_result', 'content': 'ecde36710 [1.9.0 release] [PES][EC][publicservice][spark] feat: ResultSet field processing, engine switch, task diagnosis and more (#5356)\n6285de37d Master feature (#5293)', 'is_error': False}]

**AI**：
[思考] 现在我有了足够的信息。让我生成一份详细的风险分析报告。...


## 交互记录 (2026-05-12T02:41:32.240Z)

**用户**：
[{'tool_use_id': 'call_e2e21b1728e4410a9a6f599e', 'type': 'tool_result', 'content': '+    <revision>2.0.0</revision>\n     <jedis.version>2.9.2</jedis.version>\n     <spark.version>2.4.3</spark.version>\n-    <hadoop.version>3.3.5</hadoop.version>\n+    <hadoop.version>2.7.2</hadoop.version>\n     <hbase.version>1.2.1</hbase.version>\n     <jruby.verison>9.3.4.0</jruby.verison>\n     <jcodings.version>1.0.56</jcodings.version>\n     <joni.version>2.1.42</joni.version>\n     <hadoop-hdfs-client.artifact>hadoop-hdfs</hadoop-hdfs-client.artifact>\n-    <hadoop-hdfs-client-shade.version>3.3.5</hadoop-hdfs-client-shade.version>\n+    <hadoop-hdfs-client-shade.version>2.7.2</hadoop-hdfs-client-shade.version>\n     <zookeeper.version>3.8.4</zookeeper.version>\n     <!-- hadoop 2.7 use curator 2.7.1, hadoop3.3 use curator 4.2.0-->\n-    <curator.version>4.2.0</curator.version>\n+    <curator.version>2.7.1</curator.version>\n     <guava.version>33.2.1-jre</guava.version>\n--\n     <kafka-client.version>3.9.1</kafka-client.version>\n \n+    <!-- Cloud Storage -->\n+    <hadoop-aliyun.version>2.9.1</hadoop-aliyun.version>\n+    <azure.blob.bom>1.2.30</azure.blob.bom>\n+\n     <!-- jakarta -->\n--\n+    <commons-cli.version>1.3.1</commons-cli.version>\n+    <bouncycastle.version>1.79</bouncycastle.version>\n+    <mockito-bom.version>4.3.1</mockito-bom.version>\n+    <spark.hadoop.scope>provided</spark.hadoop.scope>\n+    <spark.hadoop-common.artifactId>hadoop-common</spark.hadoop-common.artifactId>\n+    <spark.hadoop-hdfs.artifactId>hadoop-hdfs</spark.hadoop-hdfs.artifactId>\n+    <spark.hadoop.groupid>org.apache.hadoop</spark.hadoop.groupid>\n+    <spark.hadoop.version>${hadoop.version}</spark.hadoop.version>\n+    <arrow.version>2.0.0</arrow.version>\n+\n     <!-- test -->\n--\n-    <spring.boot.version>2.7.12</spring.boot.version>\n+    <spring.boot.version>2.7.18</spring.boot.version>\n     <spring-netflix.version>3.1.7</spring-netflix.version>\n     <spring-cloud.version>2021.0.8</spring-cloud.version>\n     <spring-cloud-alibaba.version>2021.0.6.0</spring-cloud-alibaba.version>\n@@ -345,11 +400,7 @@\n         <artifactId>commons-collections</artifactId>\n         <version>${commons-collections.version}</version>\n--\n+\n+      <!-- Hadoop Aliyun OSS support -->\n+      <dependency>\n+        <groupId>org.apache.hadoop</groupId>\n+        <artifactId>hadoop-aliyun</artifactId>\n+        <version>${hadoop-aliyun.version}</version>\n+      </dependency>\n+\n       <dependency>\n--\n+        <revision>2.0.0</revision>\n+\n+        <!-- Hadoop Ecosystem -->\n+        <hadoop.version>3.3.4</hadoop.version>\n+        <hadoop-hdfs-client.artifact>hadoop-hdfs-client</hadoop-hdfs-client.artifact>\n+        <hbase.version>2.5.3</hbase.version>\n+        <zookeeper.version>3.9.2</zookeeper.version>\n+        <curator.version>4.2.0</curator.version>\n--\n+        <scala.binary.version>2.12</scala.binary.version>\n+\n+        <!-- Storage -->\n+        <spark.hadoop.scope>provided</spark.hadoop.scope>\n+        <spark.hadoop-common.artifactId>hadoop-common</spark.hadoop-common.artifactId>\n+        <spark.hadoop-hdfs.artifactId>hadoop-hdfs</spark.hadoop-hdfs.artifactId>\n+        <spark.hadoop.groupid>org.apache.hadoop</spark.hadoop.groupid>\n+        <spark.hadoop.version>${hadoop.version}</spark.hadoop.version>\n+        <arrow.version>2.0.0</arrow.version>\n+        <storage.parquet.scope>provided</storage.parquet.scope>\n+        <storage.orc.scope>provided</storage.orc.scope>\n--\n+        <flink.version>1.12.2</flink.version>\n+\n+        <!-- Cloud Storage -->\n+        <hadoop-aliyun.version>3.3.4</hadoop-aliyun.version>\n+        <azure.blob.bom>1.2.30</azure.blob.bom>\n+      </properties>\n+    </profile>', 'is_error': False}]

**AI**：
基于以上分析，我来为你生成一份详细的改动分析和风险评估报告：

## Linkis 1.18.3-webank → 2.0.0-bug-fix 差异分析报告

### 一、变更规模

| 指标 | 数值 |
|------|------|
| 提交数 | 739 |
| 修改文件数 | 1778 |
| 新增行数 | 202,269 |
| 删除行数 | 9,670 |
| 新增/删除 Java/Scala 文件 | 214 |

### 二、关键架构变更

#### 1. 引擎插件架构重构（高风险）

**Flink 引擎**：拆分为 `flink-core` + `flink-shims` 模式
- 支持 Flink 1.12.2 和 1.16.2 双版本
- 原有单一 pom.xml (541 行) 拆分为多个模块

**HBase 引擎**：同样采用 shims 模式
- 支持 HBase 1.2.0、1.2.1、1.4.3、2.2.6、2.5.3
- 新增多版本适配层

**Spark 引擎**：新增 Scala 2.12 支持
- 新增 `spark/scala-2.12` 子模块 (985 行)

**新增引擎**：impala、openlookeng、repl

#### 2. 依赖版本变化（高风险）

| 组件 | 1.18.3-webank | 2.0.0-bug-fix |
|------|---------------|---------------|
| Hadoop | 3.3.5 | 2.7.2 (默认) / 3.3.4 (profile) |
| Spring Boot | 2.7.12 | 2.7.18 |
| commons-lang | 存在 | 移除，升级到 commons-lang3 3.18.0 |
| curator | 4.2.0 | 2.7.1 (Hadoop2) / 4.2.0 (Hadoop3) |

⚠️ **Hadoop 降级警告**：默认 Hadoop 从 3.3.5 降级到 2.7.2

#### 3. 核心模块改动

**RPC 通信层**：
- 新增 `LoadBalancerOptionsUtils` 类
- 修改 Spring Cloud LoadBalancer 依赖

**Entrance 服务**：
- 修复依赖注入失败问题
- 修复 Entrance 实例下线后 ParallelGroup 缓存未更新的问题
- 新增 `EntranceLabelRestfulApi` (56 行)

**引擎执行器**：
- Spark/Hive 引擎异常处理修复
- 日志脱敏功能（防止用户代码泄露）
- Token 脱敏功能

**存储层**：
- 新增 Azure Blob、OSS、S3 文件系统支持

### 三、潜在风险点

#### 🔴 高风险

1. **Hadoop 版本不兼容**
   - 默认从 Hadoop 3.3.5 降级到 2.7.2
   - 可能导致 HDFS API 不兼容
   - 需要检查所有 HDFS 调用代码

2. **commons-lang 移除**
   - 直接移除 `commons-lang` 依赖
   - 所有 `org.apache.commons.lang` 包调用需要迁移到 `org.apache.commons.lang3`
   - 可能导致大量运行时错误

3. **引擎插件 shims 模式**
   - Flink/HBase 引擎架构完全重构
   - 可能导致现有引擎配置失效
   - 需要重新测试所有引擎启动流程

#### 🟡 中风险

1. **Spring Cloud 升级**
   - Spring Boot 2.7.12 → 2.7.18
   - Spring Cloud LoadBalancer 变更
   - 可能影响服务发现和负载均衡

2. **日志脱敏功能**
   - 新增代码和 Token 脱敏
   - 可能影响日志排查效率

3. **配置服务变更**
   - 新增 Tenant 配置
   - 需要检查配置兼容性

#### 🟢 低风险

1. 新增云存储支持（不影响现有功能）
2. 前端优化
3. 文档更新

### 四、建议测试重点

1. **兼容性测试**：
   - HDFS 文件读写
   - 引擎启动（特别是 Flink 和 HBase）
   - 配置加载

2. **功能回归测试**：
   - 任务提交和执行
   - 引擎生命周期管理
   - 日志输出验证

3. **性能测试**：
   - 并发任务处理
   - 引擎启动时间
 ...

