package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealHandleRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TeacherGradeRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TeacherLlmBatchRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TeacherLlmRetryRequest;
import com.xyz.question_bank_management_system.modules.llm.service.AdminLlmModelService;
import com.xyz.question_bank_management_system.modules.bank.service.AppealService;
import com.xyz.question_bank_management_system.modules.bank.service.TeacherReviewService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAnswerEvidenceVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAppealItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentScoreItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentStudentDetailVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentTargetItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherLlmBatchGradeVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherReviewAnswerItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherReviewController {

    private final TeacherReviewService teacherReviewService;
    private final AppealService appealService;
    private final AdminLlmModelService adminLlmModelService;

    @GetMapping("/review/answers")
    public ApiResponse<PageResponse<TeacherReviewAnswerItemVO>> reviewAnswers(
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Boolean needsReview,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(teacherReviewService.reviewAnswers(
                assignmentId,
                studentId,
                questionType,
                needsReview,
                page,
                size,
                actorId,
                isAdmin
        ));
    }

    @GetMapping("/answers/{answerId}/evidence")
    public ApiResponse<TeacherAnswerEvidenceVO> evidence(@PathVariable Long answerId) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(teacherReviewService.evidence(answerId, actorId, isAdmin));
    }

    @PostMapping("/answers/{answerId}/grade")
    public ApiResponse<Void> manualGrade(@PathVariable Long answerId, @RequestBody @Valid TeacherGradeRequest request) {
        Long reviewerId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        teacherReviewService.manualGrade(answerId, request.getScore(), request.getComment(), reviewerId, isAdmin);
        return ApiResponse.ok();
    }

    @PostMapping("/answers/{answerId}/llm-retry")
    public ApiResponse<Map<String, List<Long>>> llmRetry(@PathVariable Long answerId,
                                                         @RequestBody(required = false) @Valid TeacherLlmRetryRequest request) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        String providerKey = resolveProviderKey(request);
        Double temperature = request == null ? null : request.getTemperature();
        Integer times = request == null ? 1 : request.getTimes();
        List<Long> llmCallIds = teacherReviewService.llmRetry(answerId, providerKey, temperature, times, actorId, isAdmin);
        return ApiResponse.ok("已触发 LLM 批改", Map.of("llmCallIds", llmCallIds));
    }

    @PostMapping("/answers/llm-batch")
    public ApiResponse<TeacherLlmBatchGradeVO> llmBatch(@RequestBody @Valid TeacherLlmBatchRequest request) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        String providerKey = request.getProviderKey() == null || request.getProviderKey().isBlank()
                ? request.getModelName()
                : request.getProviderKey();
        return ApiResponse.ok(teacherReviewService.llmBatch(
                request.getAnswerIds(),
                request.getAssignmentId(),
                request.getNeedsReview(),
                providerKey,
                request.getTemperature(),
                request.getTimes(),
                actorId,
                isAdmin
        ));
    }

    @GetMapping("/llm/providers")
    public ApiResponse<List<StudentLlmProviderVO>> teacherLlmProviders(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String providerType) {
        return ApiResponse.ok(adminLlmModelService.providers(keyword, providerType, true, true));
    }

    @GetMapping("/assignments/{assignmentId}/scores")
    public ApiResponse<PageResponse<TeacherAssignmentScoreItemVO>> assignmentScores(@PathVariable Long assignmentId,
                                                                                    @RequestParam(defaultValue = "1") long page,
                                                                                    @RequestParam(defaultValue = "10") long size) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(teacherReviewService.assignmentScores(assignmentId, page, size, actorId, isAdmin));
    }

    @GetMapping("/assignments/{assignmentId}/targets")
    public ApiResponse<PageResponse<TeacherAssignmentTargetItemVO>> assignmentTargets(@PathVariable Long assignmentId,
                                                                                      @RequestParam(defaultValue = "1") long page,
                                                                                      @RequestParam(defaultValue = "10") long size) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(teacherReviewService.assignmentTargets(assignmentId, page, size, actorId, isAdmin));
    }

    @GetMapping("/assignments/{assignmentId}/targets/{studentId}")
    public ApiResponse<TeacherAssignmentStudentDetailVO> assignmentStudentDetail(@PathVariable Long assignmentId,
                                                                                 @PathVariable Long studentId) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(teacherReviewService.assignmentStudentDetail(assignmentId, studentId, actorId, isAdmin));
    }

    @GetMapping("/appeals")
    public ApiResponse<PageResponse<TeacherAppealItemVO>> appeals(@RequestParam(required = false) Integer status,
                                                                  @RequestParam(defaultValue = "1") long page,
                                                                  @RequestParam(defaultValue = "10") long size) {
        Long actorId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(appealService.pageTeacherAppeals(status, page, size, actorId, isAdmin));
    }

    @PostMapping("/appeals/{appealId}/handle")
    public ApiResponse<Void> handleAppeal(@PathVariable Long appealId, @RequestBody @Valid AppealHandleRequest request) {
        Long handlerId = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        appealService.handleAppeal(appealId, request, handlerId, isAdmin);
        return ApiResponse.ok();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private String resolveProviderKey(TeacherLlmRetryRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getProviderKey() != null && !request.getProviderKey().isBlank()) {
            return request.getProviderKey();
        }
        return request.getModelName();
    }
}
