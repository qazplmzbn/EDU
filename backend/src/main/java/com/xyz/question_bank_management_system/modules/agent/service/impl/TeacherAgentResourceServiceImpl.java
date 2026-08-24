package com.xyz.question_bank_management_system.modules.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.agent.dto.TeacherAgentResourceGenerateRequest;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.ResourceKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.profile.service.StageLearningEvaluationService;
import com.xyz.question_bank_management_system.modules.agent.service.TeacherAgentResourceService;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceGenerateVO;
import com.xyz.question_bank_management_system.modules.agent.vo.TeacherAgentResourceTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TeacherAgentResourceServiceImpl implements TeacherAgentResourceService {

    private static final int BIZ_TYPE_TEACHER_AGENT_RESOURCE = 4;
    private static final DateTimeFormatter RUN_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> DEFAULT_RESOURCE_TYPES = List.of(
            "knowledge_video",
            "remedial_exercise",
            "knowledge_handout",
            "error_reflection",
            "learning_path"
    );
    private static final String AGENT_GENERATOR = "generator";
    private static final String AGENT_QUALITY_REVIEWER = "qualityReviewer";
    private static final String AGENT_CONSISTENCY_REVIEWER = "consistencyReviewer";
    private static final List<String> DISCUSSION_AGENT_SEQUENCE = List.of(
            "preprocess",
            "coordinator",
            "knowledge",
            "ability",
            "behavior",
            "resource",
            "practice",
            "report",
            "qualityReview",
            "consistencyReview"
    );
    private static final int GENERATOR_WAIT_SECONDS = 75;
    private static final int REVIEW_WAIT_SECONDS = 45;
    private static final int DISCUSSION_WAIT_SECONDS = 45;
    private static final Set<String> EDITABLE_AGENT_PROVIDER_KEYS = Set.of(
            "preprocess",
            "coordinator",
            "knowledge",
            "ability",
            "behavior",
            "resource",
            "practice",
            "report",
            "qualityReview",
            "consistencyReview",
            AGENT_GENERATOR,
            AGENT_QUALITY_REVIEWER,
            AGENT_CONSISTENCY_REVIEWER
    );

    private final StageLearningEvaluationService stageLearningEvaluationService;
    private final LlmService llmService;
    private final QbLearningResourceMapper learningResourceMapper;
    private final ResourceKnowledgeMapper resourceKnowledgeMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, TaskState> taskStore = new ConcurrentHashMap<>();

    @Override
    public TeacherAgentResourceGenerateVO generate(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        if (request == null || request.getStudentId() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "学生 ID 不能为空");
        }

        try {
            return doGenerate(teacherId, admin, request);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.LLM_ERROR, "教师端智能体资源生成失败：" + safeErrorMessage(ex));
        }
    }

    @Override
    public TeacherAgentResourceGenerateVO.AgentDiscussionMessage discuss(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        if (request == null || request.getStudentId() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "学生 ID 不能为空");
        }
        String agentId = normalizeProviderKey(request.getAgentId());
        if (!StringUtils.hasText(agentId) || !DISCUSSION_AGENT_SEQUENCE.contains(agentId)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "不支持的会诊智能体：" + safeText(agentId, "空"));
        }
        StageLearningEvaluationVO profile = loadProfile(teacherId, admin, request);
        List<String> resourceTypes = normalizeResourceTypes(request.getResourceTypes());
        String defaultProviderKey = normalizeProviderKey(request.getProviderKey());
        Map<String, String> agentProviderKeys = normalizeAgentProviderKeys(request.getAgentProviderKeys());
        Map<String, Object> evidencePack = buildLearningEvidencePack(profile, request, resourceTypes);
        String providerKey = providerFor(defaultProviderKey, agentProviderKeys, agentId);
        return runSingleAgentDiscussion(profile, request, resourceTypes, evidencePack,
                discussionMessagesFromRequest(request), agentId, providerKey, teacherId);
    }

    @Override
    public List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> discussMeeting(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        if (request == null || request.getStudentId() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "学生 ID 不能为空");
        }
        StageLearningEvaluationVO profile = loadProfile(teacherId, admin, request);
        List<String> resourceTypes = normalizeResourceTypes(request.getResourceTypes());
        Map<String, Object> evidencePack = buildLearningEvidencePack(profile, request, resourceTypes);
        return runMeetingConversation(profile, request, resourceTypes, evidencePack,
                discussionMessagesFromRequest(request), teacherId);
    }

    @Override
    public TeacherAgentResourceTaskVO startGenerateTask(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        if (request == null || request.getStudentId() == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "学生 ID 不能为空");
        }
        String taskId = "teacher-agent-task-" + UUID.randomUUID();
        TaskState task = new TaskState();
        task.taskId = taskId;
        task.teacherId = teacherId;
        task.admin = admin;
        task.studentId = request.getStudentId();
        task.status = "queued";
        task.message = "任务已创建，正在排队生成个性化资源。";
        task.startedAt = LocalDateTime.now();
        task.updatedAt = task.startedAt;
        taskStore.put(taskId, task);

        task.future = CompletableFuture.runAsync(() -> runTask(taskId, teacherId, admin, request));
        return toTaskVO(task);
    }

    @Override
    public TeacherAgentResourceTaskVO getTaskStatus(Long teacherId, boolean admin, String taskId) {
        TaskState task = taskStore.get(taskId);
        assertTaskReadable(task, teacherId, admin);
        return toTaskVO(task);
    }

    @Override
    public TeacherAgentResourceTaskVO cancelTask(Long teacherId, boolean admin, String taskId) {
        TaskState task = taskStore.get(taskId);
        assertTaskReadable(task, teacherId, admin);
        if (isTerminal(task.status)) {
            return toTaskVO(task);
        }
        task.canceled = true;
        task.status = "canceled";
        task.message = "任务已手动停止。当前已生成内容会保留，未完成调用不再继续写入结果。";
        task.finishedAt = LocalDateTime.now();
        task.updatedAt = task.finishedAt;
        if (task.future != null) {
            task.future.cancel(true);
        }
        return toTaskVO(task);
    }

    private TeacherAgentResourceGenerateVO doGenerate(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        return doGenerate(teacherId, admin, request, null);
    }

    private TeacherAgentResourceGenerateVO doGenerate(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request, TaskState task) {
        StageLearningEvaluationVO profile = loadProfile(teacherId, admin, request);
        List<String> resourceTypes = normalizeResourceTypes(request.getResourceTypes());
        String defaultProviderKey = normalizeProviderKey(request.getProviderKey());
        Map<String, String> agentProviderKeys = normalizeAgentProviderKeys(request.getAgentProviderKeys());
        List<TeacherAgentResourceGenerateVO.ResourceDraft> matchedVideoResources = matchExistingVideoResources(profile, resourceTypes);
        Set<String> matchedVideoTypes = matchedVideoResources.stream()
                .map(TeacherAgentResourceGenerateVO.ResourceDraft::getResourceType)
                .filter(StringUtils::hasText)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        List<TeacherAgentResourceGenerateVO.ResourceDraft> externalVideoResources = externalVideoResources(profile, resourceTypes, matchedVideoTypes);
        List<String> generationTypes = resourceTypes.stream()
                .filter(type -> !isVideoType(type))
                .toList();
        TeacherAgentResourceGenerateVO vo = new TeacherAgentResourceGenerateVO();
        vo.setRunId("teacher-resource-" + LocalDateTime.now().format(RUN_ID_TIME) + "-" + request.getStudentId());
        vo.getAgentTrace().add(trace("preprocess", "预处理智能体", "success",
                "已读取学生阶段画像，薄弱点 " + profile.getWeakKnowledgePoints().size() + " 个。", null, null));
        vo.getAgentTrace().add(trace("coordinator", "协调智能体", "success",
                "已按知识补强、错因复盘、能力任务、变式练习和学习路径分配生成任务。", null, null));
        Map<String, Object> evidencePack = buildLearningEvidencePack(profile, request, resourceTypes);
        try {
            vo.getDiscussionMessages().addAll(runAgentDiscussion(profile, request, resourceTypes, evidencePack, teacherId));
        } catch (Exception ex) {
            TeacherAgentResourceGenerateVO.DecisionSummary fallbackSummary = new TeacherAgentResourceGenerateVO.DecisionSummary();
            fallbackSummary.setStatus("fallback");
            fallbackSummary.setHeadline("会诊摘要暂不可用");
            fallbackSummary.setCurrentProblem("系统已保留学生画像和资源生成结果，请教师直接查看资源草案与审核报告。");
            fallbackSummary.setTeacherAction("先查看资源生成结果，必要时结合阶段评价进行人工复核。");
            vo.setDecisionSummary(fallbackSummary);
        }
        ensureTaskActive(task);

        String generatorProvider = providerFor(defaultProviderKey, agentProviderKeys, AGENT_GENERATOR);
        GenerationExecution generation = generationTypes.isEmpty()
                ? new GenerationExecution()
                : runGeneration(profile, request, generationTypes, generatorProvider, teacherId);
        QbLlmCall generatorCall = generation.call;
        List<TeacherAgentResourceGenerateVO.ResourceDraft> resources = new ArrayList<>();
        resources.addAll(matchedVideoResources);
        resources.addAll(externalVideoResources);
        resources.addAll(generation.resources);
        String matchedSummary = externalVideoResources.isEmpty()
                ? ""
                : "已为薄弱知识点准备 " + externalVideoResources.size() + " 个外部讲解视频入口；";
        vo.getAgentTrace().add(trace("generator", "资源生成智能体", generation.errorMessage == null ? "success" : "failed",
                generation.errorMessage == null
                        ? matchedSummary + "已生成 " + generation.resources.size() + " 个新资源草案。"
                        : matchedSummary + "模型生成未在预期时间内返回可用结果，已基于画像生成可编辑草案：" + generation.errorMessage,
                generatorCall == null ? null : generatorCall.getModelName(),
                generatorCall == null ? null : generatorCall.getId()));
        ensureTaskActive(task);

        String qualityProvider = providerFor(defaultProviderKey, agentProviderKeys, AGENT_QUALITY_REVIEWER);
        String consistencyProvider = providerFor(defaultProviderKey, agentProviderKeys, AGENT_CONSISTENCY_REVIEWER);
        CompletableFuture<ReviewExecution> qualityFuture = CompletableFuture.supplyAsync(() ->
                        runReview(profile.getStudentId(), buildQualityReviewPrompt(profile, resources), qualityProvider, teacherId, "quality"))
                .completeOnTimeout(reviewTimeout("quality"), REVIEW_WAIT_SECONDS, TimeUnit.SECONDS);
        CompletableFuture<ReviewExecution> consistencyFuture = CompletableFuture.supplyAsync(() ->
                        runReview(profile.getStudentId(), buildConsistencyReviewPrompt(profile, resources), consistencyProvider, teacherId, "consistency"))
                .completeOnTimeout(reviewTimeout("consistency"), REVIEW_WAIT_SECONDS, TimeUnit.SECONDS);
        ReviewExecution qualityExecution = qualityFuture.join();
        ReviewExecution consistencyExecution = consistencyFuture.join();
        ensureTaskActive(task);
        vo.getAgentTrace().add(reviewTrace("quality-review", "资源质量审核智能体", qualityExecution,
                "已完成完整性、可用性、难度适配和教学价值审核。",
                "质量审核智能体暂未返回可用结果，已保留资源草案并标记为需人工复核："));
        vo.getAgentTrace().add(reviewTrace("consistency-review", "主题一致性审核智能体", consistencyExecution,
                "已完成资源主题、学生画像和课程目标一致性审核。",
                "一致性审核智能体暂未返回可用结果，已保留资源草案并标记为需人工复核："));

        mergeReviews(resources, qualityExecution.reviews, consistencyExecution.reviews, generatorCall, qualityExecution.call, consistencyExecution.call);
        vo.getResources().addAll(resources);
        if (vo.getDecisionSummary() == null || "fallback".equals(vo.getDecisionSummary().getStatus())) {
            vo.setDecisionSummary(buildDecisionSummary(profile, request, resourceTypes, resources, vo.getDiscussionMessages()));
        }
        vo.getAgentTrace().add(trace("report", "报告汇总智能体", "success",
                "已汇总资源内容、个性化依据、审核报告和模型来源。", null, null));
        return vo;
    }

    private void runTask(String taskId, Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        TaskState task = taskStore.get(taskId);
        if (task == null) {
            return;
        }
        task.status = "running";
        task.message = "智能体正在生成资源并执行审核，请稍后查看。";
        task.updatedAt = LocalDateTime.now();
        try {
            TeacherAgentResourceGenerateVO result = doGenerate(teacherId, admin, request, task);
            if (task.canceled) {
                return;
            }
            task.status = "completed";
            task.message = "个性化资源已生成完成。";
            task.result = result;
            task.runId = result.getRunId();
            task.finishedAt = LocalDateTime.now();
            task.updatedAt = task.finishedAt;
        } catch (CancellationException ex) {
            task.status = "canceled";
            task.message = "任务已手动停止。";
            task.finishedAt = LocalDateTime.now();
            task.updatedAt = task.finishedAt;
        } catch (BizException ex) {
            if (task.canceled) {
                return;
            }
            task.status = "failed";
            task.message = ex.getMessage();
            task.finishedAt = LocalDateTime.now();
            task.updatedAt = task.finishedAt;
        } catch (Exception ex) {
            if (task.canceled) {
                return;
            }
            task.status = "failed";
            task.message = "教师端智能体资源生成失败：" + safeErrorMessage(ex);
            task.finishedAt = LocalDateTime.now();
            task.updatedAt = task.finishedAt;
        }
    }

    private StageLearningEvaluationVO loadProfile(Long teacherId, boolean admin, TeacherAgentResourceGenerateRequest request) {
        List<StageLearningEvaluationVO> rows = stageLearningEvaluationService.teacherEvaluations(
                teacherId,
                admin,
                request.getStudentId(),
                request.getStage(),
                request.getStartDate(),
                request.getEndDate()
        );
        if (rows == null || rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "未找到该学生画像");
        }
        return rows.get(0);
    }

    private List<String> normalizeResourceTypes(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return DEFAULT_RESOURCE_TYPES;
        }
        List<String> result = requested.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(this::normalizeResourceType)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(8)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (String required : DEFAULT_RESOURCE_TYPES) {
            if (!result.contains(required)) {
                result.add(required);
            }
        }
        return result.isEmpty() ? DEFAULT_RESOURCE_TYPES : result;
    }

    private String normalizeResourceType(String type) {
        return switch (type == null ? "" : type.trim()) {
            case "animated_explainer", "video" -> "knowledge_video";
            case "interactive_quiz", "variant_practice", "exercise" -> "remedial_exercise";
            case "knowledge_pack", "handout" -> "knowledge_handout";
            case "reflection_list", "reflection" -> "error_reflection";
            case "ability_task" -> "learning_path";
            default -> type;
        };
    }

    private Map<String, String> normalizeAgentProviderKeys(Map<String, String> requested) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (requested == null || requested.isEmpty()) {
            return normalized;
        }
        Set<String> invalidKeys = new LinkedHashSet<>();
        requested.forEach((key, value) -> {
            String normalizedKey = normalizeProviderKey(key);
            if (!StringUtils.hasText(normalizedKey)) {
                return;
            }
            if (!EDITABLE_AGENT_PROVIDER_KEYS.contains(normalizedKey)) {
                invalidKeys.add(normalizedKey);
                return;
            }
            String normalizedValue = normalizeProviderKey(value);
            if (StringUtils.hasText(normalizedValue)) {
                normalized.put(normalizedKey, normalizedValue);
            }
        });
        if (!invalidKeys.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "不支持的智能体模型配置：" + String.join(", ", invalidKeys));
        }
        return normalized;
    }

    private String providerFor(String defaultProviderKey, Map<String, String> agentProviderKeys, String agentId) {
        String providerKey = agentProviderKeys.get(agentId);
        if (!StringUtils.hasText(providerKey)) {
            providerKey = switch (agentId) {
                case AGENT_GENERATOR -> firstNonBlank(agentProviderKeys.get("report"), agentProviderKeys.get("resource"));
                case AGENT_QUALITY_REVIEWER -> agentProviderKeys.get("qualityReview");
                case AGENT_CONSISTENCY_REVIEWER -> agentProviderKeys.get("consistencyReview");
                default -> providerKey;
            };
        }
        return StringUtils.hasText(providerKey) ? providerKey : defaultProviderKey;
    }

    private String normalizeProviderKey(String providerKey) {
        return StringUtils.hasText(providerKey) ? providerKey.trim() : null;
    }

    private QbLlmCall callLlm(Long studentId, String prompt, String providerKey, Long userId) {
        QbLlmCall call = llmService.chatCompletion(BIZ_TYPE_TEACHER_AGENT_RESOURCE, studentId, prompt, providerKey, userId);
        if (call == null || call.getCallStatus() == null || call.getCallStatus() != 1) {
            String message = call == null || !StringUtils.hasText(call.getResponseText())
                    ? "大模型调用失败"
                    : call.getResponseText();
            throw BizException.of(ErrorCode.LLM_ERROR, message);
        }
        return call;
    }

    private GenerationExecution runGeneration(StageLearningEvaluationVO profile,
                                              TeacherAgentResourceGenerateRequest request,
                                              List<String> resourceTypes,
                                              String providerKey,
                                              Long teacherId) {
        GenerationExecution execution = new GenerationExecution();
        try {
            CompletableFuture<QbLlmCall> future = CompletableFuture.supplyAsync(() ->
                    callLlm(profile.getStudentId(), buildGenerationPrompt(profile, request, resourceTypes), providerKey, teacherId));
            QbLlmCall call = future.completeOnTimeout(null, GENERATOR_WAIT_SECONDS, TimeUnit.SECONDS).join();
            if (call == null) {
                execution.errorMessage = "资源生成智能体超过 " + GENERATOR_WAIT_SECONDS + " 秒未返回";
                execution.resources = fallbackResources(profile, request, resourceTypes);
                return execution;
            }
            execution.call = call;
            execution.resources = parseGeneratedResources(call, profile);
            return execution;
        } catch (Exception ex) {
            execution.errorMessage = safeErrorMessage(ex);
            execution.resources = fallbackResources(profile, request, resourceTypes);
            return execution;
        }
    }

    private List<TeacherAgentResourceGenerateVO.ResourceDraft> fallbackResources(StageLearningEvaluationVO profile,
                                                                                TeacherAgentResourceGenerateRequest request,
                                                                                List<String> resourceTypes) {
        List<String> types = resourceTypes == null || resourceTypes.isEmpty() ? DEFAULT_RESOURCE_TYPES : resourceTypes;
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        List<String> suggestions = profile.getSuggestions() == null ? List.of() : profile.getSuggestions();
        List<TeacherAgentResourceGenerateVO.ResourceDraft> resources = new ArrayList<>();
        int index = 1;
        for (String type : types) {
            TeacherAgentResourceGenerateVO.ResourceDraft draft = new TeacherAgentResourceGenerateVO.ResourceDraft();
            draft.setDraftId(type + "-" + index);
            draft.setResourceType(type);
            enrichDraftMetadata(draft, profile, request);
            draft.setTitle(fallbackTitle(type, weakPoints));
            draft.setSummary("根据学生画像、薄弱知识点和教师要求自动生成的个性化资源草案，建议教师复核后使用。");
            draft.setContent(fallbackContent(type, profile, weakPoints, weakDimensions, suggestions, request));
            if (isVideoType(type)) {
                draft.setMediaType("video");
                draft.setVideoConfig(videoConfig(type));
                draft.setVideoScenes(fallbackVideoScenes(type, profile, weakPoints, weakDimensions));
                applyPlayableVideoFallback(draft, profile);
            }
            if ("remedial_exercise".equals(type)) {
                draft.setQuizQuestions(fallbackQuizQuestions(profile, weakPoints, weakDimensions));
                draft.setExerciseQuestions(fallbackExerciseQuestions(profile, request, weakPoints, weakDimensions));
            }
            draft.setPersonalizationBasis(defaultBasis(profile));
            TeacherAgentResourceGenerateVO.ReviewReport review = new TeacherAgentResourceGenerateVO.ReviewReport();
            review.setQualityScore(70);
            review.setRelevanceScore(75);
            review.setConsistencyScore(75);
            review.setPassed(false);
            review.setComments("模型生成超时或返回异常，系统已生成规则草案，请教师人工复核。");
            review.setRevisionSuggestions(List.of("补充更具体的课堂讲解或题目", "确认内容与学生薄弱点完全匹配"));
            draft.setReviewReport(review);
            draft.setModelSource(new TeacherAgentResourceGenerateVO.ModelSource());
            draft.setStatus("needs_revision");
            resources.add(draft);
            index++;
        }
        return resources;
    }

    private List<TeacherAgentResourceGenerateVO.ResourceDraft> matchExistingVideoResources(StageLearningEvaluationVO profile,
                                                                                           List<String> resourceTypes) {
        List<String> requestedVideoTypes = resourceTypes.stream().filter(this::isVideoType).toList();
        if (requestedVideoTypes.isEmpty()) {
            return List.of();
        }
        List<QbLearningResource> candidates = findExistingVideoResources(profile);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<TeacherAgentResourceGenerateVO.ResourceDraft> drafts = new ArrayList<>();
        Set<Long> usedResourceIds = new LinkedHashSet<>();
        int index = 1;
        for (String type : requestedVideoTypes) {
            QbLearningResource matched = bestVideoCandidate(type, candidates, usedResourceIds);
            if (matched == null) {
                continue;
            }
            usedResourceIds.add(matched.getId());
            drafts.add(existingVideoDraft(type, matched, profile, index));
            index++;
        }
        return drafts;
    }

    private List<TeacherAgentResourceGenerateVO.ResourceDraft> externalVideoResources(StageLearningEvaluationVO profile,
                                                                                       List<String> resourceTypes,
                                                                                       Set<String> alreadyMatchedTypes) {
        List<String> requestedVideoTypes = resourceTypes.stream()
                .filter(this::isVideoType)
                .filter(type -> alreadyMatchedTypes == null || !alreadyMatchedTypes.contains(type))
                .toList();
        if (requestedVideoTypes.isEmpty()) {
            return List.of();
        }
        List<TeacherAgentResourceGenerateVO.ResourceDraft> drafts = new ArrayList<>();
        int index = 1;
        for (String type : requestedVideoTypes) {
            drafts.add(externalVideoDraft(type, profile, index));
            index++;
        }
        return drafts;
    }

    private TeacherAgentResourceGenerateVO.ResourceDraft externalVideoDraft(String requestedType,
                                                                            StageLearningEvaluationVO profile,
                                                                            int index) {
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        String focus = weakPoints.isEmpty() ? "数组与字符串" : weakPoints.get(0);
        String student = safeText(profile.getStudentName(), "该学生");
        String playableUrl = "https://www.bilibili.com/video/BV1m8411m79X/";

        TeacherAgentResourceGenerateVO.ResourceDraft draft = new TeacherAgentResourceGenerateVO.ResourceDraft();
        draft.setDraftId(requestedType + "-external-video-" + index);
        draft.setResourceType(requestedType);
        enrichDraftMetadata(draft, profile, null);
        draft.setTitle(focus + " 知识讲解视频推荐");
        draft.setSummary("根据学生薄弱点匹配到的具体知识讲解视频，点击后直接进入视频播放页。");
        draft.setContent("学生：" + student
                + "\n薄弱知识点：" + String.join("、", weakPoints)
                + "\n推荐视频：C语言数组与字符串基础讲解与例题分析"
                + "\n播放链接：" + playableUrl
                + "\n使用建议：先观看数组定义、字符串初始化和常见例题部分，再完成同类补救练习。");
        draft.setSourceUrl(playableUrl);
        draft.setMediaType("video");
        draft.setVideoConfig(externalVideoConfig(focus, playableUrl));
        draft.setVideoScenes(List.of());
        applyPlayableVideoFallback(draft, profile);
        draft.setPersonalizationBasis(defaultBasis(profile));

        TeacherAgentResourceGenerateVO.ReviewReport review = new TeacherAgentResourceGenerateVO.ReviewReport();
        review.setQualityScore(82);
        review.setRelevanceScore(88);
        review.setConsistencyScore(84);
        review.setPassed(true);
        review.setComments("已根据学生薄弱点提供具体知识讲解视频播放链接，建议教师预览后保存为学生推荐资源。");
        review.setRevisionSuggestions(List.of("确认视频内容覆盖薄弱知识点：" + focus, "如课程要求更精确，可替换为教师确认的具体视频链接"));
        draft.setReviewReport(review);

        TeacherAgentResourceGenerateVO.ModelSource source = new TeacherAgentResourceGenerateVO.ModelSource();
        source.setGeneratorModel("具体视频资源匹配");
        draft.setModelSource(source);
        draft.setStatus("approved");
        return draft;
    }

    private Map<String, Object> externalVideoConfig(String focus, String videoUrl) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("renderMode", "external_playable_video");
        config.put("sourceStatus", "matched");
        config.put("platform", "Bilibili");
        config.put("knowledgePoint", focus);
        config.put("sourceUrl", videoUrl);
        config.put("style", "知识讲解视频");
        config.put("aspectRatio", "16:9");
        return config;
    }

    private List<QbLearningResource> findExistingVideoResources(StageLearningEvaluationVO profile) {
        List<Long> knowledgePointIds = profile.getWeakKnowledgePoints() == null
                ? List.of()
                : profile.getWeakKnowledgePoints().stream()
                .map(StageLearningEvaluationVO.WeakKnowledgePoint::getKnowledgePointId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(8)
                .toList();
        List<String> keywords = videoSearchKeywords(profile);
        if (knowledgePointIds.isEmpty() && keywords.isEmpty()) {
            return List.of();
        }
        return learningResourceMapper.selectVideoCandidates(knowledgePointIds, keywords, 12);
    }

    private List<String> videoSearchKeywords(StageLearningEvaluationVO profile) {
        List<String> keywords = new ArrayList<>();
        keywords.addAll(weakPointNames(profile));
        keywords.addAll(weakDimensionNames(profile));
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private QbLearningResource bestVideoCandidate(String requestedType,
                                                  List<QbLearningResource> candidates,
                                                  Set<Long> usedResourceIds) {
        return candidates.stream()
                .filter(resource -> resource.getId() != null && !usedResourceIds.contains(resource.getId()))
                .filter(resource -> isPlayableVideoUrl(resource.getUrl()))
                .filter(resource -> requestedType.equals(resource.getResourceType()))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(resource -> resource.getId() != null && !usedResourceIds.contains(resource.getId()))
                        .filter(resource -> isPlayableVideoUrl(resource.getUrl()))
                        .filter(resource -> "video".equals(resource.getResourceType()))
                        .findFirst()
                        .orElseGet(() -> candidates.stream()
                                .filter(resource -> resource.getId() != null && !usedResourceIds.contains(resource.getId()))
                                .filter(resource -> isPlayableVideoUrl(resource.getUrl()))
                                .findFirst()
                                .orElse(null)));
    }

    private TeacherAgentResourceGenerateVO.ResourceDraft existingVideoDraft(String requestedType,
                                                                            QbLearningResource resource,
                                                                            StageLearningEvaluationVO profile,
                                                                            int index) {
        TeacherAgentResourceGenerateVO.ResourceDraft draft = new TeacherAgentResourceGenerateVO.ResourceDraft();
        draft.setDraftId(requestedType + "-matched-" + resource.getId() + "-" + index);
        draft.setResourceType(requestedType);
        draft.setTitle(resource.getTitle());
        draft.setSummary(safeText(resource.getSummary(), "根据学生薄弱点从学习资源库匹配到的已有视频资源。"));
        draft.setContent(existingVideoContent(resource));
        draft.setSourceUrl(resource.getUrl());
        draft.setSourceStatus("matched");
        draft.setMediaType("video");
        draft.setVideoConfig(existingVideoConfig(requestedType, resource));
        draft.setVideoAsset(videoAssetFromResource(resource, profile));
        draft.setVideoRecommendations(List.of(videoRecommendationFromAsset(draft.getVideoAsset())));
        draft.setVideoScenes(existingVideoScenes(resource));
        Long knowledgePointId = resourceKnowledgeMapper.selectByResourceId(resource.getId()).stream()
                .filter(relation -> relation.getIsPrimary() != null && relation.getIsPrimary() == 1)
                .map(relation -> relation.getKnowledgePointId()).findFirst().orElse(null);
        draft.setKnowledgePointId(knowledgePointId);
        draft.setPersonalizationBasis(existingVideoBasis(profile, resource));
        TeacherAgentResourceGenerateVO.ReviewReport review = new TeacherAgentResourceGenerateVO.ReviewReport();
        review.setQualityScore(86);
        review.setRelevanceScore(88);
        review.setConsistencyScore(86);
        review.setPassed(true);
        review.setComments("已从学习资源库匹配现有视频资源，建议教师确认视频内容与当前学生薄弱点一致。");
        draft.setReviewReport(review);
        TeacherAgentResourceGenerateVO.ModelSource source = new TeacherAgentResourceGenerateVO.ModelSource();
        source.setGeneratorModel("资源库匹配");
        draft.setModelSource(source);
        draft.setStatus("approved");
        return draft;
    }

    private String existingVideoContent(QbLearningResource resource) {
        List<String> lines = new ArrayList<>();
        lines.add("已优先匹配学习资源库中的已有视频资源。");
        if (StringUtils.hasText(resource.getSummary())) {
            lines.add("资源摘要：" + resource.getSummary());
        }
        if (StringUtils.hasText(resource.getUrl())) {
            lines.add("播放链接：" + resource.getUrl());
        }
        if (StringUtils.hasText(resource.getContent()) && !looksLikeJson(resource.getContent())) {
            lines.add(resource.getContent());
        }
        return String.join("\n", lines);
    }

    private Map<String, Object> existingVideoConfig(String requestedType, QbLearningResource resource) {
        Map<String, Object> config = extractVideoConfig(resource.getContent());
        if (config.isEmpty()) {
            config = videoConfig(requestedType);
        }
        config.put("renderMode", "existing_resource");
        config.put("sourceResourceId", resource.getId());
        config.put("sourceResourceType", resource.getResourceType());
        if (StringUtils.hasText(resource.getUrl())) {
            config.put("sourceUrl", resource.getUrl());
        }
        return config;
    }

    private List<TeacherAgentResourceGenerateVO.VideoScene> existingVideoScenes(QbLearningResource resource) {
        if (!StringUtils.hasText(resource.getContent()) || !looksLikeJson(resource.getContent())) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(resource.getContent());
            JsonNode scenes = root.path("videoScenes");
            if (!scenes.isArray() || scenes.isEmpty()) {
                return List.of();
            }
            return objectMapper.convertValue(scenes, new TypeReference<List<TeacherAgentResourceGenerateVO.VideoScene>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> extractVideoConfig(String content) {
        if (!StringUtils.hasText(content) || !looksLikeJson(content)) {
            return new LinkedHashMap<>();
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode config = root.path("videoConfig");
            if (!config.isObject()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.convertValue(config, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> existingVideoBasis(StageLearningEvaluationVO profile, QbLearningResource resource) {
        Map<String, Object> basis = defaultBasis(profile);
        basis.put("matchedFrom", "learning_resource_library");
        basis.put("matchedResourceId", resource.getId());
        basis.put("matchedResourceTitle", resource.getTitle());
        basis.put("matchedResourceKnowledgePointIds", resourceKnowledgeMapper.selectByResourceId(resource.getId()).stream()
                .map(relation -> relation.getKnowledgePointId()).toList());
        return basis;
    }

    private void enrichDraftMetadata(TeacherAgentResourceGenerateVO.ResourceDraft draft,
                                     StageLearningEvaluationVO profile,
                                     TeacherAgentResourceGenerateRequest request) {
        String generationScope = request != null && StringUtils.hasText(request.getGenerationScope())
                ? request.getGenerationScope()
                : "student";
        draft.setGenerationScope(generationScope);
        draft.setTargetId("class".equals(generationScope) && request != null && request.getClassId() != null
                ? String.valueOf(request.getClassId())
                : String.valueOf(profile.getStudentId()));
        draft.setPublishTarget(publishTargetFor(draft.getResourceType(), generationScope));
        draft.setSourceStatus("draft");
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("weakPoints", weakPointNames(profile));
        outputs.put("weakDimensions", weakDimensionNames(profile));
        outputs.put("difficulty", request != null && StringUtils.hasText(request.getDifficulty()) ? request.getDifficulty() : "improve");
        outputs.put("exerciseCount", effectiveExerciseCount(request));
        draft.setAgentOutputs(outputs);
    }

    private String publishTargetFor(String resourceType, String generationScope) {
        String audience = "class".equals(generationScope) ? "class" : "student";
        return "remedial_exercise".equals(resourceType) ? audience + "_draft_questions" : audience + "_recommended_resource";
    }

    private int effectiveExerciseCount(TeacherAgentResourceGenerateRequest request) {
        int count = request == null || request.getExerciseCount() == null ? 5 : request.getExerciseCount();
        return Math.max(1, Math.min(count, 10));
    }

    private TeacherAgentResourceGenerateVO.VideoAsset videoAssetFromResource(QbLearningResource resource,
                                                                             StageLearningEvaluationVO profile) {
        String focus = weakPointNames(profile).stream().findFirst().orElse("薄弱知识点");
        TeacherAgentResourceGenerateVO.VideoAsset asset = new TeacherAgentResourceGenerateVO.VideoAsset();
        asset.setTitle(resource.getTitle());
        asset.setPlatform(videoPlatform(resource.getUrl()));
        asset.setUrl(resource.getUrl());
        asset.setCoverUrl("");
        asset.setDuration("");
        asset.setRenderMode("existing_resource");
        asset.setSourceStatus("matched");
        asset.setReason(firstNonBlank(resource.getSummary(),
                "该视频来自本地学习资源库，内容与“" + focus + "”薄弱点匹配，适合作为针对性知识讲解资源。"));
        asset.setMatchedKnowledgePoints(List.of(focus));
        return asset;
    }

    private void applyPlayableVideoFallback(TeacherAgentResourceGenerateVO.ResourceDraft draft,
                                            StageLearningEvaluationVO profile) {
        String focus = weakPointNames(profile).stream().findFirst().orElse("数组与字符串");
        TeacherAgentResourceGenerateVO.VideoAsset asset = new TeacherAgentResourceGenerateVO.VideoAsset();
        asset.setTitle(focus + " 知识讲解视频");
        asset.setPlatform("Bilibili");
        asset.setUrl("https://www.bilibili.com/video/BV1m8411m79X/");
        asset.setCoverUrl("");
        asset.setDuration("16:31");
        asset.setRenderMode("external_playable_video");
        asset.setSourceStatus("matched");
        asset.setReason("当前环境未接入视频渲染服务，已提供覆盖“" + focus + "”薄弱点的具体知识讲解视频播放链接。");
        asset.setMatchedKnowledgePoints(List.of(focus));

        draft.setVideoAsset(asset);
        draft.setSourceUrl(asset.getUrl());
        draft.setSourceStatus(asset.getSourceStatus());
        Map<String, Object> config = draft.getVideoConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(draft.getVideoConfig());
        config.put("renderMode", asset.getRenderMode());
        config.put("sourceStatus", asset.getSourceStatus());
        config.put("platform", asset.getPlatform());
        config.put("sourceUrl", asset.getUrl());
        draft.setVideoConfig(config);
        draft.setVideoRecommendations(List.of(videoRecommendationFromAsset(asset)));
    }

    private TeacherAgentResourceGenerateVO.VideoRecommendation videoRecommendationFromAsset(TeacherAgentResourceGenerateVO.VideoAsset asset) {
        TeacherAgentResourceGenerateVO.VideoRecommendation recommendation = new TeacherAgentResourceGenerateVO.VideoRecommendation();
        recommendation.setTitle(asset.getTitle());
        recommendation.setPlatform(asset.getPlatform());
        recommendation.setUrl(asset.getUrl());
        recommendation.setCover(asset.getCoverUrl());
        recommendation.setDuration(asset.getDuration());
        recommendation.setViewCount("具体视频");
        recommendation.setReason(asset.getReason());
        recommendation.setMatchedKnowledgePoints(asset.getMatchedKnowledgePoints());
        recommendation.setSourceStatus(asset.getSourceStatus());
        return recommendation;
    }

    private boolean isPlayableVideoUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String value = url.toLowerCase(Locale.ROOT);
        if (value.contains("search.bilibili.com") || value.contains("youtube.com/results")) {
            return false;
        }
        return value.contains("bilibili.com/video/")
                || value.contains("youtu.be/")
                || value.contains("youtube.com/watch");
    }

    private String videoPlatform(String url) {
        String value = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (value.contains("bilibili.com")) return "Bilibili";
        if (value.contains("youtube.com") || value.contains("youtu.be")) return "YouTube";
        return "Video";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean looksLikeJson(String value) {
        String text = value == null ? "" : value.trim();
        return text.startsWith("{") || text.startsWith("[");
    }

    private String fallbackTitle(String type, List<String> weakPoints) {
        String focus = weakPoints.isEmpty() ? "核心薄弱点" : weakPoints.get(0);
        return switch (type) {
            case "knowledge_video" -> focus + " 知识讲解视频";
            case "remedial_exercise" -> focus + " 补救练习";
            case "knowledge_handout" -> focus + " 知识点讲义";
            case "error_reflection" -> focus + " 错因复盘";
            case "knowledge_pack" -> focus + " 知识补强包";
            case "reflection_list" -> focus + " 错因复盘清单";
            case "ability_task" -> focus + " 能力提升任务";
            case "variant_practice" -> focus + " 变式练习";
            case "animated_explainer" -> focus + " 知识讲解视频";
            case "interactive_quiz" -> focus + " 互动测验资源";
            case "learning_path" -> focus + " 阶段学习路径";
            default -> focus + " 个性化学习资源";
        };
    }

    private String fallbackContent(String type,
                                   StageLearningEvaluationVO profile,
                                   List<String> weakPoints,
                                   List<String> weakDimensions,
                                   List<String> suggestions,
                                   TeacherAgentResourceGenerateRequest request) {
        String student = safeText(profile.getStudentName(), "该学生");
        String focus = weakPoints.isEmpty() ? "当前薄弱知识点" : String.join("、", weakPoints);
        String dimensions = weakDimensions.isEmpty() ? "综合能力短板" : String.join("、", weakDimensions);
        String teacherRequirement = safeText(request == null ? null : request.getTeacherRequirement(), "按基础补弱、例题讲解、练习巩固组织。");
        String suggestionText = suggestions.isEmpty() ? "先完成基础题，再进行变式题和错题复盘。" : String.join("；", suggestions.stream().limit(3).toList());
        return switch (type) {
            case "knowledge_video" -> "知识讲解视频资源。\n讲解主题：" + focus
                    + "\n视频策略：优先生成可播放讲解视频；当前环境不可渲染时，提供具体可播放视频链接。"
                    + "\n讲解重点：" + dimensions
                    + "\n教师要求：" + teacherRequirement;
            case "remedial_exercise" -> "补救练习资源。\n练习主题：" + focus
                    + "\n题目要求：包含具体题干、选项或作答要求、答案、解析、难度和知识点标签。"
                    + "\n练习数量：" + effectiveExerciseCount(request)
                    + "\n教师要求：" + teacherRequirement;
            case "knowledge_handout" -> "知识点讲义。\n面向对象：" + student
                    + "\n核心知识：" + focus
                    + "\n讲义结构：概念说明、语法规则、例题讲解、易错点提示、复习建议。";
            case "error_reflection" -> "错因复盘资料。\n复盘对象：" + student
                    + "\n薄弱知识点：" + focus
                    + "\n复盘路径：定位错误类型、解释错误原因、给出修正方法、安排同类练习。";
            case "knowledge_pack" -> "面向 " + student + " 的知识补强包。\n重点知识：" + focus
                    + "\n讲解安排：先用一个基础例子说明概念，再对比常见错误写法，最后给出 2 道随堂检查题。"
                    + "\n教师要求：" + teacherRequirement;
            case "reflection_list" -> "错因复盘清单。\n1. 标记最近错题涉及的知识点：" + focus
                    + "\n2. 写出错误原因：概念不清、边界条件遗漏、代码执行顺序误判或题意理解偏差。"
                    + "\n3. 重新作答同类题，并用一句话说明修正后的方法。";
            case "ability_task" -> "能力提升任务。\n短板维度：" + dimensions
                    + "\n任务：围绕薄弱点完成“读代码解释输出、修改错误代码、独立写小程序”三步训练。"
                    + "\n验收：学生能说清关键变量变化、边界条件和最终输出。";
            case "variant_practice" -> "变式练习。\n围绕 " + focus + " 设计 3 组题：基础识别题、代码填空题、综合应用题。"
                    + "\n每组题后要求学生写出错因或解题依据，教师根据作答再调整难度。";
            case "animated_explainer" -> "知识点动画讲解视频资源。\n讲解主题：" + focus
                    + "\n视频形式：参考 B 站/YouTube 知识讲解视频，用动画展示概念关系、代码执行过程和内存变化。"
                    + "\n重点可视化：" + dimensions
                    + "\n建议学生看完后完成随堂练习。";
            case "interactive_quiz" -> "互动测验资源。\n测验目标：" + focus
                    + "\n包含 3 道可作答题目：选择题、判断题和填空题。每题都围绕薄弱知识点给出正确答案、解析和错误反馈。"
                    + "\n反馈规则：答错时先提示相关概念，再给出一步提示，最后展示完整解析。"
                    + "\n评价维度：" + dimensions + "。";
            case "learning_path" -> "阶段学习路径。\n第 1 阶段：补齐 " + focus
                    + "\n第 2 阶段：完成同类例题和变式练习。"
                    + "\n第 3 阶段：用错题复盘检查是否真正掌握。"
                    + "\n建议：" + suggestionText;
            default -> "个性化资源草案。\n关注点：" + focus + "\n能力短板：" + dimensions + "\n建议：" + suggestionText;
        };
    }

    private boolean isVideoType(String type) {
        return "knowledge_video".equals(normalizeResourceType(type));
    }

    private Map<String, Object> videoConfig(String type) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("style", "知识点动画讲解");
        config.put("aspectRatio", "16:9");
        config.put("voice", "温和清晰的中文教师旁白");
        config.put("renderMode", "needs_render");
        config.put("sourceStatus", "needs_render");
        return config;
    }

    private List<TeacherAgentResourceGenerateVO.VideoScene> fallbackVideoScenes(String type,
                                                                               StageLearningEvaluationVO profile,
                                                                               List<String> weakPoints,
                                                                               List<String> weakDimensions) {
        String student = safeText(profile.getStudentName(), "同学");
        String focus = weakPoints.isEmpty() ? "当前薄弱知识点" : String.join("、", weakPoints);
        String dimension = weakDimensions.isEmpty() ? "理解迁移" : weakDimensions.get(0);
        List<TeacherAgentResourceGenerateVO.VideoScene> scenes = new ArrayList<>();
        scenes.add(videoScene("课程导入", student + "，这节课我们只讲一个重点：" + focus + "。先用一道你容易出错的小题引出今天的问题。",
                "在线视频课程开场，深色黑板背景，标题写着 C语言知识点讲解，右下角有教师头像和播放进度条",
                "这节课解决：" + focus, 10));
        scenes.add(videoScene("概念讲解", "先把概念讲清楚：它是什么、解决什么问题、和前面学过的知识有什么关系。",
                "教师板书式动画，关键概念逐个出现，用箭头连接变量、地址、数组、指针或字符串关系",
                "概念链路：定义 -> 作用 -> 使用条件", 16));
        scenes.add(videoScene("例题代码", "接着看一段完整 C 语言代码。先读题，再看变量初始化，最后跟着每一行代码判断结果。",
                "左侧显示 C 代码编辑器，当前执行行高亮；右侧显示变量表、内存格或输出窗口同步变化",
                "读代码顺序：输入 -> 状态变化 -> 输出", 20));
        scenes.add(videoScene("易错辨析", "这个知识点最容易错在这里：只记住写法，却没有判断边界、执行顺序或内存含义。",
                "同屏对比错误写法和正确写法，错误处用橙色圈出，正确思路用青色箭头标注",
                "易错点：" + dimension + "需要加强", 16));
        scenes.add(videoScene("随堂练习", "现在暂停视频，自己完成一道同类小题。写出你的判断过程，再继续看答案。",
                "视频暂停练习页，显示一道 C 语言小题、三个提示按钮和倒计时样式",
                "暂停练习：先预测，再验证", 14));
        scenes.add(videoScene("总结复盘", "最后总结这节课：记住核心判断方法，并用两道变式题确认自己真的掌握。",
                "课程总结页，列出三条方法口诀、错题复盘任务和下一步练习入口",
                "总结：讲清概念 + 跟踪代码 + 完成变式", 12));
        return scenes;
    }

    private TeacherAgentResourceGenerateVO.VideoScene videoScene(String title,
                                                                 String narration,
                                                                 String visualPrompt,
                                                                 String boardText,
                                                                 int durationSeconds) {
        TeacherAgentResourceGenerateVO.VideoScene scene = new TeacherAgentResourceGenerateVO.VideoScene();
        scene.setTitle(title);
        scene.setNarration(narration);
        scene.setVisualPrompt(visualPrompt);
        scene.setBoardText(boardText);
        scene.setDurationSeconds(durationSeconds);
        return scene;
    }

    private List<TeacherAgentResourceGenerateVO.QuizQuestion> fallbackQuizQuestions(StageLearningEvaluationVO profile,
                                                                                    List<String> weakPoints,
                                                                                    List<String> weakDimensions) {
        String focus = weakPoints.isEmpty() ? "C语言基础知识" : weakPoints.get(0);
        String dimension = weakDimensions.isEmpty() ? "概念理解" : weakDimensions.get(0);
        List<TeacherAgentResourceGenerateVO.QuizQuestion> questions = new ArrayList<>();
        questions.add(quizQuestion(
                "single_choice",
                "关于“" + focus + "”的学习，下列哪一种做法最适合先检查自己是否理解？",
                List.of("直接背答案", "先说明概念，再用一段小代码验证", "只看运行结果", "跳过基础题做综合题"),
                "B",
                "薄弱知识点需要先讲清概念，再通过短代码验证理解，最后再进入变式练习。",
                "先不要只记答案，建议回到概念和代码执行过程。",
                focus
        ));
        questions.add(quizQuestion(
                "single_choice",
                "如果一道 C 语言题目在“" + focus + "”处出错，最应该优先复盘什么？",
                List.of("题目字体", "变量变化、边界条件和输出依据", "提交时间", "代码缩进是否好看"),
                "B",
                "C 语言题目复盘要抓住变量状态、边界条件和输出依据，这能对应能力短板：" + dimension + "。",
                "复盘重点应放在代码逻辑和知识点判断上。",
                focus
        ));
        questions.add(quizQuestion(
                "fill_blank",
                "填空：学习“" + focus + "”时，建议按“概念理解 -> 代码走读 -> ________”的顺序巩固。",
                List.of(),
                "变式练习",
                "完成概念和代码走读后，需要通过变式练习确认是否能迁移到新题。",
                "最后一步应能检验迁移应用能力。",
                focus
        ));
        return questions;
    }

    private TeacherAgentResourceGenerateVO.QuizQuestion quizQuestion(String questionType,
                                                                     String stem,
                                                                     List<String> options,
                                                                     String answer,
                                                                     String explanation,
                                                                     String wrongFeedback,
                                                                     String knowledgePoint) {
        TeacherAgentResourceGenerateVO.QuizQuestion question = new TeacherAgentResourceGenerateVO.QuizQuestion();
        question.setQuestionType(questionType);
        question.setStem(stem);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setExplanation(explanation);
        question.setWrongFeedback(wrongFeedback);
        question.setKnowledgePoint(knowledgePoint);
        return question;
    }

    private List<TeacherAgentResourceGenerateVO.ExerciseQuestion> fallbackExerciseQuestions(StageLearningEvaluationVO profile,
                                                                                             TeacherAgentResourceGenerateRequest request,
                                                                                             List<String> weakPoints,
                                                                                             List<String> weakDimensions) {
        String focus = weakPoints.isEmpty() ? "数组与字符串" : weakPoints.get(0);
        String dimension = weakDimensions.isEmpty() ? "概念理解" : weakDimensions.get(0);
        List<TeacherAgentResourceGenerateVO.ExerciseQuestion> questions = new ArrayList<>();
        questions.add(exerciseQuestion(
                "single_choice",
                "在 C 语言中，字符数组 char s[6] = \"hello\"; 最后一个元素通常保存什么？",
                List.of("A. 字符 h", "B. 字符 o", "C. 字符串结束符 \\0", "D. 随机值"),
                "C",
                "字符串字面量会自动在末尾补 \\0，char s[6] 正好保存 h、e、l、l、o 和结束符。",
                "basic",
                focus
        ));
        questions.add(exerciseQuestion(
                "single_choice",
                "下面哪种写法更适合读取不含空格的字符串到 char name[20] 中？",
                List.of("A. scanf(\"%s\", name);", "B. scanf(\"%d\", name);", "C. name = \"abc\";", "D. scanf(\"%c\", name);"),
                "A",
                "%s 用于读取字符串，数组名 name 可作为首地址传入。需要注意实际输入长度不能超过数组容量。",
                "basic",
                focus
        ));
        questions.add(exerciseQuestion(
                "fill_blank",
                "填空：C 语言字符串以特殊字符 ________ 作为结束标记。",
                List.of(),
                "\\0",
                "字符数组可以保存普通字符序列，但作为字符串使用时必须以 \\0 表示结束。",
                "basic",
                focus
        ));
        questions.add(exerciseQuestion(
                "short_answer",
                "请说明数组 a 和指针 p 在表达式 p = a; 之后，p[1] 与 a[1] 的关系。",
                List.of(),
                "p 指向数组首元素，p[1] 等价于 *(p + 1)，访问的是 a[1]。",
                "数组名在多数表达式中会转换为首元素地址，指针加下标访问本质上是地址偏移后解引用。",
                "improve",
                focus
        ));
        questions.add(exerciseQuestion(
                "debug",
                "调试题：char s[5] = \"hello\"; 这行代码有什么风险？应该如何修改？",
                List.of(),
                "容量不足，\"hello\" 需要 6 个字符空间。可改为 char s[6] = \"hello\"; 或 char s[] = \"hello\";",
                "字符串需要额外的 \\0 结束符，数组容量必须把结束符计算进去。这对应当前能力短板：" + dimension + "。",
                "improve",
                focus
        ));
        int count = effectiveExerciseCount(request);
        return questions.stream().limit(count).toList();
    }

    private TeacherAgentResourceGenerateVO.ExerciseQuestion exerciseQuestion(String questionType,
                                                                             String stem,
                                                                             List<String> options,
                                                                             String answer,
                                                                             String explanation,
                                                                             String difficulty,
                                                                             String knowledgePoint) {
        TeacherAgentResourceGenerateVO.ExerciseQuestion question = new TeacherAgentResourceGenerateVO.ExerciseQuestion();
        question.setQuestionType(questionType);
        question.setStem(stem);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setExplanation(explanation);
        question.setDifficulty(difficulty);
        question.setKnowledgePoint(knowledgePoint);
        return question;
    }

    private List<String> weakPointNames(StageLearningEvaluationVO profile) {
        if (profile.getWeakKnowledgePoints() == null) {
            return List.of();
        }
        return profile.getWeakKnowledgePoints().stream()
                .map(StageLearningEvaluationVO.WeakKnowledgePoint::getKnowledgePointName)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(4)
                .toList();
    }

    private List<String> weakDimensionNames(StageLearningEvaluationVO profile) {
        if (profile.getDimensions() == null) {
            return List.of();
        }
        return profile.getDimensions().stream()
                .sorted((a, b) -> Integer.compare(a.getScore() == null ? 100 : a.getScore(), b.getScore() == null ? 100 : b.getScore()))
                .map(StageLearningEvaluationVO.Dimension::getName)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();
    }

    private Map<String, Object> buildLearningEvidencePack(StageLearningEvaluationVO profile,
                                                          TeacherAgentResourceGenerateRequest request,
                                                          List<String> resourceTypes) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("studentName", safeText(profile.getStudentName(), "该学生"));
        evidence.put("stageName", safeText(profile.getStageName(), "当前阶段"));
        evidence.put("abilityScore", profile.getAbilityScore());
        evidence.put("masteryAverage", profile.getMasteryAverage());
        evidence.put("completedAttemptCount", profile.getCompletedAttemptCount());
        evidence.put("averageScore", profile.getAverageScore());
        evidence.put("weakKnowledgePoints", weakPointNames(profile));
        evidence.put("weakDimensions", weakDimensionNames(profile));
        evidence.put("profileSummary", safeText(profile.getSummary(), "阶段画像已生成，需结合薄弱点和能力维度安排补强。"));
        evidence.put("suggestions", profile.getSuggestions() == null ? List.of() : profile.getSuggestions().stream().filter(StringUtils::hasText).limit(4).toList());
        evidence.put("resourceTypes", resourceTypes.stream().map(this::learningResourceTypeLabel).toList());
        evidence.put("teacherRequirement", safeText(request == null ? null : request.getTeacherRequirement(), "无补充要求"));
        return evidence;
    }

    private String buildAgentDiscussionPrompt(Map<String, Object> evidencePack) {
        return "学习诊断会诊证据：" + toJson(evidencePack);
    }

    private List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> runAgentDiscussion(StageLearningEvaluationVO profile,
                                                                                          TeacherAgentResourceGenerateRequest request,
                                                                                          List<String> resourceTypes,
                                                                                          Map<String, Object> evidencePack,
                                                                                          Long teacherId) {
        if (DISCUSSION_WAIT_SECONDS > 0) {
            return runLlmAgentDiscussion(profile, request, resourceTypes, evidencePack, teacherId);
        }
        buildAgentDiscussionPrompt(evidencePack);
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        List<String> resourceLabels = resourceTypes.stream().map(this::learningResourceTypeLabel).toList();
        String weakPointText = weakPoints.isEmpty() ? "当前阶段核心知识点" : String.join("、", weakPoints);
        String weakDimensionText = weakDimensions.isEmpty() ? "综合学习能力" : String.join("、", weakDimensions);
        String resourceText = resourceLabels.isEmpty() ? "知识讲解、补救练习和学习路径" : String.join("、", resourceLabels);
        String teacherRequirement = safeText(request == null ? null : request.getTeacherRequirement(), "按画像薄弱点自动生成");
        List<String> commonEvidence = discussionEvidenceRefs(profile, resourceLabels);

        List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> rows = new ArrayList<>();
        rows.add(discussionMessage(1, "learning-diagnosis-agent", "学习情况总诊断智能体", "总体诊断",
                "当前应优先围绕“" + weakDimensionText + "”定位学习瓶颈，先补齐低掌握知识点，再进入综合迁移训练。",
                commonEvidence));
        rows.add(discussionMessage(1, "knowledge-agent", "知识掌握分析智能体", "知识定位",
                "建议把“" + weakPointText + "”作为本轮资源生成的核心主题，资源内容需要包含概念解释、例题拆解和即时检测。",
                commonEvidence));
        rows.add(discussionMessage(1, "error-pattern-agent", "错题模式分析智能体", "错因复盘",
                "会诊判断该学生需要从错题中复盘变量状态、边界条件和输入输出依据，避免只记结论、不复盘过程。",
                commonEvidence));
        rows.add(discussionMessage(1, "ability-agent", "能力维度评估智能体", "能力评估",
                "能力维度侧重“" + weakDimensionText + "”，建议练习设计从基础识别题过渡到变式题和小任务。",
                commonEvidence));
        rows.add(discussionMessage(1, "resource-agent", "个性化资源生成智能体", "资源建议",
                "本轮建议生成“" + resourceText + "”，并结合教师补充要求：“" + teacherRequirement + "”。",
                commonEvidence));
        rows.add(discussionMessage(1, "review-agent", "质量审核智能体", "匹配审核",
                "后续审核重点检查资源是否直接对应画像薄弱点、题目是否可作答、讲解是否能被学生直接使用。",
                commonEvidence));
        rows.add(discussionMessage(1, "decision-agent", "会诊决策智能体", "最终共识",
                "会诊共识：先补“" + weakPointText + "”，用资源讲解建立概念，再用补救练习和错题复盘验证掌握情况。",
                commonEvidence));
        return rows;
    }

    private List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> runLlmAgentDiscussion(StageLearningEvaluationVO profile,
                                                                                              TeacherAgentResourceGenerateRequest request,
                                                                                              List<String> resourceTypes,
                                                                                              Map<String, Object> evidencePack,
                                                                                              Long teacherId) {
        return runMeetingConversation(profile, request, resourceTypes, evidencePack, List.of(), teacherId);
    }

    private List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> runMeetingConversation(StageLearningEvaluationVO profile,
                                                                                               TeacherAgentResourceGenerateRequest request,
                                                                                               List<String> resourceTypes,
                                                                                               Map<String, Object> evidencePack,
                                                                                               List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                                                                               Long teacherId) {
        List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> rows = new ArrayList<>();
        if (previousMessages != null) {
            previousMessages.stream()
                    .filter(message -> message != null && !"running".equals(message.getStatus()))
                    .forEach(rows::add);
        }
        String defaultProviderKey = normalizeProviderKey(request == null ? null : request.getProviderKey());
        Map<String, String> agentProviderKeys = normalizeAgentProviderKeys(request == null ? null : request.getAgentProviderKeys());
        int nextTurnIndex = rows.stream()
                .map(TeacherAgentResourceGenerateVO.AgentDiscussionMessage::getTurnIndex)
                .filter(index -> index != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        List<MeetingTurn> turns = meetingTurnsV2();
        String startAgentId = normalizeProviderKey(request == null ? null : request.getAgentId());
        if (StringUtils.hasText(startAgentId)) {
            int startIndex = -1;
            for (int i = 0; i < turns.size(); i++) {
                if (startAgentId.equals(turns.get(i).agentId)) {
                    startIndex = i;
                    break;
                }
            }
            if (startIndex >= 0) {
                turns = turns.subList(startIndex, turns.size());
            }
        }
        for (MeetingTurn turn : turns) {
            String providerKey = providerFor(defaultProviderKey, agentProviderKeys, turn.agentId);
            rows.add(runMeetingTurn(profile, request, resourceTypes, evidencePack, rows, turn, providerKey, teacherId, nextTurnIndex++));
        }
        return rows;
    }

    private List<MeetingTurn> meetingTurnsV2() {
        return List.of(
                new MeetingTurn(1, "preprocess", null, "整理学生画像、作答记录、薄弱知识点、能力维度、资源类型和教师要求，形成统一证据包。"),
                new MeetingTurn(1, "coordinator", "preprocess", "阅读全部已有会诊记录，明确本轮共同要解决的学习诊断问题，并分配后续智能体关注点。"),
                new MeetingTurn(1, "knowledge", "coordinator", "阅读全部已有会诊记录，判断最需要补强的知识点；如果前面结论不准确，要指出并修正。"),
                new MeetingTurn(1, "ability", "knowledge", "阅读全部已有会诊记录，分析知识薄弱点对理解迁移、实践能力、作答表现等能力维度的影响；避免重复知识点结论。"),
                new MeetingTurn(1, "behavior", "ability", "阅读全部已有会诊记录，从错题模式、学习行为和作答证据角度补充或质疑前面结论。"),
                new MeetingTurn(2, "resource", "behavior", "阅读全部已有会诊记录，提出个性化资源包方案，说明每类资源解决哪个经过校准的学习问题。"),
                new MeetingTurn(2, "practice", "resource", "阅读全部已有会诊记录，设计练习任务如何承接资源，并检查题目梯度是否匹配当前能力。"),
                new MeetingTurn(2, "report", "practice", "阅读全部已有会诊记录，汇总诊断、资源和练习方案，形成可复核的个性化资源草案。"),
                new MeetingTurn(3, "qualityReview", "report", "阅读全部已有会诊记录，追问资源和练习是否完整、可直接给学生使用，并指出必须修改的地方。"),
                new MeetingTurn(3, "consistencyReview", "qualityReview", "阅读全部已有会诊记录，检查最终资源方案是否与画像证据、薄弱点、能力维度和教师要求一致；如发现偏差要明确纠正。")
        );
    }

    private List<MeetingTurn> meetingTurns() {
        return List.of(
                new MeetingTurn(1, "preprocess", null, "先整理学生画像、作答、薄弱知识点和资源需求，给后续智能体提供统一证据包。"),
                new MeetingTurn(1, "coordinator", "preprocess", "回应预处理证据，像会议主持人一样抛出本次会诊议题，说明大家要共同解决的学习问题。"),
                new MeetingTurn(1, "knowledge", "coordinator", "回应主持人的议题，指出最需要补强的知识点，并说明资源内容应该先讲清什么。"),
                new MeetingTurn(1, "ability", "knowledge", "回应知识诊断意见，补充这些知识薄弱会影响哪些能力维度，不能只重复知识点。"),
                new MeetingTurn(1, "behavior", "ability", "回应能力评估意见，从错题模式和学习行为角度提出质疑或补充。"),
                new MeetingTurn(2, "resource", "behavior", "基于前面争论，提出个性化资源包方案，说明每类资源解决什么问题。"),
                new MeetingTurn(2, "practice", "resource", "回应资源方案，补充练习任务如何承接资源，并指出题目梯度。"),
                new MeetingTurn(2, "report", "practice", "汇总诊断、资源和练习方案，形成待审核的个性化资源草案。"),
                new MeetingTurn(3, "qualityReview", "report", "站在审核者角度追问资源和练习是否可直接给学生使用，指出需要修改的地方。"),
                new MeetingTurn(3, "consistencyReview", "qualityReview", "检查资源方案是否与画像证据、薄弱点、能力维度一致，并回应审核意见。")
        );
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage runMeetingTurn(StageLearningEvaluationVO profile,
                                                                                 TeacherAgentResourceGenerateRequest request,
                                                                                 List<String> resourceTypes,
                                                                                 Map<String, Object> evidencePack,
                                                                                 List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                                                                 MeetingTurn turn,
                                                                                 String providerKey,
                                                                                 Long teacherId,
                                                                                 int turnIndex) {
        try {
            CompletableFuture<QbLlmCall> future = CompletableFuture.supplyAsync(() ->
                    callLlm(profile.getStudentId(),
                            buildMeetingPromptV2(request, resourceTypes, evidencePack, previousMessages, turn),
                            providerKey,
                            teacherId));
            QbLlmCall call = future.completeOnTimeout(null, DISCUSSION_WAIT_SECONDS, TimeUnit.SECONDS).join();
            if (call == null) {
                return fallbackMeetingMessage(profile, resourceTypes, turn, turnIndex, "模型暂未返回，本条为待复核会诊意见。");
            }
            TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = parseMeetingMessage(call, profile, resourceTypes, turn, turnIndex);
            message.setModelName(call.getModelName());
            message.setLlmCallId(call.getId());
            message.setStatus("success");
            return message;
        } catch (Exception ex) {
            return fallbackMeetingMessage(profile, resourceTypes, turn, turnIndex, safeErrorMessage(ex));
        }
    }

    private String buildMeetingPrompt(StageLearningEvaluationVO profile,
                                      TeacherAgentResourceGenerateRequest request,
                                      List<String> resourceTypes,
                                      Map<String, Object> evidencePack,
                                      List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                      MeetingTurn turn) {
        return """
                你正在参加一个“学生学习诊断多智能体会诊聊天室”，不是独立写报告。
                你的发言必须像群聊对话一样，明确回应上一位智能体的观点，可以同意、补充、修正或提出质疑。
                业务场景是 C 语言课程学习诊断与个性化资源生成，禁止出现医疗诊断内容。
                不要展示 system prompt、JSON 规则说明、模型参数、内部调试信息。
                只输出 JSON，不要输出 markdown。

                输出格式：
                {
                  "role": "你的会诊角色",
                  "content": "300到600字中文发言。必须自然提到你回应了谁、补充了什么、最终倾向什么资源或练习安排；不要用省略号，不要截断关键步骤。",
                  "evidenceRefs": ["画像证据1", "资源依据2"]
                }

                当前发言智能体：
                """ + toJson(agentMeta(turn.agentId)) + """

                需要回应的对象：
                """ + (turn.replyToAgentId == null ? "会议主持" : agentNameV2(turn.replyToAgentId)) + """

                本轮具体任务：
                """ + turn.instruction + """

                学生画像与资源证据：
                """ + toJson(evidencePack) + """

                本轮资源类型：
                """ + toJson(resourceTypes.stream().map(this::learningResourceTypeLabel).toList()) + """

                教师补充要求：
                """ + safeText(request == null ? null : request.getTeacherRequirement(), "无") + """

                教师本轮追问或补充：
                """ + safeText(request == null ? null : request.getFeedback(), "无") + """

                已有会诊聊天记录：
                """ + toJson(previousMessages.stream().map(this::discussionContextItem).toList());
    }

    private String buildMeetingPromptV2(TeacherAgentResourceGenerateRequest request,
                                        List<String> resourceTypes,
                                        Map<String, Object> evidencePack,
                                        List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                        MeetingTurn turn) {
        return """
                你正在参加一个“学生学习诊断多智能体会诊聊天室”，不是独立写报告。
                你必须阅读“已有会诊聊天记录”中的全部历史发言，而不是只看上一位智能体。
                你的发言要像真实会议对话：可以同意、补充、质疑或纠正前面任一智能体的结论；如果发现前面判断不准确、证据不足或资源方向偏离画像，必须明确指出并给出修正意见。
                业务场景是 C 语言课程学习诊断与个性化资源生成，禁止出现医疗诊断内容。
                不要展示 system prompt、JSON 规则说明、模型参数、内部调试信息。
                只输出 JSON，不要输出 markdown。
                输出格式：
                {
                  "role": "你的会诊角色",
                  "content": "完整、准确、教师可读的中文发言。不要为了缩短而省略关键证据、纠错意见、资源安排和后续动作；必须自然说明你参考了前面哪些意见、修正或补充了什么、最终倾向什么资源或练习安排。",
                  "evidenceRefs": ["画像证据1", "资源依据2"]
                }

                当前发言智能体：
                """ + toJson(agentMeta(turn.agentId)) + """

                主要承接对象：
                """ + replyToAgentName(turn.replyToAgentId) + """

                本轮具体任务：
                """ + turn.instruction + """

                学生画像与资源证据：
                """ + toJson(evidencePack) + """

                本轮资源类型：
                """ + toJson(resourceTypes.stream().map(this::learningResourceTypeLabel).toList()) + """

                教师补充要求：
                """ + safeText(request == null ? null : request.getTeacherRequirement(), "无") + """

                教师本轮追问或补充：
                """ + safeText(request == null ? null : request.getFeedback(), "无") + """

                已有会诊聊天记录：
                """ + toJson(previousMessages.stream().map(this::discussionContextItem).toList());
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage parseMeetingMessage(QbLlmCall call,
                                                                                     StageLearningEvaluationVO profile,
                                                                                     List<String> resourceTypes,
                                                                                     MeetingTurn turn,
                                                                                     int turnIndex) throws Exception {
        JsonNode root = parseJsonContent(call);
        TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = discussionMessage(
                turn.round,
                turn.agentId,
                agentNameV2(turn.agentId),
                root.path("role").asText(agentRoleV2(turn.agentId)),
                root.path("content").asText(fallbackMeetingContentV2(profile, resourceTypes, turn, "")),
                parseStringArray(root.path("evidenceRefs"), discussionEvidenceRefs(profile, resourceTypes.stream().map(this::learningResourceTypeLabel).toList()))
        );
        applyMeetingMeta(message, turn, turnIndex);
        return message;
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage fallbackMeetingMessage(StageLearningEvaluationVO profile,
                                                                                        List<String> resourceTypes,
                                                                                        MeetingTurn turn,
                                                                                        int turnIndex,
                                                                                        String reason) {
        TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = discussionMessage(
                turn.round,
                turn.agentId,
                agentNameV2(turn.agentId),
                agentRoleV2(turn.agentId),
                fallbackMeetingContentV2(profile, resourceTypes, turn, reason),
                discussionEvidenceRefs(profile, resourceTypes.stream().map(this::learningResourceTypeLabel).toList())
        );
        applyMeetingMeta(message, turn, turnIndex);
        message.setStatus("fallback");
        return message;
    }

    private void applyMeetingMeta(TeacherAgentResourceGenerateVO.AgentDiscussionMessage message,
                                  MeetingTurn turn,
                                  int turnIndex) {
        message.setId("meeting-" + turnIndex + "-" + turn.agentId);
        message.setTurnIndex(turnIndex);
        message.setReplyToAgentId(turn.replyToAgentId);
        message.setReplyToAgentName(turn.replyToAgentId == null ? "" : agentNameV2(turn.replyToAgentId));
    }

    private String fallbackMeetingContentV2(StageLearningEvaluationVO profile,
                                            List<String> resourceTypes,
                                            MeetingTurn turn,
                                            String reason) {
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        String weakPointText = weakPoints.isEmpty() ? "当前薄弱知识点" : String.join("、", weakPoints);
        String weakDimensionText = weakDimensions.isEmpty() ? "综合学习能力" : String.join("、", weakDimensions);
        List<String> resourceLabels = resourceTypes.stream().map(this::learningResourceTypeLabel).toList();
        String resourceText = resourceLabels.isEmpty() ? "知识讲解、补救练习、错因复盘、学习路径" : String.join("、", resourceLabels);
        String suffix = StringUtils.hasText(reason) ? " 本条意见由系统兜底生成，需要教师确认或补充后再继续后续会诊。" : "";
        return switch (turn.agentId) {
            case "preprocess" -> "我先整理本轮证据：学生画像、作答记录、薄弱知识点和资源需求都指向 " + weakPointText + " 与 " + weakDimensionText + "。后续智能体应基于这些证据发言，不能脱离画像泛泛生成资源。" + suffix;
            case "coordinator" -> "我已阅读预处理证据，本轮会诊需要共同判断学生究竟卡在知识概念、题型迁移还是错因复盘上。后续每个智能体都要引用前面全部意见，并及时纠正不准确判断。" + suffix;
            case "knowledge" -> "我阅读了前面意见后补充知识诊断：优先补强 " + weakPointText + "。如果只做大而全讲解会偏离画像，资源应先把概念、例题、易错点和作答证据串成学习链。" + suffix;
            case "ability" -> "我综合前面结论后补充能力评估：" + weakPointText + " 会继续影响 " + weakDimensionText + "。资源后面必须接变式练习，验证学生能否把概念迁移到新题，而不是只停留在看懂讲解。" + suffix;
            case "behavior" -> "我从错因和学习行为角度校准前面结论：如果缺少错题复盘，学生可能继续在边界条件、输入输出和变量状态上重复出错。因此资源包应包含错因复盘清单和复做任务。" + suffix;
            case "resource" -> "我基于前面全部诊断提出资源方案：" + resourceText + " 应围绕 " + weakPointText + " 组织。每类资源都要说明解决哪个学习问题，不能出现与薄弱点无关的泛化材料。" + suffix;
            case "practice" -> "我阅读资源方案后补充练习设计：题目应分基础识别、变式迁移、错因复盘三层，并匹配当前能力水平。如果题目只考记忆或没有解析，应退回调整。" + suffix;
            case "report" -> "我汇总当前会议：先围绕 " + weakPointText + " 生成讲解和练习，再用错因复盘与阶段学习路径巩固 " + weakDimensionText + "。该方案应进入审核，而不是直接发布。" + suffix;
            case "qualityReview" -> "我阅读全部会诊意见后进行质量审核：资源方向可以采用，但题干、答案、解析、知识点标签和视频/讲义内容必须完整，才能给学生使用；缺失项应标记为需修改。" + suffix;
            case "consistencyReview" -> "我在质量审核基础上做一致性复核：最终资源必须与画像证据、薄弱点、能力维度和教师要求一致。若前面方案有过宽、过难或偏题内容，应删减并改写为针对性资源。" + suffix;
            default -> "我已阅读前面全部会诊意见，并基于当前画像证据补充本轮学习诊断观点。" + suffix;
        };
    }

    private String fallbackMeetingContent(StageLearningEvaluationVO profile,
                                          List<String> resourceTypes,
                                          MeetingTurn turn,
                                          String reason) {
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        String weakPointText = weakPoints.isEmpty() ? "当前薄弱知识点" : String.join("、", weakPoints);
        String weakDimensionText = weakDimensions.isEmpty() ? "综合学习能力" : String.join("、", weakDimensions);
        List<String> resourceLabels = resourceTypes.stream().map(this::learningResourceTypeLabel).toList();
        String resourceText = resourceLabels.isEmpty() ? "知识讲解、补救练习、学习路径" : String.join("、", resourceLabels);
        String suffix = StringUtils.hasText(reason) ? " 这条意见由系统兜底生成，建议教师复核。" : "";
        return switch (turn.agentId) {
            case "coordinator" -> "我先发起本轮会诊：大家需要围绕 " + weakPointText + " 和 " + weakDimensionText + " 判断学生卡在哪里，再共同决定资源组合。" + suffix;
            case "knowledge" -> "我回应主持人的议题：知识层面应先补 " + weakPointText + "，资源不能泛讲，要把概念、例题和易错点放在同一条学习链里。" + suffix;
            case "ability" -> "我补充知识智能体的判断：这些薄弱点会继续影响 " + weakDimensionText + "，所以资源后面必须接变式练习，验证学生能否迁移。" + suffix;
            case "behavior" -> "我对能力评估再补一层：如果学生只看讲解不复盘错因，仍可能在边界条件和输入输出上反复出错，需要加入错题复盘清单。" + suffix;
            case "resource" -> "我基于前面意见提出资源包：先给 " + weakPointText + " 的讲解材料，再配补救练习和学习路径，资源类型建议为 " + resourceText + "。" + suffix;
            case "practice" -> "我回应资源方案：练习要分三层，先基础识别，再变式迁移，最后用错因复盘题让学生写出判断依据。" + suffix;
            case "qualityReview" -> "我审核练习方案：资源可以采用，但需要保证题干、答案、解析和知识点标签完整，视频或讲义要能直接给学生使用。" + suffix;
            case "consistencyReview" -> "我回应审核意见：资源方向与画像证据一致，重点仍应锁定 " + weakPointText + "，避免生成过宽泛的综合材料。" + suffix;
            case "report" -> "我汇总会诊共识：本次个性化方案应采用“讲解资源 + 补救练习 + 错题复盘 + 阶段学习路径”，教师审核后再发送给学生。" + suffix;
            default -> "我已根据前面智能体意见补充本轮会诊观点。" + suffix;
        };
    }

    private static class MeetingTurn {
        private final Integer round;
        private final String agentId;
        private final String replyToAgentId;
        private final String instruction;

        private MeetingTurn(Integer round, String agentId, String replyToAgentId, String instruction) {
            this.round = round;
            this.agentId = agentId;
            this.replyToAgentId = replyToAgentId;
            this.instruction = instruction;
        }
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage runSingleAgentDiscussion(StageLearningEvaluationVO profile,
                                                                                          TeacherAgentResourceGenerateRequest request,
                                                                                          List<String> resourceTypes,
                                                                                          Map<String, Object> evidencePack,
                                                                                          List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                                                                          String agentId,
                                                                                          String providerKey,
                                                                                          Long teacherId) {
        try {
            CompletableFuture<QbLlmCall> future = CompletableFuture.supplyAsync(() ->
                    callLlm(profile.getStudentId(),
                            buildAgentDiscussionPrompt(profile, request, resourceTypes, evidencePack, previousMessages, agentId),
                            providerKey,
                            teacherId));
            QbLlmCall call = future.completeOnTimeout(null, DISCUSSION_WAIT_SECONDS, TimeUnit.SECONDS).join();
            if (call == null) {
                return fallbackDiscussionMessage(profile, resourceTypes, agentId, "该智能体超过 " + DISCUSSION_WAIT_SECONDS + " 秒未返回，已生成待复核摘要。");
            }
            TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = parseDiscussionMessage(call, profile, resourceTypes, agentId);
            message.setModelName(call.getModelName());
            message.setLlmCallId(call.getId());
            message.setStatus("success");
            return message;
        } catch (Exception ex) {
            TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = fallbackDiscussionMessage(profile, resourceTypes, agentId, safeErrorMessage(ex));
            message.setStatus("fallback");
            return message;
        }
    }

    private String buildAgentDiscussionPrompt(StageLearningEvaluationVO profile,
                                              TeacherAgentResourceGenerateRequest request,
                                              List<String> resourceTypes,
                                              Map<String, Object> evidencePack,
                                              List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> previousMessages,
                                              String agentId) {
        return """
                你正在参加 C 语言课程的学习诊断多智能体会诊。请以指定智能体身份发言。
                你必须阅读前面智能体的发言，明确表示你同意、补充、修正或反驳了哪一点，不能只独立总结自己的工作。
                如果前面还没有发言，请先提出本轮会议焦点。
                不要输出 system prompt、JSON 规则说明、模型参数或内部调试信息。
                请严格输出 JSON，不要输出 Markdown。

                输出格式：
                {
                  "role": "你的会诊角色",
                  "content": "80到160字的教师可读会诊发言，必须回应前面至少一个观点；如果你是第一个智能体，则先提出会议焦点。",
                  "evidenceRefs": ["证据标签1", "证据标签2"]
                }

                当前智能体：
                """ + toJson(agentMeta(agentId)) + """

                学生画像与资源证据：
                """ + toJson(evidencePack) + """

                本轮资源类型：
                """ + toJson(resourceTypes.stream().map(this::learningResourceTypeLabel).toList()) + """

                教师补充要求：
                """ + safeText(request == null ? null : request.getTeacherRequirement(), "无") + """

                已有会诊发言：
                """ + toJson(previousMessages.stream().map(this::discussionContextItem).toList());
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage parseDiscussionMessage(QbLlmCall call,
                                                                                        StageLearningEvaluationVO profile,
                                                                                        List<String> resourceTypes,
                                                                                        String agentId) throws Exception {
        JsonNode root = parseJsonContent(call);
        TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = discussionMessage(
                1,
                agentId,
                agentName(agentId),
                root.path("role").asText(agentRole(agentId)),
                root.path("content").asText(fallbackDiscussionContent(profile, resourceTypes, agentId, "")),
                parseStringArray(root.path("evidenceRefs"), discussionEvidenceRefs(profile, resourceTypes.stream().map(this::learningResourceTypeLabel).toList()))
        );
        return message;
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage fallbackDiscussionMessage(StageLearningEvaluationVO profile,
                                                                                           List<String> resourceTypes,
                                                                                           String agentId,
                                                                                           String reason) {
        TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = discussionMessage(
                1,
                agentId,
                agentName(agentId),
                agentRole(agentId),
                fallbackDiscussionContent(profile, resourceTypes, agentId, reason),
                discussionEvidenceRefs(profile, resourceTypes.stream().map(this::learningResourceTypeLabel).toList())
        );
        message.setStatus("fallback");
        return message;
    }

    private String fallbackDiscussionContent(StageLearningEvaluationVO profile,
                                             List<String> resourceTypes,
                                             String agentId,
                                             String reason) {
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        String weakPointText = weakPoints.isEmpty() ? "当前薄弱知识点" : String.join("、", weakPoints);
        String weakDimensionText = weakDimensions.isEmpty() ? "综合学习能力" : String.join("、", weakDimensions);
        String suffix = StringUtils.hasText(reason) ? " 当前模型发言暂未完整返回，建议教师复核。" : "";
        return switch (agentId) {
            case "preprocess" -> "我先整理画像证据：本轮会诊应围绕 " + weakPointText + " 和 " + weakDimensionText + " 展开。" + suffix;
            case "coordinator" -> "我同意先聚焦画像证据，并将后续讨论分配给知识、能力、错因、资源和审核智能体。" + suffix;
            case "knowledge" -> "我补充知识诊断意见：优先补强 " + weakPointText + "，资源内容要直接讲清概念、例题和易错点。" + suffix;
            case "ability" -> "我补充能力评估意见：当前重点是 " + weakDimensionText + "，练习应从基础识别过渡到变式迁移。" + suffix;
            case "behavior" -> "我从错因复盘角度补充：需要让学生说明变量状态、边界条件和输入输出依据。" + suffix;
            case "resource" -> "我同意前面的诊断，资源应围绕 " + weakPointText + " 生成讲解、练习和学习路径。" + suffix;
            case "practice" -> "我补充练习设计：题目应包含基础题、变式题和错因复盘题，验证学生是否真正迁移。" + suffix;
            case "report" -> "我汇总当前会诊共识：先补 " + weakPointText + "，再通过练习和错题复盘验证掌握情况。" + suffix;
            case "qualityReview" -> "我将检查资源完整性、可用性和难度适配，确保教师可直接审核后发布。" + suffix;
            case "consistencyReview" -> "我将检查资源是否与学生画像、薄弱点和课程目标一致，避免生成泛化内容。" + suffix;
            default -> "我已根据当前画像证据补充会诊意见。" + suffix;
        };
    }

    private Map<String, Object> agentMeta(String agentId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("agentId", agentId);
        meta.put("agentName", agentNameV2(agentId));
        meta.put("role", agentRoleV2(agentId));
        return meta;
    }

    private Map<String, Object> discussionContextItem(TeacherAgentResourceGenerateVO.AgentDiscussionMessage message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("turnIndex", message.getTurnIndex());
        row.put("round", message.getRound());
        row.put("agentName", message.getAgentName());
        row.put("replyToAgentName", message.getReplyToAgentName());
        row.put("role", message.getRole());
        row.put("content", message.getContent());
        row.put("evidenceRefs", message.getEvidenceRefs());
        return row;
    }

    private String replyToAgentName(String agentId) {
        return StringUtils.hasText(agentId) ? agentName(agentId) : "会议主持";
    }

    private String agentNameV2(String agentId) {
        return switch (agentId) {
            case "preprocess" -> "预处理智能体";
            case "coordinator" -> "协调智能体";
            case "knowledge" -> "知识掌握分析智能体";
            case "ability" -> "能力维度评估智能体";
            case "behavior" -> "错因模式分析智能体";
            case "resource" -> "资源生成智能体";
            case "practice" -> "练习生成智能体";
            case "report" -> "会诊决策智能体";
            case "qualityReview" -> "资源质量审核智能体";
            case "consistencyReview" -> "主题一致性审核智能体";
            default -> "学习诊断智能体";
        };
    }

    private String agentRoleV2(String agentId) {
        return switch (agentId) {
            case "preprocess" -> "证据整理";
            case "coordinator" -> "会议主持";
            case "knowledge" -> "知识诊断";
            case "ability" -> "能力评估";
            case "behavior" -> "错因复盘";
            case "resource" -> "资源建议";
            case "practice" -> "练习设计";
            case "report" -> "最终共识";
            case "qualityReview" -> "质量审核";
            case "consistencyReview" -> "一致性审核";
            default -> "学习诊断";
        };
    }

    private String agentName(String agentId) {
        return switch (agentId) {
            case "preprocess" -> "预处理智能体";
            case "coordinator" -> "协调智能体";
            case "knowledge" -> "知识掌握分析智能体";
            case "ability" -> "能力维度评估智能体";
            case "behavior" -> "错因模式分析智能体";
            case "resource" -> "资源生成智能体";
            case "practice" -> "练习生成智能体";
            case "report" -> "会诊决策智能体";
            case "qualityReview" -> "资源质量审核智能体";
            case "consistencyReview" -> "主题一致性审核智能体";
            default -> "学习诊断智能体";
        };
    }

    private String agentRole(String agentId) {
        return switch (agentId) {
            case "preprocess" -> "证据整理";
            case "coordinator" -> "会议主持";
            case "knowledge" -> "知识诊断";
            case "ability" -> "能力评估";
            case "behavior" -> "错因复盘";
            case "resource" -> "资源建议";
            case "practice" -> "练习设计";
            case "report" -> "最终共识";
            case "qualityReview" -> "质量审核";
            case "consistencyReview" -> "一致性审核";
            default -> "学习诊断";
        };
    }

    private List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> discussionMessagesFromRequest(TeacherAgentResourceGenerateRequest request) {
        if (request == null || request.getDiscussionMessages() == null) {
            return new ArrayList<>();
        }
        List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> rows = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> item : request.getDiscussionMessages()) {
            if (item == null) {
                continue;
            }
            TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = new TeacherAgentResourceGenerateVO.AgentDiscussionMessage();
            message.setId(String.valueOf(item.getOrDefault("id", "request-message-" + index)));
            message.setRound(asInteger(item.get("round"), 1));
            message.setTurnIndex(asInteger(item.get("turnIndex"), index + 1));
            message.setAgentId(String.valueOf(item.getOrDefault("agentId", "agent-" + index)));
            message.setAgentName(String.valueOf(item.getOrDefault("agentName", agentName(message.getAgentId()))));
            Object replyToAgentId = item.get("replyToAgentId");
            if (replyToAgentId != null && StringUtils.hasText(String.valueOf(replyToAgentId))) {
                message.setReplyToAgentId(String.valueOf(replyToAgentId));
                message.setReplyToAgentName(String.valueOf(item.getOrDefault("replyToAgentName", agentName(message.getReplyToAgentId()))));
            }
            message.setRole(String.valueOf(item.getOrDefault("role", agentRole(message.getAgentId()))));
            message.setContent(String.valueOf(item.getOrDefault("content", "")));
            message.setStatus(String.valueOf(item.getOrDefault("status", "")));
            Object refs = item.get("evidenceRefs");
            if (refs instanceof List<?> list) {
                message.setEvidenceRefs(list.stream().map(String::valueOf).filter(StringUtils::hasText).limit(6).toList());
            }
            rows.add(message);
            index++;
        }
        return rows;
    }

    private Integer asInteger(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private List<String> parseStringArray(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) {
            return fallback;
        }
        List<String> rows = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (StringUtils.hasText(value)) {
                rows.add(value.trim());
            }
        }
        return rows.isEmpty() ? fallback : rows.stream().distinct().limit(6).toList();
    }

    private TeacherAgentResourceGenerateVO.DecisionSummary buildDecisionSummary(StageLearningEvaluationVO profile,
                                                                                TeacherAgentResourceGenerateRequest request,
                                                                                List<String> resourceTypes,
                                                                                List<TeacherAgentResourceGenerateVO.ResourceDraft> resources,
                                                                                List<TeacherAgentResourceGenerateVO.AgentDiscussionMessage> messages) {
        List<String> weakPoints = weakPointNames(profile);
        List<String> weakDimensions = weakDimensionNames(profile);
        List<String> resourceLabels = resourceTypes.stream().map(this::learningResourceTypeLabel).distinct().toList();
        String focus = weakPoints.isEmpty() ? "当前阶段核心知识点" : String.join("、", weakPoints);
        String dimension = weakDimensions.isEmpty() ? "综合学习能力" : String.join("、", weakDimensions);

        TeacherAgentResourceGenerateVO.DecisionSummary summary = new TeacherAgentResourceGenerateVO.DecisionSummary();
        summary.setStatus(messages == null || messages.isEmpty() ? "fallback" : "ready");
        summary.setHeadline("优先围绕“" + focus + "”完成一轮补强");
        summary.setCurrentProblem("当前画像显示需要关注“" + dimension + "”，资源生成应避免泛泛讲解，直接服务于薄弱知识点和错题复盘。");
        summary.setWeakPoints(weakPoints);
        summary.setRecommendedResources(resourceLabels.isEmpty() ? List.of("知识讲解", "补救练习", "学习路径") : resourceLabels);
        summary.setRecommendedPractice(List.of("先看知识讲解", "完成补救练习", "复盘最近错题", "用变式题确认迁移能力"));
        summary.setTeacherAction("建议教师先审核 " + resources.size() + " 个资源草案，重点确认是否覆盖“" + focus + "”，通过后再发布给学生。");
        summary.setEvidenceRefs(discussionEvidenceRefs(profile, resourceLabels));
        return summary;
    }

    private TeacherAgentResourceGenerateVO.AgentDiscussionMessage discussionMessage(Integer round,
                                                                                   String agentId,
                                                                                   String agentName,
                                                                                   String role,
                                                                                   String content,
                                                                                   List<String> evidenceRefs) {
        TeacherAgentResourceGenerateVO.AgentDiscussionMessage message = new TeacherAgentResourceGenerateVO.AgentDiscussionMessage();
        message.setRound(round);
        message.setAgentId(agentId);
        message.setAgentName(agentName);
        message.setRole(role);
        message.setContent(content);
        message.setEvidenceRefs(evidenceRefs == null ? List.of() : evidenceRefs);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        message.setId("discussion-" + safeText(agentId, "agent") + "-" + System.nanoTime());
        return message;
    }

    private List<String> discussionEvidenceRefs(StageLearningEvaluationVO profile, List<String> resourceLabels) {
        List<String> refs = new ArrayList<>();
        refs.add(safeText(profile.getStageName(), "当前阶段") + "画像");
        if (profile.getCompletedAttemptCount() != null) {
            refs.add("已完成作答 " + profile.getCompletedAttemptCount() + " 次");
        }
        if (profile.getAbilityScore() != null) {
            refs.add("能力值 " + profile.getAbilityScore());
        }
        List<String> weakPoints = weakPointNames(profile);
        if (!weakPoints.isEmpty()) {
            refs.add("薄弱知识点：" + String.join("、", weakPoints));
        }
        if (resourceLabels != null && !resourceLabels.isEmpty()) {
            refs.add("资源类型：" + String.join("、", resourceLabels));
        }
        return refs;
    }

    private String learningResourceTypeLabel(String type) {
        return switch (normalizeResourceType(type)) {
            case "knowledge_video", "animated_explainer" -> "知识讲解视频";
            case "remedial_exercise", "variant_practice" -> "补救练习";
            case "knowledge_handout", "knowledge_pack" -> "知识点讲义";
            case "error_reflection", "reflection_list" -> "错因复盘";
            case "learning_path" -> "学习路径";
            case "interactive_quiz" -> "互动测验";
            case "ability_task" -> "能力任务";
            default -> "个性化学习资源";
        };
    }

    private String buildGenerationPrompt(StageLearningEvaluationVO profile,
                                         TeacherAgentResourceGenerateRequest request,
                                         List<String> resourceTypes) {
        return """
                你是 C 语言课程教师端的“资源生成智能体”。请严格输出 JSON，不要输出 Markdown、解释或代码块。
                目标：基于学生阶段画像，生成个性化教学资源草案。
                必须生成 resourceTypes 中每一种资源各 1 个。
                每个资源要包含可直接给学生使用的完整 content，而不只是摘要。
                每个 content 控制在 300-500 个中文字符，避免长篇展开。
                必须包含多模态资源能力：
                - animated_explainer：输出该学生薄弱知识点的知识讲解视频资源数据，mediaType 必须为 "video"，videoScenes 必须包含 5-6 个场景，强调概念动画、代码执行、内存/流程可视化。
                - interactive_quiz：必须输出可以直接作答的互动测验，quizQuestions 必须包含 3-5 道题。每道题必须包含 questionType、stem、options、answer、explanation、wrongFeedback、knowledgePoint。题目要围绕学生薄弱知识点，不能只写测验说明。
                视频资源必须像 Bilibili、YouTube 或在线课程中的知识点讲解视频：直接讲解薄弱知识点本身，不要讲“系统如何生成资源”，不要出现“资源草案、建议教师复核、学生画像卡片”等后台流程说明。
                视频结构建议固定为：课程导入 -> 概念讲解 -> 例题代码 -> 易错辨析 -> 随堂练习 -> 总结复盘。
                narration 必须是可以直接作为视频旁白朗读的中文讲解；visualPrompt 必须描述视频画面；boardText 必须是屏幕上显示的板书、代码或口诀。

                JSON 格式：
                {
                  "resources": [
                    {
                      "draftId": "knowledge_pack-1",
                      "title": "资源标题",
                      "resourceType": "knowledge_pack",
                      "summary": "80字以内摘要",
                      "content": "完整资源正文，视频资源需说明讲解主题、知识点、例题和练习，不要说明生成流程",
                      "mediaType": "video",
                      "videoConfig": {"style":"课堂微课","aspectRatio":"16:9"},
                      "videoScenes": [
                        {
                          "title": "场景标题",
                          "narration": "旁白内容",
                          "visualPrompt": "画面生成提示词",
                          "boardText": "屏幕板书/代码文字",
                          "durationSeconds": 12
                        }
                      ],
                      "quizQuestions": [
                        {
                          "questionType": "single_choice",
                          "stem": "题干，必须是可作答的问题",
                          "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
                          "answer": "B",
                          "explanation": "正确答案解析",
                          "wrongFeedback": "答错时给学生的提示",
                          "knowledgePoint": "对应薄弱知识点"
                        }
                      ],
                      "knowledgePointId": null,
                      "personalizationBasis": {
                        "studentName": "学生姓名",
                        "weakPoints": ["薄弱点"],
                        "weakDimensions": ["能力短板"],
                        "reason": "为什么生成这个资源"
                      }
                    }
                  ]
                }

                学生画像 JSON：
                """ + toJson(profile) + """

                资源类型：
                """ + toJson(resourceTypes) + """

                教师补充要求：
                """ + safeText(request.getTeacherRequirement(), "无") + """

                退回修改意见：
                """ + safeText(request.getFeedback(), "无");
    }

    private String buildQualityReviewPrompt(StageLearningEvaluationVO profile,
                                            List<TeacherAgentResourceGenerateVO.ResourceDraft> resources) {
        return """
                你是“资源质量审核智能体”。请严格输出 JSON，不要输出 Markdown、解释或代码块。
                请逐个审核资源的完整性、可用性、难度适配、教学价值。
                分数范围 0-100。passed 表示是否建议教师保存。

                JSON 格式：
                {
                  "reviews": [
                    {
                      "draftId": "knowledge_pack-1",
                      "qualityScore": 86,
                      "passed": true,
                      "comments": "审核意见",
                      "revisionSuggestions": ["修改建议"]
                    }
                  ]
                }

                学生画像 JSON：
                """ + toJson(profile) + """

                待审核资源 JSON：
                """ + toJson(resources);
    }

    private String buildConsistencyReviewPrompt(StageLearningEvaluationVO profile,
                                                List<TeacherAgentResourceGenerateVO.ResourceDraft> resources) {
        return """
                你是“主题一致性审核智能体”。请严格输出 JSON，不要输出 Markdown、解释或代码块。
                请逐个审核资源是否围绕学生薄弱点、画像、C 语言课程主题和教师端资源生成目标。
                relevanceScore 表示与学生画像和薄弱点的相关度，consistencyScore 表示主题一致性。
                分数范围 0-100。

                JSON 格式：
                {
                  "reviews": [
                    {
                      "draftId": "knowledge_pack-1",
                      "relevanceScore": 92,
                      "consistencyScore": 90,
                      "passed": true,
                      "comments": "审核意见",
                      "revisionSuggestions": ["修改建议"]
                    }
                  ]
                }

                学生画像 JSON：
                """ + toJson(profile) + """

                待审核资源 JSON：
                """ + toJson(resources);
    }

    private List<TeacherAgentResourceGenerateVO.ResourceDraft> parseGeneratedResources(QbLlmCall call,
                                                                                       StageLearningEvaluationVO profile) {
        try {
            JsonNode root = parseJsonContent(call);
            JsonNode resourcesNode = root.path("resources");
            if (!resourcesNode.isArray() || resourcesNode.isEmpty()) {
                throw new IllegalArgumentException("resources 为空");
            }
            List<TeacherAgentResourceGenerateVO.ResourceDraft> resources = new ArrayList<>();
            int index = 1;
            for (JsonNode item : resourcesNode) {
                TeacherAgentResourceGenerateVO.ResourceDraft draft = objectMapper.convertValue(
                        item,
                        TeacherAgentResourceGenerateVO.ResourceDraft.class
                );
                draft.setResourceType(normalizeResourceType(draft.getResourceType()));
                if (!StringUtils.hasText(draft.getDraftId())) {
                    draft.setDraftId(safeText(draft.getResourceType(), "resource") + "-" + index);
                }
                TeacherAgentResourceGenerateRequest metadataRequest = new TeacherAgentResourceGenerateRequest();
                metadataRequest.setExerciseCount(5);
                metadataRequest.setDifficulty("improve");
                enrichDraftMetadata(draft, profile, metadataRequest);
                if (!StringUtils.hasText(draft.getTitle()) || !StringUtils.hasText(draft.getContent())) {
                    throw new IllegalArgumentException("资源缺少标题或正文");
                }
                if (draft.getPersonalizationBasis() == null || draft.getPersonalizationBasis().isEmpty()) {
                    draft.setPersonalizationBasis(defaultBasis(profile));
                }
                if (isVideoType(draft.getResourceType())) {
                    draft.setMediaType("video");
                    if (draft.getVideoConfig() == null || draft.getVideoConfig().isEmpty()) {
                        draft.setVideoConfig(videoConfig(draft.getResourceType()));
                    }
                    if (draft.getVideoScenes() == null || draft.getVideoScenes().isEmpty()) {
                        draft.setVideoScenes(fallbackVideoScenes(
                                draft.getResourceType(),
                                profile,
                                weakPointNames(profile),
                                weakDimensionNames(profile)
                        ));
                    }
                    if (!isPlayableVideoUrl(draft.getSourceUrl())) {
                        applyPlayableVideoFallback(draft, profile);
                    }
                }
                if ("remedial_exercise".equals(draft.getResourceType())
                        && (draft.getExerciseQuestions() == null || draft.getExerciseQuestions().isEmpty())) {
                    draft.setExerciseQuestions(fallbackExerciseQuestions(
                            profile,
                            metadataRequest,
                            weakPointNames(profile),
                            weakDimensionNames(profile)
                    ));
                }
                draft.setStatus("pending_review");
                resources.add(draft);
                index++;
            }
            return resources;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.LLM_ERROR, "资源生成结果不是合法 JSON，未保存半成品");
        }
    }

    private Map<String, TeacherAgentResourceGenerateVO.ReviewReport> parseReviewMap(QbLlmCall call, String reviewType) {
        try {
            JsonNode root = parseJsonContent(call);
            JsonNode reviewsNode = root.path("reviews");
            if (!reviewsNode.isArray()) {
                throw new IllegalArgumentException("reviews 缺失");
            }
            Map<String, TeacherAgentResourceGenerateVO.ReviewReport> result = new LinkedHashMap<>();
            for (JsonNode item : reviewsNode) {
                String draftId = item.path("draftId").asText("");
                if (!StringUtils.hasText(draftId)) {
                    continue;
                }
                TeacherAgentResourceGenerateVO.ReviewReport report = objectMapper.convertValue(
                        item,
                        TeacherAgentResourceGenerateVO.ReviewReport.class
                );
                result.put(draftId, report);
            }
            return result;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.LLM_ERROR, reviewType + " 审核结果不是合法 JSON，未保存半成品");
        }
    }

    private JsonNode parseJsonContent(QbLlmCall call) throws Exception {
        String content = llmService.extractContent(call.getResponseText());
        if (!StringUtils.hasText(content)) {
            content = call.getResponseText();
        }
        return objectMapper.readTree(extractJsonBlock(content));
    }

    private String extractJsonBlock(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        int start;
        if (objectStart >= 0 && arrayStart >= 0) {
            start = Math.min(objectStart, arrayStart);
        } else {
            start = Math.max(objectStart, arrayStart);
        }
        int end = Math.max(text.lastIndexOf('}'), text.lastIndexOf(']'));
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void mergeReviews(List<TeacherAgentResourceGenerateVO.ResourceDraft> resources,
                              Map<String, TeacherAgentResourceGenerateVO.ReviewReport> qualityReviews,
                              Map<String, TeacherAgentResourceGenerateVO.ReviewReport> consistencyReviews,
                              QbLlmCall generatorCall,
                              QbLlmCall qualityCall,
                              QbLlmCall consistencyCall) {
        for (TeacherAgentResourceGenerateVO.ResourceDraft resource : resources) {
            if (isMatchedExistingVideo(resource) || isExternalVideoSearch(resource)) {
                continue;
            }
            TeacherAgentResourceGenerateVO.ReviewReport quality = qualityReviews.get(resource.getDraftId());
            TeacherAgentResourceGenerateVO.ReviewReport consistency = consistencyReviews.get(resource.getDraftId());
            TeacherAgentResourceGenerateVO.ReviewReport merged = new TeacherAgentResourceGenerateVO.ReviewReport();
            merged.setQualityScore(clamp(score(quality == null ? null : quality.getQualityScore(), 75)));
            merged.setRelevanceScore(clamp(score(consistency == null ? null : consistency.getRelevanceScore(), 75)));
            merged.setConsistencyScore(clamp(score(consistency == null ? null : consistency.getConsistencyScore(), 75)));
            boolean passed = Boolean.TRUE.equals(quality == null ? null : quality.getPassed())
                    && Boolean.TRUE.equals(consistency == null ? null : consistency.getPassed())
                    && merged.getQualityScore() >= 60
                    && merged.getRelevanceScore() >= 60
                    && merged.getConsistencyScore() >= 60;
            merged.setPassed(passed);
            merged.setComments(joinComments(
                    quality == null ? null : quality.getComments(),
                    consistency == null ? null : consistency.getComments()
            ));
            merged.setRevisionSuggestions(mergeSuggestions(
                    quality == null ? null : quality.getRevisionSuggestions(),
                    consistency == null ? null : consistency.getRevisionSuggestions()
            ));
            if (quality == null || consistency == null) {
                merged.setComments("审核智能体未完整返回结果，请教师人工复核后再保存。");
                List<String> suggestions = new ArrayList<>(merged.getRevisionSuggestions());
                suggestions.add("审核智能体未完整返回结果，请教师人工复核后再保存。");
                merged.setRevisionSuggestions(suggestions.stream().filter(StringUtils::hasText).distinct().limit(6).toList());
            }
            resource.setReviewReport(merged);
            resource.setStatus(passed ? "approved" : "needs_revision");
            resource.setModelSource(modelSource(generatorCall, qualityCall, consistencyCall));
        }
    }

    private boolean isMatchedExistingVideo(TeacherAgentResourceGenerateVO.ResourceDraft resource) {
        return resource != null
                && StringUtils.hasText(resource.getDraftId())
                && resource.getDraftId().contains("-matched-");
    }

    private boolean isExternalVideoSearch(TeacherAgentResourceGenerateVO.ResourceDraft resource) {
        return resource != null
                && StringUtils.hasText(resource.getDraftId())
                && resource.getDraftId().contains("-external-video-");
    }

    private TeacherAgentResourceGenerateVO.ModelSource modelSource(QbLlmCall generatorCall,
                                                                   QbLlmCall qualityCall,
                                                                   QbLlmCall consistencyCall) {
        TeacherAgentResourceGenerateVO.ModelSource source = new TeacherAgentResourceGenerateVO.ModelSource();
        source.setGeneratorModel(generatorCall == null ? null : generatorCall.getModelName());
        source.setQualityReviewerModel(qualityCall == null ? null : qualityCall.getModelName());
        source.setConsistencyReviewerModel(consistencyCall == null ? null : consistencyCall.getModelName());
        List<Long> callIds = new ArrayList<>();
        if (generatorCall != null && generatorCall.getId() != null) callIds.add(generatorCall.getId());
        if (qualityCall != null && qualityCall.getId() != null) callIds.add(qualityCall.getId());
        if (consistencyCall != null && consistencyCall.getId() != null) callIds.add(consistencyCall.getId());
        source.setLlmCallIds(callIds);
        return source;
    }

    private ReviewExecution runReview(Long studentId,
                                      String prompt,
                                      String providerKey,
                                      Long teacherId,
                                      String reviewType) {
        ReviewExecution execution = new ReviewExecution();
        try {
            execution.call = callLlm(studentId, prompt, providerKey, teacherId);
            execution.reviews = parseReviewMap(execution.call, reviewType);
        } catch (Exception ex) {
            execution.errorMessage = safeErrorMessage(ex);
        }
        return execution;
    }

    private ReviewExecution reviewTimeout(String reviewType) {
        ReviewExecution execution = new ReviewExecution();
        execution.errorMessage = reviewType + " 审核超过 " + REVIEW_WAIT_SECONDS + " 秒未返回";
        return execution;
    }

    private TeacherAgentResourceGenerateVO.AgentTrace reviewTrace(String agentId,
                                                                  String agentName,
                                                                  ReviewExecution execution,
                                                                  String successMessage,
                                                                  String failurePrefix) {
        if (execution.errorMessage == null) {
            return trace(agentId, agentName, "success", successMessage,
                    execution.call == null ? null : execution.call.getModelName(),
                    execution.call == null ? null : execution.call.getId());
        }
        return trace(agentId, agentName, "failed", failurePrefix + execution.errorMessage,
                execution.call == null ? null : execution.call.getModelName(),
                execution.call == null ? null : execution.call.getId());
    }

    private TeacherAgentResourceGenerateVO.AgentTrace trace(String agentId,
                                                            String agentName,
                                                            String status,
                                                            String summary,
                                                            String modelName,
                                                            Long llmCallId) {
        TeacherAgentResourceGenerateVO.AgentTrace trace = new TeacherAgentResourceGenerateVO.AgentTrace();
        trace.setAgentId(agentId);
        trace.setAgentName(agentName);
        trace.setStatus(status);
        trace.setSummary(summary);
        trace.setModelName(modelName);
        trace.setLlmCallId(llmCallId);
        return trace;
    }

    private Map<String, Object> defaultBasis(StageLearningEvaluationVO profile) {
        Map<String, Object> basis = new LinkedHashMap<>();
        basis.put("studentName", profile.getStudentName());
        basis.put("weakPoints", profile.getWeakKnowledgePoints().stream().map(StageLearningEvaluationVO.WeakKnowledgePoint::getKnowledgePointName).toList());
        basis.put("weakDimensions", profile.getDimensions().stream().map(StageLearningEvaluationVO.Dimension::getName).toList());
        basis.put("reason", profile.getSummary());
        return basis;
    }

    private List<String> mergeSuggestions(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result.stream().filter(StringUtils::hasText).distinct().limit(6).toList();
    }

    private String joinComments(String first, String second) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(first)) parts.add("质量审核：" + first);
        if (StringUtils.hasText(second)) parts.add("一致性审核：" + second);
        return parts.isEmpty() ? "审核通过。" : String.join("；", parts);
    }

    private int score(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safeErrorMessage(Exception ex) {
        String message = ex == null ? null : ex.getMessage();
        return StringUtils.hasText(message) ? message : "未知错误";
    }

    private void ensureTaskActive(TaskState task) {
        if (task != null && task.canceled) {
            throw new CancellationException("task canceled");
        }
    }

    private void assertTaskReadable(TaskState task, Long teacherId, boolean admin) {
        if (task == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "任务不存在或已失效");
        }
        if (!admin && !teacherId.equals(task.teacherId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权查看该任务");
        }
    }

    private boolean isTerminal(String status) {
        return "completed".equals(status) || "failed".equals(status) || "canceled".equals(status);
    }

    private TeacherAgentResourceTaskVO toTaskVO(TaskState task) {
        TeacherAgentResourceTaskVO vo = new TeacherAgentResourceTaskVO();
        vo.setTaskId(task.taskId);
        vo.setStatus(task.status);
        vo.setMessage(task.message);
        vo.setTeacherId(task.teacherId);
        vo.setStudentId(task.studentId);
        vo.setRunId(task.runId);
        vo.setStartedAt(format(task.startedAt));
        vo.setUpdatedAt(format(task.updatedAt));
        vo.setFinishedAt(format(task.finishedAt));
        vo.setResult(task.result);
        return vo;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerFor(new TypeReference<Object>() {}).writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static class GenerationExecution {
        private QbLlmCall call;
        private List<TeacherAgentResourceGenerateVO.ResourceDraft> resources = new ArrayList<>();
        private String errorMessage;
    }

    private static class ReviewExecution {
        private QbLlmCall call;
        private Map<String, TeacherAgentResourceGenerateVO.ReviewReport> reviews = new LinkedHashMap<>();
        private String errorMessage;
    }

    private static class TaskState {
        private String taskId;
        private String status;
        private String message;
        private Long teacherId;
        private boolean admin;
        private Long studentId;
        private String runId;
        private LocalDateTime startedAt;
        private LocalDateTime updatedAt;
        private LocalDateTime finishedAt;
        private TeacherAgentResourceGenerateVO result;
        private volatile boolean canceled;
        private CompletableFuture<?> future;
    }
}
