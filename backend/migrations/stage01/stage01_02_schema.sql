-- Stage 01 schema. Review and execute manually after stage01_01_precheck.sql.
USE question_bank;

CREATE TABLE IF NOT EXISTS occupation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name_zh VARCHAR(255) NOT NULL,
  name_en VARCHAR(255) NULL,
  category_code VARCHAR(64) NULL,
  description TEXT NULL,
  source_name VARCHAR(64) NULL,
  source_ref VARCHAR(500) NULL,
  version VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_occupation_source (source_name, source_ref),
  KEY idx_occupation_list (is_deleted, updated_at, id),
  KEY idx_occupation_name (name_zh, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skill (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name_zh VARCHAR(255) NOT NULL,
  skill_type VARCHAR(32) NOT NULL DEFAULT 'technical',
  description TEXT NULL,
  source_name VARCHAR(64) NULL,
  source_ref VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_skill_source (source_name, source_ref),
  KEY idx_skill_list (is_deleted, updated_at, id),
  KEY idx_skill_name (name_zh, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_point (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  code VARCHAR(100) NULL,
  parent_id BIGINT UNSIGNED NULL,
  level INT NOT NULL DEFAULT 1,
  knowledge_type VARCHAR(32) NOT NULL DEFAULT 'concept',
  difficulty TINYINT NOT NULL DEFAULT 3,
  description VARCHAR(2000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_point_code (code),
  KEY idx_knowledge_point_parent (parent_id, is_deleted, id),
  KEY idx_knowledge_point_list (is_deleted, updated_at, id),
  CONSTRAINT ck_knowledge_point_level CHECK (level >= 1),
  CONSTRAINT ck_knowledge_point_difficulty CHECK (difficulty BETWEEN 1 AND 5),
  CONSTRAINT fk_knowledge_point_parent FOREIGN KEY (parent_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS occupation_alias (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  occupation_id BIGINT UNSIGNED NOT NULL,
  alias_name VARCHAR(255) NOT NULL,
  alias_type VARCHAR(24) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_occupation_alias (occupation_id, alias_name, alias_type),
  KEY idx_occupation_alias_occupation (occupation_id),
  CONSTRAINT fk_occupation_alias_occupation FOREIGN KEY (occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS occupation_skill (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  occupation_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  requirement_type VARCHAR(24) NOT NULL DEFAULT 'essential',
  importance_score DECIMAL(6,4) NULL,
  required_level DECIMAL(6,4) NULL,
  source_ref VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_occupation_skill (occupation_id, skill_id, requirement_type),
  KEY idx_occupation_skill_skill (skill_id, occupation_id),
  CONSTRAINT ck_occupation_skill_importance CHECK (importance_score IS NULL OR importance_score BETWEEN 0 AND 1),
  CONSTRAINT ck_occupation_skill_level CHECK (required_level IS NULL OR required_level BETWEEN 0 AND 1),
  CONSTRAINT fk_occupation_skill_occupation FOREIGN KEY (occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_occupation_skill_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skill_knowledge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  requirement_type VARCHAR(24) NOT NULL DEFAULT 'core',
  weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
  source_ref VARCHAR(500) NULL,
  evidence_text VARCHAR(2000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_skill_knowledge (skill_id, knowledge_point_id, requirement_type),
  KEY idx_skill_knowledge_knowledge (knowledge_point_id, skill_id),
  CONSTRAINT ck_skill_knowledge_weight CHECK (weight BETWEEN 0 AND 1),
  CONSTRAINT ck_skill_knowledge_confidence CHECK (confidence BETWEEN 0 AND 1),
  CONSTRAINT fk_skill_knowledge_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_skill_knowledge_point FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_relation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(40) NOT NULL DEFAULT 'prerequisite',
  weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  source_type VARCHAR(40) NOT NULL DEFAULT 'manual',
  description VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_relation (source_id, target_id, relation_type),
  KEY idx_knowledge_relation_source (source_id, relation_type, is_deleted),
  KEY idx_knowledge_relation_target (target_id, relation_type, is_deleted),
  CONSTRAINT ck_knowledge_relation_distinct CHECK (source_id <> target_id),
  CONSTRAINT ck_knowledge_relation_weight CHECK (weight BETWEEN 0 AND 1),
  CONSTRAINT ck_knowledge_relation_confidence CHECK (confidence BETWEEN 0 AND 1),
  CONSTRAINT fk_knowledge_relation_source FOREIGN KEY (source_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_knowledge_relation_target FOREIGN KEY (target_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_sync_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sync_type VARCHAR(40) NOT NULL,
  source_name VARCHAR(64) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL DEFAULT 'manual',
  trigger_by BIGINT UNSIGNED NULL,
  sync_version VARCHAR(64) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'running',
  fetched_count INT NOT NULL DEFAULT 0,
  inserted_count INT NOT NULL DEFAULT 0,
  updated_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(2000) NULL,
  started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_data_sync_record_list (started_at, status, id),
  KEY idx_data_sync_record_source (source_name, sync_type, started_at),
  CONSTRAINT ck_data_sync_record_counts CHECK (fetched_count >= 0 AND inserted_count >= 0 AND updated_count >= 0 AND failed_count >= 0),
  CONSTRAINT fk_data_sync_record_trigger_by FOREIGN KEY (trigger_by) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
