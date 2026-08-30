package com.xyz.question_bank_management_system.modules.knowledge.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.knowledge.service.CourseGraphImportService;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphImportDetailVO;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphValidationVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/course-graph-imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CourseGraphImportController {
    private final CourseGraphImportService service;

    @PostMapping(value="/validate",consumes="multipart/form-data")
    public ApiResponse<CourseGraphValidationVO> validate(@RequestPart("file")MultipartFile file,
                                                          @RequestParam(defaultValue="STRUCTURE_ONLY")String mode) {
        return ApiResponse.ok(service.validate(file, mode));
    }

    @PostMapping(consumes="multipart/form-data")
    public ApiResponse<CourseGraphImportDetailVO> commit(@RequestPart("file")MultipartFile file,
                                                          @RequestParam String validationHash,
                                                          @RequestParam(defaultValue="STRUCTURE_ONLY")String mode,
                                                          @RequestHeader("Idempotency-Key")String idempotencyKey) {
        return ApiResponse.ok(service.commit(file, mode, validationHash, idempotencyKey, SecurityContextUtil.getUserId()));
    }

    @GetMapping("/{importCode}")
    public ApiResponse<CourseGraphImportDetailVO> detail(@PathVariable String importCode) {
        return ApiResponse.ok(service.detail(importCode));
    }

    @PostMapping("/{importCode}/approve")
    public ApiResponse<CourseGraphImportDetailVO> approve(@PathVariable String importCode) {
        return ApiResponse.ok(service.approve(importCode, SecurityContextUtil.getUserId()));
    }
}
