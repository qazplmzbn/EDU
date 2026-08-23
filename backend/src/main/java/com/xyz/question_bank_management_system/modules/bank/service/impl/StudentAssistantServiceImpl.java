package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.modules.bank.dto.StudentAssistantChatRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptQuestionMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.bank.service.StudentAssistantService;
import com.xyz.question_bank_management_system.modules.bank.vo.StudentAssistantChatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentAssistantServiceImpl implements StudentAssistantService {

    private static final int BIZ_TYPE_STUDENT_ASSISTANT = 3;
    private static final int ATTEMPT_STATUS_IN_PROGRESS = 1;
    private static final int ATTEMPT_TYPE_PRACTICE = 2;
    private static final int MAX_CONTEXT_CHARS = 7000;
    private static final int MAX_HISTORY_ITEMS = 8;

    private final QbAttemptMapper attemptMapper;
    private final QbAttemptQuestionMapper attemptQuestionMapper;
    private final LlmService llmService;

    @Override
    public StudentAssistantChatVO chat(Long userId, StudentAssistantChatRequest request) {
        String message = request == null ? "" : request.getMessage();
        if (!StringUtils.hasText(message)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "消息不能为空");
        }

        Map<String, Object> pageContext = request.getPageContext();
        Long attemptId = readLong(pageContext, "attemptId");
        Long attemptQuestionId = readLong(pageContext, "attemptQuestionId");
        QbAttempt attempt = validateAttempt(userId, attemptId);
        QbAttemptQuestion attemptQuestion = validateAttemptQuestion(attemptId, attemptQuestionId);

        String prompt = buildPrompt(request, attempt, attemptQuestion);
        QbLlmCall call = llmService.chatCompletion(
                BIZ_TYPE_STUDENT_ASSISTANT,
                userId,
                prompt,
                request.getProviderKey(),
                userId
        );

        String content = llmService.extractContent(call.getResponseText());
        if (!StringUtils.hasText(content)) {
            content = call.getResponseText();
        }

        StudentAssistantChatVO vo = new StudentAssistantChatVO();
        vo.setReply(StringUtils.hasText(content) ? content : "小C暂时没有生成回复，请稍后再试。");
        vo.setLlmCallId(call.getId());
        vo.setContextUsed(pageContext != null && !pageContext.isEmpty());
        vo.setLockedReason(null);
        return vo;
    }

    private QbAttempt validateAttempt(Long userId, Long attemptId) {
        if (attemptId == null) {
            return null;
        }
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答记录不存在");
        }
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该作答");
        }
        boolean inProgressExam = Objects.equals(attempt.getStatus(), ATTEMPT_STATUS_IN_PROGRESS)
                && !Objects.equals(attempt.getAttemptType(), ATTEMPT_TYPE_PRACTICE);
        if (inProgressExam) {
            throw BizException.of(ErrorCode.FORBIDDEN, "模拟考试期间不能使用小C，提交后可在结果页复盘。");
        }
        return attempt;
    }

    private QbAttemptQuestion validateAttemptQuestion(Long attemptId, Long attemptQuestionId) {
        if (attemptQuestionId == null) {
            return null;
        }
        QbAttemptQuestion attemptQuestion = attemptQuestionMapper.selectById(attemptQuestionId);
        if (attemptQuestion == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答题目不存在");
        }
        if (attemptId != null && !Objects.equals(attemptQuestion.getAttemptId(), attemptId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "题目不属于当前作答");
        }
        return attemptQuestion;
    }

    private String buildPrompt(StudentAssistantChatRequest request, QbAttempt attempt, QbAttemptQuestion attemptQuestion) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                你是 C语言课程题库系统里的学生学习助手，名字叫“小C”。
                请用温和、简洁、鼓励式的中文回答学生。
                规则：
                1. 可以讲解概念、提示思路、规划练习，但不要代替学生完成考试。
                2. 如果有题目上下文，优先结合上下文给出引导，不要直接泄露可提交的完整答案。
                3. 如果学生问与学习无关的问题，简短回应后引导回到 C 语言学习。
                4. 回答控制在 3 到 6 个要点，必要时给一个小例子。

                """);

        appendAttemptContext(builder, attempt, attemptQuestion);
        appendPageContext(builder, request.getPageContext());
        appendHistory(builder, request.getHistory());
        builder.append("\n学生本次提问：").append(request.getMessage()).append('\n');

        String prompt = builder.toString();
        if (prompt.length() > MAX_CONTEXT_CHARS) {
            prompt = prompt.substring(0, MAX_CONTEXT_CHARS);
        }
        return prompt;
    }

    private void appendAttemptContext(StringBuilder builder, QbAttempt attempt, QbAttemptQuestion attemptQuestion) {
        if (attempt != null) {
            builder.append("作答上下文：\n");
            builder.append("- attemptId: ").append(attempt.getId()).append('\n');
            builder.append("- attemptType: ").append(attempt.getAttemptType()).append('\n');
            builder.append("- status: ").append(attempt.getStatus()).append('\n');
        }
        if (attemptQuestion != null) {
            builder.append("- attemptQuestionId: ").append(attemptQuestion.getId()).append('\n');
            builder.append("- orderNo: ").append(attemptQuestion.getOrderNo()).append('\n');
            builder.append("- score: ").append(attemptQuestion.getScore()).append('\n');
            if (StringUtils.hasText(attemptQuestion.getSnapshotJson())) {
                builder.append("- questionSnapshot: ").append(trim(attemptQuestion.getSnapshotJson(), 2400)).append('\n');
            }
        }
    }

    private void appendPageContext(StringBuilder builder, Map<String, Object> pageContext) {
        if (pageContext == null || pageContext.isEmpty()) {
            return;
        }
        builder.append("\n页面上下文：\n");
        for (Map.Entry<String, Object> entry : pageContext.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            builder.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(trim(String.valueOf(value), 1200))
                    .append('\n');
        }
    }

    private void appendHistory(StringBuilder builder, List<StudentAssistantChatRequest.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        builder.append("\n最近对话：\n");
        int start = Math.max(0, history.size() - MAX_HISTORY_ITEMS);
        for (int i = start; i < history.size(); i++) {
            StudentAssistantChatRequest.ChatMessage message = history.get(i);
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "小C" : "学生";
            builder.append(role).append(": ").append(trim(message.getContent(), 800)).append('\n');
        }
    }

    private Long readLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
