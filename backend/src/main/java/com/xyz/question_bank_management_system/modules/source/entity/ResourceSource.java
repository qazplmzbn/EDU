package com.xyz.question_bank_management_system.modules.source.entity;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data public class ResourceSource { private Long id; private Long resourceId; private Long sourceChunkId; private String supportType; private BigDecimal relevanceScore; private Integer citationOrder; private LocalDateTime createdAt; }
