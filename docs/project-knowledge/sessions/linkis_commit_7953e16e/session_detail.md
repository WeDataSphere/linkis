
## 交互记录 (2026-06-23T09:13:36.355Z)

**用户**：
<command-message>pr</command-message>
<command-name>/pr</command-name>
<command-args>dev-2.0.0-sso-login 提交到dev-2.0.0</command-args>

**AI**：
[思考] 用户使用了 `/pr` 命令，参数是 `dev-2.0.0-sso-login 提交到dev-2.0.0`。

根据 PR skill 的文档，我需要：
1. 先搜索/创建 GitHub Issue
2. 然后才能创建 PR
3. 必须询问分支信息并确认是创建 PR
4. 分析代码变更
5. 生成符合规范的 PR 标题和描述

从用户输入来看：
- 目标分支（head）: dev-2.0.0-sso-login
- 基础分支（base）: dev-2.0.0

但是按照规范，我需要先询问用户确认这些信息，并确认这是用于创建 Pull Request。

让我按照步骤来：

Step 1: 询问分支信息（虽然用户已经提供了，但我应该确认）...


## 交互记录 (2026-06-23T09:13:36.355Z)

**用户**：
[{'type': 'text', 'text': 'Base directory for this skill: C:\\Users\\v_kkhuang\\.claude\\skills\\pr\n\n# GitHub PR 自动创建器 (Apache Linkis 版)\n\n## ⚠️ 强制要求（MUST READ）\n\n**执行此技能时，必须严格遵守以下规则，违反任何一项都将导致输出不合格：**\n\n1. **Issue 处理优先**: 必须先搜索/创建 Issue，然后才能创建 PR。绝不能跳过 Issue 步骤直接创建 PR。\n2. **标题格式**: PR 标题和 Issue 标题必须严格遵循格式，大小写不能错。\n3. **内容全英文**: PR 和 Issue 的所有内容必须是英文，不能有中文。\n4. **三段式描述**: PR 的 "What is the purpose" 必须使用三段式格式（Background/Problem, Purpose, Value/Impact）。\n5. **Issue 关联**: PR 创建时必须关联 Issue（使用 `close #issue_id`）。\n6. **必须询问**: 找到相关 Issues 时必须询问用户选择，不能自动决定。\n7. **不允许 commit**: 只创建 PR 和 Issue，不执行任何 git commit 操作。\n\n---\n\n## 工作流程（按顺序执行）\n\n```\n┌─────────────────────────────────────────────────────────────────────────────┐\n│                        Apache Linkis PR 创建流程                              │\n├─────────────────────────────────────────────────────────────────────────────┤\n│  Step 1: 询问分支信息（base + target）                                        │\n│     ↓                                                                        │\n│  Step 2: 获取代码变更（git diff + git log）                                   │\n│     ↓                                                                        │\n│  Step 3: 分析变更，识别模块（一级 + 二级）                                    │\n│     ↓                                                                        │\n│  Step 4: 搜索 GitHub Issues（gh issue list）                                 │\n│     ↓                                                                        │\n│  Step 5: 询问用户选择 Issue（复用现有 OR 创建新 Issue）                       │\n│     ↓                                                                        │\n│  Step 6: 如果需要，创建新 Issue（gh issue create）                            │\n│     ↓                                                                        │\n│  Step 7: 生成 PR 标题（严格格式）                                              │\n│     ↓                                                                        │\n│  Step 8: 生成 PR 描述（三段式 + Brief change log + Checklist）                │\n│     ↓                                                                        │\n│  Step 9: 创建 PR 并关联 Issue（gh pr create）                                 │\n└─────────────────────────────────────────────────────────────────────────────┘\n```\n\n---\n\n## 执行步骤详解\n\n### Step 1: 询问分支信息\n\n**必须询问用户以下信息：**\n\n```\n请输入以下信息以创建 Pull Request：\n\n1. 基础分支（base branch）: 即要合并的目标分支（例如: apachemaster, dev-1.19.0）\n2. 目标分支（target branch）: 即你的特性分支（例如: dev-1.19.0-feature-xxx）\n\n请确认这是用于创建 Pull Request，而非其他操作。\n```\n\n**验证规则**：\n- 必须确认用户意图是创建 PR\n- 如果分支不存在，列出可用分支供用户选择\n- 使用 `git branch -r` 列出远程分支\n\n---\n\n### Step 2: 获取代码变更\n\n**执行以下命令获取变更信息：**\n\n```bash\n# 获取变更文件列表\ngit diff --name-status <base_branch>..<target_branch>\n\n# 获取提交历史\ngit log <base_branch>..<target_branch> --oneline --no-merges\n\n# 获取详细的代码差异（用于分析）\ngit diff <base_branch>..<target_branch> --stat\n```\n\n**分析内容**：\n- 变更的文件数量和类型\n- 变更的代码行数\n- 提交的数量和主题\n\n---\n\n### Step 3: 分析变更，识别模块\n\n**从变更文件路径中提取模块信息：**\n\n#### 一级模块映射表（严格使用）\n\n| 目录名 | 缩写 | 全称 |\n|--------|------|------|\n| `linkis-spring-cloud-services` | `MGS` | microservice-governance |\n| `linkis-computation-governance` | `CGS` | computation-governance |\n| `linkis-public-enhancements` | `PES` | public-enhancements |\n| `linkis-engineconn-plugins` | `EC` | engineconn |\n| `linkis-extensions` | `ETS` | extensions |\n| `linkis-orchestrator` | `ORC` | orchestrator |\n| `linkis-web` | `WEB` | web |\n| `linkis-commons` | `COM` | commons |\n\n#### 二级模块提取规则\n\n| 一级模块 | 二级模块提取方式 | 示例 |\n|---------|-----------------|------|\n| MGS | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-gateway` → `gateway` |\n| CGS | `linkis-cg-xxx` 中的 xxx 部分（小写）或 `linkis-xxx` 去掉前缀 | `linkis-entrance` → `entrance` |\n| PES | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-datasource` → `datasource` |\n| EC | 引擎名称（小写） | `spark` → `spark` |\n| WEB | 通常无二级模块，使用 `frontend` 或省略 | - |\n| COM | 目录名（小写） | `common` → `common` |\n\n**模块识别逻辑**：\n1. 如果所有变更文件在同一模块，使用该模块\n2. 如果跨越多个模块，选择变更最多的模块作为主模块\n3. 如果变更分散且数量相近，询问用户确认主模块\n\n---\n\n### Step 4: 搜索 GitHub Issues\n\n**必须执行 Issue 搜索，不能跳过：**\n\n```bash\n# 搜索 open issues，使用模块和变更关键词\ngh issue list --state open --search "<模块名> <关键词>" --limit 20\n\n# 例如：\n# gh issue list --state open --search "PES datasource filter" --limit 20\n# gh issue list --state open --search "CGS entrance location" --limit 20\n```\n\n**搜索关键词提取**：\n- 模块名（一级 + 二级）\n- 变更相关的关键词（从文件名、提交信息中提取）\n- action 类型（feat/fix 等）\n\n---\n\n### Step 5: 询问用户选择 Issue\n\n#### 如果找到相关 Issues（至少 1 个）\n\n**必须询问用户，不能自动选择：**\n\n```\n找到以下相关的 open issues：\n\n#123: [bug][PES][datasource] add support for getting published datasource by type and proxy user\n#456: [feature][CGS][entrance] hive location clause security check\n\n请选择：\n1. 使用现有 issue #123\n2. 使用现有 issue #456\n3. 创建新 issue\n4. 手动输入其他 issue 编号\n\n请输入选项 (1/2/3/4) 或 issue 编号:\n```\n\n#### 如果没有找到相关 Issues\n\n**直接进入创建新 Issue 流程**（Step 6）\n\n---\n\n### Step 6: 创建新 Issue（如需要）\n\n**如果用户选择创建新 Issue，或没有找到相关 Issue，则执行以下步骤：**\n\n#### Issue 标题格式（严格遵循）\n\n```\n[类型][一级模块][二级模块] 功能描述\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 示例 |\n|------|------|------|\n| `类型` | **小写** | `bug`, `feature`, `ask` |\n| `一级模块` | **大写** | `MGS`, `CGS`, `PES`, `EC`, `ETS`, `ORC`, `WEB`, `COM` |\n| `二级模块` | **小写** | `entrance`, `datasource`, `spark` 等 |\n| `功能描述` | **小写句子** | 首字母不大写，使用普通英文句子 |\n\n**类型选择规则**：\n- PR action 为 `fix` → Issue 类型为 `bug`\n- PR action 为 `feat` → Issue 类型为 `feature`\n- 其他情况 → Issue 类型为 `feature`\n\n**Issue 标题示例**：\n```\n✅ 正确: [bug][CGS][entrance] hive tasks should not be allowed to use location clause for security reasons\n✅ 正确: [feature][PES][datasource] add support for getting published datasource by type and proxy user\n❌ 错误: [Bug][CGS][Entrance] Hive Tasks Should Not Be Allowed... (大小写错误)\n❌ 错误: [bug][cgs][entrance] ... (一级模块小写错误)\n```\n\n#### Issue 内容格式（严格遵循）\n\n**根据 Issue 类型选择对应模板：**\n\n##### Bug Report 模板（fix 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar issues.\n\n### Linkis Component\n\n- [ ] linkis-commons\n- [ ] linkis-computation-governance\n- [ ] linkis-dist\n- [ ] linkis-engineconn-plugin\n- [ ] linkis-extensions\n- [ ] linkis-orchestrator\n- [x] linkis-public-enhancements\n- [ ] linkis-spring-cloud-services\n- [ ] linkis-web\n\n### Description\n\n[Brief description of the bug - what happened and what should happen]\n\n### Steps to reproduce\n\n1. Step 1...\n2. Step 2...\n3. Step 3...\n\n### Expected behavior\n\n[Describe what you expected to happen]\n\n### Your environment\n\n- Linkis version used: [x.x.x]\n- Environment name and version:\n    - hadoop-x.x.x\n    - hive-x.x.x\n    - spark-x.x.x\n    - scala-x.x.x\n    - jdk 1.8.x_xx\n\n### Anything else\n\n[Additional context, logs, frequency of occurrence, etc.]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n##### Feature Request 模板（feat 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar feature requirement.\n\n### Problem Description\n\nIs your feature request related to a problem? Please describe.\n\n[Describe the problem or need]\n\n### Description\n\nA short description of your feature\n\n[Clear and concise description of what you want to add]\n\n### Use case\n\nDescribe the use case of your feature request and the solution you\'d like.\n\n[Describe the specific use case and expected behavior]\n\n### Solutions\n\nDescribe alternatives you\'ve considered.\n\n[Describe any alternative solutions or workarounds you\'ve considered]\n\n### Anything else\n\nAdditional context or screenshots about the feature request.\n\n[Any additional information, screenshots, or examples]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n#### Issue 标签规则\n\n| Issue 类型 | 标签 |\n|-----------|------|\n| `bug` | `bug`, `status: needs triage` |\n| `feature` | `enhancement`, `status: needs triage` |\n| `ask` | `question`, `status: needs triage` |\n\n#### 创建 Issue 命令\n\n```bash\ngh issue create \\\n  --title "[<type>][<MODULE>][<submodule>] <description>" \\\n  --body "<issue_content>" \\\n  --label "<label1>,<label2>"\n```\n\n**保存返回的 issue_id，用于 PR 关联！**\n\n---\n\n### Step 7: 生成 PR 标题\n\n**PR 标题格式（严格遵循）**：\n\n```\n[action][一级模块][二级模块] 一句话总结\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 可选值 | 示例 |\n|------|------|--------|------|\n| `action` | **小写** | feat, fix, perf, refactor, docs, style, test, chore | `feat`, `fix` |\n| `一级模块` | **大写** | MGS, CGS, PES, EC, ETS, ORC, WEB, COM | `PES`, `CGS` |\n| `二级模块` | **小写** | 具体服务名称 | `datasource`, `entrance` |\n| `一句话总结` | **小写句子** | 普通英文，首字母不大写 | `add support for filtering by type` |\n\n**Action 类型定义**：\n\n| Action | 含义 | 使用场景 |\n|--------|------|----------|\n| `feat` | 新功能 | 添加新的功能、API、模块 |\n| `fix` | 修复 Bug | 修复问题、缺陷 |\n| `perf` | 性能优化 | 提升性能、优化资源使用 |\n| `refactor` | 重构 | 代码重构，不改变功能 |\n| `docs` | 文档 | 仅文档变更 |\n| `style` | 代码风格 | 格式化、代码风格调整 |\n| `test` | 测试 | 仅测试相关变更 |\n| `chore` | 构建/工具 | 构建、依赖、工具链更新 |\n\n**PR 标题示例**：\n\n```\n✅ 正确: [feat][PES][datasource] add support for getting published datasource by type and proxy user\n✅ 正确: [fix][CGS][entrance] resolve null pointer exception in engine reuse metrics update\n✅ 正确: [perf][CGS][application-manager] optimize engine creation performance\n❌ 错误: [Feat][PES][datasource] Add Support For... (大小写错误)\n❌ 错误: [feat][pES][datasource] ... (一级模块大小写混合)\n❌ 错误: feat: add support for... (缺少模块标签)\n```\n\n---\n\n### Step 8: 生成 PR 描述\n\n**PR 描述必须严格遵循以下格式：**\n\n#### PR 描述模板（完整格式）\n\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\n[Describe the background or problem - what issue exists, 2-3 sentences]\n\n**Purpose of Change:**\n[Describe the purpose of this PR - what this PR does to solve the problem, 2-3 sentences]\n\n**Value/Impact:**\n[Describe the value or impact - what benefits this change brings, 1-2 sentences]\n\n### Related issues/PRs\n\nRelated issues: close #<issue_id>\nRelated pr:none\n\n### Brief change log\n\n- [Change point 1 - start with verb]\n- [Change point 2 - start with verb]\n- [Change point 3 - start with verb]\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n#### "What is the purpose" 格式要求（严格三段式）\n\n**必须使用三段式结构，不能合并或省略：**\n\n| 段落 | 标题 | 内容要求 | 长度 |\n|------|------|----------|------|\n| 第一段 | **Background/Problem:** | 描述背景或问题 - 当前存在什么问题或需求 | 2-3 句话 |\n| 第二段 | **Purpose of Change:** | 描述改动的目的 - 本 PR 如何解决这个问题 | 2-3 句话 |\n| 第三段 | **Value/Impact:** | 描述价值或影响 - 改动带来的好处 | 1-2 句话 |\n\n**格式规范**：\n- ✅ 每段标题使用粗体，以冒号结尾\n- ✅ 段落标题使用 `**Title:**` 格式\n- ✅ 内容另起一行，不要和标题在同一行\n- ❌ 不要使用 "This PR adds..." 或 "This PR implements..." 格式\n- ✅ 使用客观描述，如 "This PR adds...", "The change enables..."\n\n**示例**：\n\n```\n✅ 正确格式:\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n❌ 错误格式:\n\nThis PR adds support for filtering datasources by type and user. (缺少三段式)\nThe problem is that users cannot filter datasources. (不完整)\n```\n\n#### "Brief change log" 格式要求\n\n**每条变更记录必须：**\n- 以动词开头\n- 使用现在时\n- 简洁明确\n- 每条独立一行\n\n**常用动词示例**：\n- `Add` - 添加新功能\n- `Fix` - 修复问题\n- `Update` - 更新现有功能\n- `Remove` - 删除功能\n- `Refactor` - 重构代码\n- `Optimize` - 优化性能\n- `Improve` - 改进\n\n**示例**：\n\n```\n✅ 正确:\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n❌ 错误:\n- Added API... (过去时)\n- Update the service... (不明确)\n```\n\n#### Checklist 智能勾选规则\n\n| 选项 | 何时勾选 | 说明 |\n|------|---------|------|\n| I have read the Contributing Guidelines | ✅ 始终勾选 | - |\n| I have explained the need for this PR | ✅ 始终勾选 | - |\n| I have explained the changes | ✅ 有代码变更时勾选 | 大多数情况勾选 |\n| I have added tests | 有测试时勾选 | 如果添加了测试代码 |\n| I have updated the documentation | 有文档变更时勾选 | 如果修改了文档 |\n| I have verified backward compatibility | ✅ 通常勾选 | 除非是破坏性变更 |\n| I have written unit tests | 有完整测试时勾选 | 如果有完整的单元测试 |\n\n---\n\n### Step 9: 创建 PR\n\n**执行 PR 创建命令：**\n\n```bash\ngh pr create \\\n  --title "<PR_TITLE>" \\\n  --body "<PR_BODY>" \\\n  --base "<base_branch>" \\\n  --head "<target_branch>" \\\n  --label "<label1>,<label2>"\n```\n\n**PR 标签映射规则**：\n\n| Action 类型 | Label |\n|-----------|-------|\n| `feat` | `enhancement` |\n| `fix` | `bug` |\n| `perf` | `performance` |\n| `refactor` | `refactor` |\n| `docs` | `documentation` |\n| `style` | `style` |\n| `test` | `test` |\n| `chore` | `build` |\n\n---\n\n## 错误处理\n\n| 错误场景 | 处理方式 |\n|---------|----------|\n| 找不到 git 仓库 | 提示用户在正确的项目目录中执行 |\n| gh 未登录 | 提示用户运行 `gh auth login` |\n| 分支不存在 | 列出可用分支供用户选择 |\n| 未找到代码变更 | 提示用户检查分支是否正确 |\n| Issue 搜索失败 | 提示用户手动关联 Issue |\n| PR 创建失败 | 显示错误信息，提示用户检查权限或网络 |\n\n---\n\n## 完整示例\n\n### 示例 1: 新增功能（创建新 Issue）\n\n**场景**: 添加数据源过滤功能，无相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-public-enhancements/linkis-datasource/src/...`\n- 模块: `PES` / `datasource`\n- Action: `feat`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "PES datasource filter" --limit 20\n```\n结果: 无相关 Issues\n\n**Step 6**: 创建新 Issue\n```bash\ngh issue create \\\n  --title "[feature][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --label "enhancement,status: needs triage"\n```\n返回: Issue #789\n\n**Step 7**: 生成 PR 标题\n```\n[feat][PES][datasource] add support for getting published datasource by type and proxy user\n```\n\n**Step 8**: 生成 PR 描述\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n### Related issues/PRs\n\nRelated issues: close #789\nRelated pr:none\n\n### Brief change log\n\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[feat][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --base "apachemaster" \\\n  --head "dev-1.19.0-feature-datasource-filter" \\\n  --label "enhancement"\n```\n\n---\n\n### 示例 2: 修复 Bug（复用现有 Issue）\n\n**场景**: 修复空指针异常，找到相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-computation-governance/linkis-cg-linkismanager/src/...`\n- 模块: `CGS` / `linkismanager` (简化为 `application-manager`)\n- Action: `fix`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "CGS application-manager null pointer" --limit 20\n```\n结果: 找到 Issue #456\n\n**Step 5**: 询问用户\n```\n找到以下相关的 open issues：\n\n#456: [bug][CGS][application-manager] null pointer exception in engine reuse metrics update causing monitoring data loss\n\n请选择：\n1. 使用现有 issue #456\n2. 创建新 issue\n3. 手动输入其他 issue 编号\n```\n用户选择: 1\n\n**Step 6**: 跳过（复用现有 Issue #456）\n\n**Step 7**: 生成 PR 标题\n```\n[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update\n```\n\n**Step 8**: 生成 PR 描述（使用 Bug Report 风格的三段式）\n```markdown\n### What is the purpose of the change\n\n**Background/Problem:**\nDuring engine creation and reuse, when updating metrics information, a null pointer exception occurs, causing some monitoring data to be lost and affecting system stability.\n\n**Purpose of Change:**\nTo address this problem, this PR adds null checks and protection to the relevant code, ensuring the robustness of metrics updates.\n\n**Value/Impact:**\nAfter the change, the system runs stably with complete and available monitoring data, improving system reliability.\n\n### Related issues/PRs\n\nRelated issues: close #456\nRelated pr:none\n\n### Brief change log\n\n- Add null pointer checks in DefaultEngineCreateService\n- Add null pointer checks in DefaultEngineReuseService\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update" \\\n  --body "..." \\\n  --base "dev-1.19.0" \\\n  --head "dev-1.19.0-fix-null-pointer" \\\n  --label "bug"\n```\n\n---\n\n## 快速参考\n\n### 大小写规则速查\n\n| 位置 | PR | Issue |\n|------|-----|-------|\n| 类型/Action | **小写** | **小写** |\n| 一级模块 | **大写** | **大写** |\n| 二级模块 | **小写** | **小写** |\n| 描述/总结 | **小写句子** | **小写句子** |\n\n### 格式速查\n\n```\nPR 标题:    [action][MODULE][submodule] description\nIssue 标题:  [type][MODULE][submodule] description\n\nPR action:   feat, fix, perf, refactor, docs, style, test, chore\nIssue type:  bug, feature, ask\n\nMODULE:      MGS, CGS, PES, EC, ETS, ORC, WEB, COM\n```\n\n### 命令速查\n\n```bash\n# 搜索 Issues\ngh issue list --state open --search "<关键词>" --limit 20\n\n# 创建 Issue\ngh issue create --title "<标题>" --body "<内容>" --label "<标签>"\n\n# 创建 PR\ngh pr create --title "<标题>" --body "<内容>" --base "<base>" --label "<标签>"\n\n# 列出分支\ngit branch -r\n```\n\n---\n\n## 使用方法\n\n```bash\n/pr\n```\n\n然后按照提示输入分支信息即可。\n\n---\n\n## 依赖工具\n\n- `git` - 版本控制\n- `gh` (GitHub CLI) - GitHub 操作\n\n确保 `gh` 已登录：`gh auth login`\n\n\nARGUMENTS: dev-2.0.0-sso-login 提交到dev-2.0.0'}]

