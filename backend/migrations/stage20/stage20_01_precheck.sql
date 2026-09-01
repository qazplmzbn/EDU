-- Stage 20 precheck: only reports prerequisites; no data is changed.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('occupation_skill','skill_knowledge','student_skill_state',
                     'student_knowledge_state','course','course_knowledge');

SELECT COUNT(*) AS unpublished_occupation_skill_count
FROM occupation_skill
WHERE required_level IS NULL;
