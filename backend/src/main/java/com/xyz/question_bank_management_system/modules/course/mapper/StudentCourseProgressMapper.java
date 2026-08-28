package com.xyz.question_bank_management_system.modules.course.mapper;

import com.xyz.question_bank_management_system.modules.course.entity.StudentCourseProgress;
import com.xyz.question_bank_management_system.modules.course.vo.CourseStudentProgressVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudentCourseProgressMapper {
    @Select("SELECT * FROM student_course_progress WHERE user_id=#{userId} AND course_id=#{courseId} LIMIT 1")
    StudentCourseProgress selectByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Insert("INSERT INTO student_course_progress(user_id,course_id,progress_rate,completed_knowledge_count,total_knowledge_count,status,last_learning_at,updated_at) VALUES(#{userId},#{courseId},#{progressRate},#{completedKnowledgeCount},#{totalKnowledgeCount},#{status},#{lastLearningAt},NOW(3)) ON DUPLICATE KEY UPDATE progress_rate=VALUES(progress_rate),completed_knowledge_count=VALUES(completed_knowledge_count),total_knowledge_count=VALUES(total_knowledge_count),status=VALUES(status),last_learning_at=VALUES(last_learning_at),updated_at=NOW(3)")
    int upsert(StudentCourseProgress progress);

    @Select("SELECT p.*,u.username,u.display_name FROM student_course_progress p JOIN sys_user u ON u.id=p.user_id AND u.is_deleted=0 WHERE p.course_id=#{courseId} ORDER BY p.progress_rate DESC,p.updated_at DESC,p.user_id")
    List<CourseStudentProgressVO> listByCourseId(@Param("courseId") Long courseId);
}
