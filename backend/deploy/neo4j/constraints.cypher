CREATE CONSTRAINT kp_version_identity IF NOT EXISTS FOR (n:KnowledgePoint) REQUIRE (n.courseId,n.graphVersion,n.knowledgePointId) IS UNIQUE;
CREATE INDEX kp_course_version IF NOT EXISTS FOR (n:KnowledgePoint) ON (n.courseId,n.graphVersion);
