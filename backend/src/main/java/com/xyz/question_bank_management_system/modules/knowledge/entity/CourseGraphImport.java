package com.xyz.question_bank_management_system.modules.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseGraphImport {
    private Long id;
    private String importCode;
    private String idempotencyKey;
    private String courseCode;
    private String courseName;
    private String schemaVersion;
    private String mode;
    private String sourceFileName;
    private String sourceFileHash;
    private String normalizedHash;
    private String validationHash;
    private String status;
    private Long courseId;
    private Long graphVersionId;
    private Integer nodeCount;
    private Integer moduleCount;
    private Integer categoryCount;
    private Integer knowledgePointCount;
    private Integer containsCount;
    private Integer prerequisiteCount;
    private Integer similarCount;
    private Integer errorCount;
    private Integer warningCount;
    private Long createdBy;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String correlationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
