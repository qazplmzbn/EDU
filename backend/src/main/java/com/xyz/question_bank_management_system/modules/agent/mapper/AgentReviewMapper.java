package com.xyz.question_bank_management_system.modules.agent.mapper;

import com.xyz.question_bank_management_system.modules.agent.entity.AgentReview;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentReviewMapper {
    @Select("SELECT * FROM agent_review WHERE agent_task_id=#{taskId} ORDER BY id")
    List<AgentReview> selectByTaskId(@Param("taskId") Long taskId);

    @Insert("INSERT INTO agent_review(agent_task_id,agent_step_id,target_type,target_id,factual_score,coverage_score,difficulty_match_score,hallucination_rate,source_consistency_score,review_status,review_report,created_at) VALUES(#{agentTaskId},#{agentStepId},#{targetType},#{targetId},#{factualScore},#{coverageScore},#{difficultyMatchScore},#{hallucinationRate},#{sourceConsistencyScore},#{reviewStatus},#{reviewReport},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentReview row);
}
