-- Stage 04 physical cleanup is blocked by default.
-- Before setting @stage04_cleanup_confirmed to 1 in the current MySQL session, take a backup,
-- stop application writes, complete regression, retire old clients, and obtain explicit release approval.
USE question_bank;

-- A new MySQL session has NULL here and is safely treated as 0.  Do not change this default.
-- The operator must explicitly run SET @stage04_cleanup_confirmed = 1; before SOURCE-ing this file.
SET @stage04_cleanup_confirmed = COALESCE(@stage04_cleanup_confirmed, 0);

SET @legacy_dependency_count = (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema=DATABASE()
    AND referenced_table_name IN ('qb_assignment_target_class','qb_question_tag','qb_tag_mastery',
                                  'qb_tag','qb_knowledge_point','qb_knowledge_relation')
);

SET @legacy_column_dependency_count = (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema=DATABASE()
    AND ((table_name='qb_attempt_question' AND column_name='tag_ids_json')
      OR (table_name='qb_learning_resource' AND column_name IN ('knowledge_point_id','tag_id'))
      OR (table_name='qb_learning_behavior' AND column_name='tag_id')
      OR (referenced_table_name='qb_attempt_question' AND referenced_column_name='tag_ids_json')
      OR (referenced_table_name='qb_learning_resource' AND referenced_column_name IN ('knowledge_point_id','tag_id'))
      OR (referenced_table_name='qb_learning_behavior' AND referenced_column_name='tag_id'))
);

SET @legacy_program_reference_count = (
  (SELECT COUNT(*) FROM information_schema.views
   WHERE table_schema=DATABASE()
     AND (view_definition LIKE '%qb_assignment_target_class%' OR view_definition LIKE '%qb_question_tag%'
       OR view_definition LIKE '%qb_tag_mastery%' OR view_definition LIKE '%qb_knowledge_point%'
       OR view_definition LIKE '%qb_knowledge_relation%' OR view_definition LIKE '%qb_tag%'))
  + (SELECT COUNT(*) FROM information_schema.routines
     WHERE routine_schema=DATABASE()
       AND (routine_definition LIKE '%qb_assignment_target_class%' OR routine_definition LIKE '%qb_question_tag%'
         OR routine_definition LIKE '%qb_tag_mastery%' OR routine_definition LIKE '%qb_knowledge_point%'
         OR routine_definition LIKE '%qb_knowledge_relation%' OR routine_definition LIKE '%qb_tag%'))
);

SET @invalid_target_count = (
  SELECT COUNT(*) FROM qb_assignment_target
  WHERE NOT ((target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
          OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL))
);

SET @orphan_count = (
  SELECT COUNT(*) FROM (
    SELECT qk.id FROM question_knowledge qk
    LEFT JOIN qb_question q ON q.id=qk.question_id AND q.is_deleted=0
    LEFT JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id AND kp.is_deleted=0
    WHERE q.id IS NULL OR kp.id IS NULL
    UNION ALL
    SELECT rk.id FROM resource_knowledge rk
    LEFT JOIN qb_learning_resource r ON r.id=rk.resource_id AND r.is_deleted=0
    LEFT JOIN knowledge_point kp ON kp.id=rk.knowledge_point_id AND kp.is_deleted=0
    WHERE r.id IS NULL OR kp.id IS NULL
  ) stage04_orphans
);

SET @has_snapshot_columns = (
  SELECT COUNT(*) = 2 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='qb_attempt_question'
    AND column_name IN ('tag_ids_json','knowledge_snapshot_json')
);
SET @snapshot_gap_count = 0;
SET @sql = IF(@has_snapshot_columns,
  'SELECT COUNT(*) INTO @snapshot_gap_count FROM qb_attempt_question WHERE tag_ids_json IS NOT NULL AND tag_ids_json <> '''' AND (knowledge_snapshot_json IS NULL OR knowledge_snapshot_json = '''')',
  'SELECT 0 INTO @snapshot_gap_count');
PREPARE stage04_cleanup_snapshot FROM @sql; EXECUTE stage04_cleanup_snapshot; DEALLOCATE PREPARE stage04_cleanup_snapshot;