**AI**：
我看到您想为 `dev-2.0.0-sso-login` 分支创建 PR 到 `dev-2.0.0`。

在开始之前，让我确认一下信息：

**请确认以下信息以创建 Pull Request：**

- **基础分支**: `dev-2.0.0` (合并的目标分支)
- **目标分支**: `dev-2.0.0-sso-login` (您的特性分支)

请确认这是用于创建 Pull Request，而非其他操作。（输入 "yes" 或 "确认" 继续）...


## 交互记录 (2026-06-23T09:14:13.614Z)

**用户**：
[{'type': 'text', 'text': 'yes'}]

**AI**：
[思考] 用户确认了分支信息，现在我需要：
1. 先获取代码变更（git diff + git log）
2. 分析变更，识别模块
3. 搜索 GitHub Issues
4. 如果需要，创建新 Issue
5. 生成 PR 标题和描述
6. 创建 PR

让我开始执行步骤。...


## 交互记录 (2026-06-23T09:14:39.147Z)

**用户**：
[{'tool_use_id': 'call_6d7dccfb3afa4b5d81f59bde', 'type': 'tool_result', 'content': '7ef086abf #AI commit# 开发阶段：新增sso支持接口', 'is_error': False}]

**AI**：
好的，现在开始分析代码变更并创建 PR。...


