package com.xyz.question_bank_management_system.modules.profile.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.profile.service.StageLearningEvaluationService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import com.xyz.question_bank_management_system.modules.profile.dto.StageEvaluationGenerateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StageLearningEvaluationVO> generate(@RequestBody @Valid StageEvaluationGenerateRequest request) {
        boolean admin = SecurityContextUtil.currentRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(stageLearningEvaluationService.generate(SecurityContextUtil.getUserId(), admin, request.getStudentId(), request.getStage(), request.getStartDate(), request.getEndDate()));
    }

    @GetMapping("/my/history")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StageLearningEvaluationVO>> myHistory(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(stageLearningEvaluationService.history(SecurityContextUtil.getUserId(), true, SecurityContextUtil.getUserId(), limit));
    }

    @GetMapping("/teacher/students/{studentId}/history")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<StageLearningEvaluationVO>> history(@PathVariable Long studentId, @RequestParam(defaultValue = "20") int limit) {
        boolean admin = SecurityContextUtil.currentRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(stageLearningEvaluationService.history(SecurityContextUtil.getUserId(), admin, studentId, limit));
    }
}
