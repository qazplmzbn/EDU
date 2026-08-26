package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentTask {
    private Long id;
    private String taskCode;
    private String taskType;
    private Long userId;
    private Long teacherId;
    private String targetType;
    private Long targetId;
    private String inputJson;
    private String status;
    private Integer currentStepNo;
    private String resultSummary;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
