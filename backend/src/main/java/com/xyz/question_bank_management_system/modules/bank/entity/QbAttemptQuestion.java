package com.xyz.question_bank_management_system.modules.bank.entity;

import lombok.Data;

@Data
public class QbAttemptQuestion {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String generatedQuestionCode;
    private Integer orderNo;
    private Integer score;
    private String snapshotJson;
    private String snapshotHash;
    private Integer questionType;
    private Integer difficulty;
    /** JSON array of knowledge point relation snapshots */
    private String knowledgeSnapshotJson;
}
