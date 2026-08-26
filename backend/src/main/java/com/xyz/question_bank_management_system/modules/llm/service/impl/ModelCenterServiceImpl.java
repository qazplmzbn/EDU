package com.xyz.question_bank_management_system.modules.llm.service.impl;

import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.llm.entity.ModelConfig;
import com.xyz.question_bank_management_system.modules.llm.entity.PromptTemplate;
import com.xyz.question_bank_management_system.modules.llm.mapper.ModelConfigMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.PromptTemplateMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.llm.service.ModelCenterService;
import com.xyz.question_bank_management_system.util.LlmSecretCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Compatibility implementation for /api/models. The old dynamic models and
 * prompt_templates tables are intentionally not queried by this service.
 */
@Service
@RequiredArgsConstructor
public class ModelCenterServiceImpl implements ModelCenterService {
    private final ModelConfigMapper modelMapper;
    private final PromptTemplateMapper templateMapper;
    private final LlmSecretCodec secretCodec;
    private final LlmService llmService;

    @Override
    public List<Map<String, Object>> listModels(Long userId, String keyword, String category, String modelType, String status, Boolean enabled) {
        List<ModelConfig> rows = new ArrayList<>(modelMapper.selectByOwner("system", null));
        rows.addAll(modelMapper.selectByOwner("user", userId));
        return rows.stream().filter(row -> matches(row, keyword, modelType, enabled)).map(this::modelMap).toList();
    }

    @Override public Map<String, Object> getModel(Long userId, Long modelId) { return modelMap(requireModel(userId, false, modelId)); }

    @Override
    public Map<String, Object> createModel(Long userId, boolean admin, Map<String, Object> payload) {
        boolean system = admin && "system".equalsIgnoreCase(text(payload, "ownerType", "user"));
        ModelConfig row = new ModelConfig();
        row.setOwnerType(system ? "system" : "user");
        row.setOwnerId(system ? 0L : userId);
        applyModel(row, payload, null);
        row.setEnabled(bool(payload, "enabled", true) ? 1 : 0);
        row.setIsDefault(bool(payload, "isDefault", false) ? 1 : 0);
        ensureUnique(row);
        if (row.getIsDefault() == 1) modelMapper.clearDefault(row.getOwnerType(), row.getOwnerId());
        modelMapper.insert(row);
        return modelMap(row);
    }

    @Override
    public Map<String, Object> updateModel(Long userId, boolean admin, Long modelId, Map<String, Object> payload) {
        ModelConfig row = requireModel(userId, admin, modelId);
        applyModel(row, payload, row.getApiKeyCipher());
        row.setEnabled(bool(payload, "enabled", row.getEnabled() != 0) ? 1 : 0);
        row.setIsDefault(bool(payload, "isDefault", row.getIsDefault() != 0) ? 1 : 0);
        ensureUnique(row);
        if (row.getIsDefault() == 1) modelMapper.clearDefault(row.getOwnerType(), row.getOwnerId());
        modelMapper.update(row);
        return modelMap(row);
    }

    @Override public void deleteModel(Long userId, boolean admin, Long modelId) { modelMapper.delete(requireModel(userId, admin, modelId).getId()); }

    @Override
    public Map<String, Object> updateModelApiKey(Long userId, boolean admin, Long modelId, String apiKey) {
        ModelConfig row = requireModel(userId, admin, modelId);
        if (!StringUtils.hasText(apiKey)) throw BizException.of(ErrorCode.PARAM_ERROR, "API Key cannot be empty");
        row.setApiKeyCipher(secretCodec.encode(apiKey));
        modelMapper.update(row);
        return modelMap(row);
    }

    @Override
    public List<Map<String, Object>> listTemplates(Long userId, String keyword, String taskType, Long modelId) {
        List<PromptTemplate> rows = new ArrayList<>(templateMapper.selectByOwner("system", null));
        rows.addAll(templateMapper.selectByOwner("user", userId));
        return rows.stream().filter(t -> matchTemplate(t, keyword, taskType)).map(this::templateMap).toList();
    }
    @Override public Map<String, Object> getTemplate(Long userId, Long templateId) { return templateMap(requireTemplate(userId, false, templateId)); }

