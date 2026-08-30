INSERT INTO migration_release_checkpoint(release_code,status,verification_json,created_at)
VALUES('course_graph_import_stage17_19','READY_FOR_VERIFY',JSON_OBJECT('cleanupAllowed',false,'neo4jRequired',true),NOW(3))
ON DUPLICATE KEY UPDATE status='READY_FOR_VERIFY',verification_json=VALUES(verification_json);
