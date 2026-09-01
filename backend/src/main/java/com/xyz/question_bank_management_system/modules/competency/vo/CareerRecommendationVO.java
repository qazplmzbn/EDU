package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CareerRecommendationVO {
    private String snapshotCode;
    private String gapSnapshotCode;
    private Long occupationId;
    private String targetBatchCode;
    private String algorithmVersion;
    private String dataStatus;
    private LocalDateTime createdAt;
    private List<Item> items;

    @Data
    public static class Item {
        private Long courseId;
        private String courseCode;
        private String courseName;
        private Integer rankNo;
        private BigDecimal courseScore;
        private BigDecimal coverageScore;
        private BigDecimal coreCoverageRate;
        private List<Long> coveredSkillIds;
        private List<Long> coveredKnowledgePointIds;
        private Map<String, Object> reason;
        private String fallbackType;
    }
}
