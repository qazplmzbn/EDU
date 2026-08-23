package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbUserPromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbUserPromptTemplateMapper {

    @Select("SELECT * FROM qb_user_prompt_template WHERE user_id=#{userId} AND is_deleted=0 ORDER BY updated_at DESC, id DESC")
    List<QbUserPromptTemplate> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM qb_user_prompt_template WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    QbUserPromptTemplate selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO qb_user_prompt_template(user_id, template_name, task_type, description, prompt_text, created_at, updated_at, is_deleted) " +
            "VALUES(#{userId}, #{templateName}, #{taskType}, #{description}, #{promptText}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbUserPromptTemplate template);

    @Update("UPDATE qb_user_prompt_template SET template_name=#{templateName}, task_type=#{taskType}, description=#{description}, prompt_text=#{promptText}, updated_at=NOW(3) WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    int update(QbUserPromptTemplate template);

    @Update("UPDATE qb_user_prompt_template SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
