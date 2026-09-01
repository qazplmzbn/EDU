package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentOccupationSkillGap {
    private Long id;
    private String snapshotCode;
    private Long userId;
    private Long occupationId;
    private Long occupationSkillId;
    private Long skillId;
    private String skillName;
    private String requirementType;
    private BigDecimal importanceScore;
    private BigDecimal requiredLevel;
    private BigDecimal currentLevel;
    private BigDecimal currentConfidence;
    private BigDecimal gapValue;
    private BigDecimal priorityScore;
    private String gapStatus;
    private String targetBatchCode;
    private String calculationVersion;
    private LocalDateTime calculatedAt;
}
