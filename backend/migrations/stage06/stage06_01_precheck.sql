-- Stage 06 precheck (read only). Run manually in MySQL before stopping application writes.
USE question_bank;

SELECT VERSION() AS mysql_version, DATABASE() AS current_database;
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema=DATABASE()
  AND table_name IN ('models','prompt_templates','qb_llm_provider','qb_user_llm_provider','qb_prompt_template','qb_user_prompt_template','qb_llm_call','model_config','prompt_template','agent_definition','agent_task','agent_step','agent_review','agent_decision')
ORDER BY table_name;

SELECT table_name,column_name,column_type,is_nullable,column_key
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('models','prompt_templates','qb_llm_provider','qb_user_llm_provider','qb_prompt_template','qb_user_prompt_template','qb_llm_call','model_config','prompt_template','agent_definition','agent_task','agent_step','agent_review','agent_decision')
ORDER BY table_name,ordinal_position;

SET @stage06_has_models := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='models');
SET @stage06_plain_key_sql := IF(@stage06_has_models=1,
  'SELECT COUNT(*) AS legacy_plain_api_key_rows FROM models WHERE api_key IS NOT NULL AND TRIM(api_key)<>''''',
  'SELECT 0 AS legacy_plain_api_key_rows');
PREPARE stage06_plain_key_check FROM @stage06_plain_key_sql; EXECUTE stage06_plain_key_check; DEALLOCATE PREPARE stage06_plain_key_check;

SET @stage06_legacy_cipher_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_llm_provider')=1,
  'SELECT ''qb_llm_provider'' AS source_table,COUNT(*) AS rows_with_cipher FROM qb_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'''' UNION ALL SELECT ''qb_user_llm_provider'',COUNT(*) FROM qb_user_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>''''',
  'SELECT ''no legacy provider tables'' AS source_table,0 AS rows_with_cipher');
PREPARE stage06_legacy_cipher_check FROM @stage06_legacy_cipher_sql; EXECUTE stage06_legacy_cipher_check; DEALLOCATE PREPARE stage06_legacy_cipher_check;

SET @stage06_legacy_plain_cipher_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_llm_provider')=1,
  'SELECT ''qb_llm_provider'' AS source_table,COUNT(*) AS suspicious_plaintext_rows FROM qb_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'''' AND api_key_cipher NOT LIKE ''v1:%'' AND api_key_cipher NOT LIKE ''v2:%'' UNION ALL SELECT ''qb_user_llm_provider'',COUNT(*) FROM qb_user_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'''' AND api_key_cipher NOT LIKE ''v1:%'' AND api_key_cipher NOT LIKE ''v2:%''',
  'SELECT ''no legacy provider tables'' AS source_table,0 AS suspicious_plaintext_rows');
PREPARE stage06_legacy_plain_cipher_check FROM @stage06_legacy_plain_cipher_sql; EXECUTE stage06_legacy_plain_cipher_check; DEALLOCATE PREPARE stage06_legacy_plain_cipher_check;

SELECT table_name,index_name,non_unique,seq_in_index,column_name
FROM information_schema.statistics
WHERE table_schema=DATABASE()
  AND table_name IN ('qb_llm_call','model_config','prompt_template','agent_definition','agent_task','agent_step','agent_review','agent_decision')
ORDER BY table_name,index_name,seq_in_index;

SELECT table_name,constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE table_schema=DATABASE()
  AND table_name IN ('qb_llm_call','model_config','prompt_template','agent_definition','agent_task','agent_step','agent_review','agent_decision')
ORDER BY table_name,constraint_name;

SELECT table_name,referenced_table_name,constraint_name
FROM information_schema.key_column_usage
WHERE table_schema=DATABASE()
  AND (table_name IN ('models','prompt_templates','qb_llm_provider','qb_user_llm_provider','qb_prompt_template','qb_user_prompt_template')
       OR referenced_table_name IN ('models','prompt_templates','qb_llm_provider','qb_user_llm_provider','qb_prompt_template','qb_user_prompt_template'));

SELECT table_name
FROM information_schema.views
WHERE table_schema=DATABASE()
  AND LOWER(view_definition) REGEXP 'models|prompt_templates|qb_llm_provider|qb_user_llm_provider|qb_prompt_template|qb_user_prompt_template';
