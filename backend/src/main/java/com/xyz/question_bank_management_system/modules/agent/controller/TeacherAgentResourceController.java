package com.xyz.question_bank_management_system.modules.agent.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.agent.dto.TeacherAgentResourceGenerateRequest;
import com.xyz.question_bank_management_system.modules.agent.service.TeacherAgentResourceService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceGenerateVO;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/agent-resources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherAgentResourceController {

    private final TeacherAgentResourceService teacherAgentResourceService;

    @PostMapping("/generate")
    public ApiResponse<TeacherAgentResourceGenerateVO> generate(@RequestBody @Valid TeacherAgentResourceGenerateRequest request) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.generate(SecurityContextUtil.getUserId(), admin, request));
    }

    @PostMapping("/discuss")
    public ApiResponse<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> discuss(@RequestBody @Valid TeacherAgentResourceGenerateRequest request) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.discuss(SecurityContextUtil.getUserId(), admin, request));
    }

    @PostMapping("/discuss-meeting")
    public ApiResponse<List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage>> discussMeeting(@RequestBody @Valid TeacherAgentResourceGenerateRequest request) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.discussMeeting(SecurityContextUtil.getUserId(), admin, request));
    }

    @PostMapping("/generate-tasks")
    public ApiResponse<TeacherAgentResourceTaskVO> startGenerateTask(@RequestBody @Valid TeacherAgentResourceGenerateRequest request) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.startGenerateTask(SecurityContextUtil.getUserId(), admin, request));
    }

    @GetMapping("/generate-tasks/{taskId}")
    public ApiResponse<TeacherAgentResourceTaskVO> taskStatus(@PathVariable String taskId) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.getTaskStatus(SecurityContextUtil.getUserId(), admin, taskId));
    }

    @PostMapping("/generate-tasks/{taskId}/cancel")
    public ApiResponse<TeacherAgentResourceTaskVO> cancelTask(@PathVariable String taskId) {
        List<String> roles = SecurityContextUtil.currentRoles();
        boolean admin = roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        return ApiResponse.ok(teacherAgentResourceService.cancelTask(SecurityContextUtil.getUserId(), admin, taskId));
    }
}
