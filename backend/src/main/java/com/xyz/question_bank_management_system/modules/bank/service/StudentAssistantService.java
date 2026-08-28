package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.modules.bank.dto.StudentAssistantChatRequest;
import com.xyz.question_bank_management_system.modules.bank.vo.StudentAssistantChatVO;
import com.xyz.question_bank_management_system.modules.dialogue.entity.DialogueMessage;
import com.xyz.question_bank_management_system.modules.dialogue.entity.DialogueSession;
import java.util.List;

public interface StudentAssistantService {
    StudentAssistantChatVO chat(Long userId, StudentAssistantChatRequest request);
    List<DialogueSession> sessions(Long userId);
    List<DialogueMessage> messages(Long sessionId, Long userId);
}
