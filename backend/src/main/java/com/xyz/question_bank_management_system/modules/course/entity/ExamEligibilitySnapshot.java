package com.xyz.question_bank_management_system.modules.course.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ExamEligibilitySnapshot {private Long id;private String snapshotCode;private Long userId;private Long goalId;private Integer eligible;private String courseVersionsJson;private String profileVersionsJson;private String ruleVersion;private String resultJson;private String correlationId;private LocalDateTime calculatedAt;private LocalDateTime createdAt;}
