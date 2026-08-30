SELECT 'knowledge_state_course_missing' check_name,COUNT(*) problem_count FROM student_knowledge_state WHERE course_id IS NULL;
SELECT 'duplicate_course_state' check_name,COUNT(*) problem_count FROM (SELECT user_id,course_id,knowledge_point_id FROM student_knowledge_state GROUP BY user_id,course_id,knowledge_point_id HAVING COUNT(*)>1)x;
SELECT 'invalid_ratio' check_name,COUNT(*) problem_count FROM student_knowledge_state WHERE mastery_value<0 OR mastery_value>1 OR confidence<0 OR confidence>1;
SELECT 'multiple_active_profile_policy' check_name,COUNT(*) problem_count FROM (SELECT 1 FROM profile_algorithm_policy WHERE status='ACTIVE' HAVING COUNT(*)>1)x;
