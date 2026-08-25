-- Stage 04 verification. Read-only; run before and again after cleanup.
USE question_bank;

WITH expected_columns AS (
  SELECT 'qb_assignment_target' AS table_name, 'id' AS column_name UNION ALL
  SELECT 'qb_assignment_target', 'assignment_id' UNION ALL
  SELECT 'qb_assignment_target', 'target_type' UNION ALL
  SELECT 'qb_assignment_target', 'student_id' UNION ALL
  SELECT 'qb_assignment_target', 'class_id' UNION ALL
  SELECT 'qb_assignment_target', 'created_at' UNION ALL
  SELECT 'qb_attempt_question', 'knowledge_snapshot_json' UNION ALL
  SELECT 'qb_learning_resource', 'resource_purpose' UNION ALL
  SELECT 'qb_learning_resource', 'difficulty' UNION ALL
  SELECT 'qb_learning_resource', 'generation_type' UNION ALL
  SELECT 'qb_learning_behavior', 'ref_type' UNION ALL
  SELECT 'qb_learning_behavior', 'event_value'
)
SELECT e.table_name, e.column_name, c.column_type
FROM expected_columns e
LEFT JOIN information_schema.columns c
  ON c.table_schema=DATABASE() AND c.table_name=e.table_name AND c.column_name=e.column_name
WHERE c.column_name IS NULL
ORDER BY e.table_name, e.column_name;

SELECT index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='qb_assignment_target'
GROUP BY index_name, non_unique
ORDER BY index_name;

SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='qb_assignment_target'
ORDER BY constraint_type, constraint_name;

SELECT COUNT(*) AS invalid_assignment_target_scope_rows
FROM qb_assignment_target
WHERE NOT ((target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
        OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL));

SELECT assignment_id, target_type, student_id, class_id, COUNT(*) AS duplicate_count
FROM qb_assignment_target
GROUP BY assignment_id, target_type, student_id, class_id
HAVING COUNT(*) > 1;

SELECT 'question_knowledge' AS relation_name, COUNT(*) AS orphan_count
FROM question_knowledge qk
LEFT JOIN qb_question q ON q.id=qk.question_id AND q.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id AND kp.is_deleted=0
WHERE q.id IS NULL OR kp.id IS NULL
UNION ALL
SELECT 'resource_knowledge', COUNT(*)
FROM resource_knowledge rk
LEFT JOIN qb_learning_resource r ON r.id=rk.resource_id AND r.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=rk.knowledge_point_id AND kp.is_deleted=0
WHERE r.id IS NULL OR kp.id IS NULL
UNION ALL
SELECT 'student_knowledge_state', COUNT(*)
FROM student_knowledge_state sk
LEFT JOIN sys_user u ON u.id=sk.user_id AND u.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=sk.knowledge_point_id AND kp.is_deleted=0
WHERE u.id IS NULL OR kp.id IS NULL;

WITH cleanup_candidates AS (
  SELECT 'qb_assignment_target_class' AS table_name UNION ALL
  SELECT 'qb_question_tag' UNION ALL SELECT 'qb_tag_mastery' UNION ALL
  SELECT 'qb_tag' UNION ALL SELECT 'qb_knowledge_point' UNION ALL
  SELECT 'qb_knowledge_relation'
), legacy_columns AS (
  SELECT 'qb_attempt_question' AS table_name, 'tag_ids_json' AS column_name UNION ALL
  SELECT 'qb_learning_resource', 'knowledge_point_id' UNION ALL
  SELECT 'qb_learning_resource', 'tag_id' UNION ALL
  SELECT 'qb_learning_behavior', 'tag_id'
)
SELECT 'legacy_table' AS artifact_type, c.table_name AS artifact, COUNT(t.table_name) AS remains
FROM cleanup_candidates c
LEFT JOIN information_schema.tables t ON t.table_schema=DATABASE() AND t.table_name=c.table_name
GROUP BY c.table_name
UNION ALL
SELECT 'legacy_column', CONCAT(l.table_name, '.', l.column_name), COUNT(col.column_name)
FROM legacy_columns l
LEFT JOIN information_schema.columns col
  ON col.table_schema=DATABASE() AND col.table_name=l.table_name AND col.column_name=l.column_name
GROUP BY l.table_name, l.column_name
ORDER BY artifact_type, artifact;
