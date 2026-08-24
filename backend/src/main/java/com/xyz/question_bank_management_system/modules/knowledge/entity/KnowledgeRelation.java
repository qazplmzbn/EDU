package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KnowledgeRelation {
    private Long id;
    private Long sourceId;
    private Long targetId;
    private String relationType;
    private BigDecimal weight;
    private BigDecimal confidence;
    private String sourceType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
