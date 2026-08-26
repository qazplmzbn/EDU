package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentLearningPreference {
    private Long id;
    private Long userId;
    private String preferenceType;
    private String preferenceValue;
    private BigDecimal preferenceScore;
    private String sourceType;
    private Integer evidenceCount;
}
