package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbAssignmentTarget {
    private Long id;
    private Long assignmentId;
    private String targetType;
    private Long studentId;
    private Long classId;
    private LocalDateTime createdAt;
}
