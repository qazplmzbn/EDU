package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.AssignmentPublishStatusEnum;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetClassMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperMapper;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private QbAssignmentMapper assignmentMapper;
    @Mock
    private QbAssignmentTargetMapper targetMapper;
    @Mock
    private QbAssignmentTargetClassMapper targetClassMapper;
    @Mock
    private QbPaperMapper paperMapper;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Test
    void pageForStudent_shouldUseAllAsDefaultStatus() {
        AssignmentMyItemVO row = new AssignmentMyItemVO();
        row.setAssignmentId(82001L);
        row.setAssignmentTitle("pointer weekly test");

        when(assignmentMapper.pageForStudent(eq(1001L), eq("all"), any(LocalDateTime.class), eq(0L), eq(10L)))
                .thenReturn(List.of(row));
        when(assignmentMapper.countForStudent(eq(1001L), eq("all"), any(LocalDateTime.class)))
                .thenReturn(1L);

        PageResponse<AssignmentMyItemVO> page = assignmentService.pageForStudent(null, 1, 10, 1001L);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(82001L, page.getList().get(0).getAssignmentId());
        verify(assignmentMapper).pageForStudent(eq(1001L), eq("all"), any(LocalDateTime.class), eq(0L), eq(10L));
    }

    @Test
    void pageForStudent_shouldRejectUnsupportedStatus() {
        BizException ex = assertThrows(BizException.class,
                () -> assignmentService.pageForStudent("invalid", 1, 10, 1001L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    void detailForStudent_shouldRejectDraftAssignment() {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(82001L);
        assignment.setPublishStatus(AssignmentPublishStatusEnum.DRAFT.getCode());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);

        BizException ex = assertThrows(BizException.class, () -> assignmentService.detailForStudent(82001L, 1001L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void detailForStudent_shouldRejectNonTargetUserWhenTargetsConfigured() {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(82001L);
        assignment.setPublishStatus(AssignmentPublishStatusEnum.PUBLISHED.getCode());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.countByAssignmentId(82001L)).thenReturn(2L);
        when(targetClassMapper.countByAssignmentId(82001L)).thenReturn(0L);
        when(targetMapper.countByAssignmentAndUser(82001L, 1001L)).thenReturn(0L);
        when(targetClassMapper.countByAssignmentAndStudent(82001L, 1001L)).thenReturn(0L);

        BizException ex = assertThrows(BizException.class, () -> assignmentService.detailForStudent(82001L, 1001L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void detailForStudent_shouldPassWhenUserInTargets() {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(82001L);
        assignment.setPublishStatus(AssignmentPublishStatusEnum.CLOSED.getCode());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.countByAssignmentId(82001L)).thenReturn(1L);
        when(targetClassMapper.countByAssignmentId(82001L)).thenReturn(0L);
        when(targetMapper.countByAssignmentAndUser(82001L, 1001L)).thenReturn(1L);

        QbAssignment detail = assignmentService.detailForStudent(82001L, 1001L);

        assertNotNull(detail);
        assertEquals(82001L, detail.getId());
    }
}
