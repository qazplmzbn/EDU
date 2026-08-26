package com.xyz.question_bank_management_system.modules.agent.mapper;

import com.xyz.question_bank_management_system.modules.agent.entity.AgentDecision;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentDecisionMapper {
    @Select("SELECT * FROM agent_decision WHERE agent_task_id=#{taskId} ORDER BY id")
    List<AgentDecision> selectByTaskId(@Param("taskId") Long taskId);

    @Insert("INSERT INTO agent_decision(agent_task_id,agent_step_id,decision_type,target_type,target_id,decision_value,decision_reason,confidence,evidence_json,created_at) VALUES(#{agentTaskId},#{agentStepId},#{decisionType},#{targetType},#{targetId},#{decisionValue},#{decisionReason},#{confidence},#{evidenceJson},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentDecision row);
}
