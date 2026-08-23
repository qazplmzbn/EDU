package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbAttemptQuestionMapper {

    @Select("SELECT * FROM qb_attempt_question WHERE attempt_id=#{attemptId} ORDER BY order_no ASC")
    List<QbAttemptQuestion> selectByAttemptId(@Param("attemptId") Long attemptId);

    @Select("SELECT * FROM qb_attempt_question WHERE id=#{id}")
    QbAttemptQuestion selectById(@Param("id") Long id);

    int batchInsert(@Param("list") List<QbAttemptQuestion> list);

    @Delete("DELETE FROM qb_attempt_question WHERE attempt_id=#{attemptId}")
    int deleteByAttemptId(@Param("attemptId") Long attemptId);
}
