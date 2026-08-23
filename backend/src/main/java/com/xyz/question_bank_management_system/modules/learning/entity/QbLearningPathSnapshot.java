package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLearningPathSnapshot {
    private Long id;
    private Long userId;
    private String stage;
    private String goal;
    private Integer days;
    private String title;
    private String summaryText;
    private String snapshotJson;
    private String snapshotHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
