package com.xyz.question_bank_management_system.modules.bank.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbAssignmentTargetMapper {

    @Delete("DELETE FROM qb_assignment_target WHERE assignment_id=#{assignmentId}")
    int deleteByAssignmentId(@Param("assignmentId") Long assignmentId);

    int batchInsert(@Param("assignmentId") Long assignmentId, @Param("userIds") List<Long> userIds);

    @Select("SELECT COUNT(1) FROM qb_assignment_target WHERE assignment_id=#{assignmentId}")
    long countByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("SELECT COUNT(1) FROM qb_assignment_target WHERE assignment_id=#{assignmentId} AND user_id=#{userId}")
    long countByAssignmentAndUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM qb_assignment_target WHERE assignment_id=#{assignmentId} ORDER BY user_id ASC")
    List<Long> listUserIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select({
            "SELECT DISTINCT t.user_id",
            "FROM qb_assignment_target t",
            "JOIN qb_assignment a ON a.id = t.assignment_id AND a.is_deleted = 0",
            "WHERE a.created_by = #{teacherId}",
            "ORDER BY t.user_id ASC"
    })
    List<Long> listUserIdsByTeacherAssignments(@Param("teacherId") Long teacherId);
}
