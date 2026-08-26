package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfileAssessmentSample {
    private Long answerId;
    private Long questionId;
    private Integer finalScore;
    private Integer maxScore;
    private Integer difficulty;
    private LocalDateTime eventTime;
}
