package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentProfileSummary {
    private Long userId;
    private BigDecimal overallKnowledgeMastery;
    private BigDecimal abilityAverageScore;
    private BigDecimal assessmentAccuracy;
    private BigDecimal learningActivityScore;
    private Integer weakKnowledgeCount;
    private Integer weakSkillCount;
    private Integer recommendedDifficulty;
    private Long lastProfileSnapshotId;
    private LocalDateTime updatedAt;
}
