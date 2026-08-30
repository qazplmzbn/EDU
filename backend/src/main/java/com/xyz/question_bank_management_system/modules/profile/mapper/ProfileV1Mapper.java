package com.xyz.question_bank_management_system.modules.profile.mapper;
import com.xyz.question_bank_management_system.modules.profile.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface ProfileV1Mapper {
 @Select("SELECT * FROM student_resource_preference WHERE user_id=#{userId} AND course_id=#{courseId} ORDER BY resource_type") List<StudentResourcePreference> preferences(@Param("userId")Long userId,@Param("courseId")Long courseId);
 @Select("SELECT * FROM student_cognitive_state WHERE user_id=#{userId} AND course_id=#{courseId} ORDER BY cognitive_level") List<StudentCognitiveState> cognitive(@Param("userId")Long userId,@Param("courseId")Long courseId);
 @Select("SELECT * FROM student_behavior_metric WHERE user_id=#{userId} AND course_id=#{courseId} ORDER BY metric_group,metric_code") List<StudentBehaviorMetric> behavior(@Param("userId")Long userId,@Param("courseId")Long courseId);
 @Insert("INSERT INTO knowledge_state_update_log(user_id,course_id,knowledge_point_id,interaction_id,evidence_scope,previous_mastery,new_mastery,previous_confidence,new_confidence,model_version,profile_version_before,profile_version_after,correlation_id,created_at) VALUES(#{userId},#{courseId},#{knowledgePointId},#{interactionId},#{evidenceScope},#{previousMastery},#{newMastery},#{previousConfidence},#{newConfidence},#{modelVersion},#{profileVersionBefore},#{profileVersionAfter},#{correlationId},NOW(3))") int insertUpdateLog(KnowledgeStateUpdateLog log);
}
