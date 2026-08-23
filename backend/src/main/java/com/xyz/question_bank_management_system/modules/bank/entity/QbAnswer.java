package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbAnswer {
    private Long id;
    private Long attemptId;
    private Long attemptQuestionId;
    private Long questionId;
    private Long userId;
    private String answerContent;
    private Integer answerFormat;
    /** 1=draft,2=submitted */
    private Integer answerStatus;
    private Integer autoScore;
    private Integer finalScore;
    private Integer isCorrect;
    private LocalDateTime answeredAt;
    private LocalDateTime gradedAt;
}
