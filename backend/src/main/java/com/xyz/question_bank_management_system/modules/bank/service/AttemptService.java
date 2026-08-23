package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.SaveAnswerDraftRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.vo.*;

import java.util.List;

public interface AttemptService {

    AttemptStartVO startAssignmentAttempt(Long assignmentId, Long userId);

    AttemptStartVO startPracticeAttempt(PracticeStartRequest request, Long userId);

    List<AttemptQuestionVO> getAttemptQuestions(Long attemptId, Long userId);

    void saveDraft(Long answerId, Long userId, SaveAnswerDraftRequest request);

    void submitAnswer(Long answerId, Long userId, SaveAnswerDraftRequest request);

    void submitAttempt(Long attemptId, Long userId);

    AttemptResultVO result(Long attemptId, Long userId);

    PageResponse<QbAttempt> myAttempts(Integer attemptType, long page, long size, Long userId);
}
