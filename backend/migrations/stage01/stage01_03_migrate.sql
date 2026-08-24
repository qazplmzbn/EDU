-- Stage 01 migration. Execute only after stage01_02_schema.sql succeeds.
USE question_bank;
START TRANSACTION;

INSERT INTO knowledge_point (id, name, code, parent_id, level, knowledge_type, difficulty, description, created_at, updated_at, is_deleted)
SELECT id, name, NULLIF(code, ''), NULL, level, 'concept', 3, description, created_at, updated_at, is_deleted
FROM qb_knowledge_point
ON DUPLICATE KEY UPDATE
  name = VALUES(name), code = VALUES(code), level = VALUES(level),
  knowledge_type = VALUES(knowledge_type), difficulty = VALUES(difficulty),
  description = VALUES(description), updated_at = VALUES(updated_at), is_deleted = VALUES(is_deleted);

UPDATE knowledge_point target
JOIN qb_knowledge_point legacy ON legacy.id = target.id
SET target.parent_id = legacy.parent_id;

INSERT INTO knowledge_relation (id, source_id, target_id, relation_type, weight, confidence, source_type, description, created_at, updated_at, is_deleted)
SELECT id, source_id, target_id, relation_type,
       LEAST(1.0000, GREATEST(0.0000, weight)),
       LEAST(1.0000, GREATEST(0.0000, confidence)),
       source_type, description, created_at, updated_at, is_deleted
FROM qb_knowledge_relation
ON DUPLICATE KEY UPDATE
  weight = VALUES(weight), confidence = VALUES(confidence), source_type = VALUES(source_type),
  description = VALUES(description), updated_at = VALUES(updated_at), is_deleted = VALUES(is_deleted);

SET @next_knowledge_point_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM knowledge_point);
SET @next_knowledge_relation_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM knowledge_relation);
SET @sql_kp = CONCAT('ALTER TABLE knowledge_point AUTO_INCREMENT = ', @next_knowledge_point_id);
SET @sql_kr = CONCAT('ALTER TABLE knowledge_relation AUTO_INCREMENT = ', @next_knowledge_relation_id);
PREPARE stmt_kp FROM @sql_kp; EXECUTE stmt_kp; DEALLOCATE PREPARE stmt_kp;
PREPARE stmt_kr FROM @sql_kr; EXECUTE stmt_kr; DEALLOCATE PREPARE stmt_kr;
COMMIT;
