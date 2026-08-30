package com.xyz.question_bank_management_system.modules.learning.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class OutboxEvent {private Long id;private String eventId;private String aggregateType;private Long aggregateId;private String eventType;private String payloadJson;private String status;private Integer retryCount;private LocalDateTime nextRetryAt;private String correlationId;private LocalDateTime createdAt;private LocalDateTime publishedAt;}
