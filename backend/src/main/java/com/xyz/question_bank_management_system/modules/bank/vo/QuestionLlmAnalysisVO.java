package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionLlmAnalysisVO {
    private String analysisKey;
    private String analysisLabel;
    private String modelName;
    private Boolean hasAnalysis;
    private Long llmCallId;
    private Integer callStatus;
    private String analysisText;
    private String errorMessage;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
