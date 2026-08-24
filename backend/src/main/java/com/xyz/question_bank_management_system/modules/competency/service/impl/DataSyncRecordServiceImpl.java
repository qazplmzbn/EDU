package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.xyz.question_bank_management_system.modules.competency.entity.DataSyncRecord;
import com.xyz.question_bank_management_system.modules.competency.mapper.DataSyncRecordMapper;
import com.xyz.question_bank_management_system.modules.competency.service.DataSyncRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DataSyncRecordServiceImpl implements DataSyncRecordService {
    private final DataSyncRecordMapper mapper;

    @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(String sourceName, String syncVersion, Long triggerBy, int fetchedCount) {
        DataSyncRecord record = new DataSyncRecord();
        record.setSyncType("competency"); record.setSourceName(sourceName); record.setSyncVersion(syncVersion);
        record.setTriggerType("manual"); record.setTriggerBy(triggerBy); record.setStatus("running");
        record.setFetchedCount(fetchedCount); record.setInsertedCount(0); record.setUpdatedCount(0); record.setFailedCount(0);
        mapper.insert(record); return record.getId();
    }

    @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishSuccess(Long id, int fetchedCount, int insertedCount, int updatedCount) {
        DataSyncRecord record = new DataSyncRecord(); record.setId(id); record.setStatus("success");
        record.setFetchedCount(fetchedCount); record.setInsertedCount(insertedCount); record.setUpdatedCount(updatedCount); record.setFailedCount(0);
        mapper.finish(record);
    }

    @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishFailure(Long id, int fetchedCount, int failedCount, String message) {
        DataSyncRecord record = new DataSyncRecord(); record.setId(id); record.setStatus("failed");
        record.setFetchedCount(fetchedCount); record.setInsertedCount(0); record.setUpdatedCount(0); record.setFailedCount(failedCount);
        record.setErrorMessage(message == null ? null : message.substring(0, Math.min(message.length(), 2000)));
        mapper.finish(record);
    }
}
