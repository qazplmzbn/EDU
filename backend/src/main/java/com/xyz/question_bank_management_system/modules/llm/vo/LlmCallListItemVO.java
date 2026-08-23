package com.xyz.question_bank_management_system.modules.llm.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LlmCallListItemVO {
    private Long llmCallId;
    private Integer bizType;
    private Long bizId;
    private String modelName;
    private Integer callStatus;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
