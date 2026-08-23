package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionUserStat;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbQuestionUserStatMapper {

    @Insert("INSERT INTO qb_question_user_stat(user_id, question_id, attempt_count, correct_count, last_attempt_at) " +
            "VALUES(#{userId}, #{questionId}, 1, #{correctInc}, #{at}) " +
            "ON DUPLICATE KEY UPDATE attempt_count=attempt_count+1, correct_count=correct_count+#{correctInc}, last_attempt_at=#{at}")
    int upsert(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("correctInc") int correctInc, @Param("at") LocalDateTime at);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM qb_question_user_stat",
            "WHERE user_id = #{userId}",
            "<if test='questionId != null'>",
            "  AND question_id = #{questionId}",
            "</if>",
            "</script>"
    })
    long countByFilter(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Select({
            "<script>",
            "SELECT user_id, question_id, attempt_count, correct_count, last_attempt_at",
            "FROM qb_question_user_stat",
            "WHERE user_id = #{userId}",
            "<if test='questionId != null'>",
            "  AND question_id = #{questionId}",
            "</if>",
            "ORDER BY last_attempt_at DESC, question_id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<QbQuestionUserStat> pageByFilter(@Param("userId") Long userId,
                                          @Param("questionId") Long questionId,
                                          @Param("offset") long offset,
                                          @Param("size") long size);

    @Select({
            "<script>",
            "SELECT user_id, question_id, attempt_count, correct_count, last_attempt_at",
            "FROM qb_question_user_stat",
            "WHERE user_id = #{userId}",
            "  AND question_id IN",
            "  <foreach collection='questionIds' item='qid' open='(' close=')' separator=','>",
            "    #{qid}",
            "  </foreach>",
            "</script>"
    })
    List<QbQuestionUserStat> selectByUserIdAndQuestionIds(@Param("userId") Long userId,
                                                          @Param("questionIds") List<Long> questionIds);
}
