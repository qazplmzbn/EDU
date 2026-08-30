SELECT 'graph_active_violation' check_name,COUNT(*) problem_count FROM (SELECT course_id FROM knowledge_graph_version WHERE status='ACTIVE' GROUP BY course_id HAVING COUNT(*)<>1)x;
SELECT 'path_active_violation' check_name,COUNT(*) problem_count FROM (SELECT path_id FROM learning_path_version WHERE status='ACTIVE' GROUP BY path_id HAVING COUNT(*)<>1)x;
SELECT 'published_bundle_review_violation' check_name,COUNT(*) problem_count FROM resource_bundle b WHERE b.status='PUBLISHED' AND (SELECT COUNT(DISTINCT expert_role) FROM resource_review r WHERE r.bundle_id=b.id AND r.result='PASS')<>5;
SELECT 'interaction_replay_violation' check_name,COUNT(*) problem_count FROM (SELECT request_id FROM resource_interaction GROUP BY request_id HAVING COUNT(*)>1)x;
