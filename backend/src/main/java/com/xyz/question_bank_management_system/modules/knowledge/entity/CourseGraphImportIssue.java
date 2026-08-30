package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseGraphImportIssue {
    private Long id;
    private Long importId;
    private String severity;
    private String issueCode;
    private String locationType;
    private String locationCode;
    private String message;
    private Integer resolved;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
