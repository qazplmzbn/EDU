-- Stage 03 precheck: read only. Run this first and retain its output.
USE question_bank;

SELECT VERSION() AS mysql_version, DATABASE() AS current_database;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('qb_assignment_target', 'qb_assignment_target_class', 'qb_attempt', 'qb_class_member')
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable, column_key
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('qb_assignment_target', 'qb_assignment_target_class')
ORDER BY table_name, ordinal_position;

SELECT index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('qb_assignment_target', 'qb_assignment_target_class')
ORDER BY table_name, index_name, seq_in_index;

SELECT
  (SELECT COUNT(*) FROM qb_assignment_target) AS legacy_student_target_rows,
  (SELECT COUNT(*) FROM qb_assignment_target_class) AS legacy_class_target_rows,
  (SELECT COUNT(DISTINCT assignment_id) FROM qb_assignment_target) AS assignments_with_student_targets,
  (SELECT COUNT(DISTINCT assignment_id) FROM qb_assignment_target_class) AS assignments_with_class_targets;

SELECT t.assignment_id, t.user_id AS student_id, tc.class_id
FROM qb_assignment_target t
JOIN qb_assignment_target_class tc ON tc.assignment_id = t.assignment_id
JOIN qb_class_member cm ON cm.class_id = tc.class_id AND cm.student_id = t.user_id
ORDER BY t.assignment_id, t.user_id, tc.class_id
LIMIT 200;

SELECT assignment_id, COUNT(*) AS attempt_count
FROM qb_attempt
WHERE assignment_id IS NOT NULL
GROUP BY assignment_id
ORDER BY attempt_count DESC, assignment_id ASC
LIMIT 200;

-- Legacy semantics treated published assignments with no target rows as unrestricted.
-- Stage 03 snapshots their current active students into direct targets during migration.
SELECT a.id AS legacy_unrestricted_assignment_id, a.assignment_title, a.created_by, a.end_time
FROM qb_assignment a
WHERE a.is_deleted=0
  AND a.publish_status=2
  AND NOT EXISTS (SELECT 1 FROM qb_assignment_target t WHERE t.assignment_id=a.id)
  AND NOT EXISTS (SELECT 1 FROM qb_assignment_target_class tc WHERE tc.assignment_id=a.id)
ORDER BY a.id ASC;

SELECT COUNT(*) AS legacy_unrestricted_published_assignment_count
FROM qb_assignment a
WHERE a.is_deleted=0
  AND a.publish_status=2
  AND NOT EXISTS (SELECT 1 FROM qb_assignment_target t WHERE t.assignment_id=a.id)
  AND NOT EXISTS (SELECT 1 FROM qb_assignment_target_class tc WHERE tc.assignment_id=a.id);
