package com.xyz.question_bank_management_system.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminLoginLogItemVO {
    private Long logId;
    private String username;
    private Integer successFlag;
    private String ipAddr;
    private LocalDateTime loginAt;
}
