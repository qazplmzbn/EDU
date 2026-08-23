package com.xyz.question_bank_management_system.modules.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassCreateRequest {
    @NotBlank(message = "班级名称不能为空")
    private String className;

    private String classDesc;

    private String classCode;

    private Long teacherId;
}
