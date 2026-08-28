package com.xyz.question_bank_management_system.modules.recommendation.dto;
import lombok.Data; import java.math.BigDecimal;
@Data public class ResourceFeedbackRequest { private Long recommendationId; private String feedbackType; private BigDecimal rating; private String feedbackValue; private String feedbackText; }
