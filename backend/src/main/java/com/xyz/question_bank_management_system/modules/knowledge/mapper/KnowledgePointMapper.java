package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface KnowledgePointMapper {
    @Select("SELECT * FROM knowledge_point WHERE id=#{id} AND is_deleted=0") KnowledgePoint selectById(Long id);
    @Select("SELECT * FROM knowledge_point WHERE code=#{code} LIMIT 1") KnowledgePoint selectByCode(String code);
    @Select("SELECT * FROM knowledge_point WHERE is_deleted=0 ORDER BY level,id") List<KnowledgePoint> selectAll();
    @Select("SELECT COUNT(*) FROM knowledge_point WHERE parent_id=#{id} AND is_deleted=0") long countChildren(Long id);
    @Insert("INSERT INTO knowledge_point(name,code,parent_id,level,knowledge_type,difficulty,description,created_at,updated_at,is_deleted) VALUES(#{name},#{code},#{parentId},#{level},#{knowledgeType},#{difficulty},#{description},NOW(3),NOW(3),0)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(KnowledgePoint entity);
    @Update("UPDATE knowledge_point SET name=#{name},code=#{code},parent_id=#{parentId},level=#{level},knowledge_type=#{knowledgeType},difficulty=#{difficulty},description=#{description},updated_at=NOW(3),is_deleted=0 WHERE id=#{id}") int update(KnowledgePoint entity);
    @Update("UPDATE knowledge_point SET is_deleted=1,updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0") int softDelete(Long id);
}
