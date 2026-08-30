-- Stage 08 precheck (read-only). Any non-zero count blocks migration.
-- The source database has one explicitly approved legacy C-language set (ids 1-7)
-- without course ownership. Stage 08 migrates that exact set to the official C
-- course. Any other orphan remains a blocking problem.
SELECT 'unexpected_knowledge_without_single_course' AS check_name, COUNT(*) AS problem_count
FROM knowledge_point kp
LEFT JOIN (SELECT knowledge_point_id,COUNT(DISTINCT course_id) c FROM course_knowledge GROUP BY knowledge_point_id) x ON x.knowledge_point_id=kp.id
WHERE kp.is_deleted=0
  AND COALESCE(x.c,0)<>1
  AND kp.id NOT IN (1,2,3,4,5,6,7);
SELECT 'legacy_c_bridge_shape_mismatch' AS check_name,
       CASE WHEN COUNT(*)=7 THEN 0 ELSE 1 END AS problem_count
FROM knowledge_point
WHERE id IN (1,2,3,4,5,6,7) AND is_deleted=0;
SELECT 'course_c_conflict' AS check_name,COUNT(*) AS problem_count
FROM course WHERE course_code='C' AND (course_name<>'C语言' OR is_deleted<>0);
SELECT 'cross_course_relation' AS check_name,COUNT(*) AS problem_count FROM knowledge_relation r
JOIN course_knowledge s ON s.knowledge_point_id=r.source_id JOIN course_knowledge t ON t.knowledge_point_id=r.target_id
WHERE s.course_id<>t.course_id AND r.is_deleted=0;
SELECT 'relation_without_source_evidence' AS check_name,COUNT(*) AS problem_count FROM knowledge_relation WHERE is_deleted=0 AND source_type NOT IN ('manual','teacher','TEACHER_CONFIRMED');
