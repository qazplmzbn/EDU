package com.xyz.question_bank_management_system.modules.profile.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class KnowledgeStateUpdateLog {private Long id;private Long userId;private Long courseId;private Long knowledgePointId;private Long interactionId;private String evidenceScope;private BigDecimal previousMastery;private BigDecimal newMastery;private BigDecimal previousConfidence;private BigDecimal newConfidence;private String modelVersion;private Long profileVersionBefore;private Long profileVersionAfter;private String correlationId;private LocalDateTime createdAt;}
