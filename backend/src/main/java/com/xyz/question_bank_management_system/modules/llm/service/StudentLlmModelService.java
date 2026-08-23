package com.xyz.question_bank_management_system.modules.llm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.entity.QbUserLlmProvider;
import com.xyz.question_bank_management_system.modules.llm.entity.QbUserPromptTemplate;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbUserLlmProviderMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbUserPromptTemplateMapper;
import com.xyz.question_bank_management_system.util.LlmSecretCodec;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentLlmModelService {

    private static final String SOURCE_PERSONAL = "personal";
    private static final String TYPE_API = "API";
    private static final String TYPE_LOCAL = "LOCAL";

    private final AdminLlmModelService adminLlmModelService;
    private final QbUserLlmProviderMapper providerMapper;
    private final QbUserPromptTemplateMapper templateMapper;
    private final LlmSecretCodec secretCodec;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<StudentLlmProviderVO> providers(Long userId, String keyword, String providerType, Boolean enabled) {
        Map<String, StudentLlmProviderVO> rows = new LinkedHashMap<>();
        adminLlmModelService.providers(null, null, null, true).stream()
                .map(this::systemProviderVo)
                .forEach(item -> rows.put(item.getProviderKey(), item));
        providerMapper.selectByUserId(userId).stream()
                .map(this::personalProviderVo)
                .forEach(item -> rows.put(item.getProviderKey(), item));
        return new ArrayList<>(rows.values()).stream()
                .filter(row -> matchesProvider(row, keyword, providerType, enabled))
                .collect(Collectors.toList());
    }

    public Long createProvider(Long userId, StudentLlmProviderRequest request) {
        QbUserLlmProvider provider = new QbUserLlmProvider();
        provider.setUserId(userId);
        applyProviderRequest(provider, request, null);
        provider.setEnabled(1);
        provider.setIsDefault(0);
        ensureProviderKeyUnique(userId, provider.getProviderKey(), null);
        providerMapper.insert(provider);
        return provider.getId();
    }

    public void updateProvider(Long userId, Long id, StudentLlmProviderRequest request) {
        QbUserLlmProvider existing = requireProvider(userId, id);
        applyProviderRequest(existing, request, existing.getApiKeyCipher());
        ensureProviderKeyUnique(userId, existing.getProviderKey(), id);
        providerMapper.update(existing);
    }

    public void updateProviderEnabled(Long userId, Long id, Boolean enabled) {
        requireProvider(userId, id);
        providerMapper.updateEnabled(id, userId, Boolean.TRUE.equals(enabled) ? 1 : 0);
    }

    public void markProviderDefault(Long userId, Long id) {
        requireProvider(userId, id);
        providerMapper.clearDefault(userId);
        providerMapper.markDefault(id, userId);
    }

    public void deleteProvider(Long userId, Long id) {
        requireProvider(userId, id);
        providerMapper.softDelete(id, userId);
    }

    public LlmProperties.ModelProvider resolveUserProvider(Long userId, String providerKey) {
        if (userId == null) {
            return null;
        }
        QbUserLlmProvider provider = null;
        try {
            if (StringUtils.hasText(providerKey)) {
                provider = providerMapper.selectByUserAndKey(userId, providerKey.trim().toLowerCase(Locale.ROOT));
            } else {
                provider = providerMapper.selectDefaultByUserId(userId);
            }
        } catch (DataAccessException ex) {
            return null;
        }
        if (provider == null || !Objects.equals(provider.getEnabled(), 1)) {
            return null;
        }
        return toModelProvider(provider);
    }

    public List<StudentPromptTemplateVO> templates(Long userId, String keyword, String taskType) {
        return templateMapper.selectByUserId(userId).stream()
                .filter(item -> matchesTemplate(item, keyword, taskType))
                .map(this::templateVo)
                .collect(Collectors.toList());
    }

    public Long createTemplate(Long userId, StudentPromptTemplateRequest request) {
        QbUserPromptTemplate template = new QbUserPromptTemplate();
        template.setUserId(userId);
        applyTemplateRequest(template, request);
        templateMapper.insert(template);
        return template.getId();
    }

    public void updateTemplate(Long userId, Long id, StudentPromptTemplateRequest request) {
        QbUserPromptTemplate template = requireTemplate(userId, id);
        applyTemplateRequest(template, request);
        templateMapper.update(template);
    }

    public void deleteTemplate(Long userId, Long id) {
        requireTemplate(userId, id);
        templateMapper.softDelete(id, userId);
    }

    private void applyProviderRequest(QbUserLlmProvider provider,
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

    private void applyTemplateRequest(QbUserPromptTemplate template, StudentPromptTemplateRequest request) {
        template.setTemplateName(text(request.getTemplateName()));
        template.setTaskType(text(request.getTaskType()));
        template.setDescription(textOrNull(request.getDescription()));
        template.setPromptText(text(request.getPromptText()));
    }

    private QbUserLlmProvider requireProvider(Long userId, Long id) {
        QbUserLlmProvider provider = providerMapper.selectOwnedById(id, userId);
        if (provider == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Model provider not found or not owned by current user");
        }
        return provider;
    }

    private QbUserPromptTemplate requireTemplate(Long userId, Long id) {
        QbUserPromptTemplate template = templateMapper.selectOwnedById(id, userId);
        if (template == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Prompt template not found or not owned by current user");
        }
        return template;
    }

    private void ensureProviderKeyUnique(Long userId, String providerKey, Long currentId) {
        QbUserLlmProvider duplicate = providerMapper.selectByUserAndKey(userId, providerKey);
        if (duplicate != null && !Objects.equals(duplicate.getId(), currentId)) {
            throw BizException.of(ErrorCode.CONFLICT, "Provider key already exists");
        }
    }

    private LlmProperties.ModelProvider toModelProvider(QbUserLlmProvider source) {
        LlmProperties.ModelProvider provider = new LlmProperties.ModelProvider();
        provider.setEnabled(Objects.equals(source.getEnabled(), 1));
        provider.setKey(source.getProviderKey());
        provider.setLabel(source.getLabel());
        provider.setBaseUrl(source.getBaseUrl());
        provider.setApiKey(secretCodec.decode(source.getApiKeyCipher()));
        provider.setModel(source.getModel());
        provider.setTemperature(source.getTemperature());
        provider.setSupportsTemperature(Boolean.TRUE.equals(source.getSupportsTemperature()));
        provider.setAuthRequired(!TYPE_LOCAL.equalsIgnoreCase(source.getProviderType()));
        return provider;
    }

    private StudentLlmProviderVO personalProviderVo(QbUserLlmProvider provider) {
        StudentLlmProviderVO vo = new StudentLlmProviderVO();
        vo.setId(provider.getId());
        vo.setSource(SOURCE_PERSONAL);
        vo.setSystem(false);
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

    private StudentLlmProviderVO systemProviderVo(StudentLlmProviderVO source) {
        StudentLlmProviderVO vo = new StudentLlmProviderVO();
        vo.setId(source.getId());
        vo.setSource(source.getSource());
        vo.setSystem(true);
        vo.setEditable(false);
        vo.setProviderKey(source.getProviderKey());
        vo.setLabel(source.getLabel());
        vo.setProviderType(source.getProviderType());
        vo.setBaseUrl(source.getBaseUrl());
        vo.setModel(source.getModel());
        vo.setTemperature(source.getTemperature());
        vo.setSupportsTemperature(source.getSupportsTemperature());
        vo.setDescription(source.getDescription());
        vo.setTags(source.getTags());
        vo.setEnabled(source.getEnabled());
        vo.setIsDefault(source.getIsDefault());
        vo.setHasApiKey(source.getHasApiKey());
        vo.setApiKeyMask(source.getApiKeyMask());
        vo.setCreatedAt(source.getCreatedAt());
        vo.setUpdatedAt(source.getUpdatedAt());
        return vo;
    }

    private StudentPromptTemplateVO templateVo(QbUserPromptTemplate template) {
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

    private boolean matchesTemplate(QbUserPromptTemplate item, String keyword, String taskType) {
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
