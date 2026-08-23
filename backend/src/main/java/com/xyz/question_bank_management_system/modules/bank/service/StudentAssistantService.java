package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.modules.bank.dto.StudentAssistantChatRequest;
import com.xyz.question_bank_management_system.modules.bank.vo.StudentAssistantChatVO;

public interface StudentAssistantService {
    StudentAssistantChatVO chat(Long userId, StudentAssistantChatRequest request);
}
