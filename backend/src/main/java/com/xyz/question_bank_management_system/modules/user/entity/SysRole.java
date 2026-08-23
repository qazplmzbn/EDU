package com.xyz.question_bank_management_system.modules.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
