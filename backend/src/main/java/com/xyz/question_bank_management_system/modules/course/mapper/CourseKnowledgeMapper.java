package com.xyz.question_bank_management_system.modules.course.mapper;

import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseKnowledgeMapper {
    @Select("SELECT * FROM course_knowledge WHERE course_id=#{courseId} ORDER BY sequence_no,id")
    List<CourseKnowledge> selectByCourseId(@Param("courseId") Long courseId);

    @Delete("DELETE FROM course_knowledge WHERE course_id=#{courseId}")
    int deleteByCourseId(@Param("courseId") Long courseId);

    @Insert({"<script>","INSERT INTO course_knowledge(course_id,knowledge_point_id,sequence_no,is_core,coverage_weight,created_at) VALUES","<foreach collection='items' item='item' separator=','>","(#{item.courseId},#{item.knowledgePointId},#{item.sequenceNo},#{item.isCore},#{item.coverageWeight},NOW(3))","</foreach>","</script>"})
    int batchInsert(@Param("items") List<CourseKnowledge> items);

    @Select("SELECT COUNT(1) FROM course_knowledge WHERE course_id=#{courseId}")
    int countByCourseId(@Param("courseId") Long courseId);
}
