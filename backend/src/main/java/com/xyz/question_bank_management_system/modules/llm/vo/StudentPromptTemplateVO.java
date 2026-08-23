package com.xyz.question_bank_management_system.modules.llm.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentPromptTemplateVO {
    private Long id;
    private String templateName;
    private String taskType;
    private String description;
    private String promptText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
