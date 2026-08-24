package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.SkillKnowledge;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SkillKnowledgeMapper {
    @Select("SELECT * FROM skill_knowledge WHERE skill_id=#{skillId} ORDER BY id") List<SkillKnowledge> selectBySkillId(Long skillId);
    @Select("SELECT COUNT(*) FROM skill_knowledge WHERE skill_id=#{id}") long countBySkillId(Long id);
    @Select("SELECT COUNT(*) FROM skill_knowledge WHERE knowledge_point_id=#{id}") long countByKnowledgePointId(Long id);
    @Select("SELECT * FROM skill_knowledge WHERE skill_id=#{skillId} AND knowledge_point_id=#{knowledgePointId} AND requirement_type=#{requirementType} LIMIT 1") SkillKnowledge selectByBusinessKey(@Param("skillId") Long skillId,@Param("knowledgePointId") Long knowledgePointId,@Param("requirementType") String requirementType);
    @Insert("INSERT INTO skill_knowledge(skill_id,knowledge_point_id,requirement_type,weight,confidence,source_type,source_ref,evidence_text,created_at) VALUES(#{skillId},#{knowledgePointId},#{requirementType},#{weight},#{confidence},#{sourceType},#{sourceRef},#{evidenceText},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(SkillKnowledge entity);
    @Update("UPDATE skill_knowledge SET weight=#{weight},confidence=#{confidence},source_type=#{sourceType},source_ref=#{sourceRef},evidence_text=#{evidenceText} WHERE id=#{id}") int update(SkillKnowledge entity);
    @Delete("DELETE FROM skill_knowledge WHERE id=#{id}") int deleteById(Long id);
}
