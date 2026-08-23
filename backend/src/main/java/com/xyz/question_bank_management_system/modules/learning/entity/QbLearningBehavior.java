package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLearningBehavior {
    private Long id;
    private Long userId;
    private String behaviorType;
    private Long refId;
    private Long knowledgePointId;
    private Long tagId;
    private Integer durationSeconds;
    private String note;
    private LocalDateTime createdAt;

    private String knowledgePointName;
    private String tagName;
}
