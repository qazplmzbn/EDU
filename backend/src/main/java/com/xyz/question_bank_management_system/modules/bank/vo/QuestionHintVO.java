package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionHintVO {
    private Long llmCallId;
    private String hint;
    private List<String> contextSources;
}
