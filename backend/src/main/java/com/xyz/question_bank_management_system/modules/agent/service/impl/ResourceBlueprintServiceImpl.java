package com.xyz.question_bank_management_system.modules.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.exception.*;
import com.xyz.question_bank_management_system.modules.agent.entity.*;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.agent.service.ResourceBlueprintService;
import com.xyz.question_bank_management_system.modules.agent.tool.*;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathV1Mapper;
import com.xyz.question_bank_management_system.modules.profile.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.util.*;

/**
 * Blueprint is the only agent allowed to read the learner profile. It must never
 * invent a mastery value: a knowledge point without verified profile evidence
 * fails the job with PROFILE_EVIDENCE_MISSING rather than silently defaulting.
 */
@Service
@RequiredArgsConstructor
public class ResourceBlueprintServiceImpl implements ResourceBlueprintService {

    private final PersonalizedResourceMapper mapper;
    private final LearningPathV1Mapper pathMapper;
    private final KnowledgeStateQueryTool knowledgeTool;
    private final ResourcePreferenceQueryTool preferenceTool;
    private final KnowledgeGraphSearchTool graphTool;
    private final CourseResourceCapabilityQueryTool capabilityTool;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResourceBlueprint design(Long studentId, ResourceUnit unit) {
        List<Long> ids = unitKnowledgePointIds(unit);
        if (ids.isEmpty()) throw BizException.of(ErrorCode.FAILED_INPUT, "ResourceUnit 没有路径步骤");

        Map<String, Object> states = knowledgeTool.query(studentId, unit.getCourseId(), ids);
        Map<String, Object> preferences = preferenceTool.query(studentId, unit.getCourseId());
        Map<String, Object> graph = graphTool.query(unit.getCourseId(), ids);
        List<String> capabilities = capabilityTool.query(unit.getCourseId());
        if (capabilities == null || capabilities.isEmpty()) {
            throw BizException.of(ErrorCode.FAILED_INPUT, "课程未启用任何资源生成能力，无法设计蓝图");
        }

        Map<Long, BigDecimal> mastery = masteryByKnowledgePoint(states);
        List<Long> missing = ids.stream().filter(id -> mastery.get(id) == null).toList();
        if (!missing.isEmpty()) {
            throw BizException.of(ErrorCode.PROFILE_EVIDENCE_MISSING,
                    "缺少知识点画像证据，无法设计蓝图：knowledgePointIds=" + missing);
        }

        String resourceType = capabilities.contains("concept_explanation") ? "concept_explanation" : capabilities.get(0);
        List<Map<String, Object>> resourcePlan = new ArrayList<>();
        List<Map<String, Object>> learning = new ArrayList<>();
        List<Map<String, Object>> hidden = new ArrayList<>();
        int slot = 0;
        for (Long id : ids) {
            BigDecimal m = mastery.get(id);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("resourceSlotId", "rs_" + (++slot));
            r.put("resourceType", resourceType);
            r.put("knowledgePointIds", List.of(id));
            r.put("instructionalPurpose", "建立或强化当前知识表征");
            r.put("scaffoldingLevel", m.compareTo(new BigDecimal("0.50")) < 0 ? "HIGH" : "MEDIUM");
            r.put("selectionReason", "mastery=" + m + "，结合教学作用、资源偏好与课程能力选择");
            r.put("profileEvidence", List.of("knowledgePoint=" + id + ",mastery=" + m));
            resourcePlan.add(r);

            BigDecimal difficulty = BigDecimal.ONE.subtract(m)
                    .max(new BigDecimal("0.20")).min(new BigDecimal("0.90"))
                    .setScale(4, RoundingMode.HALF_UP);
            learning.add(question("lq_" + slot, "VISIBLE", "LEARNING_PRACTICE", id, ids,
                    "single_choice", "UNDERSTAND", difficulty, false));
            hidden.add(question("ha_" + slot, "HIDDEN_UNTIL_ASSESSMENT", "KNOWLEDGE_ASSESSMENT", id, ids,
                    "short_answer", "APPLY", difficulty, true));
        }

        try {
            ResourceBlueprint b = new ResourceBlueprint();
            b.setBlueprintCode("bp_" + UUID.randomUUID().toString().replace("-", ""));
            b.setResourceUnitId(unit.getId());
            b.setProfileVersionUsed(((Number) states.getOrDefault("profileVersion", 0)).longValue());
            b.setPolicyVersion("resource_policy_v1");
            b.setStatus("READY");
            b.setResourcePlanJson(objectMapper.writeValueAsString(resourcePlan));
            b.setLearningQuestionPlanJson(objectMapper.writeValueAsString(learning));
            b.setHiddenAssessmentPlanJson(objectMapper.writeValueAsString(hidden));
            b.setProfileEvidenceJson(objectMapper.writeValueAsString(Map.of("knowledge", states, "preferences", preferences)));
            b.setGraphEvidenceJson(objectMapper.writeValueAsString(graph));
            b.setCapabilitySnapshotJson(objectMapper.writeValueAsString(capabilities));
            b.setSchemaVersion("personalized-resource-blueprint-v1");
            b.setCorrelationId(CorrelationContext.get());
            mapper.insertBlueprint(b);
            return b;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("蓝图序列化失败", ex);
        }
    }

    /** Steps consumed by this unit, in path order, resolved to their knowledge points. */
    private List<Long> unitKnowledgePointIds(ResourceUnit unit) {
        Set<Long> stepIds = new HashSet<>();
        for (ResourceUnitStep step : mapper.selectUnitSteps(unit.getId())) stepIds.add(step.getPathStepId());
        if (stepIds.isEmpty()) return List.of();
        return pathMapper.selectSteps(unit.getPathVersionId()).stream()
                .filter(x -> stepIds.contains(x.getId()))
                .sorted(Comparator.comparing(LearningPathItem::getOrderNo))
                .map(LearningPathItem::getKnowledgePointId)
                .toList();
    }

    private Map<Long, BigDecimal> masteryByKnowledgePoint(Map<String, Object> states) {
        Map<Long, BigDecimal> mastery = new HashMap<>();
        if (states.get("states") instanceof Collection<?> rows) {
            for (Object row : rows) {
                if (row instanceof StudentKnowledgeState s && s.getMasteryValue() != null) {
                    mastery.put(s.getKnowledgePointId(), s.getMasteryValue());
                }
            }
        }
        return mastery;
    }

    private Map<String, Object> question(String slot, String visibility, String purpose, Long primary, List<Long> ids,
                                         String type, String cognitive, BigDecimal difficulty, boolean hidden) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionSlotId", slot);
        q.put("visibility", visibility);
        q.put("purpose", purpose);
        q.put("primaryKnowledgePointId", primary);
        q.put("knowledgePointIds", ids);
        q.put("questionType", type);
        q.put("questionDifficulty", difficulty);
        q.put("cognitiveLevel", cognitive);
        q.put("cognitiveDefinition", "UNDERSTAND".equals(cognitive) ? "从教学信息中建构意义。" : "在给定或新情境中使用已有知识。");
        q.put("requiredCognitiveActions", List.of("UNDERSTAND".equals(cognitive) ? "解释与比较" : "识别情境并应用知识"));
        q.put("cognitiveLevelReason", "依据当前掌握状态和测量目标确定");
        q.put("assessmentObjective", "获取知识点 " + primary + " 的" + (hidden ? "独立应用" : "理解") + "证据");
        if (hidden) q.put("noveltyRequirement", "不得复用普通题库、可见练习、例题或历史检测题");
        return q;
    }
}
