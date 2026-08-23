package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeacherAssignmentStudentDetailVO {
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String username;
    private String displayName;
    private List<AttemptItemVO> attempts;

    @Data
    public static class AttemptItemVO {
        private Long attemptId;
        private Integer attemptNo;
        private Integer status;
        private Integer totalScore;
        private Integer objectiveScore;
        private Integer subjectiveScore;
        private Integer needsReview;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private Integer durationSec;
        private List<QuestionItemVO> questions;
    }

    @Data
    public static class QuestionItemVO {
        private Long answerId;
        private Long attemptQuestionId;
        private Integer orderNo;
        private Long questionId;
        private String title;
        private Integer questionType;
        private Integer maxScore;
        private Integer finalScore;
        private Integer autoScore;
        private Integer isCorrect;
        private String answerContent;
        private String snapshotJson;
    }
}
