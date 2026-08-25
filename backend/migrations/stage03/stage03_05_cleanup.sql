-- Stage 03 cleanup is blocked by default.
-- Set @stage03_cleanup_confirmed = 1 only after stable release, full regression,
-- no legacy clients, and explicit human approval.
USE question_bank;

SET @stage03_cleanup_confirmed = 0;
SET @has_legacy_class_table = (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='qb_assignment_target_class'
);
SET @ddl = IF(@stage03_cleanup_confirmed = 1 AND @has_legacy_class_table = 1,
  'DROP TABLE qb_assignment_target_class',
  'SELECT ''BLOCKED: set @stage03_cleanup_confirmed = 1 only after release approval.'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
