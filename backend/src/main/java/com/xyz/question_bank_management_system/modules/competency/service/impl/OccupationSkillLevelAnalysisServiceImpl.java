package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.entity.Occupation;
import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkill;
import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkillLevelAnalysis;
import com.xyz.question_bank_management_system.modules.competency.mapper.CareerRecommendationMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationSkillLevelAnalysisMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationSkillMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillMapper;
import com.xyz.question_bank_management_system.modules.competency.service.OccupationSkillLevelAnalysisService;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** A batch freezes the entire occupation-skill universe before the first model call. */
@Service
@RequiredArgsConstructor
public class OccupationSkillLevelAnalysisServiceImpl implements OccupationSkillLevelAnalysisService {
    private static final Set<String> LEVELS = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
    private static final Map<String, BigDecimal> LEVEL_VALUES = Map.of(
            "BEGINNER", new BigDecimal("0.3500"), "INTERMEDIATE", new BigDecimal("0.6500"), "ADVANCED", new BigDecimal("0.8500"));

    private final OccupationMapper occupationMapper;
    private final OccupationSkillMapper occupationSkillMapper;
    private final SkillMapper skillMapper;
    private final OccupationSkillLevelAnalysisMapper mapper;
    private final CareerRecommendationMapper recommendationMapper;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OccupationSkillLevelAnalysis analyze(Long occupationId, String providerKey, Long operatorId) {
        Occupation occupation = requireOccupation(occupationId);
        List<OccupationSkill> universe = occupationSkillMapper.selectByOccupationIdForUpdate(occupationId);
        if (universe.isEmpty()) throw BizException.of(ErrorCode.CONFLICT, "occupation has no skills");
        String batchCode = "OSLA-" + UUID.randomUUID().toString().replace("-", "");
        String input = frozenInput(occupation, universe);
        Set<Long> expectedIds = universe.stream().map(OccupationSkill::getId).collect(Collectors.toCollection(TreeSet::new));

        OccupationSkillLevelAnalysis first = runRound(batchCode, occupationId, 1, providerKey, operatorId, input, expectedIds);
        Map<Long, String> one = parseLevels(first, expectedIds);
        if (one == null) return first;
        OccupationSkillLevelAnalysis second = runRound(batchCode, occupationId, 2, providerKey, operatorId, input, expectedIds);
        Map<Long, String> two = parseLevels(second, expectedIds);
        if (two == null) return second;
        if (one.equals(two)) {
            mapper.updateBatchStatus(occupationId, batchCode, "CONSENSUS_READY", null);
            second.setStatus("CONSENSUS_READY");
            return second;
        }
        OccupationSkillLevelAnalysis third = runRound(batchCode, occupationId, 3, providerKey, operatorId, input, expectedIds);
        Map<Long, String> three = parseLevels(third, expectedIds);
        if (three == null) return third;
        if (majority(one, two, three) == null) {
            String error = "three analysis rounds contain no majority for every frozen occupation skill";
            mapper.updateBatchStatus(occupationId, batchCode, "FAILED", error);
            third.setStatus("FAILED");
            third.setErrorMessage(error);
            return third;
        }
        mapper.updateBatchStatus(occupationId, batchCode, "CONSENSUS_READY", null);
        third.setStatus("CONSENSUS_READY");
        return third;
    }

    @Override
    public List<OccupationSkillLevelAnalysis> list(Long occupationId, int limit) {
        return mapper.list(occupationId, Math.max(1, Math.min(100, limit)));
    }

