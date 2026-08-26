package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentDefinition {
    private Long id;
    private String agentCode;
    private String agentName;
    private String roleType;
    private String description;
    private Long defaultModelConfigId;
    private Long promptTemplateId;
    private String configJson;
    private Integer status;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
