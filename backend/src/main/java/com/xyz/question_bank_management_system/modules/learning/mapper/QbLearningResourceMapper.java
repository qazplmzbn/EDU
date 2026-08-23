package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbLearningResourceMapper {

    @Select("SELECT r.*, kp.name AS knowledge_point_name, t.tag_name " +
            "FROM qb_learning_resource r " +
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0 " +
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0 " +
            "WHERE r.id = #{id} AND r.is_deleted = 0")
    QbLearningResource selectById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT r.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_resource r",
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0",
            "WHERE r.is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (r.title LIKE CONCAT('%', #{keyword}, '%') OR r.summary LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='knowledgePointId != null'>",
            "  AND r.knowledge_point_id = #{knowledgePointId}",
            "</if>",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectList(@Param("keyword") String keyword, @Param("knowledgePointId") Long knowledgePointId, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT r.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_resource r",
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0",
            "WHERE r.is_deleted = 0 AND r.knowledge_point_id IN",
            "<foreach collection='knowledgePointIds' item='id' open='(' close=')' separator=','>",
            "#{id}",
            "</foreach>",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectByKnowledgePointIds(@Param("knowledgePointIds") List<Long> knowledgePointIds, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT r.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_resource r",
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0",
            "WHERE r.is_deleted = 0 AND r.tag_id IN",
            "<foreach collection='tagIds' item='id' open='(' close=')' separator=','>",
            "#{id}",
            "</foreach>",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbLearningResource> selectByTagIds(@Param("tagIds") List<Long> tagIds, @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT r.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_resource r",
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0",
            "WHERE r.is_deleted = 0",
            "AND r.resource_type IN ('video', 'animated_explainer')",
            "<if test='(tagIds != null and tagIds.size() > 0) or (keywords != null and keywords.size() > 0)'>",
            "  AND (",
            "<if test='tagIds != null and tagIds.size() > 0'>",
            "  r.tag_id IN",
            "  <foreach collection='tagIds' item='id' open='(' close=')' separator=','>",
            "  #{id}",
            "  </foreach>",
            "</if>",
            "<if test='tagIds != null and tagIds.size() > 0 and keywords != null and keywords.size() > 0'>",
            "  OR",
            "</if>",
            "<if test='keywords != null and keywords.size() > 0'>",
            "  <foreach collection='keywords' item='keyword' separator=' OR '>",
            "    r.title LIKE CONCAT('%', #{keyword}, '%')",
            "    OR r.summary LIKE CONCAT('%', #{keyword}, '%')",
            "    OR t.tag_name LIKE CONCAT('%', #{keyword}, '%')",
            "    OR kp.name LIKE CONCAT('%', #{keyword}, '%')",
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
    List<QbLearningResource> selectVideoCandidates(@Param("tagIds") List<Long> tagIds,
                                                   @Param("keywords") List<String> keywords,
                                                   @Param("limit") int limit);

    @Insert("INSERT INTO qb_learning_resource(title, resource_type, url, summary, content, personalization_basis, review_report_json, model_source_json, audit_status, knowledge_point_id, tag_id, created_by, created_at, updated_at, is_deleted) VALUES(#{title}, #{resourceType}, #{url}, #{summary}, #{content}, #{personalizationBasis}, #{reviewReportJson}, #{modelSourceJson}, #{auditStatus}, #{knowledgePointId}, #{tagId}, #{createdBy}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLearningResource resource);

    @Update("UPDATE qb_learning_resource SET title=#{title}, resource_type=#{resourceType}, url=#{url}, summary=#{summary}, content=#{content}, personalization_basis=#{personalizationBasis}, review_report_json=#{reviewReportJson}, model_source_json=#{modelSourceJson}, audit_status=#{auditStatus}, knowledge_point_id=#{knowledgePointId}, tag_id=#{tagId}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbLearningResource resource);

    @Update("UPDATE qb_learning_resource SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);
}