## 交互记录 (2026-06-23T09:14:39.610Z)

**用户**：
[{'tool_use_id': 'call_da85a8e5f15142ad94be3d68', 'type': 'tool_result', 'content': 'M\tlinkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala', 'is_error': False}]

**AI**：
[思考] 从变更信息来看：
- 只有1个文件变更：`linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala`
- 只有1个提交：`#AI commit# 开发阶段：新增sso支持接口`
- 变更很小：+2行，-1行

现在我需要：
1. 识别模块：从路径看是 `linkis-spring-cloud-services/linkis-service-gateway`
   - 一级模块：MGS (microservice-governance)
   - 二级模块：gateway
   
2. 获取详细的代码差异来看具体改了什么...


## 交互记录 (2026-06-23T09:14:39.622Z)

**用户**：
[{'tool_use_id': 'call_c3b9e2002c874d79ada721c2', 'type': 'tool_result', 'content': ' .../main/scala/org/apache/linkis/gateway/security/UserRestful.scala    | 3 ++-\n 1 file changed, 2 insertions(+), 1 deletion(-)', 'is_error': False}]

**AI**：
[思考] 现在我理解了变更内容：
- 这是在 `UserRestful.scala` 中添加了对 SSO 登录路径的支持
- 原来只支持 `sso-login` 路径，现在新增支持 `sso/login` 路径
- 这是一个功能增强，添加了新的 SSO 登录路径支持

模块识别：
- 一级模块：MGS (microservice-governance)
- 二级模块：gateway

Action 类型：feat（添加新功能）

现在我需要搜索相关的 GitHub Issues。
现在让我搜索相关的 GitHub Issues：...


## 交互记录 (2026-06-23T09:14:50.062Z)

**用户**：
[{'tool_use_id': 'call_1531afcb086f4d959eef24bf', 'type': 'tool_result', 'content': 'diff --git a/linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala b/linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala\nindex 9ee6889ca..b11730461 100644\n--- a/linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala\n+++ b/linkis-spring-cloud-services/linkis-service-gateway/linkis-gateway-core/src/main/scala/org/apache/linkis/gateway/security/UserRestful.scala\n@@ -68,7 +68,8 @@ abstract class AbstractUserRestful extends UserRestful with Logging {\n \n   override def doUserRequest(gatewayContext: GatewayContext): Unit = {\n     val path = gatewayContext.getRequest.getRequestURI.replace(userRegex, "")\n-    if (StringUtils.isNotBlank(path) && path.startsWith("sso-login")) {\n+    if (StringUtils.isNotBlank(path) &&\n+      (path.startsWith("sso-login") || path.startsWith("sso/login"))) {\n       ssoLogin(gatewayContext)\n       return\n     }', 'is_error': False}]

**AI**：
[思考] 没有找到相关的 open issues。让我再尝试其他搜索关键词。...


## 交互记录 (2026-06-23T09:15:06.122Z)

