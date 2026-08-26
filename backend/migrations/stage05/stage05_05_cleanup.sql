-- Stage 05 physical cleanup. This script is intentionally blocked by default.
-- Run only after backup, stable release, old-code shutdown, and explicit confirmation:
-- SET @stage05_cleanup_confirmed = 1; SOURCE .../stage05_05_cleanup.sql;
USE question_bank;
SET @stage05_cleanup_confirmed = COALESCE(@stage05_cleanup_confirmed,0);
SET @stage05_cleanup_sql = IF(@stage05_cleanup_confirmed=1,
  'DROP TABLE IF EXISTS qb_user_ability',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT=''stage05 cleanup blocked: set @stage05_cleanup_confirmed = 1 in this session after release confirmation''');
PREPARE stage05_cleanup FROM @stage05_cleanup_sql; EXECUTE stage05_cleanup; DEALLOCATE PREPARE stage05_cleanup;
