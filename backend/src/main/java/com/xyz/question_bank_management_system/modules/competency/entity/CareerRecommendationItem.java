package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CareerRecommendationItem {
    private Long id;
    private Long snapshotId;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer rankNo;
    private BigDecimal courseScore;
    private BigDecimal coverageScore;
    private BigDecimal coreCoverageRate;
    private String reasonJson;
    private String coveredSkillIdsJson;
    private String coveredKnowledgePointIdsJson;
    private String fallbackType;
}
