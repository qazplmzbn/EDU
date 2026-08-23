package com.xyz.question_bank_management_system.modules.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentUpsertRequest {
    @NotNull(message = "试卷编号不能为空")
    private Long paperId;
    @NotBlank(message = "作业标题不能为空")
    private String assignmentTitle;
    private String assignmentDesc;
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    private Integer timeLimitMin = 0;
    private Integer maxAttempts = 1;
    private Integer shuffleQuestions = 0;
    private Integer shuffleOptions = 0;
    /** 1=draft,2=published,3=closed */
    private Integer publishStatus = 1;
}
