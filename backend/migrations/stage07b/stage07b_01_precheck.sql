-- Stage 07B precheck: trusted sources and evidence links. Read-only.
USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('file_asset','source_document','source_chunk','knowledge_source','resource_source') ORDER BY table_name;
SELECT COUNT(*) AS active_knowledge_points FROM knowledge_point WHERE is_deleted=0;
SELECT COUNT(*) AS active_resources FROM qb_learning_resource WHERE is_deleted=0;
