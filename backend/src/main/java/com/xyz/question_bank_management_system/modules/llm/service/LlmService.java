package com.xyz.question_bank_management_system.modules.llm.service;

import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;

public interface LlmService {

    /**
     * bizType: 1=QUESTION_ANALYSIS,2=SUBJECTIVE_GRADING,3=STUDENT_ASSISTANT,4=TEACHER_AGENT_RESOURCE
     */
    QbLlmCall chatCompletion(int bizType, long bizId, String prompt);

    /**
     * bizType: 1=QUESTION_ANALYSIS,2=SUBJECTIVE_GRADING,3=STUDENT_ASSISTANT,4=TEACHER_AGENT_RESOURCE
     */
    QbLlmCall chatCompletion(int bizType, long bizId, String prompt, String providerKey);

    /**
     * bizType: 1=QUESTION_ANALYSIS,2=SUBJECTIVE_GRADING,3=STUDENT_ASSISTANT,4=TEACHER_AGENT_RESOURCE
     */
    QbLlmCall chatCompletion(int bizType, long bizId, String prompt, String providerKey, Long userId);

    /**
     * Extract text content from an OpenAI-compatible response.
     */
    String extractContent(String responseText);
}
