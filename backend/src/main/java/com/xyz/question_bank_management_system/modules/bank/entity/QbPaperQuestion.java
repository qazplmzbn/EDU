package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

@Data
public class QbPaperQuestion {
    private Long id;
    private Long paperId;
    private Long questionId;
    private Integer orderNo;
    private Integer score;
    /** JSON as String */
    private String snapshotJson;
    private String snapshotHash;
}
