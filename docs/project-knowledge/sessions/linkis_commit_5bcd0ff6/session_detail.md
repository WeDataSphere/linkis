
## 交互记录 (2026-08-12T08:07:34.746Z)

**用户**：
<command-message>pr</command-message>
<command-name>/pr</command-name>
<command-args>dev-2.1.0-hive-share 提交pr 到dev-2.1.0-webank</command-args>

**AI**：
Failed to authenticate. API Error: 401 身份验证失败。...


## 交互记录 (2026-08-12T08:07:34.746Z)

**用户**：
[{'type': 'text', 'text': 'Base directory for this skill: C:\\Users\\v_kkhuang\\.claude\\skills\\pr\n\n# GitHub PR 自动创建器 (Apache Linkis 版)\n\n## ⚠️ 强制要求（MUST READ）\n\n**执行此技能时，必须严格遵守以下规则，违反任何一项都将导致输出不合格：**\n\n1. **Issue 处理优先**: 必须先搜索/创建 Issue，然后才能创建 PR。绝不能跳过 Issue 步骤直接创建 PR。\n2. **标题格式**: PR 标题和 Issue 标题必须严格遵循格式，大小写不能错。\n3. **内容全英文**: PR 和 Issue 的所有内容必须是英文，不能有中文。\n4. **三段式描述**: PR 的 "What is the purpose" 必须使用三段式格式（Background/Problem, Purpose, Value/Impact）。\n5. **Issue 关联**: PR 创建时必须关联 Issue（使用 `close #issue_id`）。\n6. **必须询问**: 找到相关 Issues 时必须询问用户选择，不能自动决定。\n7. **不允许 commit**: 只创建 PR 和 Issue，不执行任何 git commit 操作。\n\n---\n\n## 工作流程（按顺序执行）\n\n```\n┌─────────────────────────────────────────────────────────────────────────────┐\n│                        Apache Linkis PR 创建流程                              │\n├─────────────────────────────────────────────────────────────────────────────┤\n│  Step 1: 询问分支信息（base + target）                                        │\n│     ↓                                                                        │\n│  Step 2: 获取代码变更（git diff + git log）                                   │\n│     ↓                                                                        │\n│  Step 3: 分析变更，识别模块（一级 + 二级）                                    │\n│     ↓                                                                        │\n│  Step 4: 搜索 GitHub Issues（gh issue list）                                 │\n│     ↓                                                                        │\n│  Step 5: 询问用户选择 Issue（复用现有 OR 创建新 Issue）                       │\n│     ↓                                                                        │\n│  Step 6: 如果需要，创建新 Issue（gh issue create）                            │\n│     ↓                                                                        │\n│  Step 7: 生成 PR 标题（严格格式）                                              │\n│     ↓                                                                        │\n│  Step 8: 生成 PR 描述（三段式 + Brief change log + Checklist）                │\n│     ↓                                                                        │\n│  Step 9: 创建 PR 并关联 Issue（gh pr create）                                 │\n└─────────────────────────────────────────────────────────────────────────────┘\n```\n\n---\n\n## 执行步骤详解\n\n### Step 1: 询问分支信息\n\n**必须询问用户以下信息：**\n\n```\n请输入以下信息以创建 Pull Request：\n\n1. 基础分支（base branch）: 即要合并的目标分支（例如: apachemaster, dev-1.19.0）\n2. 目标分支（target branch）: 即你的特性分支（例如: dev-1.19.0-feature-xxx）\n\n请确认这是用于创建 Pull Request，而非其他操作。\n```\n\n**验证规则**：\n- 必须确认用户意图是创建 PR\n- 如果分支不存在，列出可用分支供用户选择\n- 使用 `git branch -r` 列出远程分支\n\n---\n\n### Step 2: 获取代码变更\n\n**执行以下命令获取变更信息：**\n\n```bash\n# 获取变更文件列表\ngit diff --name-status <base_branch>..<target_branch>\n\n# 获取提交历史\ngit log <base_branch>..<target_branch> --oneline --no-merges\n\n# 获取详细的代码差异（用于分析）\ngit diff <base_branch>..<target_branch> --stat\n```\n\n**分析内容**：\n- 变更的文件数量和类型\n- 变更的代码行数\n- 提交的数量和主题\n\n---\n\n### Step 3: 分析变更，识别模块\n\n**从变更文件路径中提取模块信息：**\n\n#### 一级模块映射表（严格使用）\n\n| 目录名 | 缩写 | 全称 |\n|--------|------|------|\n| `linkis-spring-cloud-services` | `MGS` | microservice-governance |\n| `linkis-computation-governance` | `CGS` | computation-governance |\n| `linkis-public-enhancements` | `PES` | public-enhancements |\n| `linkis-engineconn-plugins` | `EC` | engineconn |\n| `linkis-extensions` | `ETS` | extensions |\n| `linkis-orchestrator` | `ORC` | orchestrator |\n| `linkis-web` | `WEB` | web |\n| `linkis-commons` | `COM` | commons |\n\n#### 二级模块提取规则\n\n| 一级模块 | 二级模块提取方式 | 示例 |\n|---------|-----------------|------|\n| MGS | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-gateway` → `gateway` |\n| CGS | `linkis-cg-xxx` 中的 xxx 部分（小写）或 `linkis-xxx` 去掉前缀 | `linkis-entrance` → `entrance` |\n| PES | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-datasource` → `datasource` |\n| EC | 引擎名称（小写） | `spark` → `spark` |\n| WEB | 通常无二级模块，使用 `frontend` 或省略 | - |\n| COM | 目录名（小写） | `common` → `common` |\n\n**模块识别逻辑**：\n1. 如果所有变更文件在同一模块，使用该模块\n2. 如果跨越多个模块，选择变更最多的模块作为主模块\n3. 如果变更分散且数量相近，询问用户确认主模块\n\n---\n\n### Step 4: 搜索 GitHub Issues\n\n**必须执行 Issue 搜索，不能跳过：**\n\n```bash\n# 搜索 open issues，使用模块和变更关键词\ngh issue list --state open --search "<模块名> <关键词>" --limit 20\n\n# 例如：\n# gh issue list --state open --search "PES datasource filter" --limit 20\n# gh issue list --state open --search "CGS entrance location" --limit 20\n```\n\n**搜索关键词提取**：\n- 模块名（一级 + 二级）\n- 变更相关的关键词（从文件名、提交信息中提取）\n- action 类型（feat/fix 等）\n\n---\n\n### Step 5: 询问用户选择 Issue\n\n#### 如果找到相关 Issues（至少 1 个）\n\n**必须询问用户，不能自动选择：**\n\n```\n找到以下相关的 open issues：\n\n#123: [bug][PES][datasource] add support for getting published datasource by type and proxy user\n#456: [feature][CGS][entrance] hive location clause security check\n\n请选择：\n1. 使用现有 issue #123\n2. 使用现有 issue #456\n3. 创建新 issue\n4. 手动输入其他 issue 编号\n\n请输入选项 (1/2/3/4) 或 issue 编号:\n```\n\n#### 如果没有找到相关 Issues\n\n**直接进入创建新 Issue 流程**（Step 6）\n\n---\n\n### Step 6: 创建新 Issue（如需要）\n\n**如果用户选择创建新 Issue，或没有找到相关 Issue，则执行以下步骤：**\n\n#### Issue 标题格式（严格遵循）\n\n```\n[类型][一级模块][二级模块] 功能描述\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 示例 |\n|------|------|------|\n| `类型` | **小写** | `bug`, `feature`, `ask` |\n| `一级模块` | **大写** | `MGS`, `CGS`, `PES`, `EC`, `ETS`, `ORC`, `WEB`, `COM` |\n| `二级模块` | **小写** | `entrance`, `datasource`, `spark` 等 |\n| `功能描述` | **小写句子** | 首字母不大写，使用普通英文句子 |\n\n**类型选择规则**：\n- PR action 为 `fix` → Issue 类型为 `bug`\n- PR action 为 `feat` → Issue 类型为 `feature`\n- 其他情况 → Issue 类型为 `feature`\n\n**Issue 标题示例**：\n```\n✅ 正确: [bug][CGS][entrance] hive tasks should not be allowed to use location clause for security reasons\n✅ 正确: [feature][PES][datasource] add support for getting published datasource by type and proxy user\n❌ 错误: [Bug][CGS][Entrance] Hive Tasks Should Not Be Allowed... (大小写错误)\n❌ 错误: [bug][cgs][entrance] ... (一级模块小写错误)\n```\n\n#### Issue 内容格式（严格遵循）\n\n**根据 Issue 类型选择对应模板：**\n\n##### Bug Report 模板（fix 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar issues.\n\n### Linkis Component\n\n- [ ] linkis-commons\n- [ ] linkis-computation-governance\n- [ ] linkis-dist\n- [ ] linkis-engineconn-plugin\n- [ ] linkis-extensions\n- [ ] linkis-orchestrator\n- [x] linkis-public-enhancements\n- [ ] linkis-spring-cloud-services\n- [ ] linkis-web\n\n### Description\n\n[Brief description of the bug - what happened and what should happen]\n\n### Steps to reproduce\n\n1. Step 1...\n2. Step 2...\n3. Step 3...\n\n### Expected behavior\n\n[Describe what you expected to happen]\n\n### Your environment\n\n- Linkis version used: [x.x.x]\n- Environment name and version:\n    - hadoop-x.x.x\n    - hive-x.x.x\n    - spark-x.x.x\n    - scala-x.x.x\n    - jdk 1.8.x_xx\n\n### Anything else\n\n[Additional context, logs, frequency of occurrence, etc.]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n##### Feature Request 模板（feat 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar feature requirement.\n\n### Problem Description\n\nIs your feature request related to a problem? Please describe.\n\n[Describe the problem or need]\n\n### Description\n\nA short description of your feature\n\n[Clear and concise description of what you want to add]\n\n### Use case\n\nDescribe the use case of your feature request and the solution you\'d like.\n\n[Describe the specific use case and expected behavior]\n\n### Solutions\n\nDescribe alternatives you\'ve considered.\n\n[Describe any alternative solutions or workarounds you\'ve considered]\n\n### Anything else\n\nAdditional context or screenshots about the feature request.\n\n[Any additional information, screenshots, or examples]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n#### Issue 标签规则\n\n| Issue 类型 | 标签 |\n|-----------|------|\n| `bug` | `bug`, `status: needs triage` |\n| `feature` | `enhancement`, `status: needs triage` |\n| `ask` | `question`, `status: needs triage` |\n\n#### 创建 Issue 命令\n\n```bash\ngh issue create \\\n  --title "[<type>][<MODULE>][<submodule>] <description>" \\\n  --body "<issue_content>" \\\n  --label "<label1>,<label2>"\n```\n\n**保存返回的 issue_id，用于 PR 关联！**\n\n---\n\n### Step 7: 生成 PR 标题\n\n**PR 标题格式（严格遵循）**：\n\n```\n[action][一级模块][二级模块] 一句话总结\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 可选值 | 示例 |\n|------|------|--------|------|\n| `action` | **小写** | feat, fix, perf, refactor, docs, style, test, chore | `feat`, `fix` |\n| `一级模块` | **大写** | MGS, CGS, PES, EC, ETS, ORC, WEB, COM | `PES`, `CGS` |\n| `二级模块` | **小写** | 具体服务名称 | `datasource`, `entrance` |\n| `一句话总结` | **小写句子** | 普通英文，首字母不大写 | `add support for filtering by type` |\n\n**Action 类型定义**：\n\n| Action | 含义 | 使用场景 |\n|--------|------|----------|\n| `feat` | 新功能 | 添加新的功能、API、模块 |\n| `fix` | 修复 Bug | 修复问题、缺陷 |\n| `perf` | 性能优化 | 提升性能、优化资源使用 |\n| `refactor` | 重构 | 代码重构，不改变功能 |\n| `docs` | 文档 | 仅文档变更 |\n| `style` | 代码风格 | 格式化、代码风格调整 |\n| `test` | 测试 | 仅测试相关变更 |\n| `chore` | 构建/工具 | 构建、依赖、工具链更新 |\n\n**PR 标题示例**：\n\n```\n✅ 正确: [feat][PES][datasource] add support for getting published datasource by type and proxy user\n✅ 正确: [fix][CGS][entrance] resolve null pointer exception in engine reuse metrics update\n✅ 正确: [perf][CGS][application-manager] optimize engine creation performance\n❌ 错误: [Feat][PES][datasource] Add Support For... (大小写错误)\n❌ 错误: [feat][pES][datasource] ... (一级模块大小写混合)\n❌ 错误: feat: add support for... (缺少模块标签)\n```\n\n---\n\n### Step 8: 生成 PR 描述\n\n**PR 描述必须严格遵循以下格式：**\n\n#### PR 描述模板（完整格式）\n\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\n[Describe the background or problem - what issue exists, 2-3 sentences]\n\n**Purpose of Change:**\n[Describe the purpose of this PR - what this PR does to solve the problem, 2-3 sentences]\n\n**Value/Impact:**\n[Describe the value or impact - what benefits this change brings, 1-2 sentences]\n\n### Related issues/PRs\n\nRelated issues: close #<issue_id>\nRelated pr:none\n\n### Brief change log\n\n- [Change point 1 - start with verb]\n- [Change point 2 - start with verb]\n- [Change point 3 - start with verb]\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n#### "What is the purpose" 格式要求（严格三段式）\n\n**必须使用三段式结构，不能合并或省略：**\n\n| 段落 | 标题 | 内容要求 | 长度 |\n|------|------|----------|------|\n| 第一段 | **Background/Problem:** | 描述背景或问题 - 当前存在什么问题或需求 | 2-3 句话 |\n| 第二段 | **Purpose of Change:** | 描述改动的目的 - 本 PR 如何解决这个问题 | 2-3 句话 |\n| 第三段 | **Value/Impact:** | 描述价值或影响 - 改动带来的好处 | 1-2 句话 |\n\n**格式规范**：\n- ✅ 每段标题使用粗体，以冒号结尾\n- ✅ 段落标题使用 `**Title:**` 格式\n- ✅ 内容另起一行，不要和标题在同一行\n- ❌ 不要使用 "This PR adds..." 或 "This PR implements..." 格式\n- ✅ 使用客观描述，如 "This PR adds...", "The change enables..."\n\n**示例**：\n\n```\n✅ 正确格式:\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n❌ 错误格式:\n\nThis PR adds support for filtering datasources by type and user. (缺少三段式)\nThe problem is that users cannot filter datasources. (不完整)\n```\n\n#### "Brief change log" 格式要求\n\n**每条变更记录必须：**\n- 以动词开头\n- 使用现在时\n- 简洁明确\n- 每条独立一行\n\n**常用动词示例**：\n- `Add` - 添加新功能\n- `Fix` - 修复问题\n- `Update` - 更新现有功能\n- `Remove` - 删除功能\n- `Refactor` - 重构代码\n- `Optimize` - 优化性能\n- `Improve` - 改进\n\n**示例**：\n\n```\n✅ 正确:\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n❌ 错误:\n- Added API... (过去时)\n- Update the service... (不明确)\n```\n\n#### Checklist 智能勾选规则\n\n| 选项 | 何时勾选 | 说明 |\n|------|---------|------|\n| I have read the Contributing Guidelines | ✅ 始终勾选 | - |\n| I have explained the need for this PR | ✅ 始终勾选 | - |\n| I have explained the changes | ✅ 有代码变更时勾选 | 大多数情况勾选 |\n| I have added tests | 有测试时勾选 | 如果添加了测试代码 |\n| I have updated the documentation | 有文档变更时勾选 | 如果修改了文档 |\n| I have verified backward compatibility | ✅ 通常勾选 | 除非是破坏性变更 |\n| I have written unit tests | 有完整测试时勾选 | 如果有完整的单元测试 |\n\n---\n\n### Step 9: 创建 PR\n\n**执行 PR 创建命令：**\n\n```bash\ngh pr create \\\n  --title "<PR_TITLE>" \\\n  --body "<PR_BODY>" \\\n  --base "<base_branch>" \\\n  --head "<target_branch>" \\\n  --label "<label1>,<label2>"\n```\n\n**PR 标签映射规则**：\n\n| Action 类型 | Label |\n|-----------|-------|\n| `feat` | `enhancement` |\n| `fix` | `bug` |\n| `perf` | `performance` |\n| `refactor` | `refactor` |\n| `docs` | `documentation` |\n| `style` | `style` |\n| `test` | `test` |\n| `chore` | `build` |\n\n---\n\n## 错误处理\n\n| 错误场景 | 处理方式 |\n|---------|----------|\n| 找不到 git 仓库 | 提示用户在正确的项目目录中执行 |\n| gh 未登录 | 提示用户运行 `gh auth login` |\n| 分支不存在 | 列出可用分支供用户选择 |\n| 未找到代码变更 | 提示用户检查分支是否正确 |\n| Issue 搜索失败 | 提示用户手动关联 Issue |\n| PR 创建失败 | 显示错误信息，提示用户检查权限或网络 |\n\n---\n\n## 完整示例\n\n### 示例 1: 新增功能（创建新 Issue）\n\n**场景**: 添加数据源过滤功能，无相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-public-enhancements/linkis-datasource/src/...`\n- 模块: `PES` / `datasource`\n- Action: `feat`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "PES datasource filter" --limit 20\n```\n结果: 无相关 Issues\n\n**Step 6**: 创建新 Issue\n```bash\ngh issue create \\\n  --title "[feature][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --label "enhancement,status: needs triage"\n```\n返回: Issue #789\n\n**Step 7**: 生成 PR 标题\n```\n[feat][PES][datasource] add support for getting published datasource by type and proxy user\n```\n\n**Step 8**: 生成 PR 描述\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n### Related issues/PRs\n\nRelated issues: close #789\nRelated pr:none\n\n### Brief change log\n\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[feat][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --base "apachemaster" \\\n  --head "dev-1.19.0-feature-datasource-filter" \\\n  --label "enhancement"\n```\n\n---\n\n### 示例 2: 修复 Bug（复用现有 Issue）\n\n**场景**: 修复空指针异常，找到相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-computation-governance/linkis-cg-linkismanager/src/...`\n- 模块: `CGS` / `linkismanager` (简化为 `application-manager`)\n- Action: `fix`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "CGS application-manager null pointer" --limit 20\n```\n结果: 找到 Issue #456\n\n**Step 5**: 询问用户\n```\n找到以下相关的 open issues：\n\n#456: [bug][CGS][application-manager] null pointer exception in engine reuse metrics update causing monitoring data loss\n\n请选择：\n1. 使用现有 issue #456\n2. 创建新 issue\n3. 手动输入其他 issue 编号\n```\n用户选择: 1\n\n**Step 6**: 跳过（复用现有 Issue #456）\n\n**Step 7**: 生成 PR 标题\n```\n[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update\n```\n\n**Step 8**: 生成 PR 描述（使用 Bug Report 风格的三段式）\n```markdown\n### What is the purpose of the change\n\n**Background/Problem:**\nDuring engine creation and reuse, when updating metrics information, a null pointer exception occurs, causing some monitoring data to be lost and affecting system stability.\n\n**Purpose of Change:**\nTo address this problem, this PR adds null checks and protection to the relevant code, ensuring the robustness of metrics updates.\n\n**Value/Impact:**\nAfter the change, the system runs stably with complete and available monitoring data, improving system reliability.\n\n### Related issues/PRs\n\nRelated issues: close #456\nRelated pr:none\n\n### Brief change log\n\n- Add null pointer checks in DefaultEngineCreateService\n- Add null pointer checks in DefaultEngineReuseService\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update" \\\n  --body "..." \\\n  --base "dev-1.19.0" \\\n  --head "dev-1.19.0-fix-null-pointer" \\\n  --label "bug"\n```\n\n---\n\n## 快速参考\n\n### 大小写规则速查\n\n| 位置 | PR | Issue |\n|------|-----|-------|\n| 类型/Action | **小写** | **小写** |\n| 一级模块 | **大写** | **大写** |\n| 二级模块 | **小写** | **小写** |\n| 描述/总结 | **小写句子** | **小写句子** |\n\n### 格式速查\n\n```\nPR 标题:    [action][MODULE][submodule] description\nIssue 标题:  [type][MODULE][submodule] description\n\nPR action:   feat, fix, perf, refactor, docs, style, test, chore\nIssue type:  bug, feature, ask\n\nMODULE:      MGS, CGS, PES, EC, ETS, ORC, WEB, COM\n```\n\n### 命令速查\n\n```bash\n# 搜索 Issues\ngh issue list --state open --search "<关键词>" --limit 20\n\n# 创建 Issue\ngh issue create --title "<标题>" --body "<内容>" --label "<标签>"\n\n# 创建 PR\ngh pr create --title "<标题>" --body "<内容>" --base "<base>" --label "<标签>"\n\n# 列出分支\ngit branch -r\n```\n\n---\n\n## 使用方法\n\n```bash\n/pr\n```\n\n然后按照提示输入分支信息即可。\n\n---\n\n## 依赖工具\n\n- `git` - 版本控制\n- `gh` (GitHub CLI) - GitHub 操作\n\n确保 `gh` 已登录：`gh auth login`\n\n\nARGUMENTS: dev-2.1.0-hive-share 提交pr 到dev-2.1.0-webank'}]

