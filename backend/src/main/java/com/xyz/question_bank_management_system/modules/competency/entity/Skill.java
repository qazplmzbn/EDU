package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Skill {
    private Long id;
    private String nameZh;
    private String skillType;
    private String description;
    private String sourceName;
    private String sourceRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
