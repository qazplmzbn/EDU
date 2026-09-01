package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CareerKnowledgeEvidence {
    private Long skillId;
    private Long knowledgePointId;
    private String requirementType;
    private BigDecimal mappingWeight;
    private BigDecimal mappingConfidence;
    private BigDecimal masteryValue;
    private BigDecimal stateConfidence;
    private Integer evidenceCount;
}
