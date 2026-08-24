package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionDetailVO {
    private Long id;
    private String title;
    private Integer questionType;
    private Integer difficulty;
    private String chapter;
    private String stem;
    private String standardAnswer;
    private Integer answerFormat;
    private String analysisText;
    private Integer analysisSource;
    private Long analysisLlmCallId;
    private Integer status;
    private Integer bankReviewStatus;
    private Long bankReviewerId;
    private LocalDateTime bankReviewedAt;
    private String bankReviewComment;
    private Boolean submitToBankReview;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<QuestionOptionVO> options;
    private List<Long> knowledgePointIds;
    private List<QuestionKnowledgeVO> knowledgeRelations;
    private List<QuestionLlmAnalysisVO> llmAnalyses;

    @Data
    public static class QuestionOptionVO {
        private Long id;
        private String optionLabel;
        private String optionContent;
        private Integer isCorrect;
        private Integer sortOrder;
    }

    @Data
    public static class QuestionKnowledgeVO {
        private Long knowledgePointId;
        private java.math.BigDecimal weight;
        private String relationType;
        private Integer isPrimary;
        private java.math.BigDecimal confidence;
        private String sourceType;
    }

}
