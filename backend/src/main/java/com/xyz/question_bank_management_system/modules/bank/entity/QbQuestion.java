package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbQuestion {
    private Long id;
    private String title;
    /** 1=single,2=multiple,3=true_false,4=blank,5=short,6=code,7=code_reading */
    private Integer questionType;
    /** 1~5 */
    private Integer difficulty;
    private String chapter;
    private String stem;
    private String standardAnswer;
    /** 1=text,2=json */
    private Integer answerFormat;
    private String analysisText;
    /** 1=manual,2=llm_draft,3=llm_final */
    private Integer analysisSource;
    private Long analysisLlmCallId;
    /** 1=draft,2=published,3=archived */
    private Integer status;
    /** 0=private,1=pending,2=approved,3=rejected */
    private Integer bankReviewStatus;
    private Long bankReviewerId;
    private LocalDateTime bankReviewedAt;
    private String bankReviewComment;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
