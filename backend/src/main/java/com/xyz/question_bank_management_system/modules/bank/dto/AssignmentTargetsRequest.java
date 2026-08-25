package com.xyz.question_bank_management_system.modules.bank.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignmentTargetsRequest {
    /**
     * A non-null empty list deliberately means that no student can access the
     * assignment.  Each selection is scoped to one class for authorization
     * and membership validation before it is flattened into target rows.
     */
    private List<AssignmentTargetSelectionDTO> targets;

    /**
     * Existing direct student targets to keep while replacing class selections.
     * A missing field retains all existing direct targets for older clients;
     * an explicit empty list keeps none.
     */
    private List<Long> retainedStudentIds;
}
