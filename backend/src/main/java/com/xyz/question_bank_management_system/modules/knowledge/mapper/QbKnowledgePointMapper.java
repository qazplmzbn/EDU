package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbKnowledgePointMapper {

    @Select("SELECT kp.*, t.tag_name FROM qb_knowledge_point kp LEFT JOIN qb_tag t ON t.id = kp.tag_id AND t.is_deleted = 0 WHERE kp.is_deleted = 0 ORDER BY kp.sort_order ASC, kp.id ASC")
    List<QbKnowledgePoint> selectAll();

    @Select("SELECT * FROM qb_knowledge_point WHERE id = #{id} AND is_deleted = 0")
    QbKnowledgePoint selectById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT kp.*, t.tag_name",
            "FROM qb_knowledge_point kp",
            "LEFT JOIN qb_tag t ON t.id = kp.tag_id AND t.is_deleted = 0",
            "WHERE kp.is_deleted = 0",
            "  AND kp.tag_id IN",
            "<foreach collection='tagIds' item='id' open='(' close=')' separator=','>",
            "#{id}",
            "</foreach>",
            "ORDER BY kp.sort_order ASC, kp.id ASC",
            "</script>"
    })
    List<QbKnowledgePoint> selectByTagIds(@Param("tagIds") List<Long> tagIds);

    @Insert("INSERT INTO qb_knowledge_point(name, code, parent_id, tag_id, level, description, sort_order, created_at, updated_at, is_deleted) VALUES(#{name}, #{code}, #{parentId}, #{tagId}, #{level}, #{description}, #{sortOrder}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbKnowledgePoint point);

    @Update("UPDATE qb_knowledge_point SET name=#{name}, code=#{code}, parent_id=#{parentId}, tag_id=#{tagId}, level=#{level}, description=#{description}, sort_order=#{sortOrder}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbKnowledgePoint point);

    @Update("UPDATE qb_knowledge_point SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);

    @Select({
            "SELECT kp.*, t.tag_name, COALESCE(tm.mastery_value, 0) AS mastery_value, COALESCE(tm.attempt_count, 0) AS attempt_count",
            "FROM qb_knowledge_point kp",
            "LEFT JOIN qb_tag t ON t.id = kp.tag_id AND t.is_deleted = 0",
            "LEFT JOIN qb_tag_mastery tm ON tm.tag_id = kp.tag_id AND tm.user_id = #{userId}",
            "WHERE kp.is_deleted = 0",
            "ORDER BY COALESCE(tm.mastery_value, 0) ASC, COALESCE(tm.attempt_count, 0) DESC, kp.sort_order ASC, kp.id ASC",
            "LIMIT #{limit}"
    })
    List<QbKnowledgePoint> selectWeakest(@Param("userId") Long userId, @Param("limit") int limit);
}
