package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherReviewAnswerItemVO {
    private Long answerId;
    private Long attemptId;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private Long questionId;
    private Integer questionType;
    private Integer score;
    private Integer currentFinalScore;
    private Integer needsReview;
    private LocalDateTime submittedAt;
}
