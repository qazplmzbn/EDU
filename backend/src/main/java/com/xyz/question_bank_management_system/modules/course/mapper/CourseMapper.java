package com.xyz.question_bank_management_system.modules.course.mapper;

import com.xyz.question_bank_management_system.modules.course.entity.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseMapper {
    @Insert("INSERT INTO course(course_code,course_name,description,teacher_id,status,created_at,updated_at,is_deleted) VALUES(#{courseCode},#{courseName},#{description},#{teacherId},#{status},NOW(3),NOW(3),0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Update("UPDATE course SET course_code=#{courseCode},course_name=#{courseName},description=#{description},teacher_id=#{teacherId},status=#{status},updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(Course course);

    @Update("UPDATE course SET is_deleted=1,updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int softDelete(@Param("id") Long id);

    @Select("SELECT * FROM course WHERE id=#{id} AND is_deleted=0")
    Course selectById(@Param("id") Long id);

    @Select("SELECT * FROM course WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    Course selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM course WHERE course_code=#{courseCode} AND is_deleted=0 LIMIT 1")
    Course selectByCode(@Param("courseCode") String courseCode);

    @Select("SELECT * FROM course WHERE course_code=#{courseCode} AND id<>#{id} AND is_deleted=0 LIMIT 1")
    Course selectByCodeExcludeId(@Param("courseCode") String courseCode, @Param("id") Long id);

    @Select("SELECT * FROM course WHERE teacher_id=#{teacherId} AND is_deleted=0 ORDER BY updated_at DESC,id DESC")
    List<Course> listByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM course WHERE is_deleted=0 ORDER BY updated_at DESC,id DESC")
    List<Course> listAll();

    @Select("SELECT DISTINCT c.* FROM course c JOIN qb_class qc ON qc.teacher_id=c.teacher_id AND qc.is_deleted=0 JOIN qb_class_member m ON m.class_id=qc.id WHERE m.student_id=#{studentId} AND c.status='active' AND c.is_deleted=0 ORDER BY c.updated_at DESC,c.id DESC")
    List<Course> listVisibleForStudent(@Param("studentId") Long studentId);
}
