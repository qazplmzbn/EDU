package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionOptionDTO {
    private Long id;
    @NotBlank(message = "选项标识不能为空")
    private String optionLabel;
    @NotBlank(message = "选项内容不能为空")
    private String optionContent;
    /** 0/1 */
    private Integer isCorrect;
    private Integer sortOrder;
}
