package com.xyz.question_bank_management_system.modules.agent.vo;

import lombok.Data;

@Data
public class TeacherAgentResourceTaskVO {

    private String taskId;
    private String status;
    private String message;
    private Long teacherId;
    private Long studentId;
    private String runId;
    private String startedAt;
    private String updatedAt;
    private String finishedAt;
    private TeacherAgentResourceGenerateVO result;
}
