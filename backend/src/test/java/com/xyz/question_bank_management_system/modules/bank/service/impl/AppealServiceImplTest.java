package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.dto.AppealCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealHandleRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAnswer;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAppeal;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAppealMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbGradingRecordMapper;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    @Mock
    private QbAppealMapper appealMapper;
    @Mock
    private QbAnswerMapper answerMapper;
    @Mock
    private QbAssignmentMapper assignmentMapper;
    @Mock
    private QbAttemptQuestionMapper attemptQuestionMapper;
    @Mock
    private QbAttemptMapper attemptMapper;
    @Mock
    private QbGradingRecordMapper gradingRecordMapper;
    @Mock
    private UserAbilityService userAbilityService;

    @InjectMocks
    private AppealServiceImpl appealService;

    @Test
    void submitAppeal_shouldMarkAttemptAsGradingWhenAppealBecomesPending() {
        QbAnswer answer = new QbAnswer();
        answer.setId(501L);
        answer.setAttemptId(9001L);
        answer.setUserId(1001L);

        QbAttempt attempt = new QbAttempt();
        attempt.setId(9001L);
        attempt.setStatus(4);
        attempt.setNeedsReview(0);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setDurationSec(300);
        attempt.setTotalScore(60);
        attempt.setObjectiveScore(20);
        attempt.setSubjectiveScore(40);

        AppealCreateRequest request = new AppealCreateRequest();
        request.setAnswerId(501L);
        request.setReasonText("please recheck");

        when(answerMapper.selectById(501L)).thenReturn(answer);
        when(appealMapper.countPendingByAnswerAndUser(501L, 1001L)).thenReturn(0L);
        when(attemptMapper.selectById(9001L)).thenReturn(attempt);
        when(answerMapper.countPendingReviewByAttemptId(9001L)).thenReturn(0L);
        when(appealMapper.countPendingByAttemptId(9001L)).thenReturn(1L);

        appealService.submitAppeal(request, 1001L);

        ArgumentCaptor<QbAttempt> attemptCaptor = ArgumentCaptor.forClass(QbAttempt.class);
        verify(attemptMapper).updateAfterSubmit(attemptCaptor.capture());
        QbAttempt updated = attemptCaptor.getValue();
        assertEquals(3, updated.getStatus());
        assertEquals(1, updated.getNeedsReview());
    }

    @Test
    void handleAppeal_shouldMarkAttemptAsGradedWhenNoPendingWorkLeft() {
        QbAppeal appeal = new QbAppeal();
        appeal.setId(801L);
        appeal.setAnswerId(501L);
        appeal.setAppealStatus(1);

        QbAnswer answer = new QbAnswer();
        answer.setId(501L);
        answer.setAttemptId(9001L);
        answer.setAttemptQuestionId(601L);
        answer.setUserId(1001L);
        answer.setFinalScore(10);
        answer.setAutoScore(10);

        QbAttemptQuestion attemptQuestion = new QbAttemptQuestion();
        attemptQuestion.setId(601L);
        attemptQuestion.setQuestionType(6);
        attemptQuestion.setScore(20);

        QbAttempt attempt = new QbAttempt();
        attempt.setId(9001L);
        attempt.setAssignmentId(9101L);
        attempt.setStatus(3);
        attempt.setNeedsReview(1);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setDurationSec(300);
        attempt.setTotalScore(60);
        attempt.setObjectiveScore(20);
        attempt.setSubjectiveScore(40);

        QbAssignment assignment = new QbAssignment();
        assignment.setId(9101L);
        assignment.setCreatedBy(7001L);

        AppealHandleRequest request = new AppealHandleRequest();
        request.setAction("approve");
        request.setFinalScore(18);
        request.setDecisionComment("updated by appeal");

        when(appealMapper.selectById(801L)).thenReturn(appeal);
        when(answerMapper.selectById(501L)).thenReturn(answer);
        when(attemptQuestionMapper.selectById(601L)).thenReturn(attemptQuestion);
        when(attemptMapper.selectById(9001L)).thenReturn(attempt);
        when(assignmentMapper.selectById(9101L)).thenReturn(assignment);
        when(answerMapper.countPendingReviewByAttemptId(9001L)).thenReturn(0L);
        when(appealMapper.countPendingByAttemptId(9001L)).thenReturn(0L);

        appealService.handleAppeal(801L, request, 7001L, false);

        verify(answerMapper).updateScoring(eq(501L), eq(10), eq(18), eq(0), any(LocalDateTime.class));
        verify(attemptMapper).updateScoreDelta(9001L, 8, 0, 8);
        ArgumentCaptor<QbAttempt> attemptCaptor = ArgumentCaptor.forClass(QbAttempt.class);
        verify(attemptMapper).updateAfterSubmit(attemptCaptor.capture());
        QbAttempt updated = attemptCaptor.getValue();
        assertEquals(4, updated.getStatus());
        assertEquals(0, updated.getNeedsReview());
        verify(userAbilityService).recomputeAndPersist(1001L);
    }
}
