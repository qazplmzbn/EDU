package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbAssignmentMapper {

    @Insert("INSERT INTO qb_assignment(paper_id, assignment_title, assignment_desc, start_time, end_time, time_limit_min, max_attempts, shuffle_questions, shuffle_options, publish_status, created_by, created_at, updated_at, is_deleted) " +
            "VALUES(#{paperId}, #{assignmentTitle}, #{assignmentDesc}, #{startTime}, #{endTime}, #{timeLimitMin}, #{maxAttempts}, #{shuffleQuestions}, #{shuffleOptions}, #{publishStatus}, #{createdBy}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbAssignment a);

    @Update("UPDATE qb_assignment SET assignment_title=#{assignmentTitle}, assignment_desc=#{assignmentDesc}, start_time=#{startTime}, end_time=#{endTime}, time_limit_min=#{timeLimitMin}, max_attempts=#{maxAttempts}, shuffle_questions=#{shuffleQuestions}, shuffle_options=#{shuffleOptions}, publish_status=#{publishStatus}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbAssignment a);

    @Select("SELECT * FROM qb_assignment WHERE id=#{id} AND is_deleted=0")
    QbAssignment selectById(@Param("id") Long id);

    @Update("UPDATE qb_assignment SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE qb_assignment SET publish_status=#{status}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int updatePublishStatus(@Param("id") Long id, @Param("status") Integer status);

    @Select({
            "<script>",
            "SELECT * FROM qb_assignment",
            "WHERE is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (assignment_title LIKE CONCAT('%', #{keyword}, '%') OR CAST(id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<QbAssignment> pageAll(@Param("keyword") String keyword,
                               @Param("offset") long offset,
                               @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM qb_assignment",
            "WHERE is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (assignment_title LIKE CONCAT('%', #{keyword}, '%') OR CAST(id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "</script>"
    })
    long countAll(@Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT * FROM qb_assignment",
            "WHERE created_by = #{teacherId} AND is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (assignment_title LIKE CONCAT('%', #{keyword}, '%') OR CAST(id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<QbAssignment> pageByTeacher(@Param("teacherId") Long teacherId,
                                     @Param("keyword") String keyword,
                                     @Param("offset") long offset,
                                     @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM qb_assignment",
            "WHERE created_by = #{teacherId} AND is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (assignment_title LIKE CONCAT('%', #{keyword}, '%') OR CAST(id AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "</script>"
    })
    long countByTeacher(@Param("teacherId") Long teacherId, @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_assignment a",
            "WHERE a.is_deleted = 0",
            "  AND a.publish_status IN (2,3)",
            "  AND (",
            "    (NOT EXISTS (SELECT 1 FROM qb_assignment_target t0 WHERE t0.assignment_id = a.id)",
            "     AND NOT EXISTS (SELECT 1 FROM qb_assignment_target_class tc0 WHERE tc0.assignment_id = a.id))",
            "    OR EXISTS (SELECT 1 FROM qb_assignment_target t1 WHERE t1.assignment_id = a.id AND t1.user_id = #{userId})",
            "    OR EXISTS (",
            "      SELECT 1",
            "      FROM qb_assignment_target_class tc1",
            "      JOIN qb_class_member cm1 ON cm1.class_id = tc1.class_id",
            "      WHERE tc1.assignment_id = a.id AND cm1.student_id = #{userId}",
            "    )",
            "  )",
            "<if test='status != null and status.equals(\"ongoing\")'>",
            "  AND a.publish_status = 2",
            "  AND (a.end_time IS NULL OR a.end_time &gt;= #{now})",
            "</if>",
            "<if test='status != null and status.equals(\"expired\")'>",
            "  AND (a.publish_status = 3 OR (a.end_time IS NOT NULL AND a.end_time &lt; #{now}))",
            "</if>",
            "</script>"
    })
    long countForStudent(@Param("userId") Long userId,
                         @Param("status") String status,
                         @Param("now") LocalDateTime now);

    @Select({
            "<script>",
            "SELECT a.id AS assignment_id,",
            "       a.assignment_title,",
            "       a.start_time,",
            "       a.end_time,",
            "       a.time_limit_min,",
            "       a.max_attempts,",
            "       a.publish_status,",
            "       (SELECT COUNT(1) FROM qb_attempt atp WHERE atp.assignment_id = a.id AND atp.user_id = #{userId}) AS my_attempt_count",
            "FROM qb_assignment a",
            "WHERE a.is_deleted = 0",
            "  AND a.publish_status IN (2,3)",
            "  AND (",
            "    (NOT EXISTS (SELECT 1 FROM qb_assignment_target t0 WHERE t0.assignment_id = a.id)",
            "     AND NOT EXISTS (SELECT 1 FROM qb_assignment_target_class tc0 WHERE tc0.assignment_id = a.id))",
            "    OR EXISTS (SELECT 1 FROM qb_assignment_target t1 WHERE t1.assignment_id = a.id AND t1.user_id = #{userId})",
            "    OR EXISTS (",
            "      SELECT 1",
            "      FROM qb_assignment_target_class tc1",
            "      JOIN qb_class_member cm1 ON cm1.class_id = tc1.class_id",
            "      WHERE tc1.assignment_id = a.id AND cm1.student_id = #{userId}",
            "    )",
            "  )",
            "<if test='status != null and status.equals(\"ongoing\")'>",
            "  AND a.publish_status = 2",
            "  AND (a.end_time IS NULL OR a.end_time &gt;= #{now})",
            "</if>",
            "<if test='status != null and status.equals(\"expired\")'>",
            "  AND (a.publish_status = 3 OR (a.end_time IS NOT NULL AND a.end_time &lt; #{now}))",
            "</if>",
            "ORDER BY a.created_at DESC, a.id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<AssignmentMyItemVO> pageForStudent(@Param("userId") Long userId,
                                            @Param("status") String status,
                                            @Param("now") LocalDateTime now,
                                            @Param("offset") long offset,
                                            @Param("size") long size);
}
