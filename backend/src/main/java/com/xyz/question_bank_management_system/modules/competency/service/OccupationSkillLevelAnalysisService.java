package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkillLevelAnalysis;
import java.util.List;

public interface OccupationSkillLevelAnalysisService {
    OccupationSkillLevelAnalysis analyze(Long occupationId, String providerKey, Long operatorId);
    List<OccupationSkillLevelAnalysis> list(Long occupationId, int limit);
    void publishConsensus(Long occupationId, String batchCode, Long operatorId);
}
