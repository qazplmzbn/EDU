package com.xyz.question_bank_management_system.modules.course.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentCourseProgress {
    private Long id;
    private Long userId;
    private Long courseId;
    private BigDecimal progressRate;
    private Integer completedKnowledgeCount;
    private Integer totalKnowledgeCount;
    private String status;
    private LocalDateTime lastLearningAt;
    private LocalDateTime updatedAt;
}
