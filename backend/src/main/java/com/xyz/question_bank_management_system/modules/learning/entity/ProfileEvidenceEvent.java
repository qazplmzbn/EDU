package com.xyz.question_bank_management_system.modules.learning.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ProfileEvidenceEvent {private Long id;private String eventId;private Long interactionId;private String consumerName;private String status;private Integer retryCount;private LocalDateTime processedAt;private String errorMessage;}