    @Override
    @Transactional
    public void publishConsensus(Long occupationId, String batchCode, Long operatorId) {
        if (batchCode == null || batchCode.isBlank()) throw BizException.of(ErrorCode.PARAM_ERROR, "batchCode is required");
        requireOccupation(occupationId);
        List<OccupationSkill> liveUniverse = occupationSkillMapper.selectByOccupationIdForUpdate(occupationId);
        if (liveUniverse.isEmpty()) throw BizException.of(ErrorCode.CONFLICT, "occupation has no skills");
        List<OccupationSkillLevelAnalysis> analyses = mapper.selectBatchForUpdate(occupationId, batchCode.trim());
        if (analyses.size() < 2 || analyses.size() > 3 || analyses.stream().anyMatch(a -> !"CONSENSUS_READY".equals(a.getStatus()))) {
            throw BizException.of(ErrorCode.CONFLICT, "level analysis batch is not consensus-ready");
        }
        Set<Long> expectedIds = frozenUniverse(analyses.get(0));
        Set<Long> liveIds = liveUniverse.stream().map(OccupationSkill::getId).collect(Collectors.toCollection(TreeSet::new));
        if (!expectedIds.equals(liveIds)) throw BizException.of(ErrorCode.CONFLICT, "occupation skill universe changed after analysis; rerun consensus");
        List<Map<Long, String>> rounds = new ArrayList<>();
        for (OccupationSkillLevelAnalysis analysis : analyses) rounds.add(parseLevelsOrThrow(analysis, expectedIds));
        Map<Long, String> agreed = rounds.size() == 2 ? equalOrThrow(rounds.get(0), rounds.get(1)) : majorityOrThrow(rounds.get(0), rounds.get(1), rounds.get(2));
        Map<Long, OccupationSkill> byId = new HashMap<>();
        liveUniverse.forEach(row -> byId.put(row.getId(), row));
        for (Long occupationSkillId : expectedIds) {
            OccupationSkill row = byId.get(occupationSkillId);
            recommendationMapper.publishOccupationSkill(row.getId(), LEVEL_VALUES.get(agreed.get(row.getId())),
                    row.getImportanceScore() == null ? BigDecimal.ONE : row.getImportanceScore(), row.getRequirementType(),
                    "ANALYSIS_CONSENSUS", "job_skill_level_consensus_v1", batchCode.trim());
        }
    }

    private OccupationSkillLevelAnalysis runRound(String batchCode, Long occupationId, int round, String providerKey, Long operatorId, String input, Set<Long> expectedIds) {
        OccupationSkillLevelAnalysis record = new OccupationSkillLevelAnalysis();
        record.setBatchCode(batchCode); record.setOccupationId(occupationId); record.setRoundNo(round);
        record.setProviderKey(providerKey); record.setInputJson(input); record.setCreatedBy(operatorId);
        String prompt = "Return strict JSON only: {\"levels\":[{\"occupationSkillId\":number,\"level\":\"BEGINNER|INTERMEDIATE|ADVANCED\",\"reason\":string}]}. "
                + "Assess every supplied occupationSkillId exactly once. Do not add or omit ids. This is analysis only; do not publish. Frozen input=" + input;
        try {
            QbLlmCall call = llmService.chatCompletion(5, occupationId, prompt, providerKey, operatorId);
            record.setModelName(call.getModelName());
            String content = llmService.extractContent(call.getResponseText());
            record.setOutputJson(content);
            if (call.getCallStatus() == null || call.getCallStatus() != 1 || content == null) {
                record.setStatus("FAILED"); record.setErrorMessage("provider failed");
            } else if (parseLevelsContent(content, expectedIds) == null) {
                record.setStatus("FAILED"); record.setErrorMessage("model output does not cover the frozen occupation skill universe");
            } else record.setStatus("PENDING_REVIEW");
        } catch (Exception ex) {
            record.setStatus("FAILED"); record.setErrorMessage(shortError(ex));
        }
        mapper.insert(record);
        return record;
    }