SET @unmapped_tag_count = 0;
SET @has_tag_mapping_tables = (
  SELECT COUNT(*) = 3 FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name IN ('qb_tag','qb_knowledge_point','knowledge_point')
);
SET @sql = IF(@has_tag_mapping_tables,
  'SELECT COUNT(*) INTO @unmapped_tag_count FROM qb_tag t LEFT JOIN qb_knowledge_point legacy ON legacy.tag_id=t.id AND legacy.is_deleted=0 LEFT JOIN knowledge_point target ON target.id=legacy.id AND target.is_deleted=0 WHERE t.is_deleted=0 AND target.id IS NULL',
  'SELECT 0 INTO @unmapped_tag_count');
PREPARE stage04_cleanup_tags FROM @sql; EXECUTE stage04_cleanup_tags; DEALLOCATE PREPARE stage04_cleanup_tags;

SET @class_target_gap_count = 0;
SET @has_legacy_class_table = (
  SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='qb_assignment_target_class'
);
SET @sql = IF(@has_legacy_class_table,
  'SELECT COUNT(*) INTO @class_target_gap_count FROM qb_assignment_target_class legacy LEFT JOIN qb_assignment_target target ON target.assignment_id=legacy.assignment_id AND target.target_type=''class'' AND target.class_id=legacy.class_id WHERE target.id IS NULL',
  'SELECT 0 INTO @class_target_gap_count');
PREPARE stage04_cleanup_targets FROM @sql; EXECUTE stage04_cleanup_targets; DEALLOCATE PREPARE stage04_cleanup_targets;

SET @cleanup_ready = @legacy_dependency_count=0 AND @legacy_column_dependency_count=0
                     AND @legacy_program_reference_count=0 AND @invalid_target_count=0
                     AND @orphan_count=0 AND @snapshot_gap_count=0
                     AND @unmapped_tag_count=0 AND @class_target_gap_count=0;
SET @sql = IF(@stage04_cleanup_confirmed=0,
  'SELECT ''BLOCKED: set @stage04_cleanup_confirmed = 1 only after release approval.'' AS message',
  IF(@cleanup_ready,
     'SELECT ''OK: cleanup gate passed.'' AS message',
     'SELECT * FROM stage04_cleanup_blocked'));
PREPARE stage04_cleanup_gate FROM @sql; EXECUTE stage04_cleanup_gate; DEALLOCATE PREPARE stage04_cleanup_gate;

SET @run_cleanup = @stage04_cleanup_confirmed=1 AND @cleanup_ready;

SET @sql = IF(@run_cleanup AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_attempt_question' AND column_name='tag_ids_json'),
  'ALTER TABLE qb_attempt_question DROP COLUMN tag_ids_json', 'SELECT ''SKIPPED: qb_attempt_question.tag_ids_json is absent or cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='knowledge_point_id'),
  'ALTER TABLE qb_learning_resource DROP COLUMN knowledge_point_id', 'SELECT ''SKIPPED: qb_learning_resource.knowledge_point_id is absent or cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_resource' AND column_name='tag_id'),
  'ALTER TABLE qb_learning_resource DROP COLUMN tag_id', 'SELECT ''SKIPPED: qb_learning_resource.tag_id is absent or cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='qb_learning_behavior' AND column_name='tag_id'),
  'ALTER TABLE qb_learning_behavior DROP COLUMN tag_id', 'SELECT ''SKIPPED: qb_learning_behavior.tag_id is absent or cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;

SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_assignment_target_class', 'SELECT ''SKIPPED: qb_assignment_target_class cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_question_tag', 'SELECT ''SKIPPED: qb_question_tag cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_tag_mastery', 'SELECT ''SKIPPED: qb_tag_mastery cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_knowledge_relation', 'SELECT ''SKIPPED: qb_knowledge_relation cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_knowledge_point', 'SELECT ''SKIPPED: qb_knowledge_point cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
SET @sql = IF(@run_cleanup, 'DROP TABLE IF EXISTS qb_tag', 'SELECT ''SKIPPED: qb_tag cleanup is blocked.'' AS message');
PREPARE stage04_cleanup FROM @sql; EXECUTE stage04_cleanup; DEALLOCATE PREPARE stage04_cleanup;
