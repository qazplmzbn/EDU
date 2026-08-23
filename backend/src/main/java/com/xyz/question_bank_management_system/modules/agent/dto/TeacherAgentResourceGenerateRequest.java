package com.xyz.question_bank_management_system.modules.agent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class TeacherAgentResourceGenerateRequest {

    @NotNull(message = "学生 ID 不能为空")
    private Long studentId;

    private String stage;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> resourceTypes;
    private String generationScope;
    private Long classId;
    private String difficulty;
    private Integer exerciseCount;
    private String publishMode;
    private List<String> selectedWeakPoints;
    private List<String> selectedResourceTypes;
    private String providerKey;
    private Map<String, String> agentProviderKeys;
    private String agentId;
    private List<Map<String, Object>> discussionMessages;
    private String teacherRequirement;
    private String feedback;
}
