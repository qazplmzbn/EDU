package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkill;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OccupationSkillMapper {
    @Select("SELECT os.*, s.name_zh AS skill_name FROM occupation_skill os JOIN skill s ON s.id=os.skill_id AND s.is_deleted=0 WHERE os.occupation_id=#{occupationId} ORDER BY os.id") @Results(id="occupationSkill", value={@Result(property="id",column="id"),@Result(property="occupationId",column="occupation_id"),@Result(property="skillId",column="skill_id"),@Result(property="requirementType",column="requirement_type"),@Result(property="importanceScore",column="importance_score"),@Result(property="requiredLevel",column="required_level"),@Result(property="sourceRef",column="source_ref"),@Result(property="createdAt",column="created_at")}) List<OccupationSkill> selectByOccupationId(Long occupationId);
    @Select("SELECT * FROM occupation_skill WHERE occupation_id=#{occupationId} ORDER BY id FOR UPDATE") List<OccupationSkill> selectByOccupationIdForUpdate(Long occupationId);
    @Select("SELECT COUNT(*) FROM occupation_skill WHERE occupation_id=#{id}") long countByOccupationId(Long id);
    @Select("SELECT COUNT(*) FROM occupation_skill WHERE skill_id=#{id}") long countBySkillId(Long id);
    @Select("SELECT * FROM occupation_skill WHERE occupation_id=#{occupationId} AND skill_id=#{skillId} AND requirement_type=#{requirementType} LIMIT 1") OccupationSkill selectByBusinessKey(@Param("occupationId") Long occupationId,@Param("skillId") Long skillId,@Param("requirementType") String requirementType);
    @Insert("INSERT INTO occupation_skill(occupation_id,skill_id,requirement_type,importance_score,required_level,source_ref,created_at) VALUES(#{occupationId},#{skillId},#{requirementType},#{importanceScore},#{requiredLevel},#{sourceRef},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(OccupationSkill entity);
    @Update("UPDATE occupation_skill SET importance_score=#{importanceScore},required_level=#{requiredLevel},source_ref=#{sourceRef} WHERE id=#{id}") int update(OccupationSkill entity);
    @Delete("DELETE FROM occupation_skill WHERE id=#{id}") int deleteById(Long id);
}
