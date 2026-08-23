package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TeacherLlmRetryRequest {

    private String providerKey;
    private String modelName;
    private Double temperature;

    @Min(value = 1, message = "重试次数不能小于 1")
    @Max(value = 5, message = "重试次数不能大于 5")
    private Integer times = 1;
}
