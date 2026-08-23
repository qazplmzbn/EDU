package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.dto.QuestionSearchQuery;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbQuestionMapper {

    @Insert("INSERT INTO qb_question(title, question_type, difficulty, chapter, stem, standard_answer, answer_format, analysis_text, analysis_source, analysis_llm_call_id, status, bank_review_status, bank_reviewer_id, bank_reviewed_at, bank_review_comment, created_by, created_at, updated_at, is_deleted) " +
            "VALUES(#{title}, #{questionType}, #{difficulty}, #{chapter}, #{stem}, #{standardAnswer}, #{answerFormat}, #{analysisText}, #{analysisSource}, #{analysisLlmCallId}, #{status}, #{bankReviewStatus}, #{bankReviewerId}, #{bankReviewedAt}, #{bankReviewComment}, #{createdBy}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbQuestion question);

    @Update("UPDATE qb_question SET title=#{title}, question_type=#{questionType}, difficulty=#{difficulty}, chapter=#{chapter}, stem=#{stem}, standard_answer=#{standardAnswer}, answer_format=#{answerFormat}, analysis_text=#{analysisText}, analysis_source=#{analysisSource}, analysis_llm_call_id=#{analysisLlmCallId}, status=#{status}, bank_review_status=#{bankReviewStatus}, bank_reviewer_id=#{bankReviewerId}, bank_reviewed_at=#{bankReviewedAt}, bank_review_comment=#{bankReviewComment}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbQuestion question);

    @Select("SELECT * FROM qb_question WHERE id=#{id} AND is_deleted=0")
    QbQuestion selectById(@Param("id") Long id);

    @Update("UPDATE qb_question SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE qb_question SET status=2, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int publish(@Param("id") Long id);

    @Update("UPDATE qb_question SET bank_review_status=#{bankReviewStatus}, bank_reviewer_id=#{bankReviewerId}, bank_reviewed_at=#{bankReviewedAt}, bank_review_comment=#{bankReviewComment}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int updateBankReview(QbQuestion question);

    /** 动态搜索（XML 实现） */
    List<QbQuestion> search(@Param("q") QuestionSearchQuery q, @Param("offset") long offset, @Param("size") long size);

    long count(@Param("q") QuestionSearchQuery q);

    List<QbQuestion> searchForPractice(@Param("tagIds") List<Long> tagIds,
                                       @Param("chapters") List<String> chapters,
                                       @Param("questionTypes") List<Integer> questionTypes,
                                       @Param("visibleTeacherIds") List<Long> visibleTeacherIds,
                                       @Param("limit") long limit);

    List<QbQuestion> selectPublishedByIds(@Param("questionIds") List<Long> questionIds,
                                          @Param("visibleTeacherIds") List<Long> visibleTeacherIds);
}
