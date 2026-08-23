package com.xyz.question_bank_management_system.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserListItemVO {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private String role;
}
