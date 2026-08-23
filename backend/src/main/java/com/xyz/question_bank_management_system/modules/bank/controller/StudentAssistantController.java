package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.StudentAssistantChatRequest;
import com.xyz.question_bank_management_system.modules.bank.service.StudentAssistantService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.StudentAssistantChatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/assistant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAssistantController {

    private final StudentAssistantService studentAssistantService;

    @PostMapping("/chat")
    public ApiResponse<StudentAssistantChatVO> chat(@RequestBody @Valid StudentAssistantChatRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        return ApiResponse.ok(studentAssistantService.chat(userId, request));
    }
}
