CREATE TABLE IF NOT EXISTS student_resource_preference(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,resource_type VARCHAR(32) NOT NULL,score DECIMAL(6,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_resource_preference(user_id,course_id,resource_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_cognitive_state(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,cognitive_level VARCHAR(24) NOT NULL,score DECIMAL(6,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_cognitive_state(user_id,course_id,cognitive_level)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS student_behavior_metric(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,metric_group VARCHAR(32) NOT NULL,metric_code VARCHAR(64) NOT NULL,value DECIMAL(10,4) NOT NULL,confidence DECIMAL(6,4) NOT NULL,evidence_count INT NOT NULL DEFAULT 0,calculated_at DATETIME(3) NOT NULL,algorithm_version VARCHAR(64) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_behavior_metric(user_id,course_id,metric_group,metric_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_state_update_log(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,user_id BIGINT UNSIGNED NOT NULL,course_id BIGINT UNSIGNED NOT NULL,knowledge_point_id BIGINT UNSIGNED NOT NULL,interaction_id BIGINT UNSIGNED NOT NULL,evidence_scope VARCHAR(16) NOT NULL,previous_mastery DECIMAL(6,4) NOT NULL,new_mastery DECIMAL(6,4) NOT NULL,previous_confidence DECIMAL(6,4) NOT NULL,new_confidence DECIMAL(6,4) NOT NULL,model_version VARCHAR(64) NOT NULL,profile_version_before BIGINT UNSIGNED NOT NULL,profile_version_after BIGINT UNSIGNED NOT NULL,correlation_id VARCHAR(64) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_state_update_interaction(interaction_id,knowledge_point_id,evidence_scope)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS profile_algorithm_policy(id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,policy_version VARCHAR(64) NOT NULL,status VARCHAR(24) NOT NULL,learning_rate DECIMAL(6,4) NOT NULL,learning_practice_weight DECIMAL(6,4) NOT NULL,hidden_assessment_weight DECIMAL(6,4) NOT NULL,confidence_scale DECIMAL(8,4) NOT NULL,dimkt_enabled TINYINT NOT NULL DEFAULT 0,config_json JSON NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),PRIMARY KEY(id),UNIQUE KEY uk_profile_policy_version(policy_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP PROCEDURE IF EXISTS stage09_add_column;
DELIMITER $$
CREATE PROCEDURE stage09_add_column(IN t VARCHAR(64),IN c VARCHAR(64),IN d TEXT)
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN
    SET @x=d; PREPARE s FROM @x; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;
CALL stage09_add_column('student_knowledge_state','course_id','ALTER TABLE student_knowledge_state ADD COLUMN course_id BIGINT UNSIGNED NULL');
CALL stage09_add_column('student_knowledge_state','state_version','ALTER TABLE student_knowledge_state ADD COLUMN state_version BIGINT UNSIGNED NOT NULL DEFAULT 0');
CALL stage09_add_column('student_knowledge_state','calculation_method','ALTER TABLE student_knowledge_state ADD COLUMN calculation_method VARCHAR(64) NOT NULL DEFAULT ''LEGACY_RATIO''');
CALL stage09_add_column('student_knowledge_state','algorithm_version','ALTER TABLE student_knowledge_state ADD COLUMN algorithm_version VARCHAR(64) NOT NULL DEFAULT ''legacy_v1''');
CALL stage09_add_column('student_knowledge_state','last_interaction_seq','ALTER TABLE student_knowledge_state ADD COLUMN last_interaction_seq BIGINT UNSIGNED NOT NULL DEFAULT 0');
CALL stage09_add_column('student_knowledge_state','last_interaction_id','ALTER TABLE student_knowledge_state ADD COLUMN last_interaction_id BIGINT UNSIGNED NULL');
CALL stage09_add_column('student_profile_snapshot','course_id','ALTER TABLE student_profile_snapshot ADD COLUMN course_id BIGINT UNSIGNED NULL');
CALL stage09_add_column('student_profile_snapshot','profile_version','ALTER TABLE student_profile_snapshot ADD COLUMN profile_version BIGINT UNSIGNED NULL');
CALL stage09_add_column('student_profile_snapshot','calculated_at','ALTER TABLE student_profile_snapshot ADD COLUMN calculated_at DATETIME(3) NULL');
CALL stage09_add_column('student_profile_snapshot','algorithm_version','ALTER TABLE student_profile_snapshot ADD COLUMN algorithm_version VARCHAR(64) NULL');
CALL stage09_add_column('student_profile_snapshot','correlation_id','ALTER TABLE student_profile_snapshot ADD COLUMN correlation_id VARCHAR(64) NULL');
CALL stage09_add_column('student_profile_snapshot','resource_preference_json','ALTER TABLE student_profile_snapshot ADD COLUMN resource_preference_json JSON NULL');
CALL stage09_add_column('student_profile_snapshot','cognitive_profile_json','ALTER TABLE student_profile_snapshot ADD COLUMN cognitive_profile_json JSON NULL');
CALL stage09_add_column('student_profile_snapshot','initiative_json','ALTER TABLE student_profile_snapshot ADD COLUMN initiative_json JSON NULL');
CALL stage09_add_column('student_profile_snapshot','regularity_json','ALTER TABLE student_profile_snapshot ADD COLUMN regularity_json JSON NULL');
DROP PROCEDURE stage09_add_column;
