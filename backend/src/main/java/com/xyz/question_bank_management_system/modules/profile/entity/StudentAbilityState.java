package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentAbilityState {
    private Long id;
    private Long userId;
    private Long dimensionId;
    private String dimensionCode;
    private String dimensionName;
    private BigDecimal score;
    private String level;
    private BigDecimal confidence;
    private Integer evidenceCount;
    private LocalDateTime updatedAt;
}
