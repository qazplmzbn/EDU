package com.xyz.question_bank_management_system.modules.course.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Course {
    private Long id;
    private String courseCode;
    private String courseName;
    private String description;
    private Long teacherId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
