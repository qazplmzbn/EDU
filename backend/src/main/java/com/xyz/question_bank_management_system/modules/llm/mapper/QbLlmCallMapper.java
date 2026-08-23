package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallListItemVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbLlmCallMapper {

    @Insert("INSERT INTO qb_llm_call(biz_type, biz_id, model_name, prompt_text, call_status, created_at) " +
            "VALUES(#{bizType}, #{bizId}, #{modelName}, #{promptText}, #{callStatus}, NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLlmCall call);

    @Update("UPDATE qb_llm_call SET response_text=#{responseText}, response_json=#{responseJson}, call_status=#{callStatus}, latency_ms=#{latencyMs}, tokens_prompt=#{tokensPrompt}, tokens_completion=#{tokensCompletion}, cost_amount=#{costAmount} WHERE id=#{id}")
    int updateResponse(QbLlmCall call);

    @Select("SELECT * FROM qb_llm_call WHERE id=#{id}")
    QbLlmCall selectById(@Param("id") Long id);

    @Select("SELECT * FROM qb_llm_call WHERE biz_type=#{bizType} AND biz_id=#{bizId} ORDER BY created_at DESC")
    List<QbLlmCall> selectByBiz(@Param("bizType") Integer bizType, @Param("bizId") Long bizId);

    @Select("SELECT * FROM qb_llm_call WHERE biz_type=#{bizType} AND biz_id=#{userId} ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<QbLlmCall> selectRecentStudentAssistantCalls(@Param("bizType") Integer bizType,
                                                      @Param("userId") Long userId,
                                                      @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM qb_llm_call WHERE biz_type=#{bizType} AND biz_id=#{userId}")
    long countStudentAssistantCalls(@Param("bizType") Integer bizType, @Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_llm_call",
            "WHERE 1=1",
            "<if test='bizType != null'>",
            "  AND biz_type = #{bizType}",
            "</if>",
            "<if test='bizId != null'>",
            "  AND biz_id = #{bizId}",
            "</if>",
            "</script>"
    })
    long countByFilter(@Param("bizType") Integer bizType, @Param("bizId") Long bizId);

    @Select({
            "<script>",
            "SELECT id AS llm_call_id, biz_type, biz_id, model_name, call_status, latency_ms, created_at",
            "FROM qb_llm_call",
            "WHERE 1=1",
            "<if test='bizType != null'>",
            "  AND biz_type = #{bizType}",
            "</if>",
            "<if test='bizId != null'>",
            "  AND biz_id = #{bizId}",
            "</if>",
            "ORDER BY created_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<LlmCallListItemVO> pageByFilter(@Param("bizType") Integer bizType,
                                         @Param("bizId") Long bizId,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM qb_llm_call c",
            "WHERE 1=1",
            "<if test='bizType != null'>",
            "  AND c.biz_type = #{bizType}",
            "</if>",
            "<if test='bizId != null'>",
            "  AND c.biz_id = #{bizId}",
            "</if>",
            "  AND (",
            "    (c.biz_type = 1 AND EXISTS (",
            "       SELECT 1 FROM qb_question q",
            "       WHERE q.id = c.biz_id AND q.is_deleted = 0 AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 2 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_answer a",
            "       JOIN qb_question q ON q.id = a.question_id AND q.is_deleted = 0",
            "       WHERE a.id = c.biz_id AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 4 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_class_member cm",
            "       JOIN qb_class cl ON cl.id = cm.class_id AND cl.is_deleted = 0",
            "       WHERE cm.student_id = c.biz_id AND cl.teacher_id = #{teacherId}",
            "    ))",
            "  )",
            "</script>"
    })
    long countByFilterForTeacher(@Param("bizType") Integer bizType,
                                 @Param("bizId") Long bizId,
                                 @Param("teacherId") Long teacherId);

    @Select({
            "<script>",
            "SELECT c.id AS llm_call_id, c.biz_type, c.biz_id, c.model_name, c.call_status, c.latency_ms, c.created_at",
            "FROM qb_llm_call c",
            "WHERE 1=1",
            "<if test='bizType != null'>",
            "  AND c.biz_type = #{bizType}",
            "</if>",
            "<if test='bizId != null'>",
            "  AND c.biz_id = #{bizId}",
            "</if>",
            "  AND (",
            "    (c.biz_type = 1 AND EXISTS (",
            "       SELECT 1 FROM qb_question q",
            "       WHERE q.id = c.biz_id AND q.is_deleted = 0 AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 2 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_answer a",
            "       JOIN qb_question q ON q.id = a.question_id AND q.is_deleted = 0",
            "       WHERE a.id = c.biz_id AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 4 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_class_member cm",
            "       JOIN qb_class cl ON cl.id = cm.class_id AND cl.is_deleted = 0",
            "       WHERE cm.student_id = c.biz_id AND cl.teacher_id = #{teacherId}",
            "    ))",
            "  )",
            "ORDER BY c.created_at DESC, c.id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<LlmCallListItemVO> pageByFilterForTeacher(@Param("bizType") Integer bizType,
                                                    @Param("bizId") Long bizId,
                                                    @Param("teacherId") Long teacherId,
                                                    @Param("offset") long offset,
                                                    @Param("size") long size);

    @Select({
            "<script>",
            "SELECT c.*",
            "FROM qb_llm_call c",
            "WHERE c.id = #{id}",
            "  AND (",
            "    (c.biz_type = 1 AND EXISTS (",
            "       SELECT 1 FROM qb_question q",
            "       WHERE q.id = c.biz_id AND q.is_deleted = 0 AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 2 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_answer a",
            "       JOIN qb_question q ON q.id = a.question_id AND q.is_deleted = 0",
            "       WHERE a.id = c.biz_id AND q.created_by = #{teacherId}",
            "    ))",
            "    OR",
            "    (c.biz_type = 4 AND EXISTS (",
            "       SELECT 1",
            "       FROM qb_class_member cm",
            "       JOIN qb_class cl ON cl.id = cm.class_id AND cl.is_deleted = 0",
            "       WHERE cm.student_id = c.biz_id AND cl.teacher_id = #{teacherId}",
            "    ))",
            "  )",
            "</script>"
    })
    QbLlmCall selectByIdForTeacher(@Param("id") Long id, @Param("teacherId") Long teacherId);
}
