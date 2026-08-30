-- Stage 17 precheck. Any non-zero count blocks schema execution.
SELECT 'stage16_checkpoint_missing' check_name,CASE WHEN COUNT(*)=1 THEN 0 ELSE 1 END problem_count
FROM migration_release_checkpoint
WHERE release_code='personalized_loop_stage08_16' AND status='READY_FOR_VERIFY';
SELECT 'source_tables_missing' check_name,
       5-COUNT(*) problem_count
FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN('file_asset','source_document','source_chunk','knowledge_relation_source','knowledge_graph_version');
SELECT 'stage17_partial_schema' check_name,CASE WHEN COUNT(*) IN(0,2) THEN 0 ELSE 1 END problem_count
FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name IN('course_graph_import','course_graph_import_issue');
