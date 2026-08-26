package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PromptTemplateMapper {
    @Select("SELECT * FROM prompt_template WHERE owner_type=#{ownerType} AND COALESCE(owner_id,0)=COALESCE(#{ownerId},0) ORDER BY updated_at DESC,id DESC")
    List<PromptTemplate> selectByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);

    @Select("SELECT * FROM prompt_template WHERE id=#{id} LIMIT 1")
    PromptTemplate selectById(@Param("id") Long id);

    @Select("SELECT * FROM prompt_template WHERE owner_type=#{ownerType} AND COALESCE(owner_id,0)=COALESCE(#{ownerId},0) AND template_name=#{name} AND task_type=#{taskType} ORDER BY created_at DESC,id DESC LIMIT 1")
    PromptTemplate selectLatest(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId, @Param("name") String name, @Param("taskType") String taskType);

    @Insert("INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) VALUES(#{ownerType},#{ownerId},#{templateName},#{taskType},#{description},#{promptText},#{version},NOW(3),NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PromptTemplate row);

    @Delete("DELETE FROM prompt_template WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
