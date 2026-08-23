package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class TeacherLlmBatchRequest {
    private List<Long> answerIds;
    private Long assignmentId;
    private Boolean needsReview = true;
    private String providerKey;
    private String modelName;
    private Double temperature;

    @Min(value = 1, message = "批改次数不能小于 1")
    @Max(value = 5, message = "批改次数不能大于 5")
    private Integer times = 1;
}
