package com.xyz.question_bank_management_system.modules.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysLoginLog {
    private Long id;
    private Long userId;
    private String username;
    private Integer successFlag;
    private String failReason;
    private String ipAddr;
    private String userAgent;
    private LocalDateTime loginAt;
}
