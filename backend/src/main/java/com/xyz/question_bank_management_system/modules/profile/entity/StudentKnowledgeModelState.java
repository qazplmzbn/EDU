package com.xyz.question_bank_management_system.modules.profile.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class StudentKnowledgeModelState {private Long id;private Long userId;private Long courseId;private String modelVersion;private String knowledgeIndexVersion;private String stateRef;private Long processedThroughSeq;private String status;private LocalDateTime updatedAt;}
