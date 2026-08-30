package com.xyz.question_bank_management_system.modules.profile.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class StudentBehaviorMetric {private Long id;private Long userId;private Long courseId;private String metricGroup;private String metricCode;private BigDecimal value;private BigDecimal confidence;private Integer evidenceCount;private LocalDateTime calculatedAt;private String algorithmVersion;}
