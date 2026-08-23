package com.xyz.question_bank_management_system.modules.org.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassStudentItemVO {
    private Long studentId;
    private String username;
    private String displayName;
    private String email;
    private LocalDateTime joinedAt;
}
