package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OccupationSkill {
    private Long id;
    private Long occupationId;
    private Long skillId;
    private String requirementType;
    private BigDecimal importanceScore;
    private BigDecimal requiredLevel;
    private String sourceRef;
    private LocalDateTime createdAt;
}