**用户**：
[{'tool_use_id': 'call_38677c6921c04b70bf686a10', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] 还是没找到。让我再尝试更广泛的搜索。...


## 交互记录 (2026-06-23T09:15:14.133Z)

**用户**：
[{'tool_use_id': 'call_8aa333d3ed1c4e24825cd19f', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] 仍然没有找到相关的 issues。现在我应该询问用户是创建新 issue 还是有现有的 issue 要关联。
没有找到相关的 open issues。

**请选择：**

1. **创建新 issue** - 我将为您创建一个新的 GitHub Issue，然后关联到 PR
2. **手动输入 issue 编号** - 如果您已有相关的 issue，请手动输入编号（格式：#123）

请输入选项 (1/2) 或 issue 编号:...


## 交互记录 (2026-06-23T09:15:53.278Z)

**用户**：
[{'tool_use_id': 'call_36a647d19a4b427185091474', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] 用户选择创建新 issue。现在我需要：

1. 分析变更内容来生成 Issue
2. 从代码差异来看，这是一个小的功能增强：
   - 在 gateway 模块中添加了对 `sso/login` 路径的支持
   - 原来只支持 `sso-login`，现在同时支持 `sso-login` 和 `sso/login`
   - 这是 feat 类型的变更

3. 生成 Issue 标题（严格格式）：
   - 类型：feature（因为这是添加新功能）
   - 一级模块：MGS（大写）
   - 二级模块：gateway（小写）
   - 描述：add support for sso/login path in addition to sso-login

   标题应该是：`[feature][MGS][gateway] add support for sso/login path in addition to sso-login`

4. 生成 Issue 内容（使用 Feature Request 模板）
5. 创建 Issue
6. 生成 PR 标题和描述
7. 创建 PR

让我开始执行。...


## 交互记录 (2026-06-23T09:16:40.539Z)

**用户**：
[{'type': 'text', 'text': '1'}]

**AI**：
好的，我将为您创建一个新的 GitHub Issue。

**分析变更内容：**
- **模块**: MGS (microservice-governance) / gateway
- **变更类型**: feat（功能增强）
- **变更内容**: 在 gateway 模块中添加对 `sso/login` 路径的支持，原有 `sso-login` 路径继续支持

现在创建 Issue：...


## 交互记录 (2026-06-23T09:17:14.864Z)

**用户**：
[{'type': 'tool_result', 'content': "Exit code 1\ncould not add label: 'status: needs triage' not found", 'is_error': True, 'tool_use_id': 'call_b9284940b4cd40f3894417cf'}]

**AI**：
[思考] 标签 `status: needs triage` 不存在。让我先查看可用的标签。...


## 交互记录 (2026-06-23T09:17:22.313Z)

**用户**：
[{'tool_use_id': 'call_391d219adfb84443a747dcf5', 'type': 'tool_result', 'content': "bug\tSomething isn't working\t#d73a4a\ndocumentation\tImprovements or additions to documentation\t#0075ca\nduplicate\tThis issue or pull request already exists\t#cfd3d7\nenhancement\tNew feature or request\t#a2eeef\ngood first issue\tGood for newcomers\t#7057ff\nhelp wanted\tExtra attention is needed\t#008672\ninvalid\tThis doesn't seem right\t#e4e669\nquestion\tFurther information is requested\t#d876e3\nwontfix\tThis will not be worked on\t#ffffff", 'is_error': False}]

**AI**：
[思考] 可用的标签中没有 `status: needs triage`，我应该只使用 `enhancement` 标签。...


## 交互记录 (2026-06-23T09:17:40.285Z)

**用户**：
[{'tool_use_id': 'call_be29433da04c489bbb22d1af', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/issues/1067', 'is_error': False}]

**AI**：
[思考] Issue 创建成功，编号是 #1067。现在我需要：

1. 生成 PR 标题（严格格式）：
   - action: feat（小写）
   - 一级模块: MGS（大写）
   - 二级模块: gateway（小写）
   - 描述: add support for sso/login path in addition to sso-login
   
   PR 标题：`[feat][MGS][gateway] add support for sso/login path in addition to sso-login`

2. 生成 PR 描述（使用三段式格式）

3. 创建 PR 并关联 Issue #1067...


## 交互记录 (2026-06-23T09:18:05.798Z)

**用户**：
[{'tool_use_id': 'call_086fc39ffcbf459397694b8a', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/pull/1068', 'is_error': False}]

**AI**：
Issue 创建成功！Issue #1067 已创建。

现在创建 Pull Request：...


## 交互记录 (2026-06-24T02:11:28.133Z)

**用户**：
[{'type': 'text', 'text': 'Continue from where you left off.'}]

**AI**：
[思考] PR 创建成功！PR #1068 已创建并关联到 Issue #1067。

让我总结一下完成的工作：
1. ✅ 分析了代码变更
2. ✅ 识别了模块（MGS/gateway）
3. ✅ 搜索了相关 Issues
4. ✅ 创建了新 Issue #1067
5. ✅ 生成了符合规范的 PR 标题和描述
6. ✅ 创建了 PR #1068 并关联了 Issue

PR 标题格式正确：`[feat][MGS][gateway] add support for sso/login path in addition to sso-login`
- action: feat（小写）✅
- 一级模块: MGS（大写）✅
- 二级模块: gateway（小写）✅
- 描述: 小写句子 ✅

PR 描述使用了三段式格式：
- Background/Problem ✅
- Purpose of Change ✅
- Value/Impact ✅

PR 关联了 Issue: close #1067 ✅...


## 交互记录 (2026-06-24T02:11:33.459Z)

**用户**：
<command-message>pr</command-message>
<command-name>/pr</command-name>
<command-args>dev-2.0.0-oscar-sup 这个也提交下</command-args>

**AI**：
✅ **Pull Request 创建成功！**

**PR 信息：**
- **PR 编号**: #1068
- **PR 标题**: `[feat][MGS][gateway] add support for sso/login path in addition to sso-login`
- **关联 Issue**: close #1067
- **基础分支**: dev-2.0.0
- **目标分支**: dev-2.0.0-sso-login
- **标签**: enhancement

**变更摘要：**
- 修改文件：`UserRestful.scala`
- 变更内容：添加对 `sso/login` 路径的支持，同时保持对 `sso-login` 的兼容
- 代码行数：+2 / -1

**Issue 信息：**
- **Issue 编号**: #1067
- **Issue 标题**: `[feature][MGS][gateway] add support for sso/login path in addition to sso-login`

您可以通过以下链接查看：
- **Issue**: https://github.com/WeDataSphere/linkis/issues/1067
- **PR**: https://github.com/WeDataSphere/linkis/pull/1068...


## 交互记录 (2026-06-24T02:11:33.459Z)

**用户**：
[{'type': 'text', 'text': 'Base directory for this skill: C:\\Users\\v_kkhuang\\.claude\\skills\\pr\n\n# GitHub PR 自动创建器 (Apache Linkis 版)\n\n## ⚠️ 强制要求（MUST READ）\n\n**执行此技能时，必须严格遵守以下规则，违反任何一项都将导致输出不合格：**\n\n1. **Issue 处理优先**: 必须先搜索/创建 Issue，然后才能创建 PR。绝不能跳过 Issue 步骤直接创建 PR。\n2. **标题格式**: PR 标题和 Issue 标题必须严格遵循格式，大小写不能错。\n3. **内容全英文**: PR 和 Issue 的所有内容必须是英文，不能有中文。\n4. **三段式描述**: PR 的 "What is the purpose" 必须使用三段式格式（Background/Problem, Purpose, Value/Impact）。\n5. **Issue 关联**: PR 创建时必须关联 Issue（使用 `close #issue_id`）。\n6. **必须询问**: 找到相关 Issues 时必须询问用户选择，不能自动决定。\n7. **不允许 commit**: 只创建 PR 和 Issue，不执行任何 git commit 操作。\n\n---\n\n## 工作流程（按顺序执行）\n\n```\n┌─────────────────────────────────────────────────────────────────────────────┐\n│                        Apache Linkis PR 创建流程                              │\n├─────────────────────────────────────────────────────────────────────────────┤\n│  Step 1: 询问分支信息（base + target）                                        │\n│     ↓                                                                        │\n│  Step 2: 获取代码变更（git diff + git log）                                   │\n│     ↓                                                                        │\n│  Step 3: 分析变更，识别模块（一级 + 二级）                                    │\n│     ↓                                                                        │\n│  Step 4: 搜索 GitHub Issues（gh issue list）                                 │\n│     ↓                                                                        │\n│  Step 5: 询问用户选择 Issue（复用现有 OR 创建新 Issue）                       │\n│     ↓                                                                        │\n│  Step 6: 如果需要，创建新 Issue（gh issue create）                            │\n│     ↓                                                                        │\n│  Step 7: 生成 PR 标题（严格格式）                                              │\n│     ↓                                                                        │\n│  Step 8: 生成 PR 描述（三段式 + Brief change log + Checklist）                │\n│     ↓                                                                        │\n│  Step 9: 创建 PR 并关联 Issue（gh pr create）                                 │\n└─────────────────────────────────────────────────────────────────────────────┘\n```\n\n---\n\n## 执行步骤详解\n\n### Step 1: 询问分支信息\n\n**必须询问用户以下信息：**\n\n```\n请输入以下信息以创建 Pull Request：\n\n1. 基础分支（base branch）: 即要合并的目标分支（例如: apachemaster, dev-1.19.0）\n2. 目标分支（target branch）: 即你的特性分支（例如: dev-1.19.0-feature-xxx）\n\n请确认这是用于创建 Pull Request，而非其他操作。\n```\n\n**验证规则**：\n- 必须确认用户意图是创建 PR\n- 如果分支不存在，列出可用分支供用户选择\n- 使用 `git branch -r` 列出远程分支\n\n---\n\n### Step 2: 获取代码变更\n\n**执行以下命令获取变更信息：**\n\n```bash\n# 获取变更文件列表\ngit diff --name-status <base_branch>..<target_branch>\n\n# 获取提交历史\ngit log <base_branch>..<target_branch> --oneline --no-merges\n\n# 获取详细的代码差异（用于分析）\ngit diff <base_branch>..<target_branch> --stat\n```\n\n**分析内容**：\n- 变更的文件数量和类型\n- 变更的代码行数\n- 提交的数量和主题\n\n---\n\n### Step 3: 分析变更，识别模块\n\n**从变更文件路径中提取模块信息：**\n\n#### 一级模块映射表（严格使用）\n\n| 目录名 | 缩写 | 全称 |\n|--------|------|------|\n| `linkis-spring-cloud-services` | `MGS` | microservice-governance |\n| `linkis-computation-governance` | `CGS` | computation-governance |\n| `linkis-public-enhancements` | `PES` | public-enhancements |\n| `linkis-engineconn-plugins` | `EC` | engineconn |\n| `linkis-extensions` | `ETS` | extensions |\n| `linkis-orchestrator` | `ORC` | orchestrator |\n| `linkis-web` | `WEB` | web |\n| `linkis-commons` | `COM` | commons |\n\n#### 二级模块提取规则\n\n| 一级模块 | 二级模块提取方式 | 示例 |\n|---------|-----------------|------|\n| MGS | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-gateway` → `gateway` |\n| CGS | `linkis-cg-xxx` 中的 xxx 部分（小写）或 `linkis-xxx` 去掉前缀 | `linkis-entrance` → `entrance` |\n| PES | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-datasource` → `datasource` |\n| EC | 引擎名称（小写） | `spark` → `spark` |\n| WEB | 通常无二级模块，使用 `frontend` 或省略 | - |\n| COM | 目录名（小写） | `common` → `common` |\n\n**模块识别逻辑**：\n1. 如果所有变更文件在同一模块，使用该模块\n2. 如果跨越多个模块，选择变更最多的模块作为主模块\n3. 如果变更分散且数量相近，询问用户确认主模块\n\n---\n\n### Step 4: 搜索 GitHub Issues\n\n**必须执行 Issue 搜索，不能跳过：**\n\n```bash\n# 搜索 open issues，使用模块和变更关键词\ngh issue list --state open --search "<模块名> <关键词>" --limit 20\n\n# 例如：\n# gh issue list --state open --search "PES datasource filter" --limit 20\n# gh issue list --state open --search "CGS entrance location" --limit 20\n```\n\n**搜索关键词提取**：\n- 模块名（一级 + 二级）\n- 变更相关的关键词（从文件名、提交信息中提取）\n- action 类型（feat/fix 等）\n\n---\n\n### Step 5: 询问用户选择 Issue\n\n#### 如果找到相关 Issues（至少 1 个）\n\n**必须询问用户，不能自动选择：**\n\n```\n找到以下相关的 open issues：\n\n#123: [bug][PES][datasource] add support for getting published datasource by type and proxy user\n#456: [feature][CGS][entrance] hive location clause security check\n\n请选择：\n1. 使用现有 issue #123\n2. 使用现有 issue #456\n3. 创建新 issue\n4. 手动输入其他 issue 编号\n\n请输入选项 (1/2/3/4) 或 issue 编号:\n```\n\n#### 如果没有找到相关 Issues\n\n**直接进入创建新 Issue 流程**（Step 6）\n\n---\n\n### Step 6: 创建新 Issue（如需要）\n\n**如果用户选择创建新 Issue，或没有找到相关 Issue，则执行以下步骤：**\n\n#### Issue 标题格式（严格遵循）\n\n```\n[类型][一级模块][二级模块] 功能描述\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 示例 |\n|------|------|------|\n| `类型` | **小写** | `bug`, `feature`, `ask` |\n| `一级模块` | **大写** | `MGS`, `CGS`, `PES`, `EC`, `ETS`, `ORC`, `WEB`, `COM` |\n| `二级模块` | **小写** | `entrance`, `datasource`, `spark` 等 |\n| `功能描述` | **小写句子** | 首字母不大写，使用普通英文句子 |\n\n**类型选择规则**：\n- PR action 为 `fix` → Issue 类型为 `bug`\n- PR action 为 `feat` → Issue 类型为 `feature`\n- 其他情况 → Issue 类型为 `feature`\n\n**Issue 标题示例**：\n```\n✅ 正确: [bug][CGS][entrance] hive tasks should not be allowed to use location clause for security reasons\n✅ 正确: [feature][PES][datasource] add support for getting published datasource by type and proxy user\n❌ 错误: [Bug][CGS][Entrance] Hive Tasks Should Not Be Allowed... (大小写错误)\n❌ 错误: [bug][cgs][entrance] ... (一级模块小写错误)\n```\n\n#### Issue 内容格式（严格遵循）\n\n**根据 Issue 类型选择对应模板：**\n\n##### Bug Report 模板（fix 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar issues.\n\n### Linkis Component\n\n- [ ] linkis-commons\n- [ ] linkis-computation-governance\n- [ ] linkis-dist\n- [ ] linkis-engineconn-plugin\n- [ ] linkis-extensions\n- [ ] linkis-orchestrator\n- [x] linkis-public-enhancements\n- [ ] linkis-spring-cloud-services\n- [ ] linkis-web\n\n### Description\n\n[Brief description of the bug - what happened and what should happen]\n\n### Steps to reproduce\n\n1. Step 1...\n2. Step 2...\n3. Step 3...\n\n### Expected behavior\n\n[Describe what you expected to happen]\n\n### Your environment\n\n- Linkis version used: [x.x.x]\n- Environment name and version:\n    - hadoop-x.x.x\n    - hive-x.x.x\n    - spark-x.x.x\n    - scala-x.x.x\n    - jdk 1.8.x_xx\n\n### Anything else\n\n[Additional context, logs, frequency of occurrence, etc.]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n##### Feature Request 模板（feat 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar feature requirement.\n\n### Problem Description\n\nIs your feature request related to a problem? Please describe.\n\n[Describe the problem or need]\n\n### Description\n\nA short description of your feature\n\n[Clear and concise description of what you want to add]\n\n### Use case\n\nDescribe the use case of your feature request and the solution you\'d like.\n\n[Describe the specific use case and expected behavior]\n\n### Solutions\n\nDescribe alternatives you\'ve considered.\n\n[Describe any alternative solutions or workarounds you\'ve considered]\n\n### Anything else\n\nAdditional context or screenshots about the feature request.\n\n[Any additional information, screenshots, or examples]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n#### Issue 标签规则\n\n| Issue 类型 | 标签 |\n|-----------|------|\n| `bug` | `bug`, `status: needs triage` |\n| `feature` | `enhancement`, `status: needs triage` |\n| `ask` | `question`, `status: needs triage` |\n\n#### 创建 Issue 命令\n\n```bash\ngh issue create \\\n  --title "[<type>][<MODULE>][<submodule>] <description>" \\\n  --body "<issue_content>" \\\n  --label "<label1>,<label2>"\n```\n\n**保存返回的 issue_id，用于 PR 关联！**\n\n---\n\n### Step 7: 生成 PR 标题\n\n**PR 标题格式（严格遵循）**：\n\n```\n[action][一级模块][二级模块] 一句话总结\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 可选值 | 示例 |\n|------|------|--------|------|\n| `action` | **小写** | feat, fix, perf, refactor, docs, style, test, chore | `feat`, `fix` |\n| `一级模块` | **大写** | MGS, CGS, PES, EC, ETS, ORC, WEB, COM | `PES`, `CGS` |\n| `二级模块` | **小写** | 具体服务名称 | `datasource`, `entrance` |\n| `一句话总结` | **小写句子** | 普通英文，首字母不大写 | `add support for filtering by type` |\n\n**Action 类型定义**：\n\n| Action | 含义 | 使用场景 |\n|--------|------|----------|\n| `feat` | 新功能 | 添加新的功能、API、模块 |\n| `fix` | 修复 Bug | 修复问题、缺陷 |\n| `perf` | 性能优化 | 提升性能、优化资源使用 |\n| `refactor` | 重构 | 代码重构，不改变功能 |\n| `docs` | 文档 | 仅文档变更 |\n| `style` | 代码风格 | 格式化、代码风格调整 |\n| `test` | 测试 | 仅测试相关变更 |\n| `chore` | 构建/工具 | 构建、依赖、工具链更新 |\n\n**PR 标题示例**：\n\n```\n✅ 正确: [feat][PES][datasource] add support for getting published datasource by type and proxy user\n✅ 正确: [fix][CGS][entrance] resolve null pointer exception in engine reuse metrics update\n✅ 正确: [perf][CGS][application-manager] optimize engine creation performance\n❌ 错误: [Feat][PES][datasource] Add Support For... (大小写错误)\n❌ 错误: [feat][pES][datasource] ... (一级模块大小写混合)\n❌ 错误: feat: add support for... (缺少模块标签)\n```\n\n---\n\n### Step 8: 生成 PR 描述\n\n**PR 描述必须严格遵循以下格式：**\n\n#### PR 描述模板（完整格式）\n\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\n[Describe the background or problem - what issue exists, 2-3 sentences]\n\n**Purpose of Change:**\n[Describe the purpose of this PR - what this PR does to solve the problem, 2-3 sentences]\n\n**Value/Impact:**\n[Describe the value or impact - what benefits this change brings, 1-2 sentences]\n\n### Related issues/PRs\n\nRelated issues: close #<issue_id>\nRelated pr:none\n\n### Brief change log\n\n- [Change point 1 - start with verb]\n- [Change point 2 - start with verb]\n- [Change point 3 - start with verb]\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n#### "What is the purpose" 格式要求（严格三段式）\n\n**必须使用三段式结构，不能合并或省略：**\n\n| 段落 | 标题 | 内容要求 | 长度 |\n|------|------|----------|------|\n| 第一段 | **Background/Problem:** | 描述背景或问题 - 当前存在什么问题或需求 | 2-3 句话 |\n| 第二段 | **Purpose of Change:** | 描述改动的目的 - 本 PR 如何解决这个问题 | 2-3 句话 |\n| 第三段 | **Value/Impact:** | 描述价值或影响 - 改动带来的好处 | 1-2 句话 |\n\n**格式规范**：\n- ✅ 每段标题使用粗体，以冒号结尾\n- ✅ 段落标题使用 `**Title:**` 格式\n- ✅ 内容另起一行，不要和标题在同一行\n- ❌ 不要使用 "This PR adds..." 或 "This PR implements..." 格式\n- ✅ 使用客观描述，如 "This PR adds...", "The change enables..."\n\n**示例**：\n\n```\n✅ 正确格式:\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n❌ 错误格式:\n\nThis PR adds support for filtering datasources by type and user. (缺少三段式)\nThe problem is that users cannot filter datasources. (不完整)\n```\n\n#### "Brief change log" 格式要求\n\n**每条变更记录必须：**\n- 以动词开头\n- 使用现在时\n- 简洁明确\n- 每条独立一行\n\n**常用动词示例**：\n- `Add` - 添加新功能\n- `Fix` - 修复问题\n- `Update` - 更新现有功能\n- `Remove` - 删除功能\n- `Refactor` - 重构代码\n- `Optimize` - 优化性能\n- `Improve` - 改进\n\n**示例**：\n\n```\n✅ 正确:\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n❌ 错误:\n- Added API... (过去时)\n- Update the service... (不明确)\n```\n\n#### Checklist 智能勾选规则\n\n| 选项 | 何时勾选 | 说明 |\n|------|---------|------|\n| I have read the Contributing Guidelines | ✅ 始终勾选 | - |\n| I have explained the need for this PR | ✅ 始终勾选 | - |\n| I have explained the changes | ✅ 有代码变更时勾选 | 大多数情况勾选 |\n| I have added tests | 有测试时勾选 | 如果添加了测试代码 |\n| I have updated the documentation | 有文档变更时勾选 | 如果修改了文档 |\n| I have verified backward compatibility | ✅ 通常勾选 | 除非是破坏性变更 |\n| I have written unit tests | 有完整测试时勾选 | 如果有完整的单元测试 |\n\n---\n\n### Step 9: 创建 PR\n\n**执行 PR 创建命令：**\n\n```bash\ngh pr create \\\n  --title "<PR_TITLE>" \\\n  --body "<PR_BODY>" \\\n  --base "<base_branch>" \\\n  --head "<target_branch>" \\\n  --label "<label1>,<label2>"\n```\n\n**PR 标签映射规则**：\n\n| Action 类型 | Label |\n|-----------|-------|\n| `feat` | `enhancement` |\n| `fix` | `bug` |\n| `perf` | `performance` |\n| `refactor` | `refactor` |\n| `docs` | `documentation` |\n| `style` | `style` |\n| `test` | `test` |\n| `chore` | `build` |\n\n---\n\n## 错误处理\n\n| 错误场景 | 处理方式 |\n|---------|----------|\n| 找不到 git 仓库 | 提示用户在正确的项目目录中执行 |\n| gh 未登录 | 提示用户运行 `gh auth login` |\n| 分支不存在 | 列出可用分支供用户选择 |\n| 未找到代码变更 | 提示用户检查分支是否正确 |\n| Issue 搜索失败 | 提示用户手动关联 Issue |\n| PR 创建失败 | 显示错误信息，提示用户检查权限或网络 |\n\n---\n\n## 完整示例\n\n### 示例 1: 新增功能（创建新 Issue）\n\n**场景**: 添加数据源过滤功能，无相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-public-enhancements/linkis-datasource/src/...`\n- 模块: `PES` / `datasource`\n- Action: `feat`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "PES datasource filter" --limit 20\n```\n结果: 无相关 Issues\n\n**Step 6**: 创建新 Issue\n```bash\ngh issue create \\\n  --title "[feature][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --label "enhancement,status: needs triage"\n```\n返回: Issue #789\n\n**Step 7**: 生成 PR 标题\n```\n[feat][PES][datasource] add support for getting published datasource by type and proxy user\n```\n\n**Step 8**: 生成 PR 描述\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n### Related issues/PRs\n\nRelated issues: close #789\nRelated pr:none\n\n### Brief change log\n\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[feat][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --base "apachemaster" \\\n  --head "dev-1.19.0-feature-datasource-filter" \\\n  --label "enhancement"\n```\n\n---\n\n### 示例 2: 修复 Bug（复用现有 Issue）\n\n**场景**: 修复空指针异常，找到相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-computation-governance/linkis-cg-linkismanager/src/...`\n- 模块: `CGS` / `linkismanager` (简化为 `application-manager`)\n- Action: `fix`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "CGS application-manager null pointer" --limit 20\n```\n结果: 找到 Issue #456\n\n**Step 5**: 询问用户\n```\n找到以下相关的 open issues：\n\n#456: [bug][CGS][application-manager] null pointer exception in engine reuse metrics update causing monitoring data loss\n\n请选择：\n1. 使用现有 issue #456\n2. 创建新 issue\n3. 手动输入其他 issue 编号\n```\n用户选择: 1\n\n**Step 6**: 跳过（复用现有 Issue #456）\n\n**Step 7**: 生成 PR 标题\n```\n[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update\n```\n\n**Step 8**: 生成 PR 描述（使用 Bug Report 风格的三段式）\n```markdown\n### What is the purpose of the change\n\n**Background/Problem:**\nDuring engine creation and reuse, when updating metrics information, a null pointer exception occurs, causing some monitoring data to be lost and affecting system stability.\n\n**Purpose of Change:**\nTo address this problem, this PR adds null checks and protection to the relevant code, ensuring the robustness of metrics updates.\n\n**Value/Impact:**\nAfter the change, the system runs stably with complete and available monitoring data, improving system reliability.\n\n### Related issues/PRs\n\nRelated issues: close #456\nRelated pr:none\n\n### Brief change log\n\n- Add null pointer checks in DefaultEngineCreateService\n- Add null pointer checks in DefaultEngineReuseService\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update" \\\n  --body "..." \\\n  --base "dev-1.19.0" \\\n  --head "dev-1.19.0-fix-null-pointer" \\\n  --label "bug"\n```\n\n---\n\n## 快速参考\n\n### 大小写规则速查\n\n| 位置 | PR | Issue |\n|------|-----|-------|\n| 类型/Action | **小写** | **小写** |\n| 一级模块 | **大写** | **大写** |\n| 二级模块 | **小写** | **小写** |\n| 描述/总结 | **小写句子** | **小写句子** |\n\n### 格式速查\n\n```\nPR 标题:    [action][MODULE][submodule] description\nIssue 标题:  [type][MODULE][submodule] description\n\nPR action:   feat, fix, perf, refactor, docs, style, test, chore\nIssue type:  bug, feature, ask\n\nMODULE:      MGS, CGS, PES, EC, ETS, ORC, WEB, COM\n```\n\n### 命令速查\n\n```bash\n# 搜索 Issues\ngh issue list --state open --search "<关键词>" --limit 20\n\n# 创建 Issue\ngh issue create --title "<标题>" --body "<内容>" --label "<标签>"\n\n# 创建 PR\ngh pr create --title "<标题>" --body "<内容>" --base "<base>" --label "<标签>"\n\n# 列出分支\ngit branch -r\n```\n\n---\n\n## 使用方法\n\n```bash\n/pr\n```\n\n然后按照提示输入分支信息即可。\n\n---\n\n## 依赖工具\n\n- `git` - 版本控制\n- `gh` (GitHub CLI) - GitHub 操作\n\n确保 `gh` 已登录：`gh auth login`\n\n\nARGUMENTS: dev-2.0.0-oscar-sup 这个也提交下'}]

**AI**：
No response requested....


## 交互记录 (2026-06-24T02:11:48.370Z)

**用户**：
[{'type': 'text', 'text': 'yes'}]

**AI**：
[思考] 用户想为 `dev-2.0.0-oscar-sup` 分支创建 PR。根据工作流程，我需要：

1. 询问分支信息（确认 base 和 target）
2. 获取代码变更
3. 分析变更，识别模块
4. 搜索 GitHub Issues
5. 询问用户选择 Issue
6. 如果需要，创建新 Issue
7. 生成 PR 标题
8. 生成 PR 描述
9. 创建 PR

让我开始执行。...


## 交互记录 (2026-06-24T02:12:03.496Z)

**用户**：
[{'tool_use_id': 'call_77b2f4a721944c2b815581ea', 'type': 'tool_result', 'content': '08f79c2dc #AI commit# 开发阶段：修复oscar链接异常\n8d9e86528 #AI commit add orsca ds\n3a4aaf10a #AI commit# feat(queryColumns): recursively flatten nested object fields in ES mappings(优化EsMetaService.queryColumns：递归展开ES mappings嵌套object字段，含type停止递归取叶子字段，含properties则用其进入下一层，层级用|分隔；FIELD_PROPS改为公开常量)\n25f5221c7 #AI commit# refactor(queryTableProps): use placeholder table name __DB_DEFAULT__ to trigger database properties query(优化HiveMetaService.queryTableProps：改用占位表名__DB_DEFAULT__触发库参数查询，部分终端不支持空表名；并在落库查询前校验占位表真实不存在，避免误判)\na6eb474c6 #AI commit# enhance(queryTableProps): when table is empty, return database properties with common metadata(优化HiveMetaService.queryTableProps：表名为空时回退查询database，返回库参数及常用元数据location/owner/ownerType/name/description)', 'is_error': False}]

**AI**：
好的，现在为 `dev-2.0.0-oscar-sup` 分支创建 PR。

**请确认以下信息以创建 Pull Request：**

- **基础分支**: `dev-2.0.0` (合并的目标分支)
- **目标分支**: `dev-2.0.0-oscar-sup` (您的特性分支)

请确认这是用于创建 Pull Request，而非其他操作。（输入 "yes" 或 "确认" 继续）...


## 交互记录 (2026-06-24T02:12:04.607Z)

**用户**：
[{'tool_use_id': 'call_2e3689e67b204362a43568b0', 'type': 'tool_result', 'content': 'M\tlinkis-dist/package/db/linkis_dml.sql\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/ElasticConnection.java\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/EsMetaService.java\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\nA\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/OscarMetaService.java\nA\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/oscar/SqlConnection.java\nM\tlinkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/cache/CacheConfiguration.java', 'is_error': False}]

**AI**：
[思考] 用户确认了分支信息，现在开始分析代码变更并创建 PR。

需要执行的步骤：
1. 获取代码变更（git diff + git log）
2. 分析变更，识别模块
3. 搜索 GitHub Issues
4. 如果需要，创建新 Issue
5. 生成 PR 标题和描述
6. 创建 PR

让我开始执行。...


## 交互记录 (2026-06-24T02:12:04.739Z)

**用户**：
[{'tool_use_id': 'call_8711a6af7acb413c9d77ad07', 'type': 'tool_result', 'content': ' linkis-dist/package/db/linkis_dml.sql              |  13 ++\n .../metadata/query/service/ElasticConnection.java  |   6 +-\n .../metadata/query/service/EsMetaService.java      |  80 ++++++--\n .../metadata/query/service/HiveMetaService.java    |  73 +++++++\n .../metadata/query/service/OscarMetaService.java   |  92 +++++++++\n .../query/service/oscar/SqlConnection.java         | 223 +++++++++++++++++++++\n .../query/common/cache/CacheConfiguration.java     |   2 +-\n 7 files changed, 472 insertions(+), 17 deletions(-)', 'is_error': False}]

**AI**：
好的，现在开始分析代码变更并创建 PR。...


## 交互记录 (2026-06-24T02:12:12.282Z)

**用户**：
[{'tool_use_id': 'call_ac1f7b30e43a4d0eadb6f2af', 'type': 'tool_result', 'content': 'diff --git a/linkis-dist/package/db/linkis_dml.sql b/linkis-dist/package/db/linkis_dml.sql\nindex 831c24ff3..e59498cca 100644\n--- a/linkis-dist/package/db/linkis_dml.sql\n+++ b/linkis-dist/package/db/linkis_dml.sql\n@@ -571,6 +571,19 @@ INSERT INTO `linkis_ps_dm_datasource_type` (`name`, `description`, `option`, `cl\n INSERT INTO `linkis_ps_dm_datasource_type` (`name`, `description`, `option`, `classifier`, `icon`, `layers`) VALUES (\'doris\',\'doris数据库\',\'doris\',\'olap\',\'\',4);\n INSERT INTO `linkis_ps_dm_datasource_type` (`name`, `description`, `option`, `classifier`, `icon`, `layers`) VALUES (\'clickhouse\',\'clickhouse数据库\',\'clickhouse\',\'olap\',\'\',4);\n INSERT INTO `linkis_ps_dm_datasource_type` (`name`, `description`, `option`, `classifier`, `icon`, `layers`) VALUES (\'starrocks\',\'starrocks数据库\',\'starrocks\',\'olap\',\'\',4);\n+INSERT INTO `linkis_ps_dm_datasource_type` (`name`, `description`, `option`, `classifier`, `icon`, `layers`) VALUES (\'oscar\',\'神通数据库\',\'oscar\',\'关系型数据库\',\'\',3);\n+\n+select @data_source_type_id := id from `linkis_ps_dm_datasource_type` where `name` = \'oscar\';\n+INSERT INTO `linkis_ps_dm_datasource_type_key`\n+    (`data_source_type_id`, `key`, `name`, `name_en`, `default_value`, `value_type`, `scope`, `require`, `description`, `description_en`, `value_regex`, `ref_id`, `ref_value`, `data_source`, `update_time`, `create_time`)\n+VALUES (@data_source_type_id, \'address\', \'地址\', \'Address\', NULL, \'TEXT\', NULL, 0, \'地址(host1:port1,host2:port2...)\', \'Address(host1:port1,host2:port2...)\', NULL, NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'host\', \'主机名(Host)\', \'Host\', NULL, \'TEXT\', NULL, 1, \'主机名(Host)\', \'Host\', NULL, NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'port\', \'端口号(Port)\', \'Port\', NULL, \'TEXT\', NULL, 1, \'端口号(Port)\', \'Port\', NULL, NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'driverClassName\', \'驱动类名(Driver class name)\', \'Driver class name\', \'com.oscar.Driver\', \'TEXT\', NULL, 1, \'驱动类名(Driver class name)\', \'Driver class name\', NULL, NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'params\', \'连接参数(Connection params)\', \'Connection params\', NULL, \'TEXT\', NULL, 0, \'输入JSON格式(Input JSON format): {"param":"value"}\', \'Input JSON format: {"param":"value"}\', NULL, NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'username\', \'用户名(Username)\', \'Username\', NULL, \'TEXT\', NULL, 1, \'用户名(Username)\', \'Username\', \'^[0-9A-Za-z_-]+$\', NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'password\', \'密码(Password)\', \'Password\', NULL, \'PASSWORD\', NULL, 1, \'密码(Password)\', \'Password\', \'\', NULL, NULL, NULL,  now(), now()),\n+       (@data_source_type_id, \'instance\', \'实例名(instance)\', \'Instance\', NULL, \'TEXT\', NULL, 1, \'实例名(instance)\', \'Instance\', NULL, NULL, NULL, NULL,  now(), now());\n \n select @data_source_type_id := id from `linkis_ps_dm_datasource_type` where `name` = \'mongodb\';\n INSERT INTO `linkis_ps_dm_datasource_type_key` (`data_source_type_id`, `key`, `name`, `name_en`, `default_value`, `value_type`, `scope`, `require`, `description`, `description_en`, `value_regex`, `ref_id`, `ref_value`, `data_source`, `update_time`, `create_time`) VALUES (@data_source_type_id, \'username\', \'用户名\', \'Username\', NULL, \'TEXT\', NULL, 1, \'用户名\', \'Username\', \'^[0-9A-Za-z_-]+$\', NULL, \'\', NULL, now(), now());\ndiff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/ElasticConnection.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/ElasticConnection.java\nindex 5f47452cd..b5683ec8e 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/ElasticConnection.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/ElasticConnection.java\n@@ -45,7 +45,11 @@ public class ElasticConnection implements Closeable {\n \n   private static final String DEFAULT_MAPPING_NAME = "mappings";\n   private static final String DEFAULT_INDEX_NAME = "index";\n-  private static final String FIELD_PROPS = "properties";\n+  /**\n+   * Nested properties key in ElasticSearch mappings, used to descend into nested objects\n+   * (ElasticSearch mappings 中嵌套对象的 properties 键，用于递归展开嵌套字段)\n+   */\n+  public static final String FIELD_PROPS = "properties";\n \n   private RestClient restClient;\n \ndiff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/EsMetaService.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/EsMetaService.java\nindex e462d088e..2f977687e 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/EsMetaService.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/elasticsearch/src/main/java/org/apache/linkis/metadata/query/service/EsMetaService.java\n@@ -23,9 +23,12 @@ import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;\n import org.apache.linkis.metadata.query.common.service.AbstractDbMetaService;\n import org.apache.linkis.metadata.query.common.service.MetadataConnection;\n \n+import org.apache.commons.lang3.StringUtils;\n+\n+import java.util.ArrayList;\n import java.util.List;\n import java.util.Map;\n-import java.util.stream.Collectors;\n+import java.util.Objects;\n \n public class EsMetaService extends AbstractDbMetaService<ElasticConnection> {\n   @Override\n@@ -76,22 +79,69 @@ public class EsMetaService extends AbstractDbMetaService<ElasticConnection> {\n       ElasticConnection connection, String database, String table) {\n     try {\n       Map<Object, Object> props = connection.getProps(database, table);\n-      return props.entrySet().stream()\n-          .map(\n-              entry -> {\n-                MetaColumnInfo info = new MetaColumnInfo();\n-                info.setName(String.valueOf(entry.getKey()));\n-                Object value = entry.getValue();\n-                if (value instanceof Map) {\n-                  info.setType(\n-                      String.valueOf(\n-                          ((Map) value).getOrDefault(ElasticConnection.DEFAULT_TYPE_NAME, "")));\n-                }\n-                return info;\n-              })\n-          .collect(Collectors.toList());\n+      List<MetaColumnInfo> columns = new ArrayList<>();\n+      if (Objects.nonNull(props)) {\n+        // Flatten nested object fields recursively(递归展开嵌套对象字段)\n+        flattenFields(props, "", columns);\n+      }\n+      return columns;\n     } catch (Exception e) {\n       throw new RuntimeException("Fail to get ElasticSearch columns(获取索引字段失败)", e);\n     }\n   }\n+\n+  /** Separator between levels of a flattened nested field name(展开嵌套字段时层级之间的分隔符) */\n+  private static final String FIELD_NAME_SEPARATOR = "|";\n+\n+  /**\n+   * Recursively flatten ElasticSearch mapping properties into a flat column list.\n+   *\n+   * <p>For each entry of the props map:\n+   *\n+   * <ul>\n+   *   <li>If the value contains {@link ElasticConnection#DEFAULT_TYPE_NAME}, stop recursion and add\n+   *       a column whose name is the parent prefix joined with the key (or just the key at the top\n+   *       level) and whose type is the value of {@code type}.\n+   *   <li>Else if the value contains {@link ElasticConnection#FIELD_PROPS} as a map, descend into\n+   *       it with the key appended to the prefix.\n+   *   <li>Otherwise stop recursion and add nothing.\n+   * </ul>\n+   *\n+   * (递归展开 ES mappings properties 为扁平字段列表：value 含 type 则停止递归并加字段；含 properties 且为 Map 则用 properties\n+   * 进入下一层；其他情况停止递归不加字段)\n+   *\n+   * @param props properties map at current level\n+   * @param prefix parent field name prefix, empty at the top level\n+   * @param columns accumulator for flattened columns\n+   */\n+  @SuppressWarnings("unchecked")\n+  private void flattenFields(\n+      Map<Object, Object> props, String prefix, List<MetaColumnInfo> columns) {\n+    for (Map.Entry<Object, Object> entry : props.entrySet()) {\n+      Object value = entry.getValue();\n+      if (!(value instanceof Map)) {\n+        // Not a structured mapping, stop recursion(非结构化 mapping，停止递归)\n+        continue;\n+      }\n+      Map<String, Object> fieldMap = (Map<String, Object>) value;\n+      String key = String.valueOf(entry.getKey());\n+      String fieldName = StringUtils.isBlank(prefix) ? key : prefix + FIELD_NAME_SEPARATOR + key;\n+      if (fieldMap.containsKey(ElasticConnection.DEFAULT_TYPE_NAME)) {\n+        // Leaf field, stop recursion(叶子字段，停止递归)\n+        MetaColumnInfo info = new MetaColumnInfo();\n+        info.setIndex(columns.size());\n+        info.setName(fieldName);\n+        info.setType(\n+            String.valueOf(fieldMap.getOrDefault(ElasticConnection.DEFAULT_TYPE_NAME, "")));\n+        columns.add(info);\n+      } else if (fieldMap.containsKey(ElasticConnection.FIELD_PROPS)\n+          && fieldMap.get(ElasticConnection.FIELD_PROPS) instanceof Map) {\n+        // Nested object, descend with the properties(嵌套对象，用 properties 进入下一层)\n+        Map<Object, Object> nestedProps =\n+            (Map<Object, Object>) fieldMap.get(ElasticConnection.FIELD_PROPS);\n+        flattenFields(nestedProps, fieldName, columns);\n+      }\n+      // Otherwise stop recursion(其他情况停止递归，不加字段)\n+    }\n+  }\n }\ndiff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\nindex 73c4e86f7..7d52cf932 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\n@@ -31,6 +31,7 @@ import org.apache.linkis.metadata.query.common.service.MetadataConnection;\n import org.apache.commons.io.FileUtils;\n import org.apache.commons.io.IOUtils;\n import org.apache.commons.lang3.StringUtils;\n+import org.apache.hadoop.hive.metastore.api.Database;\n import org.apache.hadoop.hive.metastore.api.FieldSchema;\n import org.apache.hadoop.hive.ql.metadata.Hive;\n import org.apache.hadoop.hive.ql.metadata.HiveException;\n@@ -58,6 +59,12 @@ public class HiveMetaService extends AbstractDbMetaService<HiveConnection> {\n   private static final String PARTITION_KV_SEPARATOR = "=";\n   private static final String PARTITION_CV_SEPARATOR = "/";\n \n+  /**\n+   * Placeholder table name used to request database properties when the client does not allow an\n+   * empty table name(部分终端不允许传空表名，用该占位表名代表查询库参数)\n+   */\n+  private static final String DB_DEFAULT_PLACEHOLDER_TABLE = "__DB_DEFAULT__";\n+\n   public HiveMetaService() {\n     client = BmlClientFactory.createBmlClient();\n   }\n@@ -223,6 +230,35 @@ public class HiveMetaService extends AbstractDbMetaService<HiveConnection> {\n   public Map<String, String> queryTableProps(\n       HiveConnection connection, String database, String table) {\n     try {\n+      // When the table is the placeholder, fall back to query the database properties\n+      // only if the placeholder table does not really exist(当表名为占位符时，仅在该占位表真实不存在时才回退查询数据库参数及常用元数据)\n+      if (isDbDefaultPlaceholder(table) && !placeholderTableExists(connection, database, table)) {\n+        Database database0 = connection.getClient().getDatabase(database);\n+        Map<String, String> properties = new HashMap<>();\n+        if (Objects.nonNull(database0)) {\n+          // Custom database properties set via DBPROPERTIES(库的自定义参数)\n+          if (Objects.nonNull(database0.getParameters())) {\n+            properties.putAll(database0.getParameters());\n+          }\n+          // Append the database common metadata(补充数据库的常用元数据)\n+          Optional.ofNullable(database0.getLocationUri())\n+              .filter(StringUtils::isNotBlank)\n+              .ifPresent(location -> properties.put("location", location));\n+          Optional.ofNullable(database0.getOwnerName())\n+              .filter(StringUtils::isNotBlank)\n+              .ifPresent(owner -> properties.put("owner", owner));\n+          Optional.ofNullable(database0.getOwnerType())\n+              .map(Enum::name)\n+              .ifPresent(ownerType -> properties.put("ownerType", ownerType));\n+          Optional.ofNullable(database0.getName())\n+              .filter(StringUtils::isNotBlank)\n+              .ifPresent(name -> properties.put("name", name));\n+          Optional.ofNullable(database0.getDescription())\n+              .filter(StringUtils::isNotBlank)\n+              .ifPresent(desc -> properties.put("description", desc));\n+        }\n+        return properties;\n+      }\n       Table rawTable = connection.getClient().getTable(database, table);\n       return new HashMap<>((Map) rawTable.getMetadata());\n     } catch (Exception e) {\n@@ -230,6 +266,43 @@ public class HiveMetaService extends AbstractDbMetaService<HiveConnection> {\n     }\n   }\n \n+  /**\n+   * Whether the given table name is the database-level placeholder, which is used to request\n+   * database properties instead of table ones when some clients do not allow an empty table name\n+   * (判断给定表名是否为库级占位符；部分终端不允许传空表名，故用占位表名代表查询库参数)\n+   *\n+   * @param table table name\n+   * @return true if it is the placeholder\n+   */\n+  private boolean isDbDefaultPlaceholder(String table) {\n+    return DB_DEFAULT_PLACEHOLDER_TABLE.equals(table);\n+  }\n+\n+  /**\n+   * Check whether the placeholder table really exists, so that we never silently fall back to the\n+   * database query when a real table with the same name exists(校验占位表是否真实存在，避免真实存在同名表时被误当作占位符而漏查表参数)\n+   *\n+   * @param connection hive connection\n+   * @param database database name\n+   * @param table placeholder table name\n+   * @return true if a real table with the placeholder name exists\n+   */\n+  private boolean placeholderTableExists(HiveConnection connection, String database, String table) {\n+    try {\n+      // throwException=false -> returns null when the table is missing, no exception thrown\n+      Table rawTable = connection.getClient().getTable(database, table, false);\n+      return Objects.nonNull(rawTable);\n+    } catch (Exception e) {\n+      // If the existence cannot be determined, treat it as existing to be safe(无法判定时按存在处理，走表查询更安全)\n+      LOG.warn(\n+          "Fail to check placeholder table existence:["\n+              + table\n+              + "], treat as existing(判定占位表是否存在失败，按存在处理)",\n+          e);\n+      return true;\n+    }\n+  }\n+\n   @Override\n   public Map<String, String> queryPartitionProps(\n       HiveConnection connection, String database, String table, String partition) {\ndiff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/OscarMetaService.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/OscarMetaService.java\nnew file mode 100644\nindex 000000000..15dee75a8\n--- /dev/null\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/OscarMetaService.java\n@@ -0,0 +1,92 @@\n+/*\n+ * Licensed to the Apache Software Foundation (ASF) under one or more\n+ * contributor license agreements.  See the NOTICE file distributed with\n+ * this work for additional information regarding copyright ownership.\n+ * The ASF licenses this file to You under the Apache License, Version 2.0\n+ * (the "License"); you may not use this file except in compliance with\n+ * the License.  You may obtain a copy of the License at\n+ *\n+ *    http://www.apache.org/licenses/LICENSE-2.0\n+ *\n+ * Unless required by applicable law or agreed to in writing, software\n+ * distributed under the License is distributed on an "AS IS" BASIS,\n+ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n+ * See the License for the specific language governing permissions and\n+ * limitations under the License.\n+ */\n+\n+package org.apache.linkis.metadata.query.service;\n+\n+import org.apache.linkis.datasourcemanager.common.util.json.Json;\n+import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;\n+import org.apache.linkis.metadata.query.common.service.AbstractDbMetaService;\n+import org.apache.linkis.metadata.query.common.service.MetadataConnection;\n+import org.apache.linkis.metadata.query.service.conf.SqlParamsMapper;\n+import org.apache.linkis.metadata.query.service.oscar.SqlConnection;\n+\n+import java.sql.SQLException;\n+import java.util.HashMap;\n+import java.util.List;\n+import java.util.Map;\n+\n+public class OscarMetaService extends AbstractDbMetaService<SqlConnection> {\n+\n+  @Override\n+  public MetadataConnection<SqlConnection> getConnection(\n+      String operator, Map<String, Object> params) throws Exception {\n+    String host =\n+        String.valueOf(params.getOrDefault(SqlParamsMapper.PARAM_SQL_HOST.getValue(), ""));\n+    Integer port =\n+        (Double.valueOf(\n+                String.valueOf(params.getOrDefault(SqlParamsMapper.PARAM_SQL_PORT.getValue(), 0))))\n+            .intValue();\n+    String username =\n+        String.valueOf(params.getOrDefault(SqlParamsMapper.PARAM_SQL_USERNAME.getValue(), ""));\n+    String password =\n+        String.valueOf(params.getOrDefault(SqlParamsMapper.PARAM_SQL_PASSWORD.getValue(), ""));\n+\n+    String database =\n+        String.valueOf(params.getOrDefault(SqlParamsMapper.PARAM_SQL_DATABASE.getValue(), ""));\n+    Map<String, Object> extraParams = new HashMap<>();\n+    Object sqlParamObj = params.get(SqlParamsMapper.PARAM_SQL_EXTRA_PARAMS.getValue());\n+    if (null != sqlParamObj) {\n+      if (!(sqlParamObj instanceof Map)) {\n+        extraParams =\n+            Json.fromJson(String.valueOf(sqlParamObj), Map.class, String.class, Object.class);\n+      } else {\n+        extraParams = (Map<String, Object>) sqlParamObj;\n+      }\n+    }\n+    assert extraParams != null;\n+    return new MetadataConnection<>(\n+        new SqlConnection(host, port, username, password, database, extraParams));\n+  }\n+\n+  @Override\n+  public List<String> queryDatabases(SqlConnection connection) {\n+    try {\n+      return connection.getAllDatabases();\n+    } catch (SQLException e) {\n+      throw new RuntimeException("Fail to get Sql databases(获取数据库列表失败)", e);\n+    }\n+  }\n+\n+  @Override\n+  public List<String> queryTables(SqlConnection connection, String schemaname) {\n+    try {\n+      return connection.getAllTables(schemaname);\n+    } catch (SQLException e) {\n+      throw new RuntimeException("Fail to get Sql tables(获取表列表失败)", e);\n+    }\n+  }\n+\n+  @Override\n+  public List<MetaColumnInfo> queryColumns(\n+      SqlConnection connection, String schemaname, String table) {\n+    try {\n+      return connection.getColumns(schemaname, table);\n+    } catch (SQLException | ClassNotFoundException e) {\n+      throw new RuntimeException("Fail to get Sql columns(获取字段列表失败)", e);\n+    }\n+  }\n+}\ndiff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/oscar/SqlConnection.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/oscar/SqlConnection.java\nnew file mode 100644\nindex 000000000..8fb3aa2be\n--- /dev/null\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/oscar/SqlConnection.java\n@@ -0,0 +1,223 @@\n+/*\n+ * Licensed to the Apache Software Foundation (ASF) under one or more\n+ * contributor license agreements.  See the NOTICE file distributed with\n+ * this work for additional information regarding copyright ownership.\n+ * The ASF licenses this file to You under the Apache License, Version 2.0\n+ * (the "License"); you may not use this file except in compliance with\n+ * the License.  You may obtain a copy of the License at\n+ *\n+ *    http://www.apache.org/licenses/LICENSE-2.0\n+ *\n+ * Unless required by applicable law or agreed to in writing, software\n+ * distributed under the License is distributed on an "AS IS" BASIS,\n+ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n+ * See the License for the specific language governing permissions and\n+ * limitations under the License.\n+ */\n+\n+package org.apache.linkis.metadata.query.service.oscar;\n+\n+import org.apache.linkis.common.conf.CommonVars;\n+import org.apache.linkis.common.utils.AESUtils;\n+import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;\n+\n+import org.apache.commons.lang3.StringUtils;\n+\n+import java.io.Closeable;\n+import java.io.IOException;\n+import java.sql.*;\n+import java.util.*;\n+import java.util.stream.Collectors;\n+\n+import org.slf4j.Logger;\n+import org.slf4j.LoggerFactory;\n+\n+public class SqlConnection implements Closeable {\n+\n+  private static final Logger LOG = LoggerFactory.getLogger(SqlConnection.class);\n+\n+  private static final CommonVars<String> SQL_DRIVER_CLASS =\n+      CommonVars.apply("wds.linkis.server.mdm.service.oscar.driver", "com.oscar.Driver");\n+\n+  private static final CommonVars<String> SQL_CONNECT_URL =\n+      CommonVars.apply("wds.linkis.server.mdm.service.oscar.url", "jdbc:oscar://%s:%s/%s");\n+\n+  private Connection conn;\n+\n+  private ConnectMessage connectMessage;\n+\n+  public SqlConnection(\n+      String host,\n+      Integer port,\n+      String username,\n+      String password,\n+      String database,\n+      Map<String, Object> extraParams)\n+      throws ClassNotFoundException, SQLException {\n+    connectMessage = new ConnectMessage(host, port, username, password, extraParams);\n+    conn = getDBConnection(connectMessage, database);\n+    Statement statement = conn.createStatement();\n+    statement.close();\n+  }\n+\n+  public List<String> getAllDatabases() throws SQLException {\n+    List<String> schemaNames = new ArrayList<>();\n+    Statement stmt = null;\n+    ResultSet rs = null;\n+    try {\n+      stmt = conn.createStatement();\n+      rs = stmt.executeQuery("SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME");\n+      while (rs.next()) {\n+        schemaNames.add(rs.getString(1));\n+      }\n+    } finally {\n+      closeResource(null, stmt, rs);\n+    }\n+    return schemaNames;\n+  }\n+\n+  public List<String> getAllTables(String schema) throws SQLException {\n+    List<String> tableNames = new ArrayList<>();\n+    Statement stmt = null;\n+    ResultSet rs = null;\n+    try {\n+      stmt = conn.createStatement();\n+      rs =\n+          stmt.executeQuery(\n+              "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = \'"\n+                  + schema\n+                  + "\' ORDER BY TABLE_NAME");\n+      while (rs.next()) {\n+        tableNames.add(rs.getString(1));\n+      }\n+    } finally {\n+      closeResource(null, stmt, rs);\n+    }\n+    return tableNames;\n+  }\n+\n+  public List<MetaColumnInfo> getColumns(String schema, String table)\n+      throws SQLException, ClassNotFoundException {\n+    List<MetaColumnInfo> columns = new ArrayList<>();\n+    String columnSql = "SELECT * FROM \\"" + schema + "\\".\\"" + table + "\\" WHERE 1 = 2";\n+    PreparedStatement ps = null;\n+    ResultSet rs = null;\n+    ResultSetMetaData meta = null;\n+    try {\n+      List<String> primaryKeys = getPrimaryKeys(schema, table);\n+      Map<String, String> columnCommentMap = getColumnComment(schema, table);\n+      ps = conn.prepareStatement(columnSql);\n+      rs = ps.executeQuery();\n+      meta = rs.getMetaData();\n+      int columnCount = meta.getColumnCount();\n+      for (int i = 1; i < columnCount + 1; i++) {\n+        MetaColumnInfo info = new MetaColumnInfo();\n+        info.setIndex(i);\n+        info.setLength(meta.getColumnDisplaySize(i));\n+        info.setName(meta.getColumnName(i));\n+        info.setType(meta.getColumnTypeName(i));\n+        if (primaryKeys.contains(meta.getColumnName(i))) {\n+          info.setPrimaryKey(true);\n+        }\n+        String colComment = columnCommentMap.get(meta.getColumnName(i));\n+        if (StringUtils.isNotBlank(colComment)) {\n+          info.setColComment(colComment);\n+        } else {\n+          info.setColComment(StringUtils.EMPTY);\n+        }\n+        columns.add(info);\n+      }\n+    } finally {\n+      closeResource(null, ps, rs);\n+    }\n+    return columns;\n+  }\n+\n+  private List<String> getPrimaryKeys(String schema, String table) throws SQLException {\n+    ResultSet rs = null;\n+    List<String> primaryKeys = new ArrayList<>();\n+    DatabaseMetaData dbMeta = conn.getMetaData();\n+    rs = dbMeta.getPrimaryKeys(null, schema, table);\n+    while (rs.next()) {\n+      primaryKeys.add(rs.getString("COLUMN_NAME"));\n+    }\n+    return primaryKeys;\n+  }\n+\n+  private Map<String, String> getColumnComment(String schema, String table) throws SQLException {\n+    ResultSet rs = null;\n+    Map<String, String> columnComment = new HashMap<>();\n+    DatabaseMetaData dbMeta = conn.getMetaData();\n+    rs = dbMeta.getColumns(null, schema, table, "%");\n+    while (rs.next()) {\n+      columnComment.put(rs.getString("COLUMN_NAME"), rs.getString("REMARKS"));\n+    }\n+    return columnComment;\n+  }\n+\n+  private void closeResource(Connection connection, Statement statement, ResultSet resultSet) {\n+    try {\n+      if (null != resultSet && !resultSet.isClosed()) {\n+        resultSet.close();\n+      }\n+      if (null != statement && !statement.isClosed()) {\n+        statement.close();\n+      }\n+      if (null != connection && !connection.isClosed()) {\n+        connection.close();\n+      }\n+    } catch (SQLException e) {\n+      LOG.warn("Fail to release resource [" + e.getMessage() + "]", e);\n+    }\n+  }\n+\n+  @Override\n+  public void close() throws IOException {\n+    closeResource(conn, null, null);\n+  }\n+\n+  private Connection getDBConnection(ConnectMessage connectMessage, String database)\n+      throws ClassNotFoundException, SQLException {\n+    String extraParamString =\n+        connectMessage.extraParams.entrySet().stream()\n+            .map(e -> String.join("=", e.getKey(), String.valueOf(e.getValue())))\n+            .collect(Collectors.joining("&"));\n+    Class.forName(SQL_DRIVER_CLASS.getValue());\n+    String url =\n+        String.format(\n+            SQL_CONNECT_URL.getValue(), connectMessage.host, connectMessage.port, database);\n+    if (!connectMessage.extraParams.isEmpty()) {\n+      url += "?" + extraParamString;\n+    }\n+    try {\n+      Properties prop = new Properties();\n+      prop.put("user", connectMessage.username);\n+      prop.put("password", AESUtils.isDecryptByConf(connectMessage.password));\n+      return DriverManager.getConnection(url, prop);\n+    } catch (Exception e) {\n+      LOG.error("Fail to create Oscar connection", e);\n+      throw e;\n+    }\n+  }\n+\n+  private static class ConnectMessage {\n+    private String host;\n+    private Integer port;\n+    private String username;\n+    private String password;\n+    private Map<String, Object> extraParams;\n+\n+    public ConnectMessage(\n+        String host,\n+        Integer port,\n+        String username,\n+        String password,\n+        Map<String, Object> extraParams) {\n+      this.host = host;\n+      this.port = port;\n+      this.username = username;\n+      this.password = password;\n+      this.extraParams = extraParams;\n+    }\n+  }\n+}\ndiff --git a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/cache/CacheConfiguration.java b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/cache/CacheConfiguration.java\nindex bfff0fe92..3fce2d5ee 100644\n--- a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/cache/CacheConfiguration.java\n+++ b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/cache/CacheConfiguration.java\n@@ -34,7 +34,7 @@ public class CacheConfiguration {\n   public static final CommonVars<String> MYSQL_RELATIONSHIP_LIST =\n       CommonVars.apply(\n           "wds.linkis.server.mdq.mysql.relationship",\n-          "mysql,oracle,kingbase,postgresql,sqlserver,db2,greenplum,dm,doris,clickhouse,tidb,starrocks,gaussdb,oceanbase");\n+          "mysql,oracle,kingbase,postgresql,sqlserver,db2,greenplum,dm,doris,clickhouse,tidb,starrocks,gaussdb,oceanbase,oscar");\n \n   public static final CommonVars<String> QUERY_DATABASE_RELATIONSHIP =\n       CommonVars.apply(', 'is_error': False}]

**AI**：
[思考] 好的，从变更信息来看：

1. 变更文件主要在 `linkis-public-enhancements/linkis-datasource/` 和 `linkis-dist/` 目录
2. 提交历史显示有5个commit：
   - 修复oscar链接异常
   - add orsca ds
   - 优化EsMetaService.queryColumns（递归展开ES mappings嵌套object字段）
   - 优化HiveMetaService.queryTableProps（使用占位表名__DB_DEFAULT__）
   - 优化HiveMetaService.queryTableProps（表名为空时返回库参数）

3. 新增了 OscarMetaService 和 Oscar/SqlConnection 类
4. 修改了 ES、Hive 的元数据服务
5. 修改了缓存配置

从文件路径分析：
- 主要模块：PES (public-enhancements)
- 二级模块：datasource（特别是 metadata-query）
- 涉及的功能：Oscar 数据源支持、ES 和 Hive 元数据查询优化

变更类型分析：
- 新增了 Oscar 数据库支持（新功能）→ feat
- 修复了 oscar 链接异常 → fix
- 优化了 ES 和 Hive 元数据查询 → refactor/enhance

由于主要是新增 Oscar 数据源支持，action 应该是 feat。

让我获取详细的代码差异来更好地理解变更内容。...


## 交互记录 (2026-06-24T02:12:26.580Z)

**用户**：
[{'tool_use_id': 'call_474dc50587154c01ade1ce0e', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] 从代码差异来看，这个分支包含了多个变更：

1. **数据库DDL变更**（linkis_dml.sql）：
   - 添加了 Oscar（神通数据库）数据源类型和配置参数

2. **ElasticSearch 元数据查询优化**：
   - 将 FIELD_PROPS 改为公开常量
   - 添加了递归展开嵌套 object 字段的功能

3. **Hive 元数据查询增强**：
   - 添加了占位表名 __DB_DEFAULT__ 来触发库参数查询
   - 当表名为空或占位符时，返回库参数及常用元数据

4. **新增 Oscar 数据源支持**：
   - OscarMetaService.java（新增）
   - oscar/SqlConnection.java（新增）
   - 添加了完整的 Oscar 数据库连接和元数据查询支持

5. **缓存配置更新**：
   - 在 MYSQL_RELATIONSHIP_LIST 中添加了 oscar

从整体来看，这个 PR 主要是：
- **主要变更**：新增 Oscar（神通数据库）数据源支持
- **次要变更**：优化 ES 和 Hive 元数据查询功能
- **Action 类型**：feat（新功能）

模块识别：
- 一级模块：PES（public-enhancements）
- 二级模块：datasource（特别是 metadata-query）

现在搜索相关的 GitHub Issues。...


## 交互记录 (2026-06-24T02:12:40.063Z)

**用户**：
[{'tool_use_id': 'call_54af15cf46b64168a6568fde', 'type': 'tool_result', 'content': '1029\tOPEN\t[fix][MGS][gateway] fix compilation errors in security filter and adapt to inline version\tbug\t2026-05-12T08:48:55Z', 'is_error': False}]

**AI**：
现在让我搜索相关的 GitHub Issues：...


## 交互记录 (2026-06-24T02:12:48.771Z)

**用户**：
[{'tool_use_id': 'call_2ab05e9841654be8b8942bc1', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] 没有找到相关的 open issues。让我尝试其他搜索关键词。...


## 交互记录 (2026-06-24T02:13:11.801Z)

**用户**：
[{'tool_use_id': 'call_8ac8c5af6eb34753af954ebf', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/issues/1069', 'is_error': False}]

**AI**：
[思考] 找到了一个 issue #1029，但是它是关于 MGS gateway 的编译错误修复，与我们的 oscar 数据源支持无关。

让我再搜索一下是否有其他相关 issues。...


## 交互记录 (2026-06-24T02:13:40.166Z)

**用户**：
[{'tool_use_id': 'call_ababb00e592a4406bc5c752d', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/pull/1070', 'is_error': False}]

**AI**：
[思考] 没有找到相关的 open issues。由于这是一个新功能（添加 Oscar 数据源支持），我应该创建一个新的 Issue。

让我总结一下变更内容：

**主要变更**：
1. 新增 Oscar（神通数据库）数据源支持
   - 新增 OscarMetaService.java
   - 新增 oscar/SqlConnection.java
   - 添加数据库DDL配置（数据源类型和参数）
   - 更新缓存配置列表

2. 优化 ES 元数据查询
   - 递归展开嵌套 object 字段
   - FIELD_PROPS 改为公开常量

3. 增强 Hive 元数据查询
   - 使用占位表名 __DB_DEFAULT__ 触发库参数查询
   - 表名为空时返回库参数及常用元数据

**Action 类型**：feat（新功能）
**模块**：PES / datasource
**Issue 标题**：[feature][PES][datasource] add support for Oscar database (神通数据库) in metadata query service

让我创建 Issue。...

