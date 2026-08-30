package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentKnowledgeStateMapper {
    @Select("SELECT * FROM student_knowledge_state WHERE user_id=#{userId} ORDER BY updated_at DESC,id DESC") List<StudentKnowledgeState> selectByUserId(Long userId);
    @Select("SELECT s.* FROM student_knowledge_state s JOIN knowledge_point k ON k.id=s.knowledge_point_id WHERE s.user_id=#{userId} AND s.course_id=#{courseId} AND k.status='ACTIVE' AND k.is_deleted=0 AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(k.metadata_json,'$.pathEligible')),'true')='true' ORDER BY s.knowledge_point_id") List<StudentKnowledgeState> selectByUserAndCourse(@Param("userId")Long userId,@Param("courseId")Long courseId);
    @Select("SELECT * FROM student_knowledge_state WHERE user_id=#{userId} AND course_id=#{courseId} AND knowledge_point_id=#{knowledgePointId} FOR UPDATE") StudentKnowledgeState selectForUpdate(@Param("userId")Long userId,@Param("courseId")Long courseId,@Param("knowledgePointId")Long knowledgePointId);
    @Insert("INSERT INTO student_knowledge_state(user_id,course_id,knowledge_point_id,mastery_value,mastery_level,confidence,evidence_count,correct_count,attempt_count,state_version,calculation_method,algorithm_version,last_interaction_seq,last_interaction_id,last_evidence_at,updated_at) VALUES(#{userId},#{courseId},#{knowledgePointId},#{masteryValue},#{masteryLevel},#{confidence},#{evidenceCount},#{correctCount},#{attemptCount},#{stateVersion},#{calculationMethod},#{algorithmVersion},#{lastInteractionSeq},#{lastInteractionId},NOW(3),NOW(3)) ON DUPLICATE KEY UPDATE mastery_value=VALUES(mastery_value),mastery_level=VALUES(mastery_level),confidence=VALUES(confidence),evidence_count=VALUES(evidence_count),correct_count=VALUES(correct_count),attempt_count=VALUES(attempt_count),state_version=VALUES(state_version),calculation_method=VALUES(calculation_method),algorithm_version=VALUES(algorithm_version),last_interaction_seq=VALUES(last_interaction_seq),last_interaction_id=VALUES(last_interaction_id),last_evidence_at=NOW(3),updated_at=NOW(3)") int upsertVersioned(StudentKnowledgeState state);
}
