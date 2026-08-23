package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbWrongQuestion;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbWrongQuestionMapper {

    @Insert("INSERT INTO qb_wrong_question(user_id, question_id, wrong_count, first_wrong_at, last_wrong_at, is_resolved) " +
            "VALUES(#{userId}, #{questionId}, 1, #{at}, #{at}, 0) " +
            "ON DUPLICATE KEY UPDATE wrong_count=wrong_count+1, last_wrong_at=#{at}, is_resolved=0")
    int upsertWrong(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("at") LocalDateTime at);

    @Update("UPDATE qb_wrong_question SET is_resolved=1, resolved_at=#{at} WHERE user_id=#{userId} AND question_id=#{questionId}")
    int resolve(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("at") LocalDateTime at);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM qb_wrong_question w",
            "JOIN qb_question q ON q.id = w.question_id AND q.is_deleted = 0",
            "WHERE w.user_id = #{userId}",
            "<if test='chapter != null and chapter != \"\"'>",
            "  AND q.chapter = #{chapter}",
            "</if>",
            "<if test='isResolved != null'>",
            "  AND w.is_resolved = #{isResolved}",
            "</if>",
            "<if test='tagId != null'>",
            "  AND EXISTS (",
            "    SELECT 1 FROM qb_question_tag qt",
            "    WHERE qt.question_id = w.question_id AND qt.tag_id = #{tagId}",
            "  )",
            "</if>",
            "</script>"
    })
    long countByFilter(@Param("userId") Long userId,
                       @Param("tagId") Long tagId,
                       @Param("chapter") String chapter,
                       @Param("isResolved") Integer isResolved);

    @Select({
            "<script>",
            "SELECT w.user_id, w.question_id, w.wrong_count, w.first_wrong_at, w.last_wrong_at, w.is_resolved, w.resolved_at",
            "FROM qb_wrong_question w",
            "JOIN qb_question q ON q.id = w.question_id AND q.is_deleted = 0",
            "WHERE w.user_id = #{userId}",
            "<if test='chapter != null and chapter != \"\"'>",
            "  AND q.chapter = #{chapter}",
            "</if>",
            "<if test='isResolved != null'>",
            "  AND w.is_resolved = #{isResolved}",
            "</if>",
            "<if test='tagId != null'>",
            "  AND EXISTS (",
            "    SELECT 1 FROM qb_question_tag qt",
            "    WHERE qt.question_id = w.question_id AND qt.tag_id = #{tagId}",
            "  )",
            "</if>",
            "ORDER BY w.last_wrong_at DESC, w.question_id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<QbWrongQuestion> pageByFilter(@Param("userId") Long userId,
                                       @Param("tagId") Long tagId,
                                       @Param("chapter") String chapter,
                                       @Param("isResolved") Integer isResolved,
                                       @Param("offset") long offset,
                                       @Param("size") long size);

    @Select({
            "<script>",
            "SELECT question_id",
            "FROM qb_wrong_question",
            "WHERE user_id = #{userId}",
            "  AND is_resolved = 0",
            "  AND question_id IN",
            "  <foreach collection='questionIds' item='qid' open='(' close=')' separator=','>",
            "    #{qid}",
            "  </foreach>",
            "</script>"
    })
    List<Long> selectUnresolvedQuestionIds(@Param("userId") Long userId,
                                           @Param("questionIds") List<Long> questionIds);
}
