package com.xyz.question_bank_management_system.modules.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.agent.dto.TeacherAgentResourceGenerateRequest;
import com.xyz.question_bank_management_system.modules.agent.entity.*;
import com.xyz.question_bank_management_system.modules.agent.mapper.*;
import com.xyz.question_bank_management_system.modules.agent.service.AgentTaskPersistenceService;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceGenerateVO;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.entity.ResourceKnowledge;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.ResourceKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgentTaskPersistenceServiceImpl implements AgentTaskPersistenceService {
    private final AgentTaskMapper taskMapper;
    private final AgentStepMapper stepMapper;
    private final AgentReviewMapper reviewMapper;
    private final AgentDecisionMapper decisionMapper;
    private final AgentDefinitionMapper definitionMapper;
    private final QbLearningResourceMapper learningResourceMapper;
    private final ResourceKnowledgeMapper resourceKnowledgeMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AgentTask createResourceTask(Long teacherId, Long studentId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        AgentTask task = new AgentTask();
        task.setTaskCode("teacher-agent-task-" + UUID.randomUUID());
        task.setTaskType("resource_generate");
        task.setTeacherId(teacherId);
        task.setUserId(studentId);
        task.setTargetType("resource");
        task.setInputJson(json(Map.of("request", request, "admin", admin)));
        task.setStatus("queued");
        task.setCurrentStepNo(0);
        taskMapper.insert(task);
        return task;
    }

    @Override
    public AgentTask requireReadable(String taskCode, Long teacherId, boolean admin) {
        AgentTask task = taskMapper.selectByCode(taskCode);
        if (task == null) throw BizException.of(ErrorCode.NOT_FOUND, "任务不存在");
        if (!admin && !Objects.equals(task.getTeacherId(), teacherId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权查看该任务");
        }
        return task;
    }

    @Override
    public void markRunning(Long taskId) { taskMapper.claim(taskId); }

    @Override
    public void attachProfileSnapshot(Long taskId, Object profile) {
        AgentTask task = taskMapper.selectById(taskId);
        if (task == null) return;
        try {
            Map<String, Object> input = objectMapper.readValue(task.getInputJson(), new TypeReference<>() {});
            input.put("profileSnapshot", profile);
            taskMapper.updateInput(taskId, json(input));
        } catch (Exception ignored) {
            // A task can still execute with its immutable request if an old payload cannot be decoded.
        }
    }

    @Override
    @Transactional
    public void complete(AgentTask task, TeacherAgentResourceGenerateVO result) {
        List<TeacherAgentResourceGenerateVO.AgentTrace> traces = result.getAgentTrace() == null ? List.of() : result.getAgentTrace();
        int stepNo = 0;
        Long reviewStepId = null;
        for (TeacherAgentResourceGenerateVO.AgentTrace trace : traces) {
            stepNo++;
            AgentStep step = new AgentStep();
            step.setAgentTaskId(task.getId());
            step.setStepNo(stepNo);
            step.setAgentDefinitionId(definitionId(trace.getAgentId()));
            step.setStepType(stepType(trace.getAgentId()));
            step.setInputJson(task.getInputJson());
            step.setOutputJson(json(Map.of("agent", nullSafe(trace.getAgentId()), "summary", nullSafe(trace.getSummary()), "model", nullSafe(trace.getModelName()))));
            step.setLlmCallId(trace.getLlmCallId());
            step.setStatus("success".equalsIgnoreCase(trace.getStatus()) ? "success" : "failed");
            step.setStartedAt(LocalDateTime.now());
            step.setFinishedAt(LocalDateTime.now());
            stepMapper.upsert(step);
            if ("review".equals(step.getStepType())) reviewStepId = step.getId();
        }
        boolean allPassed = true;
        if (result.getResources() != null) {
            for (TeacherAgentResourceGenerateVO.ResourceDraft resource : result.getResources()) {
                TeacherAgentResourceGenerateVO.ReviewReport source = resource.getReviewReport();
                boolean passed = source != null && Boolean.TRUE.equals(source.getPassed());
                allPassed &= passed;
                AgentReview review = new AgentReview();
                review.setAgentTaskId(task.getId());
                review.setAgentStepId(reviewStepId);
                review.setTargetType("resource");
                review.setReviewStatus(passed ? "pass" : "revise");
                review.setFactualScore(decimal(source == null ? null : source.getQualityScore()));
                review.setCoverageScore(decimal(source == null ? null : source.getRelevanceScore()));
                review.setDifficultyMatchScore(decimal(source == null ? null : source.getConsistencyScore()));
                review.setHallucinationRate(passed ? BigDecimal.ZERO : BigDecimal.ONE);
                review.setSourceConsistencyScore(decimal(source == null ? null : source.getConsistencyScore()));
                review.setReviewReport(json(source));
                reviewMapper.insert(review);
                if (passed && shouldPublish(task)) {
                    publishResource(task, resource);
                }
            }
        }
        AgentDecision decision = new AgentDecision();
        decision.setAgentTaskId(task.getId());
        decision.setAgentStepId(reviewStepId);
        decision.setDecisionType("resource_select");
        decision.setTargetType("resource");
        decision.setDecisionValue(allPassed ? "pass" : "revise");
        decision.setDecisionReason(result.getDecisionSummary() == null ? "资源生成已完成" : result.getDecisionSummary().getTeacherAction());
        decision.setConfidence(allPassed ? BigDecimal.ONE : new BigDecimal("0.5000"));
        decision.setEvidenceJson(json(result.getDecisionSummary()));
        decisionMapper.insert(decision);
        taskMapper.updateStatus(task.getId(), "completed", stepNo, json(result), null, 1);
    }

    @Override public void fail(Long taskId, String error) { taskMapper.updateStatus(taskId, "failed", 0, null, shorten(error), 1); }
    @Override public void cancel(Long taskId) { taskMapper.updateStatus(taskId, "canceled", 0, null, null, 1); }
    @Override public List<AgentStep> steps(String code, Long teacherId, boolean admin) { return stepMapper.selectByTaskId(requireReadable(code, teacherId, admin).getId()); }
    @Override public List<AgentReview> reviews(String code, Long teacherId, boolean admin) { return reviewMapper.selectByTaskId(requireReadable(code, teacherId, admin).getId()); }
    @Override public List<AgentDecision> decisions(String code, Long teacherId, boolean admin) { return decisionMapper.selectByTaskId(requireReadable(code, teacherId, admin).getId()); }
    @Override public List<AgentTask> recoverableTasks() { return taskMapper.selectRecoverable(); }
    @Override public void requeueInterruptedTasks() { taskMapper.requeueInterrupted(); }

    @Override
    public TeacherAgentResourceGenerateRequest requestOf(AgentTask task) {
        try {
            Map<String, Object> payload = objectMapper.readValue(task.getInputJson(), new TypeReference<>() {});
            return objectMapper.convertValue(payload.get("request"), TeacherAgentResourceGenerateRequest.class);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "持久化任务输入无效");
        }
    }

    @Override
    public boolean adminOf(AgentTask task) {
        try {
            Map<String, Object> payload = objectMapper.readValue(task.getInputJson(), new TypeReference<>() {});
            return Boolean.TRUE.equals(payload.get("admin"));
        } catch (Exception ignored) { return false; }
    }

    private Long definitionId(String agentId) {
        String code = switch (agentId == null ? "" : agentId.toLowerCase(Locale.ROOT)) {
            case "generator" -> "GENERATOR";
            case "quality-review", "consistency-review", "qualityreviewer", "consistencyreviewer" -> "REVIEWER";
            case "preprocess", "knowledge", "ability", "behavior" -> "DIAGNOSIS";
            default -> "PLANNER";
        };
        AgentDefinition definition = definitionMapper.selectEnabledLatest(code);
        return definition == null ? null : definition.getId();
    }

    private String stepType(String agentId) {
        String id = agentId == null ? "" : agentId.toLowerCase(Locale.ROOT);
        if (id.contains("review")) return "review";
        if (id.contains("generator")) return "generate";
        if (id.contains("decision") || id.contains("report")) return "decide";
        return "diagnose";
    }
    private BigDecimal decimal(Integer score) { return score == null ? null : BigDecimal.valueOf(score, 2); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { return "{}"; } }
    private String shorten(String value) { return value == null ? "未知错误" : value.length() > 1900 ? value.substring(0, 1900) : value; }
    private String nullSafe(String value) { return value == null ? "" : value; }

    private boolean shouldPublish(AgentTask task) {
        TeacherAgentResourceGenerateRequest request = requestOf(task);
        return "publish_after_review".equalsIgnoreCase(request.getPublishMode());
    }

    private void publishResource(AgentTask task, TeacherAgentResourceGenerateVO.ResourceDraft draft) {
        QbLearningResource resource = new QbLearningResource();
        resource.setTitle(draft.getTitle());
        resource.setResourceType(draft.getResourceType() == null ? "article" : draft.getResourceType());
        resource.setResourcePurpose("remedial");
        resource.setUrl(draft.getSourceUrl());
        resource.setSummary(draft.getSummary());
        resource.setContent(draft.getContent());
        resource.setDifficulty(3);
        resource.setGenerationType("agent");
        resource.setVersion("1");
        resource.setPersonalizationBasis(json(draft.getPersonalizationBasis()));
        resource.setReviewReportJson(json(draft.getReviewReport()));
        resource.setModelSourceJson(json(draft.getModelSource()));
        resource.setAuditStatus("approved");
        resource.setAgentTaskId(task.getId());
        resource.setCreatedBy(task.getTeacherId());
        learningResourceMapper.insert(resource);
        if (resource.getId() != null && draft.getKnowledgePointId() != null) {
            ResourceKnowledge relation = new ResourceKnowledge();
            relation.setKnowledgePointId(draft.getKnowledgePointId());
            relation.setRelationType("cover");
            relation.setCoverageWeight(BigDecimal.ONE);
            relation.setIsPrimary(1);
            resourceKnowledgeMapper.batchInsert(resource.getId(), List.of(relation));
        }
    }
}
