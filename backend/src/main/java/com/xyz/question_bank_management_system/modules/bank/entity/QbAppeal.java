package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbAppeal {
    private Long id;
    private Long answerId;
    private Long userId;
    private String reasonText;
    /** 1=pending,2=approved,3=rejected,4=resolved */
    private Integer appealStatus;
    private LocalDateTime createdAt;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String decisionComment;
    private Integer finalScore;
}
