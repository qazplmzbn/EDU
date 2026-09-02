package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentReview {
    private Long id;
    private Long agentTaskId;
    private Long agentStepId;
    private String targetType;
    private Long targetId;
    private BigDecimal factualScore;
    private BigDecimal coverageScore;
    private BigDecimal difficultyMatchScore;
    private BigDecimal hallucinationRate;
    private BigDecimal sourceConsistencyScore;
    private String reviewStatus;
    private String reviewReport;
    private Long bundleId;
    private Long blueprintId;
    private String reviewDimension;
    private String issueCode;
    private String repairTarget;
    private String repairScope;
    private String repairAction;
    private String repairInstruction;
    private Integer roundNo;
    private String evidenceRefsJson;
    private String toolResultJson;
    private String reviewerRole;
    private LocalDateTime createdAt;
}
