package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CareerStudentSkillState {
    private Long userId;
    private Long skillId;
    private BigDecimal proficiencyValue;
    private BigDecimal coreProficiencyValue;
    private String proficiencyLevel;
    private BigDecimal confidence;
    private BigDecimal knowledgeCoverageRate;
    private Integer evidenceCount;
    private String calculationVersion;
}
