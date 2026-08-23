package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperQuestionBatchUpdateItem {
    @NotNull(message = "试卷题目编号不能为空")
    private Long id;

    @NotNull(message = "题目顺序不能为空")
    @Min(value = 1, message = "题目顺序必须大于等于 1")
    private Integer orderNo;

    @NotNull(message = "题目分值不能为空")
    @Min(value = 0, message = "题目分值必须大于等于 0")
    private Integer score;
}
