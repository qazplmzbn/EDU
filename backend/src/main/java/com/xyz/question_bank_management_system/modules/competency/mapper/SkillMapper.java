package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.Skill;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SkillMapper {
    @Select("SELECT * FROM skill WHERE id=#{id} AND is_deleted=0") Skill selectById(Long id);
    @Select("SELECT * FROM skill WHERE id=#{id} FOR UPDATE") Skill selectByIdForUpdate(Long id);
    @Select("SELECT * FROM skill WHERE source_name=#{sourceName} AND source_ref=#{sourceRef} LIMIT 1") Skill selectBySource(@Param("sourceName") String sourceName, @Param("sourceRef") String sourceRef);
    @Select("SELECT COUNT(*) FROM skill WHERE is_deleted=0 AND (#{keyword} IS NULL OR name_zh LIKE CONCAT('%',#{keyword},'%'))") long countPage(@Param("keyword") String keyword);
    @Select("SELECT * FROM skill WHERE is_deleted=0 AND (#{keyword} IS NULL OR name_zh LIKE CONCAT('%',#{keyword},'%')) ORDER BY updated_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}") List<Skill> selectPage(@Param("keyword") String keyword, @Param("offset") long offset, @Param("limit") int limit);
    @Insert("INSERT INTO skill(name_zh,skill_type,description,source_name,source_ref,created_at,updated_at,is_deleted) VALUES(#{nameZh},#{skillType},#{description},#{sourceName},#{sourceRef},NOW(3),NOW(3),0)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(Skill entity);
    @Update("UPDATE skill SET name_zh=#{nameZh},skill_type=#{skillType},description=#{description},updated_at=NOW(3),is_deleted=0 WHERE id=#{id}") int updateImported(Skill entity);
    @Update("UPDATE skill SET is_deleted=1,updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0") int softDelete(Long id);
}
