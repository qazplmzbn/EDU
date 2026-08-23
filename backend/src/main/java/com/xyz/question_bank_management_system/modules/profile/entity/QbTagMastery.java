package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbTagMastery {
    private Long userId;
    private Long tagId;
    private String tagName;
    private Double masteryValue;
    private Integer correctCount;
    private Integer attemptCount;
    private LocalDateTime updatedAt;
}
