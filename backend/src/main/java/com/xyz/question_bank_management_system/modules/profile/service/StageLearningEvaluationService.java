package com.xyz.question_bank_management_system.modules.profile.service;

import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;

import java.time.LocalDate;
import java.util.List;

public interface StageLearningEvaluationService {

    StageLearningEvaluationVO myEvaluation(Long userId, String stage, LocalDate startDate, LocalDate endDate);

    List<StageLearningEvaluationVO> teacherEvaluations(
            Long teacherId,
            boolean admin,
            Long studentId,
            String stage,
            LocalDate startDate,
            LocalDate endDate
    );
}
