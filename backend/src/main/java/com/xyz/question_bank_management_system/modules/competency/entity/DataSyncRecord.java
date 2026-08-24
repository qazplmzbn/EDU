package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataSyncRecord {
    private Long id;
    private String syncType;
    private String sourceName;
    private String triggerType;
    private Long triggerBy;
    private String syncVersion;
    private String status;
    private Integer fetchedCount;
    private Integer insertedCount;
    private Integer updatedCount;
    private Integer failedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
