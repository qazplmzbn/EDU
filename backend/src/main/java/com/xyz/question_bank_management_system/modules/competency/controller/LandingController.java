package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.service.CompetencyLandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/landing")
@RequiredArgsConstructor
public class LandingController {

    private final CompetencyLandingService competencyLandingService;

    @GetMapping("/competency-layer")
    public ApiResponse<CompetencyLandingService.CompetencyLayerResponse> competencyLayer() {
        return ApiResponse.ok(competencyLandingService.getCompetencyLayer());
    }
}
