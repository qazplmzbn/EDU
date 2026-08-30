package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KnowledgePointLegacyMapping {
    private Long id;
    private Long importId;
    private Long legacyKnowledgePointId;
    private String targetType;
    private String targetExternalCode;
    private String mappingType;
    private BigDecimal confidence;
    private String reviewStatus;
    private String notes;
    private LocalDateTime createdAt;
}
