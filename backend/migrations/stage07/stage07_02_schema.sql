-- Stage 07 schema. Re-runnable; execute manually only after precheck.
USE question_bank;

CREATE TABLE IF NOT EXISTS course (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_code VARCHAR(64) NULL,
  course_name VARCHAR(200) NOT NULL,
  description VARCHAR(2000) NULL,
  teacher_id BIGINT UNSIGNED NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'active',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY(id),
  UNIQUE KEY uk_course_code(course_code),
  KEY idx_course_teacher_status(teacher_id,status,is_deleted),
  CONSTRAINT fk_course_teacher FOREIGN KEY(teacher_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_course_status CHECK(status IN ('draft','active','archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_knowledge (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  sequence_no INT NOT NULL DEFAULT 0,
  is_core TINYINT NOT NULL DEFAULT 0,
  coverage_weight DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_course_knowledge_point(course_id,knowledge_point_id),
  UNIQUE KEY uk_course_knowledge_sequence(course_id,sequence_no),
  KEY idx_course_knowledge_point(knowledge_point_id),
  CONSTRAINT fk_course_knowledge_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_course_knowledge_point FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_course_knowledge_sequence CHECK(sequence_no >= 0),
  CONSTRAINT ck_course_knowledge_weight CHECK(coverage_weight >= 0 AND coverage_weight <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_course_progress (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  progress_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
  completed_knowledge_count INT NOT NULL DEFAULT 0,
  total_knowledge_count INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL DEFAULT 'not_started',
  last_learning_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_student_course_progress(user_id,course_id),
  KEY idx_student_course_progress_course(course_id,status,updated_at),
  CONSTRAINT fk_student_course_progress_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_student_course_progress_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_student_course_progress_rate CHECK(progress_rate >= 0 AND progress_rate <= 1),
  CONSTRAINT ck_student_course_progress_counts CHECK(completed_knowledge_count >= 0 AND total_knowledge_count >= 0 AND completed_knowledge_count <= total_knowledge_count),
  CONSTRAINT ck_student_course_progress_status CHECK(status IN ('not_started','in_progress','completed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_path (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NULL,
  goal_id BIGINT UNSIGNED NULL,
  target_occupation_id BIGINT UNSIGNED NULL,
  profile_snapshot_id BIGINT UNSIGNED NULL,
  title VARCHAR(255) NOT NULL,
  stage VARCHAR(32) NULL,
  planning_days INT NOT NULL DEFAULT 14,
  version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  status VARCHAR(24) NOT NULL DEFAULT 'active',
  summary_text VARCHAR(2000) NULL,
  generated_by_agent_task_id BIGINT UNSIGNED NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY(id),
  KEY idx_learning_path_user_course_status(user_id,course_id,status,is_deleted),
  KEY idx_learning_path_course_status(course_id,status,updated_at),
  CONSTRAINT fk_learning_path_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_profile_snapshot FOREIGN KEY(profile_snapshot_id) REFERENCES student_profile_snapshot(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_learning_path_days CHECK(planning_days >= 1 AND planning_days <= 365),
  CONSTRAINT ck_learning_path_status CHECK(status IN ('draft','active','completed','obsolete'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS learning_path_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  path_id BIGINT UNSIGNED NOT NULL,
  order_no INT NOT NULL DEFAULT 0,
  item_type VARCHAR(24) NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NULL,
  resource_id BIGINT UNSIGNED NULL,
  question_id BIGINT UNSIGNED NULL,
  assignment_id BIGINT UNSIGNED NULL,
  planned_start_at DATETIME(3) NULL,
  planned_end_at DATETIME(3) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'pending',
  decision_reason VARCHAR(2000) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_learning_path_item_order(path_id,order_no),
  KEY idx_learning_path_item_knowledge(knowledge_point_id,status),
  KEY idx_learning_path_item_resource(resource_id,status),
  CONSTRAINT fk_learning_path_item_path FOREIGN KEY(path_id) REFERENCES learning_path(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_item_knowledge FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_item_resource FOREIGN KEY(resource_id) REFERENCES qb_learning_resource(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_item_question FOREIGN KEY(question_id) REFERENCES qb_question(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_path_item_assignment FOREIGN KEY(assignment_id) REFERENCES qb_assignment(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_learning_path_item_order CHECK(order_no >= 0),
  CONSTRAINT ck_learning_path_item_status CHECK(status IN ('pending','in_progress','completed','skipped')),
  CONSTRAINT ck_learning_path_item_target CHECK(
    (item_type='knowledge' AND knowledge_point_id IS NOT NULL AND resource_id IS NULL AND question_id IS NULL AND assignment_id IS NULL)
    OR (item_type='resource' AND resource_id IS NOT NULL)
    OR (item_type='question' AND question_id IS NOT NULL)
    OR (item_type='assessment' AND assignment_id IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
