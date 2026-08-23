package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbPromptTemplate {
    private Long id;
    private String templateName;
    private String taskType;
    private String description;
    private String promptText;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
