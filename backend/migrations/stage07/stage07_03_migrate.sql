-- Stage 07 legacy path snapshot migration. Execute manually after schema.
USE question_bank;

-- Each migrated row is marked in learning_path.summary_text. The original JSON remains in qb_learning_path_snapshot.
INSERT INTO learning_path(user_id,course_id,goal_id,target_occupation_id,profile_snapshot_id,title,stage,planning_days,version,status,summary_text,generated_by_agent_task_id,created_at,updated_at,is_deleted)
SELECT s.user_id,NULL,NULL,NULL,NULL,
       COALESCE(NULLIF(TRIM(s.title),''),'历史学习路径'),
       NULLIF(TRIM(s.stage),''),
       GREATEST(1,LEAST(COALESCE(s.days,14),365)),
       1,'obsolete',
       CONCAT('legacy_snapshot:',s.id,'|',LEFT(COALESCE(s.summary_text,''),1900)),
       NULL,s.created_at,s.updated_at,0
FROM qb_learning_path_snapshot s
WHERE s.is_deleted=0
  AND JSON_VALID(s.snapshot_json)
  AND NOT EXISTS (
    SELECT 1 FROM learning_path p
    WHERE p.user_id=s.user_id
      AND p.summary_text LIKE CONCAT('legacy_snapshot:',s.id,'|%')
      AND p.is_deleted=0
  );

INSERT IGNORE INTO learning_path_item(path_id,order_no,item_type,knowledge_point_id,resource_id,question_id,assignment_id,planned_start_at,planned_end_at,status,decision_reason,created_at)
SELECT p.id,
       node.order_no,
       'knowledge',
       node.knowledge_point_id,
       NULL,NULL,NULL,NULL,NULL,
       'pending',
       CONCAT('从历史路径快照 ',s.id,' 迁移'),
       s.created_at
FROM qb_learning_path_snapshot s
JOIN learning_path p ON p.user_id=s.user_id
  AND p.summary_text LIKE CONCAT('legacy_snapshot:',s.id,'|%')
  AND p.is_deleted=0
JOIN JSON_TABLE(s.snapshot_json, '$.phases[*].nodes[*]'
  COLUMNS (
    order_no FOR ORDINALITY,
    knowledge_point_id BIGINT PATH '$.knowledgePointId' NULL ON EMPTY NULL ON ERROR
  )
) node
JOIN knowledge_point kp ON kp.id=node.knowledge_point_id AND kp.is_deleted=0
WHERE s.is_deleted=0
  AND JSON_VALID(s.snapshot_json)
  AND node.knowledge_point_id IS NOT NULL;

SELECT 'OK: stage07 historical snapshots migrated where JSON was valid. Original snapshots are retained as read-only compatibility data.' AS result;
