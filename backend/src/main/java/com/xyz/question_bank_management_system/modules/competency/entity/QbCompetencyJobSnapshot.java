package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbCompetencyJobSnapshot {
    private Long id;
    private String sourcePlatform;
    private String sourceUrl;
    private String sourceJobKey;
    private String title;
    private String dimension;
    private String skill;
    private String location;
    private String salary;
    private String experience;
    private String education;
    private String company;
    private String description;
    private String tagsJson;
    private LocalDateTime sourceUpdatedAt;
    private LocalDateTime lastSeenAt;
    private String availabilityStatus;
    private Long syncVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
