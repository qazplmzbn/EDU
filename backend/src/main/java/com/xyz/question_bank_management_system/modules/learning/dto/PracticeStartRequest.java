package com.xyz.question_bank_management_system.modules.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PracticeStartRequest {

    @NotBlank(message = "练习模式不能为空")
    private String mode;

    @Valid
    private Scope scope;

    private Integer totalScore = 100;
    private Long ruleId;

    @Data
    public static class Scope {
        private List<Long> knowledgePointIds;
        private List<String> chapters;
        private List<Integer> questionTypes;
        private List<Long> questionIds;
    }
}
