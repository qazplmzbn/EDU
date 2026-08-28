package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningPathItem {
    private Long id;
    private Long pathId;
    private Integer orderNo;
    private String itemType;
    private Long knowledgePointId;
    private Long resourceId;
    private Long questionId;
    private Long assignmentId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private String status;
    private String decisionReason;
    private LocalDateTime createdAt;
}
