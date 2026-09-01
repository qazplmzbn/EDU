# Stage 20 - 职业技能差距与课程推荐 MVP

本阶段在既有 `occupation`、`occupation_skill`、`skill_knowledge`、`student_knowledge_state`、`course_knowledge` 基础上，增加岗位标准发布元数据、学生岗位差距快照和课程推荐快照。

执行顺序：`stage20_01_precheck.sql -> stage20_02_schema.sql -> stage20_03_migrate.sql -> stage20_04_verify.sql`。`stage20_05_cleanup.sql` 明确禁止执行，不删除旧字段或历史快照。

MySQL 位于 Docker 容器时，可将每个脚本通过 `docker exec -i edu-mysql mysql ...` 执行；连接凭据应使用项目部署配置，不写入本仓库。
