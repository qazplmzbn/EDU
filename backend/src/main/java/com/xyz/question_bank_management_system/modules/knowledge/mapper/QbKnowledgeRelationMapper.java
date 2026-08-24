package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbKnowledgeRelationMapper {

    @Select({
            "SELECT r.*, s.name AS source_name, t.name AS target_name",
            "FROM knowledge_relation r",
            "JOIN knowledge_point s ON s.id = r.source_id AND s.is_deleted = 0",
            "JOIN knowledge_point t ON t.id = r.target_id AND t.is_deleted = 0",
            "WHERE r.is_deleted = 0",
            "ORDER BY r.updated_at DESC, r.id DESC"
    })
    List<QbKnowledgeRelation> selectAll();

    @Insert("INSERT INTO knowledge_relation(source_id, target_id, relation_type, weight, confidence, source_type, description, created_at, updated_at, is_deleted) " +
            "VALUES(#{sourceId}, #{targetId}, #{relationType}, #{weight}, #{confidence}, #{sourceType}, #{description}, NOW(3), NOW(3), 0) " +
            "ON DUPLICATE KEY UPDATE weight=VALUES(weight), confidence=VALUES(confidence), source_type=VALUES(source_type), description=VALUES(description), updated_at=NOW(3), is_deleted=0")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(QbKnowledgeRelation relation);

    @Select("SELECT COUNT(1) FROM knowledge_relation WHERE source_id=#{sourceId} AND target_id=#{targetId} AND relation_type=#{relationType} AND is_deleted=0")
    long countActive(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId, @Param("relationType") String relationType);

    @Select("SELECT * FROM knowledge_relation WHERE id=#{id} AND is_deleted=0")
    QbKnowledgeRelation selectById(@Param("id") Long id);

    @Update("UPDATE knowledge_relation SET source_id=#{sourceId}, target_id=#{targetId}, relation_type=#{relationType}, weight=#{weight}, confidence=#{confidence}, source_type=#{sourceType}, description=#{description}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbKnowledgeRelation relation);

    @Update("UPDATE knowledge_relation SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);
}
