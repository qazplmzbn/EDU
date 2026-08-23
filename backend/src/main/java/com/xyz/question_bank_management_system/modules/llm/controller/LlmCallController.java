package com.xyz.question_bank_management_system.modules.llm.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.llm.service.LlmCallQueryService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallDetailVO;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm/calls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class LlmCallController {

    private final LlmCallQueryService llmCallQueryService;

    @GetMapping
    public ApiResponse<PageResponse<LlmCallListItemVO>> page(
            @RequestParam(required = false) Integer bizType,
            @RequestParam(required = false) Long bizId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(llmCallQueryService.page(bizType, bizId, page, size, uid, isAdmin));
    }

    @GetMapping("/{llmCallId}")
    public ApiResponse<LlmCallDetailVO> detail(@PathVariable Long llmCallId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(llmCallQueryService.detail(llmCallId, uid, isAdmin));
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }
}
