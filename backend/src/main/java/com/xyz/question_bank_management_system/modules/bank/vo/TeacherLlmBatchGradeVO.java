package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeacherLlmBatchGradeVO {
    private int successCount;
    private int failureCount;
    private List<ItemVO> items = new ArrayList<>();

    @Data
    public static class ItemVO {
        private Long answerId;
        private Boolean success;
        private String message;
        private Integer finalScore;
        private List<Long> llmCallIds = new ArrayList<>();
    }
}
