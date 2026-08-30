package com.xyz.question_bank_management_system.modules.learning.service.impl;

import com.xyz.question_bank_management_system.modules.bank.entity.*;
import com.xyz.question_bank_management_system.modules.bank.mapper.*;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningPathSnapshot;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.entity.ResourceKnowledge;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResourceTarget;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningBehaviorMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningPathSnapshotMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceTargetMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.ResourceKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.learning.service.SmartLearningService;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.org.entity.QbClass;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgeRelationMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceRecommendRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceUpsertRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningPathSnapshotSaveRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PersonalizedPracticeRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.util.HashUtil;
import com.xyz.question_bank_management_system.modules.learning.vo.PersonalizedPracticePlanVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO.*;

@Service
@RequiredArgsConstructor
public class SmartLearningServiceImpl implements SmartLearningService {

    private final QbKnowledgePointMapper knowledgePointMapper;
    private final QbLearningResourceMapper resourceMapper;
    private final QbLearningBehaviorMapper behaviorMapper;
    private final StudentKnowledgeStateMapper studentKnowledgeStateMapper;
    private final StudentProfileService studentProfileService;
    private final QbAttemptMapper attemptMapper;
    private final QbLlmCallMapper llmCallMapper;
    private final QbGradingRecordMapper gradingRecordMapper;
    private final QbKnowledgeRelationMapper knowledgeRelationMapper;
    private final QbLearningPathSnapshotMapper learningPathSnapshotMapper;
    private final QbLearningResourceTargetMapper resourceTargetMapper;
    private final ResourceKnowledgeMapper resourceKnowledgeMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final QbClassMapper classMapper;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    private static final int BIZ_TYPE_STUDENT_ASSISTANT = 3;
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> DIMENSION_NAMES = List.of(
            "知识基础",
            "目标清晰度",
            "学习自律",
            "理解迁移",
            "实践能力",
            "错题反思",
            "资源筛选",
            "协作表达"
    );
    private static final List<ScoringDefinition> SCORING_DEFINITIONS = List.of(
            new ScoringDefinition("问题关联性认知", 15, 0.82),
            new ScoringDefinition("计算思维体现", 10, 0.90),
            new ScoringDefinition("子问题求解思路清晰度", 25, 0.88),
            new ScoringDefinition("边缘情况考虑周全性", 20, 0.78),
            new ScoringDefinition("问题分解完整性", 30, 1.02)
    );

    public List<QbKnowledgePoint> knowledgePoints() {
        return knowledgePointMapper.selectAll();
    }

    public Long createKnowledgePoint(QbKnowledgePoint point) {
        if (point.getLevel() == null) point.setLevel(1);
        knowledgePointMapper.insert(point);
        return point.getId();
    }

    public void updateKnowledgePoint(Long id, QbKnowledgePoint point) {
        point.setId(id);
        knowledgePointMapper.update(point);
    }

    public void deleteKnowledgePoint(Long id) {
        knowledgePointMapper.softDelete(id);
    }

    public List<QbLearningResource> resources(String keyword, Long knowledgePointId, Integer limit) {
        return resourceMapper.selectList(keyword, knowledgePointId, normalizeLimit(limit, 50));
    }

    @Transactional
    public Long createResource(LearningResourceUpsertRequest request, Long operatorId) {
        QbLearningResource resource = toResource(request);
        resource.setCreatedBy(operatorId);
        if (!StringUtils.hasText(resource.getAuditStatus())) {
            resource.setAuditStatus("manual");
        }
        resourceMapper.insert(resource);
        replaceResourceKnowledge(resource.getId(), request.getKnowledgePointIds());
        return resource.getId();
    }

