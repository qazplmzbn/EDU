-- Stage 04 has no new business-data migration.
-- This script blocks the sequence if historical Stage 01-03 data is incomplete.
USE question_bank;

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

SET @invalid_target_count = (
  SELECT COUNT(*) FROM qb_assignment_target
  WHERE NOT ((target_type='student' AND student_id IS NOT NULL AND class_id IS NULL)
          OR (target_type='class' AND class_id IS NOT NULL AND student_id IS NULL))
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
PREPARE stage04_snapshot_gap FROM @sql; EXECUTE stage04_snapshot_gap; DEALLOCATE PREPARE stage04_snapshot_gap;

SET @blocker_count = @orphan_count + @invalid_target_count + @snapshot_gap_count;
SET @sql = IF(@blocker_count=0,
  'SELECT ''OK: Stage 04 has no pending business-data migration.'' AS message',
  'SELECT * FROM stage04_migration_blocked');
PREPARE stage04_migrate FROM @sql; EXECUTE stage04_migrate; DEALLOCATE PREPARE stage04_migrate;
