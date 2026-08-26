package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentDecision {
    private Long id;
    private Long agentTaskId;
    private Long agentStepId;
    private String decisionType;
    private String targetType;
    private Long targetId;
    private String decisionValue;
    private String decisionReason;
    private BigDecimal confidence;
    private String evidenceJson;
    private LocalDateTime createdAt;
}
