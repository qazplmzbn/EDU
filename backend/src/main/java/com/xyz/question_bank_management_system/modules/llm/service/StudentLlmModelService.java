package com.xyz.question_bank_management_system.modules.llm.service;

import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentLlmProviderRequest;
import com.xyz.question_bank_management_system.modules.llm.dto.StudentPromptTemplateRequest;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentLlmProviderVO;
import com.xyz.question_bank_management_system.modules.llm.vo.StudentPromptTemplateVO;

import java.util.List;

public interface StudentLlmModelService {

    List<StudentLlmProviderVO> providers(Long userId, String keyword, String providerType, Boolean enabled);

    Long createProvider(Long userId, StudentLlmProviderRequest request);

    void updateProvider(Long userId, Long id, StudentLlmProviderRequest request);

    void updateProviderEnabled(Long userId, Long id, Boolean enabled);

    void markProviderDefault(Long userId, Long id);

    void deleteProvider(Long userId, Long id);

    LlmProperties.ModelProvider resolveUserProvider(Long userId, String providerKey);

    List<StudentPromptTemplateVO> templates(Long userId, String keyword, String taskType);

    Long createTemplate(Long userId, StudentPromptTemplateRequest request);

    void updateTemplate(Long userId, Long id, StudentPromptTemplateRequest request);

    void deleteTemplate(Long userId, Long id);
}
