package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.bank.dto.QuestionHintRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.*;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.*;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.bank.service.QuestionHintService;
import com.xyz.question_bank_management_system.modules.bank.vo.QuestionHintVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionHintServiceImpl implements QuestionHintService {

    private static final int BIZ_TYPE_STUDENT_HINT = 3;
    private static final int MAX_CONTEXT_CHARS = 6000;

    private final QbAttemptMapper attemptMapper;
    private final QbAttemptQuestionMapper attemptQuestionMapper;
    private final QbQuestionMapper questionMapper;
    private final QbQuestionTagMapper questionTagMapper;
    private final QbKnowledgePointMapper knowledgePointMapper;
    private final QbLearningResourceMapper learningResourceMapper;
    private final LlmService llmService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QuestionHintVO generateHint(Long attemptId, Long attemptQuestionId, Long userId, QuestionHintRequest request) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答不存在");
        }
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该作答");
        }
        if (attempt.getStatus() == null || attempt.getStatus() != 1) {
            throw BizException.of(ErrorCode.FORBIDDEN, "当前作答已结束，不能继续请求提示");
        }

        QbAttemptQuestion attemptQuestion = attemptQuestionMapper.selectById(attemptQuestionId);
        if (attemptQuestion == null || !Objects.equals(attemptQuestion.getAttemptId(), attemptId)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答题目不存在");
        }

        QbQuestion question = questionMapper.selectById(attemptQuestion.getQuestionId());
        List<Long> tagIds = questionTagMapper.selectTagIdsByQuestionId(attemptQuestion.getQuestionId());
        List<String> tagNames = questionTagMapper.selectTagNamesByQuestionId(attemptQuestion.getQuestionId());
        List<QbKnowledgePoint> knowledgePoints = tagIds == null || tagIds.isEmpty()
                ? List.of()
                : knowledgePointMapper.selectByTagIds(tagIds);
        List<Long> knowledgePointIds = knowledgePoints.stream().map(QbKnowledgePoint::getId).toList();
        List<QbLearningResource> resources;
        if (!knowledgePointIds.isEmpty()) {
            resources = learningResourceMapper.selectByKnowledgePointIds(knowledgePointIds, 5);
        } else if (tagIds != null && !tagIds.isEmpty()) {
            resources = learningResourceMapper.selectByTagIds(tagIds, 5);
        } else {
            resources = List.of();
        }

        List<String> contextSources = buildContextSources(tagNames, knowledgePoints, resources, question);
        String prompt = buildPrompt(attemptQuestion, question, tagNames, knowledgePoints, resources, request);
        QbLlmCall call = llmService.chatCompletion(BIZ_TYPE_STUDENT_HINT, attemptQuestionId, prompt,
                request == null ? null : request.getProviderKey(), userId);
        String content = llmService.extractContent(call.getResponseText());
        if (!StringUtils.hasText(content)) {
            content = call.getResponseText();
        }

        QuestionHintVO vo = new QuestionHintVO();
        vo.setLlmCallId(call.getId());
        vo.setHint(content == null ? "暂时无法生成提示，请稍后再试。" : content);
        vo.setContextSources(contextSources);
        return vo;
    }

    private String buildPrompt(QbAttemptQuestion attemptQuestion,
                               QbQuestion question,
                               List<String> tagNames,
                               List<QbKnowledgePoint> knowledgePoints,
                               List<QbLearningResource> resources,
                               QuestionHintRequest request) {
        JsonNode snapshot = readSnapshot(attemptQuestion.getSnapshotJson());
        String stem = text(snapshot.path("stem").asText(null), question == null ? null : question.getStem());
        String title = text(snapshot.path("title").asText(null), question == null ? null : question.getTitle());
        String currentAnswer = request == null ? null : request.getCurrentAnswer();
        String studentQuestion = request == null ? null : request.getQuestion();

        StringBuilder context = new StringBuilder();
        appendLine(context, "题目标题", title);
        appendLine(context, "题干", stem);
        appendLine(context, "题型", String.valueOf(attemptQuestion.getQuestionType()));
        appendLine(context, "难度", String.valueOf(attemptQuestion.getDifficulty()));
        appendLine(context, "标签", join(tagNames));
        appendLine(context, "学生当前答案", currentAnswer);
        appendLine(context, "学生提问", studentQuestion);

        if (!knowledgePoints.isEmpty()) {
            context.append("\n相关知识点:\n");
            for (QbKnowledgePoint point : knowledgePoints) {
                context.append("- ").append(nullToEmpty(point.getName()));
                if (StringUtils.hasText(point.getDescription())) {
                    context.append(": ").append(point.getDescription());
                }
                context.append('\n');
            }
        }
        if (question != null && StringUtils.hasText(question.getAnalysisText())) {
            appendLine(context, "教师/系统已有解析", question.getAnalysisText());
        }
        if (!resources.isEmpty()) {
            context.append("\n本地学习资源摘要:\n");
            for (QbLearningResource resource : resources) {
                context.append("- ").append(nullToEmpty(resource.getTitle()));
                if (StringUtils.hasText(resource.getSummary())) {
                    context.append(": ").append(resource.getSummary());
                }
                context.append('\n');
            }
        }

        String contextText = context.toString();
        if (contextText.length() > MAX_CONTEXT_CHARS) {
            contextText = contextText.substring(0, MAX_CONTEXT_CHARS);
        }

        return """
                你是学生答题过程中的提示助手。请严格遵守：
                1. 只根据下方“本地数据库上下文”提供引导性提示。
                2. 不要直接给出最终答案、完整代码或可直接提交的标准答案。
                3. 如果是编程题，只指出思路、关键语法、边界条件和调试方向。
                4. 如果学生已有答案，优先指出检查方向，不要替学生完成。
                5. 回答控制在 3 到 5 条要点，语言简洁。

                本地数据库上下文：
                %s

                请输出：
                - 可能涉及的知识点
                - 解题思路提示
                - 一个检查或调试建议
                """.formatted(contextText);
    }

    private List<String> buildContextSources(List<String> tagNames,
                                             List<QbKnowledgePoint> knowledgePoints,
                                             List<QbLearningResource> resources,
                                             QbQuestion question) {
        Set<String> sources = new LinkedHashSet<>();
        if (tagNames != null && !tagNames.isEmpty()) {
            sources.add("标签: " + join(tagNames));
        }
        if (knowledgePoints != null) {
            knowledgePoints.stream()
                    .map(QbKnowledgePoint::getName)
                    .filter(StringUtils::hasText)
                    .forEach(name -> sources.add("知识点: " + name));
        }
        if (resources != null) {
            resources.stream()
                    .map(QbLearningResource::getTitle)
                    .filter(StringUtils::hasText)
                    .forEach(title -> sources.add("学习资源: " + title));
        }
        if (question != null && StringUtils.hasText(question.getAnalysisText())) {
            sources.add("题目解析");
        }
        return new ArrayList<>(sources);
    }

    private JsonNode readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readTree(snapshotJson);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(label).append(": ").append(value).append('\n');
    }

    private String text(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().filter(StringUtils::hasText).collect(Collectors.joining(", "));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
