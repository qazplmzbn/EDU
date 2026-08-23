package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AppealCreateRequest {

    @NotNull(message = "答案编号不能为空")
    private Long answerId;

    @NotBlank(message = "申诉原因不能为空")
    private String reasonText;

    private List<String> attachments;
}
