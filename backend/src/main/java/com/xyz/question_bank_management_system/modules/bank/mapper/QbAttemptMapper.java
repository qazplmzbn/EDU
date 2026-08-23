package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentScoreItemVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbAttemptMapper {

    @Insert("INSERT INTO qb_attempt(assignment_id, paper_id, user_id, attempt_type, attempt_no, status, started_at, total_score, objective_score, subjective_score, needs_review, created_at, updated_at) " +
            "VALUES(#{assignmentId}, #{paperId}, #{userId}, #{attemptType}, #{attemptNo}, #{status}, NOW(3), 0, 0, 0, 0, NOW(3), NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbAttempt attempt);

    @Select("SELECT * FROM qb_attempt WHERE id=#{id}")
    QbAttempt selectById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM qb_attempt WHERE assignment_id=#{assignmentId} AND user_id=#{userId}")
    long countByAssignmentAndUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    @Select("SELECT * FROM qb_attempt WHERE user_id=#{userId} AND status IN (2,3,4) " +
            "AND submitted_at >= #{startAt} AND submitted_at < #{endAt} " +
            "ORDER BY submitted_at DESC, id DESC")
    List<QbAttempt> selectByUserAndSubmittedRange(@Param("userId") Long userId,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);

    @Select("SELECT user_id FROM (" +
            "SELECT user_id, MAX(updated_at) AS latest_at FROM qb_attempt WHERE status IN (2,3,4) GROUP BY user_id" +
            ") t ORDER BY latest_at DESC LIMIT #{limit}")
    List<Long> listRecentStudentIds(@Param("limit") int limit);

    @Select("SELECT DISTINCT user_id FROM qb_attempt WHERE assignment_id=#{assignmentId} ORDER BY user_id ASC")
    List<Long> listUserIdsByAssignment(@Param("assignmentId") Long assignmentId);

    @Select({
            "SELECT DISTINCT t.user_id",
            "FROM qb_attempt t",
            "JOIN qb_assignment a ON a.id = t.assignment_id AND a.is_deleted = 0",
            "WHERE a.created_by = #{teacherId}",
            "  AND t.user_id IS NOT NULL",
            "ORDER BY t.user_id ASC"
    })
    List<Long> listStudentIdsByTeacherAssignments(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM qb_attempt WHERE assignment_id=#{assignmentId} AND user_id=#{userId} ORDER BY attempt_no DESC, id DESC")
    List<QbAttempt> selectByAssignmentAndUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT t.*",
            "FROM qb_attempt t",
            "LEFT JOIN qb_assignment a ON a.id = t.assignment_id",
            "WHERE t.status = 1",
            "  AND (",
            "    CASE",
            "      WHEN a.end_time IS NOT NULL AND a.time_limit_min IS NOT NULL AND a.time_limit_min &gt; 0 AND t.started_at IS NOT NULL",
            "        THEN LEAST(a.end_time, DATE_ADD(t.started_at, INTERVAL a.time_limit_min MINUTE))",
            "      WHEN a.end_time IS NOT NULL",
            "        THEN a.end_time",
            "      WHEN a.time_limit_min IS NOT NULL AND a.time_limit_min &gt; 0 AND t.started_at IS NOT NULL",
            "        THEN DATE_ADD(t.started_at, INTERVAL a.time_limit_min MINUTE)",
            "      ELSE NULL",
            "    END",
            "  ) IS NOT NULL",
            "  AND (",
            "    CASE",
            "      WHEN a.end_time IS NOT NULL AND a.time_limit_min IS NOT NULL AND a.time_limit_min &gt; 0 AND t.started_at IS NOT NULL",
            "        THEN LEAST(a.end_time, DATE_ADD(t.started_at, INTERVAL a.time_limit_min MINUTE))",
            "      WHEN a.end_time IS NOT NULL",
            "        THEN a.end_time",
            "      WHEN a.time_limit_min IS NOT NULL AND a.time_limit_min &gt; 0 AND t.started_at IS NOT NULL",
            "        THEN DATE_ADD(t.started_at, INTERVAL a.time_limit_min MINUTE)",
            "      ELSE NULL",
            "    END",
            "  ) &lt;= #{now}",
            "ORDER BY t.id ASC",
            "LIMIT #{size}",
            "</script>"
    })
    List<QbAttempt> selectExpiredInProgressAttempts(@Param("now") LocalDateTime now, @Param("size") int size);

    @Update("UPDATE qb_attempt SET status=#{status}, submitted_at=#{submittedAt}, duration_sec=#{durationSec}, total_score=#{totalScore}, objective_score=#{objectiveScore}, subjective_score=#{subjectiveScore}, needs_review=#{needsReview}, updated_at=NOW(3) WHERE id=#{id}")
    int updateAfterSubmit(QbAttempt attempt);

    @Update("UPDATE qb_attempt SET status=#{status}, updated_at=NOW(3) WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE qb_attempt SET total_score = total_score + #{deltaTotal}, objective_score = objective_score + #{deltaObjective}, " +
            "subjective_score = subjective_score + #{deltaSubjective}, updated_at=NOW(3) WHERE id=#{id}")
    int updateScoreDelta(@Param("id") Long id,
                         @Param("deltaTotal") Integer deltaTotal,
                         @Param("deltaObjective") Integer deltaObjective,
                         @Param("deltaSubjective") Integer deltaSubjective);

    @Select({
            "<script>",
            "SELECT *",
            "FROM qb_attempt",
            "WHERE user_id = #{userId}",
            "<if test='attemptType != null'>",
            "  AND attempt_type = #{attemptType}",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<QbAttempt> pageByUser(@Param("userId") Long userId,
                               @Param("attemptType") Integer attemptType,
                               @Param("offset") long offset,
                               @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_attempt",
            "WHERE user_id = #{userId}",
            "<if test='attemptType != null'>",
            "  AND attempt_type = #{attemptType}",
            "</if>",
            "</script>"
    })
    long countByUser(@Param("userId") Long userId, @Param("attemptType") Integer attemptType);

    @Select({
            "<script>",
            "SELECT t.*,",
            "       COALESCE(p.total_score, (",
            "         SELECT COALESCE(SUM(aq.score), 0)",
            "         FROM qb_attempt_question aq",
            "         WHERE aq.attempt_id = t.id",
            "       )) AS max_score",
            "FROM qb_attempt t",
            "LEFT JOIN qb_paper p ON p.id = t.paper_id",
            "WHERE t.user_id = #{userId}",
            "  AND t.status IN (2,3,4)",
            "<if test='attemptType != null'>",
            "  AND t.attempt_type = #{attemptType}",
            "</if>",
            "ORDER BY COALESCE(t.submitted_at, t.updated_at, t.created_at) DESC, t.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<QbAttempt> selectRecentCompletedByUser(@Param("userId") Long userId,
                                                @Param("attemptType") Integer attemptType,
                                                @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_attempt",
            "WHERE user_id = #{userId}",
            "  AND status IN (2,3,4)",
            "<if test='attemptType != null'>",
            "  AND attempt_type = #{attemptType}",
            "</if>",
            "</script>"
    })
    long countCompletedByUser(@Param("userId") Long userId, @Param("attemptType") Integer attemptType);

    @Select("SELECT COUNT(1) FROM qb_attempt WHERE assignment_id=#{assignmentId} AND status IN (2,3,4)")
    long countByAssignmentForTeacher(@Param("assignmentId") Long assignmentId);

    @Select("SELECT a.user_id AS student_id, u.display_name, a.id AS attempt_id, a.total_score, a.needs_review, a.submitted_at " +
            "FROM qb_attempt a LEFT JOIN sys_user u ON u.id = a.user_id " +
            "WHERE a.assignment_id=#{assignmentId} AND a.status IN (2,3,4) " +
            "ORDER BY a.submitted_at DESC, a.id DESC LIMIT #{offset}, #{size}")
    List<TeacherAssignmentScoreItemVO> pageByAssignmentForTeacher(@Param("assignmentId") Long assignmentId,
                                                                  @Param("offset") long offset,
                                                                  @Param("size") long size);
}
