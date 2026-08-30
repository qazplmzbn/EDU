SELECT 'published_bundle_without_hash' check_name,COUNT(*) problem_count FROM resource_bundle WHERE status='PUBLISHED' AND (content_hash IS NULL OR published_at IS NULL);
SELECT 'hidden_item_invalid' check_name,COUNT(*) problem_count FROM resource_item WHERE visibility='HIDDEN_UNTIL_ASSESSMENT' AND (generated_question_code IS NULL OR grading_key_json IS NULL);
SELECT 'published_bundle_failed_review' check_name,COUNT(*) problem_count FROM resource_bundle b JOIN resource_review r ON r.bundle_id=b.id WHERE b.status='PUBLISHED' AND r.result<>'PASS';
SELECT 'generated_question_metadata_missing' check_name,COUNT(*) problem_count FROM resource_item WHERE generated_question_code IS NOT NULL AND (question_difficulty IS NULL OR cognitive_level IS NULL OR grading_key_json IS NULL);
