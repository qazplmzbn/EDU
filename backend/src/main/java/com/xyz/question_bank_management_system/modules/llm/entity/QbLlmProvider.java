package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QbLlmProvider {
    private Long id;
    private String providerKey;
    private String label;
    private String providerType;
    private String baseUrl;
    private String apiKeyCipher;
    private String model;
    private Double temperature;
    private Boolean supportsTemperature;
    private String description;
    private String tagsJson;
    private Integer enabled;
    private Integer isDefault;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
