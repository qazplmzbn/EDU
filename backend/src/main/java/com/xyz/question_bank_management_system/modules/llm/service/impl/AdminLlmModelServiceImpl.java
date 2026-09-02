package com.xyz.question_bank_management_system.modules.llm.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmProvider;
import com.xyz.question_bank_management_system.modules.llm.entity.QbPromptTemplate;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmProviderMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbPromptTemplateMapper;
import com.xyz.question_bank_management_system.modules.llm.service.AdminLlmModelService;
import com.xyz.question_bank_management_system.util.LlmSecretCodec;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLlmModelServiceImpl implements AdminLlmModelService {
    private static final String SOURCE_MANAGED = "managed-system";
    private static final String SOURCE_PROPERTY = "property-system";
    private static final String TYPE_API = "API";
    private static final String TYPE_LOCAL = "LOCAL";

    private final LlmProperties llmProperties;
    private final QbLlmProviderMapper providerMapper;
    private final QbPromptTemplateMapper templateMapper;
    private final LlmSecretCodec secretCodec;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<StudentLlmProviderVO> providers(String keyword, String providerType, Boolean enabled, boolean includeReadonly) {
        List<StudentLlmProviderVO> rows = new ArrayList<>();
        providerMapper.selectAll().stream().map(this::managedProviderVo).forEach(rows::add);
        if (includeReadonly) {
            llmProperties.questionAnalysisProviders().stream().map(this::propertyProviderVo).forEach(rows::add);
        }
        return rows.stream()
                .filter(row -> matchesProvider(row, keyword, providerType, enabled))
                .collect(Collectors.toList());
    }

    public Long createProvider(Long adminId, StudentLlmProviderRequest request) {
        QbLlmProvider provider = new QbLlmProvider();
        provider.setCreatedBy(adminId);
        applyProviderRequest(provider, request, null);
        provider.setEnabled(1);
        provider.setIsDefault(0);
        ensureProviderKeyUnique(provider.getProviderKey(), null);
        providerMapper.insert(provider);
        return provider.getId();
    }

    public void updateProvider(Long id, StudentLlmProviderRequest request) {
        QbLlmProvider provider = requireProvider(id);
        applyProviderRequest(provider, request, provider.getApiKeyCipher());
        ensureProviderKeyUnique(provider.getProviderKey(), id);
        providerMapper.update(provider);
    }

    public void updateProviderEnabled(Long id, Boolean enabled) {
        requireProvider(id);
        providerMapper.updateEnabled(id, Boolean.TRUE.equals(enabled) ? 1 : 0);
    }

    public void markProviderDefault(Long id) {
        requireProvider(id);
        providerMapper.clearDefault();
        providerMapper.markDefault(id);
    }

    public void deleteProvider(Long id) {
        requireProvider(id);
        providerMapper.softDelete(id);
    }

    public LlmProperties.ModelProvider resolveSystemProvider(String providerKey) {
        QbLlmProvider provider = null;
        if (StringUtils.hasText(providerKey)) {
            provider = providerMapper.selectByKey(providerKey.trim().toLowerCase(Locale.ROOT));
        } else {
            provider = providerMapper.selectDefault();
        }
        if (provider != null && Objects.equals(provider.getEnabled(), 1)) {
            return toModelProvider(provider);
        }
        if (!StringUtils.hasText(providerKey)) {
            return llmProperties.defaultProvider();
        }
        return llmProperties.getProvider(providerKey);
    }

    public List<StudentPromptTemplateVO> templates(String keyword, String taskType) {
        return templateMapper.selectAll().stream()
                .filter(item -> matchesTemplate(item, keyword, taskType))
                .map(this::templateVo)
                .collect(Collectors.toList());
    }

    public Long createTemplate(Long adminId, StudentPromptTemplateRequest request) {
        QbPromptTemplate template = new QbPromptTemplate();
        template.setCreatedBy(adminId);
        applyTemplateRequest(template, request);
        templateMapper.insert(template);
        return template.getId();
    }

    public void updateTemplate(Long id, StudentPromptTemplateRequest request) {
        QbPromptTemplate template = requireTemplate(id);
        applyTemplateRequest(template, request);
        templateMapper.update(template);
    }

    public void deleteTemplate(Long id) {
        requireTemplate(id);
        templateMapper.softDelete(id);
    }

    private void applyProviderRequest(QbLlmProvider provider,
                                      StudentLlmProviderRequest request,
                                      String existingApiKeyCipher) {
        String providerType = normalizeProviderType(request.getProviderType());
        String providerKey = normalizeProviderKey(request.getProviderKey(), request.getLabel(), request.getModel());
        provider.setProviderKey(providerKey);
        provider.setLabel(text(request.getLabel()));
        provider.setProviderType(providerType);
        provider.setBaseUrl(trimTrailingSlash(text(request.getBaseUrl())));
        provider.setModel(text(request.getModel()));
        provider.setTemperature(normalizeTemperature(request.getTemperature()));
        provider.setSupportsTemperature(Boolean.TRUE.equals(request.getSupportsTemperature()));
        provider.setDescription(textOrNull(request.getDescription()));
        provider.setTagsJson(toTagsJson(request.getTags()));
        provider.setApiKeyCipher(StringUtils.hasText(request.getApiKey())
                ? secretCodec.encode(request.getApiKey())
                : existingApiKeyCipher);
    }

    private void applyTemplateRequest(QbPromptTemplate template, StudentPromptTemplateRequest request) {
        template.setTemplateName(text(request.getTemplateName()));
        template.setTaskType(text(request.getTaskType()));
        template.setDescription(textOrNull(request.getDescription()));
        template.setPromptText(text(request.getPromptText()));
    }

    private QbLlmProvider requireProvider(Long id) {
        QbLlmProvider provider = providerMapper.selectById(id);
        if (provider == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Model provider not found");
        }
        return provider;
    }

    private QbPromptTemplate requireTemplate(Long id) {
        QbPromptTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Prompt template not found");
        }
        return template;
    }

    private void ensureProviderKeyUnique(String providerKey, Long currentId) {
        QbLlmProvider duplicate = providerMapper.selectByKey(providerKey);
        if (duplicate != null && !Objects.equals(duplicate.getId(), currentId)) {
            throw BizException.of(ErrorCode.CONFLICT, "Provider key already exists");
        }
    }

    private LlmProperties.ModelProvider toModelProvider(QbLlmProvider source) {
        LlmProperties.ModelProvider provider = new LlmProperties.ModelProvider();
        provider.setEnabled(Objects.equals(source.getEnabled(), 1));
        provider.setKey(source.getProviderKey());
        provider.setLabel(source.getLabel());
        provider.setBaseUrl(source.getBaseUrl());
        // LOCAL providers do not authenticate, so an old encrypted API key must
        // not make an otherwise usable local fixture unavailable.
        provider.setApiKey(TYPE_LOCAL.equalsIgnoreCase(source.getProviderType())
                ? ""
                : secretCodec.decode(source.getApiKeyCipher()));
        provider.setModel(source.getModel());
        provider.setTemperature(source.getTemperature());
        provider.setSupportsTemperature(Boolean.TRUE.equals(source.getSupportsTemperature()));
        provider.setAuthRequired(!TYPE_LOCAL.equalsIgnoreCase(source.getProviderType()));
        return provider;
    }

    private StudentLlmProviderVO managedProviderVo(QbLlmProvider provider) {
        StudentLlmProviderVO vo = new StudentLlmProviderVO();
        vo.setId(provider.getId());
        vo.setSource(SOURCE_MANAGED);
        vo.setSystem(true);
        vo.setEditable(true);
        vo.setProviderKey(provider.getProviderKey());
        vo.setLabel(provider.getLabel());
        vo.setProviderType(provider.getProviderType());
        vo.setBaseUrl(provider.getBaseUrl());
        vo.setModel(provider.getModel());
        vo.setTemperature(provider.getTemperature());
        vo.setSupportsTemperature(Boolean.TRUE.equals(provider.getSupportsTemperature()));
        vo.setDescription(provider.getDescription());
        vo.setTags(parseTags(provider.getTagsJson()));
        vo.setEnabled(Objects.equals(provider.getEnabled(), 1));
        vo.setIsDefault(Objects.equals(provider.getIsDefault(), 1));
        vo.setHasApiKey(StringUtils.hasText(provider.getApiKeyCipher()));
        vo.setApiKeyMask(secretCodec.mask(provider.getApiKeyCipher()));
        vo.setCreatedAt(provider.getCreatedAt());
        vo.setUpdatedAt(provider.getUpdatedAt());
        return vo;
    }

    private StudentLlmProviderVO propertyProviderVo(LlmProperties.ModelProvider provider) {
        StudentLlmProviderVO vo = new StudentLlmProviderVO();
        vo.setSource(SOURCE_PROPERTY);
        vo.setSystem(true);
        vo.setEditable(false);
        vo.setProviderKey(provider.resolveKey());
        vo.setLabel(provider.resolveLabel());
        vo.setProviderType(TYPE_API);
        vo.setBaseUrl(provider.getBaseUrl());
        vo.setModel(provider.getModel());
        vo.setTemperature(provider.getTemperature());
        vo.setSupportsTemperature(provider.isSupportsTemperature());
        vo.setDescription("Readonly provider from application properties, shown as a system fallback model.");
        vo.setTags(List.of("properties", provider.isSupportsTemperature() ? "temperature" : "fixed-temperature"));
        vo.setEnabled(provider.isEnabled());
        vo.setIsDefault(false);
        vo.setHasApiKey(StringUtils.hasText(provider.resolveApiKey()));
        vo.setApiKeyMask(vo.getHasApiKey() ? "system-key" : "");
        return vo;
    }

    private StudentPromptTemplateVO templateVo(QbPromptTemplate template) {
        StudentPromptTemplateVO vo = new StudentPromptTemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTaskType(template.getTaskType());
        vo.setDescription(template.getDescription());
        vo.setPromptText(template.getPromptText());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }

    private boolean matchesProvider(StudentLlmProviderVO row, String keyword, String providerType, Boolean enabled) {
        if (StringUtils.hasText(providerType) && !providerType.trim().equalsIgnoreCase(row.getProviderType())) {
            return false;
        }
        if (enabled != null && !Objects.equals(enabled, row.getEnabled())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        String haystack = String.join(" ",
                safe(row.getLabel()),
                safe(row.getProviderKey()),
                safe(row.getModel()),
                safe(row.getDescription()),
                row.getTags() == null ? "" : String.join(" ", row.getTags())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(needle);
    }

    private boolean matchesTemplate(QbPromptTemplate item, String keyword, String taskType) {
        if (StringUtils.hasText(taskType) && !taskType.trim().equalsIgnoreCase(item.getTaskType())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        return String.join(" ",
                safe(item.getTemplateName()),
                safe(item.getTaskType()),
                safe(item.getDescription()),
                safe(item.getPromptText())
        ).toLowerCase(Locale.ROOT).contains(needle);
    }

    private String normalizeProviderType(String value) {
        String type = text(value).toUpperCase(Locale.ROOT);
        if ("LOCAL".equals(type) || "LOCAL_LLM".equals(type)) {
            return TYPE_LOCAL;
        }
        return TYPE_API;
    }

    private String normalizeProviderKey(String rawKey, String label, String model) {
        String source = StringUtils.hasText(rawKey) ? rawKey : (StringUtils.hasText(model) ? model : label);
        String normalized = source == null ? "" : source.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!StringUtils.hasText(normalized)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "Provider key cannot be empty");
        }
        return normalized;
    }

    private Double normalizeTemperature(Double value) {
        if (value == null) {
            return 0.2;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > 2) {
            return 2.0;
        }
        return value;
    }

    private String toTagsJson(String tags) {
        List<String> values = parseTags(tags);
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> parseTags(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                }).stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
            }
        } catch (Exception ignore) {
        }
        return List.of(trimmed.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String text(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "Required field cannot be empty");
        }
        return normalized;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

