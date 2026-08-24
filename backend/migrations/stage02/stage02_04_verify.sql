-- Stage 02 verification. Read-only.
USE question_bank;

SELECT 'unmapped_tag_count' AS check_name, COUNT(*) AS result
FROM qb_tag t
LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id = t.id AND legacy.is_deleted = 0
LEFT JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
WHERE t.is_deleted = 0 AND target.id IS NULL;

SELECT 'question_relation_source' AS check_name, COUNT(*) AS legacy_count FROM qb_question_tag
UNION ALL SELECT 'question_relation_target', COUNT(*) FROM question_knowledge
UNION ALL SELECT 'tag_mastery_source', COUNT(*) FROM qb_tag_mastery
UNION ALL SELECT 'student_knowledge_target', COUNT(*) FROM student_knowledge_state
UNION ALL SELECT 'resource_knowledge_target', COUNT(*) FROM resource_knowledge
UNION ALL SELECT 'attempt_snapshot_total', COUNT(*) FROM qb_attempt_question
UNION ALL SELECT 'attempt_snapshot_migrated', COUNT(*) FROM qb_attempt_question WHERE knowledge_snapshot_json IS NOT NULL;

SELECT qk.id, qk.question_id, qk.knowledge_point_id
FROM question_knowledge qk LEFT JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id
WHERE kp.id IS NULL;

SELECT rk.id, rk.resource_id, rk.knowledge_point_id
FROM resource_knowledge rk LEFT JOIN knowledge_point kp ON kp.id=rk.knowledge_point_id
WHERE kp.id IS NULL;

SELECT sk.id, sk.user_id, sk.knowledge_point_id
FROM student_knowledge_state sk LEFT JOIN knowledge_point kp ON kp.id=sk.knowledge_point_id
WHERE kp.id IS NULL;
