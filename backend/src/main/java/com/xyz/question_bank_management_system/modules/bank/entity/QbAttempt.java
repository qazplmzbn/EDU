package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbAttempt {
    private Long id;
    private Long assignmentId;
    private Long paperId;
    private Long userId;
    /** 1=assignment,2=practice */
    private Integer attemptType;
    private Integer attemptNo;
    /** 1=in_progress,2=submitted,3=grading,4=graded */
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer durationSec;
    private Integer totalScore;
    private Integer maxScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private Integer needsReview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
