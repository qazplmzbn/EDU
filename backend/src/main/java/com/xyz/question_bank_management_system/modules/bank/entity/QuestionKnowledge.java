package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuestionKnowledge {
    private Long id;
    private Long questionId;
    private Long knowledgePointId;
    private BigDecimal weight;
    private String relationType;
    private Integer isPrimary;
    private BigDecimal confidence;
    private String sourceType;
    private LocalDateTime createdAt;
}
