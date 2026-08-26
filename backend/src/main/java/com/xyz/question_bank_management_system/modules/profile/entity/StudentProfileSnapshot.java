package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentProfileSnapshot {
    private Long id;
    private Long userId;
    private String basicStateJson;
    private String knowledgeStateJson;
    private String skillStateJson;
    private String abilityStateJson;
    private String preferenceStateJson;
    private String goalStateJson;
    private String categoryStatJson;
    private String profileSummary;
    private String triggerType;
    private Long triggerId;
    private Integer evidenceCount;
    private LocalDateTime createdAt;
}
