package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptStartVO {
    private Long attemptId;
    private Integer attemptNo;
    private Long assignmentId;
    private Long paperId;
    private Integer status;
}
