package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TagCreateRequest {
    @NotBlank(message = "标签名不能为空")
    private String tagName;
    private String tagCode;
    private Long parentId;
    private Integer tagLevel;
    /** 1=knowledge,2=chapter,3=custom */
    @NotNull(message = "标签类型不能为空")
    private Integer tagType;
    private Integer sortOrder;
}
