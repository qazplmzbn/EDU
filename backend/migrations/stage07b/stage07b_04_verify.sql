-- Stage 07B verification. Read-only.
USE question_bank;
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('file_asset','source_document','source_chunk','knowledge_source','resource_source') ORDER BY table_name;
SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name IN ('file_asset','source_document','source_chunk','knowledge_source','resource_source') ORDER BY table_name,constraint_name;
SELECT document_id,chunk_index,COUNT(*) duplicate_count FROM source_chunk GROUP BY document_id,chunk_index HAVING COUNT(*)>1;
SELECT knowledge_point_id,source_chunk_id,COUNT(*) duplicate_count FROM knowledge_source GROUP BY knowledge_point_id,source_chunk_id HAVING COUNT(*)>1;
SELECT resource_id,source_chunk_id,COUNT(*) duplicate_count FROM resource_source GROUP BY resource_id,source_chunk_id HAVING COUNT(*)>1;