    private String frozenInput(Occupation occupation, List<OccupationSkill> universe) {
        List<Map<String, Object>> skills = universe.stream().sorted(Comparator.comparing(OccupationSkill::getId)).map(row -> {
            var skill = skillMapper.selectById(row.getSkillId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("occupationSkillId", row.getId()); item.put("skillId", row.getSkillId());
            item.put("skillName", skill == null ? "" : skill.getNameZh()); item.put("requirementType", row.getRequirementType());
            item.put("importance", row.getImportanceScore()); return item;
        }).toList();
        return json(Map.of("occupationId", occupation.getId(), "occupationName", occupation.getNameZh(), "skills", skills));
    }

    private Set<Long> frozenUniverse(OccupationSkillLevelAnalysis analysis) {
        try {
            JsonNode skills = objectMapper.readTree(analysis.getInputJson()).path("skills");
            Set<Long> ids = new TreeSet<>();
            if (!skills.isArray()) throw new IllegalArgumentException();
            for (JsonNode skill : skills) if (!skill.path("occupationSkillId").isIntegralNumber() || !ids.add(skill.get("occupationSkillId").asLong())) throw new IllegalArgumentException();
            if (ids.isEmpty()) throw new IllegalArgumentException();
            return ids;
        } catch (Exception ex) { throw BizException.of(ErrorCode.CONFLICT, "analysis batch has an invalid frozen occupation skill universe"); }
    }
    private Map<Long, String> parseLevels(OccupationSkillLevelAnalysis analysis, Set<Long> expectedIds) { return "FAILED".equals(analysis.getStatus()) ? null : parseLevelsContent(analysis.getOutputJson(), expectedIds); }
    private Map<Long, String> parseLevelsOrThrow(OccupationSkillLevelAnalysis analysis, Set<Long> expectedIds) { Map<Long, String> values = parseLevelsContent(analysis.getOutputJson(), expectedIds); if (values == null) throw BizException.of(ErrorCode.CONFLICT, "analysis batch has invalid round output"); return values; }
    private Map<Long, String> parseLevelsContent(String content, Set<Long> expectedIds) {
        try {
            JsonNode levels = objectMapper.readTree(content).path("levels");
            if (!levels.isArray() || levels.size() != expectedIds.size()) return null;
            Map<Long, String> result = new TreeMap<>();
            for (JsonNode level : levels) {
                if (!level.path("occupationSkillId").isIntegralNumber()) return null;
                long id = level.get("occupationSkillId").asLong(); String value = level.path("level").asText();
                if (!expectedIds.contains(id) || !LEVELS.contains(value) || result.put(id, value) != null) return null;
            }
            return result.keySet().equals(expectedIds) ? result : null;
        } catch (Exception ex) { return null; }
    }
    private Map<Long, String> equalOrThrow(Map<Long, String> one, Map<Long, String> two) { if (!one.equals(two)) throw BizException.of(ErrorCode.CONFLICT, "two-round level analysis does not agree"); return one; }
    private Map<Long, String> majorityOrThrow(Map<Long, String> one, Map<Long, String> two, Map<Long, String> three) { Map<Long, String> values = majority(one, two, three); if (values == null) throw BizException.of(ErrorCode.CONFLICT, "three-round level analysis has no majority"); return values; }
    private Map<Long, String> majority(Map<Long, String> one, Map<Long, String> two, Map<Long, String> three) {
        Map<Long, String> result = new TreeMap<>();
        for (Long id : one.keySet()) { String a = one.get(id), b = two.get(id), c = three.get(id); if (Objects.equals(a, b) || Objects.equals(a, c)) result.put(id, a); else if (Objects.equals(b, c)) result.put(id, b); else return null; }
        return result;
    }
    private Occupation requireOccupation(Long occupationId) { Occupation occupation = occupationId == null ? null : occupationMapper.selectById(occupationId); if (occupation == null) throw BizException.of(ErrorCode.NOT_FOUND, "occupation not found"); return occupation; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("cannot serialize level analysis input", e); } }
    private String shortError(Exception e) { String message = Objects.toString(e.getMessage(), e.getClass().getSimpleName()); return message.length() > 900 ? message.substring(0, 900) : message; }
}
