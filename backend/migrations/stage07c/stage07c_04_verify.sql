USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('student_resource_recommendation','resource_feedback') ORDER BY table_name;
SELECT user_id,resource_id,COUNT(*) duplicate_count FROM student_resource_recommendation GROUP BY user_id,resource_id HAVING COUNT(*)>1;
SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name IN ('student_resource_recommendation','resource_feedback') ORDER BY table_name,constraint_name;
