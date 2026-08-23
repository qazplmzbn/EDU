package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherAssignmentTargetItemVO {
    private Long studentId;
    private String username;
    private String displayName;
    private Integer completed;
    private Integer attemptCount;
    private Long latestAttemptId;
    private Integer latestAttemptStatus;
    private Integer latestTotalScore;
    private Integer latestNeedsReview;
    private LocalDateTime latestSubmittedAt;
}
