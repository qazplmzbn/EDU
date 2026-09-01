SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('career_mapping_import_batch','career_mapping_import_row','career_mapping_review_decision');
SELECT match_status,COUNT(*) AS row_count FROM career_mapping_import_row GROUP BY match_status ORDER BY match_status;
