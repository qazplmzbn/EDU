package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportCommitRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest;
import com.xyz.question_bank_management_system.modules.competency.service.CompetencyImportService;
import com.xyz.question_bank_management_system.modules.competency.vo.DataSyncRecordVO;
import com.xyz.question_bank_management_system.modules.competency.vo.ImportValidationVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/competency")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCompetencyImportController {
    private final CompetencyImportService service;
    @PostMapping("/import/validate") public ApiResponse<ImportValidationVO> validate(@RequestBody CompetencyImportValidateRequest request){return ApiResponse.ok(service.validate(request));}
    @PostMapping("/import") public ApiResponse<ImportValidationVO> commit(@RequestBody CompetencyImportCommitRequest request){return ApiResponse.ok(service.commit(request, SecurityContextUtil.getUserId()));}
    @GetMapping("/import-records") public ApiResponse<PageResponse<DataSyncRecordVO>> records(@RequestParam(required=false) Integer page,@RequestParam(required=false) Integer size){return ApiResponse.ok(service.records(page,size));}
    @GetMapping("/import-records/{id}") public ApiResponse<DataSyncRecordVO> record(@PathVariable Long id){return ApiResponse.ok(service.record(id));}
}
