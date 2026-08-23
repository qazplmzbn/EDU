package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbUserPromptTemplate {
    private Long id;
    private Long userId;
    private String templateName;
    private String taskType;
    private String description;
    private String promptText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
