SELECT 'course_without_active_completion_policy' check_name,COUNT(*) problem_count FROM course c LEFT JOIN course_completion_policy p ON p.course_id=c.id AND p.status='ACTIVE' WHERE c.is_deleted=0 GROUP BY c.id HAVING COUNT(p.id)=0;
SELECT 'requirement_orphan_course' check_name,COUNT(*) problem_count FROM training_goal_course_requirement r LEFT JOIN course c ON c.id=r.course_id WHERE c.id IS NULL;
SELECT 'invalid_eligibility_snapshot' check_name,COUNT(*) problem_count FROM exam_eligibility_snapshot WHERE JSON_VALID(result_json)=0 OR calculated_at IS NULL;
