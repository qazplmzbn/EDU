-- Stage 05 data migration. Stop application writes before executing this file.
USE question_bank;

-- The following guard intentionally stops the migration if legacy ability contains an orphan user.
SET @stage05_orphan_ability := (SELECT COUNT(*) FROM qb_user_ability a LEFT JOIN sys_user u ON u.id=a.user_id WHERE u.id IS NULL);
SET @stage05_guard_sql := IF(@stage05_orphan_ability=0, 'SELECT ''stage05 migration guard passed'' AS result', 'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT=''stage05 blocked: orphan qb_user_ability rows''');
PREPARE stage05_guard FROM @stage05_guard_sql; EXECUTE stage05_guard; DEALLOCATE PREPARE stage05_guard;

INSERT INTO student_basic_profile(user_id)
SELECT DISTINCT u.id
FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
WHERE UPPER(r.role_code)='STUDENT'
ON DUPLICATE KEY UPDATE updated_at=student_basic_profile.updated_at;

INSERT INTO student_ability_state(user_id,dimension_id,score,level,confidence,evidence_count)
SELECT a.user_id,d.id,a.ability_score,
 CASE WHEN a.ability_score>=80 THEN 'mastered' WHEN a.ability_score>=50 THEN 'basic' ELSE 'weak' END,
 CASE WHEN a.ability_score=0 THEN 0 ELSE 0.5000 END,CASE WHEN a.ability_score=0 THEN 0 ELSE 1 END
FROM qb_user_ability a JOIN ability_dimension d ON d.dimension_code='ABILITY' AND d.version='v1'
ON DUPLICATE KEY UPDATE score=VALUES(score),level=VALUES(level),confidence=GREATEST(student_ability_state.confidence,VALUES(confidence)),evidence_count=GREATEST(student_ability_state.evidence_count,VALUES(evidence_count)),updated_at=NOW(3);

INSERT IGNORE INTO student_evidence(user_id,evidence_type,source_entity_type,source_entity_id,target_type,target_id,evidence_value,evidence_direction,confidence,evidence_text,occurred_at,extract_version)
SELECT a.user_id,'assessment','answer',a.id,'ability',d.id,
 CASE WHEN aq.score IS NULL OR aq.score=0 THEN 0 ELSE a.final_score/aq.score END,
 CASE WHEN COALESCE(a.is_correct,0)=1 THEN 1 ELSE -1 END,1.0000,'历史作答评分',COALESCE(a.graded_at,a.answered_at,NOW(3)),'stage05-v1'
FROM qb_answer a JOIN qb_attempt_question aq ON aq.id=a.attempt_question_id
JOIN ability_dimension d ON d.dimension_code='ABILITY' AND d.version='v1'
WHERE a.final_score IS NOT NULL;

INSERT IGNORE INTO student_evidence(user_id,evidence_type,source_entity_type,source_entity_id,target_type,target_id,evidence_value,evidence_direction,confidence,evidence_text,occurred_at,extract_version)
SELECT b.user_id,'behavior','behavior',b.id,'preference',b.id,NULL,0,1.0000,'历史学习行为',b.created_at,'stage05-v1'
FROM qb_learning_behavior b;

-- Rebuild current summaries from already migrated state. The application recomputes richer summaries after startup.
INSERT INTO student_profile_summary(user_id,overall_knowledge_mastery,ability_average_score,assessment_accuracy,learning_activity_score,weak_knowledge_count,recommended_difficulty)
SELECT p.user_id,
       (SELECT AVG(k.mastery_value) FROM student_knowledge_state k WHERE k.user_id=p.user_id),
       (SELECT AVG(s.score) FROM student_ability_state s WHERE s.user_id=p.user_id),
       (SELECT AVG(CASE WHEN aq.score>0 THEN a.final_score/aq.score END) FROM qb_answer a JOIN qb_attempt_question aq ON aq.id=a.attempt_question_id WHERE a.user_id=p.user_id AND a.final_score IS NOT NULL),
       LEAST(1.0000,(SELECT COUNT(*) FROM qb_learning_behavior b WHERE b.user_id=p.user_id)/20),
       (SELECT COUNT(*) FROM student_knowledge_state k WHERE k.user_id=p.user_id AND k.mastery_value<0.5),
       3
FROM student_basic_profile p
ON DUPLICATE KEY UPDATE overall_knowledge_mastery=VALUES(overall_knowledge_mastery),ability_average_score=VALUES(ability_average_score),assessment_accuracy=VALUES(assessment_accuracy),learning_activity_score=VALUES(learning_activity_score),weak_knowledge_count=VALUES(weak_knowledge_count),updated_at=NOW(3);

INSERT INTO student_profile_category_stat(user_id,category_type,period_type,total_count,strong_count,weak_count,average_score,coverage_rate,calculated_at)
SELECT p.user_id,'knowledge','current',
       (SELECT COUNT(*) FROM student_knowledge_state k WHERE k.user_id=p.user_id),
       (SELECT COUNT(*) FROM student_knowledge_state k WHERE k.user_id=p.user_id AND k.mastery_value>=0.8),
       (SELECT COUNT(*) FROM student_knowledge_state k WHERE k.user_id=p.user_id AND k.mastery_value<0.5),
       (SELECT AVG(k.mastery_value) FROM student_knowledge_state k WHERE k.user_id=p.user_id),
       CASE WHEN EXISTS(SELECT 1 FROM student_knowledge_state k WHERE k.user_id=p.user_id) THEN 1 ELSE 0 END,NOW(3)
FROM student_basic_profile p
ON DUPLICATE KEY UPDATE total_count=VALUES(total_count),strong_count=VALUES(strong_count),weak_count=VALUES(weak_count),average_score=VALUES(average_score),coverage_rate=VALUES(coverage_rate),calculated_at=NOW(3);

INSERT INTO student_profile_snapshot(user_id,profile_summary,trigger_type,evidence_count)
SELECT p.user_id,'阶段五迁移基线画像','initial',(SELECT COUNT(*) FROM student_evidence e WHERE e.user_id=p.user_id)
FROM student_basic_profile p
WHERE NOT EXISTS (SELECT 1 FROM student_profile_snapshot s WHERE s.user_id=p.user_id AND s.trigger_type='initial');

SELECT 'OK: Stage 05 business data migration completed. No stage evaluation was backfilled.' AS result;
