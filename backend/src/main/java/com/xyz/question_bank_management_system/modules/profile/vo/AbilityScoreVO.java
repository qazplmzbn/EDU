package com.xyz.question_bank_management_system.modules.profile.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** Compatibility response for GET /api/stats/ability, backed by student_ability_state. */
@Data
public class AbilityScoreVO {
    private Long userId;
    private Integer abilityScore;
    private LocalDateTime updatedAt;
}
