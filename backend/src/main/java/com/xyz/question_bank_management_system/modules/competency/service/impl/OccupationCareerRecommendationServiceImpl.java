package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.dto.CareerRecommendationRefreshRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.OccupationSkillStandardPublishRequest;
import com.xyz.question_bank_management_system.modules.competency.entity.*;
import com.xyz.question_bank_management_system.modules.competency.mapper.CareerRecommendationMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationSkillMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.competency.service.OccupationCareerRecommendationService;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerGapVO;
import com.xyz.question_bank_management_system.modules.competency.vo.CareerRecommendationVO;
import com.xyz.question_bank_management_system.modules.agent.service.ResourceUnitService;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceUnit;
import com.xyz.question_bank_management_system.modules.course.service.PathRefreshApplicationService;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentResumeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OccupationCareerRecommendationServiceImpl implements OccupationCareerRecommendationService {
    private static final String SKILL_ALGORITHM = "career_skill_aggregation_v1";
    private static final String GAP_ALGORITHM = "occupation_skill_gap_v1";
    private static final String RECOMMENDATION_ALGORITHM = "occupation_course_recommendation_v1";
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CareerRecommendationMapper mapper;
    private final OccupationMapper occupationMapper;
    private final OccupationSkillMapper occupationSkillMapper;
    private final SkillMapper skillMapper;
    private final SkillKnowledgeMapper skillKnowledgeMapper;
    private final PathRefreshApplicationService pathRefreshApplicationService;
    private final StudentResumeMapper studentResumeMapper;
    private final ResourceUnitService resourceUnitService;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishStandard(Long occupationId, OccupationSkillStandardPublishRequest request) {
        requireOccupation(occupationId);
        String batchCode = text(request.getBatchCode());
        if (batchCode == null) throw BizException.of(ErrorCode.PARAM_ERROR, "batchCode is required");
        String version = defaultText(request.getLevelVersion(), "job_skill_level_v1");
        List<OccupationSkill> universe = occupationSkillMapper.selectByOccupationIdForUpdate(occupationId);
        Set<String> requestedKeys = new HashSet<>();
        if (request.getItems() == null || request.getItems().isEmpty()) throw BizException.of(ErrorCode.PARAM_ERROR, "items are required");
        for (OccupationSkillStandardPublishRequest.Item item : request.getItems()) {
            String key = item.getSkillId() + ":" + defaultText(item.getRequirementType(), "essential");
            if (!requestedKeys.add(key)) throw BizException.of(ErrorCode.PARAM_ERROR, "duplicate occupation skill standard item");
        }
        if (!universe.isEmpty()) {
            Set<String> existingKeys = new HashSet<>();
            for (OccupationSkill row : universe) existingKeys.add(row.getSkillId() + ":" + row.getRequirementType());
            if (!existingKeys.equals(requestedKeys)) throw BizException.of(ErrorCode.CONFLICT, "standard publication must include the complete locked occupation skill universe");
        }
        for (OccupationSkillStandardPublishRequest.Item item : request.getItems()) {
            if (item.getSkillId() == null || skillMapper.selectById(item.getSkillId()) == null) {
                throw BizException.of(ErrorCode.NOT_FOUND, "skill not found: " + item.getSkillId());
            }
            BigDecimal required = bounded(item.getRequiredLevel(), "requiredLevel");
            BigDecimal importance = item.getImportanceScore() == null ? ONE : bounded(item.getImportanceScore(), "importanceScore");
            String relationType = defaultText(item.getRequirementType(), "essential");
            OccupationSkill relation = occupationSkillMapper.selectByBusinessKey(occupationId, item.getSkillId(), relationType);
            if (relation == null) {
                relation = new OccupationSkill();
                relation.setOccupationId(occupationId);
                relation.setSkillId(item.getSkillId());
                relation.setRequirementType(relationType);
                relation.setImportanceScore(importance);
                relation.setRequiredLevel(required);
                relation.setSourceRef("career-standard:" + batchCode);
                occupationSkillMapper.insert(relation);
            }
            mapper.publishOccupationSkill(relation.getId(), required, importance, relationType,
                    defaultText(item.getSource(), "MANUAL"), version, batchCode);
        }
    }

    @Override
    @Transactional
    public CareerGapVO refreshGaps(Long userId, Long occupationId) {
        requireOccupation(occupationId);
        List<CareerSkillRequirement> requirements = publishedRequirements(occupationId);
        Map<Long, CareerStudentSkillState> states = aggregateSkillStates(userId, requirements);
        String snapshotCode = code("GAP");
        String batchCode = singleBatch(requirements);
        for (CareerSkillRequirement requirement : requirements) {
            CareerStudentSkillState state = states.get(requirement.getSkillId());
            BigDecimal aggregate = state == null ? ZERO : defaultDecimal(state.getProficiencyValue(), ZERO);
            BigDecimal core = state == null ? aggregate : defaultDecimal(state.getCoreProficiencyValue(), aggregate);
            // Core knowledge is a prerequisite floor for usable skill proficiency.
            // For a skill without core mappings aggregateOne stores aggregate again.
            BigDecimal current = aggregate.min(core);
            BigDecimal confidence = state == null ? ZERO : state.getConfidence();
            BigDecimal gap = positive(requirement.getRequiredLevel().subtract(current));
            BigDecimal typeFactor = "optional".equalsIgnoreCase(requirement.getRequirementType()) ? new BigDecimal("0.60") : ONE;
            BigDecimal confidenceFactor = new BigDecimal("0.50").add(confidence.multiply(new BigDecimal("0.50")));
            StudentOccupationSkillGap record = new StudentOccupationSkillGap();
            record.setSnapshotCode(snapshotCode); record.setUserId(userId); record.setOccupationId(occupationId);
            record.setOccupationSkillId(requirement.getOccupationSkillId()); record.setSkillId(requirement.getSkillId());
            record.setRequiredLevel(scale(requirement.getRequiredLevel())); record.setCurrentLevel(scale(current));
            record.setCurrentConfidence(scale(confidence)); record.setGapValue(scale(gap));
            record.setPriorityScore(scale6(gap.multiply(defaultDecimal(requirement.getImportanceScore(), ONE)).multiply(typeFactor).multiply(confidenceFactor)));
            record.setGapStatus(gap.signum() == 0 ? "MATCHED" : confidence.compareTo(new BigDecimal("0.20")) < 0 ? "UNKNOWN" : "GAP");
            record.setTargetBatchCode(batchCode); record.setCalculationVersion(GAP_ALGORITHM);
            mapper.insertGap(record);
        }
        return toGapVo(mapper.selectGapsBySnapshot(snapshotCode));
    }

    @Override
    public CareerGapVO latestGaps(Long userId, Long occupationId) {
        String snapshotCode = mapper.selectLatestGapSnapshotCode(userId, occupationId);
        if (snapshotCode == null) throw BizException.of(ErrorCode.NOT_FOUND, "no career gap snapshot; refresh gaps first");
        return toGapVo(mapper.selectGapsBySnapshot(snapshotCode));
    }

    @Override
    @Transactional
    public CareerRecommendationVO refreshRecommendations(Long userId, CareerRecommendationRefreshRequest request) {
        Long occupationId = request.getOccupationId();
        if (occupationId == null) throw BizException.of(ErrorCode.PARAM_ERROR, "occupationId is required");
        CareerGapVO gaps = refreshGaps(userId, occupationId);
        List<StudentOccupationSkillGap> rows = mapper.selectGapsBySnapshot(gaps.getSnapshotCode());
        List<StudentOccupationSkillGap> targetGaps = rows.stream().filter(g -> "GAP".equals(g.getGapStatus()) && g.getGapValue().signum() > 0).toList();
        long unknownGapCount = rows.stream().filter(g -> "UNKNOWN".equals(g.getGapStatus()) && g.getGapValue().signum() > 0).count();
        String snapshotCode = code("REC");
        String batchCode = gaps.getTargetBatchCode();
        List<Candidate> candidates = rankCourses(targetGaps);
        int limit = request.getLimit() == null ? 5 : Math.max(1, Math.min(20, request.getLimit()));
        if (candidates.size() > limit) candidates = candidates.subList(0, limit);
        CareerRecommendationSnapshot snapshot = new CareerRecommendationSnapshot();
        snapshot.setSnapshotCode(snapshotCode); snapshot.setGapSnapshotCode(gaps.getSnapshotCode()); snapshot.setUserId(userId);
        snapshot.setOccupationId(occupationId); snapshot.setTargetBatchCode(batchCode); snapshot.setAlgorithmVersion(RECOMMENDATION_ALGORITHM);
        snapshot.setRequestJson(json(Map.of("occupationId", occupationId, "limit", limit)));
        String dataStatus = candidates.isEmpty() ? (unknownGapCount > 0 ? "DIAGNOSTIC_REQUIRED" : "NO_ACTIVE_COURSE_COVERAGE") : "READY";
        snapshot.setResultSummaryJson(json(Map.of(
                "gapCount", targetGaps.size(),
                "unknownGapCount", unknownGapCount,
                "recommendationCount", candidates.size(),
                "dataStatus", dataStatus,
                "fallback", candidates.isEmpty() ? "DIAGNOSTIC_TARGETS" : "NONE")));
        mapper.insertRecommendationSnapshot(snapshot);
        int rank = 1;
        for (Candidate candidate : candidates) {
            CareerRecommendationItem item = new CareerRecommendationItem();
            item.setSnapshotId(snapshot.getId()); item.setCourseId(candidate.courseId); item.setRankNo(rank++);
            item.setCourseScore(candidate.score); item.setCoverageScore(candidate.coverage); item.setCoreCoverageRate(candidate.coreCoverage);
            item.setReasonJson(json(candidate.reason)); item.setCoveredSkillIdsJson(json(candidate.skillIds));
            item.setCoveredKnowledgePointIdsJson(json(candidate.knowledgePointIds)); item.setFallbackType(null);
            mapper.insertRecommendationItem(item);
        }
        return recommendation(userId, snapshotCode);
    }

    @Override
    public CareerRecommendationVO recommendation(Long userId, String snapshotCode) {
        CareerRecommendationSnapshot snapshot = mapper.selectRecommendationSnapshot(snapshotCode, userId);
        if (snapshot == null) throw BizException.of(ErrorCode.NOT_FOUND, "recommendation snapshot not found");
        CareerRecommendationVO vo = new CareerRecommendationVO();
        vo.setSnapshotCode(snapshot.getSnapshotCode()); vo.setGapSnapshotCode(snapshot.getGapSnapshotCode());
        vo.setOccupationId(snapshot.getOccupationId()); vo.setTargetBatchCode(snapshot.getTargetBatchCode());
        vo.setAlgorithmVersion(snapshot.getAlgorithmVersion()); vo.setCreatedAt(snapshot.getCreatedAt());
        vo.setItems(mapper.selectRecommendationItems(snapshot.getId()).stream().map(this::toRecommendationItem).toList());
        Object dataStatus = readMap(snapshot.getResultSummaryJson()).get("dataStatus");
        vo.setDataStatus(dataStatus == null ? (vo.getItems().isEmpty() ? "NO_ACTIVE_COURSE_COVERAGE" : "READY") : String.valueOf(dataStatus));
        return vo;
    }

    @Override
    @Transactional
    public Map<String, Object> acceptRecommendation(Long userId, String snapshotCode, Long courseId) {
        CareerRecommendationSnapshot snapshot = mapper.selectRecommendationSnapshot(snapshotCode, userId);
        if (snapshot == null) throw BizException.of(ErrorCode.NOT_FOUND, "recommendation snapshot not found");
        CareerRecommendationAcceptance existing = mapper.selectAcceptance(snapshot.getId(), courseId);
        if (existing != null) return Map.of("snapshotCode", snapshotCode, "courseId", courseId, "pathCode", existing.getLearningPathCode(), "status", existing.getStatus());
        CareerRecommendationItem item = mapper.selectRecommendationItem(snapshotCode, userId, courseId);
        if (item == null) throw BizException.of(ErrorCode.NOT_FOUND, "recommended course not found");
        List<Long> targetIds = readList(item.getCoveredKnowledgePointIdsJson()).stream().distinct().sorted().toList();
        if (targetIds.isEmpty()) throw BizException.of(ErrorCode.CONFLICT, "recommendation has no target knowledge point");
        Long target = mapper.selectRecommendationTargets(courseId, targetIds).stream()
                .filter(id -> knowledgePointMapper.selectActiveByIdAndCourse(id, courseId) != null).findFirst().orElse(null);
        if (target == null) throw BizException.of(ErrorCode.CONFLICT, "recommended course has no target in its current ACTIVE graph");
        Map<String, Object> path = pathRefreshApplicationService.create(userId, courseId, target, "career-accept-" + snapshotCode + "-" + courseId);
        CareerRecommendationAcceptance acceptance = new CareerRecommendationAcceptance();
        acceptance.setSnapshotId(snapshot.getId()); acceptance.setUserId(userId); acceptance.setCourseId(courseId);
        acceptance.setLearningPathCode(String.valueOf(path.get("pathCode"))); acceptance.setStatus("ACCEPTED");
        mapper.insertAcceptance(acceptance);
        return Map.of("snapshotCode", snapshotCode, "courseId", courseId, "pathCode", acceptance.getLearningPathCode(), "status", "ACCEPTED");
    }

    @Override
    public Map<String, Object> report(Long userId, Long occupationId) {
        requireOccupation(occupationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("occupationId", occupationId);
        result.put("confirmedResumeEvidenceCount", studentResumeMapper.confirmedEvidenceCount(userId));
        String gapCode = mapper.selectLatestGapSnapshotCode(userId, occupationId);
        result.put("gap", gapCode == null ? null : toGapVo(mapper.selectGapsBySnapshot(gapCode)));
        CareerRecommendationSnapshot snapshot = mapper.selectLatestRecommendationSnapshot(userId, occupationId);
        result.put("recommendation", snapshot == null ? null : recommendation(userId, snapshot.getSnapshotCode()));
        result.put("acceptedCourses", mapper.selectAcceptances(userId, occupationId).stream().map(value -> Map.of("courseId", value.getCourseId(), "pathCode", value.getLearningPathCode(), "status", value.getStatus(), "acceptedAt", value.getAcceptedAt())).toList());
        return result;
    }

    @Override
    public List<Map<String, Object>> diagnosticTargets(Long userId, Long occupationId) {
        String gapCode = mapper.selectLatestGapSnapshotCode(userId, occupationId);
        if (gapCode == null) return List.of();
        return mapper.selectGapsBySnapshot(gapCode).stream()
                .filter(gap -> gap.getGapValue().signum() > 0)
                .filter(gap -> "UNKNOWN".equals(gap.getGapStatus()) || mapper.selectCourseCoverage(List.of(gap.getSkillId())).isEmpty())
                .map(gap -> Map.<String, Object>of(
                        "skillId", gap.getSkillId(),
                        "skillName", gap.getSkillName(),
                        "priorityScore", gap.getPriorityScore(),
                        "knowledgePointIds", skillKnowledgeMapper.selectBySkillId(gap.getSkillId()).stream().map(item -> item.getKnowledgePointId()).distinct().sorted().toList(),
                        "reason", "职业证据不足，建议先进行课程内诊断"))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createDiagnosticResourceUnit(Long userId, Long occupationId) {
        CareerRecommendationAcceptance acceptance = mapper.selectLatestAcceptance(userId, occupationId);
        if (acceptance == null) throw BizException.of(ErrorCode.NOT_FOUND, "accept a recommended course before creating diagnostic resource");
        ResourceUnit unit = resourceUnitService.aggregate(acceptance.getLearningPathCode(), null);
        return Map.of("pathCode", acceptance.getLearningPathCode(), "resourceUnitCode", unit.getResourceUnitCode(), "resourceUnitId", unit.getId(), "status", unit.getStatus());
    }

    private List<CareerSkillRequirement> publishedRequirements(Long occupationId) {
        List<OccupationSkill> universe = occupationSkillMapper.selectByOccupationId(occupationId);
        if (universe.isEmpty()) throw BizException.of(ErrorCode.TARGET_STANDARD_INCOMPLETE, "occupation has no skill universe");
        List<CareerSkillRequirement> requirements = mapper.selectPublishedRequirements(occupationId);
        if (requirements.size() != universe.size()) throw BizException.of(ErrorCode.TARGET_STANDARD_INCOMPLETE, "occupation has unpublished required skill levels");
        singleBatch(requirements);
        return requirements;
    }

    private Map<Long, CareerStudentSkillState> aggregateSkillStates(Long userId, List<CareerSkillRequirement> requirements) {
        List<Long> skillIds = requirements.stream().map(CareerSkillRequirement::getSkillId).distinct().toList();
        Map<Long, List<CareerKnowledgeEvidence>> bySkill = new HashMap<>();
        for (CareerKnowledgeEvidence evidence : mapper.selectKnowledgeEvidence(userId, skillIds)) {
            bySkill.computeIfAbsent(evidence.getSkillId(), ignored -> new ArrayList<>()).add(evidence);
        }
        Map<Long, CareerStudentSkillState> states = new HashMap<>();
        for (Long skillId : skillIds) {
            List<CareerKnowledgeEvidence> evidence = bySkill.getOrDefault(skillId, List.of());
            CareerStudentSkillState state = aggregateOne(userId, skillId, evidence);
            mapper.upsertStudentSkillState(state);
            states.put(skillId, state);
        }
        return states;
    }

    private CareerStudentSkillState aggregateOne(Long userId, Long skillId, List<CareerKnowledgeEvidence> evidence) {
        BigDecimal totalWeight = ZERO, totalMastery = ZERO, totalConfidence = ZERO;
        BigDecimal coreWeight = ZERO, coreMastery = ZERO;
        int evidenceCount = 0; int covered = 0;
        for (CareerKnowledgeEvidence row : evidence) {
            BigDecimal weight = defaultDecimal(row.getMappingWeight(), ONE).multiply(defaultDecimal(row.getMappingConfidence(), ONE));
            BigDecimal mastery = defaultDecimal(row.getMasteryValue(), ZERO);
            BigDecimal confidence = defaultDecimal(row.getStateConfidence(), ZERO);
            totalWeight = totalWeight.add(weight); totalMastery = totalMastery.add(mastery.multiply(weight)); totalConfidence = totalConfidence.add(confidence.multiply(weight));
            if (row.getEvidenceCount() != null && row.getEvidenceCount() > 0) { covered++; evidenceCount += row.getEvidenceCount(); }
            if ("core".equalsIgnoreCase(row.getRequirementType()) || "essential".equalsIgnoreCase(row.getRequirementType())) { coreWeight = coreWeight.add(weight); coreMastery = coreMastery.add(mastery.multiply(weight)); }
        }
        BigDecimal proficiency = divide(totalMastery, totalWeight);
        BigDecimal core = coreWeight.signum() == 0 ? proficiency : divide(coreMastery, coreWeight);
        CareerStudentSkillState state = new CareerStudentSkillState();
        state.setUserId(userId); state.setSkillId(skillId); state.setProficiencyValue(scale(proficiency)); state.setCoreProficiencyValue(scale(core));
        state.setConfidence(scale(divide(totalConfidence, totalWeight))); state.setKnowledgeCoverageRate(scale(evidence.isEmpty() ? ZERO : BigDecimal.valueOf(covered).divide(BigDecimal.valueOf(evidence.size()), 4, RoundingMode.HALF_UP)));
        state.setEvidenceCount(evidenceCount); state.setCalculationVersion(SKILL_ALGORITHM);
        BigDecimal effective = proficiency.min(core);
        state.setProficiencyLevel(effective.compareTo(new BigDecimal("0.80")) >= 0 ? "ADVANCED" : effective.compareTo(new BigDecimal("0.50")) >= 0 ? "INTERMEDIATE" : "BEGINNER");
        return state;
    }

    private List<Candidate> rankCourses(List<StudentOccupationSkillGap> gaps) {
        if (gaps.isEmpty()) return List.of();
        Map<Long, BigDecimal> priorities = new TreeMap<>();
        for (StudentOccupationSkillGap gap : gaps) priorities.merge(gap.getSkillId(), gap.getPriorityScore(), BigDecimal::max);
        BigDecimal total = priorities.values().stream().reduce(ZERO, BigDecimal::add);
        if (total.signum() == 0) return List.of();
        Map<Long, Candidate> byCourse = new TreeMap<>();
        for (CareerCourseCoverage coverage : mapper.selectCourseCoverage(new ArrayList<>(priorities.keySet()))) {
            Candidate candidate = byCourse.computeIfAbsent(coverage.getCourseId(), ignored -> new Candidate(coverage));
            candidate.skillIds.add(coverage.getSkillId()); candidate.knowledgePointIds.add(coverage.getKnowledgePointId());
            candidate.coveredPriority.putIfAbsent(coverage.getSkillId(), priorities.get(coverage.getSkillId()));
            if ("core".equalsIgnoreCase(coverage.getMappingType()) || "essential".equalsIgnoreCase(coverage.getMappingType())) candidate.coreSkills.add(coverage.getSkillId());
        }
        List<Candidate> result = new ArrayList<>();
        for (Candidate candidate : byCourse.values()) {
            BigDecimal covered = candidate.coveredPriority.values().stream().reduce(ZERO, BigDecimal::add);
            BigDecimal core = candidate.coreSkills.stream().map(priorities::get).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
            candidate.coverage = scale6(divide(covered, total)); candidate.coreCoverage = scale6(divide(core, total));
            candidate.score = scale6(candidate.coverage.multiply(new BigDecimal("0.70")).add(candidate.coreCoverage.multiply(new BigDecimal("0.30"))));
            candidate.reason = Map.of("reasonCode", candidate.coreCoverage.signum() > 0 ? "HIGH_CORE_COVERAGE" : "GAP_COVERAGE", "coverage", Map.of("all", candidate.coverage, "core", candidate.coreCoverage), "coveredSkillIds", candidate.skillIds, "coveredKnowledgePointIds", candidate.knowledgePointIds);
            result.add(candidate);
        }
        result.sort(Comparator.comparing((Candidate c) -> c.score).reversed().thenComparing(c -> c.courseId));
        return result;
    }

    private CareerGapVO toGapVo(List<StudentOccupationSkillGap> rows) {
        if (rows.isEmpty()) throw BizException.of(ErrorCode.NOT_FOUND, "career gap snapshot not found");
        StudentOccupationSkillGap first = rows.get(0); CareerGapVO vo = new CareerGapVO();
        vo.setSnapshotCode(first.getSnapshotCode()); vo.setOccupationId(first.getOccupationId()); vo.setTargetBatchCode(first.getTargetBatchCode()); vo.setCalculationVersion(first.getCalculationVersion()); vo.setCalculatedAt(first.getCalculatedAt());
        vo.setItems(rows.stream().map(row -> { CareerGapVO.Item item = new CareerGapVO.Item(); item.setSkillId(row.getSkillId()); item.setSkillName(row.getSkillName()); item.setRequirementType(row.getRequirementType()); item.setRequiredLevel(row.getRequiredLevel()); item.setCurrentLevel(row.getCurrentLevel()); item.setCurrentConfidence(row.getCurrentConfidence()); item.setGapValue(row.getGapValue()); item.setPriorityScore(row.getPriorityScore()); item.setGapStatus(row.getGapStatus()); return item; }).toList());
        return vo;
    }

    private CareerRecommendationVO.Item toRecommendationItem(CareerRecommendationItem row) {
        CareerRecommendationVO.Item item = new CareerRecommendationVO.Item(); item.setCourseId(row.getCourseId()); item.setCourseCode(row.getCourseCode()); item.setCourseName(row.getCourseName()); item.setRankNo(row.getRankNo()); item.setCourseScore(row.getCourseScore()); item.setCoverageScore(row.getCoverageScore()); item.setCoreCoverageRate(row.getCoreCoverageRate()); item.setCoveredSkillIds(readList(row.getCoveredSkillIdsJson())); item.setCoveredKnowledgePointIds(readList(row.getCoveredKnowledgePointIdsJson())); item.setReason(readMap(row.getReasonJson())); item.setFallbackType(row.getFallbackType()); return item;
    }

    private String singleBatch(List<CareerSkillRequirement> requirements) {
        Set<String> batches = new HashSet<>(); for (CareerSkillRequirement requirement : requirements) batches.add(requirement.getPublishedBatchCode());
        if (batches.size() != 1 || batches.contains(null)) throw BizException.of(ErrorCode.TARGET_STANDARD_INCOMPLETE, "occupation standard must be published as one batch");
        return batches.iterator().next();
    }
    private void requireOccupation(Long occupationId) { if (occupationId == null || occupationMapper.selectById(occupationId) == null) throw BizException.of(ErrorCode.NOT_FOUND, "occupation not found"); }
    private String code(String prefix) { return prefix + "-" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "-" + UUID.randomUUID().toString().substring(0, 8); }
    private BigDecimal bounded(BigDecimal value, String field) { if (value == null || value.compareTo(ZERO) < 0 || value.compareTo(ONE) > 0) throw BizException.of(ErrorCode.PARAM_ERROR, field + " must be between 0 and 1"); return value; }
    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) { return value == null ? fallback : value; }
    private BigDecimal positive(BigDecimal value) { return value.signum() < 0 ? ZERO : value; }
    private BigDecimal divide(BigDecimal value, BigDecimal divisor) { return divisor == null || divisor.signum() == 0 ? ZERO : value.divide(divisor, 6, RoundingMode.HALF_UP); }
    private BigDecimal scale(BigDecimal value) { return value.setScale(4, RoundingMode.HALF_UP); }
    private BigDecimal scale6(BigDecimal value) { return value.setScale(6, RoundingMode.HALF_UP); }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String defaultText(String value, String fallback) { String text = text(value); return text == null ? fallback : text; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("cannot serialize career snapshot", e); } }
    private List<Long> readList(String value) { try { return objectMapper.readValue(value, new TypeReference<List<Long>>(){}); } catch (Exception e) { return List.of(); } }
    private Map<String, Object> readMap(String value) { try { return objectMapper.readValue(value, new TypeReference<Map<String, Object>>(){}); } catch (Exception e) { return Map.of(); } }

    private static class Candidate {
        private final Long courseId; private final String courseCode; private final String courseName;
        private final Set<Long> skillIds = new TreeSet<>(); private final Set<Long> knowledgePointIds = new TreeSet<>(); private final Set<Long> coreSkills = new HashSet<>(); private final Map<Long, BigDecimal> coveredPriority = new HashMap<>();
        private BigDecimal score = ZERO; private BigDecimal coverage = ZERO; private BigDecimal coreCoverage = ZERO; private Map<String,Object> reason = Map.of();
        private Candidate(CareerCourseCoverage row) { courseId = row.getCourseId(); courseCode = row.getCourseCode(); courseName = row.getCourseName(); }
    }
}
