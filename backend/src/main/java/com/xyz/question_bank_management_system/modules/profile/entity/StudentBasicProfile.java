package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentBasicProfile {
    private Long id;
    private Long userId;
    private String studentNo;
    private String majorName;
    private String gradeName;
    private String educationLevel;
    private String learningStage;
    private BigDecimal weeklyAvailableHours;
}
