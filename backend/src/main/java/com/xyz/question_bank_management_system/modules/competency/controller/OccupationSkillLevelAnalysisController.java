package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkillLevelAnalysis;
import com.xyz.question_bank_management_system.modules.competency.service.OccupationSkillLevelAnalysisService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competency/admin/occupations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OccupationSkillLevelAnalysisController {
    private final OccupationSkillLevelAnalysisService service;

    @PostMapping("/{occupationId}/skill-level-analyses")
    public ApiResponse<OccupationSkillLevelAnalysis> analyze(@PathVariable Long occupationId,
                                                              @RequestParam(defaultValue = "deepseek-flash-test") String providerKey) {
        return ApiResponse.ok(service.analyze(occupationId, providerKey, SecurityContextUtil.getUserId()));
    }

    @GetMapping("/{occupationId}/skill-level-analyses")
    public ApiResponse<List<OccupationSkillLevelAnalysis>> list(@PathVariable Long occupationId,
                                                                 @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(service.list(occupationId, limit));
    }
}
