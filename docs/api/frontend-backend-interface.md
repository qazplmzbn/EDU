# Agent System 前后端接口文档

## 1 API 说明

### 1.1 接口说明

接口根路径：

```text
http://localhost:8080
```

业务接口统一以 `/api` 开头。前端默认通过 `VITE_API_BASE_URL` 配置后端根路径，未配置时使用：

```text
http://localhost:8080
```

### 1.2 请求方式

| 方法 | 含义 | 说明 |
| --- | --- | --- |
| GET | 查询 | 获取一项或多项资源 |
| POST | 新增/动作 | 新建资源，或触发发布、提交、生成等业务动作 |
| PUT | 修改 | 全量或主要字段更新 |
| PATCH | 局部修改 | 局部状态更新 |
| DELETE | 删除 | 删除指定资源 |

### 1.3 认证方式

除注册、登录和公开展示类接口外，接口统一使用 JWT Token 认证。

请求头：

```http
Authorization: Bearer <token>
```

登录成功后，前端将 token 保存到本地，并在后续请求中自动携带。

### 1.4 角色说明

| 角色 | 说明 |
| --- | --- |
| STUDENT | 学生端，作答、学习统计、学习资源、学习路径、个性化练习 |
| TEACHER | 教师端，题目、试卷、作业、班级、阅卷、智能体资源生成 |
| ADMIN | 管理端，用户、标签、共享题库审核、知识图谱、大模型、系统日志 |

### 1.5 返回状态说明

| HTTP 状态码 | 含义 | 说明 |
| --- | --- | --- |
| 200 | OK | 请求成功，业务结果见响应体 |
| 400 | BAD REQUEST | 参数错误 |
| 401 | UNAUTHORIZED | 未登录或 token 无效 |
| 403 | FORBIDDEN | 权限不足 |
| 404 | NOT FOUND | 资源不存在 |
| 500 | INTERNAL SERVER ERROR | 服务端异常 |

### 1.6 响应数据格式

Spring Boot 后端大多数接口返回统一结构：

```json
{
  "success": true,
  "msg": "ok",
  "code": null,
  "data": {}
}
```

失败示例：

```json
{
  "success": false,
  "msg": "参数错误",
  "code": "PARAM_ERROR",
  "data": null
}
```

