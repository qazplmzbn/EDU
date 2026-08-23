package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.QbTagMastery;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbTagMasteryMapper {

    @Insert("INSERT INTO qb_tag_mastery(user_id, tag_id, mastery_value, correct_count, attempt_count, updated_at) " +
            "VALUES(#{userId}, #{tagId}, #{initMastery}, #{correctInc}, 1, NOW(3)) " +
            "ON DUPLICATE KEY UPDATE correct_count=correct_count+#{correctInc}, attempt_count=attempt_count+1, mastery_value=(correct_count+#{correctInc})/(attempt_count+1), updated_at=NOW(3)")
    int upsertAttempt(@Param("userId") Long userId, @Param("tagId") Long tagId, @Param("correctInc") int correctInc, @Param("initMastery") double initMastery);

    @Select({
            "<script>",
            "SELECT tm.user_id, tm.tag_id, tm.mastery_value, tm.correct_count, tm.attempt_count, tm.updated_at,",
            "       t.tag_name",
            "FROM qb_tag_mastery tm",
            "JOIN qb_tag t ON t.id = tm.tag_id AND t.is_deleted = 0",
            "WHERE tm.user_id = #{userId}",
            "<if test='tagType != null'>",
            "  AND t.tag_type = #{tagType}",
            "</if>",
            "ORDER BY tm.updated_at DESC, tm.tag_id DESC",
            "</script>"
    })
    List<QbTagMastery> selectByUserIdAndTagType(@Param("userId") Long userId, @Param("tagType") Integer tagType);
}
