package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeGraphVersion {
    private Long id;
    private String versionCode;
    private Long courseId;
    private String description;
    private String status;
    private Integer nodeCount;
    private Integer edgeCount;
    private String contentHash;
    private String validationReportJson;
    private String correlationId;
    private Long createdBy;
    private Long importId;
    private String reviewStatus;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
