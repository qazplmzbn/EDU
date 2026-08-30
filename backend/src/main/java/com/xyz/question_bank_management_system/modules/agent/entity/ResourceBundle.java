package com.xyz.question_bank_management_system.modules.agent.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ResourceBundle {private Long id;private String bundleCode;private Long userId;private Long courseId;private Long resourceUnitId;private Long blueprintId;private Long version;private String status;private Long profileVersionUsed;private String graphVersion;private String policyVersion;private String contentHash;private LocalDateTime publishedAt;private String staleReason;private String correlationId;private LocalDateTime createdAt;}
