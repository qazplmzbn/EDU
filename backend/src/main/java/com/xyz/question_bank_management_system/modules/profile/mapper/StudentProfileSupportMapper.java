package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentProfileSupportMapper {
    @Select("SELECT * FROM student_basic_profile WHERE user_id=#{userId}")
    StudentBasicProfile basicProfile(@Param("userId") Long userId);

    @Insert("INSERT INTO student_basic_profile(user_id,student_no,major_name,grade_name,education_level,learning_stage,weekly_available_hours,updated_at) VALUES(#{userId},#{studentNo},#{majorName},#{gradeName},#{educationLevel},#{learningStage},#{weeklyAvailableHours},NOW(3)) ON DUPLICATE KEY UPDATE student_no=VALUES(student_no),major_name=VALUES(major_name),grade_name=VALUES(grade_name),education_level=VALUES(education_level),learning_stage=VALUES(learning_stage),weekly_available_hours=VALUES(weekly_available_hours),updated_at=NOW(3)")
    int upsertBasicProfile(StudentBasicProfile profile);

    @Select("SELECT * FROM student_learning_goal WHERE user_id=#{userId} ORDER BY status='active' DESC,priority ASC,id DESC")
    List<StudentLearningGoal> goals(@Param("userId") Long userId);

    @Insert("INSERT INTO student_learning_goal(user_id,goal_type,target_occupation_id,target_skill_id,target_knowledge_point_id,goal_description,target_level,expected_completion_date,weekly_available_hours,priority,status,source_type,created_at,updated_at) VALUES(#{userId},#{goalType},#{targetOccupationId},#{targetSkillId},#{targetKnowledgePointId},#{goalDescription},#{targetLevel},#{expectedCompletionDate},#{weeklyAvailableHours},#{priority},#{status},#{sourceType},NOW(3),NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertGoal(StudentLearningGoal goal);

    @Update("UPDATE student_learning_goal SET goal_type=#{goalType},target_occupation_id=#{targetOccupationId},target_skill_id=#{targetSkillId},target_knowledge_point_id=#{targetKnowledgePointId},goal_description=#{goalDescription},target_level=#{targetLevel},expected_completion_date=#{expectedCompletionDate},weekly_available_hours=#{weeklyAvailableHours},priority=#{priority},status=#{status},updated_at=NOW(3) WHERE id=#{id} AND user_id=#{userId}")
    int updateGoal(StudentLearningGoal goal);

    @Select("SELECT * FROM student_learning_preference WHERE user_id=#{userId} AND valid_to IS NULL ORDER BY preference_type,id")
    List<StudentLearningPreference> preferences(@Param("userId") Long userId);

    @Update("UPDATE student_learning_preference SET preference_score=#{preferenceScore},source_type=#{sourceType},evidence_count=#{evidenceCount},updated_at=NOW(3) WHERE user_id=#{userId} AND preference_type=#{preferenceType} AND preference_value=#{preferenceValue} AND valid_to IS NULL")
    int updateActivePreference(StudentLearningPreference preference);

    @Insert("INSERT INTO student_learning_preference(user_id,preference_type,preference_value,preference_score,source_type,evidence_count,valid_from,updated_at) VALUES(#{userId},#{preferenceType},#{preferenceValue},#{preferenceScore},#{sourceType},#{evidenceCount},NOW(3),NOW(3))")
    int insertPreference(StudentLearningPreference preference);

    @Select("SELECT COUNT(*) FROM qb_learning_behavior WHERE user_id=#{userId}") long behaviorCount(@Param("userId") Long userId);
    @Select("SELECT COALESCE(SUM(duration_seconds),0) FROM qb_learning_behavior WHERE user_id=#{userId}") long behaviorDuration(@Param("userId") Long userId);
    @Select("SELECT COUNT(*) FROM student_skill_state WHERE user_id=#{userId} AND proficiency_value<0.5") int weakSkillCount(@Param("userId") Long userId);

    @Insert("INSERT INTO student_skill_state(user_id,skill_id,proficiency_value,proficiency_level,confidence,evidence_count,last_evidence_at,updated_at) SELECT #{userId},sk.skill_id,LEAST(1,COALESCE(SUM(ks.mastery_value*sk.weight)/NULLIF(SUM(sk.weight),0),0)),CASE WHEN COALESCE(SUM(ks.mastery_value*sk.weight)/NULLIF(SUM(sk.weight),0),0)>=0.8 THEN 'mastered' WHEN COALESCE(SUM(ks.mastery_value*sk.weight)/NULLIF(SUM(sk.weight),0),0)>=0.5 THEN 'basic' ELSE 'weak' END,LEAST(1,COALESCE(AVG(ks.confidence),0)),COALESCE(SUM(ks.evidence_count),0),MAX(ks.last_evidence_at),NOW(3) FROM skill_knowledge sk LEFT JOIN student_knowledge_state ks ON ks.knowledge_point_id=sk.knowledge_point_id AND ks.user_id=#{userId} GROUP BY sk.skill_id ON DUPLICATE KEY UPDATE proficiency_value=VALUES(proficiency_value),proficiency_level=VALUES(proficiency_level),confidence=VALUES(confidence),evidence_count=VALUES(evidence_count),last_evidence_at=VALUES(last_evidence_at),updated_at=NOW(3)")
    int rebuildSkillStates(@Param("userId") Long userId);
}
