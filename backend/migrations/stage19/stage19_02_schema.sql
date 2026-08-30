DROP PROCEDURE IF EXISTS stage19_add_index;
DELIMITER $$
CREATE PROCEDURE stage19_add_index(IN t VARCHAR(64),IN i VARCHAR(64),IN d TEXT) BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=t AND index_name=i) THEN SET @x=d;PREPARE s FROM @x;EXECUTE s;DEALLOCATE PREPARE s;END IF;END$$
DELIMITER ;
CALL stage19_add_index('knowledge_graph_version','idx_graph_import_review','CREATE INDEX idx_graph_import_review ON knowledge_graph_version(import_id,review_status,status)');
CALL stage19_add_index('knowledge_point','idx_knowledge_course_active','CREATE INDEX idx_knowledge_course_active ON knowledge_point(course_id,status,is_deleted,chapter_id)');
CALL stage19_add_index('source_document','idx_source_import_review','CREATE INDEX idx_source_import_review ON source_document(import_code,review_status,course_id)');
DROP PROCEDURE stage19_add_index;
