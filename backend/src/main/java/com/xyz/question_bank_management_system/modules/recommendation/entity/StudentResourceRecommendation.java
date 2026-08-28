package com.xyz.question_bank_management_system.modules.recommendation.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentResourceRecommendation {
    private Long id; private Long userId; private Long resourceId; private Long goalId; private Long knowledgePointId; private Long skillId; private Long profileSnapshotId; private Long pathId;
    private String recommendationType; private Integer rankNo; private BigDecimal recommendScore; private BigDecimal difficultyMatchScore; private String reasonText; private String status; private LocalDateTime recommendedAt; private LocalDateTime updatedAt;
}
