CREATE TABLE IF NOT EXISTS course_graph_import(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 import_code VARCHAR(64) NOT NULL,idempotency_key VARCHAR(128) NOT NULL,
 course_code VARCHAR(64) NOT NULL,course_name VARCHAR(255) NOT NULL,schema_version VARCHAR(32) NOT NULL,mode VARCHAR(32) NOT NULL,
 source_file_name VARCHAR(255) NOT NULL,source_file_hash CHAR(64) NOT NULL,normalized_hash CHAR(64) NOT NULL,validation_hash CHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL,course_id BIGINT UNSIGNED NULL,graph_version_id BIGINT UNSIGNED NULL,
 node_count INT NOT NULL DEFAULT 0,module_count INT NOT NULL DEFAULT 0,category_count INT NOT NULL DEFAULT 0,knowledge_point_count INT NOT NULL DEFAULT 0,
 contains_count INT NOT NULL DEFAULT 0,prerequisite_count INT NOT NULL DEFAULT 0,similar_count INT NOT NULL DEFAULT 0,
 error_count INT NOT NULL DEFAULT 0,warning_count INT NOT NULL DEFAULT 0,
 created_by BIGINT UNSIGNED NOT NULL,reviewed_by BIGINT UNSIGNED NULL,reviewed_at DATETIME(3) NULL,
 correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_course_graph_import_code(import_code),
 UNIQUE KEY uk_course_graph_source(course_code,source_file_hash,schema_version),
 UNIQUE KEY uk_course_graph_idempotency(created_by,idempotency_key),
 KEY idx_course_graph_status(status,created_at),
 CONSTRAINT ck_course_graph_import_status CHECK(status IN('VALIDATED','REJECTED','IMPORTED','APPROVED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_graph_import_issue(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,import_id BIGINT UNSIGNED NOT NULL,
 severity VARCHAR(16) NOT NULL,issue_code VARCHAR(64) NOT NULL,location_type VARCHAR(24) NOT NULL,location_code VARCHAR(255) NULL,message VARCHAR(1000) NOT NULL,
 resolved TINYINT NOT NULL DEFAULT 0,resolved_by BIGINT UNSIGNED NULL,resolved_at DATETIME(3) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),KEY idx_course_graph_issue(import_id,severity,resolved),
 CONSTRAINT fk_course_graph_issue_import FOREIGN KEY(import_id) REFERENCES course_graph_import(id) ON DELETE RESTRICT,
 CONSTRAINT ck_course_graph_issue_severity CHECK(severity IN('ERROR','WARNING')),
 CONSTRAINT ck_course_graph_issue_location CHECK(location_type IN('META','NODE','EDGE','RESOURCE','LEGACY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
