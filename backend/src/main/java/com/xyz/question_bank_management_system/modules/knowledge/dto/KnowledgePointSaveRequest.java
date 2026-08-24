package com.xyz.question_bank_management_system.modules.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgePointSaveRequest {
    @NotBlank private String name;
    private String code;
    private Long parentId;
    @Min(1) private Integer level;
    private String knowledgeType;
    @Min(1) @Max(5) private Integer difficulty;
    private String description;
}
