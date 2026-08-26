package com.xyz.question_bank_management_system.modules.agent.mapper;

import com.xyz.question_bank_management_system.modules.agent.entity.AgentStep;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentStepMapper {
    @Select("SELECT * FROM agent_step WHERE agent_task_id=#{taskId} ORDER BY step_no,id")
    List<AgentStep> selectByTaskId(@Param("taskId") Long taskId);

    @Insert("INSERT INTO agent_step(agent_task_id,step_no,agent_definition_id,step_type,input_json,output_json,llm_call_id,status,latency_ms,started_at,finished_at,created_at) VALUES(#{agentTaskId},#{stepNo},#{agentDefinitionId},#{stepType},#{inputJson},#{outputJson},#{llmCallId},#{status},#{latencyMs},#{startedAt},#{finishedAt},NOW(3)) ON DUPLICATE KEY UPDATE output_json=VALUES(output_json),llm_call_id=VALUES(llm_call_id),status=VALUES(status),latency_ms=VALUES(latency_ms),finished_at=VALUES(finished_at)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(AgentStep row);
}
