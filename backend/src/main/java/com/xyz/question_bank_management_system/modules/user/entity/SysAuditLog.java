package com.xyz.question_bank_management_system.modules.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysAuditLog {
    private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String beforeJson;
    private String afterJson;
    private String ipAddr;
    private LocalDateTime createdAt;
}
