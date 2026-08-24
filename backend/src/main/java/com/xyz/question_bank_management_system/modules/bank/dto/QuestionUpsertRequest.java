package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionUpsertRequest {
    @NotBlank(message = "题目标题不能为空")
    private String title;
    @NotNull(message = "题型不能为空")
    private Integer questionType;
    @NotNull(message = "难度不能为空")
    private Integer difficulty;
    private String chapter;
    @NotBlank(message = "题干不能为空")
    private String stem;
    private String standardAnswer;
    /** 1=text,2=json */
    private Integer answerFormat = 1;
    private String analysisText;
    /** 1=manual,2=llm_draft,3=llm_final */
    private Integer analysisSource = 1;
    /** 1=draft,2=published,3=archived */
    private Integer status = 1;
    /** teacher only: submit published question for shared bank review */
    private Boolean submitToBankReview = false;

    @Valid
    private List<QuestionOptionDTO> options;

    private List<Long> knowledgePointIds;

}
