package com.xyz.question_bank_management_system.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseUpsertRequest {
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String description;
    private Long teacherId;
    private String status;
}
