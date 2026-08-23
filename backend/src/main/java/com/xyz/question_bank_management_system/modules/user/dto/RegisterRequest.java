package com.xyz.question_bank_management_system.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d])\\S{8,20}$",
            message = "密码必须为 8 到 20 位，且同时包含字母、数字和特殊字符"
    )
    private String password;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String displayName;

    private String email;
}
