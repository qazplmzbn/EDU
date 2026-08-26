package com.xyz.question_bank_management_system.modules.profile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class StageEvaluationGenerateRequest {
    @NotNull(message = "学生不能为空")
    private Long studentId;
    private String stage = "month";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endDate;
}
