package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.OccupationAlias;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OccupationAliasMapper {
    @Select("SELECT * FROM occupation_alias WHERE occupation_id=#{occupationId} ORDER BY id") List<OccupationAlias> selectByOccupationId(Long occupationId);
    @Select("SELECT COUNT(*) FROM occupation_alias WHERE occupation_id=#{occupationId}") long countByOccupationId(Long occupationId);
    @Select("SELECT * FROM occupation_alias WHERE occupation_id=#{occupationId} AND alias_name=#{aliasName} AND alias_type=#{aliasType} LIMIT 1") OccupationAlias selectByBusinessKey(@Param("occupationId") Long occupationId,@Param("aliasName") String aliasName,@Param("aliasType") String aliasType);
    @Insert("INSERT INTO occupation_alias(occupation_id,alias_name,alias_type,created_at) VALUES(#{occupationId},#{aliasName},#{aliasType},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(OccupationAlias entity);
    @Delete("DELETE FROM occupation_alias WHERE id=#{id}") int deleteById(Long id);
}
