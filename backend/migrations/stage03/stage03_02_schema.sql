-- Stage 03 schema. Uses information_schema checks so every DDL step is repeatable.
USE question_bank;

SET @schema_name = DATABASE();

-- Add the surrogate primary key while the legacy composite primary key is still present.
SET @has_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND column_name='id'
);
SET @ddl = IF(@has_id = 0,
  'ALTER TABLE qb_assignment_target ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT FIRST, DROP PRIMARY KEY, ADD PRIMARY KEY (id)',
  'SELECT ''stage03: target id already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Rename the legacy direct-student column and make it nullable for class rows.
SET @has_user_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND column_name='user_id'
);
SET @has_student_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND column_name='student_id'
);
SET @ddl = IF(@has_user_id = 1 AND @has_student_id = 0,
  'ALTER TABLE qb_assignment_target CHANGE COLUMN user_id student_id BIGINT UNSIGNED NULL',
  'SELECT ''stage03: student_id already normalized'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_class_id = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND column_name='class_id'
);
SET @ddl = IF(@has_class_id = 0,
  'ALTER TABLE qb_assignment_target ADD COLUMN class_id BIGINT UNSIGNED NULL AFTER student_id',
  'SELECT ''stage03: class_id already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_target_type = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND column_name='target_type'
);
SET @ddl = IF(@has_target_type = 0,
  'ALTER TABLE qb_assignment_target ADD COLUMN target_type VARCHAR(16) NOT NULL DEFAULT ''student'' AFTER assignment_id',
  'SELECT ''stage03: target_type already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE qb_assignment_target
SET target_type='student'
WHERE target_type IS NULL OR target_type='';

-- The final table has no implicit target type; application writes an explicit value.
SET @ddl = 'ALTER TABLE qb_assignment_target MODIFY COLUMN target_type VARCHAR(16) NOT NULL';
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_uk_student = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND index_name='uk_assignment_target_student'
);
SET @ddl = IF(@has_uk_student = 0,
  'ALTER TABLE qb_assignment_target ADD UNIQUE KEY uk_assignment_target_student (assignment_id, target_type, student_id)',
  'SELECT ''stage03: student unique key already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_uk_class = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND index_name='uk_assignment_target_class'
);
SET @ddl = IF(@has_uk_class = 0,
  'ALTER TABLE qb_assignment_target ADD UNIQUE KEY uk_assignment_target_class (assignment_id, target_type, class_id)',
  'SELECT ''stage03: class unique key already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_student = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND index_name='idx_assignment_target_student'
);
SET @ddl = IF(@has_idx_student = 0,
  'ALTER TABLE qb_assignment_target ADD KEY idx_assignment_target_student (student_id, assignment_id)',
  'SELECT ''stage03: student lookup index already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_class = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=@schema_name AND table_name='qb_assignment_target' AND index_name='idx_assignment_target_class'
);
SET @ddl = IF(@has_idx_class = 0,
  'ALTER TABLE qb_assignment_target ADD KEY idx_assignment_target_class (class_id, assignment_id)',
  'SELECT ''stage03: class lookup index already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_check = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema=@schema_name AND table_name='qb_assignment_target'
    AND constraint_name='ck_qb_assignment_target_scope' AND constraint_type='CHECK'
);
SET @ddl = IF(@has_check = 0,
  'ALTER TABLE qb_assignment_target ADD CONSTRAINT ck_qb_assignment_target_scope CHECK ((target_type=''student'' AND student_id IS NOT NULL AND class_id IS NULL) OR (target_type=''class'' AND class_id IS NOT NULL AND student_id IS NULL))',
  'SELECT ''stage03: scope check already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
