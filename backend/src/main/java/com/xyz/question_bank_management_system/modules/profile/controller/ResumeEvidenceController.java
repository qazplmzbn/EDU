package com.xyz.question_bank_management_system.modules.profile.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeDocument;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentResumeEvidence;
import com.xyz.question_bank_management_system.modules.profile.service.ResumeEvidenceService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Canonical student resume API with the former profile route retained. */
@RestController
@RequestMapping({"/api/student/resumes", "/api/student-profile/resumes"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class ResumeEvidenceController {
    private final ResumeEvidenceService service;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<StudentResumeDocument> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String consentVersion) {
        return ApiResponse.ok(service.upload(file, consentVersion, SecurityContextUtil.getUserId()));
    }

    @GetMapping
    public ApiResponse<List<StudentResumeDocument>> documents() {
        return ApiResponse.ok(service.documents(SecurityContextUtil.getUserId()));
    }

    @PostMapping("/{resumeId}/analyze")
    public ApiResponse<StudentResumeDocument> analyze(@PathVariable Long resumeId) {
        return ApiResponse.ok(service.analyze(resumeId, SecurityContextUtil.getUserId()));
    }

    @GetMapping({"/{resumeId}/evidences", "/{resumeId}/evidence"})
    public ApiResponse<List<StudentResumeEvidence>> evidence(@PathVariable Long resumeId) {
        return ApiResponse.ok(service.evidence(resumeId, SecurityContextUtil.getUserId()));
    }

    @PostMapping("/{resumeId}/apply-to-profile")
    public ApiResponse<Map<String, Integer>> applyToProfile(@PathVariable Long resumeId) {
        return ApiResponse.ok(Map.of("appliedCount", service.applyToProfile(resumeId, SecurityContextUtil.getUserId())));
    }

    @PostMapping("/{resumeId}/evidence/{evidenceId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long resumeId,
                                     @PathVariable Long evidenceId,
                                     @RequestParam boolean accepted,
                                     @RequestParam(required = false) Long targetSkillId) {
        service.confirm(resumeId, evidenceId, accepted, targetSkillId, SecurityContextUtil.getUserId());
        return ApiResponse.ok();
    }
}
