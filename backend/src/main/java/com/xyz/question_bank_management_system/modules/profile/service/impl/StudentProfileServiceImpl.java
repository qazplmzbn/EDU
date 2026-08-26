package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.*;
import com.xyz.question_bank_management_system.modules.profile.mapper.*;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {
    private static final String VERSION = "stage05-v1";
    private final QbAnswerMapper answerMapper;
    private final StudentKnowledgeStateMapper knowledgeStateMapper;
    private final AbilityDimensionMapper dimensionMapper;
    private final StudentAbilityStateMapper abilityStateMapper;
    private final StudentEvidenceMapper evidenceMapper;
    private final StudentProfileSummaryMapper summaryMapper;
    private final StudentProfileSnapshotMapper snapshotMapper;
    private final StudentProfileCategoryStatMapper categoryStatMapper;
    private final StudentProfileSupportMapper supportMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void refreshAssessment(Long userId) {
        if (userId == null) return;
        List<ProfileAssessmentSample> samples = answerMapper.selectAbilitySamplesByUserId(userId);
        List<ProfileAssessmentSample> safeSamples = samples == null ? List.of() : samples;
        AbilityDimension ability = dimensionMapper.selectActiveByCode("ABILITY");
        if (ability == null) return;
        double theta = 0d;
        int seen = 0;
        Map<Long, Double> betaByQuestion = new HashMap<>();
        Map<Long, Integer> questionSeen = new HashMap<>();
        double performanceSum = 0d;
        int performanceCount = 0;
        for (ProfileAssessmentSample sample : safeSamples) {
            if (sample == null || sample.getQuestionId() == null || safeInt(sample.getMaxScore()) <= 0) continue;
            int max = safeInt(sample.getMaxScore());
            int score = clamp(safeInt(sample.getFinalScore()), 0, max);
            double rate = (double) score / max;
            double beta = betaByQuestion.getOrDefault(sample.getQuestionId(), (safeDifficulty(sample.getDifficulty()) - 3) * .70d);
            int itemSeen = questionSeen.getOrDefault(sample.getQuestionId(), 0);
            double expected = sigmoid(theta - beta);
            double residual = rate - expected;
            theta += (.55d / Math.sqrt(seen + 1d)) * residual;
            betaByQuestion.put(sample.getQuestionId(), beta - (.12d / Math.sqrt(itemSeen + 1d)) * residual);
            questionSeen.put(sample.getQuestionId(), itemSeen + 1);
            seen++;
            performanceSum += rate;
            performanceCount++;
            writeAssessmentEvidence(userId, sample, ability.getId(), rate);
        }
        int abilityScore = seen == 0 ? 0 : clamp((int) Math.round(sigmoid(theta) * 100), 0, 100);
        upsertDimension(ability, userId, abilityScore, confidence(seen), seen);

        List<StudentKnowledgeState> knowledge = knowledgeStateMapper.selectByUserId(userId);
        double mastery = knowledge == null || knowledge.isEmpty() ? 0d : knowledge.stream()
                .map(StudentKnowledgeState::getMasteryValue).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
        upsertByCode(userId, "MASTERY", mastery * 100d, confidence(knowledge == null ? 0 : knowledge.size()), knowledge == null ? 0 : knowledge.size());
        double performance = performanceCount == 0 ? 0d : performanceSum / performanceCount;
        upsertByCode(userId, "PERFORMANCE", performance * 100d, confidence(performanceCount), performanceCount);
        long behaviorCount = supportMapper.behaviorCount(userId);
        int participationEvidence = Math.min(100, (int) Math.min(Integer.MAX_VALUE, behaviorCount + seen));
        upsertByCode(userId, "PARTICIPATION", Math.min(100d, behaviorCount * 5d + seen * 8d), confidence(participationEvidence), participationEvidence);
        supportMapper.rebuildSkillStates(userId);
        refreshSummary(userId, mastery, performance, behaviorCount);
        refreshCategoryStats(userId, knowledge, performance, behaviorCount);
    }

    @Override
    @Transactional
    public void refreshAfterBehavior(Long userId, Long behaviorId, LocalDateTime occurredAt) {
        if (userId == null || behaviorId == null) return;
        StudentEvidence evidence = new StudentEvidence();
        evidence.setUserId(userId); evidence.setEvidenceType("behavior"); evidence.setSourceEntityType("behavior");
        evidence.setSourceEntityId(behaviorId); evidence.setTargetType("preference"); evidence.setTargetId(behaviorId); evidence.setEvidenceDirection(0);
        evidence.setConfidence(BigDecimal.ONE); evidence.setEvidenceText("学习行为");
        evidence.setOccurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt); evidence.setExtractVersion(VERSION);
        evidenceMapper.insertIgnore(evidence);
        refreshAssessment(userId);
    }

    @Override
    public int abilityScore(Long userId) {
        StudentAbilityState state = abilityStateMapper.selectByUserIdAndCode(userId, "ABILITY");
        return state == null || state.getScore() == null ? 0 : clamp(state.getScore().intValue(), 0, 100);
    }

    @Override public StudentProfileSummary summary(Long userId) { return summaryMapper.selectByUserId(userId); }
    @Override public StudentBasicProfile basicProfile(Long userId) { return supportMapper.basicProfile(userId); }

    @Override
    @Transactional
    public void saveBasicProfile(Long userId, StudentBasicProfile profile) {
        profile.setUserId(userId); supportMapper.upsertBasicProfile(profile);
        recordSelfEvidence(userId, "profile", userId, "preference", userId, "学生更新基础画像");
        refreshAssessment(userId);
    }

    @Override public List<StudentLearningGoal> goals(Long userId) { return supportMapper.goals(userId); }
    @Override
    @Transactional
    public Long saveGoal(Long userId, StudentLearningGoal goal) {
        goal.setUserId(userId); if (goal.getStatus() == null) goal.setStatus("active");
        if (goal.getSourceType() == null) goal.setSourceType("self_report"); if (goal.getPriority() == null) goal.setPriority(1);
        if (goal.getId() == null) supportMapper.insertGoal(goal); else supportMapper.updateGoal(goal);
        recordSelfEvidence(userId, "goal", goal.getId(), "goal", goal.getId(), "学生更新学习目标");
        refreshAssessment(userId); return goal.getId();
    }

    @Override public List<StudentLearningPreference> preferences(Long userId) { return supportMapper.preferences(userId); }
    @Override
    @Transactional
    public void savePreference(Long userId, StudentLearningPreference preference) {
        preference.setUserId(userId); if (preference.getSourceType() == null) preference.setSourceType("self_report");
        if (preference.getEvidenceCount() == null) preference.setEvidenceCount(1);
        if (supportMapper.updateActivePreference(preference) == 0) supportMapper.insertPreference(preference);
        recordSelfEvidence(userId, "preference", userId, "preference", userId, "学生更新学习偏好"); refreshAssessment(userId);
    }

    @Override
    @Transactional
    public StudentProfileSnapshot createSnapshot(Long userId, String triggerType, Long triggerId) {
        StudentProfileSnapshot snapshot = new StudentProfileSnapshot();
        snapshot.setUserId(userId); snapshot.setBasicStateJson(json(supportMapper.basicProfile(userId)));
        snapshot.setKnowledgeStateJson(json(knowledgeStateMapper.selectByUserId(userId)));
        snapshot.setAbilityStateJson(json(abilityStateMapper.selectByUserId(userId)));
        snapshot.setPreferenceStateJson(json(supportMapper.preferences(userId))); snapshot.setGoalStateJson(json(supportMapper.goals(userId)));
        snapshot.setSkillStateJson("[]"); snapshot.setCategoryStatJson(json(categoryStatMapper.selectCurrent(userId))); snapshot.setTriggerType(triggerType == null ? "manual" : triggerType);
        snapshot.setTriggerId(triggerId); snapshot.setEvidenceCount(evidenceMapper.countByUserId(userId));
        StudentProfileSummary summary = summaryMapper.selectByUserId(userId);
        snapshot.setProfileSummary(summary == null ? "暂无画像汇总" : "知识掌握 " + value(summary.getOverallKnowledgeMastery()) + "，能力均值 " + value(summary.getAbilityAverageScore()));
        snapshotMapper.insert(snapshot); summaryMapper.updateLastSnapshot(userId, snapshot.getId()); return snapshot;
    }

    private void refreshSummary(Long userId, double mastery, double performance, long behaviorCount) {
        StudentProfileSummary summary = new StudentProfileSummary(); summary.setUserId(userId);
        summary.setOverallKnowledgeMastery(decimal(mastery)); summary.setAssessmentAccuracy(decimal(performance));
        List<StudentAbilityState> abilities = abilityStateMapper.selectByUserId(userId);
        summary.setAbilityAverageScore(decimal(abilities.stream().map(StudentAbilityState::getScore).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0)));
        summary.setLearningActivityScore(decimal(Math.min(1d, behaviorCount / 20d)));
        List<StudentKnowledgeState> knowledge = knowledgeStateMapper.selectByUserId(userId);
        summary.setWeakKnowledgeCount((int) knowledge.stream().filter(k -> k.getMasteryValue() != null && k.getMasteryValue().doubleValue() < .5d).count());
        summary.setWeakSkillCount(supportMapper.weakSkillCount(userId)); summary.setRecommendedDifficulty(summary.getOverallKnowledgeMastery().doubleValue() < .5 ? 2 : summary.getOverallKnowledgeMastery().doubleValue() < .8 ? 3 : 4);
        summaryMapper.upsert(summary);
    }

    private void refreshCategoryStats(Long userId, List<StudentKnowledgeState> knowledge, double performance, long behaviorCount) {
        List<StudentKnowledgeState> safeKnowledge = knowledge == null ? List.of() : knowledge;
        List<StudentAbilityState> abilityStates = abilityStateMapper.selectByUserId(userId);
        categoryStatMapper.upsert(userId, "knowledge", safeKnowledge.size(),
                (int) safeKnowledge.stream().filter(k -> k.getMasteryValue() != null && k.getMasteryValue().doubleValue() >= .8d).count(),
                (int) safeKnowledge.stream().filter(k -> k.getMasteryValue() != null && k.getMasteryValue().doubleValue() < .5d).count(),
                decimal(safeKnowledge.stream().map(StudentKnowledgeState::getMasteryValue).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0d)),
                decimal(safeKnowledge.isEmpty() ? 0d : 1d));
        categoryStatMapper.upsert(userId, "ability", abilityStates.size(),
                (int) abilityStates.stream().filter(s -> s.getScore() != null && s.getScore().doubleValue() >= 80d).count(),
                (int) abilityStates.stream().filter(s -> s.getScore() != null && s.getScore().doubleValue() < 50d).count(),
                decimal(abilityStates.stream().map(StudentAbilityState::getScore).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0d)), decimal(abilityStates.isEmpty() ? 0d : 1d));
        categoryStatMapper.upsert(userId, "assessment", 1, performance >= .8d ? 1 : 0, performance < .5d ? 1 : 0, decimal(performance), decimal(performance > 0d ? 1d : 0d));
        categoryStatMapper.upsert(userId, "behavior", (int) Math.min(Integer.MAX_VALUE, behaviorCount), 0, behaviorCount == 0 ? 1 : 0, decimal(Math.min(1d, behaviorCount / 20d)), decimal(Math.min(1d, behaviorCount / 20d)));
    }

    private void writeAssessmentEvidence(Long userId, ProfileAssessmentSample sample, Long abilityDimensionId, double rate) {
        if (sample.getAnswerId() == null) return;
        StudentEvidence e = new StudentEvidence(); e.setUserId(userId); e.setEvidenceType("question"); e.setSourceEntityType("answer");
        e.setSourceEntityId(sample.getAnswerId()); e.setTargetType("ability"); e.setTargetId(abilityDimensionId);
        e.setEvidenceValue(decimal(rate)); e.setEvidenceDirection(rate >= .5 ? 1 : -1); e.setConfidence(BigDecimal.ONE);
        e.setEvidenceText("已评分作答"); e.setOccurredAt(sample.getEventTime() == null ? LocalDateTime.now() : sample.getEventTime()); e.setExtractVersion(VERSION); evidenceMapper.insertIgnore(e);
    }

    private void recordSelfEvidence(Long userId, String sourceType, Long sourceId, String targetType, Long targetId, String text) {
        StudentEvidence evidence = new StudentEvidence(); evidence.setUserId(userId); evidence.setEvidenceType("self_report");
        evidence.setSourceEntityType(sourceType); evidence.setSourceEntityId(sourceId); evidence.setTargetType(targetType); evidence.setTargetId(targetId);
        evidence.setEvidenceDirection(0); evidence.setConfidence(BigDecimal.ONE); evidence.setEvidenceText(text);
        evidence.setOccurredAt(LocalDateTime.now()); evidence.setExtractVersion(VERSION); evidenceMapper.insertIgnore(evidence);
    }

    private void upsertByCode(Long userId,String code,double score,BigDecimal confidence,int count) { AbilityDimension d=dimensionMapper.selectActiveByCode(code); if(d!=null) upsertDimension(d,userId,score,confidence,count); }
    private void upsertDimension(AbilityDimension d,Long userId,double score,BigDecimal confidence,int count) { abilityStateMapper.upsert(userId,d.getId(),decimal(score),level(score),confidence,count); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value == null ? List.of() : value); } catch (JsonProcessingException e) { return "[]"; } }
    private String value(BigDecimal v){ return v == null ? "0" : v.setScale(2,RoundingMode.HALF_UP).toPlainString(); }
    private BigDecimal decimal(double v){ return BigDecimal.valueOf(Math.max(0d,Math.min(100d,v))).setScale(4,RoundingMode.HALF_UP); }
    private BigDecimal confidence(int count){ return BigDecimal.valueOf(Math.min(1d,count/10d)).setScale(4,RoundingMode.HALF_UP); }
    private String level(double score){ return score>=80?"mastered":score>=50?"basic":"weak"; }
    private int safeInt(Integer value){ return value==null?0:Math.max(0,value); }
    private int safeDifficulty(Integer value){ return clamp(value==null?3:value,1,5); }
    private int clamp(int value,int min,int max){ return Math.max(min,Math.min(max,value)); }
    private double sigmoid(double value){ return value>=0?1d/(1d+Math.exp(-value)):Math.exp(value)/(1d+Math.exp(value)); }
}
