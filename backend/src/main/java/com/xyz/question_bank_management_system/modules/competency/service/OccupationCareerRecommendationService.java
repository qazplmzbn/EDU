package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.modules.competency.dto.CareerRecommendationRefreshRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.OccupationSkillStandardPublishRequest;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerGapVO;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerRecommendationVO;

public interface OccupationCareerRecommendationService {
    void publishStandard(Long occupationId, OccupationSkillStandardPublishRequest request);
    CareerGapVO refreshGaps(Long userId, Long occupationId);
    CareerGapVO latestGaps(Long userId, Long occupationId);
    CareerRecommendationVO refreshRecommendations(Long userId, CareerRecommendationRefreshRequest request);
    CareerRecommendationVO recommendation(Long userId, String snapshotCode);
    java.util.Map<String,Object> acceptRecommendation(Long userId,String snapshotCode,Long courseId);
    java.util.Map<String,Object> report(Long userId,Long occupationId);
    java.util.List<java.util.Map<String,Object>> diagnosticTargets(Long userId,Long occupationId);
    java.util.Map<String,Object> createDiagnosticResourceUnit(Long userId,Long occupationId);
}
