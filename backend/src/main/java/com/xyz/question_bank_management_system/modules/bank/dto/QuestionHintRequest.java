package com.xyz.question_bank_management_system.modules.bank.dto;

import lombok.Data;

@Data
public class QuestionHintRequest {
    private String question;
    private String currentAnswer;
    private String providerKey;
}
