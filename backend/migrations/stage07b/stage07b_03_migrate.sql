-- Stage 07B has no legacy source tables. This records the deliberate no-op migration.
USE question_bank;
SELECT 'OK: no legacy source-document, chunk, or evidence-link tables require migration.' AS result;
