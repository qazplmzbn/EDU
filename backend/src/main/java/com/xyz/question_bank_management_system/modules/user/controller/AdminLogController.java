package com.xyz.question_bank_management_system.modules.user.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.user.service.AdminLogService;
import com.xyz.question_bank_management_system.modules.user.vo.AdminAuditLogItemVO;
import com.xyz.question_bank_management_system.modules.user.vo.AdminLoginLogItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping("/login-logs")
    public ApiResponse<PageResponse<AdminLoginLogItemVO>> loginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean successFlag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.ok(adminLogService.pageLoginLogs(username, successFlag, startTime, endTime, page, size));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AdminAuditLogItemVO>> auditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.ok(adminLogService.pageAuditLogs(username, action, entityType, startTime, endTime, page, size));
    }
}
