package com.xyz.question_bank_management_system.modules.source.entity;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data public class KnowledgeSource { private Long id; private Long knowledgePointId; private Long sourceChunkId; private String supportType; private BigDecimal relevanceScore; private BigDecimal confidence; private String linkMethod; private String reviewStatus; private Long reviewedBy; private LocalDateTime createdAt; }
