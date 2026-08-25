package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAnswer;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAppealMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbGradingRecordMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
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
class TeacherReviewServiceImplTest {

    @Mock
    private QbAnswerMapper answerMapper;
    @Mock
    private QbAppealMapper appealMapper;
    @Mock
    private QbAssignmentMapper assignmentMapper;
    @Mock
    private QbAssignmentTargetMapper targetMapper;
    @Mock
    private QbAttemptMapper attemptMapper;
    @Mock
    private QbAttemptQuestionMapper attemptQuestionMapper;
    @Mock
    private QbGradingRecordMapper gradingRecordMapper;
    @Mock
    private QbLlmCallMapper llmCallMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private LlmService llmService;
    @Mock
    private UserAbilityService userAbilityService;

    @InjectMocks
    private TeacherReviewServiceImpl teacherReviewService;

    @Test
    void manualGrade_shouldMarkAttemptAsGradedWhenNoPendingReviewLeft() {
        QbAnswer answer = new QbAnswer();
        answer.setId(501L);
        answer.setAttemptId(9001L);
        answer.setUserId(1001L);
        answer.setFinalScore(0);
        answer.setAutoScore(0);

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
        attempt.setTotalScore(0);
        attempt.setObjectiveScore(0);
        attempt.setSubjectiveScore(0);

        QbAssignment assignment = new QbAssignment();
        assignment.setId(9101L);
        assignment.setCreatedBy(7001L);

        when(answerMapper.selectById(501L)).thenReturn(answer);
        when(attemptQuestionMapper.selectById(601L)).thenReturn(attemptQuestion);
        when(attemptMapper.selectById(9001L)).thenReturn(attempt);
        when(assignmentMapper.selectById(9101L)).thenReturn(assignment);
        when(answerMapper.countPendingReviewByAttemptId(9001L)).thenReturn(0L);
        when(appealMapper.countPendingByAttemptId(9001L)).thenReturn(0L);
        answer.setAttemptQuestionId(601L);

        teacherReviewService.manualGrade(501L, 18, "manual reviewed", 7001L, false);

        verify(answerMapper).updateScoring(eq(501L), eq(0), eq(18), eq(0), any(LocalDateTime.class));
        verify(attemptMapper).updateScoreDelta(9001L, 18, 0, 18);
        ArgumentCaptor<QbAttempt> attemptCaptor = ArgumentCaptor.forClass(QbAttempt.class);
        verify(attemptMapper).updateAfterSubmit(attemptCaptor.capture());
        QbAttempt updated = attemptCaptor.getValue();
        assertEquals(4, updated.getStatus());
        assertEquals(0, updated.getNeedsReview());
        verify(userAbilityService).recomputeAndPersist(1001L);
    }
}
