package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.modules.competency.vo.CompetencyLandingVO.CompetencyLayerResponse;
import com.xyz.question_bank_management_system.modules.competency.vo.CompetencyLandingVO.SyncRecordItem;
import com.xyz.question_bank_management_system.modules.competency.vo.CompetencyLandingVO.SyncResult;

import java.util.List;

public interface CompetencyLandingService {

    CompetencyLayerResponse getCompetencyLayer();

    SyncResult triggerManualSync(Long operatorId);

    void runScheduledSync();

    List<SyncRecordItem> recentSyncRecords(int limit);
}
