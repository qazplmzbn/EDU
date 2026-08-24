package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbKnowledgePointMapper {

    @Select("SELECT * FROM knowledge_point WHERE is_deleted = 0 ORDER BY level ASC, id ASC")
    List<QbKnowledgePoint> selectAll();

    @Select("SELECT * FROM knowledge_point WHERE id = #{id} AND is_deleted = 0")
    QbKnowledgePoint selectById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT * FROM knowledge_point",
            "WHERE is_deleted = 0",
            "  AND id IN",
            "<foreach collection='knowledgePointIds' item='id' open='(' close=')' separator=','>",
            "#{id}",
            "</foreach>",
            "ORDER BY level ASC, id ASC",
            "</script>"
    })
    List<QbKnowledgePoint> selectByKnowledgePointIds(@Param("knowledgePointIds") List<Long> knowledgePointIds);

    @Insert("INSERT INTO knowledge_point(name,code,parent_id,level,knowledge_type,difficulty,description,created_at,updated_at,is_deleted) VALUES(#{name},#{code},#{parentId},#{level},'concept',3,#{description},NOW(3),NOW(3),0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbKnowledgePoint point);

    @Update("UPDATE knowledge_point SET name=#{name},code=#{code},parent_id=#{parentId},level=#{level},description=#{description},updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbKnowledgePoint point);

    @Update("UPDATE knowledge_point SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);

    @Select({
            "SELECT kp.*, COALESCE(sks.mastery_value, 0) AS mastery_value, COALESCE(sks.attempt_count, 0) AS attempt_count",
            "FROM knowledge_point kp",
            "LEFT JOIN student_knowledge_state sks ON sks.knowledge_point_id = kp.id AND sks.user_id = #{userId}",
            "WHERE kp.is_deleted = 0",
            "ORDER BY COALESCE(sks.mastery_value, 0) ASC, COALESCE(sks.attempt_count, 0) DESC, kp.level ASC, kp.id ASC",
            "LIMIT #{limit}"
    })
    List<QbKnowledgePoint> selectWeakest(@Param("userId") Long userId, @Param("limit") int limit);
}
