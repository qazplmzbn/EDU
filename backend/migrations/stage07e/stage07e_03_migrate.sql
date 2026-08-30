USE question_bank;

-- Legacy class delivery may have expanded a class into one row per student.
-- Preserve one class target and let reads resolve current class membership.
DELETE duplicate_target
FROM qb_learning_resource_target duplicate_target
JOIN qb_learning_resource_target kept_target
  ON kept_target.resource_id = duplicate_target.resource_id
 AND kept_target.target_type = 'class'
 AND kept_target.class_id = duplicate_target.class_id
 AND kept_target.id < duplicate_target.id
WHERE duplicate_target.target_type = 'class';

UPDATE qb_learning_resource_target
SET student_id = NULL
WHERE target_type = 'class'
  AND student_id IS NOT NULL;

UPDATE qb_learning_resource_target
SET class_id = NULL
WHERE target_type = 'student'
  AND class_id IS NOT NULL;

DROP PROCEDURE IF EXISTS stage07e_finalize_resource_target;
DELIMITER //
CREATE PROCEDURE stage07e_finalize_resource_target()
BEGIN
  DECLARE student_unique_exists INT DEFAULT 0;
  DECLARE class_unique_exists INT DEFAULT 0;
  DECLARE scope_check_exists INT DEFAULT 0;
  DECLARE resource_fk_exists INT DEFAULT 0;
  DECLARE student_fk_exists INT DEFAULT 0;
  DECLARE class_fk_exists INT DEFAULT 0;
  DECLARE creator_fk_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO student_unique_exists FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND index_name = 'uk_resource_target_student';
  IF student_unique_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD UNIQUE KEY uk_resource_target_student (resource_id, target_type, student_id);
  END IF;

  SELECT COUNT(*) INTO class_unique_exists FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND index_name = 'uk_resource_target_class';
  IF class_unique_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD UNIQUE KEY uk_resource_target_class (resource_id, target_type, class_id);
  END IF;

  SELECT COUNT(*) INTO scope_check_exists FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND constraint_name = 'ck_resource_target_scope';
  IF scope_check_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD CONSTRAINT ck_resource_target_scope CHECK (
      (target_type = 'student' AND student_id IS NOT NULL AND class_id IS NULL)
      OR (target_type = 'class' AND class_id IS NOT NULL AND student_id IS NULL)
    );
  END IF;

  SELECT COUNT(*) INTO resource_fk_exists FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND constraint_name = 'fk_resource_target_resource';
  IF resource_fk_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD CONSTRAINT fk_resource_target_resource FOREIGN KEY (resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
  END IF;

  SELECT COUNT(*) INTO student_fk_exists FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND constraint_name = 'fk_resource_target_student';
  IF student_fk_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD CONSTRAINT fk_resource_target_student FOREIGN KEY (student_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
  END IF;

  SELECT COUNT(*) INTO class_fk_exists FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND constraint_name = 'fk_resource_target_class';
  IF class_fk_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD CONSTRAINT fk_resource_target_class FOREIGN KEY (class_id) REFERENCES qb_class(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
  END IF;

  SELECT COUNT(*) INTO creator_fk_exists FROM information_schema.table_constraints
  WHERE table_schema = DATABASE() AND table_name = 'qb_learning_resource_target' AND constraint_name = 'fk_resource_target_creator';
  IF creator_fk_exists = 0 THEN
    ALTER TABLE qb_learning_resource_target ADD CONSTRAINT fk_resource_target_creator FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
  END IF;
END//
DELIMITER ;
CALL stage07e_finalize_resource_target();
DROP PROCEDURE stage07e_finalize_resource_target;
