package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbGradingRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbGradingRecordMapper {

    @Insert("INSERT INTO qb_grading_record(answer_id, grading_mode, score, detail_json, llm_call_id, confidence, needs_review, reviewer_id, review_comment, is_final, created_at) " +
            "VALUES(#{answerId}, #{gradingMode}, #{score}, #{detailJson}, #{llmCallId}, #{confidence}, #{needsReview}, #{reviewerId}, #{reviewComment}, #{isFinal}, NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbGradingRecord record);

    @Select("SELECT * FROM qb_grading_record WHERE answer_id=#{answerId} ORDER BY created_at ASC")
    List<QbGradingRecord> selectByAnswerId(@Param("answerId") Long answerId);

    @Select({
            "<script>",
            "SELECT gr.*",
            "FROM qb_grading_record gr",
            "JOIN qb_answer a ON a.id = gr.answer_id",
            "JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "WHERE a.user_id = #{userId}",
            "  AND atp.status IN (2,3,4)",
            "  AND gr.is_final = 1",
            "<if test='attemptType != null'>",
            "  AND atp.attempt_type = #{attemptType}",
            "</if>",
            "ORDER BY gr.created_at DESC, gr.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbGradingRecord> selectRecentFinalByUser(@Param("userId") Long userId,
                                                  @Param("attemptType") Integer attemptType,
                                                  @Param("limit") int limit);
}
