package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbGradingRecord {
    private Long id;
    private Long answerId;
    /** 1=auto,2=llm,3=manual */
    private Integer gradingMode;
    private Integer score;
    /** JSON as String */
    private String detailJson;
    private Long llmCallId;
    private Double confidence;
    private Integer needsReview;
    private Long reviewerId;
    private String reviewComment;
    private Integer isFinal;
    private LocalDateTime createdAt;
}
