-- Stage 05 precheck. Read-only: do not change schema or data.
USE question_bank;

SELECT VERSION() AS mysql_version, DATABASE() AS current_database;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'sys_user','qb_user_ability','student_knowledge_state','skill_knowledge',
    'student_basic_profile','student_learning_goal','student_evidence','student_skill_state',
    'ability_dimension','student_ability_state','student_learning_preference',
    'student_profile_category_stat','student_profile_summary','student_profile_snapshot','stage_evaluation'
  )
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable, column_key
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('qb_user_ability','student_knowledge_state','qb_answer','qb_attempt','qb_learning_behavior')
ORDER BY table_name, ordinal_position;

SELECT
  (SELECT COUNT(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
    WHERE UPPER(r.role_code)='STUDENT') AS student_users,
  (SELECT COUNT(*) FROM qb_user_ability) AS legacy_ability_rows,
  (SELECT COUNT(*) FROM student_knowledge_state) AS knowledge_state_rows,
  (SELECT COUNT(*) FROM qb_answer) AS answer_rows,
  (SELECT COUNT(*) FROM qb_learning_behavior) AS behavior_rows;

SELECT u.user_id AS orphan_legacy_ability_user_id
FROM qb_user_ability u
LEFT JOIN sys_user s ON s.id=u.user_id
WHERE s.id IS NULL;

SELECT s.user_id AS non_student_legacy_ability_user_id
FROM qb_user_ability s
LEFT JOIN sys_user_role ur ON ur.user_id=s.user_id
LEFT JOIN sys_role r ON r.id=ur.role_id AND UPPER(r.role_code)='STUDENT'
GROUP BY s.user_id
HAVING COUNT(r.id)=0;

SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema=DATABASE()
  AND referenced_table_name IN ('qb_user_ability','student_basic_profile','student_learning_goal','student_evidence',
      'student_skill_state','ability_dimension','student_ability_state','student_learning_preference',
      'student_profile_category_stat','student_profile_summary','student_profile_snapshot','stage_evaluation')
ORDER BY table_name, constraint_name;

SELECT table_name, view_definition
FROM information_schema.views
WHERE table_schema=DATABASE()
  AND LOWER(view_definition) REGEXP 'qb_user_ability|student_profile|stage_evaluation';
