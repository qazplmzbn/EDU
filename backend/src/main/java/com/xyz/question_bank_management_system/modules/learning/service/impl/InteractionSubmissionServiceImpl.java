package com.xyz.question_bank_management_system.modules.learning.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.config.CorrelationContext;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceAssessmentRelease;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceBundle;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceItem;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceUnit;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.agent.service.ResourceGenerationWorkflow;
import com.xyz.question_bank_management_system.modules.course.service.PathRefreshApplicationService;
import com.xyz.question_bank_management_system.modules.learning.dto.ResourceInteractionSubmitRequest;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathProgress;
import com.xyz.question_bank_management_system.modules.learning.entity.OutboxEvent;
import com.xyz.question_bank_management_system.modules.learning.entity.ResourceInteraction;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathV1Mapper;
import com.xyz.question_bank_management_system.modules.learning.service.InteractionSubmissionService;
import com.xyz.question_bank_management_system.modules.learning.service.ProfileEvidenceConsumer;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionSubmissionServiceImpl implements InteractionSubmissionService {
    private static final Set<String> ACTION_ORIGINS = Set.of("USER_INITIATED", "SYSTEM_RECOMMENDED", "SYSTEM_REQUIRED");

    private final PersonalizedResourceMapper mapper;
    private final LearningPathV1Mapper pathMapper;
    private final ProfileEvidenceConsumer profileConsumer;
    private final PathRefreshApplicationService pathService;
    private final ResourceGenerationWorkflow resourceWorkflow;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Map<String, Object> submit(Long userId, String requestId, ResourceInteractionSubmitRequest request) {
        requireRequest(requestId, request);
        ResourceInteraction existing = mapper.selectInteractionByRequest(requestId);
        if (existing != null) {
            return replay(existing, userId);
        }

        ResourceItem item = requireQuestion(request.getGeneratedQuestionCode());
        ResourceBundle bundle = requirePublishedBundle(item, userId);
        ResourceAssessmentRelease release = requireReleaseIfHidden(item, userId);
        if (!Set.of("LEARNING_PRACTICE", "KNOWLEDGE_ASSESSMENT").contains(item.getPurpose())) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "该资源项不是生成习题");
        }

        ResourceUnit unit = mapper.selectUnitById(bundle.getResourceUnitId());
        if (unit == null) throw BizException.of(ErrorCode.CONFLICT, "资源包缺少路径单元");
        LearningPath path = pathMapper.selectPathById(unit.getPathId());
        if (path == null || !Objects.equals(path.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "资源包不属于当前学习路径");
        }
        LearningPathProgress progress = pathMapper.selectProgressForUpdate(path.getId());
        if (progress == null) throw BizException.of(ErrorCode.CONFLICT, "学习路径缺少进度记录");

        Map<Long, BigDecimal> weights = knowledgeWeights(item.getId());
        Long primaryKnowledgePointId = primaryKnowledgePoint(item.getId());
        Grade grade = grade(item.getGradingKeyJson(), request.getAnswer());
        long sequence = Objects.requireNonNullElse(progress.getLastProcessedInteractionSeq(), 0L) + 1;
        ResourceInteraction interaction = buildInteraction(userId, requestId, request, item, bundle, unit, sequence, primaryKnowledgePointId, weights, grade);

        if (mapper.insertInteraction(interaction) != 1) {
            ResourceInteraction raced = mapper.selectInteractionByRequestForUpdate(requestId);
            if (raced != null) return replay(raced, userId);
            throw BizException.of(ErrorCode.CONFLICT, "interaction request is being processed; retry with the same Idempotency-Key");
        }

        OutboxEvent outbox = newOutbox(interaction);
        mapper.insertOutbox(outbox);
        try {
            StudentProfileSnapshot snapshot = profileConsumer.apply(outbox.getEventId(), validated(interaction, weights));
            if (mapper.markOutboxConsumed(outbox.getId()) != 1) {
                throw BizException.of(ErrorCode.CONFLICT, "outbox 状态已变化");
            }
            pathMapper.advanceProgress(path.getId(), sequence, interaction.getId(), interaction.getCorrect());
            Map<String, Object> pathResult = pathService.evaluateEvent(path.getPathCode(), Map.of(
                    "type", grade.correct() ? "ANSWER_CORRECT" : "ANSWER_WRONG",
                    "knowledgePointId", primaryKnowledgePointId));
            String event = String.valueOf(pathResult.getOrDefault("result", "NO_PATH_EVENT"));
            String reason = "ACTIVE_UNCHANGED".equals(event) || "NO_PATH_EVENT".equals(event)
                    ? "PROFILE_VALUE_ONLY" : grade.correct() ? "RESOURCE_COMPLETED" : "CONSECUTIVE_WRONG";
            String action = String.valueOf(resourceWorkflow.decide(reason, true).get("resourceAction"));
            mapper.insertResourceDecision(userId, bundle.getCourseId(), path.getId(), path.getCurrentVersion(), interaction.getId(), action, reason, CorrelationContext.get());
            if (release != null && mapper.consumeRelease(release.getId()) != 1) {
                throw BizException.of(ErrorCode.CONFLICT, "隐藏检测题释放状态已变化");
            }
            return view(interaction, snapshot, action);
        } catch (RuntimeException error) {
            // This transaction rolls back the interaction and its PENDING
            // outbox row together. A FAILED state is meaningful only for a
            // separately committed external-delivery attempt.
            throw error;
        }
    }

    @Override
    public Map<String, Object> result(Long userId, String interactionCode) {
        ResourceInteraction interaction = mapper.selectInteraction(interactionCode, userId);
        if (interaction == null) throw BizException.of(ErrorCode.NOT_FOUND, "交互不存在");
        return view(interaction, null, null);
    }

    @Override
    @Transactional
    public Map<String, Object> applyProfileEvidence(Long interactionId) {
        ResourceInteraction interaction = mapper.selectInteractionById(interactionId);
        if (interaction == null) throw BizException.of(ErrorCode.NOT_FOUND, "交互不存在");
        try {
            Map<Long, BigDecimal> weights = objectMapper.readValue(interaction.getKnowledgePointWeightsJson(), new TypeReference<>() {});
            StudentProfileSnapshot snapshot = profileConsumer.apply("profile-interaction-" + interaction.getId(), validated(interaction, weights));
            return view(interaction, snapshot, "PROFILE_APPLIED");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "交互证据无法应用：" + ex.getMessage());
        }
    }

    private void requireRequest(String requestId, ResourceInteractionSubmitRequest request) {
        if (!StringUtils.hasText(requestId)) throw BizException.of(ErrorCode.PARAM_ERROR, "Idempotency-Key 不能为空");
        if (request == null || !StringUtils.hasText(request.getGeneratedQuestionCode())) throw BizException.of(ErrorCode.PARAM_ERROR, "generatedQuestionCode 不能为空");
        if (request.getAnswer() == null) throw BizException.of(ErrorCode.PARAM_ERROR, "answer 不能为空");
        if (!StringUtils.hasText(request.getActionOrigin()) || !ACTION_ORIGINS.contains(request.getActionOrigin().trim().toUpperCase(Locale.ROOT))) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "actionOrigin 不合法");
        }
    }

    private ResourceItem requireQuestion(String code) {
        ResourceItem item = mapper.selectQuestionForUpdate(code);
        if (item == null || item.getGeneratedQuestionCode() == null) throw BizException.of(ErrorCode.NOT_FOUND, "生成题不存在");
        return item;
    }

    private ResourceBundle requirePublishedBundle(ResourceItem item, Long userId) {
        ResourceBundle bundle = mapper.selectBundleByIdForUpdate(item.getBundleId());
        if (bundle == null || !"PUBLISHED".equals(bundle.getStatus()) || !Objects.equals(bundle.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只有归属当前学生的 PUBLISHED 资源可提交");
        }
        return bundle;
    }

    private ResourceAssessmentRelease requireReleaseIfHidden(ResourceItem item, Long userId) {
        if (!"HIDDEN_UNTIL_ASSESSMENT".equals(item.getVisibility())) return null;
        ResourceAssessmentRelease release = mapper.selectActiveRelease(userId, item.getId());
        if (release == null) throw BizException.of(ErrorCode.FORBIDDEN, "隐藏检测题尚未释放或已过期");
        return release;
    }

    private Map<Long, BigDecimal> knowledgeWeights(Long itemId) {
        Map<Long, BigDecimal> weights = new LinkedHashMap<>();
        for (Map<String, Object> row : mapper.itemKnowledge(itemId)) {
            Object id = row.containsKey("knowledge_point_id") ? row.get("knowledge_point_id") : row.get("knowledgePointId");
            Object weight = row.containsKey("coverage_weight") ? row.get("coverage_weight") : row.get("coverageWeight");
            weights.put(((Number) id).longValue(), weight instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(weight)));
        }
        if (weights.isEmpty()) throw BizException.of(ErrorCode.EVIDENCE_INSUFFICIENT, "生成题缺少冻结知识点权重");
        return weights;
    }

    private Long primaryKnowledgePoint(Long itemId) {
        for (Map<String, Object> row : mapper.itemKnowledge(itemId)) {
            Object primary = row.containsKey("is_primary") ? row.get("is_primary") : row.get("isPrimary");
            if (primary != null && Integer.parseInt(String.valueOf(primary)) == 1) {
                Object id = row.containsKey("knowledge_point_id") ? row.get("knowledge_point_id") : row.get("knowledgePointId");
                return ((Number) id).longValue();
            }
        }
        throw BizException.of(ErrorCode.EVIDENCE_INSUFFICIENT, "生成题缺少主知识点");
    }

    private Grade grade(String gradingJson, Object answer) {
        if (!StringUtils.hasText(gradingJson)) throw BizException.of(ErrorCode.EVIDENCE_INSUFFICIENT, "生成题缺少 grading key");
        try {
            JsonNode key = objectMapper.readTree(gradingJson);
            String expected = key.isTextual() ? key.asText() : key.path("standardAnswer").asText();
            String actual = answer instanceof String text ? text : objectMapper.writeValueAsString(answer);
            boolean correct = normalize(expected).equals(normalize(actual));
            return new Grade(correct, correct ? BigDecimal.ONE : BigDecimal.ZERO);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "生成题评分规则无效");
        }
    }

    private ResourceInteraction buildInteraction(Long userId, String requestId, ResourceInteractionSubmitRequest request, ResourceItem item, ResourceBundle bundle, ResourceUnit unit, long sequence, Long primaryKnowledgePointId, Map<Long, BigDecimal> weights, Grade grade) {
        try {
            ResourceInteraction value = new ResourceInteraction();
            value.setInteractionCode("int_" + UUID.randomUUID().toString().replace("-", ""));
            value.setInteractionSeq(sequence); value.setUserId(userId); value.setCourseId(bundle.getCourseId()); value.setResourceBundleId(bundle.getId()); value.setResourceVersion(bundle.getVersion()); value.setResourceUnitId(unit.getId()); value.setResourceItemId(item.getId()); value.setGeneratedQuestionCode(item.getGeneratedQuestionCode()); value.setQuestionPurpose(item.getPurpose()); value.setVisibility(item.getVisibility()); value.setQuestionDifficulty(item.getQuestionDifficulty()); value.setPrimaryKnowledgePointId(primaryKnowledgePointId); value.setKnowledgePointWeightsJson(objectMapper.writeValueAsString(weights)); value.setScoreNormalized(grade.score()); value.setCorrect(grade.correct() ? 1 : 0); value.setStatus("SUBMITTED"); value.setActionOrigin(request.getActionOrigin().trim().toUpperCase(Locale.ROOT)); value.setGradingVersion("grading_v1"); value.setAnswerJson(objectMapper.writeValueAsString(request.getAnswer())); value.setRequestId(requestId); value.setCorrelationId(CorrelationContext.get()); value.setClientOccurredAt(request.getClientOccurredAt() == null ? null : request.getClientOccurredAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
            return value;
        } catch (Exception ex) { throw new IllegalStateException("交互序列化失败", ex); }
    }

    private OutboxEvent newOutbox(ResourceInteraction interaction) {
        try {
            OutboxEvent value = new OutboxEvent(); value.setEventId("evt_" + UUID.randomUUID().toString().replace("-", "")); value.setAggregateType("RESOURCE_INTERACTION"); value.setAggregateId(interaction.getId()); value.setEventType("learning.interaction.submitted"); value.setPayloadJson(objectMapper.writeValueAsString(Map.of("interactionId", interaction.getId(), "userId", interaction.getUserId(), "courseId", interaction.getCourseId(), "interactionSeq", interaction.getInteractionSeq()))); value.setStatus("PENDING"); value.setCorrelationId(CorrelationContext.get()); return value;
        } catch (Exception ex) { throw new IllegalStateException("outbox 序列化失败", ex); }
    }

    private ValidatedInteraction validated(ResourceInteraction interaction, Map<Long, BigDecimal> weights) {
        ValidatedInteraction value = new ValidatedInteraction(); value.setId(interaction.getId()); value.setInteractionSeq(interaction.getInteractionSeq()); value.setUserId(interaction.getUserId()); value.setCourseId(interaction.getCourseId()); value.setScoreNormalized(interaction.getScoreNormalized()); value.setQuestionDifficulty(interaction.getQuestionDifficulty()); value.setQuestionPurpose(interaction.getQuestionPurpose()); value.setGradingConfidence(BigDecimal.ONE); value.setKnowledgeWeights(weights); return value;
    }

    private Map<String, Object> replay(ResourceInteraction interaction, Long userId) {
        if (!Objects.equals(interaction.getUserId(), userId)) throw BizException.of(ErrorCode.CONFLICT, "幂等键已被其他请求使用");
        return view(interaction, null, "REUSE");
    }

    private Map<String, Object> view(ResourceInteraction interaction, StudentProfileSnapshot snapshot, String action) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("interactionCode", interaction.getInteractionCode()); result.put("scoreNormalized", interaction.getScoreNormalized()); result.put("correct", Objects.equals(interaction.getCorrect(), 1)); result.put("status", interaction.getStatus()); if (snapshot != null) { result.put("profileVersion", snapshot.getProfileVersion()); result.put("masteryUpdated", true); } if (action != null) result.put("resourceAction", action); return result;
    }

    private String normalize(String value) { return Objects.toString(value, "").replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT); }
    private record Grade(boolean correct, BigDecimal score) {}
}
