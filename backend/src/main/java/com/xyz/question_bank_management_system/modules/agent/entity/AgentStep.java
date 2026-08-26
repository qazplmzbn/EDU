package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentStep {
    private Long id;
    private Long agentTaskId;
    private Integer stepNo;
    private Long agentDefinitionId;
    private String stepType;
    private String inputJson;
    private String outputJson;
    private Long llmCallId;
    private String status;
    private Integer latencyMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
