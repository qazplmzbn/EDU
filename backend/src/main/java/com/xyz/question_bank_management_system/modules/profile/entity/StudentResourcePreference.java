package com.xyz.question_bank_management_system.modules.profile.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data public class StudentResourcePreference {private Long id;private Long userId;private Long courseId;private String resourceType;private BigDecimal score;private BigDecimal confidence;private Integer evidenceCount;private LocalDateTime calculatedAt;private String algorithmVersion;}
