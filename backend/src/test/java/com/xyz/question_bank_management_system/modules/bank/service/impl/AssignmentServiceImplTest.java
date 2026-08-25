package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.AssignmentPublishStatusEnum;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetSelectionDTO;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetsRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperMapper;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import com.xyz.question_bank_management_system.modules.org.entity.QbClass;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
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
    private QbAttemptMapper attemptMapper;
    @Mock
    private QbPaperMapper paperMapper;
    @Mock
    private QbClassMapper classMapper;
    @Mock
    private QbClassMemberMapper classMemberMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private AuditLogService auditLogService;

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
    void detailForStudent_shouldRejectNonTargetUserIncludingWhenNoTargetsExist() {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(82001L);
        assignment.setPublishStatus(AssignmentPublishStatusEnum.PUBLISHED.getCode());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.countEligibleStudent(82001L, 1001L)).thenReturn(0L);

        BizException ex = assertThrows(BizException.class, () -> assignmentService.detailForStudent(82001L, 1001L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void detailForStudent_shouldPassWhenUserInTargets() {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(82001L);
        assignment.setPublishStatus(AssignmentPublishStatusEnum.CLOSED.getCode());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.countEligibleStudent(82001L, 1001L)).thenReturn(1L);

        QbAssignment detail = assignmentService.detailForStudent(82001L, 1001L);

        assertNotNull(detail);
        assertEquals(82001L, detail.getId());
    }

    @Test
    void setTargets_shouldPersistWholeClassAndSelectedStudents() {
        QbAssignment assignment = manageableAssignment(82001L, 7001L, AssignmentPublishStatusEnum.DRAFT.getCode());
        QbClass clazz = ownedClass(31L, 7001L);
        AssignmentTargetSelectionDTO wholeClass = selection(31L, List.of());
        AssignmentTargetSelectionDTO selectedStudents = selection(32L, List.of(1001L, 1002L));
        QbClass secondClass = ownedClass(32L, 7001L);
        AssignmentTargetsRequest request = new AssignmentTargetsRequest();
        request.setTargets(List.of(wholeClass, selectedStudents));

        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(classMapper.selectById(31L)).thenReturn(clazz);
        when(classMapper.selectById(32L)).thenReturn(secondClass);
        when(classMemberMapper.countByClassAndStudent(32L, 1001L)).thenReturn(1L);
        when(classMemberMapper.countByClassAndStudent(32L, 1002L)).thenReturn(1L);

        assignmentService.setTargets(82001L, request, 7001L, false);

        verify(targetMapper).deleteByAssignmentId(82001L);
        verify(targetMapper).batchInsertClasses(82001L, List.of(31L));
        verify(targetMapper).batchInsertStudents(82001L, List.of(1001L, 1002L));
    }

    @Test
    void setTargets_shouldRejectPublishedAssignmentAfterAnyAttempt() {
        QbAssignment assignment = manageableAssignment(82001L, 7001L, AssignmentPublishStatusEnum.PUBLISHED.getCode());
        AssignmentTargetsRequest request = new AssignmentTargetsRequest();
        request.setTargets(List.of());
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(attemptMapper.countAllByAssignmentId(82001L)).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
                () -> assignmentService.setTargets(82001L, request, 7001L, false));

        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void setTargets_shouldRetainExistingDirectStudentsWhenRequested() {
        QbAssignment assignment = manageableAssignment(82001L, 7001L, AssignmentPublishStatusEnum.DRAFT.getCode());
        AssignmentTargetsRequest request = new AssignmentTargetsRequest();
        request.setTargets(List.of());
        request.setRetainedStudentIds(List.of(1001L));
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.listStudentIdsByAssignmentId(82001L)).thenReturn(List.of(1001L));

        assignmentService.setTargets(82001L, request, 7001L, false);

        verify(targetMapper).batchInsertStudents(82001L, List.of(1001L));
    }

    @Test
    void setTargets_shouldRejectRetentionOfUnknownDirectStudent() {
        QbAssignment assignment = manageableAssignment(82001L, 7001L, AssignmentPublishStatusEnum.DRAFT.getCode());
        AssignmentTargetsRequest request = new AssignmentTargetsRequest();
        request.setTargets(List.of());
        request.setRetainedStudentIds(List.of(1002L));
        when(assignmentMapper.selectById(82001L)).thenReturn(assignment);
        when(targetMapper.listStudentIdsByAssignmentId(82001L)).thenReturn(List.of(1001L));

        BizException ex = assertThrows(BizException.class,
                () -> assignmentService.setTargets(82001L, request, 7001L, false));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    private QbAssignment manageableAssignment(Long id, Long creatorId, int status) {
        QbAssignment assignment = new QbAssignment();
        assignment.setId(id);
        assignment.setCreatedBy(creatorId);
        assignment.setPublishStatus(status);
        return assignment;
    }

    private QbClass ownedClass(Long id, Long teacherId) {
        QbClass clazz = new QbClass();
        clazz.setId(id);
        clazz.setTeacherId(teacherId);
        return clazz;
    }

    private AssignmentTargetSelectionDTO selection(Long classId, List<Long> studentIds) {
        AssignmentTargetSelectionDTO selection = new AssignmentTargetSelectionDTO();
        selection.setClassId(classId);
        selection.setStudentIds(studentIds);
        return selection;
    }
}
