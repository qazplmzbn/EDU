package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResourceTarget;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QbLearningResourceTargetMapper {

    @Insert({
            "<script>",
            "INSERT IGNORE INTO qb_learning_resource_target(resource_id, student_id, class_id, target_type, created_by, created_at) VALUES",
            "<foreach collection='targets' item='target' separator=','>",
            "(#{target.resourceId}, #{target.studentId}, #{target.classId}, #{target.targetType}, #{target.createdBy}, NOW(3))",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("targets") List<QbLearningResourceTarget> targets);

    @Select({
            "SELECT DISTINCT r.*, kp.name AS knowledge_point_name, t.tag_name",
            "FROM qb_learning_resource_target rt",
            "JOIN qb_learning_resource r ON r.id = rt.resource_id AND r.is_deleted = 0",
            "LEFT JOIN qb_knowledge_point kp ON kp.id = r.knowledge_point_id AND kp.is_deleted = 0",
            "LEFT JOIN qb_tag t ON t.id = r.tag_id AND t.is_deleted = 0",
            "WHERE rt.student_id = #{studentId}",
            "ORDER BY rt.created_at DESC, r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}"
    })
    List<QbLearningResource> selectResourcesByStudentId(@Param("studentId") Long studentId,
                                                        @Param("limit") int limit);
}