分页接口的 `data` 通常为：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 20
}
```

注：前端 `src/api/http.js` 会自动解包 `success=true` 的响应，业务页面通常直接拿到 `data`。

## 2 认证与用户 API

### 2.1 注册

请求路径：`/api/register`

请求方式：POST

权限：公开

请求参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 用户名 |
| password | string | 是 | 8-20 位，包含字母、数字、特殊字符 |
| role | string | 是 | STUDENT/TEACHER/ADMIN |
| displayName | string | 否 | 显示名称 |
| email | string | 否 | 邮箱 |

响应参数：`LoginResponse`，包含 `success`、`token`、`user`、`msg`。

### 2.2 登录

请求路径：`/api/login`

请求方式：POST

权限：公开

请求参数：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| role | string | 否 | 指定登录角色 |

### 2.3 登出

请求路径：`/api/logout`

请求方式：POST

权限：已登录

### 2.4 当前用户

请求路径：`/api/auth/me`

请求方式：GET

权限：已登录

响应参数：当前用户 `id`、`username`、`displayName`、`email`、`role`。

## 3 知识点与题库 API

### 3.1 题目管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 题目分页查询 | `/api/questions` | GET | 已登录 | `keyword`、`chapter`、`difficulty`、`questionType`、`status`、`bankReviewStatus`、`knowledgePointId`、`knowledgePointIds`、`source`、`studentView`、`page`、`size` |
| 题目详情 | `/api/questions/{questionId}` | GET | 已登录 | 路径参数 `questionId` |
| 创建题目 | `/api/questions` | POST | TEACHER/ADMIN | `QuestionUpsertRequest` |
| 修改题目 | `/api/questions/{questionId}` | PUT | TEACHER/ADMIN | `QuestionUpsertRequest` |
| 删除题目 | `/api/questions/{questionId}` | DELETE | TEACHER/ADMIN | 路径参数 `questionId` |
| 发布题目 | `/api/questions/{questionId}/publish` | POST | TEACHER/ADMIN | 路径参数 `questionId` |
| 提交共享题库审核 | `/api/questions/{questionId}/bank-review/submit` | POST | TEACHER | 路径参数 `questionId` |
| 取消共享题库审核 | `/api/questions/{questionId}/bank-review/cancel` | POST | TEACHER | 路径参数 `questionId` |
| 管理员审核题目 | `/api/questions/{questionId}/bank-review` | POST | ADMIN | `reviewStatus`、`reviewComment` |
| 生成 LLM 解析 | `/api/questions/{questionId}/analysis/llm` | POST | TEACHER/ADMIN | `providerKey` |

`QuestionUpsertRequest` 主要字段：

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 是 | 题目标题 |
| questionType | integer | 是 | 题型 |
| difficulty | integer | 是 | 难度 |
| chapter | string | 否 | 章节 |
| stem | string | 是 | 题干 |
| standardAnswer | string | 否 | 标准答案 |
| answerFormat | integer | 否 | 1 文本，2 JSON |
| analysisText | string | 否 | 解析 |
| status | integer | 否 | 1 草稿，2 已发布，3 归档 |
| options | array | 否 | 选项列表 |
| knowledgePointIds | array | 否 | 知识点 ID 列表 |

## 4 试卷、作业与作答 API

### 4.1 试卷管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 试卷分页 | `/api/papers` | GET | TEACHER/ADMIN | `page`、`size` |
| 试卷详情 | `/api/papers/{paperId}` | GET | TEACHER/ADMIN | 路径参数 `paperId` |
| 创建试卷 | `/api/papers` | POST | TEACHER/ADMIN | `paperTitle`、`paperDesc`、`paperType` |
| 修改试卷 | `/api/papers/{paperId}` | PUT | TEACHER/ADMIN | `paperTitle`、`paperDesc`、`paperType` |
| 删除试卷 | `/api/papers/{paperId}` | DELETE | TEACHER/ADMIN | 路径参数 `paperId` |
| 添加试题 | `/api/papers/{paperId}/questions` | POST | TEACHER/ADMIN | `questionId`、`orderNo`、`score` |
| 批量调整试题 | `/api/papers/{paperId}/questions/batch` | PUT | TEACHER/ADMIN | `questions` |
| 修改试卷试题 | `/api/papers/questions/{paperQuestionId}` | PUT | TEACHER/ADMIN | `orderNo`、`score` |
| 移除试卷试题 | `/api/papers/questions/{paperQuestionId}` | DELETE | TEACHER/ADMIN | 路径参数 `paperQuestionId` |

### 4.2 作业管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 创建作业 | `/api/assignments` | POST | TEACHER/ADMIN | `AssignmentUpsertRequest` |
| 修改作业 | `/api/assignments/{assignmentId}` | PUT | TEACHER/ADMIN | `AssignmentUpsertRequest` |
| 删除作业 | `/api/assignments/{assignmentId}` | DELETE | TEACHER/ADMIN | 路径参数 `assignmentId` |
| 作业分页 | `/api/assignments` | GET | TEACHER/ADMIN | `page`、`size`、`keyword` |
| 我的作业 | `/api/assignments/my` | GET | STUDENT | `status`、`page`、`size` |
| 作业详情 | `/api/assignments/{assignmentId}` | GET | 已登录 | 路径参数 `assignmentId` |
| 发布作业 | `/api/assignments/{assignmentId}/publish` | POST | TEACHER/ADMIN | 路径参数 `assignmentId` |
| 关闭作业 | `/api/assignments/{assignmentId}/close` | POST | TEACHER/ADMIN | 路径参数 `assignmentId` |
| 读取投放配置 | `/api/assignments/{assignmentId}/targets` | GET | TEACHER/ADMIN | 返回全班目标、独立学生目标及 `editable`、`lockedReason`，供客户端显示冻结状态 |
| 设置投放对象 | `/api/assignments/{assignmentId}/targets` | PUT | TEACHER/ADMIN | `targets:[{classId,studentIds}]`；空 `studentIds` 表示全班，空 `targets` 表示无人可见；`retainedStudentIds` 保留已存在的独立学生目标 |
| 移除独立学生目标 | `/api/assignments/{assignmentId}/targets/students/{studentId}` | DELETE | TEACHER/ADMIN | 路径参数 `assignmentId`、`studentId` |

`AssignmentUpsertRequest` 主要字段：`paperId`、`assignmentTitle`、`assignmentDesc`、`startTime`、`endTime`、`timeLimitMin`、`maxAttempts`、`shuffleQuestions`、`shuffleOptions`、`publishStatus`。

### 4.3 作答与答案

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 开始作业作答 | `/api/attempts/assignment/{assignmentId}/start` | POST | STUDENT | 路径参数 `assignmentId` |
| 开始自主练习 | `/api/attempts/practice/start` | POST | STUDENT | `mode`、`scope`、`totalScore`、`ruleId` |
| 获取作答题目 | `/api/attempts/{attemptId}/questions` | GET | STUDENT | 路径参数 `attemptId` |
| 提交作答 | `/api/attempts/{attemptId}/submit` | POST | STUDENT | 路径参数 `attemptId` |
| 作答结果 | `/api/attempts/{attemptId}/result` | GET | STUDENT | 路径参数 `attemptId` |
| 我的作答记录 | `/api/attempts/my` | GET | STUDENT | `attemptType`、`page`、`size` |
| 保存答案草稿 | `/api/answers/{answerId}/draft` | PUT | STUDENT | `answerContent` |
| 提交单题答案 | `/api/answers/{answerId}/submit` | PUT | STUDENT | `answerContent` |

注：前端保留了 `/api/attempts/{attemptId}/questions/{attemptQuestionId}/hint` 和 `/check` 的调用封装，但当前后端 Controller 未暴露对应路由；如需启用 AI 答题辅导，需要补充后端接口。

## 5 班级、阅卷与申诉 API

### 5.1 班级管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 创建班级 | `/api/classes` | POST | TEACHER/ADMIN | `className`、`classDesc`、`classCode`、`teacherId` |
| 修改班级 | `/api/classes/{classId}` | PUT | TEACHER/ADMIN | 同创建班级 |
| 删除班级 | `/api/classes/{classId}` | DELETE | TEACHER/ADMIN | 路径参数 `classId` |
| 我的授课班级 | `/api/classes/mine` | GET | TEACHER/ADMIN | 无 |
| 教师选项 | `/api/classes/teachers` | GET | TEACHER/ADMIN | 无 |
| 班级学生 | `/api/classes/{classId}/students` | GET | TEACHER/ADMIN | 路径参数 `classId` |
| 移除学生 | `/api/classes/{classId}/students/{studentId}` | DELETE | TEACHER/ADMIN | 路径参数 |
| 加入班级 | `/api/classes/join` | POST | STUDENT | `classCode` |
| 我的班级 | `/api/classes/my` | GET | STUDENT | 无 |

### 5.2 教师阅卷

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 待阅答案分页 | `/api/teacher/review/answers` | GET | TEACHER/ADMIN | `assignmentId`、`studentId`、`questionType`、`needsReview`、`page`、`size` |
| 答案证据 | `/api/teacher/answers/{answerId}/evidence` | GET | TEACHER/ADMIN | 路径参数 `answerId` |
| 人工评分 | `/api/teacher/answers/{answerId}/grade` | POST | TEACHER/ADMIN | `score`、`comment` |
| LLM 重批 | `/api/teacher/answers/{answerId}/llm-retry` | POST | TEACHER/ADMIN | `providerKey`、`modelName`、`temperature`、`times` |
| LLM 批量批改 | `/api/teacher/answers/llm-batch` | POST | TEACHER/ADMIN | `answerIds`、`assignmentId`、`needsReview`、`providerKey`、`temperature`、`times` |
| 教师可用模型 | `/api/teacher/llm/providers` | GET | TEACHER/ADMIN | `keyword`、`providerType` |
| 作业成绩 | `/api/teacher/assignments/{assignmentId}/scores` | GET | TEACHER/ADMIN | `page`、`size` |
| 作业投放对象 | `/api/teacher/assignments/{assignmentId}/targets` | GET | TEACHER/ADMIN | `page`、`size` |
| 学生作业详情 | `/api/teacher/assignments/{assignmentId}/targets/{studentId}` | GET | TEACHER/ADMIN | 路径参数 |

### 5.3 申诉

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 提交申诉 | `/api/appeals` | POST | STUDENT | `answerId`、`reasonText`、`attachments` |
| 我的申诉 | `/api/appeals/my` | GET | STUDENT | `status`、`page`、`size` |
| 教师申诉列表 | `/api/teacher/appeals` | GET | TEACHER/ADMIN | `status`、`page`、`size` |
| 处理申诉 | `/api/teacher/appeals/{appealId}/handle` | POST | TEACHER/ADMIN | `action`、`finalScore`、`decisionComment` |

## 6 智能学习与知识图谱 API

### 6.1 知识点与资源

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 知识点列表 | `/api/learning/knowledge-points` | GET | 已登录 | 无 |
| 创建知识点 | `/api/learning/knowledge-points` | POST | TEACHER/ADMIN | `QbKnowledgePoint` |
| 修改知识点 | `/api/learning/knowledge-points/{id}` | PUT | TEACHER/ADMIN | `QbKnowledgePoint` |
| 删除知识点 | `/api/learning/knowledge-points/{id}` | DELETE | TEACHER/ADMIN | 路径参数 `id` |
| 学习资源列表 | `/api/learning/resources` | GET | 已登录 | `keyword`、`knowledgePointId`、`limit` |
| 创建学习资源 | `/api/learning/resources` | POST | TEACHER/ADMIN | `QbLearningResource` |
| 推荐资源投放对象 | `/api/learning/resources/{id}/recommend-targets` | POST | TEACHER/ADMIN | `targetType`、`classId`、`studentIds` |
| 修改学习资源 | `/api/learning/resources/{id}` | PUT | TEACHER/ADMIN | `QbLearningResource` |
| 删除学习资源 | `/api/learning/resources/{id}` | DELETE | TEACHER/ADMIN | 路径参数 `id` |
| 记录学习行为 | `/api/learning/behaviors` | POST | STUDENT | `QbLearningBehavior` |

### 6.2 学习画像与推荐

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 学习画像 | `/api/learning/profile` | GET | STUDENT | 无 |
| 画像报告 | `/api/learning/profile/report` | GET | STUDENT | 无 |
| 我的基础画像 | `/api/student-profile/basic` | GET/PUT | STUDENT | 基础画像字段 |
| 我的学习目标 | `/api/student-profile/goals` | GET/POST | STUDENT | 目标字段 |
| 修改学习目标 | `/api/student-profile/goals/{id}` | PUT | STUDENT | 目标字段 |
| 我的学习偏好 | `/api/student-profile/preferences` | GET/PUT | STUDENT | 偏好字段 |
| 学生画像摘要 | `/api/student-profiles/{studentId}/summary` | GET | TEACHER/ADMIN | 路径参数 `studentId` |
| 智能推荐 | `/api/learning/recommendations` | GET | STUDENT | 无 |
| 学习路径推荐 | `/api/learning/path-recommendation` | GET | STUDENT | `stage`、`goal`、`days` |
| 保存路径快照 | `/api/learning/path-recommendation/snapshots` | POST | STUDENT | `stage`、`goal`、`days`、`snapshot` |
| 路径快照详情 | `/api/learning/path-recommendation/snapshots/{id}` | GET | STUDENT | 路径参数 `id` |
| 路径快照列表 | `/api/learning/path-recommendation/snapshots` | GET | STUDENT | `limit` |
| 个性化练习计划 | `/api/learning/personalized-practice/plan` | GET | STUDENT | `mode`、`totalScore` |
| 开始个性化练习 | `/api/learning/personalized-practice/start` | POST | STUDENT | `mode`、`totalScore` |

注：前端保留了 `/api/learning/profile/extract-features` 调用封装，但当前后端 Controller 未暴露对应路由。

### 6.3 知识图谱

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 知识关系列表 | `/api/learning/knowledge-relations` | GET | ADMIN | 无 |
| 文本抽取图谱 | `/api/learning/knowledge-graph/extract` | POST | ADMIN | `sourceText`、`providerKey`、`autoSave` |
| 文件抽取图谱 | `/api/learning/knowledge-graph/extract-file` | POST | ADMIN | multipart `file`、`autoSave`、`providerKey` |
| 创建知识关系 | `/api/learning/knowledge-relations` | POST | ADMIN | `QbKnowledgeRelation` |
| 修改知识关系 | `/api/learning/knowledge-relations/{id}` | PUT | ADMIN | `QbKnowledgeRelation` |
| 删除知识关系 | `/api/learning/knowledge-relations/{id}` | DELETE | ADMIN | 路径参数 `id` |

## 7 大模型与智能体 API

### 7.1 学生/管理员模型配置

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 学生模型列表 | `/api/student/llm/providers` | GET | STUDENT | `keyword`、`providerType` |
| 学生新增模型 | `/api/student/llm/providers` | POST | STUDENT | `StudentLlmProviderRequest` |
| 学生修改模型 | `/api/student/llm/providers/{id}` | PUT | STUDENT | `StudentLlmProviderRequest` |
| 学生启停模型 | `/api/student/llm/providers/{id}/enabled` | PATCH | STUDENT | `enabled` |
| 学生设为默认 | `/api/student/llm/providers/{id}/default` | PATCH | STUDENT | 路径参数 `id` |
| 学生删除模型 | `/api/student/llm/providers/{id}` | DELETE | STUDENT | 路径参数 `id` |
| 学生提示词模板 | `/api/student/llm/templates` | GET/POST | STUDENT | 查询或创建模板 |
| 学生修改模板 | `/api/student/llm/templates/{id}` | PUT | STUDENT | `StudentPromptTemplateRequest` |
| 学生删除模板 | `/api/student/llm/templates/{id}` | DELETE | STUDENT | 路径参数 `id` |
| 管理员模型列表 | `/api/admin/llm/providers` | GET | ADMIN | `keyword`、`providerType` |
| 管理员模型新增 | `/api/admin/llm/providers` | POST | ADMIN | `StudentLlmProviderRequest` |
| 管理员模型修改 | `/api/admin/llm/providers/{id}` | PUT | ADMIN | `StudentLlmProviderRequest` |
| 管理员模型启停 | `/api/admin/llm/providers/{id}/enabled` | PATCH | ADMIN | `enabled` |
| 管理员设默认 | `/api/admin/llm/providers/{id}/default` | PATCH | ADMIN | 路径参数 `id` |
| 管理员删除模型 | `/api/admin/llm/providers/{id}` | DELETE | ADMIN | 路径参数 `id` |

### 7.2 模型中心

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 模型列表 | `/api/models` | GET | 已登录 | `keyword`、`category`、`model_type`、`status`、`enabled` |
| 模型详情 | `/api/models/{modelId}` | GET | 已登录 | 路径参数 `modelId` |
| 创建模型 | `/api/models` | POST | 已登录 | 模型配置 Map |
| 修改模型 | `/api/models/{modelId}` | PUT | 已登录 | 模型配置 Map |
| 删除模型 | `/api/models/{modelId}` | DELETE | 已登录 | 路径参数 `modelId` |
| 配置 API Key | `/api/models/{modelId}/api-key` | PUT | 已登录 | `apiKey` |
| 测试模型 | `/api/models/{modelId}/test` | POST | 已登录 | `testPrompt` |
| 调用模型 | `/api/models/{modelId}/call` | POST | 已登录 | `prompt`、`systemPrompt` |
| 提示词模板列表 | `/api/models/templates` | GET | 已登录 | `keyword`、`task_type`、`model_id` |
| 模板详情 | `/api/models/templates/{templateId}` | GET | 已登录 | 路径参数 `templateId` |
| 创建模板 | `/api/models/templates` | POST | 已登录 | 模板配置 Map |
| 修改模板 | `/api/models/templates/{templateId}` | PUT | 已登录 | 模板配置 Map |
| 删除模板 | `/api/models/templates/{templateId}` | DELETE | 已登录 | 路径参数 `templateId` |
| 模型对比 | `/api/models/compare` | GET | 已登录 | `model_ids` |
| 推理历史 | `/api/models/inference-history` | GET | 已登录 | `model_id`、`limit` |
| 提供商列表 | `/api/models/providers` | GET | 已登录 | 无 |
| Ollama 状态 | `/api/models/ollama/status` | GET | 已登录 | `base_url` |
| Ollama 模型列表 | `/api/models/ollama/models` | GET | 已登录 | `base_url` |
| 已配置 Ollama | `/api/models/ollama/configured` | GET | 已登录 | 无 |
| 已配置 API 模型 | `/api/models/api/configured` | GET | 已登录 | 无 |

### 7.3 LLM 调用记录与学生助手

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| LLM 调用分页 | `/api/llm/calls` | GET | TEACHER/ADMIN | `page`、`size` 等过滤参数 |
| LLM 调用详情 | `/api/llm/calls/{llmCallId}` | GET | TEACHER/ADMIN | 路径参数 `llmCallId` |
| 学生助手聊天 | `/api/student/assistant/chat` | POST | STUDENT | `message`、`history`、`pageContext`、`providerKey` |

### 7.4 教师智能体资源生成

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 同步生成资源 | `/api/teacher/agent-resources/generate` | POST | TEACHER/ADMIN | `TeacherAgentResourceGenerateRequest` |
| 创建生成任务 | `/api/teacher/agent-resources/generate-tasks` | POST | TEACHER/ADMIN | `TeacherAgentResourceGenerateRequest` |
| 查询任务状态 | `/api/teacher/agent-resources/generate-tasks/{taskId}` | GET | TEACHER/ADMIN | 路径参数 `taskId` |
| 取消任务 | `/api/teacher/agent-resources/generate-tasks/{taskId}/cancel` | POST | TEACHER/ADMIN | 路径参数 `taskId` |

### 7.5 Agent 任务追溯

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 任务详情 | `/api/agent-tasks/{taskCode}` | GET | TEACHER/ADMIN | 路径参数 `taskCode` |
| 任务步骤、审核与决策 | `/api/agent-tasks/{taskCode}/trace` | GET | TEACHER/ADMIN | 路径参数 `taskCode` |
| Agent 定义列表 | `/api/admin/agents` | GET | ADMIN | 无 |
| 新增 Agent 定义 | `/api/admin/agents` | POST | ADMIN | `agentCode`、名称、角色、模型/模板配置 |
| 创建 Agent 新版本 | `/api/admin/agents/{id}` | PUT | ADMIN | 名称、角色、模型/模板配置 |
| 重加密旧模型密钥 | `/api/admin/llm/providers/reencrypt-legacy-keys` | POST | ADMIN | 无；需先设置 `APP_LLM_ENCRYPTION_KEY` |

阶段六说明：教师资源生成、会诊和异步任务均以持久化 Agent 任务为追溯来源。教师只能查看自己发起的任务，管理员可查看全部；学生不能读取原始提示词、模型输出和任务证据。模型 API Key 仅以加密形式保存，所有模型接口只返回是否已配置和脱敏状态，不返回原文密钥。

`TeacherAgentResourceGenerateRequest` 主要字段：`studentId`、`stage`、`startDate`、`endDate`、`resourceTypes`、`generationScope`、`classId`、`difficulty`、`exerciseCount`、`publishMode`、`selectedWeakPoints`、`selectedResourceTypes`、`providerKey`、`agentProviderKeys`、`teacherRequirement`、`feedback`。

## 8 统计、阶段评价与公开展示 API

### 8.1 学习统计

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 错题分页 | `/api/stats/wrong-questions` | GET | STUDENT | `page`、`size` 等过滤参数 |
| 标记错题已解决 | `/api/stats/wrong-questions/{questionId}/resolve` | POST | STUDENT | 路径参数 `questionId` |
| 掌握度 | `/api/stats/mastery` | GET | STUDENT | 无；返回知识点掌握度 |
| 能力画像 | `/api/stats/ability` | GET | STUDENT | 无；兼容返回 `userId`、`abilityScore`，数据源为 `student_ability_state` |
| 题目统计 | `/api/stats/question-stats` | GET | STUDENT | `page`、`size` 等过滤参数 |

### 8.2 阶段评价

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 我的阶段评价 | `/api/stage-evaluations/my` | GET | STUDENT | 阶段/时间等查询参数 |
| 教师查看学生阶段评价 | `/api/stage-evaluations/teacher/students` | GET | TEACHER/ADMIN | 学生/班级等查询参数 |
| 生成阶段评价 | `/api/stage-evaluations` | POST | TEACHER/ADMIN | `studentId`、`stage` 或 `startDate/endDate` |
| 我的评价历史 | `/api/stage-evaluations/my/history` | GET | STUDENT | `limit` |
| 学生评价历史 | `/api/stage-evaluations/teacher/students/{studentId}/history` | GET | TEACHER/ADMIN | `limit` |

阶段五说明：`GET /api/stage-evaluations/my` 与教师查询接口只读取已持久化的最新评价；尚未生成时返回 `generated=false`，不会在查询时写入数据。教师仅能生成或查看自己班级学生的评价，管理员可操作任意学生。每次生成均创建新的画像快照和阶段评价版本，历史结果不会覆盖。

### 8.3 公开展示与能力层同步

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 首页能力层数据 | `/api/landing/competency-layer` | GET | 公开 | 无 |
| 手动同步岗位能力数据 | `/api/admin/landing/jobs/sync` | POST | ADMIN | 无 |
| 同步记录 | `/api/admin/landing/jobs/sync-records` | GET | ADMIN | `limit` |

## 9 管理端 API

### 9.1 用户管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 用户分页 | `/api/admin/users` | GET | ADMIN | `page`、`size` |
| 创建用户 | `/api/admin/users` | POST | ADMIN | `username`、`password`、`displayName`、`email`、`status`、`role` |
| 修改用户 | `/api/admin/users/{userId}` | PUT | ADMIN | `displayName`、`email`、`status` |
| 修改用户角色 | `/api/admin/users/{userId}/role` | PUT | ADMIN | `role` |

### 9.2 系统日志

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 登录日志 | `/api/admin/login-logs` | GET | ADMIN | `page`、`size` 等过滤参数 |
| 审计日志 | `/api/admin/audit-logs` | GET | ADMIN | `page`、`size` 等过滤参数 |

## 10 前端接口封装位置

前端统一请求封装：

```text
frontend/src/api/http.js
```

业务 API 封装：

```text
frontend/src/api/services.js
```

前端路由与角色限制：

```text
frontend/src/router/index.js
```

## 11 阶段 7：课程与正式学习路径 API

### 11.1 教师/管理员课程管理

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 创建课程 | `/api/courses` | POST | TEACHER/ADMIN | `courseCode`、`courseName`、`description`、`status`；管理员可传 `teacherId` |
| 修改课程 | `/api/courses/{courseId}` | PUT | TEACHER/ADMIN | 同创建；教师仅能修改自己的课程 |
| 删除课程 | `/api/courses/{courseId}` | DELETE | TEACHER/ADMIN | 路径参数 `courseId`，逻辑删除 |
| 可管理课程 | `/api/courses/mine` | GET | TEACHER/ADMIN | 教师返回自己的课程，管理员返回全部 |
| 查询课程知识点 | `/api/courses/{courseId}/knowledge` | GET | TEACHER/ADMIN | 路径参数 `courseId` |
| 替换课程知识点 | `/api/courses/{courseId}/knowledge` | PUT | TEACHER/ADMIN | `items[]`：`knowledgePointId`、`sequenceNo`、`core`、`coverageWeight` |
| 查看学生课程进度 | `/api/courses/{courseId}/students/progress` | GET | TEACHER/ADMIN | 路径参数 `courseId` |
| 查看学生正式路径 | `/api/courses/paths/{pathId}` | GET | TEACHER/ADMIN | 路径参数 `pathId`；教师仅能查看自己课程且属于自己班级学生的路径 |

### 11.2 学生课程与路径

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 可学习课程 | `/api/learning/courses` | GET | STUDENT | 仅返回所在班级授课教师创建的 `active` 课程及当前进度 |
| 课程进度 | `/api/learning/courses/{courseId}/progress` | GET | STUDENT | 路径参数 `courseId` |
| 生成课程路径 | `/api/learning/courses/{courseId}/paths` | POST | STUDENT | 可选 `planningDays`（1–365）、`stage`；新路径替换同课程旧活动路径 |
| 我的正式路径 | `/api/learning/paths` | GET | STUDENT | 无 |
| 路径详情 | `/api/learning/paths/{pathId}` | GET | STUDENT | 路径参数 `pathId` |
| 完成路径节点 | `/api/learning/paths/{pathId}/items/{itemId}/complete` | POST | STUDENT | 幂等；首次完成记录学习行为并重算课程进度 |

旧 `/api/learning/path-recommendation/snapshots/**` 保持只读历史兼容。阶段 7 新生成的正式路径写入 `learning_path` 与 `learning_path_item`；历史快照迁移路径的 `courseId` 为 `null`。

## 12 阶段 7B：可信知识来源与资源溯源 API

以下接口仅管理员可调用。上传文件保存为本地 `file_asset`，并自动创建来源文档、文本切片；支持 txt、md、csv、json、sql、html 和 docx，单文件不超过 5MB。

| 功能 | 请求路径 | 方法 | 参数 |
| --- | --- | --- | --- |
| 上传可信来源文档 | `/api/admin/knowledge-sources/documents` | POST | multipart `file`；可选 `title`、`documentType`、`authorOrg`、`sourceUrl`、`version`、`authorityLevel` |
| 来源文档列表 | `/api/admin/knowledge-sources/documents` | GET | 无 |
| 查询文档切片 | `/api/admin/knowledge-sources/documents/{id}/chunks` | GET | 路径参数 `id` |
| 绑定知识点证据 | `/api/admin/knowledge-sources/knowledge-points/{id}/evidence` | POST | `sourceChunkId`、`supportType`、`relevanceScore`、`confidence`、`linkMethod`、`reviewStatus` |
| 查询知识点证据 | `/api/admin/knowledge-sources/knowledge-points/{id}/evidence` | GET | 路径参数 `id` |
| 绑定资源证据 | `/api/admin/knowledge-sources/resources/{id}/evidence` | POST | `sourceChunkId`、`supportType`、`relevanceScore`、`citationOrder` |
| 查询资源证据 | `/api/admin/knowledge-sources/resources/{id}/evidence` | GET | 路径参数 `id` |

## 13 阶段 7C、7D：推荐反馈与学习对话 API

| 功能 | 请求路径 | 方法 | 权限 | 参数 |
| --- | --- | --- | --- | --- |
| 刷新资源推荐记录 | `/api/learning/recommendation-records/refresh` | POST | STUDENT | 无 |
| 我的推荐记录 | `/api/learning/recommendation-records` | GET | STUDENT | 无 |
| 更新推荐状态 | `/api/learning/recommendation-records/{id}/status` | PUT | STUDENT | `recommended/viewed/started/completed/skipped/expired` |
| 提交资源反馈 | `/api/learning/recommendation-records/resources/{resourceId}/feedback` | POST | STUDENT | `recommendationId`、`feedbackType`、`rating`、`feedbackValue`、`feedbackText` |
| 学生助手聊天 | `/api/student/assistant/chat` | POST | STUDENT | 既有字段；新增可选 `sessionId` |
| 我的对话会话 | `/api/student/assistant/sessions` | GET | STUDENT | 无 |
| 会话消息 | `/api/student/assistant/sessions/{sessionId}/messages` | GET | STUDENT | 路径参数 `sessionId` |

推荐刷新持久化薄弱知识点匹配资源和已定向投放资源；学生只能更新、反馈和读取自己的记录。助手聊天未传 `sessionId` 时自动创建会话，每轮用户消息与助手回复均持久化，且助手回复关联 LLM 调用记录。

## 14 阶段 08–16：个性化学习闭环 API

所有 `/api/v1` 请求返回 `X-Correlation-Id`；创建路径、资源任务、重生成和交互提交必须携带 `Idempotency-Key`。内部接口只允许 `INTERNAL_SERVICE` 或管理员调用。

| 领域 | 方法 | 路径 | 核心输入/输出 |
| --- | --- | --- | --- |
| 课程目录 | GET | `/api/v1/courses/{courseId}/catalog` | 章节、课程知识点、ACTIVE graphVersion |
| 图版本 | POST | `/api/v1/admin/courses/{courseId}/graph-versions` | 创建 DRAFT 图版本 |
| 图关系 | PUT | `/api/v1/admin/graph-versions/{versionCode}/relations` | 全量覆盖草稿关系及 sourceChunkIds |
| 图校验/发布 | POST | `/api/v1/admin/graph-versions/{versionCode}/validate`、`/publish` | 结构问题、哈希、节点/边数、状态 |
| 画像摘要 | GET | `/api/v1/students/me/profile-summary?courseId=` | profileVersion 与课程级五维摘要 |
| 画像工具 | GET | `/internal/v1/profiles/{userId}/courses/{courseId}/knowledge-states`、`resource-preferences`、`cognitive-profile` | Agent 使用的版本化工具证据 |
| 路径 | POST/GET | `/api/v1/learning-path-sessions`、`/{pathCode}`、`/{pathCode}/refresh` | 仅返回知识点步骤 |
| 资源任务 | POST/GET | `/api/v1/personalized-resources/jobs`、`/jobs/{jobCode}` | ResourceUnit → Blueprint → 生成/监督状态 |
| 资源详情 | GET | `/api/v1/personalized-resources/{bundleCode}` | 学生仅获得 VISIBLE 项，永不返回 grading key |
| 重生成 | POST | `/api/v1/personalized-resources/{bundleCode}/regenerate` | 新 bundle version，不覆盖旧版本 |
| 隐藏题释放 | POST | `/internal/v1/personalized-resources/{bundleCode}/hidden-assessments/{itemCode}/release` | userId、expiresAt |
| 当前检测题 | GET | `/api/v1/personalized-resources/{bundleCode}/assessments/active` | 只返回已释放题目，不返回答案 |
| 最终交互 | POST | `/api/v1/resource-interactions` | generatedQuestionCode、answer、actionOrigin、clientOccurredAt |
| 交互结果 | GET | `/api/v1/resource-interactions/{interactionCode}/result` | 判分、profileVersion、resourceAction |
| 课程结果 | GET | `/api/v1/courses/{courseId}/progress`、`/api/v1/training-goals/{goalId}/exam-eligibility`、`/api/v1/students/me/learning-report` | 进度、资格快照、可审计报告 |

模型中心新增 `/internal/v1/dimkt/infer`、`/recalibrate` 和 `/models/{version}/health`。DIMKT 未通过模型清单、知识索引和权重哈希校验时返回不可用，Java 自动回退 `weighted_bkt_elo_v1`。

## 15 阶段 17–19：课程图谱导入与发布

以下接口仅允许管理员调用。首轮 commit 只接受 `course_id=C`、`course=C语言`、`schema_version=3.0` 和 `mode=STRUCTURE_ONLY`；其他课程图可校验但不能提交。JSON 内教学资源不导入。

| 功能 | 方法 | 路径 | 核心输入/输出 |
| --- | --- | --- | --- |
| 校验课程图谱 | POST | `/api/v1/admin/course-graph-imports/validate` | multipart `file`、`mode`；返回计数、ERROR/WARNING 和三类 hash，不落库 |
| 提交课程图谱 | POST | `/api/v1/admin/course-graph-imports` | Header `Idempotency-Key`；multipart `file`、`validationHash`、`mode` |
| 查询导入详情 | GET | `/api/v1/admin/course-graph-imports/{importCode}` | 状态、计数、hash、issue、legacy mapping、courseId、graphVersionCode |
| 审核导入 | POST | `/api/v1/admin/course-graph-imports/{importCode}/approve` | 仅无未解决 ERROR 的 IMPORTED 记录可通过；重复审核幂等 |

提交时服务端重新解析文件并计算 validationHash。hash 不一致返回冲突且不落库；校验存在 ERROR 时保存 REJECTED 导入和 issue，但不写课程域数据。有效 C 语言导入固定生成 12 章、51 个 ACTIVE 知识点、26 条 PREREQUISITE、17 条 SIMILAR 和 43 条关系来源证据。

Imported graph 在导入状态变为 APPROVED 前不能调用图校验或发布。图发布只同步 ACTIVE 且 `metadata.pathEligible=true` 的节点；C 语言 Neo4j 验收计数固定为 51 节点、43 边。

