package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.SaveAnswerDraftRequest;
import com.xyz.question_bank_management_system.modules.bank.service.AttemptService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class AnswerController {

    private final AttemptService attemptService;

    @PutMapping("/{answerId}/draft")
    public ApiResponse<Void> saveDraft(@PathVariable Long answerId, @RequestBody @Valid SaveAnswerDraftRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        attemptService.saveDraft(answerId, uid, request);
        return ApiResponse.ok();
    }

    @PutMapping("/{answerId}/submit")
    public ApiResponse<Void> submitAnswer(@PathVariable Long answerId, @RequestBody @Valid SaveAnswerDraftRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        attemptService.submitAnswer(answerId, uid, request);
        return ApiResponse.ok();
    }
}
