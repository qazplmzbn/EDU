package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLearningBehavior {
    private Long id;
    private Long userId;
    private String behaviorType;
    private String refType;
    private Long refId;
    private Long knowledgePointId;
    private Integer durationSeconds;
    private String eventValue;
    private String note;
    private LocalDateTime createdAt;
}
