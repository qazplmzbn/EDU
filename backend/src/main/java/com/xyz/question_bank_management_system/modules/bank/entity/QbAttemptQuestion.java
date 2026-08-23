package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

@Data
public class QbAttemptQuestion {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private Integer orderNo;
    private Integer score;
    private String snapshotJson;
    private String snapshotHash;
    private Integer questionType;
    private Integer difficulty;
    /** JSON array string */
    private String tagIdsJson;
}
