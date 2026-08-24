-- Stage 01 cleanup is intentionally deferred.
-- DO NOT drop qb_knowledge_point or qb_knowledge_relation in this stage.
-- qb_knowledge_point.tag_id remains a Stage 02 Tag-to-knowledge mapping source.
USE question_bank;

SELECT 'DEFERRED_TO_STAGE_02' AS cleanup_status,
       'qb_knowledge_point and qb_knowledge_relation remain read-only migration sources' AS message;
