package com.xyz.question_bank_management_system.modules.learning.vo;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import lombok.Data;

import java.util.List;

@Data
public class PersonalizedPracticePlanVO {
    private List<QbKnowledgePoint> weakPoints;
    private List<Long> tagIds;
    private Integer totalScore;
    private String mode;
    private String reason;
}
