package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbLearningResourceMapper {

    @Select("SELECT COUNT(*) FROM resource_knowledge rk JOIN qb_learning_resource r ON r.id=rk.resource_id WHERE rk.knowledge_point_id=#{knowledgePointId} AND r.is_deleted=0")
    long countActiveByKnowledgePointId(@Param("knowledgePointId") Long knowledgePointId);

    @Select("SELECT r.* FROM qb_learning_resource r WHERE r.id = #{id} AND r.is_deleted = 0")
    QbLearningResource selectById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT r.*",
            "FROM qb_learning_resource r",
            "WHERE r.is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (r.title LIKE CONCAT('%', #{keyword}, '%') OR r.summary LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='knowledgePointId != null'>",
            "  AND EXISTS (SELECT 1 FROM resource_knowledge rk WHERE rk.resource_id=r.id AND rk.knowledge_point_id=#{knowledgePointId})",
            "</if>",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectList(@Param("keyword") String keyword, @Param("knowledgePointId") Long knowledgePointId, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT r.*",
            "FROM qb_learning_resource r",
            "WHERE r.is_deleted = 0 AND EXISTS (SELECT 1 FROM resource_knowledge rk WHERE rk.resource_id=r.id AND rk.knowledge_point_id IN",
            "<foreach collection='knowledgePointIds' item='id' open='(' close=')' separator=','>",
            "#{id}",
            "</foreach>",
            ")",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectByKnowledgePointIds(@Param("knowledgePointIds") List<Long> knowledgePointIds, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT r.*",
            "FROM qb_learning_resource r",
            "WHERE r.is_deleted = 0",
            "AND r.resource_type IN ('video', 'animated_explainer')",
            "<if test='(knowledgePointIds != null and knowledgePointIds.size() > 0) or (keywords != null and keywords.size() > 0)'>",
            "  AND (",
            "<if test='knowledgePointIds != null and knowledgePointIds.size() > 0'>",
            "  EXISTS (SELECT 1 FROM resource_knowledge rk WHERE rk.resource_id=r.id AND rk.knowledge_point_id IN",
            "  <foreach collection='knowledgePointIds' item='id' open='(' close=')' separator=','>",
            "  #{id}",
            "  </foreach>",
            "  )",
            "</if>",
            "<if test='knowledgePointIds != null and knowledgePointIds.size() > 0 and keywords != null and keywords.size() > 0'>",
            "  OR",
            "</if>",
            "<if test='keywords != null and keywords.size() > 0'>",
            "  <foreach collection='keywords' item='keyword' separator=' OR '>",
            "    r.title LIKE CONCAT('%', #{keyword}, '%')",
            "    OR r.summary LIKE CONCAT('%', #{keyword}, '%')",
            "    OR r.content LIKE CONCAT('%', #{keyword}, '%')",
            "  </foreach>",
            "</if>",
            "  )",
            "</if>",
            "ORDER BY",
            "  CASE WHEN r.audit_status = 'approved' THEN 0 WHEN r.audit_status = 'manual' THEN 1 ELSE 2 END,",
            "  r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectVideoCandidates(@Param("knowledgePointIds") List<Long> knowledgePointIds,
                                                   @Param("keywords") List<String> keywords,
                                                   @Param("limit") int limit);

    @Insert("INSERT INTO qb_learning_resource(title,resource_type,resource_purpose,url,summary,content,difficulty,generation_type,version,personalization_basis,review_report_json,model_source_json,audit_status,agent_task_id,created_by,created_at,updated_at,is_deleted) VALUES(#{title},#{resourceType},#{resourcePurpose},#{url},#{summary},#{content},#{difficulty},#{generationType},#{version},#{personalizationBasis},#{reviewReportJson},#{modelSourceJson},#{auditStatus},#{agentTaskId},#{createdBy},NOW(3),NOW(3),0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLearningResource resource);

    @Update("UPDATE qb_learning_resource SET title=#{title},resource_type=#{resourceType},resource_purpose=#{resourcePurpose},url=#{url},summary=#{summary},content=#{content},difficulty=#{difficulty},generation_type=#{generationType},version=#{version},personalization_basis=#{personalizationBasis},review_report_json=#{reviewReportJson},model_source_json=#{modelSourceJson},audit_status=#{auditStatus},agent_task_id=#{agentTaskId},updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbLearningResource resource);

    @Update("UPDATE qb_learning_resource SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);
}
