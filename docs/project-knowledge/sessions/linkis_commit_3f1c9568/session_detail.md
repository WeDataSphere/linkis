
## 交互记录 (2026-08-21T07:32:04.351Z)

**用户**：
<command-message>pr</command-message>
<command-name>/pr</command-name>
<command-args>dev-2.1.2-task-diagnosis 提交到dev-2.1.2-webank</command-args>

**AI**：
...


## 交互记录 (2026-08-21T07:32:04.351Z)

**用户**：
[{'type': 'text', 'text': 'Base directory for this skill: C:\\Users\\v_kkhuang\\.claude\\skills\\pr\n\n# GitHub PR 自动创建器 (Apache Linkis 版)\n\n## ⚠️ 强制要求（MUST READ）\n\n**执行此技能时，必须严格遵守以下规则，违反任何一项都将导致输出不合格：**\n\n1. **Issue 处理优先**: 必须先搜索/创建 Issue，然后才能创建 PR。绝不能跳过 Issue 步骤直接创建 PR。\n2. **标题格式**: PR 标题和 Issue 标题必须严格遵循格式，大小写不能错。\n3. **内容全英文**: PR 和 Issue 的所有内容必须是英文，不能有中文。\n4. **三段式描述**: PR 的 "What is the purpose" 必须使用三段式格式（Background/Problem, Purpose, Value/Impact）。\n5. **Issue 关联**: PR 创建时必须关联 Issue（使用 `close #issue_id`）。\n6. **必须询问**: 找到相关 Issues 时必须询问用户选择，不能自动决定。\n7. **不允许 commit**: 只创建 PR 和 Issue，不执行任何 git commit 操作。\n\n---\n\n## 工作流程（按顺序执行）\n\n```\n┌─────────────────────────────────────────────────────────────────────────────┐\n│                        Apache Linkis PR 创建流程                              │\n├─────────────────────────────────────────────────────────────────────────────┤\n│  Step 1: 询问分支信息（base + target）                                        │\n│     ↓                                                                        │\n│  Step 2: 获取代码变更（git diff + git log）                                   │\n│     ↓                                                                        │\n│  Step 3: 分析变更，识别模块（一级 + 二级）                                    │\n│     ↓                                                                        │\n│  Step 4: 搜索 GitHub Issues（gh issue list）                                 │\n│     ↓                                                                        │\n│  Step 5: 询问用户选择 Issue（复用现有 OR 创建新 Issue）                       │\n│     ↓                                                                        │\n│  Step 6: 如果需要，创建新 Issue（gh issue create）                            │\n│     ↓                                                                        │\n│  Step 7: 生成 PR 标题（严格格式）                                              │\n│     ↓                                                                        │\n│  Step 8: 生成 PR 描述（三段式 + Brief change log + Checklist）                │\n│     ↓                                                                        │\n│  Step 9: 创建 PR 并关联 Issue（gh pr create）                                 │\n└─────────────────────────────────────────────────────────────────────────────┘\n```\n\n---\n\n## 执行步骤详解\n\n### Step 1: 询问分支信息\n\n**必须询问用户以下信息：**\n\n```\n请输入以下信息以创建 Pull Request：\n\n1. 基础分支（base branch）: 即要合并的目标分支（例如: apachemaster, dev-1.19.0）\n2. 目标分支（target branch）: 即你的特性分支（例如: dev-1.19.0-feature-xxx）\n\n请确认这是用于创建 Pull Request，而非其他操作。\n```\n\n**验证规则**：\n- 必须确认用户意图是创建 PR\n- 如果分支不存在，列出可用分支供用户选择\n- 使用 `git branch -r` 列出远程分支\n\n---\n\n### Step 2: 获取代码变更\n\n**执行以下命令获取变更信息：**\n\n```bash\n# 获取变更文件列表\ngit diff --name-status <base_branch>..<target_branch>\n\n# 获取提交历史\ngit log <base_branch>..<target_branch> --oneline --no-merges\n\n# 获取详细的代码差异（用于分析）\ngit diff <base_branch>..<target_branch> --stat\n```\n\n**分析内容**：\n- 变更的文件数量和类型\n- 变更的代码行数\n- 提交的数量和主题\n\n---\n\n### Step 3: 分析变更，识别模块\n\n**从变更文件路径中提取模块信息：**\n\n#### 一级模块映射表（严格使用）\n\n| 目录名 | 缩写 | 全称 |\n|--------|------|------|\n| `linkis-spring-cloud-services` | `MGS` | microservice-governance |\n| `linkis-computation-governance` | `CGS` | computation-governance |\n| `linkis-public-enhancements` | `PES` | public-enhancements |\n| `linkis-engineconn-plugins` | `EC` | engineconn |\n| `linkis-extensions` | `ETS` | extensions |\n| `linkis-orchestrator` | `ORC` | orchestrator |\n| `linkis-web` | `WEB` | web |\n| `linkis-commons` | `COM` | commons |\n\n#### 二级模块提取规则\n\n| 一级模块 | 二级模块提取方式 | 示例 |\n|---------|-----------------|------|\n| MGS | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-gateway` → `gateway` |\n| CGS | `linkis-cg-xxx` 中的 xxx 部分（小写）或 `linkis-xxx` 去掉前缀 | `linkis-entrance` → `entrance` |\n| PES | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-datasource` → `datasource` |\n| EC | 引擎名称（小写） | `spark` → `spark` |\n| WEB | 通常无二级模块，使用 `frontend` 或省略 | - |\n| COM | 目录名（小写） | `common` → `common` |\n\n**模块识别逻辑**：\n1. 如果所有变更文件在同一模块，使用该模块\n2. 如果跨越多个模块，选择变更最多的模块作为主模块\n3. 如果变更分散且数量相近，询问用户确认主模块\n\n---\n\n### Step 4: 搜索 GitHub Issues\n\n**必须执行 Issue 搜索，不能跳过：**\n\n```bash\n# 搜索 open issues，使用模块和变更关键词\ngh issue list --state open --search "<模块名> <关键词>" --limit 20\n\n# 例如：\n# gh issue list --state open --search "PES datasource filter" --limit 20\n# gh issue list --state open --search "CGS entrance location" --limit 20\n```\n\n**搜索关键词提取**：\n- 模块名（一级 + 二级）\n- 变更相关的关键词（从文件名、提交信息中提取）\n- action 类型（feat/fix 等）\n\n---\n\n### Step 5: 询问用户选择 Issue\n\n#### 如果找到相关 Issues（至少 1 个）\n\n**必须询问用户，不能自动选择：**\n\n```\n找到以下相关的 open issues：\n\n#123: [bug][PES][datasource] add support for getting published datasource by type and proxy user\n#456: [feature][CGS][entrance] hive location clause security check\n\n请选择：\n1. 使用现有 issue #123\n2. 使用现有 issue #456\n3. 创建新 issue\n4. 手动输入其他 issue 编号\n\n请输入选项 (1/2/3/4) 或 issue 编号:\n```\n\n#### 如果没有找到相关 Issues\n\n**直接进入创建新 Issue 流程**（Step 6）\n\n---\n\n### Step 6: 创建新 Issue（如需要）\n\n**如果用户选择创建新 Issue，或没有找到相关 Issue，则执行以下步骤：**\n\n#### Issue 标题格式（严格遵循）\n\n```\n[类型][一级模块][二级模块] 功能描述\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 示例 |\n|------|------|------|\n| `类型` | **小写** | `bug`, `feature`, `ask` |\n| `一级模块` | **大写** | `MGS`, `CGS`, `PES`, `EC`, `ETS`, `ORC`, `WEB`, `COM` |\n| `二级模块` | **小写** | `entrance`, `datasource`, `spark` 等 |\n| `功能描述` | **小写句子** | 首字母不大写，使用普通英文句子 |\n\n**类型选择规则**：\n- PR action 为 `fix` → Issue 类型为 `bug`\n- PR action 为 `feat` → Issue 类型为 `feature`\n- 其他情况 → Issue 类型为 `feature`\n\n**Issue 标题示例**：\n```\n✅ 正确: [bug][CGS][entrance] hive tasks should not be allowed to use location clause for security reasons\n✅ 正确: [feature][PES][datasource] add support for getting published datasource by type and proxy user\n❌ 错误: [Bug][CGS][Entrance] Hive Tasks Should Not Be Allowed... (大小写错误)\n❌ 错误: [bug][cgs][entrance] ... (一级模块小写错误)\n```\n\n#### Issue 内容格式（严格遵循）\n\n**根据 Issue 类型选择对应模板：**\n\n##### Bug Report 模板（fix 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar issues.\n\n### Linkis Component\n\n- [ ] linkis-commons\n- [ ] linkis-computation-governance\n- [ ] linkis-dist\n- [ ] linkis-engineconn-plugin\n- [ ] linkis-extensions\n- [ ] linkis-orchestrator\n- [x] linkis-public-enhancements\n- [ ] linkis-spring-cloud-services\n- [ ] linkis-web\n\n### Description\n\n[Brief description of the bug - what happened and what should happen]\n\n### Steps to reproduce\n\n1. Step 1...\n2. Step 2...\n3. Step 3...\n\n### Expected behavior\n\n[Describe what you expected to happen]\n\n### Your environment\n\n- Linkis version used: [x.x.x]\n- Environment name and version:\n    - hadoop-x.x.x\n    - hive-x.x.x\n    - spark-x.x.x\n    - scala-x.x.x\n    - jdk 1.8.x_xx\n\n### Anything else\n\n[Additional context, logs, frequency of occurrence, etc.]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n##### Feature Request 模板（feat 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar feature requirement.\n\n### Problem Description\n\nIs your feature request related to a problem? Please describe.\n\n[Describe the problem or need]\n\n### Description\n\nA short description of your feature\n\n[Clear and concise description of what you want to add]\n\n### Use case\n\nDescribe the use case of your feature request and the solution you\'d like.\n\n[Describe the specific use case and expected behavior]\n\n### Solutions\n\nDescribe alternatives you\'ve considered.\n\n[Describe any alternative solutions or workarounds you\'ve considered]\n\n### Anything else\n\nAdditional context or screenshots about the feature request.\n\n[Any additional information, screenshots, or examples]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n#### Issue 标签规则\n\n| Issue 类型 | 标签 |\n|-----------|------|\n| `bug` | `bug`, `status: needs triage` |\n| `feature` | `enhancement`, `status: needs triage` |\n| `ask` | `question`, `status: needs triage` |\n\n#### 创建 Issue 命令\n\n```bash\ngh issue create \\\n  --title "[<type>][<MODULE>][<submodule>] <description>" \\\n  --body "<issue_content>" \\\n  --label "<label1>,<label2>"\n```\n\n**保存返回的 issue_id，用于 PR 关联！**\n\n---\n\n### Step 7: 生成 PR 标题\n\n**PR 标题格式（严格遵循）**：\n\n```\n[action][一级模块][二级模块] 一句话总结\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 可选值 | 示例 |\n|------|------|--------|------|\n| `action` | **小写** | feat, fix, perf, refactor, docs, style, test, chore | `feat`, `fix` |\n| `一级模块` | **大写** | MGS, CGS, PES, EC, ETS, ORC, WEB, COM | `PES`, `CGS` |\n| `二级模块` | **小写** | 具体服务名称 | `datasource`, `entrance` |\n| `一句话总结` | **小写句子** | 普通英文，首字母不大写 | `add support for filtering by type` |\n\n**Action 类型定义**：\n\n| Action | 含义 | 使用场景 |\n|--------|------|----------|\n| `feat` | 新功能 | 添加新的功能、API、模块 |\n| `fix` | 修复 Bug | 修复问题、缺陷 |\n| `perf` | 性能优化 | 提升性能、优化资源使用 |\n| `refactor` | 重构 | 代码重构，不改变功能 |\n| `docs` | 文档 | 仅文档变更 |\n| `style` | 代码风格 | 格式化、代码风格调整 |\n| `test` | 测试 | 仅测试相关变更 |\n| `chore` | 构建/工具 | 构建、依赖、工具链更新 |\n\n**PR 标题示例**：\n\n```\n✅ 正确: [feat][PES][datasource] add support for getting published datasource by type and proxy user\n✅ 正确: [fix][CGS][entrance] resolve null pointer exception in engine reuse metrics update\n✅ 正确: [perf][CGS][application-manager] optimize engine creation performance\n❌ 错误: [Feat][PES][datasource] Add Support For... (大小写错误)\n❌ 错误: [feat][pES][datasource] ... (一级模块大小写混合)\n❌ 错误: feat: add support for... (缺少模块标签)\n```\n\n---\n\n### Step 8: 生成 PR 描述\n\n**PR 描述必须严格遵循以下格式：**\n\n#### PR 描述模板（完整格式）\n\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\n[Describe the background or problem - what issue exists, 2-3 sentences]\n\n**Purpose of Change:**\n[Describe the purpose of this PR - what this PR does to solve the problem, 2-3 sentences]\n\n**Value/Impact:**\n[Describe the value or impact - what benefits this change brings, 1-2 sentences]\n\n### Related issues/PRs\n\nRelated issues: close #<issue_id>\nRelated pr:none\n\n### Brief change log\n\n- [Change point 1 - start with verb]\n- [Change point 2 - start with verb]\n- [Change point 3 - start with verb]\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n#### "What is the purpose" 格式要求（严格三段式）\n\n**必须使用三段式结构，不能合并或省略：**\n\n| 段落 | 标题 | 内容要求 | 长度 |\n|------|------|----------|------|\n| 第一段 | **Background/Problem:** | 描述背景或问题 - 当前存在什么问题或需求 | 2-3 句话 |\n| 第二段 | **Purpose of Change:** | 描述改动的目的 - 本 PR 如何解决这个问题 | 2-3 句话 |\n| 第三段 | **Value/Impact:** | 描述价值或影响 - 改动带来的好处 | 1-2 句话 |\n\n**格式规范**：\n- ✅ 每段标题使用粗体，以冒号结尾\n- ✅ 段落标题使用 `**Title:**` 格式\n- ✅ 内容另起一行，不要和标题在同一行\n- ❌ 不要使用 "This PR adds..." 或 "This PR implements..." 格式\n- ✅ 使用客观描述，如 "This PR adds...", "The change enables..."\n\n**示例**：\n\n```\n✅ 正确格式:\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n❌ 错误格式:\n\nThis PR adds support for filtering datasources by type and user. (缺少三段式)\nThe problem is that users cannot filter datasources. (不完整)\n```\n\n#### "Brief change log" 格式要求\n\n**每条变更记录必须：**\n- 以动词开头\n- 使用现在时\n- 简洁明确\n- 每条独立一行\n\n**常用动词示例**：\n- `Add` - 添加新功能\n- `Fix` - 修复问题\n- `Update` - 更新现有功能\n- `Remove` - 删除功能\n- `Refactor` - 重构代码\n- `Optimize` - 优化性能\n- `Improve` - 改进\n\n**示例**：\n\n```\n✅ 正确:\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n❌ 错误:\n- Added API... (过去时)\n- Update the service... (不明确)\n```\n\n#### Checklist 智能勾选规则\n\n| 选项 | 何时勾选 | 说明 |\n|------|---------|------|\n| I have read the Contributing Guidelines | ✅ 始终勾选 | - |\n| I have explained the need for this PR | ✅ 始终勾选 | - |\n| I have explained the changes | ✅ 有代码变更时勾选 | 大多数情况勾选 |\n| I have added tests | 有测试时勾选 | 如果添加了测试代码 |\n| I have updated the documentation | 有文档变更时勾选 | 如果修改了文档 |\n| I have verified backward compatibility | ✅ 通常勾选 | 除非是破坏性变更 |\n| I have written unit tests | 有完整测试时勾选 | 如果有完整的单元测试 |\n\n---\n\n### Step 9: 创建 PR\n\n**执行 PR 创建命令：**\n\n```bash\ngh pr create \\\n  --title "<PR_TITLE>" \\\n  --body "<PR_BODY>" \\\n  --base "<base_branch>" \\\n  --head "<target_branch>" \\\n  --label "<label1>,<label2>"\n```\n\n**PR 标签映射规则**：\n\n| Action 类型 | Label |\n|-----------|-------|\n| `feat` | `enhancement` |\n| `fix` | `bug` |\n| `perf` | `performance` |\n| `refactor` | `refactor` |\n| `docs` | `documentation` |\n| `style` | `style` |\n| `test` | `test` |\n| `chore` | `build` |\n\n---\n\n## 错误处理\n\n| 错误场景 | 处理方式 |\n|---------|----------|\n| 找不到 git 仓库 | 提示用户在正确的项目目录中执行 |\n| gh 未登录 | 提示用户运行 `gh auth login` |\n| 分支不存在 | 列出可用分支供用户选择 |\n| 未找到代码变更 | 提示用户检查分支是否正确 |\n| Issue 搜索失败 | 提示用户手动关联 Issue |\n| PR 创建失败 | 显示错误信息，提示用户检查权限或网络 |\n\n---\n\n## 完整示例\n\n### 示例 1: 新增功能（创建新 Issue）\n\n**场景**: 添加数据源过滤功能，无相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-public-enhancements/linkis-datasource/src/...`\n- 模块: `PES` / `datasource`\n- Action: `feat`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "PES datasource filter" --limit 20\n```\n结果: 无相关 Issues\n\n**Step 6**: 创建新 Issue\n```bash\ngh issue create \\\n  --title "[feature][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --label "enhancement,status: needs triage"\n```\n返回: Issue #789\n\n**Step 7**: 生成 PR 标题\n```\n[feat][PES][datasource] add support for getting published datasource by type and proxy user\n```\n\n**Step 8**: 生成 PR 描述\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n### Related issues/PRs\n\nRelated issues: close #789\nRelated pr:none\n\n### Brief change log\n\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[feat][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --base "apachemaster" \\\n  --head "dev-1.19.0-feature-datasource-filter" \\\n  --label "enhancement"\n```\n\n---\n\n### 示例 2: 修复 Bug（复用现有 Issue）\n\n**场景**: 修复空指针异常，找到相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-computation-governance/linkis-cg-linkismanager/src/...`\n- 模块: `CGS` / `linkismanager` (简化为 `application-manager`)\n- Action: `fix`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "CGS application-manager null pointer" --limit 20\n```\n结果: 找到 Issue #456\n\n**Step 5**: 询问用户\n```\n找到以下相关的 open issues：\n\n#456: [bug][CGS][application-manager] null pointer exception in engine reuse metrics update causing monitoring data loss\n\n请选择：\n1. 使用现有 issue #456\n2. 创建新 issue\n3. 手动输入其他 issue 编号\n```\n用户选择: 1\n\n**Step 6**: 跳过（复用现有 Issue #456）\n\n**Step 7**: 生成 PR 标题\n```\n[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update\n```\n\n**Step 8**: 生成 PR 描述（使用 Bug Report 风格的三段式）\n```markdown\n### What is the purpose of the change\n\n**Background/Problem:**\nDuring engine creation and reuse, when updating metrics information, a null pointer exception occurs, causing some monitoring data to be lost and affecting system stability.\n\n**Purpose of Change:**\nTo address this problem, this PR adds null checks and protection to the relevant code, ensuring the robustness of metrics updates.\n\n**Value/Impact:**\nAfter the change, the system runs stably with complete and available monitoring data, improving system reliability.\n\n### Related issues/PRs\n\nRelated issues: close #456\nRelated pr:none\n\n### Brief change log\n\n- Add null pointer checks in DefaultEngineCreateService\n- Add null pointer checks in DefaultEngineReuseService\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update" \\\n  --body "..." \\\n  --base "dev-1.19.0" \\\n  --head "dev-1.19.0-fix-null-pointer" \\\n  --label "bug"\n```\n\n---\n\n## 快速参考\n\n### 大小写规则速查\n\n| 位置 | PR | Issue |\n|------|-----|-------|\n| 类型/Action | **小写** | **小写** |\n| 一级模块 | **大写** | **大写** |\n| 二级模块 | **小写** | **小写** |\n| 描述/总结 | **小写句子** | **小写句子** |\n\n### 格式速查\n\n```\nPR 标题:    [action][MODULE][submodule] description\nIssue 标题:  [type][MODULE][submodule] description\n\nPR action:   feat, fix, perf, refactor, docs, style, test, chore\nIssue type:  bug, feature, ask\n\nMODULE:      MGS, CGS, PES, EC, ETS, ORC, WEB, COM\n```\n\n### 命令速查\n\n```bash\n# 搜索 Issues\ngh issue list --state open --search "<关键词>" --limit 20\n\n# 创建 Issue\ngh issue create --title "<标题>" --body "<内容>" --label "<标签>"\n\n# 创建 PR\ngh pr create --title "<标题>" --body "<内容>" --base "<base>" --label "<标签>"\n\n# 列出分支\ngit branch -r\n```\n\n---\n\n## 使用方法\n\n```bash\n/pr\n```\n\n然后按照提示输入分支信息即可。\n\n---\n\n## 依赖工具\n\n- `git` - 版本控制\n- `gh` (GitHub CLI) - GitHub 操作\n\n确保 `gh` 已登录：`gh auth login`\n\n\nARGUMENTS: dev-2.1.2-task-diagnosis 提交到dev-2.1.2-webank'}]

