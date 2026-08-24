package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface KnowledgeRelationMapper {
    @Select("SELECT * FROM knowledge_relation WHERE is_deleted=0 ORDER BY updated_at DESC,id DESC") List<KnowledgeRelation> selectAll();
    @Select("SELECT COUNT(*) FROM knowledge_relation WHERE is_deleted=0 AND (source_id=#{id} OR target_id=#{id})") long countByKnowledgePointId(Long id);
    @Select("SELECT * FROM knowledge_relation WHERE source_id=#{sourceId} AND target_id=#{targetId} AND relation_type=#{relationType} LIMIT 1") KnowledgeRelation selectByBusinessKey(@Param("sourceId") Long sourceId,@Param("targetId") Long targetId,@Param("relationType") String relationType);
    @Insert("INSERT INTO knowledge_relation(source_id,target_id,relation_type,weight,confidence,source_type,description,created_at,updated_at,is_deleted) VALUES(#{sourceId},#{targetId},#{relationType},#{weight},#{confidence},#{sourceType},#{description},NOW(3),NOW(3),0)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(KnowledgeRelation entity);
    @Update("UPDATE knowledge_relation SET source_id=#{sourceId},target_id=#{targetId},relation_type=#{relationType},weight=#{weight},confidence=#{confidence},source_type=#{sourceType},description=#{description},updated_at=NOW(3),is_deleted=0 WHERE id=#{id}") int update(KnowledgeRelation entity);
    @Update("UPDATE knowledge_relation SET weight=#{weight},confidence=#{confidence},source_type=#{sourceType},description=#{description},updated_at=NOW(3),is_deleted=0 WHERE id=#{id}") int updateImported(KnowledgeRelation entity);
    @Update("UPDATE knowledge_relation SET is_deleted=1,updated_at=NOW(3) WHERE id=#{id}") int softDelete(Long id);
}
