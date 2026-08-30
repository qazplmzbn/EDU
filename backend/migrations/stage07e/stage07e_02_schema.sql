USE question_bank;

CREATE TABLE IF NOT EXISTS qb_learning_resource_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resource_id BIGINT UNSIGNED NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  student_id BIGINT UNSIGNED NULL,
  class_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_target_student (resource_id, target_type, student_id),
  UNIQUE KEY uk_resource_target_class (resource_id, target_type, class_id),
  KEY idx_resource_target_student (student_id, resource_id),
  KEY idx_resource_target_class (class_id, resource_id),
  CONSTRAINT ck_resource_target_scope CHECK (
    (target_type = 'student' AND student_id IS NOT NULL AND class_id IS NULL)
    OR (target_type = 'class' AND class_id IS NOT NULL AND student_id IS NULL)
  ),
  CONSTRAINT fk_resource_target_resource FOREIGN KEY (resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_resource_target_student FOREIGN KEY (student_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_resource_target_class FOREIGN KEY (class_id) REFERENCES qb_class(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_resource_target_creator FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Upgrade the legacy table shape before normalizing legacy rows in stage07e_03.
ALTER TABLE qb_learning_resource_target
  MODIFY id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  MODIFY resource_id BIGINT UNSIGNED NOT NULL,
  MODIFY student_id BIGINT UNSIGNED NULL,
  MODIFY class_id BIGINT UNSIGNED NULL,
  MODIFY created_by BIGINT UNSIGNED NULL,
  MODIFY target_type VARCHAR(16) NOT NULL;

DROP PROCEDURE IF EXISTS stage07e_prepare_resource_target;
DELIMITER //
CREATE PROCEDURE stage07e_prepare_resource_target()
BEGIN
  DECLARE student_unique_columns INT DEFAULT 0;
  DECLARE student_index_columns VARCHAR(255) DEFAULT '';
  DECLARE class_index_columns VARCHAR(255) DEFAULT '';

  SELECT COUNT(*) INTO student_unique_columns
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'qb_learning_resource_target'
    AND index_name = 'uk_resource_target_student';
  IF student_unique_columns = 2 THEN
    ALTER TABLE qb_learning_resource_target DROP INDEX uk_resource_target_student;
  END IF;

  SELECT COALESCE(GROUP_CONCAT(column_name ORDER BY seq_in_index), '') INTO student_index_columns
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'qb_learning_resource_target'
    AND index_name = 'idx_resource_target_student';
  IF student_index_columns <> 'student_id,resource_id' THEN
    ALTER TABLE qb_learning_resource_target DROP INDEX idx_resource_target_student;
    ALTER TABLE qb_learning_resource_target ADD KEY idx_resource_target_student (student_id, resource_id);
  END IF;

  SELECT COALESCE(GROUP_CONCAT(column_name ORDER BY seq_in_index), '') INTO class_index_columns
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'qb_learning_resource_target'
    AND index_name = 'idx_resource_target_class';
  IF class_index_columns <> 'class_id,resource_id' THEN
    ALTER TABLE qb_learning_resource_target DROP INDEX idx_resource_target_class;
    ALTER TABLE qb_learning_resource_target ADD KEY idx_resource_target_class (class_id, resource_id);
  END IF;
END//
DELIMITER ;
CALL stage07e_prepare_resource_target();
DROP PROCEDURE stage07e_prepare_resource_target;
