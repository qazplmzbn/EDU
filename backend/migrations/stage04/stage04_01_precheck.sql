-- Stage 04 precheck. Read-only; run manually before any Stage 04 action.
USE question_bank;

SELECT VERSION() AS mysql_version, DATABASE() AS current_database;

WITH expected_tables AS (
  SELECT 'occupation' AS table_name UNION ALL SELECT 'skill' UNION ALL
  SELECT 'knowledge_point' UNION ALL SELECT 'knowledge_relation' UNION ALL
  SELECT 'question_knowledge' UNION ALL SELECT 'resource_knowledge' UNION ALL
  SELECT 'student_knowledge_state' UNION ALL SELECT 'qb_assignment_target'
)
SELECT e.table_name, t.table_name IS NOT NULL AS exists_in_database
FROM expected_tables e
LEFT JOIN information_schema.tables t
  ON t.table_schema=DATABASE() AND t.table_name=e.table_name
ORDER BY e.table_name;

WITH cleanup_candidates AS (
  SELECT 'qb_assignment_target_class' AS table_name UNION ALL
  SELECT 'qb_question_tag' UNION ALL SELECT 'qb_tag_mastery' UNION ALL
  SELECT 'qb_tag' UNION ALL SELECT 'qb_knowledge_point' UNION ALL
  SELECT 'qb_knowledge_relation'
)
SELECT c.table_name, t.table_name IS NOT NULL AS exists_in_database,
       COALESCE((SELECT table_rows FROM information_schema.tables x
                 WHERE x.table_schema=DATABASE() AND x.table_name=c.table_name), 0) AS estimated_rows
FROM cleanup_candidates c
LEFT JOIN information_schema.tables t
  ON t.table_schema=DATABASE() AND t.table_name=c.table_name
ORDER BY c.table_name;

WITH legacy_columns AS (
  SELECT 'qb_attempt_question' AS table_name, 'tag_ids_json' AS column_name UNION ALL
  SELECT 'qb_learning_resource', 'knowledge_point_id' UNION ALL
  SELECT 'qb_learning_resource', 'tag_id' UNION ALL
  SELECT 'qb_learning_behavior', 'tag_id'
)
SELECT l.table_name, l.column_name, c.column_type
FROM legacy_columns l
LEFT JOIN information_schema.columns c
  ON c.table_schema=DATABASE() AND c.table_name=l.table_name AND c.column_name=l.column_name
ORDER BY l.table_name, l.column_name;

SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema=DATABASE()
  AND referenced_table_name IN ('qb_assignment_target_class','qb_question_tag','qb_tag_mastery',
                                'qb_tag','qb_knowledge_point','qb_knowledge_relation')
ORDER BY table_name, constraint_name;

SELECT table_name, column_name, constraint_name, referenced_table_name, referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema=DATABASE()
  AND ((table_name='qb_attempt_question' AND column_name='tag_ids_json')
    OR (table_name='qb_learning_resource' AND column_name IN ('knowledge_point_id','tag_id'))
    OR (table_name='qb_learning_behavior' AND column_name='tag_id')
    OR (referenced_table_name='qb_attempt_question' AND referenced_column_name='tag_ids_json')
    OR (referenced_table_name='qb_learning_resource' AND referenced_column_name IN ('knowledge_point_id','tag_id'))
    OR (referenced_table_name='qb_learning_behavior' AND referenced_column_name='tag_id'))
ORDER BY table_name, constraint_name;

SELECT table_name AS view_name
FROM information_schema.views
WHERE table_schema=DATABASE()
  AND (view_definition LIKE '%qb_assignment_target_class%' OR view_definition LIKE '%qb_question_tag%'
       OR view_definition LIKE '%qb_tag_mastery%' OR view_definition LIKE '%qb_knowledge_point%'
       OR view_definition LIKE '%qb_knowledge_relation%' OR view_definition LIKE '%qb_tag%');

SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema=DATABASE()
  AND (routine_definition LIKE '%qb_assignment_target_class%' OR routine_definition LIKE '%qb_question_tag%'
       OR routine_definition LIKE '%qb_tag_mastery%' OR routine_definition LIKE '%qb_knowledge_point%'
       OR routine_definition LIKE '%qb_knowledge_relation%' OR routine_definition LIKE '%qb_tag%');

SELECT qk.id, qk.question_id, qk.knowledge_point_id, 'question_knowledge' AS source
FROM question_knowledge qk
LEFT JOIN qb_question q ON q.id=qk.question_id AND q.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id AND kp.is_deleted=0
WHERE q.id IS NULL OR kp.id IS NULL
UNION ALL
SELECT rk.id, rk.resource_id, rk.knowledge_point_id, 'resource_knowledge' AS source
FROM resource_knowledge rk
LEFT JOIN qb_learning_resource r ON r.id=rk.resource_id AND r.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=rk.knowledge_point_id AND kp.is_deleted=0
WHERE r.id IS NULL OR kp.id IS NULL
UNION ALL
SELECT sk.id, sk.user_id, sk.knowledge_point_id, 'student_knowledge_state' AS source
FROM student_knowledge_state sk
LEFT JOIN sys_user u ON u.id=sk.user_id AND u.is_deleted=0
LEFT JOIN knowledge_point kp ON kp.id=sk.knowledge_point_id AND kp.is_deleted=0
WHERE u.id IS NULL OR kp.id IS NULL;

SELECT target_type, COUNT(*) AS row_count
FROM qb_assignment_target
GROUP BY target_type
ORDER BY target_type;

SELECT COUNT(*) AS invalid_assignment_target_scope_rows
FROM qb_assignment_target
WHERE NOT ((target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
        OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL));

SELECT assignment_id, target_type, student_id, class_id, COUNT(*) AS duplicate_count
FROM qb_assignment_target
GROUP BY assignment_id, target_type, student_id, class_id
HAVING COUNT(*) > 1;

SET @has_tag_migration_sources = (
  SELECT COUNT(*) = 4 FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name IN ('qb_tag','qb_knowledge_point','question_knowledge','qb_question_tag')
);
SET @sql = IF(@has_tag_migration_sources,
  'SELECT t.id AS unmapped_tag_id, t.tag_name FROM qb_tag t LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id=t.id AND legacy.is_deleted=0 LEFT JOIN knowledge_point target ON target.id=legacy.id AND target.is_deleted=0 WHERE t.is_deleted=0 AND target.id IS NULL ORDER BY t.id',
  'SELECT ''SKIPPED: legacy Tag migration sources are absent.'' AS message');
PREPARE stage04_precheck FROM @sql; EXECUTE stage04_precheck; DEALLOCATE PREPARE stage04_precheck;

SET @has_snapshot_columns = (
  SELECT COUNT(*) = 2 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='qb_attempt_question'
    AND column_name IN ('tag_ids_json','knowledge_snapshot_json')
);
SET @sql = IF(@has_snapshot_columns,
  'SELECT id, tag_ids_json FROM qb_attempt_question WHERE tag_ids_json IS NOT NULL AND tag_ids_json <> '''' AND (knowledge_snapshot_json IS NULL OR knowledge_snapshot_json = '''')',
  'SELECT ''SKIPPED: legacy attempt snapshot column is absent.'' AS message');
PREPARE stage04_snapshot_check FROM @sql; EXECUTE stage04_snapshot_check; DEALLOCATE PREPARE stage04_snapshot_check;
