package com.xyz.question_bank_management_system.modules.profile.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.profile.service.StageLearningEvaluationService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stage-evaluations")
public class StageLearningEvaluationController {

    private final StageLearningEvaluationService stageLearningEvaluationService;

    public StageLearningEvaluationController(StageLearningEvaluationService stageLearningEvaluationService) {
        this.stageLearningEvaluationService = stageLearningEvaluationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StageLearningEvaluationVO> myEvaluation(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(stageLearningEvaluationService.myEvaluation(
                SecurityContextUtil.getUserId(),
                stage,
                startDate,
                endDate
        ));
    }

    @GetMapping("/teacher/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<StageLearningEvaluationVO>> teacherEvaluations(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(stageLearningEvaluationService.teacherEvaluations(
                SecurityContextUtil.getUserId(),
                admin,
                studentId,
                stage,
                startDate,
                endDate
        ));
    }
}
