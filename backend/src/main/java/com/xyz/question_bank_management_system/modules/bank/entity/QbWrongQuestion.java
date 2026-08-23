package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbWrongQuestion {
    private Long userId;
    private Long questionId;
    private Integer wrongCount;
    private LocalDateTime firstWrongAt;
    private LocalDateTime lastWrongAt;
    private Integer isResolved;
    private LocalDateTime resolvedAt;
}
