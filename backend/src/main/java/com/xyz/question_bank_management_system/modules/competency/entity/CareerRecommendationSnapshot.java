package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CareerRecommendationSnapshot {
    private Long id;
    private String snapshotCode;
    private String gapSnapshotCode;
    private Long userId;
    private Long occupationId;
    private String targetBatchCode;
    private String algorithmVersion;
    private String requestJson;
    private String resultSummaryJson;
    private LocalDateTime createdAt;
}