**AI**：
No response requested....


## 交互记录 (2026-08-12T08:44:41.753Z)

**用户**：
[{'type': 'text', 'text': 'Continue from where you left off.'}]

**AI**：
...


## 交互记录 (2026-08-12T08:44:46.296Z)

**用户**：
<command-message>pr</command-message>
<command-name>/pr</command-name>
<command-args>dev-2.1.0-hive-share 提交pr 到dev-2.1.0-webank</command-args>

**AI**：
[思考] The user wants to create a PR from `dev-2.1.0-hive-share` branch to `dev-2.1.0-webank` branch. Let me follow the workflow:

1. Current branch: `dev-2.1.0-hive-share` (target/head branch)
2. Base branch: `dev-2.1.0-webank`

Let me start by getting the code changes between the base and head branches. First, let me verify the branches exist and get the diff.

Let me execute the steps in parallel where possible:
- Get the diff between branches
- Get the commit log
- Get the stat

Let me start....


## 交互记录 (2026-08-12T08:44:46.297Z)

**用户**：
[{'type': 'text', 'text': 'Base directory for this skill: C:\\Users\\v_kkhuang\\.claude\\skills\\pr\n\n# GitHub PR 自动创建器 (Apache Linkis 版)\n\n## ⚠️ 强制要求（MUST READ）\n\n**执行此技能时，必须严格遵守以下规则，违反任何一项都将导致输出不合格：**\n\n1. **Issue 处理优先**: 必须先搜索/创建 Issue，然后才能创建 PR。绝不能跳过 Issue 步骤直接创建 PR。\n2. **标题格式**: PR 标题和 Issue 标题必须严格遵循格式，大小写不能错。\n3. **内容全英文**: PR 和 Issue 的所有内容必须是英文，不能有中文。\n4. **三段式描述**: PR 的 "What is the purpose" 必须使用三段式格式（Background/Problem, Purpose, Value/Impact）。\n5. **Issue 关联**: PR 创建时必须关联 Issue（使用 `close #issue_id`）。\n6. **必须询问**: 找到相关 Issues 时必须询问用户选择，不能自动决定。\n7. **不允许 commit**: 只创建 PR 和 Issue，不执行任何 git commit 操作。\n\n---\n\n## 工作流程（按顺序执行）\n\n```\n┌─────────────────────────────────────────────────────────────────────────────┐\n│                        Apache Linkis PR 创建流程                              │\n├─────────────────────────────────────────────────────────────────────────────┤\n│  Step 1: 询问分支信息（base + target）                                        │\n│     ↓                                                                        │\n│  Step 2: 获取代码变更（git diff + git log）                                   │\n│     ↓                                                                        │\n│  Step 3: 分析变更，识别模块（一级 + 二级）                                    │\n│     ↓                                                                        │\n│  Step 4: 搜索 GitHub Issues（gh issue list）                                 │\n│     ↓                                                                        │\n│  Step 5: 询问用户选择 Issue（复用现有 OR 创建新 Issue）                       │\n│     ↓                                                                        │\n│  Step 6: 如果需要，创建新 Issue（gh issue create）                            │\n│     ↓                                                                        │\n│  Step 7: 生成 PR 标题（严格格式）                                              │\n│     ↓                                                                        │\n│  Step 8: 生成 PR 描述（三段式 + Brief change log + Checklist）                │\n│     ↓                                                                        │\n│  Step 9: 创建 PR 并关联 Issue（gh pr create）                                 │\n└─────────────────────────────────────────────────────────────────────────────┘\n```\n\n---\n\n## 执行步骤详解\n\n### Step 1: 询问分支信息\n\n**必须询问用户以下信息：**\n\n```\n请输入以下信息以创建 Pull Request：\n\n1. 基础分支（base branch）: 即要合并的目标分支（例如: apachemaster, dev-1.19.0）\n2. 目标分支（target branch）: 即你的特性分支（例如: dev-1.19.0-feature-xxx）\n\n请确认这是用于创建 Pull Request，而非其他操作。\n```\n\n**验证规则**：\n- 必须确认用户意图是创建 PR\n- 如果分支不存在，列出可用分支供用户选择\n- 使用 `git branch -r` 列出远程分支\n\n---\n\n### Step 2: 获取代码变更\n\n**执行以下命令获取变更信息：**\n\n```bash\n# 获取变更文件列表\ngit diff --name-status <base_branch>..<target_branch>\n\n# 获取提交历史\ngit log <base_branch>..<target_branch> --oneline --no-merges\n\n# 获取详细的代码差异（用于分析）\ngit diff <base_branch>..<target_branch> --stat\n```\n\n**分析内容**：\n- 变更的文件数量和类型\n- 变更的代码行数\n- 提交的数量和主题\n\n---\n\n### Step 3: 分析变更，识别模块\n\n**从变更文件路径中提取模块信息：**\n\n#### 一级模块映射表（严格使用）\n\n| 目录名 | 缩写 | 全称 |\n|--------|------|------|\n| `linkis-spring-cloud-services` | `MGS` | microservice-governance |\n| `linkis-computation-governance` | `CGS` | computation-governance |\n| `linkis-public-enhancements` | `PES` | public-enhancements |\n| `linkis-engineconn-plugins` | `EC` | engineconn |\n| `linkis-extensions` | `ETS` | extensions |\n| `linkis-orchestrator` | `ORC` | orchestrator |\n| `linkis-web` | `WEB` | web |\n| `linkis-commons` | `COM` | commons |\n\n#### 二级模块提取规则\n\n| 一级模块 | 二级模块提取方式 | 示例 |\n|---------|-----------------|------|\n| MGS | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-gateway` → `gateway` |\n| CGS | `linkis-cg-xxx` 中的 xxx 部分（小写）或 `linkis-xxx` 去掉前缀 | `linkis-entrance` → `entrance` |\n| PES | `linkis-xxx` 中的 xxx 部分（小写） | `linkis-datasource` → `datasource` |\n| EC | 引擎名称（小写） | `spark` → `spark` |\n| WEB | 通常无二级模块，使用 `frontend` 或省略 | - |\n| COM | 目录名（小写） | `common` → `common` |\n\n**模块识别逻辑**：\n1. 如果所有变更文件在同一模块，使用该模块\n2. 如果跨越多个模块，选择变更最多的模块作为主模块\n3. 如果变更分散且数量相近，询问用户确认主模块\n\n---\n\n### Step 4: 搜索 GitHub Issues\n\n**必须执行 Issue 搜索，不能跳过：**\n\n```bash\n# 搜索 open issues，使用模块和变更关键词\ngh issue list --state open --search "<模块名> <关键词>" --limit 20\n\n# 例如：\n# gh issue list --state open --search "PES datasource filter" --limit 20\n# gh issue list --state open --search "CGS entrance location" --limit 20\n```\n\n**搜索关键词提取**：\n- 模块名（一级 + 二级）\n- 变更相关的关键词（从文件名、提交信息中提取）\n- action 类型（feat/fix 等）\n\n---\n\n### Step 5: 询问用户选择 Issue\n\n#### 如果找到相关 Issues（至少 1 个）\n\n**必须询问用户，不能自动选择：**\n\n```\n找到以下相关的 open issues：\n\n#123: [bug][PES][datasource] add support for getting published datasource by type and proxy user\n#456: [feature][CGS][entrance] hive location clause security check\n\n请选择：\n1. 使用现有 issue #123\n2. 使用现有 issue #456\n3. 创建新 issue\n4. 手动输入其他 issue 编号\n\n请输入选项 (1/2/3/4) 或 issue 编号:\n```\n\n#### 如果没有找到相关 Issues\n\n**直接进入创建新 Issue 流程**（Step 6）\n\n---\n\n### Step 6: 创建新 Issue（如需要）\n\n**如果用户选择创建新 Issue，或没有找到相关 Issue，则执行以下步骤：**\n\n#### Issue 标题格式（严格遵循）\n\n```\n[类型][一级模块][二级模块] 功能描述\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 示例 |\n|------|------|------|\n| `类型` | **小写** | `bug`, `feature`, `ask` |\n| `一级模块` | **大写** | `MGS`, `CGS`, `PES`, `EC`, `ETS`, `ORC`, `WEB`, `COM` |\n| `二级模块` | **小写** | `entrance`, `datasource`, `spark` 等 |\n| `功能描述` | **小写句子** | 首字母不大写，使用普通英文句子 |\n\n**类型选择规则**：\n- PR action 为 `fix` → Issue 类型为 `bug`\n- PR action 为 `feat` → Issue 类型为 `feature`\n- 其他情况 → Issue 类型为 `feature`\n\n**Issue 标题示例**：\n```\n✅ 正确: [bug][CGS][entrance] hive tasks should not be allowed to use location clause for security reasons\n✅ 正确: [feature][PES][datasource] add support for getting published datasource by type and proxy user\n❌ 错误: [Bug][CGS][Entrance] Hive Tasks Should Not Be Allowed... (大小写错误)\n❌ 错误: [bug][cgs][entrance] ... (一级模块小写错误)\n```\n\n#### Issue 内容格式（严格遵循）\n\n**根据 Issue 类型选择对应模板：**\n\n##### Bug Report 模板（fix 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar issues.\n\n### Linkis Component\n\n- [ ] linkis-commons\n- [ ] linkis-computation-governance\n- [ ] linkis-dist\n- [ ] linkis-engineconn-plugin\n- [ ] linkis-extensions\n- [ ] linkis-orchestrator\n- [x] linkis-public-enhancements\n- [ ] linkis-spring-cloud-services\n- [ ] linkis-web\n\n### Description\n\n[Brief description of the bug - what happened and what should happen]\n\n### Steps to reproduce\n\n1. Step 1...\n2. Step 2...\n3. Step 3...\n\n### Expected behavior\n\n[Describe what you expected to happen]\n\n### Your environment\n\n- Linkis version used: [x.x.x]\n- Environment name and version:\n    - hadoop-x.x.x\n    - hive-x.x.x\n    - spark-x.x.x\n    - scala-x.x.x\n    - jdk 1.8.x_xx\n\n### Anything else\n\n[Additional context, logs, frequency of occurrence, etc.]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n##### Feature Request 模板（feat 类型使用）\n\n```markdown\n### Search before asking\n\n- [x] I had searched in the [issues](https://github.com/apache/linkis/issues) and found no similar feature requirement.\n\n### Problem Description\n\nIs your feature request related to a problem? Please describe.\n\n[Describe the problem or need]\n\n### Description\n\nA short description of your feature\n\n[Clear and concise description of what you want to add]\n\n### Use case\n\nDescribe the use case of your feature request and the solution you\'d like.\n\n[Describe the specific use case and expected behavior]\n\n### Solutions\n\nDescribe alternatives you\'ve considered.\n\n[Describe any alternative solutions or workarounds you\'ve considered]\n\n### Anything else\n\nAdditional context or screenshots about the feature request.\n\n[Any additional information, screenshots, or examples]\n\n### Are you willing to submit a PR?\n\n- [x] Yes I am willing to submit a PR!\n```\n\n#### Issue 标签规则\n\n| Issue 类型 | 标签 |\n|-----------|------|\n| `bug` | `bug`, `status: needs triage` |\n| `feature` | `enhancement`, `status: needs triage` |\n| `ask` | `question`, `status: needs triage` |\n\n#### 创建 Issue 命令\n\n```bash\ngh issue create \\\n  --title "[<type>][<MODULE>][<submodule>] <description>" \\\n  --body "<issue_content>" \\\n  --label "<label1>,<label2>"\n```\n\n**保存返回的 issue_id，用于 PR 关联！**\n\n---\n\n### Step 7: 生成 PR 标题\n\n**PR 标题格式（严格遵循）**：\n\n```\n[action][一级模块][二级模块] 一句话总结\n```\n\n**大小写规则（绝对不能错）**：\n\n| 部分 | 格式 | 可选值 | 示例 |\n|------|------|--------|------|\n| `action` | **小写** | feat, fix, perf, refactor, docs, style, test, chore | `feat`, `fix` |\n| `一级模块` | **大写** | MGS, CGS, PES, EC, ETS, ORC, WEB, COM | `PES`, `CGS` |\n| `二级模块` | **小写** | 具体服务名称 | `datasource`, `entrance` |\n| `一句话总结` | **小写句子** | 普通英文，首字母不大写 | `add support for filtering by type` |\n\n**Action 类型定义**：\n\n| Action | 含义 | 使用场景 |\n|--------|------|----------|\n| `feat` | 新功能 | 添加新的功能、API、模块 |\n| `fix` | 修复 Bug | 修复问题、缺陷 |\n| `perf` | 性能优化 | 提升性能、优化资源使用 |\n| `refactor` | 重构 | 代码重构，不改变功能 |\n| `docs` | 文档 | 仅文档变更 |\n| `style` | 代码风格 | 格式化、代码风格调整 |\n| `test` | 测试 | 仅测试相关变更 |\n| `chore` | 构建/工具 | 构建、依赖、工具链更新 |\n\n**PR 标题示例**：\n\n```\n✅ 正确: [feat][PES][datasource] add support for getting published datasource by type and proxy user\n✅ 正确: [fix][CGS][entrance] resolve null pointer exception in engine reuse metrics update\n✅ 正确: [perf][CGS][application-manager] optimize engine creation performance\n❌ 错误: [Feat][PES][datasource] Add Support For... (大小写错误)\n❌ 错误: [feat][pES][datasource] ... (一级模块大小写混合)\n❌ 错误: feat: add support for... (缺少模块标签)\n```\n\n---\n\n### Step 8: 生成 PR 描述\n\n**PR 描述必须严格遵循以下格式：**\n\n#### PR 描述模板（完整格式）\n\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\n[Describe the background or problem - what issue exists, 2-3 sentences]\n\n**Purpose of Change:**\n[Describe the purpose of this PR - what this PR does to solve the problem, 2-3 sentences]\n\n**Value/Impact:**\n[Describe the value or impact - what benefits this change brings, 1-2 sentences]\n\n### Related issues/PRs\n\nRelated issues: close #<issue_id>\nRelated pr:none\n\n### Brief change log\n\n- [Change point 1 - start with verb]\n- [Change point 2 - start with verb]\n- [Change point 3 - start with verb]\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n#### "What is the purpose" 格式要求（严格三段式）\n\n**必须使用三段式结构，不能合并或省略：**\n\n| 段落 | 标题 | 内容要求 | 长度 |\n|------|------|----------|------|\n| 第一段 | **Background/Problem:** | 描述背景或问题 - 当前存在什么问题或需求 | 2-3 句话 |\n| 第二段 | **Purpose of Change:** | 描述改动的目的 - 本 PR 如何解决这个问题 | 2-3 句话 |\n| 第三段 | **Value/Impact:** | 描述价值或影响 - 改动带来的好处 | 1-2 句话 |\n\n**格式规范**：\n- ✅ 每段标题使用粗体，以冒号结尾\n- ✅ 段落标题使用 `**Title:**` 格式\n- ✅ 内容另起一行，不要和标题在同一行\n- ❌ 不要使用 "This PR adds..." 或 "This PR implements..." 格式\n- ✅ 使用客观描述，如 "This PR adds...", "The change enables..."\n\n**示例**：\n\n```\n✅ 正确格式:\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n❌ 错误格式:\n\nThis PR adds support for filtering datasources by type and user. (缺少三段式)\nThe problem is that users cannot filter datasources. (不完整)\n```\n\n#### "Brief change log" 格式要求\n\n**每条变更记录必须：**\n- 以动词开头\n- 使用现在时\n- 简洁明确\n- 每条独立一行\n\n**常用动词示例**：\n- `Add` - 添加新功能\n- `Fix` - 修复问题\n- `Update` - 更新现有功能\n- `Remove` - 删除功能\n- `Refactor` - 重构代码\n- `Optimize` - 优化性能\n- `Improve` - 改进\n\n**示例**：\n\n```\n✅ 正确:\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n❌ 错误:\n- Added API... (过去时)\n- Update the service... (不明确)\n```\n\n#### Checklist 智能勾选规则\n\n| 选项 | 何时勾选 | 说明 |\n|------|---------|------|\n| I have read the Contributing Guidelines | ✅ 始终勾选 | - |\n| I have explained the need for this PR | ✅ 始终勾选 | - |\n| I have explained the changes | ✅ 有代码变更时勾选 | 大多数情况勾选 |\n| I have added tests | 有测试时勾选 | 如果添加了测试代码 |\n| I have updated the documentation | 有文档变更时勾选 | 如果修改了文档 |\n| I have verified backward compatibility | ✅ 通常勾选 | 除非是破坏性变更 |\n| I have written unit tests | 有完整测试时勾选 | 如果有完整的单元测试 |\n\n---\n\n### Step 9: 创建 PR\n\n**执行 PR 创建命令：**\n\n```bash\ngh pr create \\\n  --title "<PR_TITLE>" \\\n  --body "<PR_BODY>" \\\n  --base "<base_branch>" \\\n  --head "<target_branch>" \\\n  --label "<label1>,<label2>"\n```\n\n**PR 标签映射规则**：\n\n| Action 类型 | Label |\n|-----------|-------|\n| `feat` | `enhancement` |\n| `fix` | `bug` |\n| `perf` | `performance` |\n| `refactor` | `refactor` |\n| `docs` | `documentation` |\n| `style` | `style` |\n| `test` | `test` |\n| `chore` | `build` |\n\n---\n\n## 错误处理\n\n| 错误场景 | 处理方式 |\n|---------|----------|\n| 找不到 git 仓库 | 提示用户在正确的项目目录中执行 |\n| gh 未登录 | 提示用户运行 `gh auth login` |\n| 分支不存在 | 列出可用分支供用户选择 |\n| 未找到代码变更 | 提示用户检查分支是否正确 |\n| Issue 搜索失败 | 提示用户手动关联 Issue |\n| PR 创建失败 | 显示错误信息，提示用户检查权限或网络 |\n\n---\n\n## 完整示例\n\n### 示例 1: 新增功能（创建新 Issue）\n\n**场景**: 添加数据源过滤功能，无相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-public-enhancements/linkis-datasource/src/...`\n- 模块: `PES` / `datasource`\n- Action: `feat`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "PES datasource filter" --limit 20\n```\n结果: 无相关 Issues\n\n**Step 6**: 创建新 Issue\n```bash\ngh issue create \\\n  --title "[feature][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --label "enhancement,status: needs triage"\n```\n返回: Issue #789\n\n**Step 7**: 生成 PR 标题\n```\n[feat][PES][datasource] add support for getting published datasource by type and proxy user\n```\n\n**Step 8**: 生成 PR 描述\n```markdown\n<!--\nThank you for sending the PR! We appreciate you spending the time to work on these changes.\nYou can learn more about contributing to Apache Linkis here: https://linkis.apache.org/community/how-to-contribute\nHappy contributing!\n-->\n\n### What is the purpose of the change\n\n**Background/Problem:**\nCurrently, the published datasource retrieval functionality cannot filter by datasource type and proxy user. Users need to manually filter through large datasource lists, reducing work efficiency.\n\n**Purpose of Change:**\nTo address this problem, this PR adds API support to allow users to filter published datasources by both datasource type and proxy user dimensions.\n\n**Value/Impact:**\nAfter the change, users can directly obtain datasource lists that meet the criteria, improving usage efficiency and user experience.\n\n### Related issues/PRs\n\nRelated issues: close #789\nRelated pr:none\n\n### Brief change log\n\n- Add API to get published datasources filtered by type and proxy user\n- Update DataSourceInfoService and implementation\n- Add client error codes for datasource operations\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[feat][PES][datasource] add support for getting published datasource by type and proxy user" \\\n  --body "..." \\\n  --base "apachemaster" \\\n  --head "dev-1.19.0-feature-datasource-filter" \\\n  --label "enhancement"\n```\n\n---\n\n### 示例 2: 修复 Bug（复用现有 Issue）\n\n**场景**: 修复空指针异常，找到相关 Issue\n\n**Step 1-3**: 分析变更\n- 变更文件: `linkis-computation-governance/linkis-cg-linkismanager/src/...`\n- 模块: `CGS` / `linkismanager` (简化为 `application-manager`)\n- Action: `fix`\n\n**Step 4**: 搜索 Issues\n```bash\ngh issue list --state open --search "CGS application-manager null pointer" --limit 20\n```\n结果: 找到 Issue #456\n\n**Step 5**: 询问用户\n```\n找到以下相关的 open issues：\n\n#456: [bug][CGS][application-manager] null pointer exception in engine reuse metrics update causing monitoring data loss\n\n请选择：\n1. 使用现有 issue #456\n2. 创建新 issue\n3. 手动输入其他 issue 编号\n```\n用户选择: 1\n\n**Step 6**: 跳过（复用现有 Issue #456）\n\n**Step 7**: 生成 PR 标题\n```\n[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update\n```\n\n**Step 8**: 生成 PR 描述（使用 Bug Report 风格的三段式）\n```markdown\n### What is the purpose of the change\n\n**Background/Problem:**\nDuring engine creation and reuse, when updating metrics information, a null pointer exception occurs, causing some monitoring data to be lost and affecting system stability.\n\n**Purpose of Change:**\nTo address this problem, this PR adds null checks and protection to the relevant code, ensuring the robustness of metrics updates.\n\n**Value/Impact:**\nAfter the change, the system runs stably with complete and available monitoring data, improving system reliability.\n\n### Related issues/PRs\n\nRelated issues: close #456\nRelated pr:none\n\n### Brief change log\n\n- Add null pointer checks in DefaultEngineCreateService\n- Add null pointer checks in DefaultEngineReuseService\n\n### Checklist\n\n- [x] I have read the [Contributing Guidelines on pull requests](https://linkis.apache.org/community/how-to-contribute).\n- [x] I have explained the need for this PR and the problem it solves\n- [x] I have explained the changes or the new features added to this PR\n- [ ] I have added tests corresponding to this change\n- [ ] I have updated the documentation to reflect this change\n- [x] I have verified that this change is backward compatible\n- [ ] **If this is a code change**: I have written unit tests to fully verify the new behavior.\n```\n\n**Step 9**: 创建 PR\n```bash\ngh pr create \\\n  --title "[fix][CGS][application-manager] resolve null pointer exception in engine reuse metrics update" \\\n  --body "..." \\\n  --base "dev-1.19.0" \\\n  --head "dev-1.19.0-fix-null-pointer" \\\n  --label "bug"\n```\n\n---\n\n## 快速参考\n\n### 大小写规则速查\n\n| 位置 | PR | Issue |\n|------|-----|-------|\n| 类型/Action | **小写** | **小写** |\n| 一级模块 | **大写** | **大写** |\n| 二级模块 | **小写** | **小写** |\n| 描述/总结 | **小写句子** | **小写句子** |\n\n### 格式速查\n\n```\nPR 标题:    [action][MODULE][submodule] description\nIssue 标题:  [type][MODULE][submodule] description\n\nPR action:   feat, fix, perf, refactor, docs, style, test, chore\nIssue type:  bug, feature, ask\n\nMODULE:      MGS, CGS, PES, EC, ETS, ORC, WEB, COM\n```\n\n### 命令速查\n\n```bash\n# 搜索 Issues\ngh issue list --state open --search "<关键词>" --limit 20\n\n# 创建 Issue\ngh issue create --title "<标题>" --body "<内容>" --label "<标签>"\n\n# 创建 PR\ngh pr create --title "<标题>" --body "<内容>" --base "<base>" --label "<标签>"\n\n# 列出分支\ngit branch -r\n```\n\n---\n\n## 使用方法\n\n```bash\n/pr\n```\n\n然后按照提示输入分支信息即可。\n\n---\n\n## 依赖工具\n\n- `git` - 版本控制\n- `gh` (GitHub CLI) - GitHub 操作\n\n确保 `gh` 已登录：`gh auth login`\n\n\nARGUMENTS: dev-2.1.0-hive-share 提交pr 到dev-2.1.0-webank'}]

