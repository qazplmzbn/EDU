package com.xyz.question_bank_management_system.modules.dialogue.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class DialogueMessage { private Long id; private Long sessionId; private Long userId; private String role; private String content; private Long replyToMessageId; private Long llmCallId; private Integer profileExtracted; private LocalDateTime createdAt; }
