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
  course_id BIGINT UNSIGNED NULL,
  chapter_id BIGINT UNSIGNED NULL,
  name VARCHAR(150) NOT NULL,
  code VARCHAR(100) NULL,
  parent_id BIGINT UNSIGNED NULL,
  level INT NOT NULL DEFAULT 1,
  knowledge_type VARCHAR(32) NOT NULL DEFAULT 'concept',
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  content_version VARCHAR(64) NULL,
  metadata_json JSON NULL,
  difficulty TINYINT NOT NULL DEFAULT 3,
  description VARCHAR(2000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_point_course_code (course_id, code),
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
  course_id BIGINT UNSIGNED NULL,
  graph_version_id BIGINT UNSIGNED NULL,
  relation_code VARCHAR(64) NULL,
  source_knowledge_point_id BIGINT UNSIGNED NULL,
  target_knowledge_point_id BIGINT UNSIGNED NULL,
  source_id BIGINT UNSIGNED NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(40) NOT NULL DEFAULT 'prerequisite',
  weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  source_type VARCHAR(40) NOT NULL DEFAULT 'manual',
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME(3) NULL,
  created_by BIGINT UNSIGNED NULL,
  description VARCHAR(1000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_relation_version (graph_version_id, source_id, target_id, relation_type),
  KEY idx_knowledge_relation_source (source_id, relation_type, is_deleted),
  KEY idx_knowledge_relation_target (target_id, relation_type, is_deleted),
  KEY idx_knowledge_relation_source_fk (source_id),
  KEY idx_knowledge_relation_target_fk (target_id),
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
  biz_type VARCHAR(40) NOT NULL,
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
  KEY idx_qb_llm_call_biz (biz_type, biz_id, created_at),
  KEY idx_qb_llm_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, owner_type VARCHAR(24) NOT NULL DEFAULT 'system', owner_id BIGINT UNSIGNED NULL,
  provider_key VARCHAR(100) NOT NULL, label VARCHAR(128) NOT NULL, provider_type VARCHAR(32) NOT NULL DEFAULT 'API', base_url VARCHAR(500) NOT NULL,
  api_key_cipher TEXT NULL, model VARCHAR(128) NOT NULL, temperature DOUBLE NULL, enabled TINYINT NOT NULL DEFAULT 1, is_default TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_model_config_owner_key(owner_type,owner_id,provider_key), KEY idx_model_config_owner_enabled(owner_type,owner_id,enabled,is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, owner_type VARCHAR(24) NOT NULL DEFAULT 'system', owner_id BIGINT UNSIGNED NULL,
  template_name VARCHAR(128) NOT NULL, task_type VARCHAR(64) NOT NULL, description VARCHAR(1000) NULL, prompt_text LONGTEXT NOT NULL, version VARCHAR(64) NOT NULL DEFAULT 'v1',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_prompt_template_owner_version(owner_type,owner_id,template_name,task_type,version), KEY idx_prompt_template_owner_task(owner_type,owner_id,task_type,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_definition (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, agent_code VARCHAR(64) NOT NULL, agent_name VARCHAR(128) NOT NULL, role_type VARCHAR(32) NOT NULL,
  description VARCHAR(1000) NULL, default_model_config_id BIGINT UNSIGNED NULL, prompt_template_id BIGINT UNSIGNED NULL, config_json LONGTEXT NULL,
  status TINYINT NOT NULL DEFAULT 1, version VARCHAR(64) NOT NULL DEFAULT 'v1', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_agent_definition_code_version(agent_code,version), KEY idx_agent_definition_enabled(agent_code,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, task_code VARCHAR(64) NULL, task_type VARCHAR(40) NOT NULL, user_id BIGINT UNSIGNED NULL, teacher_id BIGINT UNSIGNED NULL,
  target_type VARCHAR(24) NULL, target_id BIGINT UNSIGNED NULL, input_json LONGTEXT NULL, status VARCHAR(24) NOT NULL DEFAULT 'queued', current_step_no INT NOT NULL DEFAULT 0,
  result_summary LONGTEXT NULL, error_message VARCHAR(2000) NULL, started_at DATETIME(3) NULL, finished_at DATETIME(3) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_agent_task_code(task_code), KEY idx_agent_task_status_created(status,created_at), KEY idx_agent_task_teacher_created(teacher_id,created_at), KEY idx_agent_task_user_created(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_step (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, agent_task_id BIGINT UNSIGNED NOT NULL, step_no INT NOT NULL, agent_definition_id BIGINT UNSIGNED NULL, step_type VARCHAR(40) NOT NULL,
  input_json LONGTEXT NULL, output_json LONGTEXT NULL, llm_call_id BIGINT UNSIGNED NULL, status VARCHAR(24) NOT NULL DEFAULT 'pending', latency_ms INT NULL,
  started_at DATETIME(3) NULL, finished_at DATETIME(3) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_agent_step_task_no(agent_task_id,step_no), KEY idx_agent_step_call(llm_call_id), KEY idx_agent_step_status(status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_review (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, agent_task_id BIGINT UNSIGNED NULL, agent_step_id BIGINT UNSIGNED NULL, target_type VARCHAR(24) NOT NULL, target_id BIGINT UNSIGNED NULL,
  factual_score DECIMAL(6,4) NULL, coverage_score DECIMAL(6,4) NULL, difficulty_match_score DECIMAL(6,4) NULL, hallucination_rate DECIMAL(6,4) NULL, source_consistency_score DECIMAL(6,4) NULL,
  review_status VARCHAR(24) NOT NULL DEFAULT 'pending', review_report LONGTEXT NULL, bundle_id BIGINT UNSIGNED NULL, blueprint_id BIGINT UNSIGNED NULL, review_dimension VARCHAR(64) NULL, issue_code VARCHAR(64) NULL,
  repair_target VARCHAR(64) NULL, repair_scope VARCHAR(255) NULL, repair_action VARCHAR(255) NULL, repair_instruction VARCHAR(2000) NULL, round_no INT NULL,
  evidence_refs_json JSON NULL, tool_result_json JSON NULL, reviewer_role VARCHAR(64) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  KEY idx_agent_review_task(agent_task_id,created_at), KEY idx_agent_review_step(agent_step_id), KEY idx_agent_review_status(review_status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_decision (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, agent_task_id BIGINT UNSIGNED NOT NULL, agent_step_id BIGINT UNSIGNED NULL, decision_type VARCHAR(40) NOT NULL, target_type VARCHAR(24) NULL, target_id BIGINT UNSIGNED NULL,
  decision_value VARCHAR(255) NULL, decision_reason LONGTEXT NULL, confidence DECIMAL(6,4) NULL, evidence_json LONGTEXT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  KEY idx_agent_decision_task(agent_task_id,created_at), KEY idx_agent_decision_step(agent_step_id), KEY idx_agent_decision_type(decision_type,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO agent_definition(agent_code,agent_name,role_type,description,status,version)
VALUES ('PROFILE','画像智能体','profile','读取持久化画像摘要，不修改画像',1,'v1'),
       ('DIAGNOSIS','诊断智能体','diagnosis','分析知识点、能力与行为证据',1,'v1'),
       ('PLANNER','规划智能体','plan','形成资源与教学行动建议',1,'v1'),
       ('GENERATOR','资源生成智能体','generate','生成结构化学习资源草案',1,'v1'),
       ('REVIEWER','资源审核智能体','review','审核事实、覆盖、难度与一致性',1,'v1')
ON DUPLICATE KEY UPDATE agent_name=VALUES(agent_name),role_type=VALUES(role_type),description=VALUES(description),status=VALUES(status),updated_at=NOW(3);

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
  generated_question_code VARCHAR(64) NULL,
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
  course_id BIGINT UNSIGNED NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  mastery_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  mastery_level VARCHAR(24) NULL,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  evidence_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  attempt_count INT NOT NULL DEFAULT 0,
  state_version BIGINT UNSIGNED NOT NULL DEFAULT 0,
  calculation_method VARCHAR(64) NOT NULL DEFAULT 'LEGACY_RATIO',
  algorithm_version VARCHAR(64) NOT NULL DEFAULT 'legacy_v1',
  last_interaction_seq BIGINT UNSIGNED NOT NULL DEFAULT 0,
  last_interaction_id BIGINT UNSIGNED NULL,
  last_evidence_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_knowledge_state (user_id, course_id, knowledge_point_id),
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
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, course_id BIGINT UNSIGNED NULL, profile_version BIGINT UNSIGNED NULL, calculated_at DATETIME(3) NULL, algorithm_version VARCHAR(64) NULL, correlation_id VARCHAR(64) NULL, basic_state_json LONGTEXT NULL,
  knowledge_state_json LONGTEXT NULL, skill_state_json LONGTEXT NULL, ability_state_json LONGTEXT NULL, preference_state_json LONGTEXT NULL,
  goal_state_json LONGTEXT NULL, category_stat_json LONGTEXT NULL, resource_preference_json JSON NULL, cognitive_profile_json JSON NULL, initiative_json JSON NULL, regularity_json JSON NULL, profile_summary VARCHAR(2000) NULL,
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
  resource_id BIGINT UNSIGNED NULL,
  resource_item_id BIGINT UNSIGNED NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(24) NOT NULL DEFAULT 'cover',
  coverage_weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  is_primary TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_knowledge (resource_id, knowledge_point_id, relation_type),
  UNIQUE KEY uk_resource_item_knowledge (resource_item_id, knowledge_point_id, relation_type),
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

CREATE TABLE IF NOT EXISTS course (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, course_code VARCHAR(64) NULL, course_name VARCHAR(200) NOT NULL, description VARCHAR(2000) NULL, teacher_id BIGINT UNSIGNED NULL, status VARCHAR(24) NOT NULL DEFAULT 'active', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY(id), UNIQUE KEY uk_course_code(course_code), KEY idx_course_teacher_status(teacher_id,status,is_deleted), CONSTRAINT fk_course_teacher FOREIGN KEY(teacher_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_knowledge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, course_id BIGINT UNSIGNED NOT NULL, knowledge_point_id BIGINT UNSIGNED NOT NULL, sequence_no INT NOT NULL DEFAULT 0, is_core TINYINT NOT NULL DEFAULT 0, coverage_weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_course_knowledge_point(course_id,knowledge_point_id), UNIQUE KEY uk_course_knowledge_sequence(course_id,sequence_no), KEY idx_course_knowledge_point(knowledge_point_id), CONSTRAINT fk_course_knowledge_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_course_knowledge_point FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_course_progress (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, course_id BIGINT UNSIGNED NOT NULL, progress_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000, completed_knowledge_count INT NOT NULL DEFAULT 0, total_knowledge_count INT NOT NULL DEFAULT 0, status VARCHAR(24) NOT NULL DEFAULT 'not_started', last_learning_at DATETIME(3) NULL, updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_course_progress(user_id,course_id), KEY idx_student_course_progress_course(course_id,status,updated_at), CONSTRAINT fk_student_course_progress_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_student_course_progress_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_path (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, path_code VARCHAR(64) NULL, user_id BIGINT UNSIGNED NOT NULL, course_id BIGINT UNSIGNED NULL, target_knowledge_point_id BIGINT UNSIGNED NULL, current_version BIGINT UNSIGNED NULL, idempotency_key VARCHAR(128) NULL, goal_id BIGINT UNSIGNED NULL, target_occupation_id BIGINT UNSIGNED NULL, profile_snapshot_id BIGINT UNSIGNED NULL, title VARCHAR(255) NOT NULL, stage VARCHAR(32) NULL, planning_days INT NOT NULL DEFAULT 14, version BIGINT UNSIGNED NOT NULL DEFAULT 1, status VARCHAR(24) NOT NULL DEFAULT 'active', summary_text VARCHAR(2000) NULL, generated_by_agent_task_id BIGINT UNSIGNED NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY(id), KEY idx_learning_path_user_course_status(user_id,course_id,status,is_deleted), KEY idx_learning_path_course_status(course_id,status,updated_at), CONSTRAINT fk_learning_path_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_profile_snapshot FOREIGN KEY(profile_snapshot_id) REFERENCES student_profile_snapshot(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_path_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, path_id BIGINT UNSIGNED NOT NULL, path_version_id BIGINT UNSIGNED NULL, path_step_code VARCHAR(64) NULL, order_no INT NOT NULL DEFAULT 0, item_type VARCHAR(24) NOT NULL, knowledge_point_id BIGINT UNSIGNED NULL, resource_id BIGINT UNSIGNED NULL, question_id BIGINT UNSIGNED NULL, assignment_id BIGINT UNSIGNED NULL, stage VARCHAR(24) NULL, reason_code VARCHAR(64) NULL, mastery_before DECIMAL(6,4) NULL, confidence_before DECIMAL(6,4) NULL, completed_at DATETIME(3) NULL, planned_start_at DATETIME(3) NULL, planned_end_at DATETIME(3) NULL, status VARCHAR(24) NOT NULL DEFAULT 'pending', decision_reason VARCHAR(2000) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_learning_path_item_order(path_id,order_no), KEY idx_learning_path_item_knowledge(knowledge_point_id,status), KEY idx_learning_path_item_resource(resource_id,status), CONSTRAINT fk_learning_path_item_path FOREIGN KEY(path_id) REFERENCES learning_path(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_item_knowledge FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_item_resource FOREIGN KEY(resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_item_question FOREIGN KEY(question_id) REFERENCES qb_question(id) ON DELETE RESTRICT ON UPDATE RESTRICT, CONSTRAINT fk_learning_path_item_assignment FOREIGN KEY(assignment_id) REFERENCES qb_assignment(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stage 08-16 personalized learning loop baseline.
CREATE TABLE IF NOT EXISTS course_chapter(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,course_id BIGINT UNSIGNED NOT NULL,chapter_code VARCHAR(64) NOT NULL,chapter_name VARCHAR(255) NOT NULL,order_no INT NOT NULL,status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_course_chapter_code(course_id,chapter_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_graph_version(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,version_code VARCHAR(64) NOT NULL,course_id BIGINT UNSIGNED NOT NULL,description VARCHAR(500) NULL,status VARCHAR(24) NOT NULL,node_count INT NOT NULL DEFAULT 0,edge_count INT NOT NULL DEFAULT 0,content_hash CHAR(64) NULL,validation_report_json JSON NULL,correlation_id VARCHAR(64) NULL,created_by BIGINT UNSIGNED NULL,activated_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_graph_version_code(version_code),KEY idx_graph_version_course_status(course_id,status)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_graph_sync_record(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,graph_version_id BIGINT UNSIGNED NOT NULL,sync_code VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,node_count INT NULL,edge_count INT NULL,content_hash CHAR(64) NULL,error_message VARCHAR(1000) NULL,correlation_id VARCHAR(64) NULL,started_at DATETIME(3) NULL,finished_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_graph_sync_code(sync_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_relation_source(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,relation_id BIGINT UNSIGNED NOT NULL,source_chunk_id BIGINT UNSIGNED NOT NULL,evidence_type VARCHAR(32) NOT NULL,citation_text VARCHAR(1000) NULL,confidence DECIMAL(6,4) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_relation_source(relation_id,source_chunk_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_resource_preference(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,resource_type VARCHAR(32) NOT NULL,score DECIMAL(6,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_resource_preference(user_id,course_id,resource_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_cognitive_state(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,cognitive_level VARCHAR(24) NOT NULL,score DECIMAL(6,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_cognitive_state(user_id,course_id,cognitive_level)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_behavior_metric(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,metric_group VARCHAR(32) NOT NULL,metric_code VARCHAR(64) NOT NULL,value DECIMAL(10,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_behavior_metric(user_id,course_id,metric_group,metric_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_state_update_log(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,knowledge_point_id BIGINT UNSIGNED NOT NULL,interaction_id BIGINT UNSIGNED NOT NULL,evidence_scope VARCHAR(16) NOT NULL,previous_mastery DECIMAL(6,4) NOT NULL,new_mastery DECIMAL(6,4) NOT NULL,previous_confidence DECIMAL(6,4) NOT NULL,new_confidence DECIMAL(6,4) NOT NULL,model_version VARCHAR(64) NOT NULL,profile_version_before BIGINT UNSIGNED NOT NULL,profile_version_after BIGINT UNSIGNED NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_state_update_interaction(interaction_id,knowledge_point_id,evidence_scope)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS learning_path_version(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,path_id BIGINT UNSIGNED NOT NULL,version BIGINT UNSIGNED NOT NULL,path_mode VARCHAR(32) NOT NULL,based_on_profile_version BIGINT UNSIGNED NOT NULL,graph_version VARCHAR(64) NOT NULL,policy_version VARCHAR(64) NOT NULL,model_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,change_reason VARCHAR(64) NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_path_version(path_id,version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS learning_path_progress(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,path_id BIGINT UNSIGNED NOT NULL,last_processed_interaction_seq BIGINT UNSIGNED NOT NULL DEFAULT 0,last_processed_interaction_id BIGINT UNSIGNED NULL,consecutive_wrong_count INT NOT NULL DEFAULT 0,window_attempt_count INT NOT NULL DEFAULT 0,window_mastery_start DECIMAL(6,4) NULL,updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_path_progress(path_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS path_update_log(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,path_id BIGINT UNSIGNED NOT NULL,old_version BIGINT UNSIGNED NULL,new_version BIGINT UNSIGNED NOT NULL,trigger_event_types_json JSON NOT NULL,trigger_interaction_ids_json JSON NOT NULL,affected_knowledge_point_ids_json JSON NOT NULL,added_step_ids_json JSON NOT NULL,removed_step_ids_json JSON NOT NULL,retained_step_ids_json JSON NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_unit(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,resource_unit_code VARCHAR(64) NOT NULL,course_id BIGINT UNSIGNED NOT NULL,path_id BIGINT UNSIGNED NOT NULL,path_version_id BIGINT UNSIGNED NOT NULL,status VARCHAR(24) NOT NULL,aggregation_evidence_json JSON NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_resource_unit_code(resource_unit_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_unit_step(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,resource_unit_id BIGINT UNSIGNED NOT NULL,path_step_id BIGINT UNSIGNED NOT NULL,order_no INT NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_unit_step_version(path_step_id),UNIQUE KEY uk_unit_order(resource_unit_id,order_no)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_blueprint(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,blueprint_code VARCHAR(64) NOT NULL,resource_unit_id BIGINT UNSIGNED NOT NULL,profile_version_used BIGINT UNSIGNED NOT NULL,policy_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,resource_plan_json JSON NOT NULL,learning_question_plan_json JSON NOT NULL,hidden_assessment_plan_json JSON NOT NULL,profile_evidence_json JSON NOT NULL,graph_evidence_json JSON NOT NULL,capability_snapshot_json JSON NOT NULL,schema_version VARCHAR(64) NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_blueprint_code(blueprint_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_generation_job(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,job_code VARCHAR(64) NOT NULL,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,path_version_id BIGINT UNSIGNED NOT NULL,resource_unit_id BIGINT UNSIGNED NULL,blueprint_id BIGINT UNSIGNED NULL,generation_policy_version VARCHAR(64) NOT NULL,idempotency_key VARCHAR(128) NOT NULL,status VARCHAR(24) NOT NULL,retry_count INT NOT NULL DEFAULT 0,max_revision_rounds INT NOT NULL DEFAULT 5,input_snapshot_json JSON NULL,error_code VARCHAR(64) NULL,error_message VARCHAR(1000) NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),started_at DATETIME(3) NULL,finished_at DATETIME(3) NULL,PRIMARY KEY(id),UNIQUE KEY uk_resource_job_code(job_code),UNIQUE KEY uk_resource_job_idempotency(user_id,idempotency_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_bundle(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,bundle_code VARCHAR(64) NOT NULL,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,resource_unit_id BIGINT UNSIGNED NOT NULL,blueprint_id BIGINT UNSIGNED NOT NULL,version BIGINT UNSIGNED NOT NULL,status VARCHAR(24) NOT NULL,profile_version_used BIGINT UNSIGNED NOT NULL,graph_version VARCHAR(64) NOT NULL,policy_version VARCHAR(64) NOT NULL,content_hash CHAR(64) NULL,published_at DATETIME(3) NULL,stale_reason VARCHAR(255) NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_bundle_code(bundle_code),UNIQUE KEY uk_bundle_version(resource_unit_id,version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_item(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,bundle_id BIGINT UNSIGNED NOT NULL,item_code VARCHAR(64) NOT NULL,generated_question_code VARCHAR(64) NULL,item_type VARCHAR(32) NOT NULL,purpose VARCHAR(32) NOT NULL,visibility VARCHAR(32) NOT NULL,title VARCHAR(500) NOT NULL,content_json JSON NOT NULL,question_difficulty DECIMAL(6,4) NULL,cognitive_level VARCHAR(24) NULL,grading_key_json JSON NULL,order_no INT NOT NULL,status VARCHAR(24) NOT NULL,normalized_text_hash CHAR(64) NULL,simhash64 BIGINT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_resource_item_code(bundle_id,item_code),UNIQUE KEY uk_generated_question_code(generated_question_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_review(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,job_id BIGINT UNSIGNED NOT NULL,bundle_id BIGINT UNSIGNED NULL,blueprint_id BIGINT UNSIGNED NOT NULL,expert_role VARCHAR(64) NOT NULL,result VARCHAR(16) NOT NULL,issue_type VARCHAR(64) NULL,location VARCHAR(255) NULL,description VARCHAR(1000) NULL,repair_target VARCHAR(64) NULL,repair_scope VARCHAR(255) NULL,repair_action VARCHAR(255) NULL,repair_instruction VARCHAR(2000) NULL,critical TINYINT NOT NULL DEFAULT 0,round_no INT NOT NULL,report_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_assessment_release(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,release_code VARCHAR(64) NOT NULL,user_id BIGINT UNSIGNED NOT NULL,bundle_id BIGINT UNSIGNED NOT NULL,resource_item_id BIGINT UNSIGNED NOT NULL,status VARCHAR(24) NOT NULL,released_at DATETIME(3) NOT NULL,expires_at DATETIME(3) NULL,consumed_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_assessment_release_code(release_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_interaction(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,interaction_code VARCHAR(64) NOT NULL,interaction_seq BIGINT UNSIGNED NOT NULL,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,resource_bundle_id BIGINT UNSIGNED NOT NULL,resource_version BIGINT UNSIGNED NOT NULL,resource_unit_id BIGINT UNSIGNED NOT NULL,resource_item_id BIGINT UNSIGNED NOT NULL,generated_question_code VARCHAR(64) NOT NULL,question_purpose VARCHAR(32) NOT NULL,visibility VARCHAR(32) NOT NULL,question_difficulty DECIMAL(6,4) NOT NULL,primary_knowledge_point_id BIGINT UNSIGNED NOT NULL,knowledge_point_weights_json JSON NOT NULL,score_normalized DECIMAL(6,4) NOT NULL,correct TINYINT NOT NULL,status VARCHAR(24) NOT NULL,action_origin VARCHAR(32) NOT NULL,grading_version VARCHAR(64) NOT NULL,answer_json JSON NOT NULL,request_id VARCHAR(128) NOT NULL,correlation_id VARCHAR(64) NOT NULL,client_occurred_at DATETIME(3) NULL,submitted_at DATETIME(3) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_interaction_code(interaction_code),UNIQUE KEY uk_interaction_request(request_id),UNIQUE KEY uk_interaction_seq(user_id,course_id,interaction_seq)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS profile_evidence_event(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,event_id VARCHAR(64) NOT NULL,interaction_id BIGINT UNSIGNED NOT NULL,consumer_name VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,retry_count INT NOT NULL DEFAULT 0,processed_at DATETIME(3) NULL,error_message VARCHAR(1000) NULL,PRIMARY KEY(id),UNIQUE KEY uk_profile_evidence_consumer(event_id,consumer_name),UNIQUE KEY uk_profile_evidence_interaction_consumer(interaction_id,consumer_name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS outbox_event(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,event_id VARCHAR(64) NOT NULL,aggregate_type VARCHAR(64) NOT NULL,aggregate_id BIGINT UNSIGNED NOT NULL,event_type VARCHAR(128) NOT NULL,payload_json JSON NOT NULL,status VARCHAR(24) NOT NULL,retry_count INT NOT NULL DEFAULT 0,next_retry_at DATETIME(3) NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),published_at DATETIME(3) NULL,PRIMARY KEY(id),UNIQUE KEY uk_outbox_event(event_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_action_decision(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,path_id BIGINT UNSIGNED NOT NULL,path_version BIGINT UNSIGNED NOT NULL,interaction_id BIGINT UNSIGNED NOT NULL,action VARCHAR(24) NOT NULL,reason VARCHAR(64) NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_resource_decision_interaction(interaction_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS exam_eligibility_snapshot(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,snapshot_code VARCHAR(64) NOT NULL,user_id BIGINT UNSIGNED NOT NULL,goal_id BIGINT UNSIGNED NOT NULL,eligible TINYINT NOT NULL,course_versions_json JSON NOT NULL,profile_versions_json JSON NOT NULL,rule_version VARCHAR(64) NOT NULL,result_json JSON NOT NULL,correlation_id VARCHAR(64) NOT NULL,calculated_at DATETIME(3) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_eligibility_snapshot_code(snapshot_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_knowledge_model_state(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,model_version VARCHAR(64) NOT NULL,knowledge_index_version VARCHAR(64) NOT NULL,state_ref VARCHAR(255) NOT NULL,processed_through_seq BIGINT UNSIGNED NOT NULL,status VARCHAR(24) NOT NULL,updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_student_model_state(user_id,course_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS profile_algorithm_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,policy_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,learning_rate DECIMAL(6,4) NOT NULL,learning_practice_weight DECIMAL(6,4) NOT NULL,hidden_assessment_weight DECIMAL(6,4) NOT NULL,confidence_scale DECIMAL(8,4) NOT NULL,dimkt_enabled TINYINT NOT NULL DEFAULT 0,config_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_profile_policy_version(policy_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS path_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,policy_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,mastery_threshold DECIMAL(6,4) NOT NULL,confidence_threshold DECIMAL(6,4) NOT NULL,max_path_length INT NOT NULL,max_neighbor_hops INT NOT NULL,config_json JSON NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_path_policy(policy_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS course_resource_capability(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,course_id BIGINT UNSIGNED NOT NULL,resource_type VARCHAR(32) NOT NULL,enabled TINYINT NOT NULL DEFAULT 1,config_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_course_resource_capability(course_id,resource_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_generation_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,policy_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,max_unit_knowledge_points INT NOT NULL DEFAULT 5,max_revision_rounds INT NOT NULL DEFAULT 5,question_similarity_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.7200,config_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_resource_policy(policy_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS training_goal_course_requirement(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,goal_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,required TINYINT NOT NULL DEFAULT 1,rule_version VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_goal_course_requirement(goal_id,course_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS course_completion_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,course_id BIGINT UNSIGNED NOT NULL,rule_version VARCHAR(64) NOT NULL,mastery_threshold DECIMAL(6,4) NOT NULL,confidence_threshold DECIMAL(6,4) NOT NULL,required_completion_rate DECIMAL(6,4) NOT NULL,status VARCHAR(24) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_course_completion_policy(course_id,rule_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_model_rollout_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,course_id BIGINT UNSIGNED NULL,model_version VARCHAR(64) NOT NULL,knowledge_index_version VARCHAR(64) NOT NULL,rollout_percent DECIMAL(6,4) NOT NULL DEFAULT 0,enabled TINYINT NOT NULL DEFAULT 0,fallback_algorithm VARCHAR(64) NOT NULL,config_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_model_rollout(course_id,model_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO profile_algorithm_policy(policy_version,status,learning_rate,learning_practice_weight,hidden_assessment_weight,confidence_scale,dimkt_enabled,config_json) VALUES('weighted_bkt_elo_v1','ACTIVE',0.2000,0.7000,1.0000,5.0000,0,JSON_OBJECT());
INSERT IGNORE INTO path_policy(policy_version,status,mastery_threshold,confidence_threshold,max_path_length,max_neighbor_hops,config_json) VALUES('path_policy_v1','ACTIVE',0.8000,0.6000,50,2,JSON_OBJECT());
INSERT IGNORE INTO resource_generation_policy(policy_version,status,max_unit_knowledge_points,max_revision_rounds,question_similarity_threshold,config_json) VALUES('resource_policy_v1','ACTIVE',5,5,0.7200,JSON_OBJECT());
INSERT IGNORE INTO course_resource_capability(course_id,resource_type,enabled) SELECT id,'concept_explanation',1 FROM course WHERE is_deleted=0;
INSERT IGNORE INTO course_resource_capability(course_id,resource_type,enabled) SELECT id,'worked_example',1 FROM course WHERE is_deleted=0;
INSERT IGNORE INTO course_resource_capability(course_id,resource_type,enabled) SELECT id,'guided_practice',1 FROM course WHERE is_deleted=0;

-- Stage 17-19 course graph import baseline.
CREATE TABLE IF NOT EXISTS file_asset (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,biz_type VARCHAR(40) NULL,biz_id BIGINT UNSIGNED NULL,file_name VARCHAR(255) NOT NULL,file_ext VARCHAR(32) NULL,mime_type VARCHAR(128) NULL,storage_type VARCHAR(24) NOT NULL DEFAULT 'local',storage_path VARCHAR(1000) NOT NULL,file_size BIGINT UNSIGNED NULL,file_hash VARCHAR(128) NULL,uploaded_by BIGINT UNSIGNED NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),is_deleted TINYINT NOT NULL DEFAULT 0,
 PRIMARY KEY(id),KEY idx_file_asset_biz(biz_type,biz_id,is_deleted),KEY idx_file_asset_hash(file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS source_document (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,course_id BIGINT UNSIGNED NULL,title VARCHAR(500) NOT NULL,document_type VARCHAR(40) NOT NULL DEFAULT 'document',source_kind VARCHAR(40) NULL,review_status VARCHAR(24) NULL,import_code VARCHAR(64) NULL,author_org VARCHAR(255) NULL,source_url VARCHAR(1000) NULL,file_asset_id BIGINT UNSIGNED NULL,version VARCHAR(64) NULL,published_at DATETIME(3) NULL,authority_level TINYINT NOT NULL DEFAULT 3,content_hash VARCHAR(128) NULL,parse_status VARCHAR(24) NOT NULL DEFAULT 'pending',created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),is_deleted TINYINT NOT NULL DEFAULT 0,
 PRIMARY KEY(id),KEY idx_source_document_status(parse_status,authority_level,created_at),KEY idx_source_document_hash(content_hash),KEY idx_source_import_review(import_code,review_status,course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS source_chunk (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,document_id BIGINT UNSIGNED NOT NULL,chunk_index INT NOT NULL DEFAULT 0,section_title VARCHAR(500) NULL,page_start INT NULL,page_end INT NULL,content LONGTEXT NOT NULL,content_hash VARCHAR(128) NULL,token_count INT NULL,vector_ref VARCHAR(255) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_source_chunk_document_index(document_id,chunk_index),KEY idx_source_chunk_hash(content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_source (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,knowledge_point_id BIGINT UNSIGNED NOT NULL,source_chunk_id BIGINT UNSIGNED NOT NULL,support_type VARCHAR(24) NOT NULL DEFAULT 'support',relevance_score DECIMAL(6,4) NULL,confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000,link_method VARCHAR(32) NOT NULL DEFAULT 'manual',review_status VARCHAR(24) NOT NULL DEFAULT 'pending',reviewed_by BIGINT UNSIGNED NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_knowledge_source(knowledge_point_id,source_chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_source (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,resource_id BIGINT UNSIGNED NULL,resource_item_id BIGINT UNSIGNED NULL,source_chunk_id BIGINT UNSIGNED NOT NULL,support_type VARCHAR(24) NOT NULL DEFAULT 'grounding',relevance_score DECIMAL(6,4) NULL,citation_order INT NULL,evidence_type VARCHAR(32) NULL,citation_text VARCHAR(1000) NULL,support_score DECIMAL(6,4) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_resource_source(resource_id,source_chunk_id),KEY idx_resource_source_chunk(source_chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_graph_import(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,import_code VARCHAR(64) NOT NULL,idempotency_key VARCHAR(128) NOT NULL,course_code VARCHAR(64) NOT NULL,course_name VARCHAR(255) NOT NULL,schema_version VARCHAR(32) NOT NULL,mode VARCHAR(32) NOT NULL,source_file_name VARCHAR(255) NOT NULL,source_file_hash CHAR(64) NOT NULL,normalized_hash CHAR(64) NOT NULL,validation_hash CHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,course_id BIGINT UNSIGNED NULL,graph_version_id BIGINT UNSIGNED NULL,node_count INT NOT NULL DEFAULT 0,module_count INT NOT NULL DEFAULT 0,category_count INT NOT NULL DEFAULT 0,knowledge_point_count INT NOT NULL DEFAULT 0,contains_count INT NOT NULL DEFAULT 0,prerequisite_count INT NOT NULL DEFAULT 0,similar_count INT NOT NULL DEFAULT 0,error_count INT NOT NULL DEFAULT 0,warning_count INT NOT NULL DEFAULT 0,created_by BIGINT UNSIGNED NOT NULL,reviewed_by BIGINT UNSIGNED NULL,reviewed_at DATETIME(3) NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_course_graph_import_code(import_code),UNIQUE KEY uk_course_graph_source(course_code,source_file_hash,schema_version),UNIQUE KEY uk_course_graph_idempotency(created_by,idempotency_key),KEY idx_course_graph_status(status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS course_graph_import_issue(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,import_id BIGINT UNSIGNED NOT NULL,severity VARCHAR(16) NOT NULL,issue_code VARCHAR(64) NOT NULL,location_type VARCHAR(24) NOT NULL,location_code VARCHAR(255) NULL,message VARCHAR(1000) NOT NULL,resolved TINYINT NOT NULL DEFAULT 0,resolved_by BIGINT UNSIGNED NULL,resolved_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),KEY idx_course_graph_issue(import_id,severity,resolved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_point_legacy_mapping(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,import_id BIGINT UNSIGNED NOT NULL,legacy_knowledge_point_id BIGINT UNSIGNED NOT NULL,target_type VARCHAR(24) NOT NULL,target_external_code VARCHAR(64) NULL,mapping_type VARCHAR(32) NOT NULL,confidence DECIMAL(6,4) NOT NULL,review_status VARCHAR(24) NOT NULL,notes VARCHAR(1000) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_legacy_mapping(import_id,legacy_knowledge_point_id,target_type,target_external_code),KEY idx_legacy_mapping_point(legacy_knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS full_init_add_column;
DELIMITER $$
CREATE PROCEDURE full_init_add_column(IN t VARCHAR(64),IN c VARCHAR(64),IN d TEXT) BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN SET @x=d;PREPARE s FROM @x;EXECUTE s;DEALLOCATE PREPARE s;END IF;END$$
DELIMITER ;
CALL full_init_add_column('knowledge_graph_version','import_id','ALTER TABLE knowledge_graph_version ADD COLUMN import_id BIGINT UNSIGNED NULL');
CALL full_init_add_column('knowledge_graph_version','review_status','ALTER TABLE knowledge_graph_version ADD COLUMN review_status VARCHAR(24) NULL');
CALL full_init_add_column('knowledge_graph_version','reviewed_by','ALTER TABLE knowledge_graph_version ADD COLUMN reviewed_by BIGINT UNSIGNED NULL');
CALL full_init_add_column('knowledge_graph_version','reviewed_at','ALTER TABLE knowledge_graph_version ADD COLUMN reviewed_at DATETIME(3) NULL');
DROP PROCEDURE full_init_add_column;

INSERT INTO course(course_code,course_name,description,teacher_id,status,created_at,updated_at,is_deleted)
SELECT 'C','C语言','Stage 08 legacy bridge; completed by the reviewed course-graph import',NULL,'draft',NOW(3),NOW(3),0
WHERE NOT EXISTS(SELECT 1 FROM course WHERE course_code='C' AND is_deleted=0);
INSERT INTO course_knowledge(course_id,knowledge_point_id,sequence_no,is_core,coverage_weight,created_at)
SELECT c.id,kp.id,CASE kp.id WHEN 1 THEN 10 WHEN 2 THEN 20 WHEN 3 THEN 30 WHEN 4 THEN 40 WHEN 5 THEN 50 WHEN 6 THEN 60 ELSE 70 END,CASE WHEN kp.id BETWEEN 1 AND 6 THEN 1 ELSE 0 END,1.0000,NOW(3)
FROM course c JOIN knowledge_point kp ON kp.id IN(1,2,3,4,5,6,7) AND kp.is_deleted=0 WHERE c.course_code='C' AND c.is_deleted=0
ON DUPLICATE KEY UPDATE course_id=VALUES(course_id);

-- Stage 20-24 career, resume and recommendation baseline.
-- Keep this section aligned with the incremental migrations so a new database
-- has the same schema as an upgraded database.
DROP PROCEDURE IF EXISTS full_init_add_career_column;
DELIMITER $$
CREATE PROCEDURE full_init_add_career_column(IN t VARCHAR(64), IN c VARCHAR(64), IN d TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = t AND column_name = c
  ) THEN
    SET @sql_text = d;
    PREPARE stmt FROM @sql_text;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL full_init_add_career_column('occupation_skill','required_level_source','ALTER TABLE occupation_skill ADD COLUMN required_level_source VARCHAR(32) NULL AFTER required_level');
CALL full_init_add_career_column('occupation_skill','required_level_version','ALTER TABLE occupation_skill ADD COLUMN required_level_version VARCHAR(64) NULL AFTER required_level_source');
CALL full_init_add_career_column('occupation_skill','published_batch_code','ALTER TABLE occupation_skill ADD COLUMN published_batch_code VARCHAR(64) NULL AFTER required_level_version');
CALL full_init_add_career_column('occupation_skill','required_level_updated_at','ALTER TABLE occupation_skill ADD COLUMN required_level_updated_at DATETIME(3) NULL AFTER published_batch_code');
CALL full_init_add_career_column('student_skill_state','core_proficiency_value','ALTER TABLE student_skill_state ADD COLUMN core_proficiency_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000 AFTER proficiency_value');
CALL full_init_add_career_column('student_skill_state','knowledge_coverage_rate','ALTER TABLE student_skill_state ADD COLUMN knowledge_coverage_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000 AFTER confidence');
CALL full_init_add_career_column('student_skill_state','calculation_version','ALTER TABLE student_skill_state ADD COLUMN calculation_version VARCHAR(64) NULL AFTER evidence_count');
CALL full_init_add_career_column('student_skill_state','calculated_at','ALTER TABLE student_skill_state ADD COLUMN calculated_at DATETIME(3) NULL AFTER calculation_version');
DROP PROCEDURE full_init_add_career_column;

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
  PRIMARY KEY (id),
  UNIQUE KEY uk_career_recommendation_snapshot(snapshot_code),
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
  PRIMARY KEY (id),
  UNIQUE KEY uk_career_recommendation_course(snapshot_id,course_id),
  KEY idx_recommendation_item_snapshot(snapshot_id,rank_no),
  CONSTRAINT fk_career_recommendation_item_snapshot FOREIGN KEY(snapshot_id) REFERENCES career_course_recommendation_snapshot(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_recommendation_item_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  PRIMARY KEY(id),
  UNIQUE KEY uk_career_mapping_batch_code(batch_code),
  KEY idx_career_mapping_batch_hash(file_hash),
  CONSTRAINT fk_career_mapping_batch_operator FOREIGN KEY(created_by) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_mapping_import_row (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_id BIGINT UNSIGNED NOT NULL,
  row_no INT NOT NULL,
  occupation_label_en VARCHAR(255) NULL,
  occupation_label_zh VARCHAR(255) NULL,
  skill_relation VARCHAR(32) NULL,
  skill_title_en VARCHAR(255) NULL,
  skill_title_zh VARCHAR(255) NULL,
  course_name VARCHAR(255) NULL,
  knowledge_module VARCHAR(255) NULL,
  knowledge_point VARCHAR(1000) NULL,
  onet_knowledge VARCHAR(255) NULL,
  onet_knowledge_importance VARCHAR(64) NULL,
  mapping_type VARCHAR(32) NULL,
  confidence DECIMAL(6,4) NULL,
  evidence TEXT NULL,
  occupation_id BIGINT UNSIGNED NULL,
  skill_id BIGINT UNSIGNED NULL,
  course_id BIGINT UNSIGNED NULL,
  module_external_id VARCHAR(128) NULL,
  knowledge_point_id BIGINT UNSIGNED NULL,
  normalized_mapping_type VARCHAR(32) NULL,
  match_status VARCHAR(32) NOT NULL,
  match_reason VARCHAR(1000) NULL,
  reviewer_id BIGINT UNSIGNED NULL,
  reviewed_at DATETIME(3) NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_career_mapping_row(batch_id,row_no),
  KEY idx_career_mapping_row_status(batch_id,match_status),
  CONSTRAINT fk_career_mapping_row_batch FOREIGN KEY(batch_id) REFERENCES career_mapping_import_batch(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_mapping_row_reviewer FOREIGN KEY(reviewer_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_mapping_review_decision (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  row_id BIGINT UNSIGNED NOT NULL,
  decision VARCHAR(32) NOT NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  reason VARCHAR(1000) NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  KEY idx_career_mapping_decision_row(row_id,created_at),
  CONSTRAINT fk_career_mapping_decision_row FOREIGN KEY(row_id) REFERENCES career_mapping_import_row(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_mapping_decision_operator FOREIGN KEY(operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_resume_document (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  file_asset_id BIGINT UNSIGNED NULL,
  file_name VARCHAR(255) NOT NULL,
  file_hash VARCHAR(128) NOT NULL,
  parsed_text LONGTEXT NULL,
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PARSED',
  parser_version VARCHAR(64) NOT NULL DEFAULT 'resume_text_match_v1',
  consent_version VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  parsed_at DATETIME(3) NULL,
  error_message VARCHAR(1000) NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_resume_user_hash(user_id,file_hash),
  KEY idx_resume_user_time(user_id,created_at),
  CONSTRAINT fk_resume_document_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_resume_document_asset FOREIGN KEY(file_asset_id) REFERENCES file_asset(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_resume_evidence (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id BIGINT UNSIGNED NOT NULL,
  target_type VARCHAR(24) NOT NULL,
  target_id BIGINT UNSIGNED NULL,
  raw_name VARCHAR(255) NOT NULL,
  normalized_name VARCHAR(255) NULL,
  evidence_value DECIMAL(10,4) NULL,
  confidence DECIMAL(6,4) NOT NULL,
  evidence_text VARCHAR(2000) NULL,
  match_status VARCHAR(32) NOT NULL,
  model_version VARCHAR(64) NOT NULL,
  source_span_json JSON NULL,
  applied_status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
  applied_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_resume_evidence_key(resume_id,target_type,target_id,raw_name),
  KEY idx_resume_evidence_resume(resume_id,match_status),
  CONSTRAINT fk_resume_evidence_resume FOREIGN KEY(resume_id) REFERENCES student_resume_document(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS career_course_recommendation_acceptance (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  snapshot_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  learning_path_code VARCHAR(64) NOT NULL,
  accepted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(24) NOT NULL DEFAULT 'ACCEPTED',
  PRIMARY KEY(id),
  UNIQUE KEY uk_career_acceptance(snapshot_id,course_id),
  KEY idx_career_acceptance_user(user_id,accepted_at),
  CONSTRAINT fk_career_acceptance_snapshot FOREIGN KEY(snapshot_id) REFERENCES career_course_recommendation_snapshot(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_acceptance_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_career_acceptance_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS occupation_skill_level_analysis (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_code VARCHAR(64) NOT NULL,
  occupation_id BIGINT UNSIGNED NOT NULL,
  round_no INT NOT NULL,
  provider_key VARCHAR(64) NULL,
  model_name VARCHAR(128) NULL,
  input_json JSON NOT NULL,
  output_json JSON NULL,
  status VARCHAR(24) NOT NULL,
  error_message VARCHAR(1000) NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_occupation_level_analysis_batch_round(batch_code,round_no),
  KEY idx_occupation_level_analysis(occupation_id,batch_code,round_no),
  CONSTRAINT fk_occupation_level_analysis_occupation FOREIGN KEY(occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT,
  CONSTRAINT fk_occupation_level_analysis_user FOREIGN KEY(created_by) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_resource_recommendation (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,resource_id BIGINT UNSIGNED NOT NULL,goal_id BIGINT UNSIGNED NULL,knowledge_point_id BIGINT UNSIGNED NULL,skill_id BIGINT UNSIGNED NULL,profile_snapshot_id BIGINT UNSIGNED NULL,path_id BIGINT UNSIGNED NULL,recommendation_type VARCHAR(32) NOT NULL DEFAULT 'remedial',rank_no INT NULL,recommend_score DECIMAL(6,4) NULL,difficulty_match_score DECIMAL(6,4) NULL,reason_text VARCHAR(2000) NULL,status VARCHAR(24) NOT NULL DEFAULT 'recommended',recommended_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_student_resource_recommendation(user_id,resource_id),KEY idx_recommendation_user_status(user_id,status,rank_no),CONSTRAINT fk_recommendation_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_recommendation_resource FOREIGN KEY(resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_recommendation_point FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_recommendation_snapshot FOREIGN KEY(profile_snapshot_id) REFERENCES student_profile_snapshot(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_recommendation_path FOREIGN KEY(path_id) REFERENCES learning_path(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT ck_recommendation_scores CHECK((recommend_score IS NULL OR recommend_score BETWEEN 0 AND 1) AND (difficulty_match_score IS NULL OR difficulty_match_score BETWEEN 0 AND 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS resource_feedback (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,resource_id BIGINT UNSIGNED NOT NULL,recommendation_id BIGINT UNSIGNED NULL,feedback_type VARCHAR(32) NOT NULL,rating DECIMAL(4,2) NULL,feedback_value VARCHAR(255) NULL,feedback_text VARCHAR(2000) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),KEY idx_resource_feedback_user(user_id,created_at),KEY idx_resource_feedback_resource(resource_id,created_at),CONSTRAINT fk_resource_feedback_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_resource_feedback_resource FOREIGN KEY(resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_resource_feedback_recommendation FOREIGN KEY(recommendation_id) REFERENCES student_resource_recommendation(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT ck_resource_feedback_rating CHECK(rating IS NULL OR rating BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dialogue_session (id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,title VARCHAR(255) NULL,session_type VARCHAR(32) NOT NULL DEFAULT 'tutor',status VARCHAR(24) NOT NULL DEFAULT 'active',target_goal_id BIGINT UNSIGNED NULL,started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),last_message_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),PRIMARY KEY(id),KEY idx_dialogue_session_user_status(user_id,status,last_message_at),CONSTRAINT fk_dialogue_session_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT ck_dialogue_session_status CHECK(status IN ('active','closed','archived'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dialogue_message (id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,session_id BIGINT UNSIGNED NOT NULL,user_id BIGINT UNSIGNED NULL,role VARCHAR(16) NOT NULL,content LONGTEXT NOT NULL,reply_to_message_id BIGINT UNSIGNED NULL,llm_call_id BIGINT UNSIGNED NULL,profile_extracted TINYINT NOT NULL DEFAULT 0,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),KEY idx_dialogue_message_session(session_id,created_at),KEY idx_dialogue_message_user(user_id,created_at),CONSTRAINT fk_dialogue_message_session FOREIGN KEY(session_id) REFERENCES dialogue_session(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_dialogue_message_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_dialogue_message_reply FOREIGN KEY(reply_to_message_id) REFERENCES dialogue_message(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT fk_dialogue_message_call FOREIGN KEY(llm_call_id) REFERENCES qb_llm_call(id) ON DELETE RESTRICT ON UPDATE RESTRICT,CONSTRAINT ck_dialogue_message_role CHECK(role IN ('user','assistant','system','tool'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS migration_release_checkpoint(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,release_code VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,verification_json JSON NOT NULL,created_by BIGINT UNSIGNED NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_migration_release(release_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qb_competency_job_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_platform VARCHAR(32) NOT NULL DEFAULT 'BOSS',
  source_url VARCHAR(500) NOT NULL,
  source_job_key VARCHAR(128) NULL,
  title VARCHAR(255) NOT NULL,
  dimension VARCHAR(64) NOT NULL,
  skill VARCHAR(255) NULL,
  location VARCHAR(128) NULL,
  salary VARCHAR(128) NULL,
  experience VARCHAR(128) NULL,
  education VARCHAR(128) NULL,
  company VARCHAR(255) NULL,
  description VARCHAR(1000) NULL,
  tags_json TEXT NULL,
  source_updated_at DATETIME(3) NULL,
  last_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  availability_status VARCHAR(32) NOT NULL DEFAULT 'active',
  sync_version BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_competency_job_source_url (source_url),
  KEY idx_competency_job_dimension (dimension, availability_status, is_deleted),
  KEY idx_competency_job_source_key (source_job_key),
  KEY idx_competency_job_updated (updated_at, availability_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qb_competency_job_sync_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trigger_type VARCHAR(32) NOT NULL,
  trigger_by BIGINT UNSIGNED NULL,
  platform VARCHAR(32) NOT NULL DEFAULT 'BOSS',
  status VARCHAR(32) NOT NULL,
  keyword_count INT NOT NULL DEFAULT 0,
  city_count INT NOT NULL DEFAULT 0,
  fetched_candidate_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failure_count INT NOT NULL DEFAULT 0,
  offline_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_competency_sync_started (started_at, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

