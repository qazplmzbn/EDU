package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OccupationAlias {
    private Long id;
    private Long occupationId;
    private String aliasName;
    private String aliasType;
    private LocalDateTime createdAt;
}