**AI**：
[思考] The user is invoking the `/pr` skill with arguments: `dev-2.1.2-task-diagnosis 提交到dev-2.1.2-webank`

So the user wants to create a PR:
- Source/head branch: `dev-2.1.2-task-diagnosis`
- Target/base branch: `dev-2.1.2-webank`

Let me follow the workflow:
1. Step 1: Branch info - already provided: base = `dev-2.1.2-webank`, target = `dev-2.1.2-task-diagnosis`
2. Step 2: Get code changes
3. Step 3: Analyze changes, identify modules
4. Step 4: Search GitHub Issues
5. Step 5: Ask user to choose Issue
6. Step 6: Create new Issue if needed
7. Step 7: Generate PR title
8. Step 8: Generate PR description
9. Step 9: Create PR and link Issue

Let me start by getting the code changes. I'll run the git commands in parallel to understand the changes.

The skill says NOT to commit, only create PR and Issue. Let me follow the workflow strictly.

Let me start with Step 2 - getting code changes. I'll run several git commands in parallel....


## 交互记录 (2026-08-21T07:32:40.609Z)

**用户**：
[{'tool_use_id': 'call_ad8ea090d6724e12a9fe9e95', 'type': 'tool_result', 'content': 'A\t"docs/dev-2.1.0/design/REQ-04_\\344\\273\\273\\345\\212\\241\\345\\244\\261\\350\\264\\245\\345\\210\\206\\347\\261\\273\\350\\257\\212\\346\\226\\255_\\350\\256\\276\\350\\256\\241.md"\nA\t"docs/dev-2.1.0/requirements/REQ-04_\\344\\273\\273\\345\\212\\241\\345\\244\\261\\350\\264\\245\\345\\210\\206\\347\\261\\273\\350\\257\\212\\346\\226\\255_\\351\\234\\200\\346\\261\\202.md"\nA\tlinkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/entity/DiagnosisResult.java\nM\tlinkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/restful/api/QueryRestfulApi.java\nA\tlinkis-public-enhancements/linkis-jobhistory/src/main/scala/org/apache/linkis/jobhistory/conf/TaskDiagnosisConfiguration.scala\nA\tlinkis-public-enhancements/linkis-jobhistory/src/main/scala/org/apache/linkis/jobhistory/service/TaskDiagnosisService.java\nA\tlinkis-public-enhancements/linkis-jobhistory/src/main/scala/org/apache/linkis/jobhistory/service/impl/TaskDiagnosisServiceImpl.java', 'is_error': False}]

