package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.dto.CareerRecommendationRefreshRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.OccupationSkillStandardPublishRequest;
import com.xyz.question_bank_management_system.modules.competency.service.OccupationCareerRecommendationService;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerGapVO;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerRecommendationVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/competency")
@RequiredArgsConstructor
public class OccupationCareerRecommendationController {
    private final OccupationCareerRecommendationService service;

    @PostMapping("/admin/occupations/{occupationId}/skill-standards/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> publishStandard(@PathVariable Long occupationId, @RequestBody @Valid OccupationSkillStandardPublishRequest request) {
        service.publishStandard(occupationId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/career/gaps/refresh")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CareerGapVO> refreshGaps(@RequestParam Long occupationId) {
        return ApiResponse.ok(service.refreshGaps(SecurityContextUtil.getUserId(), occupationId));
    }

    @GetMapping("/career/gaps")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CareerGapVO> latestGaps(@RequestParam Long occupationId) {
        return ApiResponse.ok(service.latestGaps(SecurityContextUtil.getUserId(), occupationId));
    }

    @PostMapping("/career/recommendations/refresh")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CareerRecommendationVO> refreshRecommendations(@RequestBody(required = false) CareerRecommendationRefreshRequest request) {
        return ApiResponse.ok(service.refreshRecommendations(SecurityContextUtil.getUserId(), request == null ? new CareerRecommendationRefreshRequest() : request));
    }

    @GetMapping("/career/recommendations/{snapshotCode}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CareerRecommendationVO> recommendation(@PathVariable String snapshotCode) {
        return ApiResponse.ok(service.recommendation(SecurityContextUtil.getUserId(), snapshotCode));
    }

    @PostMapping("/career/recommendations/{snapshotCode}/courses/{courseId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<java.util.Map<String, Object>> accept(@PathVariable String snapshotCode, @PathVariable Long courseId) {
        return ApiResponse.ok(service.acceptRecommendation(SecurityContextUtil.getUserId(), snapshotCode, courseId));
    }

    @GetMapping("/career/report")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<java.util.Map<String, Object>> report(@RequestParam Long occupationId) {
        return ApiResponse.ok(service.report(SecurityContextUtil.getUserId(), occupationId));
    }

    @GetMapping("/career/diagnostic-targets")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> diagnosticTargets(@RequestParam Long occupationId) {
        return ApiResponse.ok(service.diagnosticTargets(SecurityContextUtil.getUserId(), occupationId));
    }

    @PostMapping("/career/diagnostic-resource-unit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<java.util.Map<String, Object>> createDiagnosticResourceUnit(@RequestParam Long occupationId) {
        return ApiResponse.ok(service.createDiagnosticResourceUnit(SecurityContextUtil.getUserId(), occupationId));
    }
}
