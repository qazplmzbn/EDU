package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkillKnowledge {
    private Long id;
    private Long skillId;
    private Long knowledgePointId;
    private String requirementType;
    private BigDecimal weight;
    private BigDecimal confidence;
    private String sourceType;
    private String sourceRef;
    private String evidenceText;
    private LocalDateTime createdAt;
}
