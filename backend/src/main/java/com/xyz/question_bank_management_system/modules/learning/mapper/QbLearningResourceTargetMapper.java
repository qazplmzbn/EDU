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
            "SELECT DISTINCT r.*",
            "FROM qb_learning_resource_target rt",
            "JOIN qb_learning_resource r ON r.id = rt.resource_id AND r.is_deleted = 0",
            "LEFT JOIN qb_class_member cm ON cm.class_id = rt.class_id AND rt.target_type = 'class'",
            "WHERE (rt.target_type = 'student' AND rt.student_id = #{studentId})",
            "   OR (rt.target_type = 'class' AND cm.student_id = #{studentId})",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "LIMIT #{limit}"
    })
    List<QbLearningResource> selectResourcesByStudentId(@Param("studentId") Long studentId,
                                                        @Param("limit") int limit);
}
