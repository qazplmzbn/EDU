package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbPromptTemplate;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Compatibility mapper backed by prompt_template(owner_type='system'). */
@Mapper
public interface QbPromptTemplateMapper {
    String COLUMNS = "id,template_name,task_type,description,prompt_text,owner_id AS created_by,created_at,updated_at,0 AS is_deleted";
    @Select("SELECT " + COLUMNS + " FROM prompt_template WHERE owner_type='system' ORDER BY updated_at DESC,id DESC") List<QbPromptTemplate> selectAll();
    @Select("SELECT " + COLUMNS + " FROM prompt_template WHERE id=#{id} AND owner_type='system' LIMIT 1") QbPromptTemplate selectById(@Param("id") Long id);
    @Insert("INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) VALUES('system',0,#{templateName},#{taskType},#{description},#{promptText},'v1',NOW(3),NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(QbPromptTemplate row);
    @Insert("INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) SELECT owner_type,owner_id,#{templateName},#{taskType},#{description},#{promptText},CONCAT('v',COALESCE(CAST(SUBSTRING(version,2) AS UNSIGNED),1)+1),NOW(3),NOW(3) FROM prompt_template WHERE id=#{id} AND owner_type='system'") int update(QbPromptTemplate row);
    @Delete("DELETE FROM prompt_template WHERE id=#{id} AND owner_type='system'") int softDelete(@Param("id") Long id);
}
