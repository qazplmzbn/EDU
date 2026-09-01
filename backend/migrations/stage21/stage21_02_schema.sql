CREATE TABLE IF NOT EXISTS career_mapping_import_batch (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_code VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'SKILL_KNOWLEDGE_CSV',
  file_name VARCHAR(255) NOT NULL,
  file_hash VARCHAR(128) NOT NULL,
  schema_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  status VARCHAR(32) NOT NULL DEFAULT 'IMPORTED',
  row_count INT NOT NULL DEFAULT 0,
  candidate_count INT NOT NULL DEFAULT 0,
  unresolved_count INT NOT NULL DEFAULT 0,
  out_of_catalog_count INT NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  error_message VARCHAR(1000) NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_career_mapping_batch_code(batch_code), KEY idx_career_mapping_batch_hash(file_hash),
  CONSTRAINT fk_career_mapping_batch_operator FOREIGN KEY(created_by) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_mapping_import_row (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_id BIGINT UNSIGNED NOT NULL,
  row_no INT NOT NULL,
  occupation_label_en VARCHAR(255) NULL, occupation_label_zh VARCHAR(255) NULL,
  skill_relation VARCHAR(32) NULL, skill_title_en VARCHAR(255) NULL, skill_title_zh VARCHAR(255) NULL,
  course_name VARCHAR(255) NULL, knowledge_module VARCHAR(255) NULL, knowledge_point VARCHAR(1000) NULL,
  onet_knowledge VARCHAR(255) NULL, onet_knowledge_importance VARCHAR(64) NULL,
  mapping_type VARCHAR(32) NULL, confidence DECIMAL(6,4) NULL, evidence TEXT NULL,
  occupation_id BIGINT UNSIGNED NULL, skill_id BIGINT UNSIGNED NULL, course_id BIGINT UNSIGNED NULL,
  module_external_id VARCHAR(128) NULL, knowledge_point_id BIGINT UNSIGNED NULL,
  normalized_mapping_type VARCHAR(32) NULL, match_status VARCHAR(32) NOT NULL,
  match_reason VARCHAR(1000) NULL, reviewer_id BIGINT UNSIGNED NULL, reviewed_at DATETIME(3) NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_career_mapping_row(batch_id,row_no), KEY idx_career_mapping_row_status(batch_id,match_status),
  CONSTRAINT fk_career_mapping_row_batch FOREIGN KEY(batch_id) REFERENCES career_mapping_import_batch(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_mapping_row_reviewer FOREIGN KEY(reviewer_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_mapping_review_decision (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  row_id BIGINT UNSIGNED NOT NULL, decision VARCHAR(32) NOT NULL, before_json JSON NULL, after_json JSON NULL,
  reason VARCHAR(1000) NULL, operator_id BIGINT UNSIGNED NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), KEY idx_career_mapping_decision_row(row_id,created_at),
  CONSTRAINT fk_career_mapping_decision_row FOREIGN KEY(row_id) REFERENCES career_mapping_import_row(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_mapping_decision_operator FOREIGN KEY(operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
