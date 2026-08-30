SELECT 'goal_orphan_user' check_name,COUNT(*) problem_count FROM student_learning_goal g LEFT JOIN sys_user u ON u.id=g.user_id WHERE u.id IS NULL;
SELECT 'course_without_knowledge' check_name,COUNT(*) problem_count FROM course c LEFT JOIN course_knowledge ck ON ck.course_id=c.id WHERE c.is_deleted=0 GROUP BY c.id HAVING COUNT(ck.knowledge_point_id)=0;
