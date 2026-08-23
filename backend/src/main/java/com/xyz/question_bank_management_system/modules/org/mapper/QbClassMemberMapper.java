package com.xyz.question_bank_management_system.modules.org.mapper;

import com.xyz.question_bank_management_system.modules.org.vo.ClassStudentItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface QbClassMemberMapper {

    @Insert("INSERT INTO qb_class_member(class_id, student_id, joined_at) VALUES(#{classId}, #{studentId}, NOW(3))")
    int insert(@Param("classId") Long classId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(1) FROM qb_class_member WHERE class_id=#{classId} AND student_id=#{studentId}")
    long countByClassAndStudent(@Param("classId") Long classId, @Param("studentId") Long studentId);

    @Select("SELECT m.student_id, u.username, u.display_name, u.email, m.joined_at " +
            "FROM qb_class_member m " +
            "JOIN sys_user u ON u.id=m.student_id AND u.is_deleted=0 " +
            "WHERE m.class_id=#{classId} " +
            "ORDER BY m.joined_at DESC, m.student_id DESC")
    List<ClassStudentItemVO> listStudentsByClassId(@Param("classId") Long classId);

    @Select("SELECT DISTINCT c.teacher_id " +
            "FROM qb_class_member m " +
            "JOIN qb_class c ON c.id=m.class_id AND c.is_deleted=0 " +
            "WHERE m.student_id=#{studentId}")
    List<Long> listTeacherIdsByStudentId(@Param("studentId") Long studentId);

    @Select({
            "<script>",
            "SELECT DISTINCT cm.student_id",
            "FROM qb_class_member cm",
            "WHERE cm.class_id IN",
            "<foreach collection='classIds' item='classId' open='(' close=')' separator=','>",
            "  #{classId}",
            "</foreach>",
            "ORDER BY cm.student_id ASC",
            "</script>"
    })
    List<Long> listStudentIdsByClassIds(@Param("classIds") List<Long> classIds);

    @Select("SELECT DISTINCT cm.student_id " +
            "FROM qb_class_member cm " +
            "JOIN qb_class c ON c.id=cm.class_id AND c.is_deleted=0 " +
            "WHERE c.teacher_id=#{teacherId} " +
            "ORDER BY cm.student_id ASC")
    List<Long> listStudentIdsByTeacherId(@Param("teacherId") Long teacherId);

    @Delete("DELETE FROM qb_class_member WHERE class_id=#{classId} AND student_id=#{studentId}")
    int removeByClassAndStudent(@Param("classId") Long classId, @Param("studentId") Long studentId);
}
