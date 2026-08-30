-- No ordinary attempt is migrated into resource_interaction. Only future final submissions of PUBLISHED generated questions are legal evidence.
UPDATE resource_assessment_release SET status='EXPIRED' WHERE status='ACTIVE' AND expires_at IS NOT NULL AND expires_at<=NOW(3);
