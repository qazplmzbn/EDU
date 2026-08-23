package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class AppealHandleRequest {

    @NotBlank(message = "处理动作不能为空")
    private String action;

    @PositiveOrZero(message = "最终分数不能小于 0")
    private Integer finalScore;

    private String decisionComment;
}
