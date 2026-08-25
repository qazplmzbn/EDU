# Agent System

Agent System 是一个面向 C 语言课程教学场景的智能学习与题库管理系统。系统围绕“题库、作业、班级、阅卷、知识图谱、学习画像、学习路径推荐、大模型调用与智能体资源生成”构建，支持学生、教师、管理员三类角色协同使用。

## 功能特性

- 用户认证与角色权限：支持学生、教师、管理员注册登录，基于 JWT 进行接口鉴权。
- 题库与标签管理：支持题目增删改查、标签树、题目发布、共享题库审核、LLM 题目解析生成。
- 试卷与作业管理：教师可组卷、发布作业或考试、设置班级/学生投放范围。
- 在线作答与成绩反馈：学生可开始作业或个性化练习，保存答案、提交作答、查看结果与历史记录。
- 教师阅卷中心：支持人工评分、LLM 重批、批量批改、成绩明细、作业目标学生跟踪和申诉处理。
- 智能学习模块：支持知识点、学习资源、学习行为、知识掌握画像、学习路径推荐和个性化练习。
- 知识图谱抽取：管理员可基于文本或文件抽取知识点关系，并维护知识图谱边数据。
- 大模型中心：支持 API 模型、Ollama 本地模型、提示词模板、模型测试、模型调用和调用记录。
- 多智能体资源生成：面向教师端生成学习资源、练习、反馈、报告等教学辅助内容。
- 可视化前台：提供公开首页、知识图谱、课程目标层、教学应用层等展示页面。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、Vue Router、Pinia、Element Plus、Tailwind CSS、Three.js、GSAP、Remotion |
| 后端 | Java 17、Spring Boot 3、Spring Security、MyBatis、MySQL |
| 模型中心 | Spring Boot 内置模型中心接口；另保留 FastAPI 版 `model-center-service` |
| 大模型 | OpenAI Java SDK 兼容接口、DashScope、百度千帆、Ollama |
| 数据库 | MySQL 8.x |

## 项目结构

```text
Agent System/
├── backend/                 # Spring Boot 后端服务
│   ├── src/main/java/       # Controller、Service、Mapper、Entity、DTO、VO
│   ├── src/main/resources/  # application.properties 与 MyBatis XML
│   ├── database_*.sql       # 数据库初始化与增量脚本
│   └── pom.xml
├── frontend/                # Vue 3 前端项目
│   ├── src/api/             # 前端接口封装
│   ├── src/views/           # 页面视图
│   ├── src/components/      # 通用组件与业务组件
│   └── package.json
├── model-center-service/    # FastAPI 版模型中心服务，可选
├── picture/                 # 智能体示意图资源
├── docs/api/                # 接口文档
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 20.19+ 或 22.12+
- MySQL 8.x
- 可选：Python 3.9+、Ollama

## 快速开始

### 1. 初始化数据库

在 MySQL 中创建数据库：

```sql
CREATE DATABASE question_bank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

新建数据库推荐先执行 `backend/` 下的初始化脚本；其中已包含阶段一至三的最终职业、技能、知识点、资源关系与统一作业投放结构：

```text
backend/database_full_init.sql
```

已存在的旧数据库不要重复执行初始化脚本，应按阶段迁移脚本完成结构转换。阶段四的预检、校验与受门禁保护的清理脚本位于 `backend/migrations/stage04/`，所有 SQL 均由用户手工执行；未获得发布确认时不得执行 cleanup。

如需启用智能学习、知识图谱、模型中心、能力层同步等扩展能力，再执行对应增量脚本：

```text
backend/database_smart_learning.sql
backend/database_knowledge_graph.sql
backend/database_learning_path_snapshot.sql
backend/database_student_llm.sql
backend/database_competency_landing.sql
backend/database_teacher_agent_resource_upgrade.sql
```

### 2. 启动后端

修改 `backend/src/main/resources/application.properties` 中的数据库、JWT 与大模型配置。

```powershell
cd backend
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

开发初始化账号见 `application.properties`：

```text
admin / admin123
```

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

如后端地址不是 `http://localhost:8080`，可在前端环境变量中配置：

```text
VITE_API_BASE_URL=http://localhost:8080
```

### 4. 可选：启动 FastAPI 模型中心服务

当前 Spring Boot 后端已包含 `/api/models/**` 模型中心接口；`model-center-service` 是保留的独立服务版本。

```powershell
cd model-center-service
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload
```

## 常用页面

| 页面 | 路径 | 角色 |
| --- | --- | --- |
| 登录/公开首页 | `/login` | 公开 |
| 知识图谱展示 | `/knowledge-map` | 公开 |
| 学生作业 | `/assignments/my` | 学生 |
| 在线作答 | `/attempts/:attemptId/work` | 学生 |
| 学习画像 | `/knowledge-profile` | 学生 |
| 学习路径 | `/learning-path` | 学生 |
| 题目管理 | `/questions` | 教师/管理员 |
| 作业管理 | `/assignments/manage` | 教师/管理员 |
| 阅卷中心 | `/teacher/review` | 教师/管理员 |
| 智能体资源生成 | `/teacher/agent-resources` | 教师/管理员 |
| 大模型管理 | `/admin/llm/models` | 管理员 |
| 用户管理 | `/admin/users` | 管理员 |

## 接口文档

前后端接口文档见：

```text
docs/api/frontend-backend-interface.md
```

统一接口前缀为：

```text
http://localhost:8080/api
```

除注册、登录和公开展示接口外，其余接口需要请求头：

```http
Authorization: Bearer <token>
```

## 测试与构建

后端测试：

```powershell
cd backend
mvn test
```

前端生产构建：

```powershell
cd frontend
npm run build
```


