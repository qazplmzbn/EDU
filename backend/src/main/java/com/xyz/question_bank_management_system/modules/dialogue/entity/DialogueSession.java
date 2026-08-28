package com.xyz.question_bank_management_system.modules.dialogue.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class DialogueSession { private Long id; private Long userId; private String title; private String sessionType; private String status; private Long targetGoalId; private LocalDateTime startedAt; private LocalDateTime lastMessageAt; private LocalDateTime createdAt; private LocalDateTime updatedAt; }
