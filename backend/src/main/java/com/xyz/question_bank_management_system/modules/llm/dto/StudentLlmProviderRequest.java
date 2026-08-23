package com.xyz.question_bank_management_system.modules.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentLlmProviderRequest {
    private String providerKey;

    @NotBlank(message = "Model name cannot be empty")
    private String label;

    @NotBlank(message = "Model type cannot be empty")
    private String providerType;

    @NotBlank(message = "Base URL cannot be empty")
    private String baseUrl;

    private String apiKey;

    @NotBlank(message = "Model id cannot be empty")
    private String model;

    private Double temperature;
    private Boolean supportsTemperature;
    private String description;
    private String tags;
}