    public ResourceRecommendationPublishResult recommendResourceTargets(Long resourceId,
                                                                        LearningResourceRecommendRequest request,
                                                                        Long operatorId,
                                                                        boolean admin) {
        if (resourceId == null || resourceMapper.selectById(resourceId) == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Learning resource not found");
        }
        LearningResourceRecommendRequest safeRequest = request == null ? new LearningResourceRecommendRequest() : request;
        String targetType = StringUtils.hasText(safeRequest.getTargetType()) ? safeRequest.getTargetType() : "student";
        Long classId = safeRequest.getClassId();
        List<Long> studentIds;
        if ("class".equalsIgnoreCase(targetType)) {
            if (classId == null) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "Class id is required");
            }
            QbClass qbClass = classMapper.selectById(classId);
            if (qbClass == null) {
                throw BizException.of(ErrorCode.NOT_FOUND, "Class not found");
            }
            if (!admin && !Objects.equals(qbClass.getTeacherId(), operatorId)) {
                throw BizException.of(ErrorCode.FORBIDDEN, "No permission to publish to this class");
            }
            studentIds = classMemberMapper.listStudentIdsByClassIds(List.of(classId));
            targetType = "class";
        } else {
            studentIds = safeRequest.getStudentIds();
            targetType = "student";
            classId = null;
        }
        studentIds = studentIds == null ? Collections.emptyList() : studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "No target students found");
        }

        List<QbLearningResourceTarget> targets = new ArrayList<>();
        if ("class".equals(targetType)) {
            QbLearningResourceTarget target = new QbLearningResourceTarget();
            target.setResourceId(resourceId);
            target.setClassId(classId);
            target.setTargetType(targetType);
            target.setCreatedBy(operatorId);
            targets.add(target);
        } else {
            for (Long studentId : studentIds) {
                QbLearningResourceTarget target = new QbLearningResourceTarget();
                target.setResourceId(resourceId);
                target.setStudentId(studentId);
                target.setTargetType(targetType);
                target.setCreatedBy(operatorId);
                targets.add(target);
            }
        }
        resourceTargetMapper.batchInsert(targets);

        ResourceRecommendationPublishResult result = new ResourceRecommendationPublishResult();
        result.setResourceId(resourceId);
        result.setTargetType(targetType);
        result.setClassId(classId);
        result.setStudentIds(studentIds);
        result.setTargetCount(studentIds.size());
        return result;
    }

    @Transactional
    public void updateResource(Long id, LearningResourceUpsertRequest request) {
        if (resourceMapper.selectById(id) == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Learning resource not found");
        }
        QbLearningResource resource = toResource(request);
        resource.setId(id);
        resourceMapper.update(resource);
        replaceResourceKnowledge(id, request.getKnowledgePointIds());
    }

    public void deleteResource(Long id) {
        resourceMapper.softDelete(id);
    }

    private QbLearningResource toResource(LearningResourceUpsertRequest request) {
        QbLearningResource resource = new QbLearningResource();
        resource.setTitle(request.getTitle());
        resource.setResourceType(text(request.getResourceType(), "article"));
        resource.setResourcePurpose(text(request.getResourcePurpose(), "learn"));
        resource.setUrl(request.getUrl());
        resource.setSummary(request.getSummary());
        resource.setContent(request.getContent());
        resource.setDifficulty(request.getDifficulty());
        resource.setGenerationType(text(request.getGenerationType(), "manual"));
        resource.setVersion(request.getVersion());
        resource.setPersonalizationBasis(request.getPersonalizationBasis());
        resource.setReviewReportJson(request.getReviewReportJson());
        resource.setModelSourceJson(request.getModelSourceJson());
        resource.setAuditStatus(text(request.getAuditStatus(), "manual"));
        resource.setAgentTaskId(request.getAgentTaskId());
        return resource;
    }

    private void replaceResourceKnowledge(Long resourceId, List<Long> knowledgePointIds) {
        resourceKnowledgeMapper.deleteByResourceId(resourceId);
        List<Long> ids = knowledgePointIds == null ? Collections.emptyList() : knowledgePointIds.stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        List<ResourceKnowledge> relations = new ArrayList<>();
        for (int index = 0; index < ids.size(); index++) {
            ResourceKnowledge relation = new ResourceKnowledge();
            relation.setKnowledgePointId(ids.get(index));
            relation.setRelationType("cover");
            relation.setCoverageWeight(java.math.BigDecimal.ONE);
            relation.setIsPrimary(index == 0 ? 1 : 0);
            relations.add(relation);
        }
        resourceKnowledgeMapper.batchInsert(resourceId, relations);
    }

    public Long recordBehavior(QbLearningBehavior behavior, Long userId) {
        behavior.setUserId(userId);
        behaviorMapper.insert(behavior);
        studentProfileService.refreshAfterBehavior(userId, behavior.getId(), behavior.getCreatedAt());
        return behavior.getId();
    }

    public LearningProfile profile(Long userId) {
        List<StudentKnowledgeState> mastery = studentKnowledgeStateMapper.selectByUserId(userId);
        List<QbKnowledgePoint> weakPoints = knowledgePointMapper.selectWeakest(userId, 6);
        LearningProfile profile = new LearningProfile();
        profile.setAbilityScore(studentProfileService.abilityScore(userId));
        profile.setBehaviorCount(behaviorMapper.countByUserId(userId));
        profile.setStudyDurationSeconds(behaviorMapper.sumDurationByUserId(userId));
        profile.setMastery(mastery);
        profile.setWeakPoints(weakPoints);
        profile.setRecentBehaviors(behaviorMapper.selectRecent(userId, 10));
        profile.setAdvice(buildAdvice(profile));
        return profile;
    }

    public StudentProfileReport profileReport(Long userId) {
        LearningProfile baseProfile = profile(userId);
        List<QbAttempt> attempts = attemptMapper.selectRecentCompletedByUser(userId, null, 30);
        List<QbLlmCall> assistantCalls = llmCallMapper.selectRecentStudentAssistantCalls(BIZ_TYPE_STUDENT_ASSISTANT, userId, 30);

        ScoringItemAnalysis scoringItemAnalysis = buildScoringItemAnalysis(userId, attempts, null, "all");
        List<DimensionItem> radar = buildDimensions(baseProfile, attempts, assistantCalls, scoringItemAnalysis);
        StudentProfileReport report = new StudentProfileReport();
        report.setSummary(buildReportSummary(userId, baseProfile, radar));
        report.setRadar(radar);
        report.setInsights(buildInsights(baseProfile, radar, attempts, assistantCalls));
        report.setScoreTrend(buildScoreTrend(attempts));
        report.setEvaluationDistribution(buildEvaluationDistribution(radar));
        report.setScoringItemAnalysis(scoringItemAnalysis);
        report.setRecords(buildProfileRecords(attempts, assistantCalls));
        return report;
    }

    public LearningRecommendation recommendations(Long userId) {
        List<QbKnowledgePoint> weakPoints = knowledgePointMapper.selectWeakest(userId, 5);
        List<Long> ids = weakPoints.stream().map(QbKnowledgePoint::getId).collect(Collectors.toList());
        List<QbLearningResource> weakResources = ids.isEmpty()
                ? Collections.emptyList()
                : resourceMapper.selectByKnowledgePointIds(ids, 8);
        List<QbLearningResource> targetedResources = resourceTargetMapper.selectResourcesByStudentId(userId, 12);
        LearningRecommendation recommendation = new LearningRecommendation();
        recommendation.setWeakPoints(weakPoints);
        recommendation.setResources(mergeResources(targetedResources, weakResources, 20));
        recommendation.setPlan(buildPlan(weakPoints));
        return recommendation;
    }

    public LearningPathRecommendation pathRecommendation(Long userId, String stage, String goal, Integer days) {
        int targetDays = normalizePathDays(days);
        String normalizedStage = text(stage, "month");
        String normalizedGoal = text(goal, "improve_weakness");
        LearningProfile profile = profile(userId);
        StudentProfileReport report = profileReport(userId);
        List<QbAttempt> attempts = attemptMapper.selectRecentCompletedByUser(userId, null, 24);
        List<QbLlmCall> assistantCalls = llmCallMapper.selectRecentStudentAssistantCalls(BIZ_TYPE_STUDENT_ASSISTANT, userId, 20);
        List<QbKnowledgePoint> weakPoints = profile.getWeakPoints() == null ? Collections.emptyList() : profile.getWeakPoints();
        List<QbKnowledgeRelation> relations = knowledgeRelationMapper.selectAll();
        List<QbKnowledgePoint> allPoints = knowledgePointMapper.selectAll();

        List<QbKnowledgePoint> pathPoints = buildPathPoints(weakPoints, allPoints, relations);
        List<Long> pathPointIds = pathPoints.stream()
                .map(QbKnowledgePoint::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<QbLearningResource> resources = pathPointIds.isEmpty()
                ? Collections.emptyList()
                : resourceMapper.selectByKnowledgePointIds(pathPointIds, 30);
        Map<Long, List<QbLearningResource>> resourcesByPoint = new LinkedHashMap<>();
        for (QbLearningResource resource : resources) {
            resourceKnowledgeMapper.selectByResourceId(resource.getId()).forEach(relation ->
                    resourcesByPoint.computeIfAbsent(relation.getKnowledgePointId(), ignored -> new ArrayList<>()).add(resource));
        }

        LearningPathRecommendation recommendation = new LearningPathRecommendation();
        recommendation.setStage(normalizedStage);
        recommendation.setGoal(normalizedGoal);
        recommendation.setDays(targetDays);
        recommendation.setUpdatedAt(DATE_TIME.format(LocalDateTime.now()));
        recommendation.setSummary(buildPathSummary(profile, report, targetDays, weakPoints.isEmpty(), normalizedStage, normalizedGoal, attempts));
        recommendation.setBasis(buildPathBasis(profile, report, relations, weakPoints, pathPoints));
        recommendation.setEvidence(buildPathEvidence(profile, report, attempts, assistantCalls, weakPoints));
        recommendation.setDiagnosis(buildPathDiagnosis(profile, report, attempts, assistantCalls, weakPoints));
        recommendation.setStrategy(buildPathStrategy(normalizedStage, normalizedGoal, targetDays, weakPoints, report, attempts, assistantCalls));
        recommendation.setResources(resources);
        recommendation.setPhases(buildPathPhases(pathPoints, resourcesByPoint, relations, targetDays, weakPoints.isEmpty(), recommendation.getStrategy(), recommendation.getDiagnosis()));
        recommendation.setLlmAdvice(buildLlmAdvice(userId, recommendation));
        recommendation.setReportMeta(buildReportMeta(recommendation));
        return recommendation;
    }

    public LearningPathSnapshotSaved savePathSnapshot(Long userId, LearningPathSnapshotSaveRequest request) {
        LearningPathRecommendation recommendation = resolveSnapshotRecommendation(userId, request);
        String snapshotJson = writeJson(recommendation);

        QbLearningPathSnapshot snapshot = new QbLearningPathSnapshot();
        snapshot.setUserId(userId);
        snapshot.setStage(recommendation.getStage());
        snapshot.setGoal(recommendation.getGoal());
        snapshot.setDays(recommendation.getDays());
        snapshot.setTitle(recommendation.getReportMeta() == null ? recommendation.getSummary().getHeadline() : recommendation.getReportMeta().getPrintableTitle());
        snapshot.setSummaryText(recommendation.getLlmAdvice() == null ? recommendation.getSummary().getHeadline() : recommendation.getLlmAdvice().getHeadline());
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setSnapshotHash(HashUtil.sha256(snapshotJson));
        learningPathSnapshotMapper.insert(snapshot);

        if (recommendation.getReportMeta() != null) {
            recommendation.getReportMeta().setSnapshotId(snapshot.getId());
            recommendation.getReportMeta().setSnapshotSavedAt(format(snapshot.getCreatedAt()));
        }

        LearningPathSnapshotSaved saved = new LearningPathSnapshotSaved();
        saved.setId(snapshot.getId());
        saved.setTitle(snapshot.getTitle());
        saved.setGeneratedAt(format(snapshot.getCreatedAt()));
        saved.setStage(snapshot.getStage());
        saved.setGoal(snapshot.getGoal());
        saved.setDays(snapshot.getDays());
        return saved;
    }

    public LearningPathRecommendation pathSnapshotDetail(Long userId, Long id) {
        QbLearningPathSnapshot snapshot = learningPathSnapshotMapper.selectOwnedById(id, userId);
        if (snapshot == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "Learning path snapshot not found");
        }
        try {
            LearningPathRecommendation recommendation = objectMapper.readValue(snapshot.getSnapshotJson(), LearningPathRecommendation.class);
            if (recommendation.getReportMeta() == null) {
                recommendation.setReportMeta(new PrintableReportMeta());
            }
            recommendation.getReportMeta().setSnapshotId(snapshot.getId());
            recommendation.getReportMeta().setSnapshotSavedAt(format(snapshot.getCreatedAt()));
            if (!StringUtils.hasText(recommendation.getUpdatedAt())) {
                recommendation.setUpdatedAt(format(snapshot.getCreatedAt()));
            }
            return recommendation;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "Learning path snapshot parse failed");
        }
    }

    public List<LearningPathSnapshotItem> pathSnapshots(Long userId, Integer limit) {
        return learningPathSnapshotMapper.selectRecentByUser(userId, normalizeLimit(limit, 12)).stream()
                .map(snapshot -> {
                    LearningPathSnapshotItem item = new LearningPathSnapshotItem();
                    item.setId(snapshot.getId());
                    item.setTitle(snapshot.getTitle());
                    item.setSummaryText(snapshot.getSummaryText());
                    item.setStage(snapshot.getStage());
                    item.setGoal(snapshot.getGoal());
                    item.setDays(snapshot.getDays());
                    item.setGeneratedAt(format(snapshot.getCreatedAt()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private ReportSummary buildReportSummary(Long userId, LearningProfile profile, List<DimensionItem> radar) {
        int profileScore = radar.isEmpty() ? 0 : (int) Math.round(radar.stream()
                .mapToInt(DimensionItem::getScore)
                .average()
                .orElse(0));
        ReportSummary summary = new ReportSummary();
        summary.setProfileScore(profileScore);
        summary.setExamCount(attemptMapper.countCompletedByUser(userId, 1));
        summary.setPracticeCount(attemptMapper.countCompletedByUser(userId, 2));
        summary.setAssistantChatCount(llmCallMapper.countStudentAssistantCalls(BIZ_TYPE_STUDENT_ASSISTANT, userId));
        summary.setWeakDimensionCount(radar.stream().filter(item -> item.getScore() < 65).count());
        summary.setUpdatedAt(DATE_TIME.format(LocalDateTime.now()));
        return summary;
    }

    private List<DimensionItem> buildDimensions(LearningProfile profile,
                                                List<QbAttempt> attempts,
                                                List<QbLlmCall> assistantCalls,
                                                ScoringItemAnalysis scoringItemAnalysis) {
        int ability = clamp(profile.getAbilityScore() == null ? 58 : profile.getAbilityScore());
        long behaviorCount = profile.getBehaviorCount() == null ? 0 : profile.getBehaviorCount();
        long studyMinutes = Math.round((profile.getStudyDurationSeconds() == null ? 0 : profile.getStudyDurationSeconds()) / 60.0);
        int attemptAvg = averageAttemptScore(attempts);
        int recentAttempt = attempts.isEmpty() ? attemptAvg : attemptPercentScore(attempts.get(0));
        int weakPenalty = Math.min(profile.getWeakPoints() == null ? 0 : profile.getWeakPoints().size() * 4, 24);
        AssistantSignals signals = analyzeAssistantSignals(assistantCalls);
        int scoringBoost = scoringItemAnalysis == null ? 0 : Math.round((float) (scoringItemAnalysis.averageRate() - 60) / 10);

        Map<String, Integer> scores = Map.of(
                "知识基础", clamp(Math.round(ability * 0.42f + attemptAvg * 0.38f + recentAttempt * 0.20f) + scoringItemScore(scoringItemAnalysis, "计算思维体现")),
                "目标清晰度", clamp(58 + signals.goal * 4 + Math.min(attempts.size(), 5) * 2 + scoringItemScore(scoringItemAnalysis, "问题关联性认知")),
                "学习自律", clamp(46 + (int) Math.min(studyMinutes / 8, 24) + (int) Math.min(behaviorCount * 3, 18) + Math.min(attempts.size(), 6)),
                "理解迁移", clamp(Math.round(attemptAvg * 0.72f + ability * 0.28f) + scoringBoost + scoringItemScore(scoringItemAnalysis, "子问题求解思路清晰度")),
                "实践能力", clamp(Math.round(recentAttempt * 0.55f + attemptAvg * 0.25f + ability * 0.20f) + signals.practice * 2 + scoringBoost + scoringItemScore(scoringItemAnalysis, "问题分解完整性")),
                "错题反思", clamp(66 - weakPenalty + signals.reflection * 4 + Math.min(attempts.size(), 5) + scoringItemScore(scoringItemAnalysis, "边缘情况考虑周全性")),
                "资源筛选", clamp(55 + signals.resource * 5 + (int) Math.min(behaviorCount, 10)),
                "协作表达", clamp(54 + signals.expression * 5 + Math.min(assistantCalls.size(), 8) * 2)
        );

        return DIMENSION_NAMES.stream().map(name -> {
            DimensionItem item = new DimensionItem();
            item.setName(name);
            item.setScore(scores.getOrDefault(name, 60));
            item.setLevel(levelOf(item.getScore()));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ProfileInsight> buildInsights(LearningProfile profile,
                                               List<DimensionItem> radar,
                                               List<QbAttempt> attempts,
                                               List<QbLlmCall> assistantCalls) {
        List<DimensionItem> sorted = radar.stream()
                .sorted(Comparator.comparingInt(DimensionItem::getScore))
                .collect(Collectors.toList());
        List<ProfileInsight> insights = new ArrayList<>();
        if (!sorted.isEmpty()) {
            DimensionItem weakest = sorted.get(0);
            DimensionItem strongest = sorted.get(sorted.size() - 1);
            insights.add(insight("risk", "优先补强 " + weakest.getName(),
                    "该维度当前得分 " + weakest.getScore() + "，建议结合最近错题和小C复盘记录安排下一轮练习。"));
            insights.add(insight("strength", strongest.getName() + "较稳定",
                    "该维度当前得分 " + strongest.getScore() + "，可以作为后续迁移练习和讲解表达的支点。"));
        }
        if (attempts.isEmpty()) {
            insights.add(insight("empty", "缺少最新作答样本", "完成一次试卷或练习后，画像会自动纳入成绩、用时和错题表现。"));
        } else {
            QbAttempt latest = attempts.get(0);
            insights.add(insight("attempt", "最新作答已纳入画像",
                    attemptTypeLabel(latest.getAttemptType()) + "得分 " + attemptScoreText(latest) + "，系统已将其计入得分趋势和能力分布。"));
        }
        if (assistantCalls.isEmpty()) {
            insights.add(insight("assistant", "小C对话样本不足", "通过小C描述目标、困惑和复盘过程后，画像会实时更新表达、资源筛选和错题反思维度。"));
        } else {
            insights.add(insight("assistant", "小C对话持续更新画像",
                    "最近 " + assistantCalls.size() + " 条小C互动已纳入画像，最新对话会优先影响相关维度。"));
        }
        if (StringUtils.hasText(profile.getAdvice())) {
            insights.add(insight("advice", "系统建议", profile.getAdvice()));
        }
        return insights.stream().limit(5).collect(Collectors.toList());
    }

    private List<TrendPoint> buildScoreTrend(List<QbAttempt> attempts) {
        return attempts.stream()
                .sorted(Comparator.comparing(this::attemptTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(attempt -> {
                    TrendPoint point = new TrendPoint();
                    LocalDateTime time = attemptTime(attempt);
                    point.setDate(time == null ? "未记录" : MONTH_DAY.format(time));
                    point.setScore(attemptPercentScore(attempt));
                    point.setRawScore(scoreOf(attempt));
                    point.setMaxScore(attemptMaxScore(attempt));
                    point.setSourceType(attempt.getAttemptType() != null && attempt.getAttemptType() == 2 ? "practice" : "exam");
                    point.setAttemptId(attempt.getId());
                    return point;
                })
                .collect(Collectors.toList());
    }

    private List<DimensionItem> buildEvaluationDistribution(List<DimensionItem> radar) {
        return radar.stream().map(item -> {
            DimensionItem copy = new DimensionItem();
            copy.setName(item.getName());
            copy.setScore(item.getScore());
            copy.setLevel(levelOf(item.getScore()));
            return copy;
        }).collect(Collectors.toList());
    }

    private ScoringItemAnalysis buildScoringItemAnalysis(Long userId,
                                                         List<QbAttempt> allAttempts,
                                                         Integer attemptType,
                                                         String sourceType) {
        List<QbAttempt> attempts = allAttempts.stream()
                .filter(attempt -> attemptType == null || Objects.equals(attempt.getAttemptType(), attemptType))
                .collect(Collectors.toList());
        List<QbGradingRecord> gradingRecords = gradingRecordMapper.selectRecentFinalByUser(userId, attemptType, 80);
        List<ScoringItem> items = aggregateScoringItems(gradingRecords);
        if (items.isEmpty()) {
            items = buildFallbackScoringItems(attempts);
        }

        ScoringItemAnalysis analysis = new ScoringItemAnalysis();
        analysis.setAttemptCount((long) attempts.size());
        analysis.setSourceType(sourceType);
        analysis.setItems(items);
        fillScoringSummary(analysis);

        if ("all".equals(sourceType)) {
            Map<String, ScoringItemAnalysis> variants = new LinkedHashMap<>();
            variants.put("all", cloneScoringAnalysis(analysis, null));
            variants.put("exam", buildScoringItemAnalysis(userId, allAttempts, 1, "exam"));
            variants.put("practice", buildScoringItemAnalysis(userId, allAttempts, 2, "practice"));
            analysis.setVariants(variants);
        }
        return analysis;
    }

    private List<ScoringItem> aggregateScoringItems(List<QbGradingRecord> records) {
        Map<String, ScoreAccumulator> accumulator = new LinkedHashMap<>();
        for (QbGradingRecord record : records) {
            for (ScoringItem item : parseScoringItems(record.getDetailJson())) {
                ScoreAccumulator bucket = accumulator.computeIfAbsent(item.getName(), key -> new ScoreAccumulator());
                bucket.score += item.getAvgScore() == null ? 0 : item.getAvgScore();
                bucket.max += item.getMaxScore() == null ? 0 : item.getMaxScore();
                bucket.count++;
            }
        }
        return accumulator.entrySet().stream().map(entry -> {
            ScoreAccumulator bucket = entry.getValue();
            ScoringItem item = new ScoringItem();
            item.setName(entry.getKey());
            item.setAvgScore(round1(bucket.count == 0 ? 0 : bucket.score / bucket.count));
            item.setMaxScore(round1(bucket.count == 0 ? 0 : bucket.max / bucket.count));
            item.setScoreRate(item.getMaxScore() == null || item.getMaxScore() <= 0 ? 0 : round1(item.getAvgScore() * 100 / item.getMaxScore()));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ScoringItem> parseScoringItems(String detailJson) {
        if (!StringUtils.hasText(detailJson)) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(detailJson);
            JsonNode array = findFirstArray(root, "items", "scoreItems", "criteria", "rubric", "评分项", "评分明细");
            if (array == null) {
                return Collections.emptyList();
            }
            List<ScoringItem> items = new ArrayList<>();
            for (JsonNode node : array) {
                String name = readText(node, "name", "title", "criterion", "评分项", "名称");
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                double score = readDouble(node, "avgScore", "score", "得分", "分数");
                double maxScore = readDouble(node, "maxScore", "fullScore", "满分", "总分");
                double rate = readDouble(node, "scoreRate", "rate", "得分率");
                ScoringItem item = new ScoringItem();
                item.setName(name);
                item.setAvgScore(round1(score));
                item.setMaxScore(round1(maxScore));
                item.setScoreRate(maxScore > 0 ? round1(score * 100 / maxScore) : round1(rate));
                items.add(item);
            }
            return items;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private JsonNode findFirstArray(JsonNode root, String... names) {
        if (root == null) return null;
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        if (root.isArray()) return root;
        return null;
    }

    private List<ScoringItem> buildFallbackScoringItems(List<QbAttempt> attempts) {
        double average = attempts == null || attempts.isEmpty()
                ? 0
                : attempts.stream().mapToInt(this::attemptPercentScore).average().orElse(0);
        return SCORING_DEFINITIONS.stream().map(def -> {
            double rate = attempts == null || attempts.isEmpty() ? 0 : Math.max(0, Math.min(100, average * def.factor));
            ScoringItem item = new ScoringItem();
            item.setName(def.name);
            item.setMaxScore((double) def.maxScore);
            item.setAvgScore(round1(def.maxScore * rate / 100));
            item.setScoreRate(round1(rate));
            return item;
        }).collect(Collectors.toList());
    }

    private void fillScoringSummary(ScoringItemAnalysis analysis) {
        List<ScoringItem> items = analysis.getItems() == null ? Collections.emptyList() : analysis.getItems();
        ScoringItem best = items.stream().max(Comparator.comparingDouble(item -> item.getScoreRate() == null ? 0 : item.getScoreRate())).orElse(null);
        ScoringItem weakest = items.stream().min(Comparator.comparingDouble(item -> item.getScoreRate() == null ? 0 : item.getScoreRate())).orElse(null);
        analysis.setBestItem(best == null ? "" : best.getName());
        analysis.setWeakestItem(weakest == null ? "" : weakest.getName());
        long count = analysis.getAttemptCount() == null ? 0 : analysis.getAttemptCount();
        String source = sourceTypeText(analysis.getSourceType());
        if (count <= 0) {
            analysis.setSummary("暂无" + source + "评分项明细，完成作答并评分后会自动生成诊断。");
        } else {
            analysis.setSummary(count + "次" + source + "中，" +
                    (best == null ? "暂无优势评分项" : best.getName() + "表现最好") + "，" +
                    (weakest == null ? "暂无薄弱评分项" : weakest.getName() + "表现最弱") + "。");
        }
    }

    private ScoringItemAnalysis cloneScoringAnalysis(ScoringItemAnalysis source, Map<String, ScoringItemAnalysis> variants) {
        ScoringItemAnalysis clone = new ScoringItemAnalysis();
        clone.setAttemptCount(source.getAttemptCount());
        clone.setSourceType(source.getSourceType());
        clone.setSummary(source.getSummary());
        clone.setBestItem(source.getBestItem());
        clone.setWeakestItem(source.getWeakestItem());
        clone.setItems(source.getItems());
        clone.setVariants(variants);
        return clone;
    }

    private String readText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return "";
    }

    private double readDouble(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
        }
        return 0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int scoringItemScore(ScoringItemAnalysis analysis, String name) {
        if (analysis == null || analysis.getItems() == null) {
            return 0;
        }
        return analysis.getItems().stream()
                .filter(item -> Objects.equals(item.getName(), name))
                .findFirst()
                .map(item -> (int) Math.round(((item.getScoreRate() == null ? 0 : item.getScoreRate()) - 60) / 12))
                .orElse(0);
    }

    private String sourceTypeText(String sourceType) {
        if ("exam".equals(sourceType)) return "试卷";
        if ("practice".equals(sourceType)) return "练习";
        return "作答";
    }

    private List<ProfileRecord> buildProfileRecords(List<QbAttempt> attempts, List<QbLlmCall> assistantCalls) {
        List<ProfileRecord> records = new ArrayList<>();
        for (QbAttempt attempt : attempts) {
            ProfileRecord record = new ProfileRecord();
            record.setType(attempt.getAttemptType() != null && attempt.getAttemptType() == 2 ? "practice" : "exam");
            record.setTitle(attemptTypeLabel(attempt.getAttemptType()) + " #" + attempt.getId());
            record.setTime(format(attemptTime(attempt)));
            record.setScore(scoreOf(attempt));
            record.setMaxScore(attemptMaxScore(attempt));
            record.setScoreRate(attemptPercentScore(attempt));
            record.setSummary("得分 " + attemptScoreText(attempt) + "，客观分 " + value(attempt.getObjectiveScore()) + "，主观分 " + value(attempt.getSubjectiveScore()));
            record.setRefId(attempt.getId());
            record.setDetail("记录编号：" + attempt.getId()
                    + "\n记录类型：" + attemptTypeLabel(attempt.getAttemptType())
                    + "\n得分：" + attemptScoreText(attempt)
                    + "\n客观分：" + value(attempt.getObjectiveScore())
                    + "\n主观分：" + value(attempt.getSubjectiveScore())
                    + "\n提交时间：" + format(attemptTime(attempt)));
            records.add(record);
        }
        for (QbLlmCall call : assistantCalls) {
            ProfileRecord record = new ProfileRecord();
            record.setType("assistant");
            record.setTitle("小C对话 #" + call.getId());
            record.setTime(format(call.getCreatedAt()));
            record.setScore(null);
            record.setSummary(summarizeAssistantCall(call));
            record.setRefId(call.getId());
            String question = extractAssistantQuestion(call);
            String response = extractAssistantAnswer(call);
            record.setDetail("问题：\n" + truncate(question, 320)
                    + "\n\n回答：\n" + truncate(response, 720));
            records.add(record);
        }
        records.sort(Comparator.comparing(ProfileRecord::getTime, Comparator.nullsLast(String::compareTo)).reversed());
        return records.stream().limit(40).collect(Collectors.toList());
    }

    private ProfileInsight insight(String type, String title, String description) {
        ProfileInsight insight = new ProfileInsight();
        insight.setType(type);
        insight.setTitle(title);
        insight.setDescription(description);
        return insight;
    }

    private AssistantSignals analyzeAssistantSignals(List<QbLlmCall> calls) {
        AssistantSignals signals = new AssistantSignals();
        for (QbLlmCall call : calls) {
            String text = ((call.getPromptText() == null ? "" : call.getPromptText()) + " " +
                    (call.getResponseText() == null ? "" : call.getResponseText())).toLowerCase();
            if (containsAny(text, "目标", "计划", "考试", "提升", "安排", "通过")) signals.goal++;
            if (containsAny(text, "资源", "资料", "视频", "讲义", "题单", "推荐")) signals.resource++;
            if (containsAny(text, "错题", "复盘", "为什么错", "原因", "订正", "反思")) signals.reflection++;
            if (containsAny(text, "代码", "实验", "项目", "案例", "动手", "运行")) signals.practice++;
            if (containsAny(text, "解释", "提问", "表达", "讨论", "讲给", "小c")) signals.expression++;
        }
        return signals;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private int averageAttemptScore(List<QbAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return 58;
        }
        return clamp((int) Math.round(attempts.stream().mapToInt(this::attemptPercentScore).average().orElse(58)));
    }

    private int scoreOf(QbAttempt attempt) {
        return clamp(attempt == null || attempt.getTotalScore() == null ? 0 : attempt.getTotalScore());
    }

    private int attemptPercentScore(QbAttempt attempt) {
        int score = scoreOf(attempt);
        int maxScore = attemptMaxScore(attempt);
        if (maxScore <= 0) {
            return score;
        }
        return clamp((int) Math.round(score * 100.0 / maxScore));
    }

    private int attemptMaxScore(QbAttempt attempt) {
        return attempt == null || attempt.getMaxScore() == null ? 0 : Math.max(0, attempt.getMaxScore());
    }

    private String attemptScoreText(QbAttempt attempt) {
        int score = scoreOf(attempt);
        int maxScore = attemptMaxScore(attempt);
        return maxScore > 0 ? score + " / " + maxScore : score + " 分";
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String levelOf(int score) {
        if (score >= 85) return "excellent";
        if (score >= 70) return "good";
        if (score >= 60) return "normal";
        return "risk";
    }

    private String attemptTypeLabel(Integer attemptType) {
        return attemptType != null && attemptType == 2 ? "练习题" : "试卷";
    }

    private LocalDateTime attemptTime(QbAttempt attempt) {
        if (attempt == null) return null;
        if (attempt.getSubmittedAt() != null) return attempt.getSubmittedAt();
        if (attempt.getUpdatedAt() != null) return attempt.getUpdatedAt();
        return attempt.getCreatedAt();
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : DATE_TIME.format(time);
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private String summarizeAssistantCall(QbLlmCall call) {
        String content = extractAssistantQuestion(call);
        if (!StringUtils.hasText(content)) {
            content = call != null && StringUtils.hasText(call.getResponseText()) ? call.getResponseText().replaceAll("\\s+", " ").trim() : "小C学习对话";
        }
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    private String extractAssistantQuestion(QbLlmCall call) {
        String prompt = call == null ? "" : call.getPromptText();
        if (!StringUtils.hasText(prompt)) {
            return "暂无问题详情";
        }
        String content = prompt;
        String[] markers = {
                "学生本次提问：",
                "学生本次提问:",
                "学生本次提问",
                "对话： 学生：",
                "对话: 学生:",
                "学生：",
                "学生:",
                "用户：",
                "用户:"
        };
        for (String marker : markers) {
            int index = content.lastIndexOf(marker);
            if (index >= 0) {
                content = content.substring(index + marker.length());
                break;
            }
        }
        content = stripAfterAny(content, "页面上下文", "上下文：", "上下文:", "回答要求", "规则：", "规则:");
        return content.replaceAll("\\s+", " ").trim();
    }

    private String extractAssistantAnswer(QbLlmCall call) {
        String responseText = call == null || call.getResponseText() == null ? "" : llmService.extractContent(call.getResponseText());
        if (!StringUtils.hasText(responseText)) {
            return "暂无回答详情";
        }
        String response = responseText.replaceAll("\\s+", " ").trim();
        String readableJson = readableAssistantJson(response);
        return StringUtils.hasText(readableJson) ? readableJson : response;
    }

    private String readableAssistantJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonBlock(text));
            List<String> parts = new ArrayList<>();
            appendJsonText(parts, root, "headline", "");
            appendJsonList(parts, root, "priorityActions", "建议");
            appendJsonList(parts, root, "riskWarnings", "风险提醒");
            appendJsonList(parts, root, "resourceAdvice", "资源使用");
            appendJsonList(parts, root, "dialogueAdvice", "小C复盘");
            appendJsonText(parts, root, "explanation", "原因");
            return String.join("\n", parts);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void appendJsonText(List<String> parts, JsonNode root, String field, String label) {
        String value = readText(root, field);
        if (!StringUtils.hasText(value)) {
            return;
        }
        parts.add(StringUtils.hasText(label) ? label + "：" + value : value);
    }

    private void appendJsonList(List<String> parts, JsonNode root, String field, String label) {
        List<String> values = readStringList(root.get(field));
        if (values.isEmpty()) {
            return;
        }
        parts.add(label + "：" + String.join("；", values));
    }

    private String stripAfterAny(String text, String... markers) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        int end = text.length();
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index > 0) {
                end = Math.min(end, index);
            }
        }
        return text.substring(0, end);
    }

    private String truncate(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return "暂无详情";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > limit ? normalized.substring(0, limit) + "..." : normalized;
    }

    public PersonalizedPracticePlanVO personalizedPracticePlan(Long userId, PersonalizedPracticeRequest request) {
        List<QbKnowledgePoint> weakPoints = knowledgePointMapper.selectWeakest(userId, 5);
        List<Long> knowledgePointIds = weakPoints.stream()
                .map(QbKnowledgePoint::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        PersonalizedPracticePlanVO plan = new PersonalizedPracticePlanVO();
        plan.setWeakPoints(weakPoints);
        plan.setKnowledgePointIds(knowledgePointIds);
        plan.setMode(text(request == null ? null : request.getMode(), "adaptive"));
        plan.setTotalScore(normalizeTotalScore(request == null ? null : request.getTotalScore()));
        plan.setReason(knowledgePointIds.isEmpty()
                ? "No weak knowledge points yet. The system will fall back to the general practice question pool."
                : "Practice will focus on weak knowledge points from the current learner profile.");
        return plan;
    }

    public PracticeStartRequest buildPersonalizedPracticeRequest(Long userId, PersonalizedPracticeRequest request) {
        PersonalizedPracticePlanVO plan = personalizedPracticePlan(userId, request);
        PracticeStartRequest practice = new PracticeStartRequest();
        practice.setMode(plan.getMode());
        practice.setTotalScore(plan.getTotalScore());
        if (plan.getKnowledgePointIds() != null && !plan.getKnowledgePointIds().isEmpty()) {
            PracticeStartRequest.Scope scope = new PracticeStartRequest.Scope();
            scope.setKnowledgePointIds(plan.getKnowledgePointIds());
            practice.setScope(scope);
        }
        return practice;
    }

    private List<QbKnowledgePoint> buildPathPoints(List<QbKnowledgePoint> weakPoints,
                                                   List<QbKnowledgePoint> allPoints,
                                                   List<QbKnowledgeRelation> relations) {
        Map<Long, QbKnowledgePoint> byId = allPoints.stream()
                .filter(point -> point.getId() != null)
                .collect(Collectors.toMap(QbKnowledgePoint::getId, point -> point, (a, b) -> a, LinkedHashMap::new));
        List<QbKnowledgePoint> seeds = weakPoints == null || weakPoints.isEmpty()
                ? allPoints.stream().limit(4).collect(Collectors.toList())
                : weakPoints.stream()
                .sorted(Comparator.comparingDouble(point -> point.getMasteryValue() == null ? 0 : point.getMasteryValue()))
                .limit(5)
                .collect(Collectors.toList());

        Map<Long, QbKnowledgePoint> ordered = new LinkedHashMap<>();
        for (QbKnowledgePoint seed : seeds) {
            if (seed == null || seed.getId() == null) {
                continue;
            }
            relations.stream()
                    .filter(relation -> Objects.equals(relation.getTargetId(), seed.getId()))
                    .filter(relation -> relation.getSourceId() != null)
                    .sorted(Comparator.comparingDouble(relation -> relation.getWeight() == null ? 1 : -relation.getWeight()))
                    .limit(2)
                    .map(relation -> byId.get(relation.getSourceId()))
                    .filter(Objects::nonNull)
                    .forEach(point -> ordered.putIfAbsent(point.getId(), point));
            QbKnowledgePoint canonical = byId.getOrDefault(seed.getId(), seed);
            ordered.putIfAbsent(canonical.getId(), canonical);
        }
        return ordered.values().stream().limit(8).collect(Collectors.toList());
    }

    private StudentPathSummary buildPathSummary(LearningProfile profile,
                                                StudentProfileReport report,
                                                int days,
                                                boolean coldStart,
                                                String stage,
                                                String goal,
                                                List<QbAttempt> attempts) {
        int latestScore = attempts == null || attempts.isEmpty() ? 0 : attemptPercentScore(attempts.get(0));
        String primaryProblem = extractPrimaryProblem(report, profile);
        StudentPathSummary summary = new StudentPathSummary();
        summary.setProfileScore(report.getSummary() == null ? 0 : report.getSummary().getProfileScore());
        summary.setAbilityScore(profile.getAbilityScore() == null ? 0 : profile.getAbilityScore());
        summary.setBehaviorCount(profile.getBehaviorCount() == null ? 0 : profile.getBehaviorCount());
        summary.setStudyMinutes(Math.round((profile.getStudyDurationSeconds() == null ? 0 : profile.getStudyDurationSeconds()) / 60.0));
        summary.setTargetDays(days);
        summary.setMode(coldStart ? "cold_start" : "adaptive");
        summary.setPathTypeLabel(strategyLabel(goal));
        summary.setCycleLabel(stageLabel(stage) + " / " + days + "天");
        summary.setPrimaryProblem(primaryProblem);
        summary.setPriorityLabel(priorityLabel(summary.getProfileScore(), latestScore));
        summary.setHeadline(coldStart
                ? "当前学习数据较少，先建立诊断样本并逐步补齐核心基础。"
                : "已综合学习画像、作题表现、考试表现与小C对话生成个性化学习路径。");
        return summary;
    }

    private PathBasis buildPathBasis(LearningProfile profile,
                                     StudentProfileReport report,
                                     List<QbKnowledgeRelation> relations,
                                     List<QbKnowledgePoint> weakPoints,
                                     List<QbKnowledgePoint> pathPoints) {
        PathBasis basis = new PathBasis();
        basis.setAdvice(profile.getAdvice());
        basis.setWeakPointNames((weakPoints == null ? Collections.<QbKnowledgePoint>emptyList() : weakPoints).stream()
                .map(QbKnowledgePoint::getName)
                .filter(StringUtils::hasText)
                .limit(5)
                .collect(Collectors.toList()));
        basis.setPathPointNames(pathPoints.stream()
                .map(QbKnowledgePoint::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        basis.setWeakDimensionNames((report.getRadar() == null ? Collections.<DimensionItem>emptyList() : report.getRadar()).stream()
                .sorted(Comparator.comparingInt(item -> item.getScore() == null ? 100 : item.getScore()))
                .limit(3)
                .map(DimensionItem::getName)
                .collect(Collectors.toList()));
        basis.setRelationCount(relations == null ? 0 : relations.size());
        basis.setRule("薄弱掌握度优先，其次补齐前置知识点，再安排资源学习、变式练习和错题复盘。");
        return basis;
    }

    private EvidenceSummary buildPathEvidence(LearningProfile profile,
                                              StudentProfileReport report,
                                              List<QbAttempt> attempts,
                                              List<QbLlmCall> assistantCalls,
                                              List<QbKnowledgePoint> weakPoints) {
        EvidenceSummary evidence = new EvidenceSummary();
        List<EvidenceSection> sections = new ArrayList<>();

        sections.add(section("画像与能力", List.of(
                "画像分 " + safeInt(report.getSummary() == null ? null : report.getSummary().getProfileScore()),
                "能力值 " + safeInt(profile.getAbilityScore()),
                "学习行为 " + safeLong(profile.getBehaviorCount()) + " 次",
                "薄弱知识点 " + weakPoints.stream().map(QbKnowledgePoint::getName).filter(StringUtils::hasText).limit(4).collect(Collectors.joining("、"))
        )));

        int examAverage = attempts.stream()
                .filter(item -> Objects.equals(item.getAttemptType(), 1))
                .mapToInt(this::attemptPercentScore)
                .findFirst()
                .orElse(attempts.isEmpty() ? 0 : averageAttemptScore(attempts));
        int practiceAverage = attempts.stream()
                .filter(item -> Objects.equals(item.getAttemptType(), 2))
                .mapToInt(this::attemptPercentScore)
                .findFirst()
                .orElse(attempts.isEmpty() ? 0 : averageAttemptScore(attempts));
        sections.add(section("作题与考试", List.of(
                "最近作答 " + attempts.size() + " 次",
                "最近考试/作业分数参考 " + examAverage,
                "最近练习分数参考 " + practiceAverage,
                "评分弱项 " + (report.getScoringItemAnalysis() == null ? "暂无" : text(report.getScoringItemAnalysis().getWeakestItem(), "暂无"))
        )));

        AssistantSignals signals = analyzeAssistantSignals(assistantCalls);
        sections.add(section("小C 对话证据", List.of(
                "对话轮次 " + assistantCalls.size(),
                "目标表达信号 " + signals.goal,
                "资源偏好信号 " + signals.resource,
                "复盘反思信号 " + signals.reflection
        )));

        evidence.setSections(sections);
        evidence.setDialogueSignals(extractDialogueSignals(assistantCalls));
        List<String> missing = new ArrayList<>();
        if (attempts.isEmpty()) missing.add("近期作题/考试数据较少");
        if (assistantCalls.isEmpty()) missing.add("缺少与小C的自然语言对话证据");
        if (weakPoints.isEmpty()) missing.add("薄弱知识点画像不足");
        evidence.setMissingItems(missing);
        evidence.setCompletenessScore(Math.max(40, 100 - missing.size() * 18));
        return evidence;
    }

    private DiagnosisSummary buildPathDiagnosis(LearningProfile profile,
                                                StudentProfileReport report,
                                                List<QbAttempt> attempts,
                                                List<QbLlmCall> assistantCalls,
                                                List<QbKnowledgePoint> weakPoints) {
        DiagnosisSummary diagnosis = new DiagnosisSummary();
        diagnosis.setKnowledgeGaps(weakPoints.stream()
                .map(QbKnowledgePoint::getName)
                .filter(StringUtils::hasText)
                .limit(4)
                .collect(Collectors.toList()));
        diagnosis.setAbilityRisks((report.getRadar() == null ? Collections.<DimensionItem>emptyList() : report.getRadar()).stream()
                .sorted(Comparator.comparingInt(item -> item.getScore() == null ? 100 : item.getScore()))
                .limit(3)
                .map(item -> item.getName() + "偏弱(" + safeInt(item.getScore()) + ")")
                .collect(Collectors.toList()));
        diagnosis.setBehaviorRisks(buildBehaviorRisks(profile, assistantCalls));
        diagnosis.setExamRisks(buildExamRisks(attempts, report));
        diagnosis.setOpportunityPoints(buildOpportunityPoints(report));
        diagnosis.setConclusion(buildDiagnosisConclusion(diagnosis));
        return diagnosis;
    }

    private PathStrategy buildPathStrategy(String stage,
                                           String goal,
                                           int days,
                                           List<QbKnowledgePoint> weakPoints,
                                           StudentProfileReport report,
                                           List<QbAttempt> attempts,
                                           List<QbLlmCall> assistantCalls) {
        PathStrategy strategy = new PathStrategy();
        String mode = decideStrategyMode(goal, report, attempts, weakPoints);
        strategy.setMode(mode);
        strategy.setLabel(strategyLabel(mode));
        strategy.setTargetCycle(stageLabel(stage) + " / " + days + "天");
        strategy.setPriority(priorityLabel(report.getSummary() == null ? 0 : report.getSummary().getProfileScore(), attempts.isEmpty() ? 0 : attemptPercentScore(attempts.get(0))));
        strategy.setReason(buildStrategyReason(mode, weakPoints, report, assistantCalls));
        strategy.setRoutingBasis(buildRoutingBasis(mode, weakPoints, report, attempts));
        strategy.setGoals(buildStrategyGoals(mode, weakPoints));
        return strategy;
    }

    private List<PathPhase> buildPathPhases(List<QbKnowledgePoint> pathPoints,
                                            Map<Long, List<QbLearningResource>> resourcesByPoint,
                                            List<QbKnowledgeRelation> relations,
                                            int days,
                                            boolean coldStart,
                                            PathStrategy strategy,
                                            DiagnosisSummary diagnosis) {
        List<PhaseDefinition> definitions = phaseDefinitionsForStrategy(strategy == null ? null : strategy.getMode());
        List<PathPhase> phases = definitions.stream().map(definition -> {
            PathPhase phase = new PathPhase();
            phase.setKey(definition.key);
            phase.setTitle(definition.title);
            phase.setDescription(definition.description);
            phase.setGoal(definition.goal);
            phase.setCheckpoint(definition.checkpoint);
            phase.setRiskReminder(definition.riskReminder);
            phase.setNodes(new ArrayList<>());
            return phase;
        }).collect(Collectors.toList());

        if (pathPoints.isEmpty()) {
            return phases;
        }

        int minutesPerNode = Math.max(25, Math.min(90, (days * 45) / Math.max(pathPoints.size(), 1)));
        for (int index = 0; index < pathPoints.size(); index++) {
            QbKnowledgePoint point = pathPoints.get(index);
            PathNode node = new PathNode();
            node.setKnowledgePointId(point.getId());
            node.setTitle(point.getName());
            node.setCode(point.getCode());
            node.setMasteryValue(point.getMasteryValue() == null ? 0 : point.getMasteryValue());
            node.setEstimatedMinutes(minutesPerNode);
            node.setReason(buildNodeReason(point, relations, coldStart));
            node.setTasks(buildNodeTasks(index, point, coldStart));
            node.setResources(resourcesByPoint.getOrDefault(point.getId(), Collections.emptyList()).stream().limit(4).collect(Collectors.toList()));
            node.setCheckpoint(buildNodeCheckpoint(index));
            node.setRelatedEvidence(buildNodeEvidence(point, diagnosis));
            node.setExpectedOutcome(buildNodeOutcome(index, point, strategy));
            int phaseIndex = Math.min(phases.size() - 1, (int) Math.floor(index * phases.size() * 1.0 / Math.max(pathPoints.size(), 1)));
            phases.get(phaseIndex).getNodes().add(node);
        }
        return phases;
    }

    private String buildNodeReason(QbKnowledgePoint point, List<QbKnowledgeRelation> relations, boolean coldStart) {
        if (coldStart) {
            return "学习数据不足，先选择该基础知识点建立诊断样本。";
        }
        boolean isPrerequisite = relations.stream().anyMatch(relation -> Objects.equals(relation.getSourceId(), point.getId()));
        if (isPrerequisite) {
            return "该知识点与后续薄弱点存在前置关系，优先补强可降低后续学习阻力。";
        }
        double mastery = point.getMasteryValue() == null ? 0 : point.getMasteryValue();
        if (mastery > 0 && mastery < 0.6) {
            return "当前掌握度偏低，需要优先复习并配合练习验证。";
        }
        return "该知识点进入推荐路径，用于连接当前薄弱点与后续综合任务。";
    }

    private List<String> buildNodeTasks(int index, QbKnowledgePoint point, boolean coldStart) {
        String name = StringUtils.hasText(point.getName()) ? point.getName() : "当前知识点";
        if (coldStart) {
            return List.of("阅读一份基础资源，记录 3 个关键词。", "完成一次诊断练习，形成初始画像。", "把不会的题目标记到错题复盘。");
        }
        if (index == 0) {
            return List.of("回看最近错题，标注与 " + name + " 相关的错因。", "补读资源摘要，整理 1 条易错规则。", "完成 3 道基础题验证概念。");
        }
        return List.of("学习 " + name + " 的关联资源。", "完成基础题和 1 道变式题。", "用一句话说明本知识点和前后知识点的关系。");
    }

    private String buildNodeCheckpoint(int index) {
        if (index == 0) {
            return "能说清主要错因，并把至少 1 道错题重新做对。";
        }
        if (index % 2 == 0) {
            return "能独立完成同类变式题，并解释关键步骤。";
        }
        return "能把该知识点用于一道综合题或小实验任务。";
    }

    private LearningPathRecommendation resolveSnapshotRecommendation(Long userId, LearningPathSnapshotSaveRequest request) {
        if (request != null && request.getSnapshot() != null) {
            try {
                LearningPathRecommendation recommendation = objectMapper.convertValue(request.getSnapshot(), LearningPathRecommendation.class);
                recommendation.setStage(text(recommendation.getStage(), text(request.getStage(), "month")));
                recommendation.setGoal(text(recommendation.getGoal(), text(request.getGoal(), "improve_weakness")));
                recommendation.setDays(normalizePathDays(recommendation.getDays() == null ? request.getDays() : recommendation.getDays()));
                if (!StringUtils.hasText(recommendation.getUpdatedAt())) {
                    recommendation.setUpdatedAt(DATE_TIME.format(LocalDateTime.now()));
                }
                if (recommendation.getReportMeta() == null) {
                    recommendation.setReportMeta(buildReportMeta(recommendation));
                }
                return recommendation;
            } catch (IllegalArgumentException ignore) {
            }
        }
        return pathRecommendation(userId,
                request == null ? null : request.getStage(),
                request == null ? null : request.getGoal(),
                request == null ? null : request.getDays());
    }

    private PrintableReportMeta buildReportMeta(LearningPathRecommendation recommendation) {
        PrintableReportMeta meta = new PrintableReportMeta();
        meta.setPrintableTitle("个性化学习路径报告");
        meta.setGeneratedAt(text(recommendation.getUpdatedAt(), DATE_TIME.format(LocalDateTime.now())));
        meta.setGeneratedBy("智能学习系统");
        meta.setEvidenceCount(recommendation.getEvidence() == null || recommendation.getEvidence().getSections() == null
                ? 0
                : recommendation.getEvidence().getSections().size());
        meta.setReportVersion("v2.path-report");
        meta.setLlmAdviceSource(recommendation.getLlmAdvice() == null ? "fallback" : recommendation.getLlmAdvice().getSource());
        return meta;
    }

    private LlmAdviceBlock buildLlmAdvice(Long userId, LearningPathRecommendation recommendation) {
        LlmAdviceBlock fallback = buildFallbackAdvice(recommendation);
        try {
            String prompt = buildPathAdvicePrompt(recommendation);
            QbLlmCall call = CompletableFuture
                    .supplyAsync(() -> llmService.chatCompletion(BIZ_TYPE_STUDENT_ASSISTANT, userId, prompt, null, userId))
                    .get(8, TimeUnit.SECONDS);
            if (!Objects.equals(call.getCallStatus(), 1)) {
                fallback.setModelName(call.getModelName());
                return fallback;
            }
            String content = llmService.extractContent(call.getResponseText());
            LlmAdviceBlock parsed = parseLlmAdvice(content);
            if (parsed == null) {
                fallback.setModelName(call.getModelName());
                return fallback;
            }
            parsed.setSource("llm");
            parsed.setModelName(call.getModelName());
            return parsed;
        } catch (Exception ignore) {
            return fallback;
        }
    }

    private LlmAdviceBlock buildFallbackAdvice(LearningPathRecommendation recommendation) {
        LlmAdviceBlock advice = new LlmAdviceBlock();
        advice.setSource("fallback");
        advice.setHeadline("先稳住核心薄弱点，再用阶段检查点验证是否真正掌握。");
        advice.setPriorityActions(List.of(
                "先完成路径前两阶段中的基础补强与错题复盘",
                "优先解决 2-3 个最薄弱知识点，不要同时铺开过多任务",
                "每完成一阶段，和小C复盘一次原因、方法和下一步行动"
        ));
        advice.setRiskWarnings(List.of(
                "只看资源不做题，容易形成假掌握",
                "考试/练习波动较大时，需要先缩短反馈周期",
                "如果对话证据较少，目标偏好判断会不够稳定"
        ));
        advice.setResourceAdvice(List.of(
                "先用讲解型资源补概念，再切换到变式练习",
                "把同一知识点的资源控制在 2-4 个，避免信息过载"
        ));
        advice.setDialogueAdvice(List.of(
                "向小C说明近期目标、卡点和预期考试时间",
                "完成节点后，用小C复盘错因和下一次练习重点"
        ));
        advice.setExplanation(recommendation.getStrategy() == null
                ? "当前建议基于学习画像和近期作答数据生成。"
                : "当前路径先按照“" + recommendation.getStrategy().getLabel() + "”组织，再用证据补强解释和风险提醒。");
        return advice;
    }

    private LlmAdviceBlock parseLlmAdvice(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonBlock(content));
            LlmAdviceBlock advice = new LlmAdviceBlock();
            advice.setHeadline(readText(root, "headline"));
            advice.setPriorityActions(readStringList(root.get("priorityActions")));
            advice.setRiskWarnings(readStringList(root.get("riskWarnings")));
            advice.setResourceAdvice(readStringList(root.get("resourceAdvice")));
            advice.setDialogueAdvice(readStringList(root.get("dialogueAdvice")));
            advice.setExplanation(readText(root, "explanation"));
            return StringUtils.hasText(advice.getHeadline()) ? advice : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractJsonBlock(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String normalized = text.trim();
        int objectStart = normalized.indexOf('{');
        int objectEnd = normalized.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return normalized.substring(objectStart, objectEnd + 1);
        }
        return normalized;
    }

    private String buildPathAdvicePrompt(LearningPathRecommendation recommendation) {
        return "你是学习路径分析助手。请基于以下学生学习路径证据，输出严格 JSON，不要输出 markdown。"
                + "字段必须包含：headline,priorityActions,riskWarnings,resourceAdvice,dialogueAdvice,explanation。"
                + "每个数组给 2 到 3 条中文短句。"
                + "\n路径类型:" + safeText(recommendation.getStrategy() == null ? null : recommendation.getStrategy().getLabel())
                + "\n主要问题:" + safeText(recommendation.getSummary() == null ? null : recommendation.getSummary().getPrimaryProblem())
                + "\n诊断结论:" + safeText(recommendation.getDiagnosis() == null ? null : recommendation.getDiagnosis().getConclusion())
                + "\n知识缺口:" + String.join("、", safeList(recommendation.getDiagnosis() == null ? null : recommendation.getDiagnosis().getKnowledgeGaps()))
                + "\n能力风险:" + String.join("、", safeList(recommendation.getDiagnosis() == null ? null : recommendation.getDiagnosis().getAbilityRisks()))
                + "\n行为风险:" + String.join("、", safeList(recommendation.getDiagnosis() == null ? null : recommendation.getDiagnosis().getBehaviorRisks()))
                + "\n对话信号:" + String.join("、", safeList(recommendation.getEvidence() == null ? null : recommendation.getEvidence().getDialogueSignals()));
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull() && StringUtils.hasText(item.asText())) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private List<PhaseDefinition> phaseDefinitionsForStrategy(String mode) {
        if ("exam_sprint".equals(mode)) {
            return List.of(
                    new PhaseDefinition("diagnosis", "考前定位", "先确认最近考试薄弱章节与高频失分点。", "锁定最影响分数的题型与章节", "完成一次小范围自测", "避免把时间平均分散到所有章节"),
                    new PhaseDefinition("intensive", "高频补救", "集中补救最影响分数的知识点和题型。", "把高频错题改成稳定得分", "同类题正确率明显提升", "不要只复习熟悉内容"),
                    new PhaseDefinition("simulation", "限时演练", "用限时任务模拟考试压力与节奏。", "稳定答题顺序和时间分配", "能在限定时间内完成并复盘", "注意粗心与时间挤压"),
                    new PhaseDefinition("review", "冲刺复盘", "把错因、模板和提醒沉淀成考前清单。", "形成考前可快速查看的复盘单", "能口头复述关键注意事项", "避免最后阶段频繁换资料")
            );
        }
        if ("transfer".equals(mode)) {
            return List.of(
                    new PhaseDefinition("diagnosis", "迁移诊断", "识别哪些概念会、哪些应用场景不会。", "区分记住概念与真正会用", "能说出典型应用场景", "避免只停留在定义记忆"),
                    new PhaseDefinition("concept", "核心抽取", "抽取可迁移的关键规则与步骤。", "提炼跨题型可复用的方法", "能写出自己的步骤卡片", "注意步骤遗漏"),
                    new PhaseDefinition("variation", "变式训练", "通过变式题检验理解迁移。", "在新情境中应用已有方法", "能解释为什么方法仍然成立", "避免套公式式作答"),
                    new PhaseDefinition("project", "综合应用", "把知识点应用到综合题或小任务。", "形成更高层次的知识连接", "能独立完成一项综合任务", "避免跳过复盘")
            );
        }
        if ("reinforcement".equals(mode)) {
            return List.of(
                    new PhaseDefinition("diagnosis", "优势定位", "先明确已有优势与可放大方向。", "找到最值得强化的能力点", "能说明自己的当前优势", "避免重复做过于简单的内容"),
                    new PhaseDefinition("focus", "重点强化", "围绕优势主题做集中训练。", "把优势转化成稳定能力", "高质量完成主题训练", "注意保持反馈闭环"),
                    new PhaseDefinition("stretch", "进阶挑战", "加入更高层次变式与综合任务。", "突破舒适区并提升上限", "能完成至少一项进阶任务", "避免难度跳升过猛"),
                    new PhaseDefinition("solidify", "成果固化", "把方法沉淀为长期可复用策略。", "沉淀个人方法库", "形成自己的复盘模板", "避免只追求数量")
            );
        }
        return List.of(
                new PhaseDefinition("diagnosis", "诊断复盘", "先确认薄弱来源，整理最近错题和知识断点。", "识别最影响进步的核心薄弱点", "能说清主要错因", "避免直接跳到高难题"),
                new PhaseDefinition("foundation", "基础补强", "补齐前置概念，避免直接进入高难综合题。", "先稳住基础知识与关键方法", "基础题与变式题开始稳定", "注意边学边练"),
                new PhaseDefinition("transfer", "迁移练习", "用变式题和小任务检查能否迁移应用。", "验证能否把概念转成应用能力", "能解释关键步骤与判断依据", "避免只记答案"),
                new PhaseDefinition("consolidation", "综合巩固", "完成阶段复盘，把路径结果沉淀为下一轮计划。", "形成下一轮学习闭环", "能总结本轮最有效的方法", "避免复盘流于形式")
        );
    }

    private List<String> buildBehaviorRisks(LearningProfile profile, List<QbLlmCall> assistantCalls) {
        List<String> risks = new ArrayList<>();
        long behaviors = profile.getBehaviorCount() == null ? 0 : profile.getBehaviorCount();
        if (behaviors < 3) risks.add("近期学习行为记录偏少，执行连续性不足");
        if (assistantCalls.isEmpty()) risks.add("缺少目标表达与复盘对话，难以判断真实行动意愿");
        if (!StringUtils.hasText(profile.getAdvice())) risks.add("当前画像建议较弱，需要更多练习样本");
        return risks.isEmpty() ? List.of("行为证据整体稳定，可维持当前节奏") : risks;
    }

    private List<String> buildExamRisks(List<QbAttempt> attempts, StudentProfileReport report) {
        List<String> risks = new ArrayList<>();
        int latest = attempts.isEmpty() ? 0 : attemptPercentScore(attempts.get(0));
        int average = averageAttemptScore(attempts);
        if (!attempts.isEmpty() && latest + 8 < average) risks.add("最近一次成绩低于近期均值，存在波动风险");
        if (report.getScoringItemAnalysis() != null && StringUtils.hasText(report.getScoringItemAnalysis().getWeakestItem())) {
            risks.add("评分细项“" + report.getScoringItemAnalysis().getWeakestItem() + "”表现偏弱");
        }
        return risks.isEmpty() ? List.of("考试/练习表现暂未出现明显风险") : risks;
    }

    private List<String> buildOpportunityPoints(StudentProfileReport report) {
        if (report.getRadar() == null || report.getRadar().isEmpty()) {
            return List.of("先补充近期作答与对话数据，系统再识别可放大优势");
        }
        return report.getRadar().stream()
                .sorted((a, b) -> Integer.compare(safeInt(b.getScore()), safeInt(a.getScore())))
                .limit(2)
                .map(item -> item.getName() + "可继续放大")
                .collect(Collectors.toList());
    }

    private String buildDiagnosisConclusion(DiagnosisSummary diagnosis) {
        String gap = diagnosis.getKnowledgeGaps().isEmpty() ? "知识缺口暂不明显" : "当前主要知识缺口集中在" + String.join("、", diagnosis.getKnowledgeGaps());
        String risk = diagnosis.getAbilityRisks().isEmpty() ? "" : "；能力短板主要体现在" + String.join("、", diagnosis.getAbilityRisks());
        return gap + risk + "。";
    }

    private String decideStrategyMode(String goal, StudentProfileReport report, List<QbAttempt> attempts, List<QbKnowledgePoint> weakPoints) {
        if ("exam_sprint".equals(goal)) return "exam_sprint";
        if ("transfer".equals(goal)) return "transfer";
        int profileScore = report.getSummary() == null ? 0 : safeInt(report.getSummary().getProfileScore());
        if (profileScore >= 76 && weakPoints.size() <= 2) {
            return "reinforcement";
        }
        return "improve_weakness";
    }

    private String buildStrategyReason(String mode, List<QbKnowledgePoint> weakPoints, StudentProfileReport report, List<QbLlmCall> assistantCalls) {
        if ("exam_sprint".equals(mode)) {
            return "当前目标偏考前提分，路径会优先围绕高频失分点、限时练习和考前复盘组织。";
        }
        if ("transfer".equals(mode)) {
            return "当前更需要把已有知识迁移到新情境，路径会强调变式训练和综合应用。";
        }
        if ("reinforcement".equals(mode)) {
            return "当前基础相对稳定，路径将从优势主题切入，提升上限与稳定度。";
        }
        return "当前存在明显薄弱知识点或能力短板，路径先聚焦补弱，再逐步迁移和巩固。";
    }

    private List<String> buildRoutingBasis(String mode, List<QbKnowledgePoint> weakPoints, StudentProfileReport report, List<QbAttempt> attempts) {
        List<String> basis = new ArrayList<>();
        basis.add("薄弱知识点 " + weakPoints.stream().map(QbKnowledgePoint::getName).filter(StringUtils::hasText).limit(3).collect(Collectors.joining("、")));
        basis.add("画像分 " + safeInt(report.getSummary() == null ? null : report.getSummary().getProfileScore()));
        basis.add("近期作答 " + attempts.size() + " 次");
        basis.add("策略模式 " + strategyLabel(mode));
        return basis;
    }

    private List<String> buildStrategyGoals(String mode, List<QbKnowledgePoint> weakPoints) {
        if ("exam_sprint".equals(mode)) {
            return List.of("压缩高频失分点", "提升限时稳定度", "形成考前复盘清单");
        }
        if ("transfer".equals(mode)) {
            return List.of("提炼可迁移方法", "完成变式应用", "提升解释与表达能力");
        }
        if ("reinforcement".equals(mode)) {
            return List.of("放大优势主题", "提升综合应用上限", "沉淀个人方法库");
        }
        return List.of("补齐前置知识点", "完成错题复盘", "通过阶段检查点验证掌握度");
    }

    private List<String> extractDialogueSignals(List<QbLlmCall> assistantCalls) {
        return assistantCalls.stream()
                .map(this::summarizeAssistantCall)
                .filter(StringUtils::hasText)
                .limit(4)
                .collect(Collectors.toList());
    }

    private EvidenceSection section(String title, List<String> bullets) {
        EvidenceSection section = new EvidenceSection();
        section.setTitle(title);
        section.setBullets(bullets.stream().filter(StringUtils::hasText).collect(Collectors.toList()));
        return section;
    }

    private String extractPrimaryProblem(StudentProfileReport report, LearningProfile profile) {
        if (report.getRadar() != null && !report.getRadar().isEmpty()) {
            DimensionItem item = report.getRadar().stream()
                    .min(Comparator.comparingInt(d -> d.getScore() == null ? 100 : d.getScore()))
                    .orElse(null);
            if (item != null) {
                return item.getName() + "偏弱，带动整体学习效率下降";
            }
        }
        if (profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
            return "薄弱知识点集中在" + profile.getWeakPoints().stream().map(QbKnowledgePoint::getName).filter(StringUtils::hasText).limit(2).collect(Collectors.joining("、"));
        }
        return "近期有效诊断样本不足，需要先补充练习与对话证据";
    }

    private String priorityLabel(Integer profileScore, int latestScore) {
        int score = Math.min(safeInt(profileScore), latestScore <= 0 ? safeInt(profileScore) : latestScore);
        if (score < 60) return "高优先级补救";
        if (score < 75) return "中优先级强化";
        return "保持并进阶";
    }

    private String strategyLabel(String value) {
        if ("exam_sprint".equals(value)) return "冲刺型路径";
        if ("transfer".equals(value)) return "迁移型路径";
        if ("reinforcement".equals(value)) return "强化型路径";
        return "补救型路径";
    }

    private String stageLabel(String value) {
        if ("week".equals(value)) return "本周";
        if ("term".equals(value)) return "本学期";
        return "本月";
    }

    private List<String> buildNodeEvidence(QbKnowledgePoint point, DiagnosisSummary diagnosis) {
        List<String> evidence = new ArrayList<>();
        if (point != null && StringUtils.hasText(point.getName())) {
            evidence.add("知识点: " + point.getName());
        }
        if (diagnosis != null && diagnosis.getKnowledgeGaps() != null && diagnosis.getKnowledgeGaps().contains(point.getName())) {
            evidence.add("来自知识缺口诊断");
        }
        if (diagnosis != null && diagnosis.getAbilityRisks() != null && !diagnosis.getAbilityRisks().isEmpty()) {
            evidence.add("关联能力风险: " + diagnosis.getAbilityRisks().get(0));
        }
        return evidence;
    }

    private String buildNodeOutcome(int index, QbKnowledgePoint point, PathStrategy strategy) {
        if (strategy != null && "exam_sprint".equals(strategy.getMode())) {
            return "围绕 " + safeText(point == null ? null : point.getName()) + " 建立稳定得分能力。";
        }
        if (index <= 1) {
            return "补齐该知识点的基础理解与常见错因。";
        }
        return "能把 " + safeText(point == null ? null : point.getName()) + " 应用到变式题或综合题中。";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "Learning path snapshot serialize failed");
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit <= 0) return defaultLimit;
        return Math.min(limit, 100);
    }

    private int normalizeTotalScore(Integer totalScore) {
        if (totalScore == null || totalScore <= 0) return 100;
        return Math.min(totalScore, 300);
    }

    private int normalizePathDays(Integer days) {
        if (days == null || days <= 0) return 14;
        return Math.max(3, Math.min(days, 60));
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String buildAdvice(LearningProfile profile) {
        if (profile.getWeakPoints().isEmpty()) {
            return "当前暂无薄弱知识点数据，建议先完成一次题库练习或作业，系统会自动生成知识画像。";
        }
        String names = profile.getWeakPoints().stream()
                .limit(3)
                .map(QbKnowledgePoint::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return "建议优先复习 " + names + "，先看资源，再做对应标签题目，最后回到错题记录中完成订正。";
    }

    private List<Map<String, Object>> buildPlan(List<QbKnowledgePoint> weakPoints) {
        return weakPoints.stream().limit(5).map(point -> Map.<String, Object>of(
                "knowledgePointId", point.getId(),
                "title", "复习 " + point.getName(),
                "masteryValue", point.getMasteryValue() == null ? 0 : point.getMasteryValue(),
                "actions", List.of("阅读关联资源", "完成题库练习", "订正错题并记录反思")
        )).collect(Collectors.toList());
    }

    private List<QbLearningResource> mergeResources(List<QbLearningResource> first,
                                                    List<QbLearningResource> second,
                                                    int limit) {
        Map<Long, QbLearningResource> merged = new LinkedHashMap<>();
        List<QbLearningResource> rows = new ArrayList<>();
        if (first != null) rows.addAll(first);
        if (second != null) rows.addAll(second);
        for (QbLearningResource resource : rows) {
            if (resource == null || resource.getId() == null || merged.containsKey(resource.getId())) {
                continue;
            }
            merged.put(resource.getId(), resource);
            if (merged.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static class AssistantSignals {
        private int goal;
        private int resource;
        private int reflection;
        private int practice;
        private int expression;
    }

    private static class ScoreAccumulator {
        private double score;
        private double max;
        private int count;
    }

    private static class PhaseDefinition {
        private final String key;
        private final String title;
        private final String description;
        private final String goal;
        private final String checkpoint;
        private final String riskReminder;

        private PhaseDefinition(String key, String title, String description, String goal, String checkpoint, String riskReminder) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.goal = goal;
            this.checkpoint = checkpoint;
            this.riskReminder = riskReminder;
        }
    }

    private static class ScoringDefinition {
        private final String name;
        private final int maxScore;
        private final double factor;

        private ScoringDefinition(String name, int maxScore, double factor) {
            this.name = name;
            this.maxScore = maxScore;
            this.factor = factor;
        }
    }
}
