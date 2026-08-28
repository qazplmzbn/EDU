package com.xyz.question_bank_management_system.modules.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CourseKnowledgeReplaceRequest {
    @NotEmpty(message = "课程知识点不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "知识点不能为空")
        private Long knowledgePointId;
        @NotNull(message = "顺序不能为空")
        private Integer sequenceNo;
        private Boolean core;
        private BigDecimal coverageWeight;
    }
}
