package com.xyz.question_bank_management_system.modules.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String email;
    /** 1=active,0=disabled */
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
