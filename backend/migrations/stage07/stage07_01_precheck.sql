-- Stage 07 precheck. Read-only; execute manually before schema changes.
USE question_bank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema=DATABASE()
  AND table_name IN ('course','course_knowledge','student_course_progress','learning_path','learning_path_item','qb_learning_path_snapshot')
ORDER BY table_name;

SELECT table_name,column_name,column_type,is_nullable,column_default
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('qb_learning_path_snapshot','qb_class','qb_class_member','knowledge_point','student_profile_snapshot')
ORDER BY table_name,ordinal_position;

SELECT COUNT(*) AS legacy_snapshot_count,
       SUM(JSON_VALID(snapshot_json)) AS valid_json_snapshot_count,
       SUM(NOT JSON_VALID(snapshot_json)) AS invalid_json_snapshot_count
FROM qb_learning_path_snapshot
WHERE is_deleted=0;

SELECT COUNT(*) AS active_class_members
FROM qb_class_member m
JOIN qb_class c ON c.id=m.class_id AND c.is_deleted=0;

SELECT COUNT(*) AS active_knowledge_points FROM knowledge_point WHERE is_deleted=0;

SELECT constraint_name,table_name,constraint_type
FROM information_schema.table_constraints
WHERE table_schema=DATABASE()
  AND table_name IN ('course','course_knowledge','student_course_progress','learning_path','learning_path_item')
ORDER BY table_name,constraint_name;
