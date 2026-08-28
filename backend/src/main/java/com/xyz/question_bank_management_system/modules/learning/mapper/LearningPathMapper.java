package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningPathMapper {
    @Insert("INSERT INTO learning_path(user_id,course_id,goal_id,target_occupation_id,profile_snapshot_id,title,stage,planning_days,version,status,summary_text,generated_by_agent_task_id,created_at,updated_at,is_deleted) VALUES(#{userId},#{courseId},#{goalId},#{targetOccupationId},#{profileSnapshotId},#{title},#{stage},#{planningDays},#{version},#{status},#{summaryText},#{generatedByAgentTaskId},NOW(3),NOW(3),0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningPath path);

    @Select("SELECT * FROM learning_path WHERE user_id=#{userId} AND course_id=#{courseId} AND status='active' AND is_deleted=0 ORDER BY version DESC,id DESC LIMIT 1")
    LearningPath selectActiveByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Update("UPDATE learning_path SET status='obsolete',updated_at=NOW(3) WHERE user_id=#{userId} AND course_id=#{courseId} AND status='active' AND is_deleted=0")
    int obsoleteActiveByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Select("SELECT * FROM learning_path WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    LearningPath selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM learning_path WHERE user_id=#{userId} AND is_deleted=0 ORDER BY created_at DESC,id DESC")
    List<LearningPath> listByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM learning_path WHERE id=#{id} AND is_deleted=0")
    LearningPath selectById(@Param("id") Long id);
}
