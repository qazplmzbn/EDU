-- Stage 06 data migration. Execute only after schema and after setting APP_LLM_ENCRYPTION_KEY for the backend.
USE question_bank;

SET @stage06_plain_model_keys := 0;
SET @stage06_has_models := (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='models');
SET @stage06_plain_count_sql := IF(@stage06_has_models=1, 'SELECT COUNT(*) INTO @stage06_plain_model_keys FROM models WHERE api_key IS NOT NULL AND TRIM(api_key)<>''''', 'SET @stage06_plain_model_keys=0');
PREPARE stage06_count_plain FROM @stage06_plain_count_sql; EXECUTE stage06_count_plain; DEALLOCATE PREPARE stage06_count_plain;
SET @stage06_guard_sql := IF(@stage06_plain_model_keys=0, 'SELECT ''stage06 migration guard passed'' AS result', 'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT=''stage06 blocked: legacy models.api_key contains plaintext; re-save those keys through the encrypted API after schema migration''');
PREPARE stage06_guard FROM @stage06_guard_sql; EXECUTE stage06_guard; DEALLOCATE PREPARE stage06_guard;

SET @stage06_legacy_plain_cipher := 0;
SET @stage06_legacy_plain_cipher_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_llm_provider')=1,
  'SELECT COUNT(*) INTO @stage06_legacy_plain_cipher FROM (SELECT api_key_cipher FROM qb_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'''' AND api_key_cipher NOT LIKE ''v1:%'' AND api_key_cipher NOT LIKE ''v2:%'' UNION ALL SELECT api_key_cipher FROM qb_user_llm_provider WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'''' AND api_key_cipher NOT LIKE ''v1:%'' AND api_key_cipher NOT LIKE ''v2:%'') x',
  'SET @stage06_legacy_plain_cipher=0');
PREPARE stage06_count_legacy_plain FROM @stage06_legacy_plain_cipher_sql; EXECUTE stage06_count_legacy_plain; DEALLOCATE PREPARE stage06_count_legacy_plain;
SET @stage06_guard_sql := IF(@stage06_legacy_plain_cipher=0, 'SELECT ''stage06 legacy cipher guard passed'' AS result', 'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT=''stage06 blocked: a legacy api_key_cipher value is plaintext or has an unknown format''');
PREPARE stage06_legacy_cipher_guard FROM @stage06_guard_sql; EXECUTE stage06_legacy_cipher_guard; DEALLOCATE PREPARE stage06_legacy_cipher_guard;

SET @stage06_copy_models_sql := IF(@stage06_has_models=1,
'INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) SELECT IF(owner_user_id IS NULL OR owner_user_id=0,''system'',''user''),IF(owner_user_id IS NULL OR owner_user_id=0,0,owner_user_id),model_code,model_name,UPPER(IF(model_type=''ollama'',''LOCAL'',''API'')),COALESCE(NULLIF(api_base_url,''''),NULLIF(ollama_base_url,''''),''http://localhost:11434''),NULL,COALESCE(NULLIF(ollama_model_name,''''),model_code),temperature,IF(is_available=1,1,0),IF(is_default=1,1,0),updated_at FROM models ON DUPLICATE KEY UPDATE label=VALUES(label),provider_type=VALUES(provider_type),base_url=VALUES(base_url),model=VALUES(model),temperature=VALUES(temperature),enabled=VALUES(enabled),is_default=VALUES(is_default),updated_at=VALUES(updated_at)',
'SELECT ''no models table to migrate''');
PREPARE stage06_copy_models FROM @stage06_copy_models_sql; EXECUTE stage06_copy_models; DEALLOCATE PREPARE stage06_copy_models;

SET @stage06_copy_system_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_llm_provider')=1,
'INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) SELECT ''system'',0,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at FROM qb_llm_provider WHERE is_deleted=0 ON DUPLICATE KEY UPDATE label=VALUES(label),provider_type=VALUES(provider_type),base_url=VALUES(base_url),api_key_cipher=VALUES(api_key_cipher),model=VALUES(model),temperature=VALUES(temperature),enabled=VALUES(enabled),is_default=VALUES(is_default),updated_at=VALUES(updated_at)', 'SELECT ''no qb_llm_provider to migrate''');
PREPARE stage06_copy_system FROM @stage06_copy_system_sql; EXECUTE stage06_copy_system; DEALLOCATE PREPARE stage06_copy_system;

