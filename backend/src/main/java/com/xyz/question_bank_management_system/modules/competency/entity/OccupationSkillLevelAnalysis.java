package com.xyz.question_bank_management_system.modules.competency.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class OccupationSkillLevelAnalysis {private Long id;private String batchCode;private Long occupationId;private Integer roundNo;private String providerKey;private String modelName;private String inputJson;private String outputJson;private String status;private String errorMessage;private Long createdBy;private LocalDateTime createdAt;}
