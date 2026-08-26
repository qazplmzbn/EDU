-- Stage 06 verification (read only).
USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('model_config','prompt_template','qb_llm_call','agent_definition','agent_task','agent_step','agent_review','agent_decision') ORDER BY table_name;
SELECT owner_type,COUNT(*) AS model_count,SUM(api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'') AS encrypted_key_count FROM model_config GROUP BY owner_type;
SELECT id,owner_type,provider_key AS invalid_cipher_row FROM model_config WHERE api_key_cipher IS NOT NULL AND TRIM(api_key_cipher)<>'' AND api_key_cipher NOT LIKE 'v1:%' AND api_key_cipher NOT LIKE 'v2:%';
SELECT id,owner_type,provider_key AS legacy_v1_cipher_pending_rotation FROM model_config WHERE api_key_cipher LIKE 'v1:%';
SELECT owner_type,COUNT(*) AS template_count FROM prompt_template GROUP BY owner_type;
SELECT agent_code,version,status FROM agent_definition ORDER BY agent_code,version;
SELECT task_code,COUNT(*) AS duplicate_count FROM agent_task GROUP BY task_code HAVING task_code IS NOT NULL AND COUNT(*)>1;
SELECT agent_task_id,step_no,COUNT(*) AS duplicate_count FROM agent_step GROUP BY agent_task_id,step_no HAVING COUNT(*)>1;
SELECT s.id AS orphan_step FROM agent_step s LEFT JOIN agent_task t ON t.id=s.agent_task_id WHERE t.id IS NULL;
SELECT r.id AS orphan_review FROM agent_review r LEFT JOIN agent_task t ON t.id=r.agent_task_id WHERE t.id IS NULL;
SELECT d.id AS orphan_decision FROM agent_decision d LEFT JOIN agent_task t ON t.id=d.agent_task_id WHERE t.id IS NULL;
SELECT table_name,index_name,non_unique,column_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name IN ('model_config','prompt_template','qb_llm_call','agent_definition','agent_task','agent_step','agent_review','agent_decision') ORDER BY table_name,index_name,seq_in_index;
SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name IN ('model_config','prompt_template','agent_definition','agent_task','agent_step','agent_review','agent_decision') ORDER BY table_name,constraint_name;
