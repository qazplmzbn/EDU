-- Stage 03 verification: read only. Run after stage03_03_migrate.sql.
USE question_bank;

SELECT target_type, COUNT(*) AS row_count
FROM qb_assignment_target
GROUP BY target_type
ORDER BY target_type;

SELECT COUNT(*) AS invalid_scope_rows
FROM qb_assignment_target
WHERE NOT (
  (target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
  OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL)
);

SELECT assignment_id, target_type, student_id, class_id, COUNT(*) AS duplicate_count
FROM qb_assignment_target
GROUP BY assignment_id, target_type, student_id, class_id
HAVING COUNT(*) > 1;

SELECT
  (SELECT COUNT(*) FROM qb_assignment_target WHERE target_type='student') AS migrated_student_targets,
  (SELECT COUNT(*) FROM qb_assignment_target WHERE target_type='class') AS migrated_class_targets,
  (SELECT COUNT(*) FROM qb_assignment_target_class) AS legacy_class_target_rows;

SELECT tc.assignment_id, tc.class_id
FROM qb_assignment_target_class tc
LEFT JOIN qb_assignment_target t
  ON t.assignment_id=tc.assignment_id AND t.target_type='class' AND t.class_id=tc.class_id
WHERE t.id IS NULL;

SELECT a.id AS published_assignment_without_targets, a.assignment_title
FROM qb_assignment a
WHERE a.is_deleted=0
  AND a.publish_status=2
  AND NOT EXISTS (SELECT 1 FROM qb_assignment_target t WHERE t.assignment_id=a.id)
ORDER BY a.id ASC;

SELECT index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND table_name='qb_assignment_target'
ORDER BY index_name, seq_in_index;

SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE()
  AND table_name='qb_assignment_target'
ORDER BY constraint_type, constraint_name;
