package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionTagLink;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbQuestionTagMapper {

    @Select("SELECT tag_id FROM qb_question_tag WHERE question_id=#{questionId}")
    List<Long> selectTagIdsByQuestionId(@Param("questionId") Long questionId);

    @Select("SELECT t.tag_name FROM qb_question_tag qt " +
            "JOIN qb_tag t ON t.id=qt.tag_id " +
            "WHERE qt.question_id=#{questionId} AND t.is_deleted=0 " +
            "ORDER BY t.sort_order ASC, t.id ASC")
    List<String> selectTagNamesByQuestionId(@Param("questionId") Long questionId);

    @Delete("DELETE FROM qb_question_tag WHERE question_id=#{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    @Select({
            "<script>",
            "SELECT question_id, tag_id",
            "FROM qb_question_tag",
            "WHERE question_id IN",
            "<foreach collection='questionIds' item='qid' open='(' close=')' separator=','>",
            "  #{qid}",
            "</foreach>",
            "</script>"
    })
    List<QbQuestionTagLink> selectLinksByQuestionIds(@Param("questionIds") List<Long> questionIds);

    int batchInsert(@Param("questionId") Long questionId, @Param("tagIds") List<Long> tagIds);
}
