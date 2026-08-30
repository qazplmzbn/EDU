package com.xyz.question_bank_management_system.modules.knowledge.vo;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePointLegacyMapping;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CourseGraphImportDetailVO {
    private String importCode;
    private String status;
    private String courseCode;
    private String courseName;
    private String schemaVersion;
    private String mode;
    private String sourceFileName;
    private String sourceFileHash;
    private String normalizedHash;
    private String validationHash;
    private Long courseId;
    private String graphVersionCode;
    private CourseGraphValidationVO.Counts counts;
    private List<CourseGraphValidationVO.Issue> errors = new ArrayList<>();
    private List<CourseGraphValidationVO.Issue> warnings = new ArrayList<>();
    private List<KnowledgePointLegacyMapping> legacyMappings = new ArrayList<>();
    private Long createdBy;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
