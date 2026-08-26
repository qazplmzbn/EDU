-- Stage 06 physical cleanup. Blocked by default; run manually only after release confirmation and a restorable backup.
USE question_bank;
SET @stage06_cleanup_confirmed = COALESCE(@stage06_cleanup_confirmed,0);
SET @stage06_legacy_dependency_count := (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema=DATABASE() AND referenced_table_name IN ('models','prompt_templates','qb_llm_provider','qb_user_llm_provider','qb_prompt_template','qb_user_prompt_template')
) + (
  SELECT COUNT(*) FROM information_schema.views
  WHERE table_schema=DATABASE() AND LOWER(view_definition) REGEXP 'models|prompt_templates|qb_llm_provider|qb_user_llm_provider|qb_prompt_template|qb_user_prompt_template'
);
SET @stage06_cleanup_sql = IF(@stage06_cleanup_confirmed=1 AND @stage06_legacy_dependency_count=0,'SELECT ''stage06 cleanup confirmed'' AS result','SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT=''stage06 cleanup blocked: confirmation missing or legacy database dependency remains''');
PREPARE stage06_cleanup_guard FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup_guard; DEALLOCATE PREPARE stage06_cleanup_guard;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS qb_user_prompt_template'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS qb_prompt_template'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS qb_user_llm_provider'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS qb_llm_provider'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS prompt_templates'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
SET @stage06_cleanup_sql='DROP TABLE IF EXISTS models'; PREPARE stage06_cleanup FROM @stage06_cleanup_sql; EXECUTE stage06_cleanup; DEALLOCATE PREPARE stage06_cleanup;
