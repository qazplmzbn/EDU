package com.xyz.question_bank_management_system.modules.learning.service.impl;

import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.entity.ProfileEvidenceEvent;
import com.xyz.question_bank_management_system.modules.learning.service.ProfileEvidenceConsumer;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentProfileSnapshotMapper;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import com.xyz.question_bank_management_system.modules.profile.service.ProfileAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Idempotently transfers one immutable interaction into the course profile. */
@Service
@RequiredArgsConstructor
public class ProfileEvidenceConsumerImpl implements ProfileEvidenceConsumer {
    private static final String CONSUMER = "PROFILE_V1";

    private final PersonalizedResourceMapper mapper;
    private final ProfileAggregationService profile;
    private final StudentProfileSnapshotMapper snapshotMapper;

    @Override
    public StudentProfileSnapshot apply(String eventId, ValidatedInteraction interaction) {
        ProfileEvidenceEvent existing = mapper.selectEvidenceEventByInteraction(interaction.getId(), CONSUMER);
        if (existing != null) {
            return existingSnapshot(interaction);
        }

        ProfileEvidenceEvent event = new ProfileEvidenceEvent();
        event.setEventId(eventId);
        event.setInteractionId(interaction.getId());
        event.setConsumerName(CONSUMER);
        event.setStatus("PROCESSING");
        if (mapper.insertEvidenceEvent(event) == 0) {
            return existingSnapshot(interaction);
        }

        StudentProfileSnapshot snapshot = profile.apply(interaction);
        mapper.markEvidenceProcessed(eventId, CONSUMER);
        return snapshot;
    }

    private StudentProfileSnapshot existingSnapshot(ValidatedInteraction interaction) {
        StudentProfileSnapshot snapshot = snapshotMapper.selectLatest(interaction.getUserId(), interaction.getCourseId());
        if (snapshot != null) {
            return snapshot;
        }
        throw BizException.of(ErrorCode.CONFLICT, "画像证据正在处理，请稍后重试");
    }
}
