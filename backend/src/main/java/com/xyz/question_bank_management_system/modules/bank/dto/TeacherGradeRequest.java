package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherGradeRequest {

    @NotNull(message = "分数不能为空")
    @Min(value = 0, message = "分数不能小于 0")
    private Integer score;

    private String comment;
}
