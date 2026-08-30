SELECT 'stage17_tables_missing' check_name,2-COUNT(*) problem_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN('course_graph_import','course_graph_import_issue');
SELECT 'stage17_invalid_status' check_name,COUNT(*) problem_count FROM course_graph_import WHERE status NOT IN('VALIDATED','REJECTED','IMPORTED','APPROVED','FAILED');
SELECT 'stage17_orphan_issue' check_name,COUNT(*) problem_count FROM course_graph_import_issue i LEFT JOIN course_graph_import x ON x.id=i.import_id WHERE x.id IS NULL;
