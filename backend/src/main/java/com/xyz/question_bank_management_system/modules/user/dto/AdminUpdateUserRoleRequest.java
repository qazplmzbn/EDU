package com.xyz.question_bank_management_system.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateUserRoleRequest {
    @NotBlank(message = "角色不能为空")
    private String role;
}
