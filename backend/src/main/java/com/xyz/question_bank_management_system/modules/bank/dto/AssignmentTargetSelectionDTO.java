package com.xyz.question_bank_management_system.modules.bank.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignmentTargetSelectionDTO {
    private Long classId;

    /** An empty list means the complete class; null is an invalid request. */
    private List<Long> studentIds;
}
