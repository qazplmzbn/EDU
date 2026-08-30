package com.xyz.question_bank_management_system.modules.learning.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class PathUpdateLog {private Long id;private Long pathId;private Long oldVersion;private Long newVersion;private String triggerEventTypesJson;private String triggerInteractionIdsJson;private String affectedKnowledgePointIdsJson;private String addedStepIdsJson;private String removedStepIdsJson;private String retainedStepIdsJson;private String correlationId;private LocalDateTime createdAt;}
