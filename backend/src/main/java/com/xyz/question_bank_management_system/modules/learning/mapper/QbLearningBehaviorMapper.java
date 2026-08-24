package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbLearningBehaviorMapper {

    @Insert("INSERT INTO qb_learning_behavior(user_id, behavior_type, ref_id, knowledge_point_id, tag_id, duration_seconds, note, created_at) VALUES(#{userId}, #{behaviorType}, #{refId}, #{knowledgePointId}, #{tagId}, #{durationSeconds}, #{note}, NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLearningBehavior behavior);

    @Select("SELECT COUNT(1) FROM qb_learning_behavior WHERE user_id=#{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(duration_seconds), 0) FROM qb_learning_behavior WHERE user_id=#{userId}")
    long sumDurationByUserId(@Param("userId") Long userId);

    @Select({
            "SELECT b.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_behavior b",
            "LEFT JOIN knowledge_point kp ON kp.id = b.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = b.tag_id AND t.is_deleted = 0",
            "WHERE b.user_id = #{userId}",
            "ORDER BY b.created_at DESC, b.id DESC",
            "LIMIT #{limit}"
    })
    List<QbLearningBehavior> selectRecent(@Param("userId") Long userId, @Param("limit") int limit);
}
