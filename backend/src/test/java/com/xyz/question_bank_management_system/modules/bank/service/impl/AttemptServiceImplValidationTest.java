package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.*;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceImplValidationTest {

    @Mock
    private QbAssignmentMapper assignmentMapper;
    @Mock
    private QbAssignmentTargetMapper targetMapper;
    @Mock
    private QbAttemptMapper attemptMapper;
    @Mock
    private QbAttemptQuestionMapper attemptQuestionMapper;
    @Mock
    private QbAnswerMapper answerMapper;
    @Mock
    private QbGradingRecordMapper gradingRecordMapper;
    @Mock
    private QbPaperQuestionMapper paperQuestionMapper;
    @Mock
    private QbQuestionMapper questionMapper;
    @Mock
    private QbQuestionOptionMapper optionMapper;
    @Mock
    private QuestionKnowledgeMapper questionKnowledgeMapper;
    @Mock
    private QbClassMemberMapper classMemberMapper;
    @Mock
    private QbQuestionUserStatMapper questionUserStatMapper;
    @Mock
    private QbWrongQuestionMapper wrongQuestionMapper;
    @Mock
    private StudentKnowledgeStateMapper studentKnowledgeStateMapper;
    @Mock
    private StudentProfileService studentProfileService;
    @Mock
    private LlmService llmService;
    @Mock
    private UserAbilityService userAbilityService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TaskExecutor attemptGradingExecutor;

    @InjectMocks
    private AttemptServiceImpl attemptService;

    @Test
    void myAttempts_shouldRejectUnsupportedAttemptType() {
        BizException ex = assertThrows(BizException.class, () -> attemptService.myAttempts(9, 1, 10, 1001L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    void startPracticeAttempt_shouldRejectInvalidMode() {
        PracticeStartRequest request = new PracticeStartRequest();
        request.setMode("unknown");
        request.setTotalScore(100);

        BizException ex = assertThrows(BizException.class, () -> attemptService.startPracticeAttempt(request, 1001L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    void submitAttempt_shouldSealAnswersAndGradeAsync() {
        ReflectionTestUtils.setField(attemptService, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(attemptService, "attemptGradingExecutor", attemptGradingExecutor);

        QbAttempt inProgress = new QbAttempt();
        inProgress.setId(2001L);
        inProgress.setUserId(1001L);
        inProgress.setStatus(1);
        inProgress.setStartedAt(LocalDateTime.now().minusMinutes(5));

        QbAttempt grading = new QbAttempt();
        grading.setId(2001L);
        grading.setUserId(1001L);
        grading.setStatus(3);
        grading.setSubmittedAt(LocalDateTime.now());
        grading.setDurationSec(300);

        when(attemptMapper.selectById(2001L)).thenReturn(inProgress, inProgress, grading);
        when(attemptQuestionMapper.selectByAttemptId(2001L)).thenReturn(List.of());
        when(answerMapper.selectByAttemptId(2001L)).thenReturn(List.of());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class));
        });
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(attemptGradingExecutor).execute(any(Runnable.class));

        attemptService.submitAttempt(2001L, 1001L);

        verify(answerMapper).submitAllByAttempt(eq(2001L), any(LocalDateTime.class));
        verify(attemptMapper, times(2)).updateAfterSubmit(any(QbAttempt.class));
        verify(userAbilityService).recomputeAndPersist(1001L);
    }
}
