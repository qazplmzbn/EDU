package com.xyz.question_bank_management_system.modules.agent.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.agent.entity.AgentDefinition;
import com.xyz.question_bank_management_system.modules.agent.mapper.AgentDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administrator-only, versioned Agent-definition management. */
@RestController
@RequestMapping("/api/admin/agents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentDefinitionController {
    private final AgentDefinitionMapper definitionMapper;

    @GetMapping
    public ApiResponse<List<AgentDefinition>> list() { return ApiResponse.ok(definitionMapper.selectAll()); }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody AgentDefinition request) {
        validate(request);
        if (request.getAgentCode() == null || request.getAgentCode().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "Agent code is required");
        }
        request.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        request.setVersion(request.getVersion() == null || request.getVersion().isBlank() ? "v1" : request.getVersion());
        definitionMapper.insert(request);
        return ApiResponse.ok(request.getId());
    }

    @PutMapping("/{id}")
    public ApiResponse<Long> version(@PathVariable Long id, @RequestBody AgentDefinition request) {
        AgentDefinition previous = definitionMapper.selectById(id);
        if (previous == null) throw BizException.of(ErrorCode.NOT_FOUND, "Agent definition not found");
        validate(request);
        definitionMapper.disable(id);
        request.setId(null);
        request.setAgentCode(previous.getAgentCode());
        request.setVersion(nextVersion(previous.getVersion()));
        request.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        definitionMapper.insert(request);
        return ApiResponse.ok(request.getId());
    }

    private void validate(AgentDefinition row) {
        if (row == null || row.getAgentName() == null || row.getAgentName().isBlank() || row.getRoleType() == null || row.getRoleType().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "Agent name and role type are required");
        }
    }
    private String nextVersion(String version) {
        try { return "v" + (Integer.parseInt(String.valueOf(version).replaceFirst("^[vV]", "")) + 1); }
        catch (Exception ignored) { return "v2"; }
    }
}
