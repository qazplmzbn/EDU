-- Stage 03 data migration. Run after stage03_02_schema.sql.
USE question_bank;

-- Legacy published assignments without either target table used to be visible
-- to every student. The final schema has no global target type, so preserve
-- existing access by snapshotting every active STUDENT account as a direct
-- target. New students created after migration are not added retroactively.
SET @legacy_unrestricted_dml = '
  INSERT INTO qb_assignment_target(assignment_id, target_type, student_id, class_id, created_at)
  SELECT DISTINCT a.id, ''student'', u.id, NULL, NOW(3)
  FROM qb_assignment a
  JOIN sys_user u ON u.is_deleted=0 AND u.status=1
  JOIN sys_user_role ur ON ur.user_id=u.id
  JOIN sys_role r ON r.id=ur.role_id AND r.role_code=''STUDENT''
  WHERE a.is_deleted=0
    AND a.publish_status=2
    AND NOT EXISTS (SELECT 1 FROM qb_assignment_target t WHERE t.assignment_id=a.id)
    AND NOT EXISTS (SELECT 1 FROM qb_assignment_target_class tc WHERE tc.assignment_id=a.id)';
PREPARE stmt FROM @legacy_unrestricted_dml; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_legacy_class_table = (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='qb_assignment_target_class'
);

SET @dml = IF(@has_legacy_class_table = 1,
  'INSERT INTO qb_assignment_target(assignment_id, target_type, student_id, class_id, created_at) '
  'SELECT tc.assignment_id, ''class'', NULL, tc.class_id, tc.created_at '
  'FROM qb_assignment_target_class tc '
  'LEFT JOIN qb_assignment_target t ON t.assignment_id=tc.assignment_id AND t.target_type=''class'' AND t.class_id=tc.class_id '
  'WHERE t.id IS NULL',
  'SELECT ''stage03: legacy class target table is absent; no rows migrated'' AS message');
PREPARE stmt FROM @dml; EXECUTE stmt; DEALLOCATE PREPARE stmt;
