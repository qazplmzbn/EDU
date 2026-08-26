package com.xyz.question_bank_management_system.modules.llm.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Stage 06 authoritative model configuration. Secrets are cipher text only. */
@Data
public class ModelConfig {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String providerKey;
    private String label;
    private String providerType;
    private String baseUrl;
    private String apiKeyCipher;
    private String model;
    private Double temperature;
    private Integer enabled;
    private Integer isDefault;
    private LocalDateTime updatedAt;
}
