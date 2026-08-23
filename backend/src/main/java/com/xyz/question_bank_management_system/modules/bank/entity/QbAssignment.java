package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbAssignment {
    private Long id;
    private Long paperId;
    private String assignmentTitle;
    private String assignmentDesc;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer timeLimitMin;
    private Integer maxAttempts;
    private Integer shuffleQuestions;
    private Integer shuffleOptions;
    /** 1=draft,2=published,3=closed */
    private Integer publishStatus;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
