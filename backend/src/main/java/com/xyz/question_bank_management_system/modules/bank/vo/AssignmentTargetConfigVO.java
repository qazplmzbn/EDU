package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.List;

@Data
public class AssignmentTargetConfigVO {
    private List<AssignmentTargetClassVO> classTargets;
    private List<AssignmentTargetStudentVO> studentTargets;
}
