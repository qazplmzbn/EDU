package com.xyz.question_bank_management_system.modules.llm.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentLlmProviderVO {
    private Long id;
    private String source;
    private Boolean system;
    private Boolean editable;
    private String providerKey;
    private String label;
    private String providerType;
    private String baseUrl;
    private String model;
    private Double temperature;
    private Boolean supportsTemperature;
    private String description;
    private List<String> tags;
    private Boolean enabled;
    private Boolean isDefault;
    private Boolean hasApiKey;
    private String apiKeyMask;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