SET @stage06_copy_user_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_user_llm_provider')=1,
'INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) SELECT ''user'',user_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at FROM qb_user_llm_provider WHERE is_deleted=0 ON DUPLICATE KEY UPDATE label=VALUES(label),provider_type=VALUES(provider_type),base_url=VALUES(base_url),api_key_cipher=VALUES(api_key_cipher),model=VALUES(model),temperature=VALUES(temperature),enabled=VALUES(enabled),is_default=VALUES(is_default),updated_at=VALUES(updated_at)', 'SELECT ''no qb_user_llm_provider to migrate''');
PREPARE stage06_copy_user FROM @stage06_copy_user_sql; EXECUTE stage06_copy_user; DEALLOCATE PREPARE stage06_copy_user;

SET @stage06_copy_system_prompt_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_prompt_template')=1,
'INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) SELECT ''system'',0,template_name,task_type,description,prompt_text,''v1'',created_at,updated_at FROM qb_prompt_template WHERE is_deleted=0 ON DUPLICATE KEY UPDATE description=VALUES(description),prompt_text=VALUES(prompt_text),updated_at=VALUES(updated_at)', 'SELECT ''no qb_prompt_template to migrate''');
PREPARE stage06_copy_system_prompt FROM @stage06_copy_system_prompt_sql; EXECUTE stage06_copy_system_prompt; DEALLOCATE PREPARE stage06_copy_system_prompt;

SET @stage06_copy_user_prompt_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='qb_user_prompt_template')=1,
'INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) SELECT ''user'',user_id,template_name,task_type,description,prompt_text,''v1'',created_at,updated_at FROM qb_user_prompt_template WHERE is_deleted=0 ON DUPLICATE KEY UPDATE description=VALUES(description),prompt_text=VALUES(prompt_text),updated_at=VALUES(updated_at)', 'SELECT ''no qb_user_prompt_template to migrate''');
PREPARE stage06_copy_user_prompt FROM @stage06_copy_user_prompt_sql; EXECUTE stage06_copy_user_prompt; DEALLOCATE PREPARE stage06_copy_user_prompt;

SET @stage06_copy_prompt_models_sql := IF((SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='prompt_templates')=1,
'INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) SELECT IF(owner_user_id IS NULL OR owner_user_id=0,''system'',''user''),IF(owner_user_id IS NULL OR owner_user_id=0,0,owner_user_id),name,task_type,description,prompt_content,''legacy'',created_at,updated_at FROM prompt_templates ON DUPLICATE KEY UPDATE description=VALUES(description),prompt_text=VALUES(prompt_text),updated_at=VALUES(updated_at)',
'SELECT ''no prompt_templates table to migrate''');
PREPARE stage06_copy_prompt_models FROM @stage06_copy_prompt_models_sql; EXECUTE stage06_copy_prompt_models; DEALLOCATE PREPARE stage06_copy_prompt_models;

INSERT INTO agent_definition(agent_code,agent_name,role_type,description,status,version)
VALUES ('PROFILE','画像智能体','profile','读取持久化画像摘要，不修改画像',1,'v1'),('DIAGNOSIS','诊断智能体','diagnosis','分析知识点、能力与行为证据',1,'v1'),('PLANNER','规划智能体','plan','形成资源与教学行动建议',1,'v1'),('GENERATOR','资源生成智能体','generate','生成结构化学习资源草案',1,'v1'),('REVIEWER','资源审核智能体','review','审核事实、覆盖、难度与一致性',1,'v1')
ON DUPLICATE KEY UPDATE agent_name=VALUES(agent_name),role_type=VALUES(role_type),description=VALUES(description),status=VALUES(status),updated_at=NOW(3);

SELECT 'OK: Stage 06 migration completed. After starting the backend with APP_LLM_ENCRYPTION_KEY, an administrator must explicitly call POST /api/admin/llm/providers/reencrypt-legacy-keys before final verification.' AS result;
