SELECT 'path_orphan_course' check_name,COUNT(*) problem_count FROM learning_path p LEFT JOIN course c ON c.id=p.course_id WHERE c.id IS NULL;
SELECT 'path_item_without_knowledge' check_name,COUNT(*) problem_count FROM learning_path_item WHERE item_type='knowledge' AND knowledge_point_id IS NULL;
SELECT 'duplicate_path_order' check_name,COUNT(*) problem_count FROM (SELECT path_id,order_no FROM learning_path_item GROUP BY path_id,order_no HAVING COUNT(*)>1)x;
