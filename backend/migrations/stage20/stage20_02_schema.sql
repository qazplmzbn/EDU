-- Stage 20 schema. Safe to re-run on MySQL 8.
DROP PROCEDURE IF EXISTS stage20_add_column;
DELIMITER $$
CREATE PROCEDURE stage20_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN d TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=t AND column_name=c
  ) THEN
    SET @sql_text=d; PREPARE stmt FROM @sql_text; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL stage20_add_column('occupation_skill','required_level_source',
  'ALTER TABLE occupation_skill ADD COLUMN required_level_source VARCHAR(32) NULL AFTER required_level');
CALL stage20_add_column('occupation_skill','required_level_version',
  'ALTER TABLE occupation_skill ADD COLUMN required_level_version VARCHAR(64) NULL AFTER required_level_source');
CALL stage20_add_column('occupation_skill','published_batch_code',
  'ALTER TABLE occupation_skill ADD COLUMN published_batch_code VARCHAR(64) NULL AFTER required_level_version');
CALL stage20_add_column('occupation_skill','required_level_updated_at',
  'ALTER TABLE occupation_skill ADD COLUMN required_level_updated_at DATETIME(3) NULL AFTER published_batch_code');
CALL stage20_add_column('student_skill_state','core_proficiency_value',
  'ALTER TABLE student_skill_state ADD COLUMN core_proficiency_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000 AFTER proficiency_value');
CALL stage20_add_column('student_skill_state','knowledge_coverage_rate',
  'ALTER TABLE student_skill_state ADD COLUMN knowledge_coverage_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000 AFTER confidence');
CALL stage20_add_column('student_skill_state','calculation_version',
  'ALTER TABLE student_skill_state ADD COLUMN calculation_version VARCHAR(64) NULL AFTER evidence_count');
CALL stage20_add_column('student_skill_state','calculated_at',
  'ALTER TABLE student_skill_state ADD COLUMN calculated_at DATETIME(3) NULL AFTER calculation_version');
DROP PROCEDURE stage20_add_column;

CREATE TABLE IF NOT EXISTS student_occupation_skill_gap (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  snapshot_code VARCHAR(64) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  occupation_id BIGINT UNSIGNED NOT NULL,
  occupation_skill_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  required_level DECIMAL(6,4) NOT NULL,
  current_level DECIMAL(6,4) NOT NULL,
  current_confidence DECIMAL(6,4) NOT NULL,
  gap_value DECIMAL(6,4) NOT NULL,
  priority_score DECIMAL(10,6) NOT NULL,
  gap_status VARCHAR(32) NOT NULL,
  target_batch_code VARCHAR(64) NOT NULL,
  calculation_version VARCHAR(64) NOT NULL,
  calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  correlation_id VARCHAR(64) NULL,
  PRIMARY KEY (id),
  KEY idx_gap_snapshot(snapshot_code),
  KEY idx_gap_user_occupation(user_id,occupation_id,calculated_at),
  KEY idx_gap_priority(user_id,occupation_id,priority_score),
  CONSTRAINT fk_gap_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_gap_occupation FOREIGN KEY(occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT,
  CONSTRAINT fk_gap_occupation_skill FOREIGN KEY(occupation_skill_id) REFERENCES occupation_skill(id) ON DELETE RESTRICT,
  CONSTRAINT fk_gap_skill FOREIGN KEY(skill_id) REFERENCES skill(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_course_recommendation_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  snapshot_code VARCHAR(64) NOT NULL,
  gap_snapshot_code VARCHAR(64) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  occupation_id BIGINT UNSIGNED NOT NULL,
  target_batch_code VARCHAR(64) NOT NULL,
  profile_version VARCHAR(64) NULL,
  graph_versions_json JSON NULL,
  skill_state_version VARCHAR(64) NULL,
  course_catalog_hash VARCHAR(128) NULL,
  algorithm_version VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'READY',
  request_json JSON NULL,
  result_summary_json JSON NULL,
  correlation_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_career_recommendation_snapshot(snapshot_code),
  KEY idx_recommendation_user_occupation(user_id,occupation_id,created_at),
  CONSTRAINT fk_career_recommendation_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_recommendation_occupation FOREIGN KEY(occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_course_recommendation_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  snapshot_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  rank_no INT NOT NULL,
  course_score DECIMAL(10,6) NOT NULL,
  coverage_score DECIMAL(10,6) NOT NULL,
  core_coverage_rate DECIMAL(10,6) NOT NULL,
  difficulty_fit DECIMAL(10,6) NOT NULL DEFAULT 1.000000,
  unfinished_factor DECIMAL(10,6) NOT NULL DEFAULT 1.000000,
  course_quality DECIMAL(10,6) NOT NULL DEFAULT 1.000000,
  estimated_hours DECIMAL(10,2) NULL,
  reason_json JSON NOT NULL,
  covered_skill_ids_json JSON NOT NULL,
  covered_knowledge_point_ids_json JSON NOT NULL,
  fallback_type VARCHAR(32) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'RECOMMENDED',
  PRIMARY KEY (id), UNIQUE KEY uk_career_recommendation_course(snapshot_id,course_id),
  KEY idx_recommendation_item_snapshot(snapshot_id,rank_no),
  CONSTRAINT fk_career_recommendation_item_snapshot FOREIGN KEY(snapshot_id) REFERENCES career_course_recommendation_snapshot(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_recommendation_item_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
