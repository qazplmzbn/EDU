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

    @Insert("INSERT INTO agent_review(agent_task_id,agent_step_id,target_type,target_id,review_status,review_report,bundle_id,blueprint_id,review_dimension,issue_code,repair_target,repair_scope,repair_action,repair_instruction,round_no,evidence_refs_json,tool_result_json,reviewer_role,created_at) VALUES(#{agentTaskId},#{agentStepId},#{targetType},#{targetId},#{reviewStatus},#{reviewReport},#{bundleId},#{blueprintId},#{reviewDimension},#{issueCode},#{repairTarget},#{repairScope},#{repairAction},#{repairInstruction},#{roundNo},#{evidenceRefsJson},#{toolResultJson},#{reviewerRole},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertResourceAudit(AgentReview row);
}
