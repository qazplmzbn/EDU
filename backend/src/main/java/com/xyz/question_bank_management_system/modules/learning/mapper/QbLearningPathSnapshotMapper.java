package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningPathSnapshot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QbLearningPathSnapshotMapper {

    @Insert("INSERT INTO qb_learning_path_snapshot(user_id, stage, goal, days, title, summary_text, snapshot_json, snapshot_hash, created_at, updated_at, is_deleted) " +
            "VALUES(#{userId}, #{stage}, #{goal}, #{days}, #{title}, #{summaryText}, #{snapshotJson}, #{snapshotHash}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLearningPathSnapshot snapshot);

    @Select("SELECT * FROM qb_learning_path_snapshot WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    QbLearningPathSnapshot selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM qb_learning_path_snapshot WHERE user_id=#{userId} AND is_deleted=0 ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<QbLearningPathSnapshot> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
