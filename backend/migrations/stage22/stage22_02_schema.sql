CREATE TABLE IF NOT EXISTS student_resume_document (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, file_asset_id BIGINT UNSIGNED NULL,
 file_name VARCHAR(255) NOT NULL, file_hash VARCHAR(128) NOT NULL, parsed_text LONGTEXT NULL,
 parse_status VARCHAR(32) NOT NULL DEFAULT 'PARSED', parser_version VARCHAR(64) NOT NULL DEFAULT 'resume_text_match_v1',
 consent_version VARCHAR(64) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), parsed_at DATETIME(3) NULL, error_message VARCHAR(1000) NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_resume_user_hash(user_id,file_hash), KEY idx_resume_user_time(user_id,created_at),
 CONSTRAINT fk_resume_document_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
 CONSTRAINT fk_resume_document_asset FOREIGN KEY(file_asset_id) REFERENCES file_asset(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS student_resume_evidence (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, resume_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(24) NOT NULL, target_id BIGINT UNSIGNED NULL,
 raw_name VARCHAR(255) NOT NULL, normalized_name VARCHAR(255) NULL, evidence_value DECIMAL(10,4) NULL, confidence DECIMAL(6,4) NOT NULL,
 evidence_text VARCHAR(2000) NULL, match_status VARCHAR(32) NOT NULL, model_version VARCHAR(64) NOT NULL, source_span_json JSON NULL,
 applied_status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE', applied_at DATETIME(3) NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uk_resume_evidence_key(resume_id,target_type,target_id,raw_name), KEY idx_resume_evidence_resume(resume_id,match_status),
 CONSTRAINT fk_resume_evidence_resume FOREIGN KEY(resume_id) REFERENCES student_resume_document(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
