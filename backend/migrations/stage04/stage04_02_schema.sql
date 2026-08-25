-- Stage 04 non-destructive schema reconciliation.
-- Run manually only after stage04_01_precheck.sql is clean.
USE question_bank;

-- Add only the final-stage lookup indexes if an older migration omitted them.
SET @has_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='qb_assignment_target'
    AND index_name='idx_assignment_target_student'
);
SET @sql = IF(@has_index=0,
  'ALTER TABLE qb_assignment_target ADD KEY idx_assignment_target_student (student_id, assignment_id)',
  'SELECT ''OK: idx_assignment_target_student already exists.'' AS message');
PREPARE stage04_schema FROM @sql; EXECUTE stage04_schema; DEALLOCATE PREPARE stage04_schema;

SET @has_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='qb_assignment_target'
    AND index_name='idx_assignment_target_class'
);
SET @sql = IF(@has_index=0,
  'ALTER TABLE qb_assignment_target ADD KEY idx_assignment_target_class (class_id, assignment_id)',
  'SELECT ''OK: idx_assignment_target_class already exists.'' AS message');
PREPARE stage04_schema FROM @sql; EXECUTE stage04_schema; DEALLOCATE PREPARE stage04_schema;

SET @has_constraint = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE() AND table_name='qb_assignment_target'
    AND constraint_name='ck_qb_assignment_target_scope' AND constraint_type='CHECK'
);
SET @sql = IF(@has_constraint=0,
  'ALTER TABLE qb_assignment_target ADD CONSTRAINT ck_qb_assignment_target_scope CHECK ((target_type=''student'' AND student_id IS NOT NULL AND class_id IS NULL) OR (target_type=''class'' AND class_id IS NOT NULL AND student_id IS NULL))',
  'SELECT ''OK: ck_qb_assignment_target_scope already exists.'' AS message');
PREPARE stage04_schema FROM @sql; EXECUTE stage04_schema; DEALLOCATE PREPARE stage04_schema;
