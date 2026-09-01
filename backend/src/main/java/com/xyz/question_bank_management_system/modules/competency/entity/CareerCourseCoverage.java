package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CareerCourseCoverage {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long skillId;
    private Long knowledgePointId;
    private Integer courseCore;
    private String mappingType;
    private BigDecimal coverageWeight;
}
