-- Stage 01 precheck. Read-only; execute manually before schema changes.
USE question_bank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('occupation','occupation_alias','skill','occupation_skill',
                     'knowledge_point','skill_knowledge','knowledge_relation','data_sync_record')
ORDER BY table_name;

SELECT 'qb_knowledge_point' AS table_name, COUNT(*) AS row_count FROM qb_knowledge_point
UNION ALL SELECT 'qb_knowledge_relation', COUNT(*) FROM qb_knowledge_relation;

SELECT code, COUNT(*) AS duplicate_count
FROM qb_knowledge_point
WHERE code IS NOT NULL AND code <> ''
GROUP BY code HAVING COUNT(*) > 1;

SELECT child.id, child.parent_id
FROM qb_knowledge_point child
LEFT JOIN qb_knowledge_point parent ON parent.id = child.parent_id
WHERE child.parent_id IS NOT NULL AND parent.id IS NULL;

SELECT id, source_id, target_id, relation_type, weight, confidence
FROM qb_knowledge_relation
WHERE source_id = target_id
   OR weight NOT BETWEEN 0 AND 1
   OR confidence NOT BETWEEN 0 AND 1;

SELECT source_id, target_id, relation_type, COUNT(*) AS duplicate_count
FROM qb_knowledge_relation
GROUP BY source_id, target_id, relation_type HAVING COUNT(*) > 1;

SELECT r.id, r.source_id, r.target_id
FROM qb_knowledge_relation r
LEFT JOIN qb_knowledge_point source_point ON source_point.id = r.source_id
LEFT JOIN qb_knowledge_point target_point ON target_point.id = r.target_id
WHERE source_point.id IS NULL OR target_point.id IS NULL;
