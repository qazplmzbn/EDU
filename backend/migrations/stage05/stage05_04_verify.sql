-- Stage 05 verification. Read-only.
USE question_bank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN ('student_basic_profile','student_learning_goal','student_evidence','student_skill_state','ability_dimension','student_ability_state','student_learning_preference','student_profile_category_stat','student_profile_summary','student_profile_snapshot','stage_evaluation')
ORDER BY table_name;

SELECT dimension_code,dimension_name,score_min,score_max,status FROM ability_dimension WHERE version='v1' ORDER BY dimension_code;

SELECT COUNT(*) AS invalid_evidence_rows FROM student_evidence WHERE evidence_direction NOT IN(-1,0,1) OR confidence<0 OR confidence>1;
SELECT user_id,source_entity_type,source_entity_id,target_type,target_id,extract_version,COUNT(*) AS duplicate_count
FROM student_evidence GROUP BY user_id,source_entity_type,source_entity_id,target_type,target_id,extract_version HAVING COUNT(*)>1;
SELECT s.user_id AS orphan_profile_summary_user FROM student_profile_summary s LEFT JOIN sys_user u ON u.id=s.user_id WHERE u.id IS NULL;
SELECT e.id AS orphan_stage_snapshot FROM stage_evaluation e LEFT JOIN student_profile_snapshot s ON s.id=e.profile_snapshot_id WHERE e.profile_snapshot_id IS NOT NULL AND s.id IS NULL;
SET @stage05_has_legacy_ability := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_user_ability');
SET @stage05_legacy_verify_sql := IF(@stage05_has_legacy_ability=1,
  'SELECT COUNT(*) AS legacy_ability_rows, (SELECT COUNT(*) FROM student_ability_state) AS migrated_ability_rows FROM qb_user_ability',
  'SELECT 0 AS legacy_ability_rows, (SELECT COUNT(*) FROM student_ability_state) AS migrated_ability_rows');
PREPARE stage05_legacy_verify FROM @stage05_legacy_verify_sql; EXECUTE stage05_legacy_verify; DEALLOCATE PREPARE stage05_legacy_verify;
SELECT table_name,index_name,non_unique,column_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name IN('student_evidence','student_ability_state','stage_evaluation','student_profile_snapshot') ORDER BY table_name,index_name,seq_in_index;
SELECT constraint_name,constraint_type,table_name FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name IN('student_evidence','student_ability_state','stage_evaluation') ORDER BY table_name,constraint_name;