**AI**：
我将遵循 PR 创建工作流程。分支信息很明确：
- **Base (目标) 分支**: `dev-2.1.2-webank`
- **Head (源) 分支**: `dev-2.1.2-task-diagnosis`

让我先从 Step 2 开始：获取代码变更。...


## 交互记录 (2026-08-21T07:32:40.641Z)

**用户**：
[{'tool_use_id': 'call_2a352606defe490fb6d74322', 'type': 'tool_result', 'content': '2fe0badae #AI commit# 开发阶段： 支持ai运维诊断，增加诊断接口\naafaef900 #AI commit# 开发阶段： 支持ai运维诊断，增加诊断接口', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:32:40.816Z)

**用户**：
[{'tool_use_id': 'call_2c994615c7eb41c79a7251ca', 'type': 'tool_result', 'content': ' ...57\\212\\346\\226\\255_\\350\\256\\276\\350\\256\\241.md" | 1492 ++++++++++++++++++++\n ...57\\212\\346\\226\\255_\\351\\234\\200\\346\\261\\202.md" |  622 ++++++++\n .../linkis/jobhistory/entity/DiagnosisResult.java  |  151 ++\n .../jobhistory/restful/api/QueryRestfulApi.java    |   32 +\n .../conf/TaskDiagnosisConfiguration.scala          |   51 +\n .../jobhistory/service/TaskDiagnosisService.java   |   32 +\n .../service/impl/TaskDiagnosisServiceImpl.java     |  369 +++++\n 7 files changed, 2749 insertions(+)', 'is_error': False}]

**AI**：
[思考] Let me analyze the changes:

1. Two docs files (design doc and requirements doc) - in Chinese, related to "任务失败分类诊断" (task failure classification diagnosis)
2. `DiagnosisResult.java` - new entity
3. `QueryRestfulApi.java` - modified, added diagnosis interface
4. `TaskDiagnosisConfiguration.scala` - new config
5. `TaskDiagnosisService.java` - new service interface
6. `TaskDiagnosisServiceImpl.java` - new service implementation

All the code changes are in `linkis-public-enhancements/linkis-jobhistory/`. So the module is:
- One-level: `PES` (public-enhancements)
- Two-level: `jobhistory`

The action type: This is a new feature (AI ops diagnosis, diagnosis interface). The commits say "支持ai运维诊断，增加诊断接口" (support AI ops diagnosis, add diagnosis interface). So this is `feat`.

Let me look at the actual code changes to understand what the feature does, so I can write a good PR description. Let me read the key files....


## 交互记录 (2026-08-21T07:32:58.062Z)

**用户**：
[{'tool_use_id': 'call_7de58f80d7b3488b9fb9f95d', 'type': 'tool_result', 'content': 'diff --git a/linkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/restful/api/QueryRestfulApi.java b/linkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/restful/api/QueryRestfulApi.java\nindex 8bbfb3f6f..1d821b117 100644\n--- a/linkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/restful/api/QueryRestfulApi.java\n+++ b/linkis-public-enhancements/linkis-jobhistory/src/main/java/org/apache/linkis/jobhistory/restful/api/QueryRestfulApi.java\n@@ -23,10 +23,12 @@ import org.apache.linkis.governance.common.constant.job.JobRequestConstants;\n import org.apache.linkis.governance.common.entity.job.QueryException;\n import org.apache.linkis.jobhistory.cache.impl.DefaultQueryCacheManager;\n import org.apache.linkis.jobhistory.conf.JobhistoryConfiguration;\n+import org.apache.linkis.jobhistory.conf.TaskDiagnosisConfiguration;\n import org.apache.linkis.jobhistory.conversions.TaskConversions;\n import org.apache.linkis.jobhistory.entity.*;\n import org.apache.linkis.jobhistory.service.JobHistoryDiagnosisService;\n import org.apache.linkis.jobhistory.service.JobHistoryQueryService;\n+import org.apache.linkis.jobhistory.service.TaskDiagnosisService;\n import org.apache.linkis.jobhistory.transitional.TaskStatus;\n import org.apache.linkis.jobhistory.util.JobhistoryUtils;\n import org.apache.linkis.jobhistory.util.QueryUtils;\n@@ -79,6 +81,7 @@ public class QueryRestfulApi {\n \n   @Autowired private DefaultQueryCacheManager queryCacheManager;\n   @Autowired private JobHistoryDiagnosisService jobHistoryDiagnosisService;\n+  @Autowired private TaskDiagnosisService taskDiagnosisService;\n \n   @ApiOperation(\n       value = "governanceStationAdmin",\n@@ -873,4 +876,33 @@ public class QueryRestfulApi {\n     }\n     return Message.ok().data("diagnosisMsg", diagnosisMsg);\n   }\n+\n+  @ApiOperation(\n+      value = "task-diagnosis",\n+      notes = "classify failed task diagnosis type and return detailed info",\n+      response = Message.class)\n+  @ApiImplicitParams({@ApiImplicitParam(name = "taskID", dataType = "Long", required = true)})\n+  @RequestMapping(path = "/task-diagnosis", method = RequestMethod.GET)\n+  public Message classifyTaskDiagnosis(\n+      HttpServletRequest req, @RequestParam(value = "taskID") Long taskID) {\n+    String username = ModuleUserUtils.getOperationUser(req, "task-diagnosis");\n+    if (!(Boolean) TaskDiagnosisConfiguration.TASK_CLASSIFIED_DIAGNOSIS_ENABLE().getHotValue()) {\n+      return Message.ok().data("diagnosisType", "DISABLED").data("message", "分类诊断功能未开启");\n+    }\n+    DiagnosisResult result;\n+    try {\n+      result = taskDiagnosisService.classify(taskID, username);\n+    } catch (Exception t) {\n+      log.warn("Failed to classify task diagnosis for taskID: {}", taskID, t);\n+      DiagnosisResult errorResult = new DiagnosisResult();\n+      errorResult.setTaskID(taskID);\n+      errorResult.setDiagnosisType("ERROR");\n+      errorResult.setErrorDesc("诊断过程发生异常: " + t.getMessage());\n+      result = errorResult;\n+    }\n+    if (result == null) {\n+      return Message.error("任务不存在");\n+    }\n+    return Message.ok().data("result", result);\n+  }\n }', 'is_error': False}]

**AI**：
现在让我查看实际的代码变更，以了解该功能。让我读取关键文件。...


## 交互记录 (2026-08-21T07:33:00.285Z)

**用户**：
[{'tool_use_id': 'call_ea24082fe043431ba600dc35', 'type': 'tool_result', 'content': '/*\n * Licensed to the Apache Software Foundation (ASF) under one or more\n * contributor license agreements.  See the NOTICE file distributed with\n * this work for additional information regarding copyright ownership.\n * The ASF licenses this file to You under the Apache License, Version 2.0\n * (the "License"); you may not use this file except in compliance with\n * the License.  You may obtain a copy of the License at\n *\n *    http://www.apache.org/licenses/LICENSE-2.0\n *\n * Unless required by applicable law or agreed to in writing, software\n * distributed under the License is distributed on an "AS IS" BASIS,\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n * See the License for the specific language governing permissions and\n * limitations under the License.\n */\n\npackage org.apache.linkis.jobhistory.entity;\n\npublic class DiagnosisResult {\n\n  private Long taskID;\n  private String taskStatus;\n  private String diagnosisType;\n  private String failedStage;\n  private Integer errorCode;\n  private String errorDesc;\n  private String engineType;\n  private String engineConnInstance;\n  private Object yarnResource;\n\n  public DiagnosisResult() {}\n\n  public static DiagnosisResult linkis(\n      Long taskID, String taskStatus, String failedStage, Integer errorCode, String errorDesc) {\n    DiagnosisResult result = new DiagnosisResult();\n    result.setTaskID(taskID);\n    result.setTaskStatus(taskStatus);\n    result.setDiagnosisType("LINKIS");\n    result.setFailedStage(failedStage);\n    result.setErrorCode(errorCode);\n    result.setErrorDesc(errorDesc);\n    return result;\n  }\n\n  public static DiagnosisResult underlying(\n      Long taskID,\n      String taskStatus,\n      String failedStage,\n      Integer errorCode,\n      String errorDesc,\n      String engineType,\n      String engineConnInstance,\n      Object yarnResource) {\n    DiagnosisResult result = new DiagnosisResult();\n    result.setTaskID(taskID);\n    result.setTaskStatus(taskStatus);\n    result.setDiagnosisType("UNDERLYING");\n    result.setFailedStage(failedStage);\n    result.setErrorCode(errorCode);\n    result.setErrorDesc(errorDesc);\n    result.setEngineType(engineType);\n    result.setEngineConnInstance(engineConnInstance);\n    result.setYarnResource(yarnResource);\n    return result;\n  }\n\n  public static DiagnosisResult unknown(\n      Long taskID, String taskStatus, String failedStage, Integer errorCode, String errorDesc) {\n    DiagnosisResult result = new DiagnosisResult();\n    result.setTaskID(taskID);\n    result.setTaskStatus(taskStatus);\n    result.setDiagnosisType("UNKNOWN");\n    result.setFailedStage(failedStage);\n    result.setErrorCode(errorCode);\n    result.setErrorDesc(errorDesc);\n    return result;\n  }\n\n  public Long getTaskID() {\n    return taskID;\n  }\n\n  public void setTaskID(Long taskID) {\n    this.taskID = taskID;\n  }\n\n  public String getTaskStatus() {\n    return taskStatus;\n  }\n\n  public void setTaskStatus(String taskStatus) {\n    this.taskStatus = taskStatus;\n  }\n\n  public String getDiagnosisType() {\n    return diagnosisType;\n  }\n\n  public void setDiagnosisType(String diagnosisType) {\n    this.diagnosisType = diagnosisType;\n  }\n\n  public String getFailedStage() {\n    return failedStage;\n  }\n\n  public void setFailedStage(String failedStage) {\n    this.failedStage = failedStage;\n  }\n\n  public Integer getErrorCode() {\n    return errorCode;\n  }\n\n  public void setErrorCode(Integer errorCode) {\n    this.errorCode = errorCode;\n  }\n\n  public String getErrorDesc() {\n    return errorDesc;\n  }\n\n  public void setErrorDesc(String errorDesc) {\n    this.errorDesc = errorDesc;\n  }\n\n  public String getEngineType() {\n    return engineType;\n  }\n\n  public void setEngineType(String engineType) {\n    this.engineType = engineType;\n  }\n\n  public String getEngineConnInstance() {\n    return engineConnInstance;\n  }\n\n  public void setEngineConnInstance(String engineConnInstance) {\n    this.engineConnInstance = engineConnInstance;\n  }\n\n  public Object getYarnResource() {\n    return yarnResource;\n  }\n\n  public void setYarnResource(Object yarnResource) {\n    this.yarnResource = yarnResource;\n  }\n}', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:33:02.818Z)

**用户**：
[{'tool_use_id': 'call_1399a2edb3c24f198c6d5ca9', 'type': 'tool_result', 'content': '/*\n * Licensed to the Apache Software Foundation (ASF) under one or more\n * contributor license agreements.  See the NOTICE file distributed with\n * this work for additional information regarding copyright ownership.\n * The ASF licenses this file to You under the Apache License, Version 2.0\n * (the "License"); you may not use this file except in compliance with\n * the License.  You may obtain a copy of the License at\n *\n *    http://www.apache.org/licenses/LICENSE-2.0\n *\n * Unless required by applicable law or agreed to in writing, software\n * distributed under the License is distributed on an "AS IS" BASIS,\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n * See the License for the specific language governing permissions and\n * limitations under the License.\n */\n\npackage org.apache.linkis.jobhistory.conf\n\nimport org.apache.linkis.common.conf.CommonVars\n\nobject TaskDiagnosisConfiguration {\n\n  val TASK_CLASSIFIED_DIAGNOSIS_ENABLE: CommonVars[Boolean] =\n    CommonVars("linkis.task.classified.diagnosis.enable", false)\n\n  val LINKIS_ERROR_CODES: CommonVars[String] =\n    CommonVars(\n      "linkis.task.classified-diagnosis.linkis.error-codes",\n      "20039,12003,40102,40103,40100,40105,20010,20011,20052"\n    )\n\n  val ENGINE_ERROR_CODE_RANGE_START: CommonVars[Int] =\n    CommonVars("linkis.task.classified-diagnosis.engine.error-code-range-start", 26000)\n\n  val ENGINE_ERROR_CODE_RANGE_END: CommonVars[Int] =\n    CommonVars("linkis.task.classified-diagnosis.engine.error-code-range-end", 29999)\n\n  val UNDERLYING_KEYWORDS: CommonVars[String] =\n    CommonVars(\n      "linkis.task.classified-diagnosis.underlying.keywords",\n      "Container killed by YARN,Container killed by the Yarn,java.lang.OutOfMemoryError,OOM,引擎OOM,引擎退出,引擎意外退出,EC exits unexpectedly,quited unexpectedly,java.io.FileNotFoundException,org.apache.spark.SparkException,org.apache.hadoop.hive.ql.exec,Connection refused,Could not connect,Table not found,Database not found,Permission denied,无权限访问,权限不足,AccessControlException,Application killed by user,ParseException,语法错误,AnalysisException"\n    )\n\n  val LINKIS_KEYWORDS: CommonVars[String] =\n    CommonVars(\n      "linkis.task.classified-diagnosis.linkis.keywords",\n      "EngineConn closed,engineconn is ShuttingDown,requestEngineFailed,ask engine failed,engine not exists,EngineConn not found,Failed to launch EngineConn,SendToEntrance error,主动kill引擎"\n    )\n\n}', 'is_error': False}]

**AI**：
[思考] Now I have a good understanding. Let me look at the service implementation to get the full picture, and also understand the feature better....


## 交互记录 (2026-08-21T07:33:11.114Z)

**用户**：
[{'tool_use_id': 'call_dd3325fe84d74afeb33fe3af', 'type': 'tool_result', 'content': '/*\n * Licensed to the Apache Software Foundation (ASF) under one or more\n * contributor license agreements.  See the NOTICE file distributed with\n * this work for additional information regarding copyright ownership.\n * The ASF licenses this file to You under the Apache License, Version 2.0\n * (the "License"); you may not use this file except in compliance with\n * the License.  You may obtain a copy of the License at\n *\n *    http://www.apache.org/licenses/LICENSE-2.0\n *\n * Unless required by applicable law or agreed to in writing, software\n * distributed under the License is distributed on an "AS IS" BASIS,\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n * See the License for the specific language governing permissions and\n * limitations under the License.\n */\n\npackage org.apache.linkis.jobhistory.service.impl;\n\nimport org.apache.linkis.common.conf.Configuration;\nimport org.apache.linkis.jobhistory.conf.TaskDiagnosisConfiguration;\nimport org.apache.linkis.jobhistory.entity.DiagnosisResult;\nimport org.apache.linkis.jobhistory.entity.JobHistory;\nimport org.apache.linkis.jobhistory.service.JobHistoryQueryService;\nimport org.apache.linkis.jobhistory.service.TaskDiagnosisService;\nimport org.apache.linkis.jobhistory.transitional.TaskStatus;\nimport org.apache.linkis.jobhistory.util.JobhistoryUtils;\n\nimport org.apache.commons.lang3.StringUtils;\nimport com.fasterxml.jackson.databind.ObjectMapper;\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.stereotype.Service;\n\nimport java.util.*;\nimport java.util.regex.Pattern;\nimport java.util.stream.Collectors;\n\n@Service\npublic class TaskDiagnosisServiceImpl implements TaskDiagnosisService {\n\n  private static final Logger logger =\n      LoggerFactory.getLogger(TaskDiagnosisServiceImpl.class);\n\n  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();\n\n  @Autowired private JobHistoryQueryService jobHistoryQueryService;\n\n  /**\n   * Mapping from Linkis error code to [module, reason] for logging purposes.\n   */\n  private static final HashMap<Integer, String[]> LINKIS_ERROR_CODE_MAP = new HashMap<>();\n\n  static {\n    LINKIS_ERROR_CODE_MAP.put(20039, new String[] {"interceptor", "Interceptor error"});\n    LINKIS_ERROR_CODE_MAP.put(12003, new String[] {"ecm", "ECM error"});\n    LINKIS_ERROR_CODE_MAP.put(40102, new String[] {"engineconn", "EngineConn error"});\n    LINKIS_ERROR_CODE_MAP.put(40103, new String[] {"engineconn", "EngineConn error"});\n    LINKIS_ERROR_CODE_MAP.put(40100, new String[] {"engineconn", "EngineConn error"});\n    LINKIS_ERROR_CODE_MAP.put(40105, new String[] {"engineconn", "EngineConn error"});\n    LINKIS_ERROR_CODE_MAP.put(20010, new String[] {"entrance", "Entrance error"});\n    LINKIS_ERROR_CODE_MAP.put(20011, new String[] {"entrance", "Entrance error"});\n    LINKIS_ERROR_CODE_MAP.put(20052, new String[] {"jobhistory", "JobHistory error"});\n  }\n\n  @Override\n  public DiagnosisResult classify(Long taskID, String username) {\n    logger.info("Starting task diagnosis classification for taskID: {}, username: {}", taskID, username);\n\n    JobHistory jobHistory = getJobHistoryWithPermission(taskID, username);\n    if (jobHistory == null) {\n      logger.warn("JobHistory not found for taskID: {}, username: {}", taskID, username);\n      return null;\n    }\n\n    String taskStatus = jobHistory.getStatus();\n    Integer errorCode = jobHistory.getErrorCode();\n    String errorDesc = jobHistory.getErrorDesc();\n\n    if ("Succeed".equals(taskStatus)) {\n      logger.info("Task {} completed successfully", taskID);\n      DiagnosisResult result = new DiagnosisResult();\n      result.setTaskID(taskID);\n      result.setTaskStatus(taskStatus);\n      result.setDiagnosisType("SUCCESS");\n      return result;\n    }\n\n    Map<String, Object> metricsMap = parseMetrics(jobHistory.getMetrics());\n\n    if (isFailedStatus(taskStatus)) {\n      logger.info("Task {} is in failed status: {}, applying failed diagnosis", taskID, taskStatus);\n      return applyFailedDiagnosis(taskID, taskStatus, errorCode, errorDesc, jobHistory, metricsMap);\n    }\n\n    logger.info("Task {} is in progress: {}, diagnosing blocking stage", taskID, taskStatus);\n    return buildRunningTaskResult(taskID, taskStatus, jobHistory, metricsMap);\n  }\n\n  /**\n   * Apply stage-first classification for failed tasks.\n   * 1. Determine stage from metrics (Scheduling vs Execution)\n   * 2. UNDERLYING keywords override everything\n   * 3. Scheduling stage defaults to LINKIS; Execution stage applies errorCode → Linkis keyword → UNKNOWN\n   */\n  private DiagnosisResult applyFailedDiagnosis(\n      Long taskID, String taskStatus, Integer errorCode, String errorDesc,\n      JobHistory jobHistory, Map<String, Object> metricsMap) {\n\n    String stage = determineFailedStage(metricsMap);\n    String keywordResult = applyKeywordRule(errorDesc);\n\n    // UNDERLYING keywords override stage\n    if ("UNDERLYING".equals(keywordResult)) {\n      String engineInstance = metricsMap != null ? (String) metricsMap.get("engineInstance") : null;\n      String engineType = jobHistory.getEngineType();\n      Object yarnResource = metricsMap != null ? metricsMap.get("yarnResource") : null;\n      logger.info("Task {} classified as UNDERLYING by keyword rule at {} stage", taskID, stage);\n      return DiagnosisResult.underlying(\n          taskID, taskStatus, stage, errorCode, errorDesc,\n          engineType, engineInstance, yarnResource);\n    }\n\n    // Scheduling stage: Linkis keyword → errorCode → default LINKIS\n    if ("Scheduling".equals(stage)) {\n      if ("LINKIS".equals(keywordResult)) {\n        logger.info("Task {} classified as LINKIS by keyword rule at Scheduling stage", taskID);\n        return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);\n      }\n      String errorCodeResult = applyErrorCodeRule(errorCode);\n      if ("LINKIS".equals(errorCodeResult)) {\n        logLinkisErrorCode(taskID, errorCode, "Scheduling");\n        return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);\n      }\n      logger.info("Task {} classified as LINKIS at Scheduling stage (default)", taskID);\n      return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);\n    }\n\n    // Execution stage: errorCode → Linkis keyword → default UNKNOWN\n    String errorCodeResult = applyErrorCodeRule(errorCode);\n    if (errorCodeResult != null) {\n      if ("LINKIS".equals(errorCodeResult)) {\n        logLinkisErrorCode(taskID, errorCode, "Execution");\n        return DiagnosisResult.linkis(taskID, taskStatus, "Execution", errorCode, errorDesc);\n      } else {\n        String engineInstance = metricsMap != null ? (String) metricsMap.get("engineInstance") : null;\n        String engineType = jobHistory.getEngineType();\n        Object yarnResource = metricsMap != null ? metricsMap.get("yarnResource") : null;\n        logger.info("Task {} classified as UNDERLYING by error code range {} at Execution stage", taskID, errorCode);\n        return DiagnosisResult.underlying(\n            taskID, taskStatus, "Execution", errorCode, errorDesc,\n            engineType, engineInstance, yarnResource);\n      }\n    }\n\n    if ("LINKIS".equals(keywordResult)) {\n      logger.info("Task {} classified as LINKIS by keyword rule at Execution stage", taskID);\n      return DiagnosisResult.linkis(taskID, taskStatus, "Execution", errorCode, errorDesc);\n    }\n\n    logger.info("Task {} classified as UNKNOWN at Execution stage (no matching rule)", taskID);\n    return DiagnosisResult.unknown(taskID, taskStatus, "Execution", errorCode, errorDesc);\n  }\n\n  private void logLinkisErrorCode(Long taskID, Integer errorCode, String stage) {\n    String[] moduleInfo = LINKIS_ERROR_CODE_MAP.get(errorCode);\n    String module = moduleInfo != null ? moduleInfo[0] : "unknown";\n    String reason = moduleInfo != null ? moduleInfo[1] : "Linkis error code: " + errorCode;\n    logger.info("Task {} classified as LINKIS by error code {} at {} stage, module: {}, reason: {}",\n        taskID, errorCode, stage, module, reason);\n  }\n\n  /**\n   * Determine which stage the failed task reached based on metrics.\n   * 1. yarnResource with applicationId → Execution (strongest signal)\n   * 2. engineInstance + jobToECTime → Execution\n   * 3. engineInstance only → Scheduling\n   * 4. No engineInstance → Scheduling\n   */\n  private String determineFailedStage(Map<String, Object> metricsMap) {\n    if (metricsMap == null) {\n      return "Scheduling";\n    }\n    String applicationId = extractApplicationId(metricsMap);\n    if (applicationId != null) {\n      return "Execution";\n    }\n    Object engineInstance = metricsMap.get("engineInstance");\n    if (engineInstance == null || StringUtils.isBlank(engineInstance.toString())) {\n      return "Scheduling";\n    }\n    Object jobToECTime = metricsMap.get("jobToECTime");\n    if (jobToECTime == null) {\n      return "Scheduling";\n    }\n    return "Execution";\n  }\n\n  /**', 'is_error': False}]

**AI**：
我来查看一下 `service` 的实现，以充分了解该功能。...


## 交互记录 (2026-08-21T07:33:13.334Z)

**用户**：
[{'tool_use_id': 'call_482f3306b7b84de69bc77be9', 'type': 'tool_result', 'content': '/*\n * Licensed to the Apache Software Foundation (ASF) under one or more\n * contributor license agreements.  See the NOTICE file distributed with\n * this work for additional information regarding copyright ownership.\n * The ASF licenses this file to You under the Apache License, Version 2.0\n * (the "License"); you may not use this file except in compliance with\n * the License.  You may obtain a copy of the License at\n *\n *    http://www.apache.org/licenses/LICENSE-2.0\n *\n * Unless required by applicable law or agreed to in writing, software\n * distributed under the License is distributed on an "AS IS" BASIS,\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n * See the License for the specific language governing permissions and\n * limitations under the License.\n */\n\npackage org.apache.linkis.jobhistory.service;\n\nimport org.apache.linkis.jobhistory.entity.DiagnosisResult;\n\npublic interface TaskDiagnosisService {\n\n  /**\n   * Classify a failed task by diagnosis type and return detailed diagnosis info.\n   *\n   * @param taskID the task ID to diagnose\n   * @param username the requesting user for permission check\n   * @return diagnosis result with classification details\n   */\n  DiagnosisResult classify(Long taskID, String username);\n}', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:33:39.580Z)

**用户**：
[{'tool_use_id': 'call_b1d37700e07d4098a683adb8', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] Now I have a comprehensive understanding of the changes:

**Feature**: Task failure classification diagnosis (任务失败分类诊断 / AI ops diagnosis)

The feature:
1. Adds a new RESTful endpoint `/task-diagnosis` to `QueryRestfulApi` that classifies failed tasks by diagnosis type
2. Classifies failures into categories: `LINKIS` (Linkis framework issue), `UNDERLYING` (underlying engine/resource issue), `UNKNOWN`, plus `SUCCESS` for successful tasks and `DISABLED` when the feature is off
3. Classifies by stage: `Scheduling` vs `Execution`
4. Uses error code ranges, keyword matching (configurable), and metrics parsing to determine the diagnosis type
5. Has a feature switch `linkis.task.classified.diagnosis.enable` (default false) - follows the project's iron rule (§4.1)
6. Uses `getHotValue()` for hot-reload of the switch (good - can disable without restart)
7. Has try/catch protection in the REST API
8. New files: `DiagnosisResult.java` (entity), `TaskDiagnosisService.java` (interface), `TaskDiagnosisServiceImpl.java` (impl), `TaskDiagnosisConfiguration.scala` (config)
9. Docs: design doc + requirements doc

**Module identification**:
- All code changes in `linkis-public-enhancements/linkis-jobhistory/`
- One-level: `PES` (public-enhancements)
- Two-level: `jobhistory`

**Action type**: `feat` - new feature (task diagnosis classification)

**No tests were added** in this change (no test files in the diff).

Now Step 4: Search GitHub Issues. Let me search for related issues....


## 交互记录 (2026-08-21T07:33:45.558Z)

**用户**：
[{'tool_use_id': 'call_c46efc4dd84446b5b343f673', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
我现在已经清楚地了解了该功能。让我进行 **第 4 步：搜索 GitHub Issues**。

该功能是 **任务失败分类诊断** —— 它添加了一个 `/task-diagnosis` 端点，用于将失败的任务分类为 `LINKIS` / `UNDERLYING` / `UNKNOWN` 类型，并按阶段（调度/执行）进行分类，通过错误代码范围和关键字匹配来确定。所有变更均位于 `linkis-public-enhancements/linkis-jobhistory/` 中 → 模块 **PES / jobhistory**，操作类型为 **feat**。

让我搜索现有的相关 Issues。...


## 交互记录 (2026-08-21T07:33:50.182Z)

**用户**：
[{'tool_use_id': 'call_b07c7fb808154fe7ad8c89f3', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:34:00.503Z)

**用户**：
[{'tool_use_id': 'call_e937008c16f34bd8bbb11c1d', 'type': 'tool_result', 'content': "github.com\n  ✓ Logged in to github.com account v-kkhuang (keyring)\n  - Active account: true\n  - Git operations protocol: https\n  - Token: gho_************************************\n  - Token scopes: 'gist', 'read:org', 'repo', 'workflow'\n---EXIT:0---", 'is_error': False}]

**AI**：
[思考] No output - that's strange. Let me check if `gh` is logged in and works. The results show no output, which could mean no matching issues, or gh is not working. Let me verify gh auth status and try a simpler query....


## 交互记录 (2026-08-21T07:34:06.451Z)

**用户**：
[{'tool_use_id': 'call_dd6276745c254453904c25e3', 'type': 'tool_result', 'content': '975\tOPEN\t[feat][ETS][monitor] optimize monitor module with log cleanup and connection pool expansion\tenhancement\t2026-04-07T11:08:26Z\n---EXIT:0---', 'is_error': False}]

**AI**：
没有返回结果。让我验证一下 `gh` 是否工作正常，并尝试进行更广泛的搜索。...


## 交互记录 (2026-08-21T07:34:16.066Z)

**用户**：
[{'tool_use_id': 'call_55511832abab44ceb8e8f3cf', 'type': 'tool_result', 'content': '---EXIT:0---', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:34:20.106Z)

**用户**：
[{'tool_use_id': 'call_19f0d494bb8c43e7b849d012', 'type': 'tool_result', 'content': '---EXIT:0---', 'is_error': False}]

**AI**：
[思考] `gh` works. The searches for "diagnosis", "task diagnosis", "PES jobhistory diagnosis", "jobhistory failure classify" returned no relevant issues. The only result was an unrelated issue #975 about ETS monitor.

Let me search a bit more broadly to be sure there's no related issue about task failure classification or jobhistory diagnosis....


## 交互记录 (2026-08-21T07:34:24.205Z)

**用户**：
[{'tool_use_id': 'call_9cb35cd26e26458791ba93e9', 'type': 'tool_result', 'content': '---EXIT:0---', 'is_error': False}]

**AI**：
`gh` 运行正常。我来进行几次更广泛的搜索，以确保没有相关的 Issue。...


## 交互记录 (2026-08-21T07:34:52.081Z)

**用户**：
[{'tool_use_id': 'call_e69bb714a6914a009dee8e27', 'type': 'tool_result', 'content': 'origin\tgit@github.com:WeDataSphere/linkis.git (fetch)\norigin\tgit@github.com:WeDataSphere/linkis.git (push)\n---\nWeDataSphere/linkis\n---EXIT:0---', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:36:22.199Z)

**用户**：
[{'tool_use_id': 'call_5494b3d4bb544161b9f1d88e', 'type': 'tool_result', 'content': '71:@RestController\n72:@RequestMapping(path = "/jobhistory")\n73:public class QueryRestfulApi {\n90:  @RequestMapping(path = "/governanceStationAdmin", method = RequestMethod.GET)\n111:  @RequestMapping(path = "/{id}/get", method = RequestMethod.GET)\n182:  @RequestMapping(path = "/list", method = RequestMethod.GET)\n263:  @RequestMapping(path = "/listundonetasks", method = RequestMethod.GET)\n376:  @RequestMapping(path = "/listundone", method = RequestMethod.GET)\n423:  @RequestMapping(path = "/list-taskids", method = RequestMethod.GET)\n457:  @RequestMapping(path = "/job-extra-info", method = RequestMethod.GET)', 'is_error': False}]

**AI**：
[思考] No related issues found at all. So per the workflow, I should proceed to create a new Issue. But wait, the skill says:

"#### 如果没有找到相关 Issues
**直接进入创建新 Issue 流程**（Step 6）"

So since no related issues were found, I go directly to creating a new Issue. The skill rule #6 says "找到相关 Issues 时必须询问用户选择，不能自动决定" - meaning I MUST ask the user when related issues ARE found. But when none are found, I proceed directly to creating a new issue.

Actually, let me re-read. The rule says "必须询问: 找到相关 Issues 时必须询问用户选择，不能自动决定。" This means: when related issues are found, must ask the user. Since I found NONE, I proceed directly to creating a new issue.

However, given this is a fairly consequential step (creating an issue + PR), and the skill is clear about the flow, let me proceed. But I want to confirm a couple things with the user before creating. Actually, the skill is explicit about the workflow and says not to ask when no issues found — proceed to create.

But there's an important consideration: this is an internal fork/workflow. The base branch is `dev-2.1.2-webank` — this is a WebBank internal branch, not the apache/master. Let me check the remote to understand the repo context. Actually, `gh` is logged in as `v-kkhuang` and the token has `repo` scope. Let me check what repo this is.

Actually, the skill is clear: no related issues found → create new issue. Let me follow the workflow.

But wait — I should consider whether the user wants me to actually create the issue on the apache/linkis repo, or whether this repo is a fork. Let me check the git remote to understand. Actually the CLAUDE.md says repo is https://github.com/apache/linkis. But the branch `dev-2.1.2-webank` suggests this might be a fork. Let me check the remote.

Let me verify the remote and the repo that gh will operate on. This matters because creating an issue on apache/linkis (the upstream) vs a fork is very different — and outward-facing actions need confirmation.

Actually, given the rule about confirming...


## 交互记录 (2026-08-21T07:37:05.435Z)

**用户**：
[{'tool_use_id': 'call_43191b8d98d64c43852c96ad', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\.tmp_issue_body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
未找到相关的 Issue。让我检查一下 git remote，以了解将影响哪个仓库。...


## 交互记录 (2026-08-21T07:37:18.007Z)

**用户**：
[{'tool_use_id': 'call_187e6ac6d15946c582851718', 'type': 'tool_result', 'content': "could not add label: 'status: needs triage' not found\n---EXIT:1---", 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:37:25.977Z)

**用户**：
[{'tool_use_id': 'call_0ff1d005151d4ed6b2ffb7a7', 'type': 'tool_result', 'content': "bug\tSomething isn't working\t#d73a4a\ndocumentation\tImprovements or additions to documentation\t#0075ca\nduplicate\tThis issue or pull request already exists\t#cfd3d7\nenhancement\tNew feature or request\t#a2eeef\ngood first issue\tGood for newcomers\t#7057ff\nhelp wanted\tExtra attention is needed\t#008672\ninvalid\tThis doesn't seem right\t#e4e669\nquestion\tFurther information is requested\t#d876e3\nwontfix\tThis will not be worked on\t#ffffff", 'is_error': False}]

**AI**：
[思考] The remote is `WeDataSphere/linkis` (a fork of apache/linkis, used by WeBank's WeDataSphere). The default gh repo is `WeDataSphere/linkis`. So issues and PRs will be created on the `WeDataSphere/linkis` repo, which is the correct target since the base branch `dev-2.1.2-webank` is a WebBank/WeDataSphere branch.

This is the user's internal fork. Creating issues and PRs here is the expected workflow. 

Now, per the workflow:
- No related issues found → proceed to Step 6: create new issue
- Issue type: `feature` (since PR action is `feat`)
- Issue title format: `[feature][PES][jobhistory] <description>`
- Then Step 7: PR title `[feat][PES][jobhistory] <description>`
- Step 8: PR description (three-paragraph + brief change log + checklist)
- Step 9: create PR with `close #<issue_id>`

Let me think about the description for the issue and PR title.

The feature: Task failure classification diagnosis. It classifies failed tasks into:
- LINKIS: Linkis framework issue (scheduling/entrance/engineconn errors with specific error codes or keywords)
- UNDERLYING: underlying engine/resource issue (YARN kill, OOM, file not found, connection refused, etc.)
- UNKNOWN: unmatched at execution stage
- SUCCESS: task succeeded

It determines the stage (Scheduling vs Execution) based on metrics, then applies rules (error code ranges, keyword matching).

PR/Issue title description (lowercase sentence):
"add task failure classification diagnosis api for jobhistory"
or
"classify failed task diagnosis type and return detailed info"

Let me make it clear and descriptive:
- Issue title: `[feature][PES][jobhistory] add task failure classification diagnosis api to classify failed tasks by linkis or underlying cause`
- PR title: `[feat][PES][jobhistory] add task failure classification diagnosis api to classify failed tasks by linkis or underlying cause`

Hmm, that's a bit long. Let me simplify:
- `[feat][PES][jobhistory] add task failure classification diagnosis api`

Actually let me make it m...


## 交互记录 (2026-08-21T07:37:39.289Z)

**用户**：
[{'tool_use_id': 'call_1021d38cecf84f73a9219fbd', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/issues/1094\n---EXIT:0---', 'is_error': False}]

**AI**：
远程仓库是 `WeDataSphere/linkis`（默认的 gh 仓库），且未找到相关 issue。按照工作流程，我将继续创建一个新 issue。让我先确认一下 API 接口路径以确保准确性。...


## 交互记录 (2026-08-21T07:38:16.347Z)

**用户**：
[{'tool_use_id': 'call_aae1686dc5c047f38bcea94a', 'type': 'tool_result', 'content': '2fe0badae0dd3c8d238d22f2a75c5b2d255b1e5d\trefs/heads/dev-2.1.2-task-diagnosis\n48df77cf58e4c8d683b33e33caffba1a1b8bd7d5\trefs/heads/dev-2.1.2-webank\n---EXIT:0---', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-21T07:38:31.729Z)

**用户**：
[{'tool_use_id': 'call_63be569331854da08e710fd5', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\.tmp_pr_body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
[思考] The endpoint is `GET /api/rest_j/v1/jobhistory/task-diagnosis` (the `/jobhistory` class-level + `/task-diagnosis` method-level). 

Now I have all the info. Let me create the issue first.

I'll write the issue body to a file to avoid shell-escaping issues with the multi-line markdown content (the skill's memory note about `--content "$(cat file)"` is for dpms, but for gh issue create with --body, I can use a file via `--body-file`). Actually `gh issue create` supports `--body-file <path>`. Let me use that to avoid escaping issues.

Let me create the issue body file first, then create the issue.

Actually, I can use `gh issue create --title "..." --body-file <file>`. Let me write the body to a temp file.

Let me prepare the issue body. I'll write it to a temp file.

Issue title: `[feature][PES][jobhistory] add task-diagnosis api to classify failed tasks into linkis or underlying cause`

Let me write the issue body file....


## 交互记录 (2026-08-21T07:38:49.076Z)

**用户**：
[{'tool_use_id': 'call_f2147609175a4b0a9346b024', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/pull/1095\n---EXIT:0---', 'is_error': False}]

**AI**：
该端点为 `GET /api/rest_j/v1/jobhistory/task-diagnosis?taskID=xxx`。我现在已准备好所需的一切。未找到相关 Issue，因此按照工作流程，我将创建一个新 Issue。让我将 Issue 正文写入文件（以避免转义问题）并创建 Issue。...


## 交互记录 (2026-08-21T07:38:55.828Z)

**用户**：
[{'tool_use_id': 'call_2b0e6a6e757b47d8be8b3982', 'type': 'tool_result', 'content': 'cleaned', 'is_error': False}]

**AI**：
...

