package com.xyz.question_bank_management_system.modules.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeGraphExtractionRequest {
    private String sourceText;
    private String providerKey;
    private Boolean autoSave;
}
