package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Immutable once referenced by an agent task; later edits create a new version. */
@Data
public class PromptTemplate {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String templateName;
    private String taskType;
    private String description;
    private String promptText;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
