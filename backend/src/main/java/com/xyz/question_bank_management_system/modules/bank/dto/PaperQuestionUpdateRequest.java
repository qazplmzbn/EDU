package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperQuestionUpdateRequest {
    @NotNull(message = "题目顺序不能为空")
    private Integer orderNo;
    @NotNull(message = "题目分值不能为空")
    private Integer score;
}
