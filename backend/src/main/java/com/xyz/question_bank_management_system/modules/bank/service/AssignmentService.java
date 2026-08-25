package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetsRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentTargetConfigVO;

public interface AssignmentService {

    Long create(AssignmentUpsertRequest request, Long creatorId, boolean isAdmin);

    void update(Long assignmentId, AssignmentUpsertRequest request, Long actorId, boolean isAdmin);

    void delete(Long assignmentId, Long actorId, boolean isAdmin);

    void publish(Long assignmentId, Long actorId, boolean isAdmin);

    void close(Long assignmentId, Long actorId, boolean isAdmin);

    void setTargets(Long assignmentId, AssignmentTargetsRequest request, Long actorId, boolean isAdmin);

    AssignmentTargetConfigVO getTargets(Long assignmentId, Long actorId, boolean isAdmin);

    void removeStudentTarget(Long assignmentId, Long studentId, Long actorId, boolean isAdmin);

    /**
     * teacherId: 教师本人（教师端列表）；isAdmin=true 时忽略 teacherId 返回全量。
     */
    PageResponse<QbAssignment> pageMineOrAll(long page, long size, String keyword, Long teacherId, boolean isAdmin);

    QbAssignment detail(Long assignmentId, Long actorId, boolean isAdmin);

    QbAssignment detailForStudent(Long assignmentId, Long userId);

    PageResponse<AssignmentMyItemVO> pageForStudent(String status, long page, long size, Long userId);
}
