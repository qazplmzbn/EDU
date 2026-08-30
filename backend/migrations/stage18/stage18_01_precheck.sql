SELECT 'stage17_tables_missing' check_name,2-COUNT(*) problem_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN('course_graph_import','course_graph_import_issue');
SELECT 'legacy_reference_baseline_mismatch' check_name,
 (SELECT COUNT(*) FROM question_knowledge WHERE knowledge_point_id IN(1,2,3,4,5,6,7))-3
 +(SELECT COUNT(*) FROM resource_knowledge WHERE knowledge_point_id IN(1,2,3,4,5,6,7))-7
 +(SELECT COUNT(*) FROM student_knowledge_state WHERE knowledge_point_id IN(1,2,3,4,5,6,7))-3 problem_count;
SELECT 'legacy_point_shape_mismatch' check_name,CASE WHEN COUNT(*)=7 THEN 0 ELSE 1 END problem_count FROM knowledge_point WHERE id IN(1,2,3,4,5,6,7) AND is_deleted=0;
