-- Stage 08 deterministic backfill. Precheck guarantees exactly one course per point.
INSERT INTO course(course_code,course_name,description,teacher_id,status,created_at,updated_at,is_deleted)
SELECT 'C','C语言','Stage 08 legacy bridge; completed by the reviewed course-graph import',NULL,'draft',NOW(3),NOW(3),0
WHERE NOT EXISTS(SELECT 1 FROM course WHERE course_code='C' AND is_deleted=0);
INSERT INTO course_knowledge(course_id,knowledge_point_id,sequence_no,is_core,coverage_weight,created_at)
SELECT c.id,kp.id,
       CASE kp.id WHEN 1 THEN 10 WHEN 2 THEN 20 WHEN 3 THEN 30 WHEN 4 THEN 40 WHEN 5 THEN 50 WHEN 6 THEN 60 ELSE 70 END,
       CASE WHEN kp.id BETWEEN 1 AND 6 THEN 1 ELSE 0 END,1.0000,NOW(3)
FROM course c JOIN knowledge_point kp ON kp.id IN (1,2,3,4,5,6,7) AND kp.is_deleted=0
WHERE c.course_code='C' AND c.is_deleted=0
ON DUPLICATE KEY UPDATE course_id=VALUES(course_id);
UPDATE knowledge_point kp JOIN (SELECT knowledge_point_id,MIN(course_id) course_id FROM course_knowledge GROUP BY knowledge_point_id) x ON x.knowledge_point_id=kp.id SET kp.course_id=x.course_id,kp.content_version=COALESCE(kp.content_version,CONCAT('kp_',kp.id,'_v1')) WHERE kp.course_id IS NULL;
INSERT INTO knowledge_graph_version(version_code,course_id,description,status,node_count,edge_count,correlation_id,created_at,updated_at)
SELECT CONCAT('graph_course',c.id,'_legacy_v1'),c.id,'Stage 08 legacy MySQL relation baseline','DRAFT',COUNT(DISTINCT kp.id),0,'stage08-migration',NOW(3),NOW(3) FROM course c JOIN knowledge_point kp ON kp.course_id=c.id AND kp.is_deleted=0 GROUP BY c.id ON DUPLICATE KEY UPDATE version_code=version_code;
UPDATE knowledge_relation r JOIN knowledge_point p ON p.id=r.source_id JOIN knowledge_graph_version gv ON gv.course_id=p.course_id AND gv.version_code=CONCAT('graph_course',p.course_id,'_legacy_v1') SET r.course_id=p.course_id,r.graph_version_id=gv.id,r.relation_code=COALESCE(r.relation_code,CONCAT('legacy_rel_',r.id)),r.source_knowledge_point_id=r.source_id,r.target_knowledge_point_id=r.target_id WHERE r.graph_version_id IS NULL;
UPDATE knowledge_graph_version gv SET edge_count=(SELECT COUNT(*) FROM knowledge_relation r WHERE r.graph_version_id=gv.id AND r.is_deleted=0) WHERE gv.version_code LIKE '%_legacy_v1';
