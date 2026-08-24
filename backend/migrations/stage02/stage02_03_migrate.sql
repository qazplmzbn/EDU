-- Stage 02 data migration. Execute only when stage02_01_precheck.sql reports zero unmapped rows.
USE question_bank;
START TRANSACTION;

INSERT INTO question_knowledge(question_id, knowledge_point_id, weight, relation_type, is_primary, confidence, source_type, created_at)
SELECT qt.question_id, target.id, 1.0000, 'assess', 0, 1.0000, 'import', qt.created_at
FROM qb_question_tag qt
JOIN qb_knowledge_point legacy ON legacy.tag_id = qt.tag_id AND legacy.is_deleted = 0
JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
ON DUPLICATE KEY UPDATE weight=VALUES(weight), confidence=VALUES(confidence), source_type=VALUES(source_type);

UPDATE question_knowledge qk
JOIN (
  SELECT question_id, MIN(id) AS primary_id FROM question_knowledge GROUP BY question_id
) first_relation ON first_relation.primary_id = qk.id
SET qk.is_primary = 1;

INSERT INTO student_knowledge_state(user_id, knowledge_point_id, mastery_value, mastery_level, confidence, evidence_count, correct_count, attempt_count, last_evidence_at, updated_at)
SELECT tm.user_id, target.id,
       LEAST(1.0000, GREATEST(0.0000, AVG(tm.mastery_value))),
       CASE WHEN AVG(tm.mastery_value) >= 0.8 THEN 'mastered' WHEN AVG(tm.mastery_value) >= 0.5 THEN 'basic' ELSE 'weak' END,
       LEAST(1.0000, SUM(tm.attempt_count) / 10.0000),
       SUM(tm.attempt_count), SUM(tm.correct_count), SUM(tm.attempt_count), MAX(tm.updated_at), MAX(tm.updated_at)
FROM qb_tag_mastery tm
JOIN qb_knowledge_point legacy ON legacy.tag_id = tm.tag_id AND legacy.is_deleted = 0
JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
GROUP BY tm.user_id, target.id
ON DUPLICATE KEY UPDATE mastery_value=VALUES(mastery_value), mastery_level=VALUES(mastery_level), confidence=VALUES(confidence), evidence_count=VALUES(evidence_count), correct_count=VALUES(correct_count), attempt_count=VALUES(attempt_count), last_evidence_at=VALUES(last_evidence_at), updated_at=VALUES(updated_at);

INSERT INTO resource_knowledge(resource_id, knowledge_point_id, relation_type, coverage_weight, is_primary, created_at)
SELECT r.id, direct_point.id, 'cover', 1.0000, 1, r.created_at
FROM qb_learning_resource r
JOIN knowledge_point direct_point ON direct_point.id = r.knowledge_point_id AND direct_point.is_deleted = 0
WHERE r.is_deleted = 0 AND r.knowledge_point_id IS NOT NULL
ON DUPLICATE KEY UPDATE coverage_weight=VALUES(coverage_weight), is_primary=VALUES(is_primary);

INSERT INTO resource_knowledge(resource_id, knowledge_point_id, relation_type, coverage_weight, is_primary, created_at)
SELECT r.id, target.id, 'cover', 1.0000, CASE WHEN r.knowledge_point_id IS NULL THEN 1 ELSE 0 END, r.created_at
FROM qb_learning_resource r
JOIN qb_knowledge_point legacy ON legacy.tag_id = r.tag_id AND legacy.is_deleted = 0
JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
WHERE r.is_deleted = 0 AND r.tag_id IS NOT NULL
ON DUPLICATE KEY UPDATE coverage_weight=VALUES(coverage_weight);

UPDATE qb_attempt_question aq
LEFT JOIN (
  SELECT aq2.id,
         JSON_ARRAYAGG(JSON_OBJECT('knowledgePointId', target.id, 'weight', 1.0000, 'isPrimary', 0, 'mappingVersion', 'stage02')) AS snapshot
  FROM qb_attempt_question aq2
  JOIN JSON_TABLE(CASE WHEN JSON_VALID(aq2.tag_ids_json) THEN aq2.tag_ids_json ELSE JSON_ARRAY() END, '$[*]' COLUMNS(tag_id BIGINT PATH '$')) jt
  JOIN qb_knowledge_point legacy ON legacy.tag_id = jt.tag_id AND legacy.is_deleted = 0
  JOIN knowledge_point target ON target.id = legacy.id AND target.is_deleted = 0
  GROUP BY aq2.id
) migrated ON migrated.id = aq.id
SET aq.knowledge_snapshot_json = COALESCE(migrated.snapshot, JSON_ARRAY());

UPDATE qb_learning_behavior SET ref_type = COALESCE(ref_type, 'unknown');
COMMIT;
