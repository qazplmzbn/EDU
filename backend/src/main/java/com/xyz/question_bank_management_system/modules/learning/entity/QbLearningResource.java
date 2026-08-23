package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLearningResource {
    private Long id;
    private String title;
    private String resourceType;
    private String url;
    private String summary;
    private String content;
    private String personalizationBasis;
    private String reviewReportJson;
    private String modelSourceJson;
    private String auditStatus;
    private Long knowledgePointId;
    private Long tagId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    private String knowledgePointName;
    private String tagName;
}
