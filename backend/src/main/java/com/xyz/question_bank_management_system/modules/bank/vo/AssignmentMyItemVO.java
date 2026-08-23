package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentMyItemVO {
    private Long assignmentId;
    private String assignmentTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer timeLimitMin;
    private Integer maxAttempts;
    private Long myAttemptCount;
    private Integer publishStatus;
}
