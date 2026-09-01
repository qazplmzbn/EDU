SELECT table_name
FROM information_schema.tables
WHERE table_schema=DATABASE()
  AND table_name IN ('student_occupation_skill_gap','career_course_recommendation_snapshot','career_course_recommendation_item');

SELECT column_name
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='occupation_skill'
  AND column_name IN ('required_level_source','required_level_version','published_batch_code','required_level_updated_at');

SELECT 'invalid_student_skill_state' AS check_name, COUNT(*) AS problem_count
FROM student_skill_state
WHERE proficiency_value NOT BETWEEN 0 AND 1 OR confidence NOT BETWEEN 0 AND 1
   OR core_proficiency_value NOT BETWEEN 0 AND 1 OR knowledge_coverage_rate NOT BETWEEN 0 AND 1;