    @Override
    public Map<String, Object> createTemplate(Long userId, boolean admin, Map<String, Object> payload) {
        boolean system = admin && "system".equalsIgnoreCase(text(payload, "ownerType", "user"));
        PromptTemplate row = new PromptTemplate();
        row.setOwnerType(system ? "system" : "user"); row.setOwnerId(system ? 0L : userId);
        row.setTemplateName(required(payload, "templateName", "name")); row.setTaskType(required(payload, "taskType", "task_type"));
        row.setDescription(text(payload, "description", null)); row.setPromptText(required(payload, "promptText", "prompt_content")); row.setVersion("v1");
        templateMapper.insert(row); return templateMap(row);
    }

    @Override
    public Map<String, Object> updateTemplate(Long userId, boolean admin, Long templateId, Map<String, Object> payload) {
        PromptTemplate current = requireTemplate(userId, admin, templateId);
        // Versioned copy: past agent tasks retain their exact prompt row.
        PromptTemplate next = new PromptTemplate();
        next.setOwnerType(current.getOwnerType()); next.setOwnerId(current.getOwnerId());
        next.setTemplateName(text(payload, "templateName", current.getTemplateName())); next.setTaskType(text(payload, "taskType", current.getTaskType()));
        next.setDescription(text(payload, "description", current.getDescription())); next.setPromptText(text(payload, "promptText", current.getPromptText()));
        next.setVersion(nextVersion(current.getVersion())); templateMapper.insert(next); return templateMap(next);
    }
    @Override public void deleteTemplate(Long userId, boolean admin, Long templateId) { templateMapper.delete(requireTemplate(userId, admin, templateId).getId()); }

    @Override public List<Map<String, Object>> compareModels(Long userId, String modelIds) {
        if (!StringUtils.hasText(modelIds)) return List.of();
        return Arrays.stream(modelIds.split(",")).map(String::trim).filter(StringUtils::hasText).map(Long::valueOf)
                .map(id -> modelMap(requireModel(userId, false, id))).toList();
    }
    @Override public Map<String, Object> inferenceHistory(Long userId, Long modelId, int limit) { return Map.of("total", 0, "list", List.of(), "model_id", modelId); }
    @Override public Map<String, Object> getOllamaStatus(String baseUrl) { return Map.of("available", false, "base_url", baseUrl == null ? "http://localhost:11434" : baseUrl, "models", List.of()); }
    @Override public List<Map<String, Object>> getConfiguredOllamaModels(Long userId) { return listModels(userId,null,"LOCAL","LOCAL",null,true); }
    @Override public List<Map<String, Object>> getConfiguredApiModels(Long userId) { return listModels(userId,null,"API","API",null,true); }
    @Override public List<Map<String, Object>> listProviders() { return List.of(Map.of("key","API","label","OpenAI-compatible API"),Map.of("key","LOCAL","label","Ollama / local model")); }

    @Override
    public Map<String, Object> testModel(Long userId, Long modelId, String prompt) {
        ModelConfig model = requireModel(userId, false, modelId);
        return call(model, userId, prompt == null ? "Please answer with OK" : prompt, null);
    }
    @Override
    public Map<String, Object> callModel(Long userId, Long modelId, String prompt, String systemPrompt) {
        ModelConfig model = requireModel(userId, false, modelId);
        return call(model, userId, prompt, systemPrompt);
    }

    private Map<String, Object> call(ModelConfig model, Long userId, String prompt, String systemPrompt) {
        var call = llmService.chatCompletion(5, userId == null ? 0L : userId, prompt == null ? "" : prompt, model.getProviderKey(), "user".equals(model.getOwnerType()) ? userId : null);
        return Map.of("success", call.getCallStatus() != null && call.getCallStatus() == 1, "modelId", model.getId(), "modelName", model.getModel(), "response", call.getResponseText() == null ? "" : call.getResponseText(), "llmCallId", call.getId());
    }

