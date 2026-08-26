package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StudentLearningGoal {
    private Long id;
    private Long userId;
    private String goalType;
    private Long targetOccupationId;
    private Long targetSkillId;
    private Long targetKnowledgePointId;
    private String goalDescription;
    private BigDecimal targetLevel;
    private LocalDate expectedCompletionDate;
    private BigDecimal weeklyAvailableHours;
    private Integer priority;
    private String status;
    private String sourceType;
}
