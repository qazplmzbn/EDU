-- Stage 02 cleanup is intentionally blocked until all application code and frontend calls use knowledge points.
-- Do not execute during the compatibility period.
USE question_bank;
SELECT 'BLOCKED' AS cleanup_status,
       'Run only after unmapped tags are resolved, regression passes, and no old Tag clients remain.' AS message;
