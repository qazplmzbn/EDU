package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.service.CompetencyLandingService;
import com.xyz.question_bank_management_system.modules.competency.vo.CompetencyLandingVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/landing/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLandingJobController {

    private final CompetencyLandingService competencyLandingService;

    @PostMapping("/sync")
    public ApiResponse<CompetencyLandingVO.SyncResult> syncNow() {
        return ApiResponse.ok(competencyLandingService.triggerManualSync(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/sync-records")
    public ApiResponse<List<CompetencyLandingVO.SyncRecordItem>> syncRecords(@RequestParam(required = false) Integer limit) {
        int safeLimit = limit == null ? 10 : limit;
        return ApiResponse.ok(competencyLandingService.recentSyncRecords(safeLimit));
    }
}