    private ModelConfig requireModel(Long userId, boolean admin, Long id) {
        ModelConfig row = modelMapper.selectById(id);
        if (row == null || (!admin && "user".equals(row.getOwnerType()) && !Objects.equals(row.getOwnerId(), userId))) throw BizException.of(ErrorCode.NOT_FOUND, "Model not found");
        return row;
    }
    private PromptTemplate requireTemplate(Long userId, boolean admin, Long id) {
        PromptTemplate row = templateMapper.selectById(id);
        if (row == null || (!admin && "user".equals(row.getOwnerType()) && !Objects.equals(row.getOwnerId(), userId))) throw BizException.of(ErrorCode.NOT_FOUND, "Template not found");
        return row;
    }
    private void applyModel(ModelConfig row, Map<String,Object> payload, String existingCipher) {
        row.setProviderKey(required(payload,"providerKey","modelCode").toLowerCase(Locale.ROOT)); row.setLabel(required(payload,"label","modelName"));
        row.setProviderType(text(payload,"providerType",text(payload,"modelType","API")).toUpperCase(Locale.ROOT)); row.setBaseUrl(required(payload,"baseUrl","apiBaseUrl"));
        row.setModel(required(payload,"model","modelCode")); row.setTemperature(number(payload,"temperature",0.2D));
        String apiKey=text(payload,"apiKey",null); row.setApiKeyCipher(StringUtils.hasText(apiKey)?secretCodec.encode(apiKey):existingCipher);
    }
    private void ensureUnique(ModelConfig row) { ModelConfig existing=modelMapper.selectByOwnerAndKey(row.getOwnerType(),row.getOwnerId(),row.getProviderKey()); if(existing!=null&&!Objects.equals(existing.getId(),row.getId())) throw BizException.of(ErrorCode.CONFLICT,"Provider key already exists"); }
    private boolean matches(ModelConfig row,String keyword,String type,Boolean enabled){ return (enabled==null||enabled==(row.getEnabled()!=0)) && (!StringUtils.hasText(type)||type.equalsIgnoreCase(row.getProviderType())) && (!StringUtils.hasText(keyword)||String.join(" ",row.getProviderKey(),row.getLabel(),row.getModel()).toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))); }
    private boolean matchTemplate(PromptTemplate row,String keyword,String type){ return (!StringUtils.hasText(type)||type.equalsIgnoreCase(row.getTaskType())) && (!StringUtils.hasText(keyword)||String.join(" ",row.getTemplateName(),row.getTaskType(),String.valueOf(row.getDescription()),row.getPromptText()).toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))); }
    private Map<String,Object> modelMap(ModelConfig row){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",row.getId());m.put("modelName",row.getLabel());m.put("modelCode",row.getProviderKey());m.put("modelCategory",row.getProviderType().equalsIgnoreCase("LOCAL")?"local_llm":"api");m.put("modelType",row.getProviderType().toLowerCase(Locale.ROOT));m.put("provider",row.getProviderKey());m.put("apiBaseUrl",row.getBaseUrl());m.put("model",row.getModel());m.put("temperature",row.getTemperature());m.put("enabled",row.getEnabled()!=0);m.put("status",row.getEnabled()!=0?"active":"inactive");m.put("isDefault",row.getIsDefault()!=0);m.put("ownerUserId",row.getOwnerId());m.put("sourceScope",row.getOwnerType());m.put("hasApiKey",StringUtils.hasText(row.getApiKeyCipher()));m.put("apiKeyMask",secretCodec.mask(row.getApiKeyCipher()));m.put("updatedAt",row.getUpdatedAt());return m; }
    private Map<String,Object> templateMap(PromptTemplate row){ return Map.of("id",row.getId(),"name",row.getTemplateName(),"templateName",row.getTemplateName(),"taskType",row.getTaskType(),"description",row.getDescription()==null?"":row.getDescription(),"promptText",row.getPromptText(),"version",row.getVersion(),"ownerType",row.getOwnerType(),"ownerId",row.getOwnerId()==null?0L:row.getOwnerId()); }
    private String required(Map<String,Object> map,String... keys){ for(String key:keys){String value=text(map,key,null);if(StringUtils.hasText(value))return value;}throw BizException.of(ErrorCode.PARAM_ERROR,"Required field cannot be empty"); }
    private String text(Map<String,Object> map,String key,String fallback){Object v=map==null?null:map.get(key);return v==null||!StringUtils.hasText(String.valueOf(v).trim())?fallback:String.valueOf(v).trim();}
    private boolean bool(Map<String,Object> map,String key,boolean fallback){Object v=map==null?null:map.get(key);return v==null?fallback:v instanceof Boolean b?b:Boolean.parseBoolean(String.valueOf(v));}
    private double number(Map<String,Object> map,String key,double fallback){try{return map!=null&&map.get(key)!=null?Double.parseDouble(String.valueOf(map.get(key))):fallback;}catch(Exception ex){return fallback;}}
    private String nextVersion(String version){try{return "v"+(Integer.parseInt(String.valueOf(version).replaceFirst("^[vV]",""))+1);}catch(Exception e){return "v2";}}
}
