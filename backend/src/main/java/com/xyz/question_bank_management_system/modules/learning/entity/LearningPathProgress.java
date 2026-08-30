package com.xyz.question_bank_management_system.modules.learning.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class LearningPathProgress {private Long id;private Long pathId;private Long lastProcessedInteractionSeq;private Long lastProcessedInteractionId;private Integer consecutiveWrongCount;private Integer windowAttemptCount;private BigDecimal windowMasteryStart;private LocalDateTime updatedAt;}
