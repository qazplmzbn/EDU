package com.xyz.question_bank_management_system.modules.learning.dto;

import lombok.Data;
import java.util.List;

@Data
public class LearningResourceUpsertRequest {
    private String title;
    private String resourceType;
    private String resourcePurpose;
    private String url;
    private String summary;
    private String content;
    private Integer difficulty;
    private String generationType;
    private String version;
    private String personalizationBasis;
    private String reviewReportJson;
    private String modelSourceJson;
    private String auditStatus;
    private Long agentTaskId;
    private List<Long> knowledgePointIds;
}
