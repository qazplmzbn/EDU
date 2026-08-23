package com.xyz.question_bank_management_system.modules.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentPromptTemplateRequest {
    @NotBlank(message = "Template name cannot be empty")
    private String templateName;

    @NotBlank(message = "Task type cannot be empty")
    private String taskType;

    private String description;

    @NotBlank(message = "Prompt text cannot be empty")
    private String promptText;
}
