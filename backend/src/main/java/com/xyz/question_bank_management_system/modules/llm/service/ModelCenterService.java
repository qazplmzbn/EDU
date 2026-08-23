package com.xyz.question_bank_management_system.modules.llm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.defaultUserId;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.iso;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.lower;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.normalizeAuthorization;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.number;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.numberDouble;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.numberInt;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.text;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.textOrDefault;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.textOrNull;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.timestamp;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.timestampOrNow;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.trimTrailingSlash;
import static com.xyz.question_bank_management_system.modules.llm.service.ModelCenterSupport.truthy;

@Service
@RequiredArgsConstructor
public class ModelCenterService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public List<Map<String, Object>> listModels(Long userId, String keyword, String category, String modelType, String status, Boolean enabled) {
        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM models
            WHERE (owner_user_id = 0 OR owner_user_id = ?)
            """);
        List<Object> args = new ArrayList<>();
        args.add(defaultUserId(userId));

        if (StringUtils.hasText(keyword)) {
            sql.append("""
                 AND (
                   LOWER(model_name) LIKE ? OR LOWER(model_code) LIKE ? OR LOWER(COALESCE(description, '')) LIKE ?
                 )
                """);
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(category)) {
            sql.append(" AND model_category = ? ");
            args.add(category.trim());
        }
        if (StringUtils.hasText(modelType)) {
            sql.append(" AND model_type = ? ");
            args.add(modelType.trim());
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND status = ? ");
            args.add(status.trim());
        }
        if (enabled != null) {
            sql.append(" AND is_available = ? ");
            args.add(Boolean.TRUE.equals(enabled) ? 1 : 0);
        }
        sql.append(" ORDER BY is_default DESC, is_builtin DESC, updated_at DESC, id DESC ");

        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(row -> toModelResponse(row, userId))
                .toList();
    }

    public Map<String, Object> getModel(Long userId, Long modelId) {
        return toModelResponse(requireVisibleModel(userId, modelId), userId);
    }

    public Map<String, Object> createModel(Long userId, boolean admin, Map<String, Object> payload) {
        long ownerUserId = admin && truthy(payload.get("createAsSystem")) ? 0L : defaultUserId(userId);
        boolean builtin = ownerUserId == 0L && truthy(payload.get("isBuiltin"));
        Map<String, Object> normalized = normalizeModelPayload(payload, ownerUserId, builtin, null);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO models (
                  model_name, model_code, model_category, model_type, provider, api_key, api_base_url,
                  config_template, ollama_model_name, ollama_base_url, model_path, lora_path, detection_type,
                  model_file_path, embedding_file_path, supported_datasets, description, version, is_available,
                  is_default, is_builtin, performance_metrics, status, error_message, last_used_at, usage_count,
                  avg_processing_time_ms, created_at, updated_at, temperature, max_tokens, top_p, timeout,
                  owner_user_id, source_scope
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            bindModelStatement(ps, normalized);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "Failed to create model");
        }
        return getModel(userId, key.longValue());
    }

    public Map<String, Object> updateModel(Long userId, boolean admin, Long modelId, Map<String, Object> payload) {
        Map<String, Object> existing = admin ? requireVisibleModel(0L, modelId) : requireOwnedEditableModel(userId, modelId);
        boolean builtin = truthy(existing.get("is_builtin"));
        Map<String, Object> normalized = normalizeModelPayload(payload, number(existing.get("owner_user_id")), builtin, existing);

        jdbcTemplate.update("""
            UPDATE models
            SET model_name = ?, model_code = ?, model_category = ?, model_type = ?, provider = ?, api_key = ?,
                api_base_url = ?, config_template = ?, ollama_model_name = ?, ollama_base_url = ?, model_path = ?,
                lora_path = ?, detection_type = ?, model_file_path = ?, embedding_file_path = ?, supported_datasets = CAST(? AS JSON),
                description = ?, version = ?, is_available = ?, is_default = ?, performance_metrics = CAST(? AS JSON),
                status = ?, temperature = ?, max_tokens = ?, top_p = ?, timeout = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
                normalized.get("model_name"),
                normalized.get("model_code"),
                normalized.get("model_category"),
                normalized.get("model_type"),
                normalized.get("provider"),
                normalized.get("api_key"),
                normalized.get("api_base_url"),
                normalized.get("config_template"),
                normalized.get("ollama_model_name"),
                normalized.get("ollama_base_url"),
                normalized.get("model_path"),
                normalized.get("lora_path"),
                normalized.get("detection_type"),
                normalized.get("model_file_path"),
                normalized.get("embedding_file_path"),
                normalized.get("supported_datasets"),
                normalized.get("description"),
                normalized.get("version"),
                normalized.get("is_available"),
                normalized.get("is_default"),
                normalized.get("performance_metrics"),
                normalized.get("status"),
                normalized.get("temperature"),
                normalized.get("max_tokens"),
                normalized.get("top_p"),
                normalized.get("timeout"),
                modelId
        );
        if (truthy(normalized.get("is_default"))) {
            resetDefaultModel(number(existing.get("owner_user_id")), modelId);
        }
        return getModel(userId, modelId);
    }

    public void deleteModel(Long userId, boolean admin, Long modelId) {
        Map<String, Object> row = admin ? requireVisibleModel(0L, modelId) : requireOwnedEditableModel(userId, modelId);
        if (truthy(row.get("is_builtin"))) {
            throw BizException.of(ErrorCode.FORBIDDEN, "Builtin models cannot be deleted");
        }
        jdbcTemplate.update("DELETE FROM models WHERE id = ?", modelId);
    }

    public Map<String, Object> updateModelApiKey(Long userId, boolean admin, Long modelId, String apiKey) {
        Map<String, Object> row = admin ? requireVisibleModel(0L, modelId) : requireOwnedEditableModel(userId, modelId);
        if (!"api".equals(String.valueOf(row.get("model_type")))) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "Only API models support apiKey");
        }
        String normalizedKey = StringUtils.hasText(apiKey) ? apiKey.trim() : null;
        jdbcTemplate.update("""
            UPDATE models
            SET api_key = ?, status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, normalizedKey, normalizedKey == null ? "inactive" : "active", modelId);
        return getModel(userId, modelId);
    }

    public List<Map<String, Object>> listTemplates(Long userId, String keyword, String taskType, Long modelId) {
        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM prompt_templates
            WHERE (owner_user_id = 0 OR owner_user_id = ?)
            """);
        List<Object> args = new ArrayList<>();
        args.add(defaultUserId(userId));
        if (StringUtils.hasText(keyword)) {
            sql.append("""
                 AND (
                   LOWER(name) LIKE ? OR LOWER(task_type) LIKE ? OR LOWER(COALESCE(description, '')) LIKE ? OR LOWER(prompt_content) LIKE ?
                 )
                """);
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(taskType)) {
            sql.append(" AND task_type = ? ");
            args.add(taskType.trim());
        }
        if (modelId != null) {
            sql.append(" AND model_id = ? ");
            args.add(modelId);
        }
        sql.append(" ORDER BY updated_at DESC, id DESC ");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    public Map<String, Object> getTemplate(Long userId, Long templateId) {
        return toTemplateResponse(requireVisibleTemplate(userId, templateId));
    }

    public Map<String, Object> createTemplate(Long userId, boolean admin, Map<String, Object> payload) {
        long ownerUserId = admin && truthy(payload.get("createAsSystem")) ? 0L : defaultUserId(userId);
        Map<String, Object> normalized = normalizeTemplatePayload(payload, ownerUserId, null);
        ensureTemplateUnique(normalized, null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO prompt_templates (
                  name, task_type, description, prompt_content, variables, model_id, is_active, usage_count,
                  created_at, updated_at, owner_user_id, source_scope
                ) VALUES (?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            bindTemplateStatement(ps, normalized);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "Failed to create template");
        }
        return getTemplate(userId, key.longValue());
    }

    public Map<String, Object> updateTemplate(Long userId, boolean admin, Long templateId, Map<String, Object> payload) {
        Map<String, Object> existing = admin ? requireVisibleTemplate(0L, templateId) : requireOwnedTemplate(userId, templateId);
        Map<String, Object> normalized = normalizeTemplatePayload(payload, number(existing.get("owner_user_id")), existing);
        ensureTemplateUnique(normalized, templateId);
        jdbcTemplate.update("""
            UPDATE prompt_templates
            SET name = ?, task_type = ?, description = ?, prompt_content = ?, variables = CAST(? AS JSON),
                model_id = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
                normalized.get("name"),
                normalized.get("task_type"),
                normalized.get("description"),
                normalized.get("prompt_content"),
                normalized.get("variables"),
                normalized.get("model_id"),
                normalized.get("is_active"),
                templateId
        );
        return getTemplate(userId, templateId);
    }

    public void deleteTemplate(Long userId, boolean admin, Long templateId) {
        if (admin) {
            requireVisibleTemplate(0L, templateId);
        } else {
            requireOwnedTemplate(userId, templateId);
        }
        jdbcTemplate.update("DELETE FROM prompt_templates WHERE id = ?", templateId);
    }

    public List<Map<String, Object>> compareModels(Long userId, String modelIds) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.hasText(modelIds)) {
            for (String part : modelIds.split(",")) {
                if (StringUtils.hasText(part)) {
                    ids.add(Long.parseLong(part.trim()));
                }
            }
        } else {
            ids = listModels(userId, null, "detection", null, null, null).stream()
                    .map(item -> number(item.get("id")))
                    .toList();
        }
        List<Map<String, Object>> models = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> row = requireVisibleModel(userId, id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", row.get("id"));
            response.put("name", row.get("model_name"));
            response.put("metrics", parseJsonMap(row.get("performance_metrics")));
            models.add(response);
        }
        return models;
    }

    public Map<String, Object> inferenceHistory(Long userId, Long modelId, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (modelId != null) {
            requireVisibleModel(userId, modelId);
        }
        return Map.of("history", rows, "total", rows.size(), "limit", limit);
    }

    public Map<String, Object> getOllamaStatus(String baseUrl) {
        String resolved = trimTrailingSlash(StringUtils.hasText(baseUrl) ? baseUrl : "http://localhost:11434");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolved + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                List<Map<String, Object>> models = new ArrayList<>();
                JsonNode array = root.path("models");
                if (array.isArray()) {
                    for (JsonNode node : array) {
                        models.add(Map.of(
                                "name", node.path("name").asText(""),
                                "size", node.path("size").asLong(0L)
                        ));
                    }
                }
                return Map.of(
                        "success", true,
                        "available", true,
                        "base_url", resolved,
                        "models", models
                );
            }
        } catch (Exception ex) {
            return Map.of(
                    "success", false,
                    "available", false,
                    "base_url", resolved,
                    "error", ex.getMessage(),
                    "models", List.of()
            );
        }
        return Map.of("success", false, "available", false, "base_url", resolved, "models", List.of());
    }

    public List<Map<String, Object>> getConfiguredOllamaModels(Long userId) {
        return listModels(userId, null, "local_llm", "ollama", null, null);
    }

    public List<Map<String, Object>> getConfiguredApiModels(Long userId) {
        return listModels(userId, null, "api", "api", null, null);
    }

    public List<Map<String, Object>> listProviders() {
        return List.of(
                provider("openai", "OpenAI"),
                provider("deepseek", "DeepSeek"),
                provider("dashscope", "DashScope"),
                provider("moonshot", "Moonshot"),
                provider("zhipu", "Zhipu"),
                provider("google", "Google Gemini"),
                provider("hunyuan", "Tencent Hunyuan"),
                provider("ollama", "Ollama")
        );
    }

    public Map<String, Object> testModel(Long userId, Long modelId, String prompt) {
        Map<String, Object> model = requireVisibleModel(userId, modelId);
        return callModelInternal(model, prompt, "You are a test assistant.", false);
    }

    public Map<String, Object> callModel(Long userId, Long modelId, String prompt, String systemPrompt) {
        Map<String, Object> model = requireVisibleModel(userId, modelId);
        return callModelInternal(model, prompt, StringUtils.hasText(systemPrompt) ? systemPrompt : llmProperties.getSystemPrompt(), true);
    }

    private Map<String, Object> callModelInternal(Map<String, Object> model, String prompt, String systemPrompt, boolean updateUsage) {
        String modelType = text(model.get("model_type"));
        if (!StringUtils.hasText(prompt)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "prompt is required");
        }
        long start = System.currentTimeMillis();
        Map<String, Object> result;
        if ("api".equals(modelType)) {
            result = callApiModel(model, prompt, systemPrompt);
        } else if ("ollama".equals(modelType)) {
            result = callOllamaModel(model, prompt, systemPrompt);
        } else {
            result = Map.of(
                    "success", false,
                    "data", Map.of(
                            "model_id", model.get("id"),
                            "model_name", model.get("model_name"),
                            "model_type", modelType,
                            "response", "",
                            "error", "Model type does not support online inference"
                    )
            );
        }
        if (updateUsage && truthy(result.get("success"))) {
            updateModelUsage(number(model.get("id")), (int) (System.currentTimeMillis() - start));
        }
        return result;
    }

    private Map<String, Object> callApiModel(Map<String, Object> model, String prompt, String systemPrompt) {
        try {
            String baseUrl = trimTrailingSlash(text(model.get("api_base_url")));
            String apiKey = textOrNull(model.get("api_key"));
            if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(text(model.get("model_code")))) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "API model is not fully configured");
            }
            if (!StringUtils.hasText(apiKey)) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "API key is missing");
            }
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("model", text(model.get("model_code")));
            requestPayload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)
            ));
            requestPayload.put("temperature", numberDouble(model.get("temperature"), 0.7));
            requestPayload.put("max_tokens", numberInt(model.get("max_tokens"), 2048));
            if (model.get("top_p") != null) {
                requestPayload.put("top_p", numberDouble(model.get("top_p"), 0.9));
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", normalizeAuthorization(apiKey))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestPayload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String content = extractContent(response.body());
                return Map.of(
                        "success", true,
                        "data", Map.of(
                                "model_id", model.get("id"),
                                "model_name", model.get("model_name"),
                                "model_type", model.get("model_type"),
                                "provider", text(model.get("provider")),
                                "response", content == null ? response.body() : content,
                                "raw", response.body(),
                                "error", ""
                        )
                );
            }
            return Map.of(
                    "success", false,
                    "data", Map.of(
                            "model_id", model.get("id"),
                            "model_name", model.get("model_name"),
                            "model_type", model.get("model_type"),
                            "provider", text(model.get("provider")),
                            "response", "",
                            "raw", response.body(),
                            "error", "HTTP " + response.statusCode()
                    )
            );
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            return Map.of(
                    "success", false,
                    "data", Map.of(
                            "model_id", model.get("id"),
                            "model_name", model.get("model_name"),
                            "model_type", model.get("model_type"),
                            "provider", text(model.get("provider")),
                            "response", "",
                            "error", ex.getMessage()
                    )
            );
        }
    }

    private Map<String, Object> callOllamaModel(Map<String, Object> model, String prompt, String systemPrompt) {
        try {
            String baseUrl = trimTrailingSlash(textOrDefault(model.get("ollama_base_url"), "http://localhost:11434"));
            String ollamaModelName = textOrDefault(model.get("ollama_model_name"), text(model.get("model_code")));
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("model", ollamaModelName);
            requestPayload.put("stream", false);
            requestPayload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestPayload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("message").path("content").asText("");
                return Map.of(
                        "success", true,
                        "data", Map.of(
                                "model_id", model.get("id"),
                                "model_name", model.get("model_name"),
                                "model_type", model.get("model_type"),
                                "response", content,
                                "raw", response.body(),
                                "error", ""
                        )
                );
            }
            return Map.of(
                    "success", false,
                    "data", Map.of(
                            "model_id", model.get("id"),
                            "model_name", model.get("model_name"),
                            "model_type", model.get("model_type"),
                            "response", "",
                            "raw", response.body(),
                            "error", "HTTP " + response.statusCode()
                    )
            );
        } catch (Exception ex) {
            return Map.of(
                    "success", false,
                    "data", Map.of(
                            "model_id", model.get("id"),
                            "model_name", model.get("model_name"),
                            "model_type", model.get("model_type"),
                            "response", "",
                            "error", ex.getMessage()
                    )
            );
        }
    }

    private void updateModelUsage(Long modelId, int latencyMs) {
        Map<String, Object> existing = jdbcTemplate.queryForMap(
                "SELECT usage_count, avg_processing_time_ms FROM models WHERE id = ?",
                modelId
        );
        int usageCount = numberInt(existing.get("usage_count"), 0);
        int newCount = usageCount + 1;
        Integer currentAvg = existing.get("avg_processing_time_ms") == null ? null : numberInt(existing.get("avg_processing_time_ms"), latencyMs);
        int newAvg = currentAvg == null ? latencyMs : (int) Math.round(((double) currentAvg * usageCount + latencyMs) / newCount);
        jdbcTemplate.update("""
            UPDATE models
            SET usage_count = ?, avg_processing_time_ms = ?, last_used_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, newCount, newAvg, modelId);
    }

    private void bindModelStatement(PreparedStatement ps, Map<String, Object> normalized) throws java.sql.SQLException {
        ps.setString(1, text(normalized.get("model_name")));
        ps.setString(2, text(normalized.get("model_code")));
        ps.setString(3, text(normalized.get("model_category")));
        ps.setString(4, text(normalized.get("model_type")));
        ps.setString(5, textOrNull(normalized.get("provider")));
        ps.setString(6, textOrNull(normalized.get("api_key")));
        ps.setString(7, textOrNull(normalized.get("api_base_url")));
        ps.setString(8, textOrNull(normalized.get("config_template")));
        ps.setString(9, textOrNull(normalized.get("ollama_model_name")));
        ps.setString(10, textOrDefault(normalized.get("ollama_base_url"), "http://localhost:11434"));
        ps.setString(11, textOrNull(normalized.get("model_path")));
        ps.setString(12, textOrNull(normalized.get("lora_path")));
        ps.setString(13, textOrNull(normalized.get("detection_type")));
        ps.setString(14, textOrNull(normalized.get("model_file_path")));
        ps.setString(15, textOrNull(normalized.get("embedding_file_path")));
        ps.setString(16, text(normalized.get("supported_datasets")));
        ps.setString(17, textOrNull(normalized.get("description")));
        ps.setString(18, textOrNull(normalized.get("version")));
        ps.setInt(19, truthy(normalized.get("is_available")) ? 1 : 0);
        ps.setInt(20, truthy(normalized.get("is_default")) ? 1 : 0);
        ps.setInt(21, truthy(normalized.get("is_builtin")) ? 1 : 0);
        ps.setString(22, text(normalized.get("performance_metrics")));
        ps.setString(23, textOrDefault(normalized.get("status"), "active"));
        ps.setString(24, textOrNull(normalized.get("error_message")));
        ps.setTimestamp(25, timestamp(normalized.get("last_used_at")));
        ps.setInt(26, numberInt(normalized.get("usage_count"), 0));
        if (normalized.get("avg_processing_time_ms") == null) {
            ps.setObject(27, null);
        } else {
            ps.setInt(27, numberInt(normalized.get("avg_processing_time_ms"), 0));
        }
        ps.setTimestamp(28, timestampOrNow(normalized.get("created_at")));
        ps.setTimestamp(29, timestampOrNow(normalized.get("updated_at")));
        ps.setDouble(30, numberDouble(normalized.get("temperature"), 0.7));
        ps.setInt(31, numberInt(normalized.get("max_tokens"), 2048));
        ps.setDouble(32, numberDouble(normalized.get("top_p"), 0.9));
        ps.setInt(33, numberInt(normalized.get("timeout"), 120));
        ps.setLong(34, number(normalized.get("owner_user_id")));
        ps.setString(35, textOrDefault(normalized.get("source_scope"), "personal"));
    }

    private void bindTemplateStatement(PreparedStatement ps, Map<String, Object> normalized) throws java.sql.SQLException {
        ps.setString(1, text(normalized.get("name")));
        ps.setString(2, text(normalized.get("task_type")));
        ps.setString(3, textOrNull(normalized.get("description")));
        ps.setString(4, text(normalized.get("prompt_content")));
        ps.setString(5, text(normalized.get("variables")));
        if (normalized.get("model_id") == null) {
            ps.setObject(6, null);
        } else {
            ps.setLong(6, number(normalized.get("model_id")));
        }
        ps.setInt(7, truthy(normalized.get("is_active")) ? 1 : 0);
        ps.setInt(8, numberInt(normalized.get("usage_count"), 0));
        ps.setTimestamp(9, timestampOrNow(normalized.get("created_at")));
        ps.setTimestamp(10, timestampOrNow(normalized.get("updated_at")));
        ps.setLong(11, number(normalized.get("owner_user_id")));
        ps.setString(12, textOrDefault(normalized.get("source_scope"), "personal"));
    }

    private Map<String, Object> normalizeModelPayload(Map<String, Object> payload, Long ownerUserId, boolean builtin, Map<String, Object> existing) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        String category = lower(textOrDefault(value(payload, existing, "modelCategory", "model_category"), "api"));
        String modelType = lower(textOrDefault(value(payload, existing, "modelType", "model_type"), "api"));
        String modelName = text(value(payload, existing, "modelName", "model_name"));
        String requestedCode = textOrDefault(value(payload, existing, "modelCode", "model_code"), "");
        String modelCode = ensureUniqueModelCode(requestedCode, modelName, ownerUserId, existing == null ? null : number(existing.get("id")));
        boolean available = truthy(value(payload, existing, "isAvailable", "is_available")) || "active".equalsIgnoreCase(textOrDefault(payload.get("status"), textOrDefault(existing == null ? null : existing.get("status"), "active")));
        String status = textOrDefault(payload.get("status"), available ? "active" : "inactive");

        normalized.put("model_name", modelName);
        normalized.put("model_code", modelCode);
        normalized.put("model_category", category);
        normalized.put("model_type", modelType);
        normalized.put("provider", textOrDefault(value(payload, existing, "provider"), inferProvider(category, modelType, payload)));
        normalized.put("api_key", textOrNull(payload.get("apiKey")));
        normalized.put("api_base_url", trimTrailingSlash(textOrNull(value(payload, existing, "apiBaseUrl", "api_base_url"))));
        normalized.put("config_template", textOrNull(value(payload, existing, "configTemplate", "config_template")));
        normalized.put("ollama_model_name", textOrDefault(value(payload, existing, "ollamaModelName", "ollama_model_name"), "local_llm".equals(category) ? textOrNull(payload.get("modelCode")) : null));
        normalized.put("ollama_base_url", trimTrailingSlash(textOrDefault(value(payload, existing, "ollamaBaseUrl", "ollama_base_url"), "http://localhost:11434")));
        normalized.put("model_path", textOrNull(value(payload, existing, "modelPath", "model_path")));
        normalized.put("lora_path", textOrNull(value(payload, existing, "loraPath", "lora_path")));
        normalized.put("detection_type", textOrNull(value(payload, existing, "detectionType", "detection_type")));
        normalized.put("model_file_path", textOrNull(value(payload, existing, "modelFilePath", "model_file_path")));
        normalized.put("embedding_file_path", textOrNull(value(payload, existing, "embeddingFilePath", "embedding_file_path")));
        normalized.put("supported_datasets", toJsonArray(value(payload, existing, "supportedDatasets", "supported_datasets")));
        normalized.put("description", textOrNull(value(payload, existing, "description")));
        normalized.put("version", textOrDefault(value(payload, existing, "version"), builtin ? "builtin" : "v1.0"));
        normalized.put("is_available", available);
        normalized.put("is_default", truthy(value(payload, existing, "isDefault", "is_default")));
        normalized.put("is_builtin", builtin);
        normalized.put("performance_metrics", toJsonObject(value(payload, existing, "performanceMetrics", "performance_metrics")));
        normalized.put("status", status);
        normalized.put("error_message", textOrNull(value(payload, existing, "errorMessage", "error_message")));
        normalized.put("last_used_at", value(payload, existing, "lastUsedAt", "last_used_at"));
        normalized.put("usage_count", numberInt(value(payload, existing, "usageCount", "usage_count"), 0));
        normalized.put("avg_processing_time_ms", value(payload, existing, "avgProcessingTimeMs", "avg_processing_time_ms"));
        normalized.put("created_at", existing == null ? LocalDateTime.now() : existing.get("created_at"));
        normalized.put("updated_at", LocalDateTime.now());
        normalized.put("temperature", numberDouble(value(payload, existing, "temperature"), 0.7));
        normalized.put("max_tokens", numberInt(value(payload, existing, "maxTokens", "max_tokens"), 2048));
        normalized.put("top_p", numberDouble(value(payload, existing, "topP", "top_p"), 0.9));
        normalized.put("timeout", numberInt(value(payload, existing, "timeout"), 120));
        normalized.put("owner_user_id", ownerUserId);
        normalized.put("source_scope", ownerUserId == 0 ? "system" : "personal");
        return normalized;
    }

    private Map<String, Object> normalizeTemplatePayload(Map<String, Object> payload, Long ownerUserId, Map<String, Object> existing) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("name", text(value(payload, existing, "name", "templateName")));
        normalized.put("task_type", text(value(payload, existing, "taskType", "task_type")));
        normalized.put("description", textOrNull(value(payload, existing, "description")));
        normalized.put("prompt_content", text(value(payload, existing, "promptContent", "prompt_content", "promptText")));
        normalized.put("variables", toJsonArray(value(payload, existing, "variables")));
        Object modelId = value(payload, existing, "modelId", "model_id");
        normalized.put("model_id", modelId == null ? null : number(modelId));
        normalized.put("is_active", truthy(value(payload, existing, "isActive", "is_active")) || existing == null);
        normalized.put("usage_count", numberInt(value(payload, existing, "usageCount", "usage_count"), 0));
        normalized.put("created_at", existing == null ? LocalDateTime.now() : existing.get("created_at"));
        normalized.put("updated_at", LocalDateTime.now());
        normalized.put("owner_user_id", ownerUserId);
        normalized.put("source_scope", ownerUserId == 0 ? "system" : "personal");
        return normalized;
    }

    private void ensureTemplateUnique(Map<String, Object> normalized, Long currentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id
            FROM prompt_templates
            WHERE name = ? AND task_type = ? AND owner_user_id = ?
            """,
                normalized.get("name"),
                normalized.get("task_type"),
                normalized.get("owner_user_id")
        );
        if (!rows.isEmpty()) {
            Long existingId = number(rows.get(0).get("id"));
            if (!Objects.equals(existingId, currentId)) {
                throw BizException.of(ErrorCode.CONFLICT, "Template name already exists for this task type");
            }
        }
    }

    private void resetDefaultModel(Long ownerUserId, Long keepModelId) {
        jdbcTemplate.update("UPDATE models SET is_default = 0 WHERE owner_user_id = ? AND id <> ?", ownerUserId, keepModelId);
        jdbcTemplate.update("UPDATE models SET is_default = 1, is_available = 1, status = 'active' WHERE id = ?", keepModelId);
    }

    private Map<String, Object> requireVisibleModel(Long userId, Long modelId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT *
            FROM models
            WHERE id = ? AND (owner_user_id = 0 OR owner_user_id = ?)
            """, modelId, defaultUserId(userId));
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Model not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> requireOwnedEditableModel(Long userId, Long modelId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT *
            FROM models
            WHERE id = ? AND owner_user_id = ?
            """, modelId, defaultUserId(userId));
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Model not found");
        }
        Map<String, Object> row = rows.get(0);
        if (truthy(row.get("is_builtin"))) {
            throw BizException.of(ErrorCode.FORBIDDEN, "Builtin models are read-only");
        }
        return row;
    }

    private Map<String, Object> requireVisibleTemplate(Long userId, Long templateId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT *
            FROM prompt_templates
            WHERE id = ? AND (owner_user_id = 0 OR owner_user_id = ?)
            """, templateId, defaultUserId(userId));
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Template not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> requireOwnedTemplate(Long userId, Long templateId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT *
            FROM prompt_templates
            WHERE id = ? AND owner_user_id = ?
            """, templateId, defaultUserId(userId));
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Template not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> toModelResponse(Map<String, Object> row, Long currentUserId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", number(row.get("id")));
        result.put("modelName", text(row.get("model_name")));
        result.put("modelCode", text(row.get("model_code")));
        result.put("modelCategory", text(row.get("model_category")));
        result.put("modelType", text(row.get("model_type")));
        result.put("provider", textOrNull(row.get("provider")));
        result.put("apiBaseUrl", textOrNull(row.get("api_base_url")));
        result.put("configTemplate", textOrNull(row.get("config_template")));
        result.put("ollamaModelName", textOrNull(row.get("ollama_model_name")));
        result.put("ollamaBaseUrl", textOrNull(row.get("ollama_base_url")));
        result.put("modelPath", textOrNull(row.get("model_path")));
        result.put("loraPath", textOrNull(row.get("lora_path")));
        result.put("detectionType", textOrNull(row.get("detection_type")));
        result.put("modelFilePath", textOrNull(row.get("model_file_path")));
        result.put("embeddingFilePath", textOrNull(row.get("embedding_file_path")));
        result.put("supportedDatasets", parseJsonList(row.get("supported_datasets")));
        result.put("description", textOrNull(row.get("description")));
        result.put("version", textOrNull(row.get("version")));
        result.put("isAvailable", truthy(row.get("is_available")));
        result.put("isDefault", truthy(row.get("is_default")));
        result.put("isBuiltin", truthy(row.get("is_builtin")));
        result.put("performanceMetrics", parseJsonMap(row.get("performance_metrics")));
        result.put("status", textOrDefault(row.get("status"), truthy(row.get("is_available")) ? "active" : "inactive"));
        result.put("errorMessage", textOrNull(row.get("error_message")));
        result.put("lastUsedAt", iso(row.get("last_used_at")));
        result.put("usageCount", numberInt(row.get("usage_count"), 0));
        result.put("avgProcessingTimeMs", row.get("avg_processing_time_ms"));
        result.put("createdAt", iso(row.get("created_at")));
        result.put("updatedAt", iso(row.get("updated_at")));
        result.put("temperature", numberDouble(row.get("temperature"), 0.7));
        result.put("maxTokens", numberInt(row.get("max_tokens"), 2048));
        result.put("topP", numberDouble(row.get("top_p"), 0.9));
        result.put("timeout", numberInt(row.get("timeout"), 120));
        result.put("hasApiKey", "api".equals(text(row.get("model_type"))) ? StringUtils.hasText(textOrNull(row.get("api_key"))) : null);
        result.put("editable", number(row.get("owner_user_id")) == defaultUserId(currentUserId) && !truthy(row.get("is_builtin")));
        result.put("source", number(row.get("owner_user_id")) == 0L ? "system" : "personal");
        result.put("apiKeyMask", StringUtils.hasText(textOrNull(row.get("api_key"))) ? "configured" : "");
        result.put("ownerUserId", number(row.get("owner_user_id")));
        return result;
    }

    private Map<String, Object> toTemplateResponse(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", number(row.get("id")));
        result.put("name", text(row.get("name")));
        result.put("taskType", text(row.get("task_type")));
        result.put("description", textOrNull(row.get("description")));
        result.put("promptContent", text(row.get("prompt_content")));
        result.put("variables", parseJsonValue(row.get("variables")));
        result.put("modelId", row.get("model_id") == null ? null : number(row.get("model_id")));
        result.put("isActive", truthy(row.get("is_active")));
        result.put("usageCount", numberInt(row.get("usage_count"), 0));
        result.put("createdAt", iso(row.get("created_at")));
        result.put("updatedAt", iso(row.get("updated_at")));
        result.put("ownerUserId", number(row.get("owner_user_id")));
        result.put("source", number(row.get("owner_user_id")) == 0L ? "system" : "personal");
        return result;
    }

    private Object value(Map<String, Object> payload, Map<String, Object> existing, String... keys) {
        for (String key : keys) {
            if (payload != null && payload.containsKey(key) && payload.get(key) != null) {
                return payload.get(key);
            }
            if (existing != null && existing.containsKey(key) && existing.get(key) != null) {
                return existing.get(key);
            }
        }
        return null;
    }

    private String ensureUniqueModelCode(String requestedCode, String modelName, Long ownerUserId, Long currentId) {
        String base = StringUtils.hasText(requestedCode) ? requestedCode : modelName;
        String normalized = lower(base).replaceAll("[^a-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "model";
        }
        String candidate = normalized;
        int suffix = 2;
        while (true) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id FROM models WHERE model_code = ?",
                    candidate
            );
            if (rows.isEmpty()) {
                return candidate;
            }
            Long existingId = number(rows.get(0).get("id"));
            if (Objects.equals(existingId, currentId)) {
                return candidate;
            }
            candidate = normalized + "-" + suffix++;
        }
    }

    private String inferProvider(String category, String modelType, Map<String, Object> payload) {
        if ("local_llm".equals(category) && "ollama".equals(modelType)) {
            return "ollama";
        }
        String haystack = String.join(" ",
                textOrDefault(payload.get("configTemplate"), ""),
                textOrDefault(payload.get("apiBaseUrl"), ""),
                textOrDefault(payload.get("modelCode"), ""),
                textOrDefault(payload.get("modelName"), "")
        ).toLowerCase(Locale.ROOT);
        if (haystack.contains("deepseek")) return "deepseek";
        if (haystack.contains("dashscope") || haystack.contains("qwen")) return "dashscope";
        if (haystack.contains("moonshot") || haystack.contains("kimi")) return "moonshot";
        if (haystack.contains("glm") || haystack.contains("zhipu")) return "zhipu";
        if (haystack.contains("google") || haystack.contains("gemini")) return "google";
        if (haystack.contains("hunyuan")) return "hunyuan";
        if (haystack.contains("openai")) return "openai";
        return "custom";
    }

    private String toJsonArray(Object value) {
        try {
            if (value == null) return "[]";
            if (value instanceof String stringValue) {
                String trimmed = stringValue.trim();
                if (!StringUtils.hasText(trimmed)) return "[]";
                if (trimmed.startsWith("[")) {
                    return trimmed;
                }
                List<String> parts = List.of(trimmed.split("[,\\s]+")).stream()
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
                return objectMapper.writeValueAsString(parts);
            }
            if (value instanceof List<?> list) {
                return objectMapper.writeValueAsString(list);
            }
            return objectMapper.writeValueAsString(List.of(value));
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String toJsonObject(Object value) {
        try {
            if (value == null) return "{}";
            if (value instanceof String stringValue) {
                String trimmed = stringValue.trim();
                return StringUtils.hasText(trimmed) ? trimmed : "{}";
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private List<String> parseJsonList(Object value) {
        try {
            if (value == null) return List.of();
            if (value instanceof String stringValue) {
                if (!StringUtils.hasText(stringValue)) return List.of();
                return objectMapper.readValue(stringValue, new TypeReference<List<String>>() {});
            }
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignore) {
        }
        return List.of();
    }

    private Map<String, Object> parseJsonMap(Object value) {
        try {
            if (value == null) return Map.of();
            if (value instanceof String stringValue) {
                if (!StringUtils.hasText(stringValue)) return Map.of();
                return objectMapper.readValue(stringValue, new TypeReference<Map<String, Object>>() {});
            }
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((k, v) -> result.put(String.valueOf(k), v));
                return result;
            }
        } catch (Exception ignore) {
        }
        return Map.of();
    }

    private Object parseJsonValue(Object value) {
        try {
            if (value == null) return List.of();
            if (value instanceof String stringValue) {
                if (!StringUtils.hasText(stringValue)) return List.of();
                return objectMapper.readValue(stringValue, Object.class);
            }
        } catch (Exception ignore) {
        }
        return value;
    }

    private String extractContent(String responseText) {
        try {
            JsonNode root = objectMapper.readTree(responseText);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode contentNode = choices.get(0).path("message").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
            if (contentNode.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : contentNode) {
                    String text = item.path("text").asText("");
                    if (!text.isBlank()) {
                        if (builder.length() > 0) builder.append('\n');
                        builder.append(text);
                    }
                }
                return builder.toString();
            }
            return contentNode.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> provider(String key, String label) {
        return Map.of("key", key, "label", label);
    }

}
