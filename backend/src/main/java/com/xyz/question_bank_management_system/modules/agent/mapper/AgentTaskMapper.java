package com.xyz.question_bank_management_system.modules.agent.mapper;

import com.xyz.question_bank_management_system.modules.agent.entity.AgentTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentTaskMapper {
    @Insert("INSERT INTO agent_task(task_code,task_type,user_id,teacher_id,target_type,target_id,input_json,status,current_step_no,created_at) VALUES(#{taskCode},#{taskType},#{userId},#{teacherId},#{targetType},#{targetId},#{inputJson},#{status},#{currentStepNo},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTask row);

    @Select("SELECT * FROM agent_task WHERE task_code=#{taskCode} LIMIT 1")
    AgentTask selectByCode(@Param("taskCode") String taskCode);

    @Select("SELECT * FROM agent_task WHERE id=#{id} LIMIT 1")
    AgentTask selectById(@Param("id") Long id);

    @Select("SELECT * FROM agent_task WHERE status IN ('queued','running') ORDER BY created_at ASC")
    List<AgentTask> selectRecoverable();

    @Update("UPDATE agent_task SET status='running',started_at=COALESCE(started_at,NOW(3)),error_message=NULL WHERE id=#{id} AND status IN ('queued','running')")
    int claim(@Param("id") Long id);

    @Update("UPDATE agent_task SET status=#{status},current_step_no=#{stepNo},result_summary=#{summary},error_message=#{error},finished_at=CASE WHEN #{terminal}=1 THEN NOW(3) ELSE finished_at END WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("stepNo") Integer stepNo, @Param("summary") String summary, @Param("error") String error, @Param("terminal") int terminal);

    @Update("UPDATE agent_task SET status='queued',error_message=NULL WHERE status='running'")
    int requeueInterrupted();

    @Update("UPDATE agent_task SET input_json=#{inputJson} WHERE id=#{id}")
    int updateInput(@Param("id") Long id, @Param("inputJson") String inputJson);
}
