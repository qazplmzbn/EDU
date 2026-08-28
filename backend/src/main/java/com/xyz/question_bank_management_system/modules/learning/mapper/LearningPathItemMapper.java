package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningPathItemMapper {
    @Insert({"<script>","INSERT INTO learning_path_item(path_id,order_no,item_type,knowledge_point_id,resource_id,question_id,assignment_id,planned_start_at,planned_end_at,status,decision_reason,created_at) VALUES","<foreach collection='items' item='item' separator=','>","(#{item.pathId},#{item.orderNo},#{item.itemType},#{item.knowledgePointId},#{item.resourceId},#{item.questionId},#{item.assignmentId},#{item.plannedStartAt},#{item.plannedEndAt},#{item.status},#{item.decisionReason},NOW(3))","</foreach>","</script>"})
    int batchInsert(@Param("items") List<LearningPathItem> items);

    @Select("SELECT * FROM learning_path_item WHERE path_id=#{pathId} ORDER BY order_no,id")
    List<LearningPathItem> selectByPathId(@Param("pathId") Long pathId);

    @Select("SELECT * FROM learning_path_item WHERE id=#{itemId} AND path_id=#{pathId}")
    LearningPathItem selectByIdAndPathId(@Param("itemId") Long itemId, @Param("pathId") Long pathId);

    @Update("UPDATE learning_path_item SET status='completed' WHERE id=#{itemId} AND path_id=#{pathId} AND status<>'completed'")
    int complete(@Param("pathId") Long pathId, @Param("itemId") Long itemId);

    @Select("SELECT COUNT(DISTINCT i.knowledge_point_id) FROM learning_path_item i JOIN course_knowledge ck ON ck.course_id=#{courseId} AND ck.knowledge_point_id=i.knowledge_point_id WHERE i.path_id=#{pathId} AND i.item_type='knowledge' AND i.status='completed'")
    int countCompletedCourseKnowledge(@Param("pathId") Long pathId, @Param("courseId") Long courseId);
}
