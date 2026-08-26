/*
Full database initialization for the merged Smart Learning Question Bank system.
Target: MySQL 8.x
Use this script after selecting the question_bank database.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NULL,
  email VARCHAR(128) NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled',
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, role_id),
  KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  username VARCHAR(64) NULL,
  success_flag TINYINT NOT NULL DEFAULT 0,
  fail_reason VARCHAR(255) NULL,
  ip_addr VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  login_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_sys_login_log_user (user_id),
  KEY idx_sys_login_log_time (login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  action VARCHAR(100) NOT NULL,
  entity_type VARCHAR(100) NULL,
  entity_id BIGINT UNSIGNED NULL,
  before_json LONGTEXT NULL,
  after_json LONGTEXT NULL,
  ip_addr VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_sys_audit_user_time (user_id, created_at),
  KEY idx_sys_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS qb_llm_call (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  biz_type TINYINT NOT NULL COMMENT '1=QUESTION_ANALYSIS,2=SUBJECTIVE_GRADING,3=OTHER',
  biz_id BIGINT UNSIGNED NULL,
  model_name VARCHAR(128) NULL,
  prompt_text LONGTEXT NULL,
  response_text LONGTEXT NULL,
  response_json LONGTEXT NULL,
  call_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending,1=success,2=failed',
  latency_ms INT NULL,
  tokens_prompt INT NULL,
  tokens_completion INT NULL,
  cost_amount DECIMAL(12,6) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_qb_llm_biz (biz_type, biz_id),
  KEY idx_qb_llm_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_llm_provider (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_key VARCHAR(100) NOT NULL,
  label VARCHAR(128) NOT NULL,
  provider_type VARCHAR(32) NOT NULL DEFAULT 'API',
  base_url VARCHAR(500) NOT NULL,
  api_key_cipher TEXT NULL,
  model VARCHAR(128) NOT NULL,
  temperature DOUBLE NULL,
  supports_temperature TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(1000) NULL,
  tags_json TEXT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  is_default TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_llm_provider_key (provider_key, is_deleted),
  KEY idx_llm_provider_default (is_default, enabled, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_prompt_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  description VARCHAR(1000) NULL,
  prompt_text LONGTEXT NOT NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_prompt_template_task (task_type, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_user_llm_provider (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  provider_key VARCHAR(100) NOT NULL,
  label VARCHAR(128) NOT NULL,
  provider_type VARCHAR(32) NOT NULL DEFAULT 'API',
  base_url VARCHAR(500) NOT NULL,
  api_key_cipher TEXT NULL,
  model VARCHAR(128) NOT NULL,
  temperature DOUBLE NULL,
  supports_temperature TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(1000) NULL,
  tags_json TEXT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  is_default TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_llm_provider_user (user_id, is_deleted),
  KEY idx_user_llm_provider_key (user_id, provider_key, is_deleted),
  KEY idx_user_llm_provider_default (user_id, is_default, enabled, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_user_prompt_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  description VARCHAR(1000) NULL,
  prompt_text LONGTEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_prompt_template_user (user_id, is_deleted),
  KEY idx_user_prompt_template_task (user_id, task_type, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  question_type TINYINT NOT NULL COMMENT '1=single,2=multiple,3=true_false,4=blank,5=short,6=code,7=code_reading',
  difficulty TINYINT NOT NULL DEFAULT 3,
  chapter VARCHAR(128) NULL,
  stem LONGTEXT NOT NULL,
  standard_answer LONGTEXT NULL,
  answer_format TINYINT NOT NULL DEFAULT 1 COMMENT '1=text,2=json',
  analysis_text LONGTEXT NULL,
  analysis_source TINYINT NOT NULL DEFAULT 1 COMMENT '1=manual,2=llm_draft,3=llm_final',
  analysis_llm_call_id BIGINT UNSIGNED NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=draft,2=published,3=archived',
  bank_review_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=private,1=pending,2=approved,3=rejected',
  bank_reviewer_id BIGINT UNSIGNED NULL,
  bank_reviewed_at DATETIME(3) NULL,
  bank_review_comment VARCHAR(255) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_qb_question_status (status, is_deleted),
  KEY idx_qb_question_review (bank_review_status, status),
  KEY idx_qb_question_creator (created_by),
  KEY idx_qb_question_type (question_type),
  KEY idx_qb_question_chapter (chapter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_question_option (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  option_label VARCHAR(20) NOT NULL,
  option_content LONGTEXT NOT NULL,
  is_correct TINYINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_qb_option_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS qb_paper (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  paper_title VARCHAR(255) NOT NULL,
  paper_desc TEXT NULL,
  paper_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=assignment,2=paper',
  total_score INT NOT NULL DEFAULT 0,
  creator_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_qb_paper_creator (creator_id),
  KEY idx_qb_paper_type (paper_type, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_paper_question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  paper_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  order_no INT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  snapshot_json LONGTEXT NULL,
  snapshot_hash VARCHAR(128) NULL,
  PRIMARY KEY (id),
  KEY idx_qb_paper_question_paper (paper_id, order_no),
  KEY idx_qb_paper_question_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_assignment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  paper_id BIGINT UNSIGNED NOT NULL,
  assignment_title VARCHAR(255) NOT NULL,
  assignment_desc TEXT NULL,
  start_time DATETIME(3) NULL,
  end_time DATETIME(3) NULL,
  time_limit_min INT NULL,
  max_attempts INT NOT NULL DEFAULT 1,
  shuffle_questions TINYINT NOT NULL DEFAULT 0,
  shuffle_options TINYINT NOT NULL DEFAULT 0,
  publish_status TINYINT NOT NULL DEFAULT 1 COMMENT '1=draft,2=published,3=closed',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_qb_assignment_paper (paper_id),
  KEY idx_qb_assignment_status (publish_status, is_deleted),
  KEY idx_qb_assignment_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_assignment_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assignment_id BIGINT UNSIGNED NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  student_id BIGINT UNSIGNED NULL,
  class_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_assignment_target_student (assignment_id, target_type, student_id),
  UNIQUE KEY uk_assignment_target_class (assignment_id, target_type, class_id),
  KEY idx_assignment_target_student (student_id, assignment_id),
  KEY idx_assignment_target_class (class_id, assignment_id),
  CONSTRAINT ck_qb_assignment_target_scope CHECK (
    (target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
    OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_class (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_name VARCHAR(128) NOT NULL,
  class_code VARCHAR(16) NOT NULL,
  class_desc TEXT NULL,
  teacher_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_qb_class_code (class_code),
  KEY idx_qb_class_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_class_member (
  class_id BIGINT UNSIGNED NOT NULL,
  student_id BIGINT UNSIGNED NOT NULL,
  joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (class_id, student_id),
  KEY idx_qb_class_member_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_attempt (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assignment_id BIGINT UNSIGNED NULL,
  paper_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  attempt_type TINYINT NOT NULL COMMENT '1=assignment,2=practice',
  attempt_no INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=in_progress,2=submitted,3=grading,4=graded',
  started_at DATETIME(3) NULL,
  submitted_at DATETIME(3) NULL,
  duration_sec INT NULL,
  total_score INT NOT NULL DEFAULT 0,
  objective_score INT NOT NULL DEFAULT 0,
  subjective_score INT NOT NULL DEFAULT 0,
  needs_review TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_qb_attempt_assignment_user (assignment_id, user_id),
  KEY idx_qb_attempt_user_time (user_id, created_at),
  KEY idx_qb_attempt_paper (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_attempt_question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  attempt_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  order_no INT NOT NULL DEFAULT 0,
  score INT NOT NULL DEFAULT 0,
  snapshot_json LONGTEXT NULL,
  snapshot_hash VARCHAR(128) NULL,
  question_type TINYINT NULL,
  difficulty TINYINT NULL,
  knowledge_snapshot_json LONGTEXT NULL,
  PRIMARY KEY (id),
  KEY idx_qb_attempt_question_attempt (attempt_id, order_no),
  KEY idx_qb_attempt_question_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_answer (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  attempt_id BIGINT UNSIGNED NOT NULL,
  attempt_question_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  answer_content LONGTEXT NULL,
  answer_format TINYINT NOT NULL DEFAULT 1,
  answer_status TINYINT NOT NULL DEFAULT 1 COMMENT '1=draft,2=submitted',
  auto_score INT NULL,
  final_score INT NULL,
  is_correct TINYINT NULL,
  answered_at DATETIME(3) NULL,
  graded_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_qb_answer_attempt_question (attempt_id, attempt_question_id),
  KEY idx_qb_answer_user (user_id),
  KEY idx_qb_answer_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_grading_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  answer_id BIGINT UNSIGNED NOT NULL,
  grading_mode TINYINT NOT NULL COMMENT '1=auto,2=llm,3=manual',
  score INT NULL,
  detail_json LONGTEXT NULL,
  llm_call_id BIGINT UNSIGNED NULL,
  confidence DOUBLE NULL,
  needs_review TINYINT NOT NULL DEFAULT 0,
  reviewer_id BIGINT UNSIGNED NULL,
  review_comment VARCHAR(1000) NULL,
  is_final TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_qb_grading_answer (answer_id, created_at),
  KEY idx_qb_grading_llm (llm_call_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_appeal (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  answer_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  reason_text TEXT NOT NULL,
  appeal_status TINYINT NOT NULL DEFAULT 1 COMMENT '1=pending,2=approved,3=rejected,4=resolved',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  handled_by BIGINT UNSIGNED NULL,
  handled_at DATETIME(3) NULL,
  decision_comment VARCHAR(1000) NULL,
  final_score INT NULL,
  PRIMARY KEY (id),
  KEY idx_qb_appeal_user (user_id),
  KEY idx_qb_appeal_answer (answer_id),
  KEY idx_qb_appeal_status (appeal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_question_user_stat (
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  last_attempt_at DATETIME(3) NULL,
  PRIMARY KEY (user_id, question_id),
  KEY idx_qbus_user_question (user_id, question_id),
  KEY idx_qbus_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_wrong_question (
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  wrong_count INT NOT NULL DEFAULT 1,
  first_wrong_at DATETIME(3) NOT NULL,
  last_wrong_at DATETIME(3) NOT NULL,
  is_resolved TINYINT NOT NULL DEFAULT 0,
  resolved_at DATETIME(3) NULL,
  PRIMARY KEY (user_id, question_id),
  KEY idx_qbw_user_resolved_question (user_id, is_resolved, question_id),
  KEY idx_qbw_question (question_id)
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

-- Stage 05: persistent student profile and stage-evaluation structures.
CREATE TABLE IF NOT EXISTS student_basic_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, student_no VARCHAR(64) NULL,
  major_name VARCHAR(128) NULL, grade_name VARCHAR(64) NULL, education_level VARCHAR(32) NULL, learning_stage VARCHAR(40) NULL,
  weekly_available_hours DECIMAL(6,2) NULL, updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_basic_profile_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_learning_goal (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, goal_type VARCHAR(24) NOT NULL DEFAULT 'occupation',
  target_occupation_id BIGINT UNSIGNED NULL, target_skill_id BIGINT UNSIGNED NULL, target_knowledge_point_id BIGINT UNSIGNED NULL,
  goal_description VARCHAR(1000) NULL, target_level DECIMAL(6,4) NULL, expected_completion_date DATE NULL, weekly_available_hours DECIMAL(6,2) NULL,
  priority INT NOT NULL DEFAULT 1, status VARCHAR(24) NOT NULL DEFAULT 'active', source_type VARCHAR(32) NOT NULL DEFAULT 'self_report',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), KEY idx_student_goal_user_status(user_id,status,priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_evidence (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, evidence_type VARCHAR(40) NOT NULL,
  source_entity_type VARCHAR(40) NOT NULL, source_entity_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(24) NOT NULL, target_id BIGINT UNSIGNED NULL,
  evidence_value DECIMAL(10,4) NULL, evidence_direction TINYINT NOT NULL DEFAULT 1, confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  evidence_text VARCHAR(2000) NULL, occurred_at DATETIME(3) NOT NULL, extract_version VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_student_evidence_source_target(user_id,source_entity_type,source_entity_id,target_type,target_id,extract_version),
  KEY idx_student_evidence_user_time(user_id,occurred_at), KEY idx_student_evidence_target(target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ability_dimension (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, dimension_code VARCHAR(64) NOT NULL, dimension_name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL, score_min DECIMAL(10,4) NOT NULL DEFAULT 0.0000, score_max DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
  version VARCHAR(64) NOT NULL DEFAULT 'v1', status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_ability_dimension_code_version(dimension_code,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_ability_state (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, dimension_id BIGINT UNSIGNED NOT NULL,
  score DECIMAL(10,4) NOT NULL DEFAULT 0.0000, level VARCHAR(24) NULL, confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  evidence_count INT NOT NULL DEFAULT 0, updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_ability_state(user_id,dimension_id), KEY idx_student_ability_dimension(dimension_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_skill_state (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, skill_id BIGINT UNSIGNED NOT NULL,
  proficiency_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000, proficiency_level VARCHAR(24) NULL, confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  evidence_count INT NOT NULL DEFAULT 0, gap_to_target DECIMAL(6,4) NULL, last_evidence_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_skill_state(user_id,skill_id), KEY idx_student_skill_state_skill(skill_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_learning_preference (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, preference_type VARCHAR(40) NOT NULL,
  preference_value VARCHAR(255) NOT NULL, preference_score DECIMAL(6,4) NULL, source_type VARCHAR(32) NOT NULL DEFAULT 'self_report',
  evidence_count INT NOT NULL DEFAULT 1, valid_from DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), valid_to DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_student_preference_active(user_id,preference_type,preference_value,valid_to), KEY idx_student_preference_user_type(user_id,preference_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_summary (
  user_id BIGINT UNSIGNED NOT NULL, target_occupation_id BIGINT UNSIGNED NULL, active_goal_id BIGINT UNSIGNED NULL,
  overall_knowledge_mastery DECIMAL(6,4) NULL, core_knowledge_mastery DECIMAL(6,4) NULL, skill_match_score DECIMAL(6,4) NULL,
  ability_average_score DECIMAL(10,4) NULL, assessment_accuracy DECIMAL(6,4) NULL, course_completion_rate DECIMAL(6,4) NULL,
  learning_activity_score DECIMAL(6,4) NULL, weak_knowledge_count INT NOT NULL DEFAULT 0, weak_skill_count INT NOT NULL DEFAULT 0,
  recommended_difficulty TINYINT NULL, last_profile_snapshot_id BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), PRIMARY KEY(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, basic_state_json LONGTEXT NULL,
  knowledge_state_json LONGTEXT NULL, skill_state_json LONGTEXT NULL, ability_state_json LONGTEXT NULL, preference_state_json LONGTEXT NULL,
  goal_state_json LONGTEXT NULL, category_stat_json LONGTEXT NULL, profile_summary VARCHAR(2000) NULL,
  trigger_type VARCHAR(32) NOT NULL DEFAULT 'scheduled', trigger_id BIGINT UNSIGNED NULL, evidence_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id), KEY idx_student_profile_snapshot_user_time(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_category_stat (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, category_type VARCHAR(32) NOT NULL,
  period_type VARCHAR(24) NOT NULL DEFAULT 'current', total_count INT NOT NULL DEFAULT 0, strong_count INT NOT NULL DEFAULT 0,
  weak_count INT NOT NULL DEFAULT 0, average_score DECIMAL(10,4) NULL, coverage_rate DECIMAL(6,4) NULL,
  top_strengths_json LONGTEXT NULL, top_weaknesses_json LONGTEXT NULL, profile_snapshot_id BIGINT UNSIGNED NULL,
  calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_student_profile_category_current(user_id,category_type,period_type), KEY idx_profile_category_snapshot(profile_snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stage_evaluation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, stage_type VARCHAR(24) NOT NULL,
  start_date DATE NOT NULL, end_date DATE NOT NULL, profile_snapshot_id BIGINT UNSIGNED NULL, overall_score DECIMAL(10,4) NULL,
  dimension_scores_json LONGTEXT NULL, evaluation_text LONGTEXT NULL, evaluator_type VARCHAR(24) NOT NULL DEFAULT 'system',
  status VARCHAR(24) NOT NULL DEFAULT 'final', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  KEY idx_stage_evaluation_user_stage(user_id,stage_type,start_date,end_date,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ability_dimension(dimension_code,dimension_name,description,score_min,score_max,version,status)
VALUES ('ABILITY','能力水平','基于已评分作答的综合能力分数',0,100,'v1',1),('MASTERY','知识掌握','学生知识点掌握度聚合',0,100,'v1',1),('PERFORMANCE','作答表现','近期作答得分率',0,100,'v1',1),('PARTICIPATION','学习参与','学习行为与完成作答参与度',0,100,'v1',1)
ON DUPLICATE KEY UPDATE dimension_name=VALUES(dimension_name),description=VALUES(description),status=VALUES(status),updated_at=NOW(3);

CREATE TABLE IF NOT EXISTS qb_learning_resource (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  resource_type VARCHAR(40) NOT NULL DEFAULT 'article',
  resource_purpose VARCHAR(32) NOT NULL DEFAULT 'learn',
  url VARCHAR(1000) NULL,
  summary VARCHAR(2000) NULL,
  content LONGTEXT NULL,
  difficulty TINYINT NULL,
  generation_type VARCHAR(32) NOT NULL DEFAULT 'manual',
  version VARCHAR(64) NULL,
  personalization_basis JSON NULL,
  review_report_json JSON NULL,
  model_source_json JSON NULL,
  audit_status VARCHAR(40) NOT NULL DEFAULT 'manual',
  agent_task_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_resource_audit_status (audit_status),
  KEY idx_resource_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_learning_behavior (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  behavior_type VARCHAR(40) NOT NULL,
  ref_type VARCHAR(32) NULL,
  ref_id BIGINT UNSIGNED NULL,
  knowledge_point_id BIGINT UNSIGNED NULL,
  duration_seconds INT NULL,
  event_value VARCHAR(255) NULL,
  note VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_behavior_user_time (user_id, created_at),
  KEY idx_behavior_kp (knowledge_point_id)
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

CREATE TABLE IF NOT EXISTS qb_learning_path_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  stage VARCHAR(32) NOT NULL,
  goal VARCHAR(64) NOT NULL,
  days INT NOT NULL DEFAULT 14,
  title VARCHAR(255) NULL,
  summary_text VARCHAR(1000) NULL,
  snapshot_json LONGTEXT NOT NULL,
  snapshot_hash VARCHAR(128) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_learning_path_snapshot_user (user_id, created_at, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_role(role_code, role_name, created_at, updated_at) VALUES
('STUDENT', '学生', NOW(3), NOW(3)),
('TEACHER', '教师', NOW(3), NOW(3)),
('ADMIN', '管理员', NOW(3), NOW(3));

INSERT IGNORE INTO knowledge_point(id, name, code, parent_id, level, knowledge_type, difficulty, description, created_at, updated_at, is_deleted) VALUES
(1, 'C语言基础', 'kp-c-basic', NULL, 1, 'concept', 2, 'C语言课程基础知识结构', NOW(3), NOW(3), 0),
(2, '变量与数据类型', 'kp-c-variable-type', 1, 2, 'concept', 2, '变量声明、基本数据类型与类型转换', NOW(3), NOW(3), 0),
(3, '分支与循环', 'kp-c-control-flow', 1, 2, 'concept', 3, 'if、switch、for、while 等控制结构', NOW(3), NOW(3), 0),
(4, '数组与字符串', 'kp-c-array-string', 1, 2, 'concept', 3, '数组、字符数组与字符串处理', NOW(3), NOW(3), 0),
(5, '函数', 'kp-c-function', 1, 2, 'concept', 3, '函数定义、调用、参数与返回值', NOW(3), NOW(3), 0),
(6, '指针', 'kp-c-pointer', 1, 2, 'concept', 4, '指针、地址、指针运算与数组关系', NOW(3), NOW(3), 0);

INSERT IGNORE INTO qb_learning_resource(id, title, resource_type, resource_purpose, url, summary, generation_type, created_by, created_at, updated_at, is_deleted) VALUES
(1, 'C语言变量与数据类型复习', 'article', 'learn', 'https://www.runoob.com/cprogramming/c-variables.html', '复习变量声明、基础类型、常量与类型转换。', 'manual', NULL, NOW(3), NOW(3), 0),
(2, 'C语言循环结构复习', 'article', 'learn', 'https://www.runoob.com/cprogramming/c-loops.html', '复习 for、while、do while 的使用场景。', 'manual', NULL, NOW(3), NOW(3), 0),
(3, 'C语言数组复习', 'article', 'learn', 'https://www.runoob.com/cprogramming/c-arrays.html', '复习一维数组、多维数组和数组遍历。', 'manual', NULL, NOW(3), NOW(3), 0),
(4, 'C语言函数复习', 'article', 'learn', 'https://www.runoob.com/cprogramming/c-functions.html', '复习函数声明、定义、参数传递和返回值。', 'manual', NULL, NOW(3), NOW(3), 0),
(5, 'C语言指针复习', 'article', 'learn', 'https://www.runoob.com/cprogramming/c-pointers.html', '复习指针变量、取地址、解引用和指针数组关系。', 'manual', NULL, NOW(3), NOW(3), 0);

INSERT IGNORE INTO resource_knowledge(resource_id, knowledge_point_id, relation_type, coverage_weight, is_primary, created_at) VALUES
(1, 2, 'cover', 1.0000, 1, NOW(3)),
(2, 3, 'cover', 1.0000, 1, NOW(3)),
(3, 4, 'cover', 1.0000, 1, NOW(3)),
(4, 5, 'cover', 1.0000, 1, NOW(3)),
(5, 6, 'cover', 1.0000, 1, NOW(3));

SET FOREIGN_KEY_CHECKS = 1;

