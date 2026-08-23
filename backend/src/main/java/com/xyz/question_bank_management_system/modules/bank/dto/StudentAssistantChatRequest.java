package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StudentAssistantChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;
    private List<ChatMessage> history;
    private Map<String, Object> pageContext;
    private String providerKey;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
