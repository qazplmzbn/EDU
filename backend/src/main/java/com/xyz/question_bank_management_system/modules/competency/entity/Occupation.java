package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Occupation {
    private Long id;
    private String nameZh;
    private String nameEn;
    private String categoryCode;
    private String description;
    private String sourceName;
    private String sourceRef;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
