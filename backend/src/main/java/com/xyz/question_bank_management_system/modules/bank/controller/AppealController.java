package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.service.AppealService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.AppealMyItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Map<String, Long>> submit(@RequestBody @Valid AppealCreateRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        Long appealId = appealService.submitAppeal(request, userId);
        return ApiResponse.ok("提交成功", Map.of("appealId", appealId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PageResponse<AppealMyItemVO>> my(@RequestParam(required = false) Integer status,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(appealService.pageMyAppeals(userId, status, page, size));
    }
}
