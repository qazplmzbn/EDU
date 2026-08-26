package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSummary;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentProfileSummaryMapper {
    @Select("SELECT * FROM student_profile_summary WHERE user_id=#{userId}")
    StudentProfileSummary selectByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO student_profile_summary(user_id,overall_knowledge_mastery,ability_average_score,assessment_accuracy,learning_activity_score,weak_knowledge_count,weak_skill_count,recommended_difficulty,updated_at) VALUES(#{userId},#{overallKnowledgeMastery},#{abilityAverageScore},#{assessmentAccuracy},#{learningActivityScore},#{weakKnowledgeCount},#{weakSkillCount},#{recommendedDifficulty},NOW(3)) ON DUPLICATE KEY UPDATE overall_knowledge_mastery=VALUES(overall_knowledge_mastery),ability_average_score=VALUES(ability_average_score),assessment_accuracy=VALUES(assessment_accuracy),learning_activity_score=VALUES(learning_activity_score),weak_knowledge_count=VALUES(weak_knowledge_count),weak_skill_count=VALUES(weak_skill_count),recommended_difficulty=VALUES(recommended_difficulty),updated_at=NOW(3)")
    int upsert(StudentProfileSummary summary);

    @Update("UPDATE student_profile_summary SET last_profile_snapshot_id=#{snapshotId},updated_at=NOW(3) WHERE user_id=#{userId}")
    int updateLastSnapshot(@Param("userId") Long userId, @Param("snapshotId") Long snapshotId);
}
