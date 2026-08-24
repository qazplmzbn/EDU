package com.xyz.question_bank_management_system.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.util.LlmSecretCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.migration.model-center", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ModelCenterMigrationRunner implements CommandLineRunner {

    private static final String CREATE_MODELS_SQL = """
        CREATE TABLE IF NOT EXISTS models (
          id BIGINT NOT NULL AUTO_INCREMENT,
          model_name VARCHAR(255) NOT NULL,
          model_code VARCHAR(100) NOT NULL,
          model_category ENUM('api','local_llm','detection') NOT NULL,
          model_type ENUM('api','ollama','transformers','fealearner','emoji') NOT NULL,
          provider VARCHAR(100) DEFAULT NULL,
          api_key VARCHAR(500) DEFAULT NULL,
          api_base_url VARCHAR(500) DEFAULT NULL,
          config_template VARCHAR(50) DEFAULT NULL,
          ollama_model_name VARCHAR(255) DEFAULT NULL,
          ollama_base_url VARCHAR(500) DEFAULT 'http://localhost:11434',
          model_path VARCHAR(500) DEFAULT NULL,
          lora_path VARCHAR(500) DEFAULT NULL,
          detection_type ENUM('fealearner','emoji') DEFAULT NULL,
          model_file_path VARCHAR(500) DEFAULT NULL,
          embedding_file_path VARCHAR(500) DEFAULT NULL,
          supported_datasets JSON DEFAULT NULL,
          description TEXT DEFAULT NULL,
          version VARCHAR(50) DEFAULT NULL,
          is_available TINYINT(1) DEFAULT 1,
          is_default TINYINT(1) DEFAULT 0,
          is_builtin TINYINT(1) DEFAULT 0,
          performance_metrics JSON DEFAULT NULL,
          status ENUM('active','inactive','error') DEFAULT 'active',
          error_message TEXT DEFAULT NULL,
          last_used_at DATETIME DEFAULT NULL,
          usage_count INT DEFAULT 0,
          avg_processing_time_ms INT DEFAULT NULL,
          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          temperature FLOAT DEFAULT 0.7,
          max_tokens INT DEFAULT 2048,
          top_p FLOAT DEFAULT 0.9,
          timeout INT DEFAULT 120,
          owner_user_id BIGINT NOT NULL DEFAULT 0,
          source_scope VARCHAR(32) NOT NULL DEFAULT 'system',
          PRIMARY KEY (id),
          UNIQUE KEY uniq_model_code (model_code),
          KEY idx_model_category (model_category),
          KEY idx_model_type (model_type),
          KEY idx_provider (provider),
          KEY idx_detection_type (detection_type),
          KEY idx_is_available (is_available),
          KEY idx_is_default (is_default),
          KEY idx_status (status),
          KEY idx_owner_scope (owner_user_id, source_scope)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """;

    private static final String CREATE_PROMPT_TEMPLATES_SQL = """
        CREATE TABLE IF NOT EXISTS prompt_templates (
          id BIGINT NOT NULL AUTO_INCREMENT,
          name VARCHAR(255) NOT NULL,
          task_type VARCHAR(100) NOT NULL,
          description TEXT DEFAULT NULL,
          prompt_content TEXT NOT NULL,
          variables JSON DEFAULT NULL,
          model_id BIGINT DEFAULT NULL,
          is_active TINYINT(1) DEFAULT 1,
          usage_count INT DEFAULT 0,
          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          owner_user_id BIGINT NOT NULL DEFAULT 0,
          source_scope VARCHAR(32) NOT NULL DEFAULT 'system',
          PRIMARY KEY (id),
          UNIQUE KEY uniq_template_name_task_owner (name, task_type, owner_user_id),
          KEY idx_task_type (task_type),
          KEY idx_model_id (model_id),
          KEY idx_is_active (is_active),
          KEY idx_owner_scope (owner_user_id, source_scope),
          CONSTRAINT fk_prompt_templates_model
            FOREIGN KEY (model_id) REFERENCES models (id) ON DELETE SET NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final LlmSecretCodec secretCodec;

    @Override
    public void run(String... args) {
        ensureSchema();
        seedReferenceBuiltins();
        migrateLegacyProviders();
        migrateLegacyTemplates();
    }

    private void ensureSchema() {
        jdbcTemplate.execute(CREATE_MODELS_SQL);
        jdbcTemplate.execute(CREATE_PROMPT_TEMPLATES_SQL);
        ensureCompatibilityColumns();
        ensureCompatibilityIndexes();
    }

    private void ensureCompatibilityColumns() {
        addColumnIfMissing("models", "owner_user_id", "ALTER TABLE models ADD COLUMN owner_user_id BIGINT NOT NULL DEFAULT 0");
        addColumnIfMissing("models", "source_scope", "ALTER TABLE models ADD COLUMN source_scope VARCHAR(32) NOT NULL DEFAULT 'system'");
        addColumnIfMissing("prompt_templates", "owner_user_id", "ALTER TABLE prompt_templates ADD COLUMN owner_user_id BIGINT NOT NULL DEFAULT 0");
        addColumnIfMissing("prompt_templates", "source_scope", "ALTER TABLE prompt_templates ADD COLUMN source_scope VARCHAR(32) NOT NULL DEFAULT 'system'");
    }

    private void ensureCompatibilityIndexes() {
        addIndexIfMissing("models", "idx_owner_scope", "CREATE INDEX idx_owner_scope ON models (owner_user_id, source_scope)");
        addIndexIfMissing("prompt_templates", "idx_owner_scope", "CREATE INDEX idx_owner_scope ON prompt_templates (owner_user_id, source_scope)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND INDEX_NAME = ?
            """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void seedReferenceBuiltins() {
        upsertModel(mapOf(
                "model_code", "qwen-flash",
                "model_name", "DashScope Qwen Flash",
                "model_category", "api",
                "model_type", "api",
                "provider", "dashscope",
                "api_base_url", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "config_template", "dashscope",
                "description", "DashScope qwen-flash API model.",
                "version", "v1.0",
                "is_available", 1,
                "is_default", 0,
                "is_builtin", 1,
                "status", "inactive",
                "supported_datasets", toJson(List.of("reddit")),
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));

        upsertModel(mapOf(
                "model_code", "deepseek-v3",
                "model_name", "DeepSeek V3",
                "model_category", "api",
                "model_type", "api",
                "provider", "deepseek",
                "api_base_url", "https://api.deepseek.com/v1",
                "config_template", "deepseek",
                "description", "DeepSeek V3 API model.",
                "version", "v1.0",
                "is_available", 1,
                "is_default", 0,
                "is_builtin", 1,
                "status", "inactive",
                "supported_datasets", toJson(List.of("reddit")),
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));

        upsertModel(mapOf(
                "model_code", "qwen2-1.5b",
                "model_name", "Qwen2 1.5B Local",
                "model_category", "local_llm",
                "model_type", "ollama",
                "provider", "ollama",
                "ollama_model_name", "qwen2:1.5b",
                "ollama_base_url", "http://localhost:11434",
                "description", "Ollama local Qwen model.",
                "version", "v2.0",
                "is_available", 1,
                "is_default", 0,
                "is_builtin", 1,
                "status", "active",
                "supported_datasets", toJson(List.of("reddit")),
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));

        upsertModel(mapOf(
                "model_code", "emocc-reddit",
                "model_name", "Emocc Reddit",
                "model_category", "detection",
                "model_type", "emoji",
                "provider", "VIS4SRD",
                "detection_type", "emoji",
                "description", "Emoji-based risk detection model.",
                "version", "v1.0",
                "is_available", 1,
                "is_default", 0,
                "is_builtin", 1,
                "status", "active",
                "supported_datasets", toJson(List.of("reddit")),
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));

        upsertTemplate(mapOf(
                "name", "Risk Detection Template",
                "task_type", "risk_detection",
                "description", "Template for structured risk assessment.",
                "prompt_content", "Assess the user text carefully and return structured JSON.",
                "variables", "[]",
                "is_active", 1,
                "usage_count", 0,
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));

        upsertTemplate(mapOf(
                "name", "Mistake Explanation Template",
                "task_type", "mistake_explanation",
                "description", "Template for study question explanation.",
                "prompt_content", "Explain this question from knowledge point, solution steps, and common mistakes.",
                "variables", "[]",
                "is_active", 1,
                "usage_count", 0,
                "owner_user_id", 0L,
                "source_scope", "builtin"
        ));
    }

    private void migrateLegacyProviders() {
        List<Map<String, Object>> systemRows = jdbcTemplate.queryForList("""
            SELECT id, provider_key, label, provider_type, base_url, api_key_cipher, model,
                   temperature, supports_temperature, description, tags_json, enabled, is_default,
                   created_at, updated_at
            FROM qb_llm_provider
            WHERE is_deleted = 0
            """);
        for (Map<String, Object> row : systemRows) {
            migrateProviderRow(row, 0L, "legacy_system");
        }

        List<Map<String, Object>> personalRows = jdbcTemplate.queryForList("""
            SELECT id, user_id, provider_key, label, provider_type, base_url, api_key_cipher, model,
                   temperature, supports_temperature, description, tags_json, enabled, is_default,
                   created_at, updated_at
            FROM qb_user_llm_provider
            WHERE is_deleted = 0
            """);
        for (Map<String, Object> row : personalRows) {
            Number userId = (Number) row.get("user_id");
            migrateProviderRow(row, userId == null ? 0L : userId.longValue(), "personal");
        }
    }

    private void migrateLegacyTemplates() {
        List<Map<String, Object>> systemRows = jdbcTemplate.queryForList("""
            SELECT id, template_name, task_type, description, prompt_text, created_at, updated_at
            FROM qb_prompt_template
            WHERE is_deleted = 0
            """);
        for (Map<String, Object> row : systemRows) {
            upsertTemplate(mapOf(
                    "name", text(row.get("template_name"), "Legacy Template"),
                    "task_type", text(row.get("task_type"), "general"),
                    "description", textOrNull(row.get("description")),
                    "prompt_content", text(row.get("prompt_text"), ""),
                    "variables", "[]",
                    "is_active", 1,
                    "usage_count", 0,
                    "owner_user_id", 0L,
                    "source_scope", "legacy_system",
                    "created_at", row.get("created_at"),
                    "updated_at", row.get("updated_at")
            ));
        }

        List<Map<String, Object>> personalRows = jdbcTemplate.queryForList("""
            SELECT id, user_id, template_name, task_type, description, prompt_text, created_at, updated_at
            FROM qb_user_prompt_template
            WHERE is_deleted = 0
            """);
        for (Map<String, Object> row : personalRows) {
            Number userId = (Number) row.get("user_id");
            upsertTemplate(mapOf(
                    "name", text(row.get("template_name"), "Legacy Template"),
                    "task_type", text(row.get("task_type"), "general"),
                    "description", textOrNull(row.get("description")),
                    "prompt_content", text(row.get("prompt_text"), ""),
                    "variables", "[]",
                    "is_active", 1,
                    "usage_count", 0,
                    "owner_user_id", userId == null ? 0L : userId.longValue(),
                    "source_scope", "personal",
                    "created_at", row.get("created_at"),
                    "updated_at", row.get("updated_at")
            ));
        }
    }

    private void migrateProviderRow(Map<String, Object> row, Long ownerUserId, String sourceScope) {
        String providerKey = text(row.get("provider_key"), "legacy-model");
        String modelName = text(row.get("label"), providerKey);
        String providerType = text(row.get("provider_type"), "API").toUpperCase(Locale.ROOT);
        String description = textOrNull(row.get("description"));
        String modelValue = text(row.get("model"), providerKey);
        List<String> tags = parseTags(row.get("tags_json"));

        String modelCategory = inferModelCategory(providerType, tags, modelValue, description);
        String modelType = inferModelType(modelCategory, providerType, tags, modelValue, row.get("base_url"));
        String detectionType = "detection".equals(modelCategory)
                ? (tags.stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains("fealearner")) ? "fealearner" : "emoji")
                : null;

        Set<String> datasets = inferDatasets(tags, description, modelValue);
        String modelCodeBase = ownerUserId == 0
                ? "legacy-system-" + providerKey
                : "legacy-user-" + ownerUserId + "-" + providerKey;

        String baseUrl = textOrNull(row.get("base_url"));
        String apiBaseUrl = "api".equals(modelCategory) ? trimTrailingSlash(baseUrl) : null;
        String ollamaBaseUrl = "local_llm".equals(modelCategory) && "ollama".equals(modelType)
                ? trimTrailingSlash(baseUrl == null ? "http://localhost:11434" : baseUrl)
                : null;
        String apiKeyCipher = textOrNull(row.get("api_key_cipher"));
        String apiKey = StringUtils.hasText(apiKeyCipher) ? blankToNull(secretCodec.decode(apiKeyCipher)) : null;

        upsertModel(mapOf(
                "model_code", ensureUniqueCode(modelCodeBase, ownerUserId),
                "model_name", modelName,
                "model_category", modelCategory,
                "model_type", modelType,
                "provider", inferProvider(modelCategory, modelType, baseUrl, modelValue, providerKey),
                "api_key", apiKey == null ? "" : apiKey,
                "api_base_url", apiBaseUrl == null ? "" : apiBaseUrl,
                "config_template", inferConfigTemplate(baseUrl, providerKey),
                "ollama_model_name", "local_llm".equals(modelCategory) ? modelValue : "",
                "ollama_base_url", ollamaBaseUrl == null ? "" : ollamaBaseUrl,
                "model_path", "transformers".equals(modelType) ? modelValue : "",
                "detection_type", detectionType == null ? "" : detectionType,
                "model_file_path", "detection".equals(modelCategory) ? modelValue : "",
                "supported_datasets", toJson(new ArrayList<>(datasets)),
                "description", description == null ? "" : description,
                "version", "legacy",
                "is_available", truthy(row.get("enabled")) ? 1 : 0,
                "is_default", truthy(row.get("is_default")) ? 1 : 0,
                "is_builtin", 0,
                "status", truthy(row.get("enabled")) ? "active" : "inactive",
                "temperature", row.get("temperature") == null ? 0.2 : row.get("temperature"),
                "owner_user_id", ownerUserId,
                "source_scope", sourceScope,
                "created_at", row.get("created_at"),
                "updated_at", row.get("updated_at")
        ));
    }

    private void upsertModel(Map<String, Object> values) {
        jdbcTemplate.update("""
            INSERT INTO models (
              model_name, model_code, model_category, model_type, provider, api_key, api_base_url,
              config_template, ollama_model_name, ollama_base_url, model_path, lora_path, detection_type,
              model_file_path, embedding_file_path, supported_datasets, description, version, is_available,
              is_default, is_builtin, performance_metrics, status, error_message, last_used_at, usage_count,
              avg_processing_time_ms, created_at, updated_at, temperature, max_tokens, top_p, timeout,
              owner_user_id, source_scope
            ) VALUES (?, ?, ?, ?, ?, NULLIF(?, ''), NULLIF(?, ''), NULLIF(?, ''), NULLIF(?, ''),
                      COALESCE(NULLIF(?, ''), 'http://localhost:11434'), NULLIF(?, ''), NULL, NULLIF(?, ''),
                      NULLIF(?, ''), NULL, CAST(? AS JSON), NULLIF(?, ''), NULLIF(?, ''), ?, ?, ?, NULL, ?,
                      NULL, NULL, 0, NULL, COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, CURRENT_TIMESTAMP),
                      COALESCE(?, 0.7), 2048, 0.9, 120, ?, ?)
            ON DUPLICATE KEY UPDATE
              model_name = VALUES(model_name),
              model_category = VALUES(model_category),
              model_type = VALUES(model_type),
              provider = VALUES(provider),
              api_base_url = VALUES(api_base_url),
              config_template = VALUES(config_template),
              ollama_model_name = VALUES(ollama_model_name),
              ollama_base_url = VALUES(ollama_base_url),
              model_path = VALUES(model_path),
              detection_type = VALUES(detection_type),
              model_file_path = VALUES(model_file_path),
              supported_datasets = VALUES(supported_datasets),
              description = VALUES(description),
              version = VALUES(version),
              is_available = VALUES(is_available),
              is_default = VALUES(is_default),
              is_builtin = VALUES(is_builtin),
              status = VALUES(status),
              temperature = VALUES(temperature),
              owner_user_id = VALUES(owner_user_id),
              source_scope = VALUES(source_scope),
              updated_at = COALESCE(VALUES(updated_at), CURRENT_TIMESTAMP)
            """,
                values.get("model_name"),
                values.get("model_code"),
                values.get("model_category"),
                values.get("model_type"),
                values.get("provider"),
                values.get("api_key"),
                values.get("api_base_url"),
                values.get("config_template"),
                values.get("ollama_model_name"),
                values.get("ollama_base_url"),
                values.get("model_path"),
                values.get("detection_type"),
                values.get("model_file_path"),
                values.get("supported_datasets"),
                values.get("description"),
                values.get("version"),
                values.get("is_available"),
                values.get("is_default"),
                values.get("is_builtin"),
                values.get("status"),
                values.get("created_at"),
                values.get("updated_at"),
                values.get("temperature"),
                values.get("owner_user_id"),
                values.get("source_scope")
        );
    }

    private void upsertTemplate(Map<String, Object> values) {
        jdbcTemplate.update("""
            INSERT INTO prompt_templates (
              name, task_type, description, prompt_content, variables, model_id,
              is_active, usage_count, created_at, updated_at, owner_user_id, source_scope
            ) VALUES (?, ?, NULLIF(?, ''), ?, CAST(? AS JSON), ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP),
                      COALESCE(?, CURRENT_TIMESTAMP), ?, ?)
            ON DUPLICATE KEY UPDATE
              description = VALUES(description),
              prompt_content = VALUES(prompt_content),
              variables = VALUES(variables),
              model_id = VALUES(model_id),
              is_active = VALUES(is_active),
              updated_at = COALESCE(VALUES(updated_at), CURRENT_TIMESTAMP),
              source_scope = VALUES(source_scope)
            """,
                values.get("name"),
                values.get("task_type"),
                values.get("description"),
                values.get("prompt_content"),
                values.get("variables"),
                values.get("model_id"),
                values.get("is_active"),
                values.get("usage_count"),
                values.get("created_at"),
                values.get("updated_at"),
                values.get("owner_user_id"),
                values.get("source_scope")
        );
    }

    private String ensureUniqueCode(String baseCode, Long ownerUserId) {
        String base = normalizeCode(baseCode);
        if (base.isBlank()) {
            base = "legacy-model";
        }
        String candidate = base;
        int suffix = 2;
        while (existsModelCode(candidate, ownerUserId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean existsModelCode(String modelCode, Long ownerUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM models WHERE model_code = ? AND owner_user_id = ?",
                Integer.class,
                modelCode,
                ownerUserId
        );
        return count != null && count > 0;
    }

    private String inferModelCategory(String providerType, List<String> tags, String modelValue, String description) {
        if ("API".equalsIgnoreCase(providerType)) {
            return "api";
        }
        String haystack = (String.join(" ", tags) + " " + modelValue + " " + String.valueOf(description))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("emoji") || haystack.contains("fealearner") || haystack.contains("检测") || haystack.contains("detection")) {
            return "detection";
        }
        return "local_llm";
    }

    private String inferModelType(String modelCategory, String providerType, List<String> tags, String modelValue, Object baseUrl) {
        if ("api".equals(modelCategory) || "API".equalsIgnoreCase(providerType)) {
            return "api";
        }
        String haystack = (String.join(" ", tags) + " " + modelValue + " " + String.valueOf(baseUrl))
                .toLowerCase(Locale.ROOT);
        if ("detection".equals(modelCategory)) {
            return haystack.contains("fealearner") ? "fealearner" : "emoji";
        }
        if (haystack.contains("11434") || haystack.contains("ollama") || modelValue.contains(":")) {
            return "ollama";
        }
        return "transformers";
    }

    private String inferProvider(String modelCategory, String modelType, String baseUrl, String modelValue, String providerKey) {
        if ("local_llm".equals(modelCategory) && "ollama".equals(modelType)) {
            return "ollama";
        }
        String haystack = (String.valueOf(baseUrl) + " " + providerKey + " " + modelValue).toLowerCase(Locale.ROOT);
        if (haystack.contains("deepseek")) return "deepseek";
        if (haystack.contains("dashscope") || haystack.contains("qwen")) return "dashscope";
        if (haystack.contains("openai")) return "openai";
        if (haystack.contains("moonshot") || haystack.contains("kimi")) return "moonshot";
        if (haystack.contains("zhipu") || haystack.contains("glm")) return "zhipu";
        if (haystack.contains("google") || haystack.contains("gemini")) return "google";
        if (haystack.contains("hunyuan") || haystack.contains("tencent")) return "hunyuan";
        return "local_llm".equals(modelCategory) ? "local" : "custom";
    }

    private String inferConfigTemplate(String baseUrl, String providerKey) {
        String haystack = (String.valueOf(baseUrl) + " " + providerKey).toLowerCase(Locale.ROOT);
        if (haystack.contains("deepseek")) return "deepseek";
        if (haystack.contains("dashscope") || haystack.contains("qwen")) return "dashscope";
        if (haystack.contains("openai")) return "openai";
        if (haystack.contains("moonshot") || haystack.contains("kimi")) return "moonshot";
        if (haystack.contains("zhipu") || haystack.contains("glm")) return "zhipu";
        if (haystack.contains("google") || haystack.contains("gemini")) return "google";
        if (haystack.contains("hunyuan") || haystack.contains("tencent")) return "hunyuan";
        return null;
    }

    private Set<String> inferDatasets(List<String> tags, String description, String modelValue) {
        LinkedHashSet<String> datasets = new LinkedHashSet<>();
        for (String tag : tags) {
            String lower = tag.toLowerCase(Locale.ROOT);
            if (lower.startsWith("数据集:")) {
                datasets.add(tag.substring(4).trim());
            } else if (lower.startsWith("dataset:")) {
                datasets.add(tag.substring(8).trim());
            } else if (lower.contains("reddit")) {
                datasets.add("reddit");
            } else if (lower.contains("weibo")) {
                datasets.add("weibo");
            } else if (lower.contains("bigdata")) {
                datasets.add("bigdata");
            } else if (lower.contains("sigir")) {
                datasets.add("sigir");
            }
        }
        String extra = (String.valueOf(description) + " " + modelValue).toLowerCase(Locale.ROOT);
        if (datasets.isEmpty()) {
            if (extra.contains("reddit")) datasets.add("reddit");
            if (extra.contains("weibo")) datasets.add("weibo");
        }
        return datasets;
    }

    private List<String> parseTags(Object value) {
        if (value == null) {
            return List.of();
        }
        String raw = String.valueOf(value).trim();
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            if (raw.startsWith("[")) {
                return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            }
        } catch (Exception ignore) {
        }
        return List.of(raw.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private String normalizeCode(String input) {
        return String.valueOf(input == null ? "" : input).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String text(Object value, String fallback) {
        if (value == null) return fallback;
        String normalized = String.valueOf(value).trim();
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String textOrNull(Object value) {
        if (value == null) return null;
        String normalized = String.valueOf(value).trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
