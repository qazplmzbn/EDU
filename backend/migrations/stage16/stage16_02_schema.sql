CREATE TABLE IF NOT EXISTS migration_release_checkpoint(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,release_code VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,verification_json JSON NOT NULL,created_by BIGINT UNSIGNED NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_migration_release(release_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP PROCEDURE IF EXISTS stage16_add_index;
DELIMITER $$
CREATE PROCEDURE stage16_add_index(IN t VARCHAR(64),IN i VARCHAR(64),IN d TEXT) BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=t AND index_name=i)THEN SET @x=d;PREPARE s FROM @x;EXECUTE s;DEALLOCATE PREPARE s;END IF;END$$
DELIMITER ;
CALL stage16_add_index('resource_interaction','idx_resource_interaction_trace','CREATE INDEX idx_resource_interaction_trace ON resource_interaction(correlation_id,created_at)');
CALL stage16_add_index('resource_bundle','idx_resource_bundle_trace','CREATE INDEX idx_resource_bundle_trace ON resource_bundle(correlation_id,created_at)');
CALL stage16_add_index('learning_path_version','idx_path_version_trace','CREATE INDEX idx_path_version_trace ON learning_path_version(correlation_id,created_at)');
DROP PROCEDURE stage16_add_index;
