package com.xyz.question_bank_management_system.modules.competency.dto;

import lombok.Data;

@Data
public class CompetencyImportCommitRequest extends CompetencyImportValidateRequest {
    private String validationHash;
}
