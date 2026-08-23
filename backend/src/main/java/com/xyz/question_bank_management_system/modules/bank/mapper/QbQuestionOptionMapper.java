package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionOption;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbQuestionOptionMapper {

    @Select("SELECT * FROM qb_question_option WHERE question_id=#{questionId} ORDER BY sort_order ASC, id ASC")
    List<QbQuestionOption> selectByQuestionId(@Param("questionId") Long questionId);

    @Delete("DELETE FROM qb_question_option WHERE question_id=#{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    int batchInsert(@Param("list") List<QbQuestionOption> list);
}
