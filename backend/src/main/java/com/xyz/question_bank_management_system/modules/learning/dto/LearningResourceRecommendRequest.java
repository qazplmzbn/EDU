package com.xyz.question_bank_management_system.modules.learning.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningResourceRecommendRequest {
    private String targetType;
    private Long classId;
    private List<Long> studentIds = new ArrayList<>();
}
