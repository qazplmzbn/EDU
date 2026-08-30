package com.xyz.question_bank_management_system.modules.agent.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ResourceUnit {private Long id;private String resourceUnitCode;private Long courseId;private Long pathId;private Long pathVersionId;private String status;private String aggregationEvidenceJson;private String correlationId;private LocalDateTime createdAt;}
