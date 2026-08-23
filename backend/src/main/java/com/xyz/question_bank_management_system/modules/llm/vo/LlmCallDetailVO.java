package com.xyz.question_bank_management_system.modules.llm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LlmCallDetailVO {
    private Long llmCallId;
    private Integer bizType;
    private Long bizId;
    private String modelName;
    private String promptText;
    private String responseText;
    private String responseJson;
    private Integer callStatus;
    private Integer tokensPrompt;
    private Integer tokensCompletion;
    private Integer latencyMs;
    private BigDecimal costAmount;
    private LocalDateTime createdAt;
}
