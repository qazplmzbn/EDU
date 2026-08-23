package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.util.List;

@Data
public class TeacherAnswerEvidenceVO {
    private Long answerId;
    private StudentVO student;
    private Object questionSnapshot;
    private String studentAnswer;
    private List<GradingRecordVO> gradingRecords;

    @Data
    public static class StudentVO {
        private Long id;
        private String displayName;
    }

    @Data
    public static class GradingRecordVO {
        private Integer gradingMode;
        private Integer score;
        private Double confidence;
        private Integer needsReview;
        private String detailJson;
        private String reviewComment;
        private LlmCallVO llmCall;
    }

    @Data
    public static class LlmCallVO {
        private Long llmCallId;
        private String modelName;
        private String promptText;
        private String responseText;
        private String responseJson;
        private Integer callStatus;
    }
}
