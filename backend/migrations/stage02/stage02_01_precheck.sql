-- Stage 02 precheck. Read-only; execute manually before any Stage 02 schema change.
USE question_bank;

SELECT t.id AS tag_id, t.tag_name, legacy.id AS legacy_knowledge_point_id, target.id AS knowledge_point_id,
       CASE WHEN target.id IS NULL THEN 'UNMAPPED' ELSE 'MAPPED' END AS mapping_status
FROM qb_tag t
LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id = t.id AND legacy.is_deleted = 0
LEFT JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
WHERE t.is_deleted = 0
ORDER BY t.id;

SELECT 'unmapped_tag_count' AS check_name, COUNT(*) AS result
FROM qb_tag t
LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id = t.id AND legacy.is_deleted = 0
LEFT JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
WHERE t.is_deleted = 0 AND target.id IS NULL;

SELECT 'qb_question_tag' AS table_name, COUNT(*) AS row_count FROM qb_question_tag
UNION ALL SELECT 'qb_tag_mastery', COUNT(*) FROM qb_tag_mastery
UNION ALL SELECT 'qb_learning_resource', COUNT(*) FROM qb_learning_resource
UNION ALL SELECT 'qb_attempt_question', COUNT(*) FROM qb_attempt_question;

SELECT r.id AS resource_id, r.knowledge_point_id, r.tag_id
FROM qb_learning_resource r
LEFT JOIN knowledge_point direct_point ON direct_point.id = r.knowledge_point_id AND direct_point.is_deleted = 0
LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id = r.tag_id AND legacy.is_deleted = 0
LEFT JOIN knowledge_point mapped_point ON mapped_point.id = legacy.id AND mapped_point.is_deleted = 0
WHERE r.is_deleted = 0
  AND (r.knowledge_point_id IS NOT NULL OR r.tag_id IS NOT NULL)
  AND COALESCE(direct_point.id, mapped_point.id) IS NULL;

SELECT id, tag_ids_json
FROM qb_attempt_question
WHERE tag_ids_json IS NOT NULL
  AND tag_ids_json <> ''
  AND JSON_VALID(tag_ids_json) = 0;
