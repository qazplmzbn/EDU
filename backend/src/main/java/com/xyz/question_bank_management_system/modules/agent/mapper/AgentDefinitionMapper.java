package com.xyz.question_bank_management_system.modules.agent.mapper;

import com.xyz.question_bank_management_system.modules.agent.entity.AgentDefinition;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentDefinitionMapper {
    @Select("SELECT * FROM agent_definition WHERE agent_code=#{code} AND status=1 ORDER BY updated_at DESC,id DESC LIMIT 1")
    AgentDefinition selectEnabledLatest(@Param("code") String code);

    @Select("SELECT * FROM agent_definition ORDER BY agent_code,updated_at DESC,id DESC")
    List<AgentDefinition> selectAll();

    @Select("SELECT * FROM agent_definition WHERE id=#{id} LIMIT 1")
    AgentDefinition selectById(@Param("id") Long id);

    @Insert("INSERT INTO agent_definition(agent_code,agent_name,role_type,description,default_model_config_id,prompt_template_id,config_json,status,version,created_at,updated_at) VALUES(#{agentCode},#{agentName},#{roleType},#{description},#{defaultModelConfigId},#{promptTemplateId},#{configJson},#{status},#{version},NOW(3),NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentDefinition row);

    @Update("UPDATE agent_definition SET status=0,updated_at=NOW(3) WHERE id=#{id}")
    int disable(@Param("id") Long id);
}
