package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeGraphSyncRecord {
    private Long id;
    private Long graphVersionId;
    private String syncCode;
    private String status;
    private Integer nodeCount;
    private Integer edgeCount;
    private String contentHash;
    private String errorMessage;
    private String correlationId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
