-- Destructive cleanup is blocked by default.
SET @stage08_cleanup_confirmed=COALESCE(@stage08_cleanup_confirmed,0);
SELECT IF(@stage08_cleanup_confirmed=1,'Cleanup approval recorded; execute reviewed ALTER statements manually.','BLOCKED: retain difficulty/source_id/target_id until Stage 16 cutover.') AS cleanup_status;
