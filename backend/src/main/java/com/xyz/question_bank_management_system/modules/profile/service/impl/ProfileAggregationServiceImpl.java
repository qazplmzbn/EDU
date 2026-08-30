package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.modules.profile.entity.*;
import com.xyz.question_bank_management_system.modules.profile.mapper.*;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import com.xyz.question_bank_management_system.modules.profile.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProfileAggregationServiceImpl implements ProfileAggregationService {
    private final StudentKnowledgeStateMapper stateMapper;
    private final StudentProfileSnapshotMapper snapshotMapper;
    private final ProfileV1Mapper profileMapper;
    private final KnowledgeStateService stateService;
    private final DimktClient dimktClient;
    private final ObjectMapper objectMapper;

    @Override @Transactional
    public StudentProfileSnapshot apply(ValidatedInteraction interaction) {
        StudentProfileSnapshot latest = snapshotMapper.selectLatestForUpdate(interaction.getUserId(), interaction.getCourseId());
        long before = latest == null ? 0 : Objects.requireNonNullElse(latest.getProfileVersion(), 0L);
        Map<Long,BigDecimal> dimkt = dimktClient.infer(interaction).orElse(Map.of());
        for (Long kp : interaction.getKnowledgeWeights().keySet()) {
            StudentKnowledgeState previous = stateMapper.selectForUpdate(interaction.getUserId(), interaction.getCourseId(), kp);
            if (previous == null) previous = initial(interaction, kp);
            StudentKnowledgeState next = stateService.update(previous, List.of(interaction)).getStates().get(0);
            if (dimkt.containsKey(kp)) {
                next.setMasteryValue(dimkt.get(kp).max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP));
                next.setCalculationMethod("DIMKT_ONLINE"); next.setAlgorithmVersion("dimkt_v1");
            }
            stateMapper.upsertVersioned(next);
            KnowledgeStateUpdateLog log = new KnowledgeStateUpdateLog();
            log.setUserId(interaction.getUserId()); log.setCourseId(interaction.getCourseId()); log.setKnowledgePointId(kp);
            log.setInteractionId(interaction.getId()); log.setEvidenceScope("DIRECT");
            log.setPreviousMastery(previous.getMasteryValue()); log.setNewMastery(next.getMasteryValue());
            log.setPreviousConfidence(previous.getConfidence()); log.setNewConfidence(next.getConfidence());
            log.setModelVersion(next.getAlgorithmVersion()); log.setProfileVersionBefore(before); log.setProfileVersionAfter(before + 1);
            log.setCorrelationId(CorrelationContext.get()); profileMapper.insertUpdateLog(log);
        }
        return snapshot(interaction.getUserId(), interaction.getCourseId(), before + 1, interaction.getId(), "INTERACTION");
    }

    @Override @Transactional
    public StudentProfileSnapshot recalibrate(Long userId, Long courseId, List<ValidatedInteraction> history) {
        StudentProfileSnapshot latest = snapshotMapper.selectLatestForUpdate(userId, courseId);
        long next = latest == null ? 1 : Objects.requireNonNullElse(latest.getProfileVersion(), 0L) + 1;
        return snapshot(userId, courseId, next, null, "RECALIBRATE");
    }

    private StudentKnowledgeState initial(ValidatedInteraction interaction, Long kp) {
        StudentKnowledgeState value = new StudentKnowledgeState(); value.setUserId(interaction.getUserId()); value.setCourseId(interaction.getCourseId()); value.setKnowledgePointId(kp);
        value.setMasteryValue(new BigDecimal("0.30")); value.setConfidence(BigDecimal.ZERO); value.setEvidenceCount(0); value.setAttemptCount(0); value.setCorrectCount(0); value.setStateVersion(0L); value.setLastInteractionSeq(0L); return value;
    }

    private StudentProfileSnapshot snapshot(Long userId, Long courseId, long version, Long trigger, String type) {
        try {
            StudentProfileSnapshot s = new StudentProfileSnapshot(); s.setUserId(userId); s.setCourseId(courseId); s.setProfileVersion(version); s.setCalculatedAt(LocalDateTime.now()); s.setAlgorithmVersion("profile_v1"); s.setCorrelationId(CorrelationContext.get());
            List<StudentKnowledgeState> states = stateMapper.selectByUserAndCourse(userId, courseId); s.setKnowledgeStateJson(objectMapper.writeValueAsString(states)); s.setResourcePreferenceJson(objectMapper.writeValueAsString(profileMapper.preferences(userId, courseId))); s.setCognitiveProfileJson(objectMapper.writeValueAsString(profileMapper.cognitive(userId, courseId)));
            List<StudentBehaviorMetric> behavior = profileMapper.behavior(userId, courseId); s.setInitiativeJson(objectMapper.writeValueAsString(behavior.stream().filter(x -> "INITIATIVE".equals(x.getMetricGroup())).toList())); s.setRegularityJson(objectMapper.writeValueAsString(behavior.stream().filter(x -> "REGULARITY".equals(x.getMetricGroup())).toList()));
            s.setTriggerType(type); s.setTriggerId(trigger); s.setEvidenceCount(states.stream().mapToInt(x -> Objects.requireNonNullElse(x.getEvidenceCount(), 0)).sum()); s.setProfileSummary("course=" + courseId + ", version=" + version); snapshotMapper.insertVersioned(s); return s;
        } catch (Exception ex) { throw new IllegalStateException("画像快照序列化失败", ex); }
    }
}
