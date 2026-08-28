package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

@Data
public class StudentAssistantChatVO {
    private String reply;
    private Long sessionId;
    private Long llmCallId;
    private Boolean contextUsed;
    private String lockedReason;
}
