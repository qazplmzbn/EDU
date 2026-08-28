package com.xyz.question_bank_management_system.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CoursePathGenerateRequest {
    @Min(value = 1, message = "规划天数至少为 1")
    @Max(value = 365, message = "规划天数不能超过 365")
    private Integer planningDays;
    private String stage;
}
