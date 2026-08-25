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

CREATE TABLE IF NOT EXISTS qb_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tag_name VARCHAR(100) NOT NULL,
  tag_code VARCHAR(100) NULL,
  parent_id BIGINT UNSIGNED NULL,
  tag_level INT NOT NULL DEFAULT 1,
  tag_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=knowledge,2=chapter,3=custom',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_qb_tag_code (tag_code),
  KEY idx_qb_tag_parent (parent_id),
  KEY idx_qb_tag_type (tag_type, is_deleted)
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

CREATE TABLE IF NOT EXISTS qb_question_tag (
  question_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (question_id, tag_id),
  KEY idx_qbqt_question_tag (question_id, tag_id),
  KEY idx_qbqt_tag (tag_id)
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
  tag_ids_json TEXT NULL,
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

CREATE TABLE IF NOT EXISTS qb_tag_mastery (
  user_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  mastery_value DOUBLE NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  attempt_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, tag_id),
  KEY idx_qb_tag_mastery_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_user_ability (
  user_id BIGINT UNSIGNED NOT NULL,
  ability_score INT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_knowledge_point (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(100) NULL,
  parent_id BIGINT UNSIGNED NULL,
  tag_id BIGINT UNSIGNED NULL,
  level INT NOT NULL DEFAULT 1,
  description VARCHAR(1000) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_kp_code (code),
  KEY idx_kp_parent (parent_id),
  KEY idx_kp_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_knowledge_relation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_id BIGINT UNSIGNED NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(40) NOT NULL DEFAULT 'prerequisite',
  weight DOUBLE NOT NULL DEFAULT 1.0,
  confidence DOUBLE NOT NULL DEFAULT 1.0,
  source_type VARCHAR(40) NOT NULL DEFAULT 'manual',
  description VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_relation (source_id, target_id, relation_type),
  KEY idx_knowledge_relation_source (source_id, relation_type, is_deleted),
  KEY idx_knowledge_relation_target (target_id, relation_type, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_learning_resource (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  resource_type VARCHAR(40) NOT NULL DEFAULT 'article',
  url VARCHAR(1000) NULL,
  summary VARCHAR(2000) NULL,
  content LONGTEXT NULL,
  personalization_basis JSON NULL,
  review_report_json JSON NULL,
  model_source_json JSON NULL,
  audit_status VARCHAR(40) NOT NULL DEFAULT 'manual',
  knowledge_point_id BIGINT UNSIGNED NULL,
  tag_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_resource_kp (knowledge_point_id),
  KEY idx_resource_tag (tag_id),
  KEY idx_resource_audit_status (audit_status),
  KEY idx_resource_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_learning_behavior (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  behavior_type VARCHAR(40) NOT NULL,
  ref_id BIGINT UNSIGNED NULL,
  knowledge_point_id BIGINT UNSIGNED NULL,
  tag_id BIGINT UNSIGNED NULL,
  duration_seconds INT NULL,
  note VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_behavior_user_time (user_id, created_at),
  KEY idx_behavior_kp (knowledge_point_id),
  KEY idx_behavior_tag (tag_id)
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

INSERT IGNORE INTO qb_tag(id, tag_name, tag_code, parent_id, tag_level, tag_type, sort_order, created_at, updated_at, is_deleted) VALUES
(1, 'C语言基础', 'c-basic', NULL, 1, 1, 10, NOW(3), NOW(3), 0),
(2, '变量与数据类型', 'c-variable-type', 1, 2, 1, 20, NOW(3), NOW(3), 0),
(3, '分支与循环', 'c-control-flow', 1, 2, 1, 30, NOW(3), NOW(3), 0),
(4, '数组与字符串', 'c-array-string', 1, 2, 1, 40, NOW(3), NOW(3), 0),
(5, '函数', 'c-function', 1, 2, 1, 50, NOW(3), NOW(3), 0),
(6, '指针', 'c-pointer', 1, 2, 1, 60, NOW(3), NOW(3), 0);

INSERT IGNORE INTO qb_knowledge_point(id, name, code, parent_id, tag_id, level, description, sort_order, created_at, updated_at, is_deleted) VALUES
(1, 'C语言基础', 'kp-c-basic', NULL, 1, 1, 'C语言课程基础知识结构', 10, NOW(3), NOW(3), 0),
(2, '变量与数据类型', 'kp-c-variable-type', 1, 2, 2, '变量声明、基本数据类型与类型转换', 20, NOW(3), NOW(3), 0),
(3, '分支与循环', 'kp-c-control-flow', 1, 3, 2, 'if/switch/for/while 等控制结构', 30, NOW(3), NOW(3), 0),
(4, '数组与字符串', 'kp-c-array-string', 1, 4, 2, '数组、字符数组与字符串处理', 40, NOW(3), NOW(3), 0),
(5, '函数', 'kp-c-function', 1, 5, 2, '函数定义、调用、参数与返回值', 50, NOW(3), NOW(3), 0),
(6, '指针', 'kp-c-pointer', 1, 6, 2, '指针、地址、指针运算与数组关系', 60, NOW(3), NOW(3), 0);

INSERT IGNORE INTO qb_learning_resource(title, resource_type, url, summary, knowledge_point_id, tag_id, created_by, created_at, updated_at, is_deleted) VALUES
('C语言变量与数据类型复习', 'article', 'https://www.runoob.com/cprogramming/c-variables.html', '复习变量声明、基础类型、常量与类型转换。', 2, 2, NULL, NOW(3), NOW(3), 0),
('C语言循环结构复习', 'article', 'https://www.runoob.com/cprogramming/c-loops.html', '复习 for、while、do while 的使用场景。', 3, 3, NULL, NOW(3), NOW(3), 0),
('C语言数组复习', 'article', 'https://www.runoob.com/cprogramming/c-arrays.html', '复习一维数组、多维数组和数组遍历。', 4, 4, NULL, NOW(3), NOW(3), 0),
('C语言函数复习', 'article', 'https://www.runoob.com/cprogramming/c-functions.html', '复习函数声明、定义、参数传递和返回值。', 5, 5, NULL, NOW(3), NOW(3), 0),
('C语言指针复习', 'article', 'https://www.runoob.com/cprogramming/c-pointers.html', '复习指针变量、取地址、解引用和指针数组关系。', 6, 6, NULL, NOW(3), NOW(3), 0);

SET FOREIGN_KEY_CHECKS = 1;

