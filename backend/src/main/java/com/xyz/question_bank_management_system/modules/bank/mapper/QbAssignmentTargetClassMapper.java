package com.xyz.question_bank_management_system.modules.bank.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QbAssignmentTargetClassMapper {

    @Delete("DELETE FROM qb_assignment_target_class WHERE assignment_id=#{assignmentId}")
    int deleteByAssignmentId(@Param("assignmentId") Long assignmentId);

    int batchInsert(@Param("assignmentId") Long assignmentId, @Param("classIds") List<Long> classIds);

    @Select("SELECT COUNT(1) FROM qb_assignment_target_class WHERE assignment_id=#{assignmentId}")
    long countByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("SELECT COUNT(1) FROM qb_assignment_target_class tc JOIN qb_class_member cm ON cm.class_id=tc.class_id WHERE tc.assignment_id=#{assignmentId} AND cm.student_id=#{studentId}")
    long countByAssignmentAndStudent(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    @Select("SELECT class_id FROM qb_assignment_target_class WHERE assignment_id=#{assignmentId} ORDER BY class_id ASC")
    List<Long> listClassIdsByAssignmentId(@Param("assignmentId") Long assignmentId);
}
