package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbPromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbPromptTemplateMapper {

    @Select("SELECT * FROM qb_prompt_template WHERE is_deleted=0 ORDER BY updated_at DESC, id DESC")
    List<QbPromptTemplate> selectAll();

    @Select("SELECT * FROM qb_prompt_template WHERE id=#{id} AND is_deleted=0")
    QbPromptTemplate selectById(@Param("id") Long id);

    @Insert("INSERT INTO qb_prompt_template(template_name, task_type, description, prompt_text, created_by, created_at, updated_at, is_deleted) " +
            "VALUES(#{templateName}, #{taskType}, #{description}, #{promptText}, #{createdBy}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbPromptTemplate template);

    @Update("UPDATE qb_prompt_template SET template_name=#{templateName}, task_type=#{taskType}, description=#{description}, prompt_text=#{promptText}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbPromptTemplate template);

    @Update("UPDATE qb_prompt_template SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int softDelete(@Param("id") Long id);
}
