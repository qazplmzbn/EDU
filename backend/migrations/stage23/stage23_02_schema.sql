CREATE TABLE IF NOT EXISTS career_course_recommendation_acceptance (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, snapshot_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL, course_id BIGINT UNSIGNED NOT NULL,
 learning_path_code VARCHAR(64) NOT NULL, accepted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), status VARCHAR(24) NOT NULL DEFAULT 'ACCEPTED',
 PRIMARY KEY(id), UNIQUE KEY uk_career_acceptance(snapshot_id,course_id), KEY idx_career_acceptance_user(user_id,accepted_at),
 CONSTRAINT fk_career_acceptance_snapshot FOREIGN KEY(snapshot_id) REFERENCES career_course_recommendation_snapshot(id) ON DELETE RESTRICT,
 CONSTRAINT fk_career_acceptance_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
 CONSTRAINT fk_career_acceptance_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
