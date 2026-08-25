package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.AssignmentPublishStatusEnum;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetsRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetSelectionDTO;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperMapper;
import com.xyz.question_bank_management_system.modules.org.entity.QbClass;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import com.xyz.question_bank_management_system.modules.bank.service.AssignmentService;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentTargetClassVO;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentTargetConfigVO;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentTargetStudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final QbAssignmentMapper assignmentMapper;
    private final QbAssignmentTargetMapper targetMapper;
    private final QbAttemptMapper attemptMapper;
    private final QbPaperMapper paperMapper;
    private final QbClassMapper classMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Long create(AssignmentUpsertRequest request, Long creatorId, boolean isAdmin) {
        ensurePaperUsable(request.getPaperId(), creatorId, isAdmin);
        validateTimeRange(request);

        QbAssignment a = new QbAssignment();
        applyAssignmentUpsert(a, request);
        a.setPublishStatus(AssignmentPublishStatusEnum.DRAFT.getCode());
        a.setCreatedBy(creatorId);
        assignmentMapper.insert(a);
        recordAudit(creatorId, "ASSIGNMENT_CREATE", "ASSIGNMENT", a.getId(), null, assignmentAuditSnapshot(a));
        return a.getId();
    }

    @Override
    @Transactional
    public void update(Long assignmentId, AssignmentUpsertRequest request, Long actorId, boolean isAdmin) {
        QbAssignment a = loadAssignmentForManage(assignmentId, actorId, isAdmin);
        Map<String, Object> before = assignmentAuditSnapshot(a);

        ensurePaperUsable(request.getPaperId(), actorId, isAdmin);
        validateTimeRange(request);

        applyAssignmentUpsert(a, request);
        assignmentMapper.update(a);
        recordAudit(actorId, "ASSIGNMENT_UPDATE", "ASSIGNMENT", assignmentId, before, assignmentAuditSnapshot(a));
    }

    @Override
    @Transactional
    public void delete(Long assignmentId, Long actorId, boolean isAdmin) {
        QbAssignment assignment = loadAssignmentForManage(assignmentId, actorId, isAdmin);
        assignmentMapper.softDelete(assignmentId);
        targetMapper.deleteByAssignmentId(assignmentId);
        recordAudit(actorId, "ASSIGNMENT_DELETE", "ASSIGNMENT", assignmentId, assignmentAuditSnapshot(assignment), null);
    }

    @Override
    public void publish(Long assignmentId, Long actorId, boolean isAdmin) {
        changePublishStatus(assignmentId, actorId, isAdmin, AssignmentPublishStatusEnum.PUBLISHED, "ASSIGNMENT_PUBLISH");
    }

    @Override
    public void close(Long assignmentId, Long actorId, boolean isAdmin) {
        changePublishStatus(assignmentId, actorId, isAdmin, AssignmentPublishStatusEnum.CLOSED, "ASSIGNMENT_CLOSE");
    }

    @Override
    @Transactional
    public void setTargets(Long assignmentId, AssignmentTargetsRequest request, Long actorId, boolean isAdmin) {
        QbAssignment assignment = loadAssignmentForManage(assignmentId, actorId, isAdmin);
        ensureTargetsMutable(assignment);
        if (request == null || request.getTargets() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "targets 不能为空");
        }

        Map<String, Object> before = targetAuditSnapshot(assignmentId);
        LinkedHashSet<Long> classIds = new LinkedHashSet<>();
        LinkedHashSet<Long> studentIds = new LinkedHashSet<>();
        LinkedHashSet<Long> existingDirectStudentIds = new LinkedHashSet<>(targetMapper.listStudentIdsByAssignmentId(assignmentId));
        List<Long> retainedStudentIds = request.getRetainedStudentIds() == null
                ? new java.util.ArrayList<>(existingDirectStudentIds)
                : distinctIds(request.getRetainedStudentIds());
        if (!existingDirectStudentIds.containsAll(retainedStudentIds)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "只能保留当前已配置的独立学生目标");
        }
        studentIds.addAll(retainedStudentIds);
        for (AssignmentTargetSelectionDTO selection : request.getTargets()) {
            if (selection == null || selection.getClassId() == null || selection.getStudentIds() == null) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "每个目标组都必须包含 classId 和 studentIds");
            }
            Long classId = selection.getClassId();
            if (!classIds.add(classId)) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "同一班级不能重复配置目标组");
            }
            QbClass clazz = classMapper.selectById(classId);
            if (clazz == null) {
                throw BizException.of(ErrorCode.NOT_FOUND, "目标班级不存在");
            }
            if (!isAdmin && !Objects.equals(clazz.getTeacherId(), actorId)) {
                throw BizException.of(ErrorCode.FORBIDDEN, "只能为自己负责的班级配置作业目标");
            }
            List<Long> selectedStudents = distinctIds(selection.getStudentIds());
            if (selectedStudents.isEmpty()) {
                continue;
            }
            for (Long studentId : selectedStudents) {
                if (studentId <= 0 || classMemberMapper.countByClassAndStudent(classId, studentId) <= 0) {
                    throw BizException.of(ErrorCode.PARAM_ERROR, "指定学生不是目标班级的当前成员");
                }
                studentIds.add(studentId);
            }
        }

        List<Long> wholeClassIds = request.getTargets().stream()
                .filter(Objects::nonNull)
                .filter(selection -> selection.getStudentIds() != null && selection.getStudentIds().isEmpty())
                .map(AssignmentTargetSelectionDTO::getClassId)
                .toList();
        targetMapper.deleteByAssignmentId(assignmentId);
        if (!wholeClassIds.isEmpty()) {
            targetMapper.batchInsertClasses(assignmentId, wholeClassIds);
        }
        if (!studentIds.isEmpty()) {
            targetMapper.batchInsertStudents(assignmentId, new java.util.ArrayList<>(studentIds));
        }
        recordAudit(actorId, "ASSIGNMENT_SET_TARGETS", "ASSIGNMENT", assignmentId, before, targetAuditSnapshot(assignmentId));
    }

    @Override
    public AssignmentTargetConfigVO getTargets(Long assignmentId, Long actorId, boolean isAdmin) {
        loadAssignmentForManage(assignmentId, actorId, isAdmin);
        return buildTargetConfig(assignmentId);
    }

    @Override
    @Transactional
    public void removeStudentTarget(Long assignmentId, Long studentId, Long actorId, boolean isAdmin) {
        QbAssignment assignment = loadAssignmentForManage(assignmentId, actorId, isAdmin);
        ensureTargetsMutable(assignment);
        Map<String, Object> before = targetAuditSnapshot(assignmentId);
        targetMapper.deleteStudentTarget(assignmentId, studentId);
        recordAudit(actorId, "ASSIGNMENT_REMOVE_STUDENT_TARGET", "ASSIGNMENT", assignmentId, before, targetAuditSnapshot(assignmentId));
    }

    @Override
    public PageResponse<QbAssignment> pageMineOrAll(long page, long size, String keyword, Long teacherId, boolean isAdmin) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        if (isAdmin) {
            List<QbAssignment> rows = assignmentMapper.pageAll(keyword, offset, safeSize);
            long total = assignmentMapper.countAll(keyword);
            return PageResponse.of(safePage, safeSize, total, rows);
        }
        List<QbAssignment> rows = assignmentMapper.pageByTeacher(teacherId, keyword, offset, safeSize);
        long total = assignmentMapper.countByTeacher(teacherId, keyword);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public QbAssignment detail(Long assignmentId, Long actorId, boolean isAdmin) {
        return loadAssignmentForManage(assignmentId, actorId, isAdmin);
    }

    @Override
    public QbAssignment detailForStudent(Long assignmentId, Long userId) {
        QbAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作业不存在");
        }
        if (a.getPublishStatus() == null || a.getPublishStatus() == AssignmentPublishStatusEnum.DRAFT.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "该作业当前不可用");
        }

        if (targetMapper.countEligibleStudent(assignmentId, userId) <= 0) {
            throw BizException.of(ErrorCode.FORBIDDEN, "你不在该作业的目标名单中");
        }
        return a;
    }

    @Override
    public PageResponse<AssignmentMyItemVO> pageForStudent(String status, long page, long size, Long userId) {
        String safeStatus = normalizeStudentStatus(status);
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);
        LocalDateTime now = LocalDateTime.now();

        List<AssignmentMyItemVO> rows = assignmentMapper.pageForStudent(userId, safeStatus, now, offset, safeSize);
        long total = assignmentMapper.countForStudent(userId, safeStatus, now);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    private void applyAssignmentUpsert(QbAssignment assignment, AssignmentUpsertRequest request) {
        assignment.setPaperId(request.getPaperId());
        assignment.setAssignmentTitle(request.getAssignmentTitle());
        assignment.setAssignmentDesc(request.getAssignmentDesc());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setTimeLimitMin(request.getTimeLimitMin());
        assignment.setMaxAttempts(request.getMaxAttempts());
        assignment.setShuffleQuestions(0);
        assignment.setShuffleOptions(request.getShuffleOptions());
    }

    private void changePublishStatus(Long assignmentId,
                                     Long actorId,
                                     boolean isAdmin,
                                     AssignmentPublishStatusEnum targetStatus,
                                     String auditAction) {
        QbAssignment assignment = loadAssignmentForManage(assignmentId, actorId, isAdmin);
        Map<String, Object> before = assignmentAuditSnapshot(assignment);
        assignmentMapper.updatePublishStatus(assignmentId, targetStatus.getCode());
        assignment.setPublishStatus(targetStatus.getCode());
        recordAudit(actorId, auditAction, "ASSIGNMENT", assignmentId, before, assignmentAuditSnapshot(assignment));
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void ensureTargetsMutable(QbAssignment assignment) {
        Integer status = assignment.getPublishStatus();
        if (status != null && status == AssignmentPublishStatusEnum.CLOSED.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "作业已关闭，不能修改目标范围");
        }
        if (status != null && status == AssignmentPublishStatusEnum.PUBLISHED.getCode()
                && attemptMapper.countAllByAssignmentId(assignment.getId()) > 0) {
            throw BizException.of(ErrorCode.FORBIDDEN, "已有作答记录，不能修改目标范围");
        }
    }

    private AssignmentTargetConfigVO buildTargetConfig(Long assignmentId) {
        AssignmentTargetConfigVO config = new AssignmentTargetConfigVO();
        List<AssignmentTargetClassVO> classTargets = targetMapper.listClassIdsByAssignmentId(assignmentId).stream()
                .map(classMapper::selectById)
                .filter(Objects::nonNull)
                .map(clazz -> {
                    AssignmentTargetClassVO vo = new AssignmentTargetClassVO();
                    vo.setClassId(clazz.getId());
                    vo.setClassName(clazz.getClassName());
                    vo.setClassCode(clazz.getClassCode());
                    return vo;
                })
                .toList();
        List<AssignmentTargetStudentVO> studentTargets = targetMapper.listStudentIdsByAssignmentId(assignmentId).stream()
                .map(studentId -> {
                    SysUser student = sysUserMapper.selectById(studentId);
                    AssignmentTargetStudentVO vo = new AssignmentTargetStudentVO();
                    vo.setStudentId(studentId);
                    if (student != null) {
                        vo.setUsername(student.getUsername());
                        vo.setDisplayName(student.getDisplayName());
                    }
                    return vo;
                })
                .toList();
        config.setClassTargets(classTargets);
        config.setStudentTargets(studentTargets);
        return config;
    }

    private Map<String, Object> targetAuditSnapshot(Long assignmentId) {
        AssignmentTargetConfigVO config = buildTargetConfig(assignmentId);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("classTargets", config.getClassTargets());
        snapshot.put("studentTargets", config.getStudentTargets());
        return snapshot;
    }

    private void ensurePaperUsable(Long paperId, Long actorId, boolean isAdmin) {
        QbPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        if (!isAdmin && !Objects.equals(paper.getCreatorId(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权使用该试卷");
        }
    }

    private void validateTimeRange(AssignmentUpsertRequest request) {
        if (request.getEndTime() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "结束时间不能为空");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "结束时间不能早于开始时间");
        }
    }

    private String normalizeStudentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "all";
        }
        String normalized = status.trim().toLowerCase();
        if ("ongoing".equals(normalized) || "expired".equals(normalized) || "all".equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, "作业状态参数不合法");
    }

    private QbAssignment loadAssignmentForManage(Long assignmentId, Long actorId, boolean isAdmin) {
        QbAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作业不存在");
        }
        if (!isAdmin && !Objects.equals(a.getCreatedBy(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权管理该作业");
        }
        return a;
    }

    private Map<String, Object> assignmentAuditSnapshot(QbAssignment assignment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", assignment.getId());
        snapshot.put("paperId", assignment.getPaperId());
        snapshot.put("assignmentTitle", assignment.getAssignmentTitle());
        snapshot.put("assignmentDesc", assignment.getAssignmentDesc());
        snapshot.put("startTime", assignment.getStartTime());
        snapshot.put("endTime", assignment.getEndTime());
        snapshot.put("timeLimitMin", assignment.getTimeLimitMin());
        snapshot.put("maxAttempts", assignment.getMaxAttempts());
        snapshot.put("shuffleOptions", assignment.getShuffleOptions());
        snapshot.put("publishStatus", assignment.getPublishStatus());
        snapshot.put("createdBy", assignment.getCreatedBy());
        return snapshot;
    }

    private void recordAudit(Long userId,
                             String action,
                             String entityType,
                             Long entityId,
                             Object beforeData,
                             Object afterData) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.record(userId, action, entityType, entityId, beforeData, afterData);
    }
}
