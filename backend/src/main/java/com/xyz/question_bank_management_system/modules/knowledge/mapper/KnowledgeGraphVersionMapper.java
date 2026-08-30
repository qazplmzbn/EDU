package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeGraphVersion;
import org.apache.ibatis.annotations.*;

@Mapper
public interface KnowledgeGraphVersionMapper {
    @Insert("INSERT INTO knowledge_graph_version(version_code,course_id,description,status,node_count,edge_count,correlation_id,created_by,import_id,review_status,reviewed_by,reviewed_at,created_at,updated_at) VALUES(#{versionCode},#{courseId},#{description},#{status},#{nodeCount},#{edgeCount},#{correlationId},#{createdBy},#{importId},#{reviewStatus},#{reviewedBy},#{reviewedAt},NOW(3),NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id") int insert(KnowledgeGraphVersion value);
    @Select("SELECT * FROM knowledge_graph_version WHERE version_code=#{code} LIMIT 1") KnowledgeGraphVersion selectByCode(String code);
    @Select("SELECT * FROM knowledge_graph_version WHERE version_code=#{code} LIMIT 1 FOR UPDATE") KnowledgeGraphVersion selectByCodeForUpdate(String code);
    @Select("SELECT * FROM knowledge_graph_version WHERE course_id=#{courseId} AND status='ACTIVE' ORDER BY activated_at DESC,id DESC LIMIT 1") KnowledgeGraphVersion selectActive(Long courseId);
    @Select("SELECT * FROM knowledge_graph_version WHERE import_id=#{importId} LIMIT 1") KnowledgeGraphVersion selectByImportId(Long importId);
    @Update("UPDATE knowledge_graph_version SET status=#{status},node_count=#{nodeCount},edge_count=#{edgeCount},content_hash=#{contentHash},validation_report_json=#{validationReportJson},updated_at=NOW(3) WHERE id=#{id}") int updateValidation(KnowledgeGraphVersion value);
    @Update("UPDATE knowledge_graph_version SET status='ARCHIVED',updated_at=NOW(3) WHERE course_id=#{courseId} AND status='ACTIVE'") int archiveActive(Long courseId);
    @Update("UPDATE knowledge_graph_version SET status='ACTIVE',activated_at=NOW(3),updated_at=NOW(3) WHERE id=#{id} AND status='VALIDATING'") int activate(Long id);
    @Update("UPDATE knowledge_graph_version SET review_status='APPROVED',reviewed_by=#{reviewerId},reviewed_at=NOW(3),updated_at=NOW(3) WHERE id=#{id}") int approveReview(@Param("id")Long id,@Param("reviewerId")Long reviewerId);
    @Update("UPDATE knowledge_graph_version SET status='DRAFT',updated_at=NOW(3) WHERE id=#{id} AND status='REJECTED'") int resetRejectedToDraft(Long id);
}
