package com.xyz.question_bank_management_system.modules.llm.service;

import java.util.List;
import java.util.Map;

public interface ModelCenterService {

    List<Map<String, Object>> listModels(Long userId, String keyword, String category, String modelType, String status, Boolean enabled);

    Map<String, Object> getModel(Long userId, Long modelId);

    Map<String, Object> createModel(Long userId, boolean admin, Map<String, Object> payload);

    Map<String, Object> updateModel(Long userId, boolean admin, Long modelId, Map<String, Object> payload);

    void deleteModel(Long userId, boolean admin, Long modelId);

    Map<String, Object> updateModelApiKey(Long userId, boolean admin, Long modelId, String apiKey);

    List<Map<String, Object>> listTemplates(Long userId, String keyword, String taskType, Long modelId);

    Map<String, Object> getTemplate(Long userId, Long templateId);

    Map<String, Object> createTemplate(Long userId, boolean admin, Map<String, Object> payload);

    Map<String, Object> updateTemplate(Long userId, boolean admin, Long templateId, Map<String, Object> payload);

    void deleteTemplate(Long userId, boolean admin, Long templateId);

    List<Map<String, Object>> compareModels(Long userId, String modelIds);

    Map<String, Object> inferenceHistory(Long userId, Long modelId, int limit);

    Map<String, Object> getOllamaStatus(String baseUrl);

    List<Map<String, Object>> getConfiguredOllamaModels(Long userId);

    List<Map<String, Object>> getConfiguredApiModels(Long userId);

    List<Map<String, Object>> listProviders();

    Map<String, Object> testModel(Long userId, Long modelId, String prompt);

    Map<String, Object> callModel(Long userId, Long modelId, String prompt, String systemPrompt);
}
