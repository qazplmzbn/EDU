package com.xyz.question_bank_management_system.modules.learning.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class LearningPathVersion {private Long id;private Long pathId;private Long version;private String pathMode;private Long basedOnProfileVersion;private String graphVersion;private String policyVersion;private String modelVersion;private String status;private String changeReason;private String correlationId;private LocalDateTime createdAt;}
