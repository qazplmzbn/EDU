package com.xyz.question_bank_management_system.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAuditLogItemVO {
    private Long logId;
    private String username;
    private String action;
    private String operationLabel;
    private String entityType;
    private Long entityId;
    private String beforeJson;
    private String afterJson;
    private String ipAddr;
    private LocalDateTime createdAt;
}
