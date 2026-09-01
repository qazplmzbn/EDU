# Stage 21 - 职业映射 CSV 暂存与人工审核

导入 `data/技能知识映射` 的 13 列原始 CSV 到审计暂存表。导入只生成候选/缺失状态，不直接覆盖 `occupation_skill`、`skill_knowledge` 或课程图谱。

执行顺序：`stage21_01_precheck.sql -> stage21_02_schema.sql -> stage21_04_verify.sql`。清理脚本默认阻止执行。
