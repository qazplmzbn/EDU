CREATE TABLE IF NOT EXISTS occupation_skill_level_analysis (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, batch_code VARCHAR(64) NOT NULL, occupation_id BIGINT UNSIGNED NOT NULL, round_no INT NOT NULL,
 provider_key VARCHAR(64) NULL, model_name VARCHAR(128) NULL, input_json JSON NOT NULL, output_json JSON NULL, status VARCHAR(24) NOT NULL,
 error_message VARCHAR(1000) NULL, created_by BIGINT UNSIGNED NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), KEY idx_occupation_level_analysis(occupation_id,batch_code,round_no),
 CONSTRAINT fk_occupation_level_analysis_occupation FOREIGN KEY(occupation_id) REFERENCES occupation(id) ON DELETE RESTRICT,
 CONSTRAINT fk_occupation_level_analysis_user FOREIGN KEY(created_by) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
