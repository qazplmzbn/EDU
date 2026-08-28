package com.xyz.question_bank_management_system.modules.course.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseKnowledge {
    private Long id;
    private Long courseId;
    private Long knowledgePointId;
    private Integer sequenceNo;
    private Integer isCore;
    private BigDecimal coverageWeight;
    private LocalDateTime createdAt;
}
