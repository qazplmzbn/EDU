package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.QbAbilitySample;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAnswer;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherReviewAnswerItemVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbAnswerMapper {

    @Insert("INSERT INTO qb_answer(attempt_id, attempt_question_id, question_id, user_id, answer_content, answer_format, answer_status, auto_score, final_score, is_correct) " +
            "VALUES(#{attemptId}, #{attemptQuestionId}, #{questionId}, #{userId}, #{answerContent}, #{answerFormat}, #{answerStatus}, #{autoScore}, #{finalScore}, #{isCorrect})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbAnswer answer);

    @Select("SELECT * FROM qb_answer WHERE id=#{id}")
    QbAnswer selectById(@Param("id") Long id);

    @Select("SELECT * FROM qb_answer WHERE attempt_id=#{attemptId} ORDER BY attempt_question_id ASC")
    List<QbAnswer> selectByAttemptId(@Param("attemptId") Long attemptId);

    @Update("UPDATE qb_answer SET answer_content=#{answerContent}, answer_status=1 WHERE id=#{id}")
    int updateDraft(@Param("id") Long id, @Param("answerContent") String answerContent);

    @Update("UPDATE qb_answer SET answer_status=2, answered_at=#{answeredAt} WHERE attempt_id=#{attemptId}")
    int submitAllByAttempt(@Param("attemptId") Long attemptId, @Param("answeredAt") LocalDateTime answeredAt);

    @Update("UPDATE qb_answer SET answer_content=#{answerContent}, answer_status=2, answered_at=#{answeredAt} WHERE id=#{id}")
    int submitOne(@Param("id") Long id,
                  @Param("answerContent") String answerContent,
                  @Param("answeredAt") LocalDateTime answeredAt);

    @Update("UPDATE qb_answer SET auto_score=#{autoScore}, final_score=#{finalScore}, is_correct=#{isCorrect}, graded_at=#{gradedAt} WHERE id=#{id}")
    int updateScoring(@Param("id") Long id,
                      @Param("autoScore") Integer autoScore,
                      @Param("finalScore") Integer finalScore,
                      @Param("isCorrect") Integer isCorrect,
                      @Param("gradedAt") LocalDateTime gradedAt);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_answer a",
            "JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "LEFT JOIN qb_assignment ass ON ass.id = atp.assignment_id",
            "LEFT JOIN qb_attempt_question aq ON aq.id = a.attempt_question_id",
            "LEFT JOIN qb_grading_record gr ON gr.id = (",
            "  SELECT g2.id FROM qb_grading_record g2",
            "  WHERE g2.answer_id = a.id ORDER BY g2.id DESC LIMIT 1",
            ")",
            "WHERE 1=1",
            "<if test='ownerId != null'>",
            "  AND ass.created_by = #{ownerId}",
            "</if>",
            "<if test='assignmentId != null'>",
            "  AND atp.assignment_id = #{assignmentId}",
            "</if>",
            "<if test='studentId != null'>",
            "  AND a.user_id = #{studentId}",
            "</if>",
            "<if test='questionType != null'>",
            "  AND aq.question_type = #{questionType}",
            "</if>",
            "<if test='needsReview != null'>",
            "  AND COALESCE(gr.needs_review, 0) = #{needsReview}",
            "</if>",
            "</script>"
    })
    long countTeacherReview(@Param("assignmentId") Long assignmentId,
                            @Param("studentId") Long studentId,
                            @Param("questionType") Integer questionType,
                            @Param("needsReview") Integer needsReview,
                            @Param("ownerId") Long ownerId);

    @Select({
            "SELECT COUNT(1)",
            "FROM qb_answer a",
            "LEFT JOIN qb_grading_record gr ON gr.id = (",
            "  SELECT g2.id FROM qb_grading_record g2",
            "  WHERE g2.answer_id = a.id ORDER BY g2.id DESC LIMIT 1",
            ")",
            "WHERE a.attempt_id = #{attemptId}",
            "  AND COALESCE(gr.needs_review, 0) = 1"
    })
    long countPendingReviewByAttemptId(@Param("attemptId") Long attemptId);

    @Select({
            "<script>",
            "SELECT a.id AS answer_id,",
            "       a.attempt_id,",
            "       atp.assignment_id,",
            "       ass.assignment_title,",
            "       a.user_id AS student_id,",
            "       COALESCE(u.display_name, u.username) AS student_name,",
            "       a.question_id,",
            "       aq.question_type,",
            "       aq.score,",
            "       a.final_score AS current_final_score,",
            "       COALESCE(gr.needs_review, 0) AS needs_review,",
            "       atp.submitted_at",
            "FROM qb_answer a",
            "JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "LEFT JOIN qb_assignment ass ON ass.id = atp.assignment_id",
            "LEFT JOIN qb_attempt_question aq ON aq.id = a.attempt_question_id",
            "LEFT JOIN sys_user u ON u.id = a.user_id",
            "LEFT JOIN qb_grading_record gr ON gr.id = (",
            "  SELECT g2.id FROM qb_grading_record g2",
            "  WHERE g2.answer_id = a.id ORDER BY g2.id DESC LIMIT 1",
            ")",
            "WHERE 1=1",
            "<if test='ownerId != null'>",
            "  AND ass.created_by = #{ownerId}",
            "</if>",
            "<if test='assignmentId != null'>",
            "  AND atp.assignment_id = #{assignmentId}",
            "</if>",
            "<if test='studentId != null'>",
            "  AND a.user_id = #{studentId}",
            "</if>",
            "<if test='questionType != null'>",
            "  AND aq.question_type = #{questionType}",
            "</if>",
            "<if test='needsReview != null'>",
            "  AND COALESCE(gr.needs_review, 0) = #{needsReview}",
            "</if>",
            "ORDER BY a.id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<TeacherReviewAnswerItemVO> pageTeacherReview(@Param("assignmentId") Long assignmentId,
                                                      @Param("studentId") Long studentId,
                                                      @Param("questionType") Integer questionType,
                                                      @Param("needsReview") Integer needsReview,
                                                      @Param("ownerId") Long ownerId,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size);

    @Select({
            "<script>",
            "SELECT a.id",
            "FROM qb_answer a",
            "JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "LEFT JOIN qb_assignment ass ON ass.id = atp.assignment_id",
            "LEFT JOIN qb_attempt_question aq ON aq.id = a.attempt_question_id",
            "LEFT JOIN qb_grading_record gr ON gr.id = (",
            "  SELECT g2.id FROM qb_grading_record g2",
            "  WHERE g2.answer_id = a.id ORDER BY g2.id DESC LIMIT 1",
            ")",
            "WHERE atp.assignment_id = #{assignmentId}",
            "<if test='ownerId != null'>",
            "  AND ass.created_by = #{ownerId}",
            "</if>",
            "<if test='needsReview != null'>",
            "  AND COALESCE(gr.needs_review, 0) = #{needsReview}",
            "</if>",
            "ORDER BY a.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Long> listTeacherReviewAnswerIds(@Param("assignmentId") Long assignmentId,
                                          @Param("needsReview") Integer needsReview,
                                          @Param("ownerId") Long ownerId,
                                          @Param("limit") int limit);

    @Select({
            "SELECT a.question_id,",
            "       a.final_score,",
            "       aq.score AS max_score,",
            "       aq.difficulty,",
            "       COALESCE(a.graded_at, a.answered_at, atp.submitted_at, atp.created_at) AS event_time",
            "FROM qb_answer a",
            "JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "JOIN qb_attempt_question aq ON aq.id = a.attempt_question_id",
            "WHERE a.user_id = #{userId}",
            "  AND atp.status IN (2, 3, 4)",
            "  AND a.answer_status = 2",
            "ORDER BY event_time ASC, a.id ASC"
    })
    List<QbAbilitySample> selectAbilitySamplesByUserId(@Param("userId") Long userId);
}
