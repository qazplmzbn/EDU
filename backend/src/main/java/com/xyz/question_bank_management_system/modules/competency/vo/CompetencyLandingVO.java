package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;

import java.util.List;

public final class CompetencyLandingVO {

    private CompetencyLandingVO() {
    }

    @Data
    public static class CompetencyLayerResponse {
        private List<CompetencyDimensionDto> dimensions;
        private List<CompetencyJobDto> jobs;
        private SyncMeta syncMeta;
    }

    @Data
    public static class CompetencyDimensionDto {
        private String name;
        private Integer count;
        private String color;
        private String desc;
        private List<DetailItem> details;
    }

    @Data
    public static class DetailItem {
        private String title;
        private String text;
    }

    @Data
    public static class CompetencyJobDto {
        private Long id;
        private String title;
        private String dimension;
        private String skill;
        private String location;
        private String salary;
        private String experience;
        private String education;
        private String company;
        private String description;
        private List<String> tags;
        private String example;
        private String sourcePlatform;
        private String sourceUrl;
        private String sourceUpdatedAt;
        private String availabilityStatus;
        private String sourceNote;
    }

    @Data
    public static class SyncMeta {
        private String platform;
        private String lastSuccessAt;
        private String lastAttemptAt;
        private String status;
        private Integer jobCount;
    }

    @Data
    public static class SyncResult {
        private String status;
        private String message;
        private String startedAt;
        private String finishedAt;
        private Integer fetchedCandidateCount;
        private Integer successCount;
        private Integer failureCount;
        private Integer offlineCount;
    }

    @Data
    public static class SyncRecordItem {
        private Long id;
        private String triggerType;
        private String status;
        private String startedAt;
        private String finishedAt;
        private Integer fetchedCandidateCount;
        private Integer successCount;
        private Integer failureCount;
        private Integer offlineCount;
        private String errorMessage;
    }
}
