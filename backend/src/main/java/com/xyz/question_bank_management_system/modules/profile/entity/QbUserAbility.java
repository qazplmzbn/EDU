package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbUserAbility {
    private Long userId;
    private Integer abilityScore;
    private LocalDateTime updatedAt;
}
