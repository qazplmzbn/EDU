-- Stage 07 cleanup is intentionally non-destructive.
-- Do not delete qb_learning_path_snapshot: it remains the raw JSON source for legacy read-only endpoints.
USE question_bank;

SELECT 'No Stage 07 destructive cleanup is authorized. Keep qb_learning_path_snapshot until all legacy clients and historical-read requirements are retired by an explicit later release decision.' AS result;

SELECT COUNT(*) AS legacy_snapshot_count
FROM qb_learning_path_snapshot
WHERE is_deleted=0;
