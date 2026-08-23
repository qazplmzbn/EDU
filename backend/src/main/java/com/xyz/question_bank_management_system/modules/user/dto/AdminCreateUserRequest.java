package com.xyz.question_bank_management_system.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String displayName;
    private String email;

    /** 1=active,0=disabled */
    private Integer status = 1;

    @NotBlank(message = "角色不能为空")
    private String role;
}
