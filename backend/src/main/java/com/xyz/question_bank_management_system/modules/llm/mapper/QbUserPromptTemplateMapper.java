package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbUserPromptTemplate;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Compatibility mapper backed by prompt_template(owner_type='user'). */
@Mapper
public interface QbUserPromptTemplateMapper {
    String COLUMNS = "id,owner_id AS user_id,template_name,task_type,description,prompt_text,created_at,updated_at,0 AS is_deleted";
    @Select("SELECT " + COLUMNS + " FROM prompt_template WHERE owner_type='user' AND owner_id=#{userId} ORDER BY updated_at DESC,id DESC") List<QbUserPromptTemplate> selectByUserId(@Param("userId") Long userId);
    @Select("SELECT " + COLUMNS + " FROM prompt_template WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId} LIMIT 1") QbUserPromptTemplate selectOwnedById(@Param("id") Long id,@Param("userId") Long userId);
    @Insert("INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) VALUES('user',#{userId},#{templateName},#{taskType},#{description},#{promptText},'v1',NOW(3),NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(QbUserPromptTemplate row);
    @Insert("INSERT INTO prompt_template(owner_type,owner_id,template_name,task_type,description,prompt_text,version,created_at,updated_at) SELECT owner_type,owner_id,#{templateName},#{taskType},#{description},#{promptText},CONCAT('v',COALESCE(CAST(SUBSTRING(version,2) AS UNSIGNED),1)+1),NOW(3),NOW(3) FROM prompt_template WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int update(QbUserPromptTemplate row);
    @Delete("DELETE FROM prompt_template WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int softDelete(@Param("id") Long id,@Param("userId") Long userId);
}
