package com.xyz.question_bank_management_system.modules.recommendation.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResourceFeedback { private Long id; private Long userId; private Long resourceId; private Long recommendationId; private String feedbackType; private BigDecimal rating; private String feedbackValue; private String feedbackText; private LocalDateTime createdAt; }
