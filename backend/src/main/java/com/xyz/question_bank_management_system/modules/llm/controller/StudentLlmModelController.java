package com.xyz.question_bank_management_system.modules.llm.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderEnabledRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.service.StudentLlmModelService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/llm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentLlmModelController {

    private final StudentLlmModelService studentLlmModelService;

    @GetMapping("/providers")
    public ApiResponse<List<StudentLlmProviderVO>> providers(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String providerType,
                                                             @RequestParam(required = false) Boolean enabled) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(studentLlmModelService.providers(userId, keyword, providerType, enabled));
    }

    @PostMapping("/providers")
    public ApiResponse<Long> createProvider(@RequestBody @Valid StudentLlmProviderRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(studentLlmModelService.createProvider(userId, request));
    }

    @PutMapping("/providers/{id}")
    public ApiResponse<Void> updateProvider(@PathVariable Long id,
                                            @RequestBody @Valid StudentLlmProviderRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.updateProvider(userId, id, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/providers/{id}/enabled")
    public ApiResponse<Void> updateProviderEnabled(@PathVariable Long id,
                                                   @RequestBody(required = false) StudentLlmProviderEnabledRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.updateProviderEnabled(userId, id, request == null ? Boolean.FALSE : request.getEnabled());
        return ApiResponse.ok();
    }

    @PatchMapping("/providers/{id}/default")
    public ApiResponse<Void> markProviderDefault(@PathVariable Long id) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.markProviderDefault(userId, id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/providers/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.deleteProvider(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping("/templates")
    public ApiResponse<List<StudentPromptTemplateVO>> templates(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String taskType) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(studentLlmModelService.templates(userId, keyword, taskType));
    }

    @PostMapping("/templates")
    public ApiResponse<Long> createTemplate(@RequestBody @Valid StudentPromptTemplateRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(studentLlmModelService.createTemplate(userId, request));
    }

    @PutMapping("/templates/{id}")
    public ApiResponse<Void> updateTemplate(@PathVariable Long id,
                                            @RequestBody @Valid StudentPromptTemplateRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.updateTemplate(userId, id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        Long userId = SecurityContextUtil.getUserId();
        studentLlmModelService.deleteTemplate(userId, id);
        return ApiResponse.ok();
    }
}
