package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAppeal;
import com.xyz.question_bank_management_system.modules.bank.vo.AppealMyItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAppealItemVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbAppealMapper {

    @Insert("INSERT INTO qb_appeal(answer_id, user_id, reason_text, appeal_status, created_at) " +
            "VALUES(#{answerId}, #{userId}, #{reasonText}, #{appealStatus}, NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbAppeal appeal);

    @Select("SELECT * FROM qb_appeal WHERE id=#{id}")
    QbAppeal selectById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM qb_appeal WHERE answer_id=#{answerId} AND user_id=#{userId} AND appeal_status=1")
    long countPendingByAnswerAndUser(@Param("answerId") Long answerId, @Param("userId") Long userId);

    @Select({
            "SELECT COUNT(1)",
            "FROM qb_appeal ap",
            "INNER JOIN qb_answer a ON a.id = ap.answer_id",
            "WHERE a.attempt_id = #{attemptId}",
            "  AND ap.appeal_status = 1"
    })
    long countPendingByAttemptId(@Param("attemptId") Long attemptId);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_appeal",
            "WHERE user_id=#{userId}",
            "<if test='status != null'>",
            "  AND appeal_status=#{status}",
            "</if>",
            "</script>"
    })
    long countByUser(@Param("userId") Long userId, @Param("status") Integer status);

    @Select({
            "<script>",
            "SELECT id AS appeal_id, answer_id, appeal_status, created_at",
            "FROM qb_appeal",
            "WHERE user_id=#{userId}",
            "<if test='status != null'>",
            "  AND appeal_status=#{status}",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<AppealMyItemVO> pageByUser(@Param("userId") Long userId,
                                    @Param("status") Integer status,
                                    @Param("offset") long offset,
                                    @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_appeal ap",
            "LEFT JOIN qb_answer a ON a.id = ap.answer_id",
            "LEFT JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "LEFT JOIN qb_assignment ass ON ass.id = atp.assignment_id",
            "WHERE 1=1",
            "<if test='ownerId != null'>",
            "  AND ass.created_by = #{ownerId}",
            "</if>",
            "<if test='status != null'>",
            "  AND ap.appeal_status=#{status}",
            "</if>",
            "</script>"
    })
    long countForTeacher(@Param("status") Integer status, @Param("ownerId") Long ownerId);

    @Select({
            "<script>",
            "SELECT ap.id AS appeal_id,",
            "       ap.answer_id,",
            "       ap.user_id AS student_id,",
            "       atp.assignment_id,",
            "       ass.assignment_title,",
            "       ap.reason_text,",
            "       ap.appeal_status,",
            "       ap.created_at",
            "FROM qb_appeal ap",
            "LEFT JOIN qb_answer a ON a.id = ap.answer_id",
            "LEFT JOIN qb_attempt atp ON atp.id = a.attempt_id",
            "LEFT JOIN qb_assignment ass ON ass.id = atp.assignment_id",
            "WHERE 1=1",
            "<if test='ownerId != null'>",
            "  AND ass.created_by = #{ownerId}",
            "</if>",
            "<if test='status != null'>",
            "  AND ap.appeal_status=#{status}",
            "</if>",
            "ORDER BY ap.created_at DESC, ap.id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<TeacherAppealItemVO> pageForTeacher(@Param("status") Integer status,
                                             @Param("ownerId") Long ownerId,
                                             @Param("offset") long offset,
                                             @Param("size") long size);

    @Update("UPDATE qb_appeal SET appeal_status=#{appealStatus}, handled_by=#{handledBy}, handled_at=#{handledAt}, " +
            "decision_comment=#{decisionComment}, final_score=#{finalScore} WHERE id=#{id}")
    int updateHandle(@Param("id") Long id,
                     @Param("appealStatus") Integer appealStatus,
                     @Param("handledBy") Long handledBy,
                     @Param("handledAt") LocalDateTime handledAt,
                     @Param("decisionComment") String decisionComment,
                     @Param("finalScore") Integer finalScore);
}
