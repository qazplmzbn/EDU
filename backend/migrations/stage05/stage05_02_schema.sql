-- Stage 05 target schema. Safe to re-run; no data migration is performed here.
USE question_bank;

CREATE TABLE IF NOT EXISTS student_basic_profile (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  student_no VARCHAR(64) NULL,
  major_name VARCHAR(128) NULL,
  grade_name VARCHAR(64) NULL,
  education_level VARCHAR(32) NULL,
  learning_stage VARCHAR(40) NULL,
  weekly_available_hours DECIMAL(6,2) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_student_basic_profile_user (user_id),
  CONSTRAINT fk_student_basic_profile_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_learning_goal (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL,
  goal_type VARCHAR(24) NOT NULL DEFAULT 'occupation', target_occupation_id BIGINT UNSIGNED NULL,
  target_skill_id BIGINT UNSIGNED NULL, target_knowledge_point_id BIGINT UNSIGNED NULL,
  goal_description VARCHAR(1000) NULL, target_level DECIMAL(6,4) NULL,
  expected_completion_date DATE NULL, weekly_available_hours DECIMAL(6,2) NULL,
  priority INT NOT NULL DEFAULT 1, status VARCHAR(24) NOT NULL DEFAULT 'active',
  source_type VARCHAR(32) NOT NULL DEFAULT 'self_report', created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), KEY idx_student_goal_user_status (user_id,status,priority),
  CONSTRAINT fk_student_goal_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_evidence (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL,
  evidence_type VARCHAR(40) NOT NULL, source_entity_type VARCHAR(40) NOT NULL,
  source_entity_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(24) NOT NULL, target_id BIGINT UNSIGNED NULL,
  evidence_value DECIMAL(10,4) NULL, evidence_direction TINYINT NOT NULL DEFAULT 1,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 1.0000, evidence_text VARCHAR(2000) NULL,
  occurred_at DATETIME(3) NOT NULL, extract_version VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_evidence_source_target (user_id,source_entity_type,source_entity_id,target_type,target_id,extract_version),
  KEY idx_student_evidence_user_time (user_id,occurred_at), KEY idx_student_evidence_target (target_type,target_id),
  CONSTRAINT fk_student_evidence_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_student_evidence_direction CHECK (evidence_direction IN (-1,0,1)),
  CONSTRAINT ck_student_evidence_confidence CHECK (confidence BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_skill_state (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, skill_id BIGINT UNSIGNED NOT NULL,
  proficiency_value DECIMAL(6,4) NOT NULL DEFAULT 0.0000, proficiency_level VARCHAR(24) NULL,
  confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000, evidence_count INT NOT NULL DEFAULT 0,
  gap_to_target DECIMAL(6,4) NULL, last_evidence_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_student_skill_state (user_id,skill_id), KEY idx_student_skill_state_skill (skill_id,user_id),
  CONSTRAINT fk_student_skill_state_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_student_skill_state_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_student_skill_value CHECK (proficiency_value BETWEEN 0 AND 1),
  CONSTRAINT ck_student_skill_confidence CHECK (confidence BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ability_dimension (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, dimension_code VARCHAR(64) NOT NULL, dimension_name VARCHAR(128) NOT NULL,
  description VARCHAR(1000) NULL, score_min DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  score_max DECIMAL(10,4) NOT NULL DEFAULT 1.0000, version VARCHAR(64) NOT NULL DEFAULT 'v1',
  status TINYINT NOT NULL DEFAULT 1, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_ability_dimension_code_version(dimension_code,version),
  CONSTRAINT ck_ability_dimension_range CHECK(score_min < score_max), CONSTRAINT ck_ability_dimension_status CHECK(status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_ability_state (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, dimension_id BIGINT UNSIGNED NOT NULL,
  score DECIMAL(10,4) NOT NULL DEFAULT 0.0000, level VARCHAR(24) NULL, confidence DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  evidence_count INT NOT NULL DEFAULT 0, updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_ability_state(user_id,dimension_id), KEY idx_student_ability_dimension(dimension_id,user_id),
  CONSTRAINT fk_student_ability_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_student_ability_dimension FOREIGN KEY(dimension_id) REFERENCES ability_dimension(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_student_ability_confidence CHECK(confidence BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_learning_preference (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL,
  preference_type VARCHAR(40) NOT NULL, preference_value VARCHAR(255) NOT NULL, preference_score DECIMAL(6,4) NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'self_report', evidence_count INT NOT NULL DEFAULT 1,
  valid_from DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), valid_to DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_student_preference_active(user_id,preference_type,preference_value,valid_to),
  KEY idx_student_preference_user_type(user_id,preference_type),
  CONSTRAINT fk_student_preference_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_student_preference_score CHECK(preference_score IS NULL OR preference_score BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_summary (
  user_id BIGINT UNSIGNED NOT NULL, target_occupation_id BIGINT UNSIGNED NULL, active_goal_id BIGINT UNSIGNED NULL,
  overall_knowledge_mastery DECIMAL(6,4) NULL, core_knowledge_mastery DECIMAL(6,4) NULL, skill_match_score DECIMAL(6,4) NULL,
  ability_average_score DECIMAL(10,4) NULL, assessment_accuracy DECIMAL(6,4) NULL, course_completion_rate DECIMAL(6,4) NULL,
  learning_activity_score DECIMAL(6,4) NULL, weak_knowledge_count INT NOT NULL DEFAULT 0, weak_skill_count INT NOT NULL DEFAULT 0,
  recommended_difficulty TINYINT NULL, last_profile_snapshot_id BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(user_id), CONSTRAINT fk_student_profile_summary_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL,
  basic_state_json LONGTEXT NULL, knowledge_state_json LONGTEXT NULL, skill_state_json LONGTEXT NULL,
  ability_state_json LONGTEXT NULL, preference_state_json LONGTEXT NULL, goal_state_json LONGTEXT NULL,
  category_stat_json LONGTEXT NULL, profile_summary VARCHAR(2000) NULL, trigger_type VARCHAR(32) NOT NULL DEFAULT 'scheduled',
  trigger_id BIGINT UNSIGNED NULL, evidence_count INT NOT NULL DEFAULT 0, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), KEY idx_student_profile_snapshot_user_time(user_id,created_at),
  CONSTRAINT fk_student_profile_snapshot_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile_category_stat (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, category_type VARCHAR(32) NOT NULL,
  period_type VARCHAR(24) NOT NULL DEFAULT 'current', total_count INT NOT NULL DEFAULT 0, strong_count INT NOT NULL DEFAULT 0,
  weak_count INT NOT NULL DEFAULT 0, average_score DECIMAL(10,4) NULL, coverage_rate DECIMAL(6,4) NULL,
  top_strengths_json LONGTEXT NULL, top_weaknesses_json LONGTEXT NULL, profile_snapshot_id BIGINT UNSIGNED NULL,
  calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  UNIQUE KEY uk_student_profile_category_current(user_id,category_type,period_type), KEY idx_profile_category_snapshot(profile_snapshot_id),
  CONSTRAINT fk_profile_category_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stage_evaluation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, stage_type VARCHAR(24) NOT NULL,
  start_date DATE NOT NULL, end_date DATE NOT NULL, profile_snapshot_id BIGINT UNSIGNED NULL,
  overall_score DECIMAL(10,4) NULL, dimension_scores_json LONGTEXT NULL, evaluation_text LONGTEXT NULL,
  evaluator_type VARCHAR(24) NOT NULL DEFAULT 'system', status VARCHAR(24) NOT NULL DEFAULT 'final',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), PRIMARY KEY(id),
  KEY idx_stage_evaluation_user_stage(user_id,stage_type,start_date,end_date,created_at),
  CONSTRAINT fk_stage_evaluation_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stage_evaluation_snapshot FOREIGN KEY(profile_snapshot_id) REFERENCES student_profile_snapshot(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_stage_evaluation_dates CHECK(start_date <= end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ability_dimension(dimension_code,dimension_name,description,score_min,score_max,version,status)
VALUES
 ('ABILITY','能力水平','基于已评分作答的综合能力分数',0,100,'v1',1),
 ('MASTERY','知识掌握','学生知识点掌握度聚合',0,100,'v1',1),
 ('PERFORMANCE','作答表现','近期作答得分率',0,100,'v1',1),
 ('PARTICIPATION','学习参与','学习行为与完成作答参与度',0,100,'v1',1)
ON DUPLICATE KEY UPDATE dimension_name=VALUES(dimension_name),description=VALUES(description),status=VALUES(status),updated_at=NOW(3);
