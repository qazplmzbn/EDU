package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbCompetencyJobSyncRecord {
    private Long id;
    private String triggerType;
    private Long triggerBy;
    private String platform;
    private String status;
    private Integer keywordCount;
    private Integer cityCount;
    private Integer fetchedCandidateCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer offlineCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
