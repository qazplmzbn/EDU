package com.xyz.question_bank_management_system.modules.course.mapper;

import com.xyz.question_bank_management_system.modules.course.entity.CourseChapter;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CourseChapterMapper {
    @Insert("INSERT INTO course_chapter(course_id,chapter_code,chapter_name,order_no,status,created_at,updated_at) VALUES(#{courseId},#{chapterCode},#{chapterName},#{orderNo},#{status},NOW(3),NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(CourseChapter value);
    @Select("SELECT * FROM course_chapter WHERE course_id=#{courseId} AND chapter_code=#{code} LIMIT 1") CourseChapter selectByCode(@Param("courseId")Long courseId,@Param("code")String code);
    @Select("SELECT * FROM course_chapter WHERE course_id=#{courseId} ORDER BY order_no,id") List<CourseChapter> selectByCourse(Long courseId);
    @Select("SELECT * FROM course_chapter WHERE id=#{id} AND course_id=#{courseId}") CourseChapter selectByIdAndCourse(@Param("id") Long id,@Param("courseId") Long courseId);
}
