package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentTargetsRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AssignmentUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.service.AssignmentService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.AssignmentMyItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Long> create(@RequestBody @Valid AssignmentUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(assignmentService.create(request, uid, isAdmin));
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long assignmentId, @RequestBody @Valid AssignmentUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        assignmentService.update(assignmentId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long assignmentId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        assignmentService.delete(assignmentId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<PageResponse<QbAssignment>> page(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String keyword) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(assignmentService.pageMineOrAll(page, size, keyword, uid, isAdmin));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<AssignmentMyItemVO>> my(@RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(assignmentService.pageForStudent(status, page, size, uid));
    }

    @GetMapping("/{assignmentId}")
    public ApiResponse<QbAssignment> detail(@PathVariable Long assignmentId) {
        Long uid = SecurityContextUtil.getUserId();
        if (hasRole("ROLE_ADMIN") || hasRole("ROLE_TEACHER")) {
            return ApiResponse.ok(assignmentService.detail(assignmentId, uid, hasRole("ROLE_ADMIN")));
        }
        return ApiResponse.ok(assignmentService.detailForStudent(assignmentId, uid));
    }

    @PostMapping("/{assignmentId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> publish(@PathVariable Long assignmentId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        assignmentService.publish(assignmentId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @PostMapping("/{assignmentId}/close")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> close(@PathVariable Long assignmentId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        assignmentService.close(assignmentId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @PutMapping("/{assignmentId}/targets")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> setTargets(@PathVariable Long assignmentId, @RequestBody @Valid AssignmentTargetsRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        assignmentService.setTargets(assignmentId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }
}
