package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StageEvaluation;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StageEvaluationMapper {
    @Insert("INSERT INTO stage_evaluation(user_id,stage_type,start_date,end_date,profile_snapshot_id,overall_score,dimension_scores_json,evaluation_text,evaluator_type,status,created_at) VALUES(#{userId},#{stageType},#{startDate},#{endDate},#{profileSnapshotId},#{overallScore},#{dimensionScoresJson},#{evaluationText},#{evaluatorType},#{status},NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(StageEvaluation evaluation);

    @Select("SELECT * FROM stage_evaluation WHERE user_id=#{userId} AND stage_type=#{stageType} AND start_date=#{startDate} AND end_date=#{endDate} ORDER BY created_at DESC,id DESC LIMIT 1")
    StageEvaluation latest(@Param("userId") Long userId,@Param("stageType") String stageType,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM stage_evaluation WHERE user_id=#{userId} ORDER BY created_at DESC,id DESC LIMIT #{limit}")
    List<StageEvaluation> history(@Param("userId") Long userId,@Param("limit") int limit);

    @Select("SELECT * FROM stage_evaluation WHERE id=#{id}") StageEvaluation selectById(@Param("id") Long id);
}
