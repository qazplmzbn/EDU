package com.xyz.question_bank_management_system.modules.learning.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLearningResourceTarget {
    private Long id;
    private Long resourceId;
    private Long studentId;
    private Long classId;
    private String targetType;
    private Long createdBy;
    private LocalDateTime createdAt;
}
