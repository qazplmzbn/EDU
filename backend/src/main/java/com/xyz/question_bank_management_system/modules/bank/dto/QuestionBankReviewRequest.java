package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionBankReviewRequest {
    /** 2=approved, 3=rejected */
    @NotNull(message = "审核状态不能为空")
    private Integer reviewStatus;

    private String reviewComment;
}
