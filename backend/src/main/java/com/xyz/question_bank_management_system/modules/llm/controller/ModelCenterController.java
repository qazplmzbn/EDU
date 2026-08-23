package com.xyz.question_bank_management_system.modules.llm.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.llm.service.ModelCenterService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ModelCenterController {

    private final ModelCenterService modelCenterService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> models(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false, name = "model_type") String modelType,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(modelCenterService.listModels(SecurityContextUtil.currentUserId(), keyword, category, modelType, status, enabled));
    }

    @GetMapping("/{modelId}")
    public ApiResponse<Map<String, Object>> modelDetail(@PathVariable Long modelId) {
        return ApiResponse.ok(modelCenterService.getModel(SecurityContextUtil.currentUserId(), modelId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createModel(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(modelCenterService.createModel(SecurityContextUtil.currentUserId(), isAdmin(), payload));
    }

    @PutMapping("/{modelId}")
    public ApiResponse<Map<String, Object>> updateModel(@PathVariable Long modelId,
                                                        @RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(modelCenterService.updateModel(SecurityContextUtil.currentUserId(), isAdmin(), modelId, payload));
    }

    @DeleteMapping("/{modelId}")
    public ApiResponse<Void> deleteModel(@PathVariable Long modelId) {
        modelCenterService.deleteModel(SecurityContextUtil.currentUserId(), isAdmin(), modelId);
        return ApiResponse.ok();
    }

    @PutMapping("/{modelId}/api-key")
    public ApiResponse<Map<String, Object>> updateModelApiKey(@PathVariable Long modelId,
                                                              @RequestBody Map<String, Object> payload) {
        String apiKey = payload.get("apiKey") == null ? null : String.valueOf(payload.get("apiKey"));
        return ApiResponse.ok(modelCenterService.updateModelApiKey(SecurityContextUtil.currentUserId(), isAdmin(), modelId, apiKey));
    }

    @PostMapping("/{modelId}/test")
    public ApiResponse<Map<String, Object>> testModel(@PathVariable Long modelId,
                                                      @RequestBody(required = false) Map<String, Object> payload) {
        String prompt = payload == null || payload.get("testPrompt") == null
                ? "Please answer with OK"
                : String.valueOf(payload.get("testPrompt"));
        return ApiResponse.ok(modelCenterService.testModel(SecurityContextUtil.currentUserId(), modelId, prompt));
    }

    @PostMapping("/{modelId}/call")
    public ApiResponse<Map<String, Object>> callModel(@PathVariable Long modelId,
                                                      @RequestBody Map<String, Object> payload) {
        String prompt = payload.get("prompt") == null ? "" : String.valueOf(payload.get("prompt"));
        String systemPrompt = payload.get("systemPrompt") == null ? null : String.valueOf(payload.get("systemPrompt"));
        return ApiResponse.ok(modelCenterService.callModel(SecurityContextUtil.currentUserId(), modelId, prompt, systemPrompt));
    }

    @GetMapping("/templates")
    public ApiResponse<List<Map<String, Object>>> templates(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false, name = "task_type") String taskType,
                                                            @RequestParam(required = false, name = "model_id") Long modelId) {
        return ApiResponse.ok(modelCenterService.listTemplates(SecurityContextUtil.currentUserId(), keyword, taskType, modelId));
    }

    @GetMapping("/templates/{templateId}")
    public ApiResponse<Map<String, Object>> templateDetail(@PathVariable Long templateId) {
        return ApiResponse.ok(modelCenterService.getTemplate(SecurityContextUtil.currentUserId(), templateId));
    }

    @PostMapping("/templates")
    public ApiResponse<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(modelCenterService.createTemplate(SecurityContextUtil.currentUserId(), isAdmin(), payload));
    }

    @PutMapping("/templates/{templateId}")
    public ApiResponse<Map<String, Object>> updateTemplate(@PathVariable Long templateId,
                                                           @RequestBody Map<String, Object> payload) {
        return ApiResponse.ok(modelCenterService.updateTemplate(SecurityContextUtil.currentUserId(), isAdmin(), templateId, payload));
    }

    @DeleteMapping("/templates/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long templateId) {
        modelCenterService.deleteTemplate(SecurityContextUtil.currentUserId(), isAdmin(), templateId);
        return ApiResponse.ok();
    }

    @GetMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(@RequestParam(required = false, name = "model_ids") String modelIds) {
        List<Map<String, Object>> models = modelCenterService.compareModels(SecurityContextUtil.currentUserId(), modelIds);
        return ApiResponse.ok(Map.of(
                "models", models,
                "radar_data", Map.of(
                        "dimensions", List.of("accuracy", "precision", "recall", "f1", "auc"),
                        "models", models.stream().map(item -> Map.of(
                                "name", item.get("name"),
                                "values", List.of(
                                        metric(item, "accuracy"),
                                        metric(item, "precision"),
                                        metric(item, "recall"),
                                        metric(item, "f1"),
                                        metric(item, "auc")
                                )
                        )).toList()
                )
        ));
    }

    @GetMapping("/inference-history")
    public ApiResponse<Map<String, Object>> inferenceHistory(@RequestParam(required = false, name = "model_id") Long modelId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(modelCenterService.inferenceHistory(SecurityContextUtil.currentUserId(), modelId, limit));
    }

    @GetMapping("/providers")
    public ApiResponse<List<Map<String, Object>>> providers() {
        return ApiResponse.ok(modelCenterService.listProviders());
    }

    @GetMapping("/ollama/status")
    public ApiResponse<Map<String, Object>> ollamaStatus(@RequestParam(required = false, name = "base_url") String baseUrl) {
        return ApiResponse.ok(modelCenterService.getOllamaStatus(baseUrl));
    }

    @GetMapping("/ollama/models")
    public ApiResponse<Map<String, Object>> ollamaModels(@RequestParam(required = false, name = "base_url") String baseUrl) {
        Map<String, Object> status = modelCenterService.getOllamaStatus(baseUrl);
        return ApiResponse.ok(Map.of(
                "models", status.getOrDefault("models", List.of()),
                "base_url", status.getOrDefault("base_url", baseUrl == null ? "http://localhost:11434" : baseUrl)
        ));
    }

    @GetMapping("/ollama/configured")
    public ApiResponse<List<Map<String, Object>>> configuredOllama() {
        return ApiResponse.ok(modelCenterService.getConfiguredOllamaModels(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/api/configured")
    public ApiResponse<List<Map<String, Object>>> configuredApi() {
        return ApiResponse.ok(modelCenterService.getConfiguredApiModels(SecurityContextUtil.currentUserId()));
    }

    private boolean isAdmin() {
        return SecurityContextUtil.currentRoles().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    @SuppressWarnings("unchecked")
    private double metric(Map<String, Object> item, String key) {
        Object metrics = item.get("metrics");
        if (!(metrics instanceof Map<?, ?> map)) {
            return 0D;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0D : Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0D;
        }
    }
}
