package com.xyz.question_bank_management_system.modules.llm.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderEnabledRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.service.AdminLlmModelService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/llm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLlmModelController {

    private final AdminLlmModelService adminLlmModelService;

    @GetMapping("/providers")
    public ApiResponse<List<StudentLlmProviderVO>> providers(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String providerType,
                                                             @RequestParam(required = false) Boolean enabled,
                                                             @RequestParam(defaultValue = "true") boolean includeReadonly) {
        return ApiResponse.ok(adminLlmModelService.providers(keyword, providerType, enabled, includeReadonly));
    }

    @PostMapping("/providers")
    public ApiResponse<Long> createProvider(@RequestBody @Valid StudentLlmProviderRequest request) {
        return ApiResponse.ok(adminLlmModelService.createProvider(SecurityContextUtil.getUserId(), request));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<Void> updateProvider(@PathVariable Long id,
                                            @RequestBody @Valid StudentLlmProviderRequest request) {
        adminLlmModelService.updateProvider(id, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/providers/{id}/enabled")
    public ApiResponse<Void> updateProviderEnabled(@PathVariable Long id,
                                                   @RequestBody(required = false) StudentLlmProviderEnabledRequest request) {
        adminLlmModelService.updateProviderEnabled(id, request == null ? Boolean.FALSE : request.getEnabled());
        return ApiResponse.ok();
    }

    @PatchMapping("/providers/{id}/default")
    public ApiResponse<Void> markProviderDefault(@PathVariable Long id) {
        adminLlmModelService.markProviderDefault(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id) {
        adminLlmModelService.deleteProvider(id);
        return ApiResponse.ok();
    }

    @GetMapping("/templates")
    public ApiResponse<List<StudentPromptTemplateVO>> templates(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String taskType) {
        return ApiResponse.ok(adminLlmModelService.templates(keyword, taskType));
    }

    @PostMapping("/templates")
    public ApiResponse<Long> createTemplate(@RequestBody @Valid StudentPromptTemplateRequest request) {
        return ApiResponse.ok(adminLlmModelService.createTemplate(SecurityContextUtil.getUserId(), request));
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id,
                                            @RequestBody @Valid StudentPromptTemplateRequest request) {
        adminLlmModelService.updateTemplate(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        adminLlmModelService.deleteTemplate(id);
        return ApiResponse.ok();
    }
}
