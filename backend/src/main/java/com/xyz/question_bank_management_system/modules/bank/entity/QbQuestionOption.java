package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

@Data
public class QbQuestionOption {
    private Long id;
    private Long questionId;
    private String optionLabel;
    private String optionContent;
    private Integer isCorrect;
    private Integer sortOrder;
}
