package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StageEvaluation {
    private Long id;
    private Long userId;
    private String stageType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long profileSnapshotId;
    private BigDecimal overallScore;
    private String dimensionScoresJson;
    private String evaluationText;
    private String evaluatorType;
    private String status;
    private LocalDateTime createdAt;
}
