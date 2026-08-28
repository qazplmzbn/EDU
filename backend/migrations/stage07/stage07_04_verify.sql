-- Stage 07 verification. Read-only; execute manually after migration.
USE question_bank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema=DATABASE()
  AND table_name IN ('course','course_knowledge','student_course_progress','learning_path','learning_path_item')
ORDER BY table_name;

SELECT table_name,index_name,non_unique,column_name
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND table_name IN ('course','course_knowledge','student_course_progress','learning_path','learning_path_item')
ORDER BY table_name,index_name,seq_in_index;

SELECT table_name,constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE table_schema=DATABASE()
  AND table_name IN ('course','course_knowledge','student_course_progress','learning_path','learning_path_item')
ORDER BY table_name,constraint_name;

SELECT path_id,order_no,COUNT(*) AS duplicate_count
FROM learning_path_item
GROUP BY path_id,order_no
HAVING COUNT(*)>1;

SELECT course_id,knowledge_point_id,COUNT(*) AS duplicate_count
FROM course_knowledge
GROUP BY course_id,knowledge_point_id
HAVING COUNT(*)>1;

SELECT user_id,course_id,COUNT(*) AS active_path_count
FROM learning_path
WHERE status='active' AND course_id IS NOT NULL AND is_deleted=0
GROUP BY user_id,course_id
HAVING COUNT(*)>1;

SELECT COUNT(*) AS legacy_snapshot_count,
       SUM(JSON_VALID(snapshot_json)) AS valid_legacy_snapshot_count,
       (SELECT COUNT(*) FROM learning_path WHERE course_id IS NULL AND summary_text LIKE 'legacy_snapshot:%' AND is_deleted=0) AS migrated_legacy_path_count
FROM qb_learning_path_snapshot
WHERE is_deleted=0;

SELECT p.id AS orphan_progress
FROM student_course_progress p
LEFT JOIN course c ON c.id=p.course_id
LEFT JOIN sys_user u ON u.id=p.user_id
WHERE c.id IS NULL OR u.id IS NULL;
