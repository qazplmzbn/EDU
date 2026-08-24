package com.xyz.question_bank_management_system.modules.bank.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionSearchQuery {
    private String keyword;
    private String chapter;
    private Integer difficulty;
    private Integer questionType;
    private Integer status;
    private Integer bankReviewStatus;
    /** single knowledge point filter */
    private Long knowledgePointId;
    /** multi-knowledge-point OR filter */
    private List<Long> knowledgePointIds;
    /** creator filter */
    private Long createdBy;

    /** current viewer id (for teacher/student scope filtering) */
    private Long viewerId;

    /** teacher source: all/mine/bank */
    private String sourceType;

    /** apply teacher visibility scope */
    private Boolean teacherScope;

    /** apply student visibility scope */
    private Boolean studentScope;

    /** teacher ids visible to student (joined class teachers) */
    private List<Long> visibleTeacherIds;
}
