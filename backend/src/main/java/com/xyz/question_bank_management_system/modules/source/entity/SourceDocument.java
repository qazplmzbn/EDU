package com.xyz.question_bank_management_system.modules.source.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class SourceDocument { private Long id; private String title; private String documentType; private String authorOrg; private String sourceUrl; private Long fileAssetId; private String version; private LocalDateTime publishedAt; private Integer authorityLevel; private String contentHash; private String parseStatus; private LocalDateTime createdAt; private LocalDateTime updatedAt; private Integer isDeleted; }
