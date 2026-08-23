package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperUpsertRequest {
    @NotBlank(message = "试卷标题不能为空")
    private String paperTitle;

    private String paperDesc;

    /** paper type: 1=assignment, 2=paper */
    @NotNull(message = "试卷类型不能为空")
    private Integer paperType;
}

