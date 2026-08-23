package com.xyz.question_bank_management_system.modules.bank.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssignmentTargetsRequest {
    private List<Long> userIds = new ArrayList<>();
    private List<Long> classIds = new ArrayList<>();
}
