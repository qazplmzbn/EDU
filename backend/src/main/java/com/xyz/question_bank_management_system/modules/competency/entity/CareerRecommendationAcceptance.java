package com.xyz.question_bank_management_system.modules.competency.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class CareerRecommendationAcceptance { private Long id; private Long snapshotId; private Long userId; private Long courseId; private String learningPathCode; private LocalDateTime acceptedAt; private String status; }
