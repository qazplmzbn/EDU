package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherAssignmentScoreItemVO {
    private Long studentId;
    private String displayName;
    private Long attemptId;
    private Integer totalScore;
    private Integer needsReview;
    private LocalDateTime submittedAt;
}
