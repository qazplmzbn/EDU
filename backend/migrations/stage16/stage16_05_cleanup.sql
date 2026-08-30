-- Cleanup is intentionally blocked. Execute reviewed ALTER statements only after a stable release and backup.
SET @stage16_cleanup_confirmed=COALESCE(@stage16_cleanup_confirmed,0);
SELECT IF(@stage16_cleanup_confirmed=1,'Approval recorded: separately review dropping knowledge_point.difficulty, knowledge_relation.source_id/target_id and learning_path_item mixed-node columns.','BLOCKED: no destructive DDL executed.') cleanup_status;
