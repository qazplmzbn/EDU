package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionUserStat;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;
import com.xyz.question_bank_management_system.modules.bank.entity.QbWrongQuestion;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionUserStatMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbUserAbilityMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbWrongQuestionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private QbWrongQuestionMapper wrongQuestionMapper;
    @Mock
    private QbQuestionUserStatMapper questionUserStatMapper;
    @Mock
    private StudentKnowledgeStateMapper studentKnowledgeStateMapper;
    @Mock
    private QbUserAbilityMapper userAbilityMapper;

    @InjectMocks
    private StatsServiceImpl statsService;

    @Test
    void wrongQuestions_shouldUseNormalizedPageAndFilters() {
        QbWrongQuestion row = new QbWrongQuestion();
        row.setQuestionId(50001L);
        row.setWrongCount(2);

        when(wrongQuestionMapper.pageByFilter(1001L, 11L, "chapter-1", 1, 0L, 200L))
                .thenReturn(List.of(row));
        when(wrongQuestionMapper.countByFilter(1001L, 11L, "chapter-1", 1))
                .thenReturn(1L);

        PageResponse<QbWrongQuestion> page = statsService.wrongQuestions(
                1001L, 11L, "  chapter-1  ", true, 0, 999
        );

        assertEquals(1L, page.getPage());
        assertEquals(200L, page.getSize());
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        verify(wrongQuestionMapper).pageByFilter(1001L, 11L, "chapter-1", 1, 0L, 200L);
        verify(wrongQuestionMapper).countByFilter(1001L, 11L, "chapter-1", 1);
    }

    @Test
    void questionStats_shouldReturnPagedResult() {
        QbQuestionUserStat row = new QbQuestionUserStat();
        row.setQuestionId(9001L);
        row.setAttemptCount(3);
        row.setCorrectCount(2);

        when(questionUserStatMapper.pageByFilter(1001L, 9001L, 10L, 10L)).thenReturn(List.of(row));
        when(questionUserStatMapper.countByFilter(1001L, 9001L)).thenReturn(1L);

        PageResponse<QbQuestionUserStat> page = statsService.questionStats(1001L, 9001L, 2, 10);

        assertEquals(2L, page.getPage());
        assertEquals(10L, page.getSize());
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        verify(questionUserStatMapper).pageByFilter(1001L, 9001L, 10L, 10L);
        verify(questionUserStatMapper).countByFilter(1001L, 9001L);
    }

    @Test
    void mastery_shouldReturnKnowledgeStates() {
        StudentKnowledgeState row = new StudentKnowledgeState();
        row.setKnowledgePointId(1L);
        row.setMasteryLevel("basic");
        when(studentKnowledgeStateMapper.selectByUserId(1001L)).thenReturn(List.of(row));

        List<StudentKnowledgeState> list = statsService.mastery(1001L);

        assertEquals(1, list.size());
        assertEquals("basic", list.get(0).getMasteryLevel());
        verify(studentKnowledgeStateMapper).selectByUserId(1001L);
    }

    @Test
    void resolveWrongQuestion_shouldThrowWhenNotFound() {
        when(wrongQuestionMapper.resolve(eq(1001L), eq(50001L), any(LocalDateTime.class))).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> statsService.resolveWrongQuestion(1001L, 50001L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void ability_shouldReturnZeroWhenNoRecord() {
        when(userAbilityMapper.selectByUserId(1001L)).thenReturn(null);

        QbUserAbility ability = statsService.ability(1001L);

        assertEquals(1001L, ability.getUserId());
        assertEquals(0, ability.getAbilityScore());
    }
}
