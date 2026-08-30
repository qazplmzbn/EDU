SELECT 'path_code_missing' check_name,COUNT(*) problem_count FROM learning_path WHERE is_deleted=0 AND path_code IS NULL;
SELECT 'multiple_active_path_versions' check_name,COUNT(*) problem_count FROM (SELECT path_id FROM learning_path_version WHERE status='ACTIVE' GROUP BY path_id HAVING COUNT(*)>1)x;
SELECT 'new_path_non_knowledge_item' check_name,COUNT(*) problem_count FROM learning_path_item WHERE path_version_id IS NOT NULL AND knowledge_point_id IS NULL;
SELECT 'duplicate_version_step' check_name,COUNT(*) problem_count FROM (SELECT path_version_id,knowledge_point_id FROM learning_path_item WHERE path_version_id IS NOT NULL GROUP BY path_version_id,knowledge_point_id HAVING COUNT(*)>1)x;
