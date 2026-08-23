package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.modules.bank.dto.QuestionHintRequest;
import com.xyz.question_bank_management_system.modules.bank.vo.QuestionHintVO;

public interface QuestionHintService {
    QuestionHintVO generateHint(Long attemptId, Long attemptQuestionId, Long userId, QuestionHintRequest request);
}
