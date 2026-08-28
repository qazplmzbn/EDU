package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningPath {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long goalId;
    private Long targetOccupationId;
    private Long profileSnapshotId;
    private String title;
    private String stage;
    private Integer planningDays;
    private Long version;
    private String status;
    private String summaryText;
    private Long generatedByAgentTaskId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
