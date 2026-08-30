package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeGraphVersionRelation {
    private Long id;
    private Long courseId;
    private Long graphVersionId;
    private String relationCode;
    private Long sourceKnowledgePointId;
    private Long targetKnowledgePointId;
    private String relationType;
    private BigDecimal weight;
    private BigDecimal confidence;
    private String sourceType;
    private String status;
    private LocalDateTime publishedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<Long> sourceChunkIds;
}
