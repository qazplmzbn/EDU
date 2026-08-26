package com.xyz.question_bank_management_system.modules.agent.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentTask;
import com.xyz.question_bank_management_system.modules.agent.service.AgentTaskPersistenceService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only audit trail. Raw agent evidence is deliberately never exposed to students. */
@RestController
@RequestMapping("/api/agent-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class AgentTaskController {
    private final AgentTaskPersistenceService taskService;

    @GetMapping("/{taskCode}")
    public ApiResponse<AgentTask> detail(@PathVariable String taskCode) {
        return ApiResponse.ok(taskService.requireReadable(taskCode, SecurityContextUtil.getUserId(), isAdmin()));
    }

    @GetMapping("/{taskCode}/trace")
    public ApiResponse<Map<String, Object>> trace(@PathVariable String taskCode) {
        Long userId = SecurityContextUtil.getUserId();
        boolean admin = isAdmin();
        AgentTask task = taskService.requireReadable(taskCode, userId, admin);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("steps", taskService.steps(taskCode, userId, admin));
        result.put("reviews", taskService.reviews(taskCode, userId, admin));
        result.put("decisions", taskService.decisions(taskCode, userId, admin));
        return ApiResponse.ok(result);
    }

    private boolean isAdmin() {
        return SecurityContextUtil.currentRoles().stream().anyMatch("ROLE_ADMIN"::equals);
    }
}
