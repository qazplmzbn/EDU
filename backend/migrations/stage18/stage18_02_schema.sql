CREATE TABLE IF NOT EXISTS knowledge_point_legacy_mapping(
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,import_id BIGINT UNSIGNED NOT NULL,legacy_knowledge_point_id BIGINT UNSIGNED NOT NULL,
 target_type VARCHAR(24) NOT NULL,target_external_code VARCHAR(64) NULL,mapping_type VARCHAR(32) NOT NULL,
 confidence DECIMAL(6,4) NOT NULL,review_status VARCHAR(24) NOT NULL,notes VARCHAR(1000) NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id),UNIQUE KEY uk_legacy_mapping(import_id,legacy_knowledge_point_id,target_type,target_external_code),
 KEY idx_legacy_mapping_point(legacy_knowledge_point_id),
 CONSTRAINT fk_legacy_mapping_import FOREIGN KEY(import_id) REFERENCES course_graph_import(id) ON DELETE RESTRICT,
 CONSTRAINT fk_legacy_mapping_point FOREIGN KEY(legacy_knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE RESTRICT,
 CONSTRAINT ck_legacy_mapping_target CHECK(target_type IN('COURSE','CHAPTER','KNOWLEDGE_POINT','NONE')),
 CONSTRAINT ck_legacy_mapping_confidence CHECK(confidence BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS stage18_add_column;
DELIMITER $$
CREATE PROCEDURE stage18_add_column(IN t VARCHAR(64),IN c VARCHAR(64),IN d TEXT) BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN SET @x=d;PREPARE s FROM @x;EXECUTE s;DEALLOCATE PREPARE s;END IF;END$$
DELIMITER ;
CALL stage18_add_column('knowledge_graph_version','import_id','ALTER TABLE knowledge_graph_version ADD COLUMN import_id BIGINT UNSIGNED NULL');
CALL stage18_add_column('knowledge_graph_version','review_status','ALTER TABLE knowledge_graph_version ADD COLUMN review_status VARCHAR(24) NULL');
CALL stage18_add_column('knowledge_graph_version','reviewed_by','ALTER TABLE knowledge_graph_version ADD COLUMN reviewed_by BIGINT UNSIGNED NULL');
CALL stage18_add_column('knowledge_graph_version','reviewed_at','ALTER TABLE knowledge_graph_version ADD COLUMN reviewed_at DATETIME(3) NULL');
CALL stage18_add_column('source_document','course_id','ALTER TABLE source_document ADD COLUMN course_id BIGINT UNSIGNED NULL');
CALL stage18_add_column('source_document','source_kind','ALTER TABLE source_document ADD COLUMN source_kind VARCHAR(40) NULL');
CALL stage18_add_column('source_document','review_status','ALTER TABLE source_document ADD COLUMN review_status VARCHAR(24) NULL');
CALL stage18_add_column('source_document','import_code','ALTER TABLE source_document ADD COLUMN import_code VARCHAR(64) NULL');
DROP PROCEDURE stage18_add_column;
