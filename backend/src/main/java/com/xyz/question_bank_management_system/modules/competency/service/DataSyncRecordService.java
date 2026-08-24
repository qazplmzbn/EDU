package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.modules.competency.entity.DataSyncRecord;

public interface DataSyncRecordService {
    Long start(String sourceName, String syncVersion, Long triggerBy, int fetchedCount);
    void finishSuccess(Long id, int fetchedCount, int insertedCount, int updatedCount);
    void finishFailure(Long id, int fetchedCount, int failedCount, String message);
}
