package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttemptResultVO {
    private Long attemptId;
    private Integer status;
    private Integer totalScore;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private Integer needsReview;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer durationSec;

    private List<AnswerResultVO> answers;

    @Data
    public static class AnswerResultVO {
        private Long answerId;
        private Long attemptQuestionId;
        private Integer orderNo;
        private Long questionId;
        private Integer maxScore;
        private Integer finalScore;
        private Integer autoScore;
        private Integer isCorrect;
        private String answerContent;
        private String snapshotJson;
    }
}
