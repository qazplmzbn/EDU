-- Stage 02 additive schema. Review and execute manually after precheck is clean.
USE question_bank;

CREATE TABLE IF NOT EXISTS question_knowledge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  relation_type VARCHAR(24) NOT NULL DEFAULT 'assess',
  is_primary TINYINT NOT NULL DEFAULT 0,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  source_type VARCHAR(24) NOT NULL DEFAULT 'manual',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_knowledge (question_id, knowledge_point_id, relation_type),
  KEY idx_question_knowledge_point (knowledge_point_id, question_id),
  CONSTRAINT ck_question_knowledge_weight CHECK (weight BETWEEN 0 AND 1),
  CONSTRAINT ck_question_knowledge_confidence CHECK (confidence BETWEEN 0 AND 1),
  CONSTRAINT fk_question_knowledge_question FOREIGN KEY (question_id) REFERENCES qb_question(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_question_knowledge_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resource_knowledge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resource_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(24) NOT NULL DEFAULT 'cover',
  coverage_weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  is_primary TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_knowledge (resource_id, knowledge_point_id, relation_type),
  KEY idx_resource_knowledge_point (knowledge_point_id, resource_id),
  CONSTRAINT ck_resource_knowledge_weight CHECK (coverage_weight BETWEEN 0 AND 1),
  CONSTRAINT fk_resource_knowledge_resource FOREIGN KEY (resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_resource_knowledge_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_knowledge_state (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  mastery_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  mastery_level VARCHAR(24) NULL,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  evidence_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  attempt_count INT NOT NULL DEFAULT 0,
  last_evidence_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_knowledge_state (user_id, knowledge_point_id),
  KEY idx_student_knowledge_state_point (knowledge_point_id, user_id),
  CONSTRAINT ck_student_knowledge_mastery CHECK (mastery_value BETWEEN 0 AND 1),
  CONSTRAINT ck_student_knowledge_confidence CHECK (confidence BETWEEN 0 AND 1),
  CONSTRAINT fk_student_knowledge_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_student_knowledge_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- The local MySQL build does not support ADD COLUMN IF NOT EXISTS.
-- Each conditional dynamic statement is idempotent and may be rerun safely.
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_attempt_question' AND column_name='knowledge_snapshot_json');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_attempt_question ADD COLUMN knowledge_snapshot_json LONGTEXT NULL AFTER difficulty', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='resource_purpose');
SET @sql = IF(@exists=0, "ALTER TABLE qb_learning_resource ADD COLUMN resource_purpose VARCHAR(32) NOT NULL DEFAULT 'learn' AFTER resource_type", 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='difficulty');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_learning_resource ADD COLUMN difficulty TINYINT NULL AFTER content', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='generation_type');
SET @sql = IF(@exists=0, "ALTER TABLE qb_learning_resource ADD COLUMN generation_type VARCHAR(32) NOT NULL DEFAULT 'manual' AFTER difficulty", 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='version');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_learning_resource ADD COLUMN version VARCHAR(64) NULL AFTER generation_type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='agent_task_id');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_learning_resource ADD COLUMN agent_task_id BIGINT UNSIGNED NULL AFTER audit_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_behavior' AND column_name='ref_type');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_learning_behavior ADD COLUMN ref_type VARCHAR(32) NULL AFTER behavior_type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_behavior' AND column_name='event_value');
SET @sql = IF(@exists=0, 'ALTER TABLE qb_learning_behavior ADD COLUMN event_value VARCHAR(255) NULL AFTER duration_seconds', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
