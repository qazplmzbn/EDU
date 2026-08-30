SELECT 'knowledge_course_missing' check_name,COUNT(*) problem_count FROM knowledge_point WHERE is_deleted=0 AND course_id IS NULL;
SELECT 'official_c_course_missing' check_name,CASE WHEN COUNT(*)=1 THEN 0 ELSE 1 END problem_count FROM course WHERE course_code='C' AND course_name='C语言' AND is_deleted=0;
SELECT 'legacy_c_bridge_missing' check_name,CASE WHEN COUNT(*)=7 THEN 0 ELSE 1 END problem_count FROM course_knowledge ck JOIN course c ON c.id=ck.course_id WHERE c.course_code='C' AND ck.knowledge_point_id IN(1,2,3,4,5,6,7);
SELECT 'relation_semantic_columns_missing' check_name,COUNT(*) problem_count FROM knowledge_relation WHERE is_deleted=0 AND (course_id IS NULL OR graph_version_id IS NULL OR source_knowledge_point_id IS NULL OR target_knowledge_point_id IS NULL);
SELECT 'multiple_active_graphs' check_name,COUNT(*) problem_count FROM (SELECT course_id FROM knowledge_graph_version WHERE status='ACTIVE' GROUP BY course_id HAVING COUNT(*)>1) x;
SELECT 'invalid_relation_type' check_name,COUNT(*) problem_count FROM knowledge_relation WHERE is_deleted=0 AND UPPER(relation_type) NOT IN('PREREQUISITE','SIMILAR','PART_OF','RELATED_TO','SUPPORTS');
