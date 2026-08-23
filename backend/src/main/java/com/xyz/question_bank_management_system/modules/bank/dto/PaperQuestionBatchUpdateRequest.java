package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PaperQuestionBatchUpdateRequest {
    @Valid
    @NotEmpty(message = "试卷题目列表不能为空")
    private List<PaperQuestionBatchUpdateItem> questions;
}
