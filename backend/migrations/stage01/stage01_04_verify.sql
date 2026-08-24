-- Stage 01 verification. Read-only except no schema/data change is performed.
USE question_bank;

SELECT 'knowledge_point_migration' AS check_name,
       (SELECT COUNT(*) FROM qb_knowledge_point) AS legacy_count,
       (SELECT COUNT(*) FROM knowledge_point) AS target_count;
SELECT 'knowledge_relation_migration' AS check_name,
       (SELECT COUNT(*) FROM qb_knowledge_relation) AS legacy_count,
       (SELECT COUNT(*) FROM knowledge_relation) AS target_count;

SELECT kp.id, kp.parent_id
FROM knowledge_point kp LEFT JOIN knowledge_point parent ON parent.id = kp.parent_id
WHERE kp.parent_id IS NOT NULL AND parent.id IS NULL;

SELECT r.id, r.source_id, r.target_id
FROM knowledge_relation r
LEFT JOIN knowledge_point source_point ON source_point.id = r.source_id
LEFT JOIN knowledge_point target_point ON target_point.id = r.target_id
WHERE source_point.id IS NULL OR target_point.id IS NULL;

SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('knowledge_point','knowledge_relation')
  AND column_name IN ('tag_id','tag_name','sort_order');

SELECT table_name, index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('occupation','occupation_alias','skill','occupation_skill','knowledge_point','skill_knowledge','knowledge_relation','data_sync_record')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;
