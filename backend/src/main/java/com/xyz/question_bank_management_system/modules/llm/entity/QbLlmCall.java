package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QbLlmCall {
    private Long id;
    /** 1=QUESTION_ANALYSIS,2=SUBJECTIVE_GRADING,3=STUDENT_ASSISTANT,4=TEACHER_AGENT_RESOURCE */
    private Integer bizType;
    private Long bizId;
    private String modelName;
    private String promptText;
    private String responseText;
    /** JSON as String */
    private String responseJson;
    /** 0=pending,1=success,2=failed */
    private Integer callStatus;
    private Integer latencyMs;
    private Integer tokensPrompt;
    private Integer tokensCompletion;
    private BigDecimal costAmount;
    private LocalDateTime createdAt;
}
