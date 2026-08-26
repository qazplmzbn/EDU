package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentEvidence {
    private Long id;
    private Long userId;
    private String evidenceType;
    private String sourceEntityType;
    private Long sourceEntityId;
    private String targetType;
    private Long targetId;
    private BigDecimal evidenceValue;
    private Integer evidenceDirection;
    private BigDecimal confidence;
    private String evidenceText;
    private LocalDateTime occurredAt;
    private String extractVersion;
}
