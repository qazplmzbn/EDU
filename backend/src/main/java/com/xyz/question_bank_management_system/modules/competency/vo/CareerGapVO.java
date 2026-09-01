package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CareerGapVO {
    private String snapshotCode;
    private Long occupationId;
    private String targetBatchCode;
    private String calculationVersion;
    private LocalDateTime calculatedAt;
    private List<Item> items;

    @Data
    public static class Item {
        private Long skillId;
        private String skillName;
        private String requirementType;
        private BigDecimal requiredLevel;
        private BigDecimal currentLevel;
        private BigDecimal currentConfidence;
        private BigDecimal gapValue;
        private BigDecimal priorityScore;
        private String gapStatus;
    }
}
