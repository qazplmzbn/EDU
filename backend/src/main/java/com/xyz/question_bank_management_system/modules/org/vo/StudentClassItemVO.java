package com.xyz.question_bank_management_system.modules.org.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentClassItemVO {
    private Long id;
    private String className;
    private String classCode;
    private String classDesc;
    private Long teacherId;
    private String teacherName;
    private LocalDateTime joinedAt;
}
