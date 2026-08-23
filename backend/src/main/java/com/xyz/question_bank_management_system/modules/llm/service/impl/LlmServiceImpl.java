package com.xyz.question_bank_management_system.modules.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.llm.service.AdminLlmModelService;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.llm.service.StudentLlmModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final LlmProperties llmProperties;
    private final QbLlmCallMapper llmCallMapper;
    private final AdminLlmModelService adminLlmModelService;
    private final StudentLlmModelService studentLlmModelService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    @Override
    public QbLlmCall chatCompletion(int bizType, long bizId, String prompt) {
        return chatCompletion(bizType, bizId, prompt, null);
    }

    @Override
    public QbLlmCall chatCompletion(int bizType, long bizId, String prompt, String providerKey) {
        return chatCompletion(bizType, bizId, prompt, providerKey, null);
    }

    @Override
    public QbLlmCall chatCompletion(int bizType, long bizId, String prompt, String providerKey, Long userId) {
        LlmProperties.ModelProvider provider = resolveProvider(providerKey, userId);

        QbLlmCall call = new QbLlmCall();
        call.setBizType(bizType);
        call.setBizId(bizId);
        call.setModelName(provider == null ? providerKey : provider.getModel());
        call.setPromptText(prompt);
        call.setCallStatus(0);
        llmCallMapper.insert(call);

        if (provider == null) {
            return failCall(call, "未找到可用的大模型配置", "{\"error\":\"provider_missing\"}", 0);
        }

        String apiKey = provider.resolveApiKey();
        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
            return failCall(call, "大模型服务地址未配置", "{\"error\":\"base_url_missing\"}", 0);
        }
        if (provider.getModel() == null || provider.getModel().isBlank()) {
            return failCall(call, "大模型名称未配置", "{\"error\":\"model_missing\"}", 0);
        }
        if (provider.isAuthRequired() && (apiKey == null || apiKey.isBlank())) {
            return failCall(call, "大模型密钥未配置", "{\"error\":\"api_key_missing\"}", 0);
        }

        long start = System.currentTimeMillis();
        try {
            String endpoint = normalizeBaseUrl(provider.getBaseUrl()) + "/chat/completions";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", provider.getModel());
            if (provider.isSupportsTemperature() && provider.getTemperature() != null) {
                requestBody.put("temperature", provider.getTemperature());
            }

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", llmProperties.getSystemPrompt());
            messages.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.set("messages", messages);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", normalizeAuthorization(apiKey));
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) (System.currentTimeMillis() - start);

            call.setLatencyMs(latencyMs);
            call.setTokensPrompt(0);
            call.setTokensCompletion(0);
            call.setCostAmount(BigDecimal.ZERO);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                call.setResponseText(response.body());
                call.setResponseJson(response.body());
                fillUsage(call, response.body());
                call.setCallStatus(1);
            } else {
                call.setResponseText("模型调用失败，HTTP 状态码：" + response.statusCode());
                call.setResponseJson(response.body());
                call.setCallStatus(2);
            }

            llmCallMapper.updateResponse(call);
            return call;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            int latencyMs = (int) (System.currentTimeMillis() - start);
            log.warn("大模型调用被中断，provider={}", provider.getKey(), ex);
            return failCall(call, "模型调用被中断，请稍后重试", "{\"error\":\"interrupted\"}", latencyMs);
        } catch (Exception ex) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            log.warn("大模型调用失败，provider={}", provider.getKey(), ex);
            return failCall(
                    call,
                    "模型调用失败，请检查网络、密钥或模型配置",
                    "{\"error\":\"exception\",\"message\":\"" + safeJson(ex.getMessage()) + "\"}",
                    latencyMs
            );
        }
    }

    @Override
    public String extractContent(String responseText) {
        try {
            JsonNode root = objectMapper.readTree(responseText);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode contentNode = choices.get(0).path("message").path("content");
            return extractMessageContent(contentNode);
        } catch (Exception e) {
            return null;
        }
    }

    private LlmProperties.ModelProvider resolveProvider(String providerKey, Long userId) {
        LlmProperties.ModelProvider userProvider = studentLlmModelService.resolveUserProvider(userId, providerKey);
        if (userProvider != null) {
            return userProvider;
        }
        return adminLlmModelService.resolveSystemProvider(providerKey);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeAuthorization(String apiKey) {
        String normalized = apiKey == null ? "" : apiKey.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return normalized;
        }
        return "Bearer " + normalized;
    }

    private void fillUsage(QbLlmCall call, String responseBody) {
        try {
            JsonNode usage = objectMapper.readTree(responseBody).path("usage");
            if (usage.has("prompt_tokens")) {
                call.setTokensPrompt(usage.get("prompt_tokens").asInt());
            }
            if (usage.has("completion_tokens")) {
                call.setTokensCompletion(usage.get("completion_tokens").asInt());
            }
        } catch (Exception ignore) {
        }
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String text = item.has("text") ? item.path("text").asText("") : item.asText("");
                if (text.isBlank()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(text);
            }
            return builder.isEmpty() ? null : builder.toString();
        }
        return contentNode.toString();
    }

    private QbLlmCall failCall(QbLlmCall call, String responseText, String responseJson, int latencyMs) {
        call.setResponseText(responseText);
        call.setResponseJson(responseJson);
        call.setCallStatus(2);
        call.setLatencyMs(latencyMs);
        call.setTokensPrompt(0);
        call.setTokensCompletion(0);
        call.setCostAmount(BigDecimal.ZERO);
        llmCallMapper.updateResponse(call);
        return call;
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
