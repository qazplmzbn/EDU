package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.service.AttemptService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/assignment/{assignmentId}/start")
    public ApiResponse<AttemptStartVO> startAssignment(@PathVariable Long assignmentId) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(attemptService.startAssignmentAttempt(assignmentId, uid));
    }

    @PostMapping("/practice/start")
    public ApiResponse<AttemptStartVO> startPractice(@RequestBody @Valid PracticeStartRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(attemptService.startPracticeAttempt(request, uid));
    }

    //作答业
    @GetMapping("/{attemptId}/questions")
    public ApiResponse<List<AttemptQuestionVO>> questions(@PathVariable Long attemptId) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(attemptService.getAttemptQuestions(attemptId, uid));
    }

    @PostMapping("/{attemptId}/submit")
    public ApiResponse<Void> submit(@PathVariable Long attemptId) {
        Long uid = SecurityContextUtil.getUserId();
        attemptService.submitAttempt(attemptId, uid);
        return ApiResponse.ok();
    }

    @GetMapping("/{attemptId}/result")
    public ApiResponse<AttemptResultVO> result(@PathVariable Long attemptId) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(attemptService.result(attemptId, uid));
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<QbAttempt>> my(@RequestParam(required = false) Integer attemptType,
                                                    @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(attemptService.myAttempts(attemptType, page, size, uid));
    }
}
