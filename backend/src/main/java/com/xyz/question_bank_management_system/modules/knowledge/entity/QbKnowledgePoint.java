package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbKnowledgePoint {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String name;
    private String code;
    private Long parentId;
    private Integer level;
    private String knowledgeType;
    private String status;
    private String contentVersion;
    private String metadataJson;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    private Double masteryValue;
    private Integer attemptCount;
}
