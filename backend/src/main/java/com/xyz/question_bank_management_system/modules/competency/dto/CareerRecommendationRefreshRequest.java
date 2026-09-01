package com.xyz.question_bank_management_system.modules.competency.dto;

import lombok.Data;

@Data
public class CareerRecommendationRefreshRequest {
    private Long occupationId;
    private Integer limit = 5;
}
