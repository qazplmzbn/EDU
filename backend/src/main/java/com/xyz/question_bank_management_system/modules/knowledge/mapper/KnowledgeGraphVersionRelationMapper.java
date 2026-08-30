package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeGraphVersionRelation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface KnowledgeGraphVersionRelationMapper {
    @Delete("DELETE FROM knowledge_relation_source WHERE relation_id IN (SELECT id FROM knowledge_relation WHERE graph_version_id=#{versionId})") int deleteEvidence(Long versionId);
    @Delete("DELETE FROM knowledge_relation WHERE graph_version_id=#{versionId}") int deleteByVersion(Long versionId);
    @Insert({"<script>","INSERT INTO knowledge_relation(course_id,graph_version_id,relation_code,source_knowledge_point_id,target_knowledge_point_id,source_id,target_id,relation_type,weight,confidence,source_type,status,created_by,created_at,updated_at,is_deleted) VALUES","<foreach collection='rows' item='r' separator=','>","(#{r.courseId},#{r.graphVersionId},#{r.relationCode},#{r.sourceKnowledgePointId},#{r.targetKnowledgePointId},#{r.sourceKnowledgePointId},#{r.targetKnowledgePointId},#{r.relationType},#{r.weight},#{r.confidence},#{r.sourceType},'DRAFT',#{r.createdBy},NOW(3),NOW(3),0)","</foreach>","</script>"})
    int batchInsert(@Param("rows") List<KnowledgeGraphVersionRelation> rows);
    @Select("SELECT * FROM knowledge_relation WHERE graph_version_id=#{versionId} AND is_deleted=0 ORDER BY relation_code,id") List<KnowledgeGraphVersionRelation> selectByVersion(Long versionId);
    @Insert("INSERT INTO knowledge_relation_source(relation_id,source_chunk_id,evidence_type,created_at) SELECT r.id,#{sourceChunkId},'GRAPH_RELATION',NOW(3) FROM knowledge_relation r WHERE r.graph_version_id=#{versionId} AND r.relation_code=#{relationCode}") int insertEvidence(@Param("versionId") Long versionId,@Param("relationCode") String relationCode,@Param("sourceChunkId") Long sourceChunkId);
    @Insert("INSERT INTO knowledge_relation_source(relation_id,source_chunk_id,evidence_type,citation_text,confidence,created_at) SELECT r.id,#{sourceChunkId},#{evidenceType},#{citationText},#{confidence},NOW(3) FROM knowledge_relation r WHERE r.graph_version_id=#{versionId} AND r.relation_code=#{relationCode}") int insertEvidenceTyped(@Param("versionId")Long versionId,@Param("relationCode")String relationCode,@Param("sourceChunkId")Long sourceChunkId,@Param("evidenceType")String evidenceType,@Param("citationText")String citationText,@Param("confidence")java.math.BigDecimal confidence);
    @Select("SELECT COUNT(*) FROM knowledge_relation_source WHERE relation_id=#{relationId}") int countEvidence(Long relationId);
    @Update("UPDATE knowledge_relation SET status='ACTIVE',published_at=NOW(3),updated_at=NOW(3) WHERE graph_version_id=#{versionId}") int publishVersionRelations(Long versionId);
}
