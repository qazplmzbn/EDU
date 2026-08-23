package com.xyz.question_bank_management_system.modules.llm.service;

import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;

import java.util.List;

public interface AdminLlmModelService {

    List<StudentLlmProviderVO> providers(String keyword, String providerType, Boolean enabled, boolean includeReadonly);

    Long createProvider(Long adminId, StudentLlmProviderRequest request);

    void updateProvider(Long id, StudentLlmProviderRequest request);

    void updateProviderEnabled(Long id, Boolean enabled);

    void markProviderDefault(Long id);

    void deleteProvider(Long id);

    LlmProperties.ModelProvider resolveSystemProvider(String providerKey);

    List<StudentPromptTemplateVO> templates(String keyword, String taskType);

    Long createTemplate(Long adminId, StudentPromptTemplateRequest request);

    void updateTemplate(Long id, StudentPromptTemplateRequest request);

    void deleteTemplate(Long id);
}
