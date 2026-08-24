package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.dto.PaperAddQuestionRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateItem;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaperQuestion;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestion;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionOptionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QuestionKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceImplTest {

    @Mock
    private QbPaperMapper paperMapper;
    @Mock
    private QbPaperQuestionMapper paperQuestionMapper;
    @Mock
    private QbQuestionMapper questionMapper;
    @Mock
    private QbQuestionOptionMapper optionMapper;
    @Mock
    private QuestionKnowledgeMapper questionKnowledgeMapper;
    @Mock
    private SysRoleMapper roleMapper;

    @InjectMocks
    private PaperServiceImpl paperService;

    @Test
    void addQuestion_shouldTranslateDuplicateOrderToConflict() {
        QbPaper paper = new QbPaper();
        paper.setId(9001L);
        paper.setCreatorId(1001L);

        QbQuestion question = new QbQuestion();
        question.setId(501L);
        question.setCreatedBy(1001L);
        question.setStatus(2);
        question.setTitle("demo");
        question.setStem("stem");

        PaperAddQuestionRequest request = new PaperAddQuestionRequest();
        request.setQuestionId(501L);
        request.setOrderNo(1);
        request.setScore(5);

        when(paperMapper.selectById(9001L)).thenReturn(paper);
        when(questionMapper.selectById(501L)).thenReturn(question);
        when(optionMapper.selectByQuestionId(501L)).thenReturn(List.of());
        when(questionKnowledgeMapper.selectByQuestionId(501L)).thenReturn(List.of());
        when(paperQuestionMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        BizException ex = assertThrows(BizException.class, () -> paperService.addQuestion(9001L, request, 1001L, false));

        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        assertEquals("该排序位置已存在试题", ex.getMessage());
    }

    @Test
    void batchUpdatePaperQuestions_shouldSwapOrderInSingleSave() {
        QbPaper paper = new QbPaper();
        paper.setId(9001L);
        paper.setCreatorId(1001L);

        QbPaperQuestion first = new QbPaperQuestion();
        first.setId(11L);
        first.setPaperId(9001L);
        first.setQuestionId(501L);
        first.setOrderNo(1);
        first.setScore(5);
        first.setSnapshotJson("{}");
        first.setSnapshotHash("hash-1");

        QbPaperQuestion second = new QbPaperQuestion();
        second.setId(12L);
        second.setPaperId(9001L);
        second.setQuestionId(502L);
        second.setOrderNo(2);
        second.setScore(20);
        second.setSnapshotJson("{}");
        second.setSnapshotHash("hash-2");

        PaperQuestionBatchUpdateItem firstItem = new PaperQuestionBatchUpdateItem();
        firstItem.setId(11L);
        firstItem.setOrderNo(2);
        firstItem.setScore(10);

        PaperQuestionBatchUpdateItem secondItem = new PaperQuestionBatchUpdateItem();
        secondItem.setId(12L);
        secondItem.setOrderNo(1);
        secondItem.setScore(20);

        PaperQuestionBatchUpdateRequest request = new PaperQuestionBatchUpdateRequest();
        request.setQuestions(List.of(firstItem, secondItem));

        List<Integer> orderSnapshots = new ArrayList<>();
        List<Integer> scoreSnapshots = new ArrayList<>();

        when(paperMapper.selectById(9001L)).thenReturn(paper);
        when(paperQuestionMapper.selectByPaperId(9001L)).thenReturn(List.of(first, second));
        when(paperQuestionMapper.sumScoreByPaperId(9001L)).thenReturn(30);
        doAnswer(invocation -> {
            QbPaperQuestion question = invocation.getArgument(0);
            orderSnapshots.add(question.getOrderNo());
            scoreSnapshots.add(question.getScore());
            return 1;
        }).when(paperQuestionMapper).update(any(QbPaperQuestion.class));

        paperService.batchUpdatePaperQuestions(9001L, request, 1001L, false);

        assertEquals(List.of(105, 106, 2, 1), orderSnapshots);
        assertEquals(List.of(10, 20, 10, 20), scoreSnapshots);
        verify(paperQuestionMapper, times(4)).update(any(QbPaperQuestion.class));
        verify(paperMapper).update(any(QbPaper.class));
    }
}
