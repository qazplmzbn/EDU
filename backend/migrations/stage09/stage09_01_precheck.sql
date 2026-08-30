-- Stage 09 precheck (read-only).
SELECT 'knowledge_state_orphan_user' check_name,COUNT(*) problem_count FROM student_knowledge_state s LEFT JOIN sys_user u ON u.id=s.user_id WHERE u.id IS NULL;
SELECT 'knowledge_state_orphan_point' check_name,COUNT(*) problem_count FROM student_knowledge_state s LEFT JOIN knowledge_point k ON k.id=s.knowledge_point_id WHERE k.id IS NULL;
SELECT 'ambiguous_state_course' check_name,COUNT(*) problem_count FROM student_knowledge_state s JOIN (SELECT knowledge_point_id,COUNT(DISTINCT course_id)c FROM course_knowledge GROUP BY knowledge_point_id) x ON x.knowledge_point_id=s.knowledge_point_id WHERE x.c<>1;
