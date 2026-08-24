package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentKnowledgeStateMapper {
    @Insert("INSERT INTO student_knowledge_state(user_id,knowledge_point_id,mastery_value,mastery_level,confidence,evidence_count,correct_count,attempt_count,last_evidence_at,updated_at) VALUES(#{userId},#{knowledgePointId},#{initMastery},#{masteryLevel},#{initConfidence},1,#{correctInc},1,NOW(3),NOW(3)) ON DUPLICATE KEY UPDATE correct_count=correct_count+#{correctInc},attempt_count=attempt_count+1,evidence_count=evidence_count+1,mastery_value=(correct_count+#{correctInc})/(attempt_count+1),mastery_level=CASE WHEN (correct_count+#{correctInc})/(attempt_count+1)>=0.8 THEN 'mastered' WHEN (correct_count+#{correctInc})/(attempt_count+1)>=0.5 THEN 'basic' ELSE 'weak' END,confidence=LEAST(1,(attempt_count+1)/10),last_evidence_at=NOW(3),updated_at=NOW(3)") int upsertAttempt(@Param("userId") Long userId,@Param("knowledgePointId") Long knowledgePointId,@Param("correctInc") int correctInc,@Param("initMastery") double initMastery,@Param("masteryLevel") String masteryLevel,@Param("initConfidence") double initConfidence);
    @Select("SELECT * FROM student_knowledge_state WHERE user_id=#{userId} ORDER BY updated_at DESC,id DESC") List<StudentKnowledgeState> selectByUserId(Long userId);
}
