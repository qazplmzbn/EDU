package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CareerSkillRequirement {
    private Long occupationSkillId;
    private Long occupationId;
    private Long skillId;
    private String skillName;
    private String requirementType;
    private BigDecimal importanceScore;
    private BigDecimal requiredLevel;
    private String publishedBatchCode;
    private String requiredLevelVersion;
}
