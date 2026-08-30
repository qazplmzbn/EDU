package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentKnowledgeState {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long knowledgePointId;
    private BigDecimal masteryValue;
    private String masteryLevel;
    private BigDecimal confidence;
    private Integer evidenceCount;
    private Integer correctCount;
    private Integer attemptCount;
    private Long stateVersion;
    private String calculationMethod;
    private String algorithmVersion;
    private Long lastInteractionSeq;
    private Long lastInteractionId;
    private LocalDateTime lastEvidenceAt;
    private LocalDateTime updatedAt;
}
