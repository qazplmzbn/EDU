package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionLlmBatchResultVO {
    private Long questionId;
    private String providerKey;
    private String providerLabel;
    private Integer requestedCount;
    private List<String> requestedModels;
}
