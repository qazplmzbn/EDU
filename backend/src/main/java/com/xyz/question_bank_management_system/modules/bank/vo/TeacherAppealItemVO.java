package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherAppealItemVO {
    private Long appealId;
    private Long answerId;
    private Long studentId;
    private Long assignmentId;
    private String assignmentTitle;
    private String reasonText;
    private Integer appealStatus;
    private LocalDateTime createdAt;
}
