package com.xyz.question_bank_management_system.modules.bank.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbAssignmentTargetMapper {

    @Delete("DELETE FROM qb_assignment_target WHERE assignment_id=#{assignmentId}")
    int deleteByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Delete("DELETE FROM qb_assignment_target WHERE assignment_id=#{assignmentId} AND target_type='student' AND student_id=#{studentId}")
    int deleteStudentTarget(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    int batchInsertStudents(@Param("assignmentId") Long assignmentId, @Param("studentIds") List<Long> studentIds);

    int batchInsertClasses(@Param("assignmentId") Long assignmentId, @Param("classIds") List<Long> classIds);

    @Select("SELECT COUNT(1) FROM qb_assignment_target WHERE assignment_id=#{assignmentId}")
    long countByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select({
            "SELECT COUNT(1)",
            "FROM qb_assignment_target t",
            "WHERE t.assignment_id=#{assignmentId}",
            "  AND (",
            "    (t.target_type='student' AND t.student_id=#{studentId})",
            "    OR (t.target_type='class' AND EXISTS (",
            "      SELECT 1 FROM qb_class_member cm",
            "      WHERE cm.class_id=t.class_id AND cm.student_id=#{studentId}",
            "    ))",
            "  )"
    })
    long countEligibleStudent(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    @Select("SELECT student_id FROM qb_assignment_target WHERE assignment_id=#{assignmentId} AND target_type='student' ORDER BY student_id ASC")
    List<Long> listStudentIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("SELECT class_id FROM qb_assignment_target WHERE assignment_id=#{assignmentId} AND target_type='class' ORDER BY class_id ASC")
    List<Long> listClassIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select({
            "SELECT DISTINCT target_student_id FROM (",
            "  SELECT student_id AS target_student_id",
            "  FROM qb_assignment_target",
            "  WHERE assignment_id=#{assignmentId} AND target_type='student'",
            "  UNION",
            "  SELECT cm.student_id AS target_student_id",
            "  FROM qb_assignment_target t",
            "  JOIN qb_class_member cm ON cm.class_id=t.class_id",
            "  WHERE t.assignment_id=#{assignmentId} AND t.target_type='class'",
            ") target_students",
            "ORDER BY target_student_id ASC"
    })
    List<Long> listEligibleStudentIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select({
            "SELECT DISTINCT target_student_id FROM (",
            "  SELECT t.student_id AS target_student_id",
            "  FROM qb_assignment_target t",
            "JOIN qb_assignment a ON a.id = t.assignment_id AND a.is_deleted = 0",
            "WHERE a.created_by = #{teacherId} AND t.target_type='student'",
            "  UNION",
            "  SELECT cm.student_id AS target_student_id",
            "  FROM qb_assignment_target t",
            "  JOIN qb_assignment a ON a.id=t.assignment_id AND a.is_deleted=0",
            "  JOIN qb_class_member cm ON cm.class_id=t.class_id",
            "  WHERE a.created_by=#{teacherId} AND t.target_type='class'",
            ") target_students ORDER BY target_student_id ASC"
    })
    List<Long> listStudentIdsByTeacherAssignments(@Param("teacherId") Long teacherId);
}
