package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.Occupation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OccupationMapper {
    @Select("SELECT * FROM occupation WHERE id=#{id} AND is_deleted=0") Occupation selectById(Long id);
    @Select("SELECT * FROM occupation WHERE id=#{id} FOR UPDATE") Occupation selectByIdForUpdate(Long id);
    @Select("SELECT * FROM occupation WHERE source_name=#{sourceName} AND source_ref=#{sourceRef} LIMIT 1") Occupation selectBySource(@Param("sourceName") String sourceName, @Param("sourceRef") String sourceRef);
    @Select("SELECT COUNT(*) FROM occupation WHERE is_deleted=0 AND (#{keyword} IS NULL OR name_zh LIKE CONCAT('%',#{keyword},'%'))") long countPage(@Param("keyword") String keyword);
    @Select("SELECT * FROM occupation WHERE is_deleted=0 AND (#{keyword} IS NULL OR name_zh LIKE CONCAT('%',#{keyword},'%')) ORDER BY updated_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}") List<Occupation> selectPage(@Param("keyword") String keyword, @Param("offset") long offset, @Param("limit") int limit);
    @Insert("INSERT INTO occupation(name_zh,name_en,category_code,description,source_name,source_ref,version,created_at,updated_at,is_deleted) VALUES(#{nameZh},#{nameEn},#{categoryCode},#{description},#{sourceName},#{sourceRef},#{version},NOW(3),NOW(3),0)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(Occupation entity);
    @Update("UPDATE occupation SET name_zh=#{nameZh},name_en=#{nameEn},category_code=#{categoryCode},description=#{description},version=#{version},updated_at=NOW(3),is_deleted=0 WHERE id=#{id}") int updateImported(Occupation entity);
    @Update("UPDATE occupation SET is_deleted=1,updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0") int softDelete(Long id);
}
