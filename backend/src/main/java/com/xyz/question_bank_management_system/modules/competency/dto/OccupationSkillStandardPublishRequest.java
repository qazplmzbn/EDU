package com.xyz.question_bank_management_system.modules.competency.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OccupationSkillStandardPublishRequest {
    @NotBlank
    private String batchCode;
    private String levelVersion = "job_skill_level_v1";
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long skillId;
        @NotNull
        private BigDecimal requiredLevel;
        private BigDecimal importanceScore = BigDecimal.ONE;
        private String requirementType = "essential";
        private String source = "MANUAL";
    }
}
