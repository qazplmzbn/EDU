package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbKnowledgeRelation {
    private Long id;
    private Long sourceId;
    private Long targetId;
    private String relationType;
    private Double weight;
    private Double confidence;
    private String sourceType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    private String sourceName;
    private String targetName;
}