**AI**：
我将从 `dev-2.1.0-hive-share` 创建一个指向 `dev-2.1.0-webank` 的 PR。让我先来收集分支信息和代码变更。...


## 交互记录 (2026-08-12T08:45:17.687Z)

**用户**：
[{'tool_use_id': 'call_b1ea80d7b49a43d89dd2758f', 'type': 'tool_result', 'content': 'A\t"docs/dev-2.1.0/testing/REQ-04_Hive\\346\\225\\260\\346\\215\\256\\346\\272\\220\\345\\205\\261\\344\\272\\253\\346\\224\\257\\346\\214\\201_\\346\\265\\213\\350\\257\\225\\347\\224\\250\\344\\276\\213.md"\nA\t"docs/dev-2.1.0/testing/wemind/REQ-04_Hive\\346\\225\\260\\346\\215\\256\\346\\272\\220\\345\\205\\261\\344\\272\\253\\346\\224\\257\\346\\214\\201_wemind\\345\\257\\274\\345\\205\\245.json"\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/server/src/main/java/org/apache/linkis/metadata/query/server/service/impl/MetadataQueryServiceImpl.java\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\nM\tlinkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/db2/SqlConnection.java\nM\tlinkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/datasourcemanager/common/auth/AuthContext.java\nM\tlinkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/MdmConfiguration.java', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T08:45:17.778Z)

**用户**：
[{'tool_use_id': 'call_8bb20730def9439883a04141', 'type': 'tool_result', 'content': ' ...265\\213\\350\\257\\225\\347\\224\\250\\344\\276\\213.md" | 663 +++++++++++++++++++++\n ...46\\214\\201_wemind\\345\\257\\274\\345\\205\\245.json" | 205 +++++++\n .../service/impl/MetadataQueryServiceImpl.java     |  23 +-\n .../metadata/query/service/HiveMetaService.java    |  33 +-\n .../metadata/query/service/db2/SqlConnection.java  |   2 +-\n .../datasourcemanager/common/auth/AuthContext.java |   7 +\n .../metadata/query/common/MdmConfiguration.java    |  11 +\n 7 files changed, 938 insertions(+), 6 deletions(-)', 'is_error': False}]

**AI**：
[思考] Let me look at the actual code changes to understand what this PR does. The branch name is "hive-share" and the commit message is "开发阶段：hive数据源携带keytab，支持其他用户使用" which translates to "Development phase: hive datasource carries keytab, supporting other users to use".

Let me look at the actual diffs to understand the changes better....


## 交互记录 (2026-08-12T08:45:17.824Z)

**用户**：
[{'tool_use_id': 'call_7c19ad5b1f984708a0f265ba', 'type': 'tool_result', 'content': '5bf455364 #AI commit# 开发阶段：hive数据源携带keytab，支持其他用户使用', 'is_error': False}]

**AI**：
两个分支都存在。现在让我详细检查一下实际的代码变更，以了解该 PR 的目的。...


## 交互记录 (2026-08-12T08:45:17.884Z)

**用户**：
[{'tool_use_id': 'call_5ba4a9ae603d40a399c07171', 'type': 'tool_result', 'content': '  origin/dev-2.1.0-hive-share\n  origin/dev-2.1.0-webank\n  origin/dev-2.1.0-webank-chore-version', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T08:46:52.473Z)

**用户**：
[{'tool_use_id': 'call_55fa11992cfb435bb473946a', 'type': 'tool_result', 'content': 'diff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/server/src/main/java/org/apache/linkis/metadata/query/server/service/impl/MetadataQueryServiceImpl.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/server/src/main/java/org/apache/linkis/metadata/query/server/service/impl/MetadataQueryServiceImpl.java\nindex 1caf5776f..684df5f97 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/server/src/main/java/org/apache/linkis/metadata/query/server/service/impl/MetadataQueryServiceImpl.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/server/src/main/java/org/apache/linkis/metadata/query/server/service/impl/MetadataQueryServiceImpl.java\n@@ -452,7 +452,8 @@ public class MetadataQueryServiceImpl implements MetadataQueryService {\n       boolean hasPermission =\n           (AuthContext.isAdministrator(userName)\n               || (StringUtils.isNotBlank(response.getCreator())\n-                  && userName.equals(response.getCreator())));\n+                  && userName.equals(response.getCreator()))\n+              || isHiveShareEnabled(response.getDsType()));\n       if (!hasPermission) {\n         throw new ErrorException(-1, "Don\'t have query permission for data source [没有数据源的查询权限]");\n       } else if (response.getParams().isEmpty()) {\n@@ -511,7 +512,8 @@ public class MetadataQueryServiceImpl implements MetadataQueryService {\n       boolean hasPermission =\n           (AuthContext.isAdministrator(userName)\n               || (StringUtils.isNotBlank(response.getCreator())\n-                  && userName.equals(response.getCreator())));\n+                  && userName.equals(response.getCreator()))\n+              || isHiveShareEnabled(response.getDsType()));\n       if (!hasPermission) {\n         throw new ErrorException(-1, "Don\'t have query permission for data source [没有数据源的查询权限]");\n       } else if (!useDefault && response.getParams().isEmpty()) {\n@@ -541,6 +543,23 @@ public class MetadataQueryServiceImpl implements MetadataQueryService {\n         : null;\n   }\n \n+  /**\n+   * Check if Hive datasource share is enabled for the given datasource type. When the switch is on\n+   * and the datasource type is "hive", non-creator users are allowed to query the datasource.\n+   *\n+   * @param dsType datasource type\n+   * @return true if Hive share is enabled and the type is hive\n+   */\n+  private boolean isHiveShareEnabled(String dsType) {\n+    boolean shareEnabled = MdmConfiguration.HIVE_DATASOURCE_SHARE_ENABLE.getValue();\n+    if (shareEnabled && "hive".equalsIgnoreCase(dsType)) {\n+      logger.info(\n+          "Hive datasource share is enabled, allowing access for non-creator user to hive datasource");\n+      return true;\n+    }\n+    return false;\n+  }\n+\n   /**\n    * Invoke method in meta service\n    *', 'is_error': False}]

**AI**：
[思考] Now I have a clear understanding of the changes. Let me summarize:

1. **New feature switch**: Added `linkis.datasource.hive.share.enable` (default false) and `linkis.datasource.hive.share.keytab.path` (default `/appcom/keytab/`) in `MdmConfiguration.java`.

2. **HiveMetaService.java**: When Hive datasource share is enabled, uses a local keytab file path (derived from the principal primary, e.g. `hadoop.keytab` from `hadoop@REALM`) instead of downloading the keytab from BML. Falls back to original BML download behavior when disabled.

3. **MetadataQueryServiceImpl.java**: Added `isHiveShareEnabled()` method. When share is enabled and datasource type is "hive", non-creator users are allowed to query the datasource (bypass the creator-only permission check). Applied to two permission check points.

4. **AuthContext.java**: Added similar check in `hasPermission()` - when share is enabled and datasource type is hive, returns true (allows non-creator access).

5. **SqlConnection.java (db2)**: Minor fix - added `TRIM()` to the schema query SQL to trim schema names. This is a small fix.

6. **Testing docs**: Added two test case documents.

So this is primarily a **feat** (new feature) - Hive datasource sharing that allows non-creator users to use Hive datasources with kerberos keytab. There's also a small db2 schema query fix.

The primary module is `linkis-public-enhancements` → `PES` / `datasource` (metadata-query is part of datasource module).

Now let me search for related GitHub issues. The keywords would be: hive datasource share, keytab, kerberos.

Let me search for issues....


## 交互记录 (2026-08-12T08:46:55.149Z)

**用户**：
[{'tool_use_id': 'call_3bbd4c7cbcce4680b9a4858e', 'type': 'tool_result', 'content': 'diff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\nindex 049c496b2..91d1dfb04 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/hive/src/main/java/org/apache/linkis/metadata/query/service/HiveMetaService.java\n@@ -22,6 +22,7 @@ import org.apache.linkis.bml.client.BmlClientFactory;\n import org.apache.linkis.bml.protocol.BmlDownloadResponse;\n import org.apache.linkis.common.conf.CommonVars;\n import org.apache.linkis.datasourcemanager.common.util.json.Json;\n+import org.apache.linkis.metadata.query.common.MdmConfiguration;\n import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;\n import org.apache.linkis.metadata.query.common.domain.MetaPartitionInfo;\n import org.apache.linkis.metadata.query.common.exception.MetaRuntimeException;\n@@ -83,10 +84,36 @@ public class HiveMetaService extends AbstractDbMetaService<HiveConnection> {\n       LOG.info("Try to connect Hive MetaStore in kerberos with principle:[" + principle + "]");\n       String keytabResourceId =\n           String.valueOf(params.getOrDefault(HiveParamsMapper.PARAM_HIVE_KEYTAB.getValue(), ""));\n-      if (StringUtils.isNotBlank(keytabResourceId)) {\n+\n+      boolean hiveShareEnabled = MdmConfiguration.HIVE_DATASOURCE_SHARE_ENABLE.getValue();\n+      String keytabFilePath;\n+\n+      if (hiveShareEnabled && StringUtils.isNotBlank(principle)) {\n+        // When Hive datasource share is enabled, use local keytab path instead of BML download\n+        // Extract primary from principal (e.g., "hadoop" from "hadoop@REALM" or\n+        // "hadoop/host@REALM")\n+        String principalPrimary = principle.split("/")[0].split("@")[0];\n+        String keytabDir = MdmConfiguration.HIVE_DATASOURCE_SHARE_KEYTAB_PATH.getValue();\n+        keytabFilePath = keytabDir + principalPrimary + ".keytab";\n+        LOG.info(\n+            "Hive datasource share is enabled, using local keytab path:["\n+                + keytabFilePath\n+                + "] for principal:["\n+                + principle\n+                + "]");\n+        File keytabFile = new File(keytabFilePath);\n+        if (!keytabFile.exists()) {\n+          throw new MetaRuntimeException(\n+              "Local keytab file not found:["\n+                  + keytabFilePath\n+                  + "], please ensure the keytab file is placed correctly",\n+              null);\n+        }\n+      } else if (StringUtils.isNotBlank(keytabResourceId)) {\n+        // Original behavior: download keytab from BML\n         FileUtils.forceMkdir(new File(TMP_FILE_STORE_LOCATION.getValue()));\n         LOG.info("Start to download resource id:[" + keytabResourceId + "]");\n-        String keytabFilePath =\n+        keytabFilePath =\n             TMP_FILE_STORE_LOCATION.getValue()\n                 + "/"\n                 + UUID.randomUUID().toString().replace("-", "")\n@@ -96,10 +123,10 @@ public class HiveMetaService extends AbstractDbMetaService<HiveConnection> {\n           throw new MetaRuntimeException(\n               "Fail to download resource i:[" + keytabResourceId + "]", null);\n         }\n-        conn = new HiveConnection(uris, principle, keytabFilePath, getExtraHadoopConf(params));\n       } else {\n         throw new MetaRuntimeException("Cannot find the keytab file in connect parameters", null);\n       }\n+      conn = new HiveConnection(uris, principle, keytabFilePath, getExtraHadoopConf(params));\n     } else {\n       conn = new HiveConnection(uris, getExtraHadoopConf(params));\n     }', 'is_error': False}]

**AI**：
现在我已经清楚了解了这些变更。主要功能是 Hive 数据源共享（允许非创建者用户通过 Kerberos keytab 使用 Hive 数据源），此外还有一个小的 DB2 schema 查询修复。让我搜索一下相关的 GitHub issues。...


## 交互记录 (2026-08-12T08:46:59.232Z)

**用户**：
[{'tool_use_id': 'call_56b23b88338642758e5f0b4e', 'type': 'tool_result', 'content': 'diff --git a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/db2/SqlConnection.java b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/db2/SqlConnection.java\nindex 27019ba0d..0799689fb 100644\n--- a/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/db2/SqlConnection.java\n+++ b/linkis-public-enhancements/linkis-datasource/linkis-metadata-query/service/jdbc/src/main/java/org/apache/linkis/metadata/query/service/db2/SqlConnection.java\n@@ -47,7 +47,7 @@ public class SqlConnection implements Closeable {\n   private static final CommonVars<String> SQL_SCHEMA_QUERY =\n       CommonVars.apply(\n           "wds.linkis.server.mdm.service.db2.schema.query.sql",\n-          "SELECT SCHEMANAME FROM SYSCAT.SCHEMATA WHERE SCHEMANAME NOT LIKE \'SYS%\' AND SCHEMANAME NOT IN (\'NULLID\', \'SQLJ\') WITH UR");\n+          "SELECT TRIM(SCHEMANAME) FROM SYSCAT.SCHEMATA WHERE SCHEMANAME NOT LIKE \'SYS%\' AND SCHEMANAME NOT IN (\'NULLID\', \'SQLJ\') WITH UR");\n \n   private Connection conn;\n \ndiff --git a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/datasourcemanager/common/auth/AuthContext.java b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/datasourcemanager/common/auth/AuthContext.java\nindex c37048393..016232214 100644\n--- a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/datasourcemanager/common/auth/AuthContext.java\n+++ b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/datasourcemanager/common/auth/AuthContext.java\n@@ -19,6 +19,7 @@ package org.apache.linkis.datasourcemanager.common.auth;\n \n import org.apache.linkis.common.conf.CommonVars;\n import org.apache.linkis.datasourcemanager.common.domain.DataSource;\n+import org.apache.linkis.metadata.query.common.MdmConfiguration;\n \n import org.apache.commons.lang3.StringUtils;\n \n@@ -50,6 +51,12 @@ public class AuthContext {\n   public static boolean hasPermission(DataSource dataSource, String username) {\n     if (Objects.nonNull(dataSource)) {\n       String creator = dataSource.getCreateUser();\n+      if (MdmConfiguration.HIVE_DATASOURCE_SHARE_ENABLE.getValue()) {\n+        String name = dataSource.getDataSourceType().getName();\n+        if ("hive".equals(name)) {\n+          return true;\n+        }\n+      }\n       return (administrators.contains(username)\n           || (StringUtils.isNotBlank(creator) && username.equals(creator)));\n     }\ndiff --git a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/MdmConfiguration.java b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/MdmConfiguration.java\nindex d9de9d02d..40c00080b 100644\n--- a/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/MdmConfiguration.java\n+++ b/linkis-public-enhancements/linkis-pes-common/src/main/java/org/apache/linkis/metadata/query/common/MdmConfiguration.java\n@@ -26,4 +26,15 @@ public class MdmConfiguration {\n \n   public static CommonVars<String> DATA_SOURCE_SERVICE_APPLICATION =\n       CommonVars.apply("wds.linkis.server.dsm.app.name", "linkis-ps-data-source-manager");\n+\n+  /**\n+   * Feature switch for Hive datasource sharing. When enabled, non-creator users can query Hive\n+   * datasource.\n+   */\n+  public static CommonVars<Boolean> HIVE_DATASOURCE_SHARE_ENABLE =\n+      CommonVars.apply("linkis.datasource.hive.share.enable", false);\n+\n+  /** Local keytab directory path for Hive datasource sharing (used when share is enabled). */\n+  public static CommonVars<String> HIVE_DATASOURCE_SHARE_KEYTAB_PATH =\n+      CommonVars.apply("linkis.datasource.hive.share.keytab.path", "/appcom/keytab/");\n }', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T08:47:30.836Z)

**用户**：
[{'tool_use_id': 'call_6f25677768e0413dbca3c2f8', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
[思考] No related issues found. Let me also check the repo to make sure gh is logged in and pointing to the right repo....


## 交互记录 (2026-08-12T08:47:34.595Z)

**用户**：
[{'tool_use_id': 'call_1da7065837344b4885fb8e67', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
未找到相关 Issue。让我核实一下 gh CLI 是否已登录并指向正确的仓库。...


## 交互记录 (2026-08-12T08:47:38.451Z)

**用户**：
[{'tool_use_id': 'call_abe1216e747449d1848df56b', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T08:48:49.733Z)

**用户**：
[{'tool_use_id': 'call_202c226ee80c4fb1bca6a034', 'type': 'tool_result', 'content': '{"nameWithOwner":"WeDataSphere/linkis","url":"https://github.com/WeDataSphere/linkis"}', 'is_error': False}]

**AI**：
[思考] The repo is `WeDataSphere/linkis`, not `apache/linkis`. This is the WeDataSphere fork. The target branch is `dev-2.1.0-webank` which is a WeBank internal branch.

No related issues found, so I need to create a new issue. The feature is `feat` type, so the issue type would be `feature`.

Let me analyze the changes:

**Module identification:**
- All code changes are in `linkis-public-enhancements/` → PES
- Sub-module: `datasource` (specifically metadata-query and pes-common)
- Main feature: Hive datasource sharing
- Secondary: DB2 schema query TRIM fix

**Issue title:** `[feature][PES][datasource] add support for hive datasource sharing with kerberos keytab for non-creator users`

**PR title:** `[feat][PES][datasource] add hive datasource sharing with kerberos keytab for non-creator users`

Wait, there's also the DB2 fix. But the primary change is the hive share feature. Let me focus the PR on the main feature. The DB2 TRIM fix is minor/incidental.

Actually, let me reconsider. The PR contains both a feature (hive datasource share) and a small fix (db2 schema query TRIM). The dominant change is the hive share feature. I'll make the PR about the hive share feature primarily, but mention the db2 fix in the change log.

Since no issues were found, per the skill rules I should "直接进入创建新 Issue 流程" (directly enter the create new Issue flow). The skill says if no related issues found, directly go to Step 6 (create new issue). 

But wait - rule 6 says "找到相关 Issues 时必须询问用户选择，不能自动决定" (when finding related issues, must ask user to choose, cannot auto-decide). Since NO related issues were found, the skill says to directly create a new issue. However, it might be good practice to still confirm with the user before creating an issue since that's an outward-facing action. Let me follow the skill: no related issues → directly create new issue flow.

Actually, let me reconsider. Creating a GitHub issue is an outward-facing action that publishes content. Per my general guidance "Fo...


## 交互记录 (2026-08-12T09:00:37.228Z)

**用户**：
[{'type': 'tool_result', 'content': 'Your questions have been answered: "No related open issues were found. I will create a new feature issue on WeDataSphere/linkis, then create the PR from dev-2.1.0-hive-share to dev-2.1.0-webank linking it. How should I proceed?"="Create Issue + PR (Recommended)". You can now continue with these answers in mind.', 'tool_use_id': 'call_a744ce7a400040ed9c9c6779'}]

**AI**：
## 变更分析摘要

**分支**: `dev-2.1.0-hive-share` → `dev-2.1.0-webank`
**仓库**: `WeDataSphere/linkis`
**提交**: 1 个提交 (`5bf455364`)
**变更**: 7 个文件 (938 次插入，6 次删除)

### 模块识别
- **一级模块**: `PES` (linkis-public-enhancements)
- **二级模块**: `datasource` (metadata-query / pes-common)
- **操作**: `feat` (新功能，默认关闭，可通过开关控制)

### 变更内容
1. **MdmConfiguration.java** — 添加了 `linkis.datasource.hive.share.enable` (默认为 false) 和 `linkus.datasource.hive.share.keytab.path` (默认为 `/appcom/keytab/`) 开关。
2. **HiveMetaService.java** — 当开启共享时，使用从 principal 派生的本地 keytab 路径，而非从 BML 下载；禁用时回退到原始的 BML 下载方式。
3. **MetadataQueryServiceImpl.java** — 添加了 `isHiveShareEnabled()`；针对 Hive 数据源放宽了 2 处权限检查，允许非创建者用户查询。
4. **AuthContext.java** — 当开启共享时，针对 Hive 数据源放宽了 `hasPermission()`。
5. **SqlConnection.java (db2)** — 小修复：在 schema 查询中添加了 `TRIM(SCHEMANAME)`。
6. **测试文档** — 添加了测试用例文档。

### Issue 搜索结果
未找到相关的未解决 Issue。根据工作流，我将创建一个新的 **feature** Issue，然后创建关联的 PR。

---

### 起草的 Issue

**标题**: `[feature][PES][datasource] add support for hive datasource sharing with kerberos keytab for non-creator users`

**内容**: 功能请求 — 问题描述（仅限创建者的 Hive 访问权限导致多租户环境下的配置重复），描述（共享开关 + 本地 keytab），用例（共享服务 keytab），解决方案。

### 起草的 PR

**标题**: `[feat][PES][datasource] add hive datasource sharing with kerberos keytab for non-creator users`

**正文**: 三段式目的说明（背景/问题 → 目的 → 价值/影响），`close #<issue_id>`，简要变更日志（4 点），检查清单。

---

在执行 `gh issue create` 和 `gh pr create` 之前，请确认：...


## 交互记录 (2026-08-12T09:00:59.172Z)

**用户**：
[{'tool_use_id': 'call_cb4d5b530d0845c39e45706e', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\pr-issue-body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
...


## 交互记录 (2026-08-12T09:01:35.268Z)

**用户**：
[{'type': 'tool_result', 'content': "Exit code 1\ncould not add label: 'status: needs triage' not found", 'is_error': True, 'tool_use_id': 'call_f522a04653b5433e84174447'}]

**AI**：
[思考] The user confirmed. Let me proceed with creating the issue first, then the PR.

Let me create the issue using the Feature Request template. I'll write the body to a temp file to handle the multi-line content properly, then use gh issue create with --body-file.

Let me create the issue body file first....


## 交互记录 (2026-08-12T09:02:28.764Z)

**用户**：
[{'tool_use_id': 'call_17c78637953541ffb9a5e10c', 'type': 'tool_result', 'content': "bug\tSomething isn't working\t#d73a4a\ndocumentation\tImprovements or additions to documentation\t#0075ca\nduplicate\tThis issue or pull request already exists\t#cfd3d7\nenhancement\tNew feature or request\t#a2eeef\ngood first issue\tGood for newcomers\t#7057ff\nhelp wanted\tExtra attention is needed\t#008672\ninvalid\tThis doesn't seem right\t#e4e669\nquestion\tFurther information is requested\t#d876e3\nwontfix\tThis will not be worked on\t#ffffff", 'is_error': False}]

**AI**：
我将先创建 feature issue，然后创建链接的 PR。...


## 交互记录 (2026-08-12T09:03:02.367Z)

**用户**：
[{'tool_use_id': 'call_f4fafe7257674603a808a018', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/issues/1090', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T09:03:24.795Z)

**用户**：
[{'tool_use_id': 'call_403c4a4ac88a40b2a7d86e86', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\pr-body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
...


## 交互记录 (2026-08-12T09:03:49.211Z)

**用户**：
[{'tool_use_id': 'call_d39d9df91d2e4e7a8b6ee06a', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/pull/1091', 'is_error': False}]

**AI**：
[思考] The label `status: needs triage` doesn't exist on WeDataSphere/linkis. Let me check available labels and retry with just `enhancement`, or find the correct label name....


## 交互记录 (2026-08-12T09:04:45.849Z)

**用户**：
[{'tool_use_id': 'call_b2f26715fe7045fda0f33171', 'type': 'tool_result', 'content': '{"baseRefName":"dev-2.1.0-webank","headRefName":"dev-2.1.0-hive-share","number":1091,"state":"OPEN","title":"[feat][PES][datasource] add hive datasource sharing with kerberos keytab for non-creator users","url":"https://github.com/WeDataSphere/linkis/pull/1091"}', 'is_error': False}]

**AI**：
`status: needs triage` 标签在这个仓库中不存在。我来检查一下可用的标签，并只使用现有的标签进行重试。...


## 交互记录 (2026-08-12T09:05:25.687Z)

**用户**：
[{'type': 'text', 'text': 'dev-2.1.0-keytab-error-fix  这个也提交下'}]

**AI**：
...


## 交互记录 (2026-08-12T09:05:55.911Z)

**用户**：
[{'tool_use_id': 'call_2b8d13ac0739428188441bb9', 'type': 'tool_result', 'content': '  origin/dev-2.1.0-keytab-error-fix', 'is_error': False}]

**AI**：
[思考] The available labels are: bug, documentation, duplicate, enhancement, good first issue, help wanted, invalid, question, wontfix. No `status: needs triage`. Let me create the issue with just `enhancement`....


## 交互记录 (2026-08-12T09:05:57.592Z)

**用户**：
[{'tool_use_id': 'call_f63656adffe542a7ac100df3', 'type': 'tool_result', 'content': 'A\tlinkis-commons/linkis-hadoop-common/src/main/java/org/apache/linkis/hadoop/common/utils/KerberosTgtUtils.java\nM\tlinkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala\nM\tlinkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/entity/HDFSFileSystemContainer.scala\nM\tlinkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala\nM\tlinkis-commons/linkis-hadoop-common/src/test/scala/org/apache/linkis/hadoop/common/conf/HadoopConfTest.scala', 'is_error': False}]

**AI**：
仅有标准标签。让我仅使用 `enhancement` 来创建 issue。...


## 交互记录 (2026-08-12T09:05:59.522Z)

**用户**：
[{'tool_use_id': 'call_3d25dec274fc43f7afd86d2f', 'type': 'tool_result', 'content': '0d5825cd5 #AI commit# 开发阶段：修复keytab开启缓存时，引发TGT错误', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T09:06:01.264Z)

**用户**：
[{'tool_use_id': 'call_29da62cccc5b4903a4c9bbbc', 'type': 'tool_result', 'content': ' .../hadoop/common/utils/KerberosTgtUtils.java      |  99 ++++++++++++++++++++\n .../linkis/hadoop/common/conf/HadoopConf.scala     |   9 ++\n .../common/entity/HDFSFileSystemContainer.scala    |  21 ++++-\n .../linkis/hadoop/common/utils/HDFSUtils.scala     | 101 +++++++++++++++++----\n .../linkis/hadoop/common/conf/HadoopConfTest.scala |   1 +\n 5 files changed, 210 insertions(+), 21 deletions(-)', 'is_error': False}]

**AI**：
[思考] Issue #1090 created. Now let me create the PR body file and create the PR linking close #1090....


## 交互记录 (2026-08-12T09:07:47.172Z)

**用户**：
[{'tool_use_id': 'call_d8f471d0d8ea4da2816965b0', 'type': 'tool_result', 'content': 'diff --git a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala\nindex 864b8f650..56ca8a89f 100644\n--- a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala\n+++ b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/conf/HadoopConf.scala\n@@ -81,4 +81,13 @@ object HadoopConf {\n    */\n   val KEYTAB_TEMP_DIR = CommonVars("linkis.keytab.temp.dir", "/tmp/keytab")\n \n+  /**\n+   * Enable proactive Kerberos TGT refresh when retrieving cached HDFS FileSystem. When enabled,\n+   * before returning a cached FileSystem, Hadoop\'s `reloginFromKeytab()` is called on the UGI to\n+   * refresh the TGT if near expiry. If refresh fails (TGT expired and keytab unavailable), the\n+   * cached entry is removed and a new FileSystem with fresh TGT is created.\n+   */\n+  val HDFS_TGT_PROACTIVE_CHECK_ENABLE =\n+    CommonVars("linkis.hadoop.hdfs.tgt.proactive.check.enable", false)\n+\n }\ndiff --git a/linkis-commons/linkis-hadoop-common/src/test/scala/org/apache/linkis/hadoop/common/conf/HadoopConfTest.scala b/linkis-commons/linkis-hadoop-common/src/test/scala/org/apache/linkis/hadoop/common/conf/HadoopConfTest.scala\nindex 7c2c7b383..9c3c008ad 100644\n--- a/linkis-commons/linkis-hadoop-common/src/test/scala/org/apache/linkis/hadoop/common/conf/HadoopConfTest.scala\n+++ b/linkis-commons/linkis-hadoop-common/src/test/scala/org/apache/linkis/hadoop/common/conf/HadoopConfTest.scala\n@@ -38,6 +38,7 @@ class HadoopConfTest {\n     )\n     Assertions.assertFalse(HadoopConf.HDFS_ENABLE_CACHE)\n     Assertions.assertTrue(180000 == HadoopConf.HDFS_ENABLE_CACHE_IDLE_TIME)\n+    Assertions.assertFalse(HadoopConf.HDFS_TGT_PROACTIVE_CHECK_ENABLE.getValue)\n \n   }', 'is_error': False}]

**AI**：
Issue **#1090** 已创建。现在让我来编写 PR 正文并创建链接到它的 PR。...


## 交互记录 (2026-08-12T09:07:49.760Z)

**用户**：
[{'tool_use_id': 'call_d30005b883c54157b3ee9f2b', 'type': 'tool_result', 'content': 'diff --git a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala\nindex 45a03f0dc..0ae9cf7bf 100644\n--- a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala\n+++ b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/utils/HDFSUtils.scala\n@@ -233,9 +233,23 @@ object HDFSUtils extends Logging {\n       locker.intern().synchronized {\n         if (fileSystemCache.containsKey(cacheKey)) {\n           val hdfsFileSystemContainer = fileSystemCache.get(cacheKey)\n-          hdfsFileSystemContainer.addAccessCount()\n-          hdfsFileSystemContainer.updateLastAccessTime\n-          hdfsFileSystemContainer.getFileSystem\n+          // Proactive TGT expiry check: if expired, remove from cache and recreate\n+          if (\n+              HadoopConf.HDFS_TGT_PROACTIVE_CHECK_ENABLE.getValue && !hdfsFileSystemContainer\n+                .isTgtValid()\n+          ) {\n+            logger.info(\n+              s"Cached HDFS FileSystem TGT expired - user: $userName, label: $cacheLabel, removing from cache and recreating"\n+            )\n+            fileSystemCache.remove(cacheKey)\n+            IOUtils.closeQuietly(hdfsFileSystemContainer.getFileSystem)\n+            // Fall through to create new FileSystem with fresh TGT\n+            getHDFSUserFileSystem(userName, label, getConfigurationByLabel(userName, label))\n+          } else {\n+            hdfsFileSystemContainer.addAccessCount()\n+            hdfsFileSystemContainer.updateLastAccessTime\n+            hdfsFileSystemContainer.getFileSystem\n+          }\n         } else {\n           getHDFSUserFileSystem(userName, label, getConfigurationByLabel(userName, label))\n         }\n@@ -259,25 +273,33 @@ object HDFSUtils extends Logging {\n       val cacheLabel = if (label == null) DEFAULT_CACHE_LABEL else label\n       val cacheKey = userName + JOINT + cacheLabel\n       locker.intern().synchronized {\n-        val hdfsFileSystemContainer = if (fileSystemCache.containsKey(cacheKey)) {\n-          fileSystemCache.get(cacheKey)\n-        } else {\n-          // we use cacheLabel to create HDFSFileSystemContainer, and in the rest part of HDFSUtils, we consistently\n-          // use the same cacheLabel to operate HDFSFileSystemContainer, like close or remove.\n-          // At the same time, we don\'t want to change the behavior of createFileSystem which is out of HDFSUtils,\n-          // so we continue to use the original label to createFileSystem.\n-          val newHDFSFileSystemContainer =\n-            new HDFSFileSystemContainer(\n-              createFileSystem(userName, label, conf),\n-              userName,\n-              cacheLabel\n+        if (fileSystemCache.containsKey(cacheKey)) {\n+          val hdfsFileSystemContainer = fileSystemCache.get(cacheKey)\n+          // Proactive TGT expiry check: if expired, remove from cache and recreate\n+          if (\n+              HadoopConf.HDFS_TGT_PROACTIVE_CHECK_ENABLE.getValue && !hdfsFileSystemContainer\n+                .isTgtValid()\n+          ) {\n+            logger.info(\n+              s"Cached HDFS FileSystem TGT expired - user: $userName, label: $cacheLabel, removing from cache and recreating"\n             )\n-          fileSystemCache.put(cacheKey, newHDFSFileSystemContainer)\n-          newHDFSFileSystemContainer\n+            fileSystemCache.remove(cacheKey)\n+            IOUtils.closeQuietly(hdfsFileSystemContainer.getFileSystem)\n+            // Fall through to create new FileSystem with fresh TGT\n+          } else {\n+            hdfsFileSystemContainer.addAccessCount()\n+            hdfsFileSystemContainer.updateLastAccessTime\n+            return hdfsFileSystemContainer.getFileSystem\n+          }\n         }\n-        hdfsFileSystemContainer.addAccessCount()\n-        hdfsFileSystemContainer.updateLastAccessTime\n-        hdfsFileSystemContainer.getFileSystem\n+        // Cache miss or TGT expired → create new FileSystem with UGI\n+        val (newFs, newUgi) = createFileSystemWithUgi(userName, label, conf)\n+        val newHDFSFileSystemContainer =\n+          new HDFSFileSystemContainer(newFs, userName, cacheLabel, newUgi)\n+        fileSystemCache.put(cacheKey, newHDFSFileSystemContainer)\n+        newHDFSFileSystemContainer.addAccessCount()\n+        newHDFSFileSystemContainer.updateLastAccessTime\n+        newHDFSFileSystemContainer.getFileSystem\n       }\n     } else {\n       createFileSystem(userName, label, conf)\n@@ -287,6 +309,45 @@ object HDFSUtils extends Logging {\n   def createFileSystem(userName: String, conf: org.apache.hadoop.conf.Configuration): FileSystem =\n     createFileSystem(userName, null, conf)\n \n+  /**\n+   * Create a FileSystem and also return the UserGroupInformation used to create it. The UGI is\n+   * needed for proactive TGT validity checking in the cache layer.\n+   */\n+  private def createFileSystemWithUgi(\n+      userName: String,\n+      label: String,\n+      conf: org.apache.hadoop.conf.Configuration\n+  ): (FileSystem, UserGroupInformation) = {\n+    val createCount = count.getAndIncrement()\n+    val startTime = System.currentTimeMillis()\n+    val labelInfo = if (label == null) "default" else label\n+    logger.info(\n+      s"Creating Hadoop FileSystem - user: $userName, label: $labelInfo, createCount: $createCount"\n+    )\n+    try {\n+      val ugi = getUserGroupInformation(userName, label)\n+      val fs = ugi\n+        .doAs(new PrivilegedExceptionAction[FileSystem] {\n+          def run: FileSystem = FileSystem.newInstance(conf)\n+        })\n+      val duration = System.currentTimeMillis() - startTime\n+      logger.info(\n+        s"Hadoop FileSystem created successfully - user: $userName, label: $labelInfo, duration: ${ByteTimeUtils\n+          .msDurationToString(duration)}, createCount: $createCount"\n+      )\n+      (fs, ugi)\n+    } catch {\n+      case e: Exception =>\n+        val duration = System.currentTimeMillis() - startTime\n+        logger.error(\n+          s"Failed to create Hadoop FileSystem - user: $userName, label: $labelInfo, duration: ${ByteTimeUtils\n+            .msDurationToString(duration)}, createCount: $createCount",\n+          e\n+        )\n+        throw e\n+    }\n+  }\n+\n   def createFileSystem(\n       userName: String,\n       label: String,', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T09:07:52.461Z)

**用户**：
[{'tool_use_id': 'call_9f27a03eb7a14d519f0eeb9a', 'type': 'tool_result', 'content': "diff --git a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/entity/HDFSFileSystemContainer.scala b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/entity/HDFSFileSystemContainer.scala\nindex f87f89393..10bca1c4c 100644\n--- a/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/entity/HDFSFileSystemContainer.scala\n+++ b/linkis-commons/linkis-hadoop-common/src/main/scala/org/apache/linkis/hadoop/common/entity/HDFSFileSystemContainer.scala\n@@ -18,10 +18,17 @@\n package org.apache.linkis.hadoop.common.entity\n \n import org.apache.linkis.hadoop.common.conf.HadoopConf\n+import org.apache.linkis.hadoop.common.utils.KerberosTgtUtils\n \n import org.apache.hadoop.fs.FileSystem\n+import org.apache.hadoop.security.UserGroupInformation\n \n-class HDFSFileSystemContainer(fs: FileSystem, user: String, label: String) {\n+class HDFSFileSystemContainer(\n+    fs: FileSystem,\n+    user: String,\n+    label: String,\n+    ugi: UserGroupInformation = null\n+) {\n \n   private var lastAccessTime: Long = System.currentTimeMillis()\n \n@@ -33,6 +40,8 @@ class HDFSFileSystemContainer(fs: FileSystem, user: String, label: String) {\n \n   def getLabel: String = this.label\n \n+  def getUgi: UserGroupInformation = this.ugi\n+\n   def getLastAccessTime: Long = this.lastAccessTime\n \n   def updateLastAccessTime: Unit = {\n@@ -51,4 +60,14 @@ class HDFSFileSystemContainer(fs: FileSystem, user: String, label: String) {\n     idleTime > HadoopConf.HDFS_ENABLE_CACHE_MAX_TIME || ((idleTime > HadoopConf.HDFS_ENABLE_CACHE_IDLE_TIME) && count <= 0)\n   }\n \n+  /**\n+   * Check if the Kerberos TGT in this container's UGI is still valid. Delegates to\n+   * KerberosTgtUtils.isTgtValid() which checks the KerberosTicket end time against current time.\n+   *\n+   * @return\n+   *   true if TGT is valid (or UGI is null / non-Kerberos); false if TGT has expired → caller\n+   *   should remove from cache and recreate FileSystem\n+   */\n+  def isTgtValid(): Boolean = KerberosTgtUtils.isTgtValid(ugi)\n+\n }", 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T09:08:15.911Z)

**用户**：
[{'tool_use_id': 'call_4fe4f37d277548e3a53ce0fd', 'type': 'tool_result', 'content': '/*\n * Licensed to the Apache Software Foundation (ASF) under one or more\n * contributor license agreements.  See the NOTICE file distributed with\n * this work for additional information regarding copyright ownership.\n * The ASF licenses this file to You under the Apache License, Version 2.0\n * (the "License"); you may not use this file except in compliance with\n * the License.  You may obtain a copy of the License at\n *\n *    http://www.apache.org/licenses/LICENSE-2.0\n *\n * Unless required by applicable law or agreed to in writing, software\n * distributed under the License is distributed on an "AS IS" BASIS,\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n * See the License for the specific language governing permissions and\n * limitations under the License.\n */\n\npackage org.apache.linkis.hadoop.common.utils;\n\nimport org.apache.hadoop.security.UserGroupInformation;\n\nimport javax.security.auth.Subject;\nimport javax.security.auth.kerberos.KerberosTicket;\n\nimport java.lang.reflect.Method;\nimport java.util.Set;\n\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n\npublic class KerberosTgtUtils {\n\n  private static final Logger logger = LoggerFactory.getLogger(KerberosTgtUtils.class);\n\n  private static volatile Method getSubjectMethod = null;\n  private static volatile boolean getSubjectMethodResolved = false;\n\n  /**\n   * Get the Subject from UserGroupInformation via reflection, since getSubject() is protected in\n   * Hadoop 2.7.x.\n   */\n  private static Subject getSubjectFromUgi(UserGroupInformation ugi) {\n    if (!getSubjectMethodResolved) {\n      synchronized (KerberosTgtUtils.class) {\n        if (!getSubjectMethodResolved) {\n          try {\n            Method method = UserGroupInformation.class.getDeclaredMethod("getSubject");\n            method.setAccessible(true);\n            getSubjectMethod = method;\n          } catch (NoSuchMethodException e) {\n            logger.warn("Failed to find getSubject method on UserGroupInformation", e);\n            getSubjectMethod = null;\n          }\n          getSubjectMethodResolved = true;\n        }\n      }\n    }\n    if (getSubjectMethod == null) {\n      return null;\n    }\n    try {\n      return (Subject) getSubjectMethod.invoke(ugi);\n    } catch (Exception e) {\n      logger.warn("Failed to invoke getSubject on UserGroupInformation", e);\n      return null;\n    }\n  }\n\n  /**\n   * Check if the Kerberos TGT in the given UGI is still valid (not expired). For proxy UGI, the\n   * real user\'s TGT is checked.\n   *\n   * @param ugi the UserGroupInformation to check\n   * @return true if UGI is null, security not enabled, or TGT is still valid; false if TGT has\n   *     expired\n   */\n  public static boolean isTgtValid(UserGroupInformation ugi) {\n    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) {\n      return true;\n    }\n    // For proxy UGI, check the real user\'s tickets\n    UserGroupInformation checkUgi = ugi.getRealUser() != null ? ugi.getRealUser() : ugi;\n    Subject subject = getSubjectFromUgi(checkUgi);\n    if (subject == null) {\n      return true;\n    }\n    Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);\n    if (tickets == null || tickets.isEmpty()) {\n      return true;\n    }\n    long now = System.currentTimeMillis();\n    for (KerberosTicket ticket : tickets) {\n      if (ticket.getEndTime().getTime() <= now) {\n        return false;\n      }\n    }\n    return true;\n  }\n}', 'is_error': False}]

**AI**：
[思考] PR #1091 created. Let me verify the PR was created correctly and clean up the temp files.

Let me clean up the temp body files I created....


## 交互记录 (2026-08-12T09:09:00.482Z)

**用户**：
[{'tool_use_id': 'call_1de26497d88d4d749ec12953', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
PR **#1091** 创建成功。我来清理一下临时 body 文件并验证 PR。...


## 交互记录 (2026-08-12T09:09:05.557Z)

**用户**：
[{'tool_use_id': 'call_77b223025cca4b13b5ecddc9', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
✅ PR 创建成功。

## 总结

| 项目 | 详情 |
|------|--------|
| **Issue** | [#1090](https://github.com/WeDataSphere/linkis/issues/1090) — `[feature][PES][datasource] add support for hive datasource sharing with kerberos keytab for non-creator users` |
| **PR** | [#1091](https://github.com/WeDataSphere/linkis/pull/1091) — `[feat][PES][datasource] add hive datasource sharing with kerberos keytab for non-creator users` |
| **分支** | `dev-2.1.0-hive-share` → `dev-2.1.0-webank` |
| **状态** | OPEN, 关联 `close #1090` |

### 创建内容
- **新 Issue #1090** (功能请求) — 未找到相关 Issue，因此创建了新 Issue，描述了多租户 Hive 数据源共享需求。
- **PR #1091** 将该 Issue 关联为 `close #1090`，包含三部分的目的说明、4 点简要变更日志，以及带有 `enhancement` 标签的检查清单。

### 注意事项
- 未执行 `git commit` 操作（根据技能规则，仅创建 PR/Issue）。
- 该仓库 (`WeDataSphere/linkis`) 没有 `status: needs triage` 标签，因此 Issue 和 PR 仅添加了 `enhancement` 标签。
- 检查清单项目 **测试** 和 **文档** 未勾选 — 已添加测试用例文档，但没有单元测试代码，且新的配置开关尚未添加到 `linkis-dist/package/conf/*.properties`。您可能需要添加配置文档，并在合并前考虑为 `isHiveShareEnabled` / keytab 路径逻辑编写单元测试。...


## 交互记录 (2026-08-12T09:09:09.340Z)

**用户**：
[{'tool_use_id': 'call_fe72bec15ed246fda0c92cdd', 'type': 'tool_result', 'content': '(Bash completed with no output)', 'is_error': False}]

**AI**：
...


## 交互记录 (2026-08-12T09:10:59.996Z)

**用户**：
[{'tool_use_id': 'call_63abb2e0f9fc47b88cd54dd6', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\pr-issue-body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
[思考] The user wants me to create a PR for the `dev-2.1.0-keytab-error-fix` branch as well. Following the same workflow as before. Let me first gather the branch info and changes. I need to figure out the base branch - the previous one was `dev-2.1.0-webank`, so likely the same target. Let me check....


## 交互记录 (2026-08-12T09:11:41.572Z)

**用户**：
[{'tool_use_id': 'call_8b027c895d344979832f9fd7', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/issues/1092', 'is_error': False}]

**AI**：
我将为 `dev-2.1.0-keytab-error-fix` 分支创建 PR，目标指向 `dev-2.1.0-webank`（与之前的目标相同）。让我来收集一下更改。...


## 交互记录 (2026-08-12T09:13:18.268Z)

**用户**：
[{'tool_use_id': 'call_29aafe59c3d346d486bda2a4', 'type': 'tool_result', 'content': 'File created successfully at: g:\\kkhuang\\work\\linkis\\.ai\\pr-body.md (file state is current in your context — no need to Read it back)'}]

**AI**：
分支已存在。这是对 `linkis-commons` 中 Kerberos TGT 处理的一个 bug 修复。让我详细检查一下实际的代码变更。...


## 交互记录 (2026-08-12T09:14:56.627Z)

**用户**：
[{'tool_use_id': 'call_6a0fc1a3f5eb4b1298371ce3', 'type': 'tool_result', 'content': 'https://github.com/WeDataSphere/linkis/pull/1093', 'is_error': False}]

**AI**：
现在让我查看一下新文件 `KerberosTgtUtils.java`，以了解 TGT 验证逻辑。...


## 交互记录 (2026-08-12T09:15:15.811Z)

**用户**：
[{'tool_use_id': 'call_bd8bc56eb9b943ca844731e4', 'type': 'tool_result', 'content': '{"baseRefName":"dev-2.1.0-webank","headRefName":"dev-2.1.0-keytab-error-fix","number":1093,"state":"OPEN","title":"[fix][COM][hadoop-common] resolve kerberos tgt expiry issue for cached hdfs filesystem","url":"https://github.com/WeDataSphere/linkis/pull/1093"}', 'is_error': False}]

**AI**：
...

