package com.xyz.question_bank_management_system.modules.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinClassRequest {
    @NotBlank(message = "班级邀请码不能为空")
    private String classCode;
}
