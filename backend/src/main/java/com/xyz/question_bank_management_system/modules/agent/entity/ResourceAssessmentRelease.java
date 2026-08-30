package com.xyz.question_bank_management_system.modules.agent.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ResourceAssessmentRelease {private Long id;private String releaseCode;private Long userId;private Long bundleId;private Long resourceItemId;private String status;private LocalDateTime releasedAt;private LocalDateTime expiresAt;private LocalDateTime consumedAt;private LocalDateTime createdAt;}
