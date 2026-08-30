package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.*;
import com.xyz.question_bank_management_system.config.LlmProperties;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.SaveAnswerDraftRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.*;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.*;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.learning.dto.ResourceInteractionSubmitRequest;
import com.xyz.question_bank_management_system.modules.learning.service.InteractionSubmissionService;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import com.xyz.question_bank_management_system.modules.bank.service.AttemptService;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import com.xyz.question_bank_management_system.util.HashUtil;
import com.xyz.question_bank_management_system.util.LlmPromptBuilder;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.util.TextRepairUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.xyz.question_bank_management_system.modules.bank.service.impl.AttemptPracticeScoring.PRACTICE_DEFAULT_SCORE;
import static com.xyz.question_bank_management_system.modules.bank.service.impl.AttemptPracticeScoring.buildPracticeScores;
import static com.xyz.question_bank_management_system.modules.bank.service.impl.AttemptPracticeScoring.estimatePracticeQuestionLimit;
import static com.xyz.question_bank_management_system.modules.bank.service.impl.AttemptPracticeScoring.isObjective;
import static com.xyz.question_bank_management_system.modules.bank.service.impl.AttemptPracticeScoring.pickPracticeQuestionsByScore;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptServiceImpl implements AttemptService {

    private static final double ADAPTIVE_TARGET_SUCCESS_RATE = 0.75;
    private static final double ADAPTIVE_DIFFICULTY_STEP = 0.70;
    private static final int ADAPTIVE_MIN_DIFFICULTY = 1;
    private static final int ADAPTIVE_MAX_DIFFICULTY = 5;
    private static final int ADAPTIVE_MIN_ABILITY = 0;
    private static final int ADAPTIVE_MAX_ABILITY = 100;
    private static final double ADAPTIVE_MIN_SIGMOID = 0.01;
    private static final double ADAPTIVE_MAX_SIGMOID = 0.99;
    private static final double ADAPTIVE_MATCH_WEIGHT = 0.62;
    private static final double ADAPTIVE_WEAK_KNOWLEDGE_WEIGHT = 0.20;
    private static final double ADAPTIVE_WRONG_WEIGHT = 0.14;
    private static final double ADAPTIVE_NOVELTY_WEIGHT = 0.06;
    private static final double ADAPTIVE_RANDOM_JITTER = 0.02;
    private final QbAssignmentMapper assignmentMapper;
    private final QbAssignmentTargetMapper targetMapper;

    private final QbAttemptMapper attemptMapper;
    private final QbAttemptQuestionMapper attemptQuestionMapper;
    private final QbAnswerMapper answerMapper;
    private final QbGradingRecordMapper gradingRecordMapper;

    private final QbPaperQuestionMapper paperQuestionMapper;
    private final QbQuestionMapper questionMapper;
    private final QbQuestionOptionMapper optionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final QbLlmCallMapper llmCallMapper;

    private final QbQuestionUserStatMapper questionUserStatMapper;
    private final QbWrongQuestionMapper wrongQuestionMapper;
    private final StudentKnowledgeStateMapper studentKnowledgeStateMapper;
    private final InteractionSubmissionService interactionSubmissionService;
    private final StudentProfileService studentProfileService;
    private final UserAbilityService userAbilityService;

    private final LlmService llmService;
    private final LlmProperties llmProperties;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("attemptGradingExecutor")
    private TaskExecutor attemptGradingExecutor;

    @Override
    @Transactional
    public AttemptStartVO startAssignmentAttempt(Long assignmentId, Long userId) {
        QbAssignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u4e1a\u4e0d\u5b58\u5728");
        }
        if (a.getPublishStatus() == null || a.getPublishStatus() != AssignmentPublishStatusEnum.PUBLISHED.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u4f5c\u4e1a\u672a\u53d1\u5e03");
        }
        LocalDateTime now = LocalDateTime.now();
        if (a.getStartTime() != null && now.isBefore(a.getStartTime())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u4f5c\u4e1a\u672a\u5f00\u59cb");
        }
        if (a.getEndTime() != null && now.isAfter(a.getEndTime())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u4f5c\u4e1a\u5df2\u7ed3\u675f");
        }

        if (targetMapper.countEligibleStudent(assignmentId, userId) <= 0) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u4f60\u4e0d\u5728\u8be5\u4f5c\u4e1a\u7684\u76ee\u6807\u540d\u5355\u4e2d");
        }

        long usedAttempts = attemptMapper.countByAssignmentAndUser(assignmentId, userId);
        int maxAttempts = a.getMaxAttempts() == null ? 1 : a.getMaxAttempts();
        if (maxAttempts > 0 && usedAttempts >= maxAttempts) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u5df2\u8fbe\u5230\u6700\u5927\u4f5c\u7b54\u6b21\u6570");
        }

        int attemptNo = (int) usedAttempts + 1;

        QbAttempt attempt = new QbAttempt();
        attempt.setAssignmentId(assignmentId);
        attempt.setPaperId(a.getPaperId());
        attempt.setUserId(userId);
        attempt.setAttemptType(AttemptTypeEnum.ASSIGNMENT.getCode());
        attempt.setAttemptNo(attemptNo);
        attempt.setStatus(AttemptStatusEnum.IN_PROGRESS.getCode());
        attemptMapper.insert(attempt);

        // Create attempt_question snapshots.
        List<QbPaperQuestion> pqs = paperQuestionMapper.selectByPaperId(a.getPaperId());
        if (pqs == null || pqs.isEmpty()) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u8bd5\u5377\u672a\u914d\u7f6e\u9898\u76ee");
        }

        List<QbPaperQuestion> ordered = new ArrayList<>(pqs);

        List<QbAttemptQuestion> aqList = new ArrayList<>();
        int order = 1;
        for (QbPaperQuestion pq : ordered) {
            String snapshotJson = repairSnapshotMojibake(pq.getSnapshotJson());
            if (a.getShuffleOptions() != null && a.getShuffleOptions() == 1) {
                snapshotJson = shuffleOptionsInSnapshot(snapshotJson);
            }
            String snapshotHash = HashUtil.sha256(snapshotJson);

            Map<String, Object> snap = readSnapshotMap(snapshotJson);
            Integer qt = asInt(snap.get("questionType"));
            Integer diff = asInt(snap.get("difficulty"));
            String knowledgeSnapshotJson = "[]";
            try {
                knowledgeSnapshotJson = serializeKnowledgeSnapshot(pq.getQuestionId());
            } catch (Exception ignore) {}

            QbAttemptQuestion aq = new QbAttemptQuestion();
            aq.setAttemptId(attempt.getId());
            aq.setQuestionId(pq.getQuestionId());
            aq.setOrderNo(order);
            aq.setScore(pq.getScore());
            aq.setSnapshotJson(snapshotJson);
            aq.setSnapshotHash(snapshotHash);
            aq.setQuestionType(qt);
            aq.setDifficulty(diff);
            aq.setKnowledgeSnapshotJson(knowledgeSnapshotJson);
            aqList.add(aq);
            order++;
        }
        attemptQuestionMapper.batchInsert(aqList);
        initAnswersForAttempt(attempt.getId(), userId);
        recordAudit(userId,
                "ATTEMPT_START_ASSIGNMENT",
                "ATTEMPT",
                attempt.getId(),
                null,
                attemptAuditSnapshot(attempt));

        return new AttemptStartVO(attempt.getId(), attemptNo, assignmentId, a.getPaperId(), attempt.getStatus());
    }

    @Override
    @Transactional
    public AttemptStartVO startPracticeAttempt(PracticeStartRequest request, Long userId) {
        String mode = normalizePracticeMode(request.getMode());
        int totalScore = normalizePracticeTotalScore(request.getTotalScore());

        List<Long> knowledgePointIds = normalizeLongList(request.getScope() == null ? null : request.getScope().getKnowledgePointIds());
        List<String> chapters = normalizeStringList(request.getScope() == null ? null : request.getScope().getChapters());
        List<Integer> questionTypes = normalizeQuestionTypes(request.getScope() == null ? null : request.getScope().getQuestionTypes());
        List<Long> questionIds = normalizeLongList(request.getScope() == null ? null : request.getScope().getQuestionIds());
        List<Long> visibleTeacherIds = resolveVisibleTeacherIds(userId);
        int candidateQuestionLimit = estimatePracticeQuestionLimit(totalScore);

        List<QbQuestion> selected;
        if (questionIds != null && !questionIds.isEmpty()) {
            selected = selectPracticeQuestionsByIds(questionIds, visibleTeacherIds);
        } else if ("adaptive".equals(mode)) {
            List<QbQuestion> rankedCandidates = selectAdaptivePracticeQuestions(
                    userId,
                    candidateQuestionLimit,
                    knowledgePointIds,
                    chapters,
                    questionTypes,
                    visibleTeacherIds
            );
            selected = pickPracticeQuestionsByScore(rankedCandidates, totalScore);
        } else {
            long candidateLimit = Math.max(candidateQuestionLimit * 5L, 50L);
            List<QbQuestion> candidates = questionMapper.searchForPractice(knowledgePointIds, chapters, questionTypes, visibleTeacherIds, candidateLimit);
            if (candidates == null || candidates.isEmpty()) {
                throw BizException.of(ErrorCode.BIZ_ERROR, "\u5f53\u524d\u7b5b\u9009\u8303\u56f4\u5185\u6ca1\u6709\u53ef\u7528\u7684\u5df2\u53d1\u5e03\u9898\u76ee");
            }
            Collections.shuffle(candidates);
            selected = pickPracticeQuestionsByScore(candidates, totalScore);
        }
        if (selected == null || selected.isEmpty()) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u5f53\u524d\u7b5b\u9009\u8303\u56f4\u4e0b\u65e0\u6cd5\u751f\u6210\u7ec3\u4e60");
        }
        int[] scores = buildPracticeScores(selected);

        long usedAttempts = attemptMapper.countByUser(userId, AttemptTypeEnum.PRACTICE.getCode());
        int attemptNo = (int) usedAttempts + 1;
        QbAttempt attempt = new QbAttempt();
        attempt.setAssignmentId(null);
        attempt.setPaperId(null);
        attempt.setUserId(userId);
        attempt.setAttemptType(AttemptTypeEnum.PRACTICE.getCode());
        attempt.setAttemptNo(attemptNo);
        attempt.setStatus(AttemptStatusEnum.IN_PROGRESS.getCode());
        attemptMapper.insert(attempt);

        List<QbAttemptQuestion> aqList = new ArrayList<>();
        int orderNo = 1;
        for (QbQuestion q : selected) {
            String snapshotJson = repairSnapshotMojibake(buildQuestionSnapshot(q.getId()));
            String snapshotHash = HashUtil.sha256(snapshotJson);
            String knowledgeSnapshotJson;
            try {
                knowledgeSnapshotJson = serializeKnowledgeSnapshot(q.getId());
            } catch (Exception e) {
                knowledgeSnapshotJson = "[]";
            }

            QbAttemptQuestion aq = new QbAttemptQuestion();
            aq.setAttemptId(attempt.getId());
            aq.setQuestionId(q.getId());
            aq.setOrderNo(orderNo);
            aq.setScore(scores[orderNo - 1]);
            aq.setSnapshotJson(snapshotJson);
            aq.setSnapshotHash(snapshotHash);
            aq.setQuestionType(q.getQuestionType());
            aq.setDifficulty(q.getDifficulty());
            aq.setKnowledgeSnapshotJson(knowledgeSnapshotJson);
            aqList.add(aq);
            orderNo++;
        }
        attemptQuestionMapper.batchInsert(aqList);
        initAnswersForAttempt(attempt.getId(), userId);
        recordAudit(userId,
                "ATTEMPT_START_PRACTICE",
                "ATTEMPT",
                attempt.getId(),
                null,
                attemptAuditSnapshot(attempt));

        return new AttemptStartVO(attempt.getId(), attemptNo, null, null, attempt.getStatus());
    }

    @Override
    public List<AttemptQuestionVO> getAttemptQuestions(Long attemptId, Long userId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee\u8be5\u4f5c\u7b54");
        }
        autoSubmitExpiredAttempt(attempt, userId);

        List<QbAttemptQuestion> aqs = attemptQuestionMapper.selectByAttemptId(attemptId);
        List<QbAnswer> answers = answerMapper.selectByAttemptId(attemptId);
        Map<Long, QbAnswer> byAttemptQuestionId = new HashMap<>();
        for (QbAnswer a : answers) {
            byAttemptQuestionId.put(a.getAttemptQuestionId(), a);
        }
        QbAssignment assignment = resolveAttemptAssignment(attempt);
        LocalDateTime deadlineAt = resolveAttemptDeadline(attempt, assignment);
        Integer remainingSec = resolveRemainingSeconds(deadlineAt);
        Integer timeLimitMin = assignment == null ? null : assignment.getTimeLimitMin();

        List<AttemptQuestionVO> res = new ArrayList<>();
        for (QbAttemptQuestion aq : aqs) {
            AttemptQuestionVO vo = new AttemptQuestionVO();
            vo.setAttemptQuestionId(aq.getId());
            vo.setQuestionId(aq.getQuestionId());
            vo.setOrderNo(aq.getOrderNo());
            vo.setScore(aq.getScore());
            vo.setSnapshotJson(sanitizeSnapshotForStudent(repairSnapshotMojibake(aq.getSnapshotJson())));

            QbAnswer ans = byAttemptQuestionId.get(aq.getId());
            if (ans != null) {
                vo.setAnswerId(ans.getId());
                vo.setAnswerContent(ans.getAnswerContent());
                vo.setAnswerStatus(ans.getAnswerStatus());
            }
            vo.setAttemptStatus(attempt.getStatus());
            vo.setAttemptType(attempt.getAttemptType());
            vo.setTimeLimitMin(timeLimitMin);
            vo.setDeadlineAt(deadlineAt);
            vo.setRemainingSec(remainingSec);
            res.add(vo);
        }
        return res;
    }

    @Override
    public void saveDraft(Long answerId, Long userId, SaveAnswerDraftRequest request) {
        QbAnswer ans = answerMapper.selectById(answerId);
        if (ans == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u7b54\u6848\u4e0d\u5b58\u5728");
        if (!Objects.equals(ans.getUserId(), userId)) throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u4fee\u6539\u8be5\u7b54\u6848");
        QbAttempt attempt = attemptMapper.selectById(ans.getAttemptId());
        if (attempt == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        if (!Objects.equals(attempt.getUserId(), userId)) throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee\u8be5\u4f5c\u7b54");
        if (attempt.getStatus() == null || attempt.getStatus() != AttemptStatusEnum.IN_PROGRESS.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u5f53\u524d\u72b6\u6001\u4e0d\u5141\u8bb8\u4fdd\u5b58\u8349\u7a3f");
        }
        autoSubmitExpiredAttempt(attempt, userId);
        answerMapper.updateDraft(answerId, normalizeAnswerContent(request.getAnswerContent()));
    }

    @Override
    public void submitAnswer(Long answerId, Long userId, SaveAnswerDraftRequest request) {
        QbAnswer ans = answerMapper.selectById(answerId);
        if (ans == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "\u7b54\u6848\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(ans.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u63d0\u4ea4\u8be5\u7b54\u6848");
        }
        QbAttempt attempt = attemptMapper.selectById(ans.getAttemptId());
        if (attempt == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee\u8be5\u4f5c\u7b54");
        }
        if (attempt.getStatus() == null || attempt.getStatus() != AttemptStatusEnum.IN_PROGRESS.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u5f53\u524d\u4f5c\u7b54\u4e0d\u5904\u4e8e\u8fdb\u884c\u4e2d\u72b6\u6001");
        }
        autoSubmitExpiredAttempt(attempt, userId);
        answerMapper.submitOne(answerId, normalizeAnswerContent(request.getAnswerContent()), LocalDateTime.now());
    }

    @Override
    public void submitAttempt(Long attemptId, Long userId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        if (!Objects.equals(attempt.getUserId(), userId)) throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u63d0\u4ea4\u8be5\u4f5c\u7b54");
        if (attempt.getStatus() == null || attempt.getStatus() != AttemptStatusEnum.IN_PROGRESS.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u5f53\u524d\u72b6\u6001\u4e0d\u5141\u8bb8\u63d0\u4ea4");
        }

        if (Thread.currentThread() != null) {
            SubmissionContext context = transactionTemplate.execute(status -> prepareAttemptSubmission(attemptId, userId));
            if (context == null) {
                throw BizException.of(ErrorCode.BIZ_ERROR, "\u63d0\u4ea4\u4f5c\u7b54\u5931\u8d25");
            }

            try {
                // Return quickly after sealing the answers; slow grading continues in the background.
                attemptGradingExecutor.execute(() -> gradeAttemptSafely(context));
            } catch (RuntimeException ex) {
                log.error("Failed to dispatch async grading for attempt {}", attemptId, ex);
                gradeAttemptSafely(context);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        answerMapper.submitAllByAttempt(attemptId, now);

        List<QbAttemptQuestion> aqs = attemptQuestionMapper.selectByAttemptId(attemptId);
        List<QbAnswer> answers = answerMapper.selectByAttemptId(attemptId);
        Map<Long, QbAnswer> byAttemptQuestionId = new HashMap<>();
        for (QbAnswer a : answers) {
            byAttemptQuestionId.put(a.getAttemptQuestionId(), a);
        }

        int objectiveScore = 0;
        int subjectiveScore = 0;
        int needsReview = 0;

        for (QbAttemptQuestion aq : aqs) {
            QbAnswer ans = byAttemptQuestionId.get(aq.getId());
            if (ans == null) continue;

            Integer questionType = aq.getQuestionType();
            if (questionType == null) {
                questionType = extractQuestionType(aq.getSnapshotJson());
            }

            // objective
            if (isObjective(questionType)) {
                String correct = extractStandardAnswer(aq.getSnapshotJson());
                boolean ok = isObjectiveCorrect(questionType, correct, ans.getAnswerContent());
                int s = ok ? (aq.getScore() == null ? 0 : aq.getScore()) : 0;

                answerMapper.updateScoring(ans.getId(), s, s, ok ? 1 : 0, now);
                objectiveScore += s;

                QbGradingRecord gr = new QbGradingRecord();
                gr.setAnswerId(ans.getId());
                gr.setGradingMode(GradingModeEnum.AUTO.getCode());
                gr.setScore(s);
                gr.setDetailJson(buildAutoGradingDetailJson(correct, ans.getAnswerContent()));
                gr.setLlmCallId(null);
                gr.setConfidence(1.0);
                gr.setNeedsReview(0);
                gr.setReviewerId(null);
                gr.setReviewComment(null);
                gr.setIsFinal(1);
                gradingRecordMapper.insert(gr);

                // stats
                updateStats(userId, ans, aq, ok ? 1 : 0, now);
                if (!ok) {
                    wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), now);
                }
            } else {
                // subjective
                int maxScore = aq.getScore() == null ? 0 : aq.getScore();
                LlmGradeResult llm = tryLlmGrade(ans, aq, maxScore);

                if (llm != null && llm.success && llm.score != null) {
                    int score = Math.max(0, Math.min(maxScore, llm.score));
                    subjectiveScore += score;
                    needsReview = needsReview | (llm.needsReview ? 1 : 0);

                    answerMapper.updateScoring(ans.getId(), 0, score, score == maxScore ? 1 : 0, now);

                    QbGradingRecord gr = new QbGradingRecord();
                    gr.setAnswerId(ans.getId());
                    gr.setGradingMode(GradingModeEnum.LLM.getCode());
                    gr.setScore(score);
                    gr.setDetailJson(buildLlmDetailJson(score, llm.confidence, llm.needsReview, llm.comment));
                    gr.setLlmCallId(llm.llmCallId);
                    gr.setConfidence(llm.confidence);
                    gr.setNeedsReview(llm.needsReview ? 1 : 0);
                    gr.setReviewerId(null);
                    gr.setReviewComment(llm.comment);
                    gr.setIsFinal(llm.needsReview ? 0 : 1);
                    gradingRecordMapper.insert(gr);

                    updateStats(userId, ans, aq, score == maxScore ? 1 : 0, now);
                    if (score < maxScore) {
                        wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), now);
                    }
                } else {
                    // Fall back to manual review when the LLM result is unavailable.
                    needsReview = 1;
                    answerMapper.updateScoring(ans.getId(), 0, 0, 0, now);
                    updateStats(userId, ans, aq, 0, now);
                    wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), now);

                    QbGradingRecord gr = new QbGradingRecord();
                    gr.setAnswerId(ans.getId());
                    gr.setGradingMode(GradingModeEnum.MANUAL.getCode());
                    gr.setScore(0);
                    gr.setDetailJson("{\"\u8bf4\u660e\":\"\u9700\u8981\u4eba\u5de5\u590d\u6838\"}");
                    gr.setNeedsReview(1);
                    gr.setIsFinal(0);
                    gradingRecordMapper.insert(gr);
                }
            }
        }

        int totalScore = objectiveScore + subjectiveScore;
        // duration
        QbAttempt fresh = attemptMapper.selectById(attemptId);
        int durationSec = 0;
        if (fresh != null && fresh.getStartedAt() != null) {
            durationSec = (int) Duration.between(fresh.getStartedAt(), now).getSeconds();
        }

        QbAttempt upd = new QbAttempt();
        upd.setId(attemptId);
        upd.setStatus(needsReview == 1 ? AttemptStatusEnum.GRADING.getCode() : AttemptStatusEnum.GRADED.getCode());
        upd.setSubmittedAt(now);
        upd.setDurationSec(durationSec);
        upd.setTotalScore(totalScore);
        upd.setObjectiveScore(objectiveScore);
        upd.setSubjectiveScore(subjectiveScore);
        upd.setNeedsReview(needsReview);
        attemptMapper.updateAfterSubmit(upd);

        userAbilityService.recomputeAndPersist(userId);
    }

    @Override
    public AttemptResultVO result(Long attemptId, Long userId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        if (!Objects.equals(attempt.getUserId(), userId)) throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee");

        AttemptResultVO vo = new AttemptResultVO();
        vo.setAttemptId(attempt.getId());
        vo.setStatus(attempt.getStatus());
        vo.setTotalScore(attempt.getTotalScore());
        vo.setObjectiveScore(attempt.getObjectiveScore());
        vo.setSubjectiveScore(attempt.getSubjectiveScore());
        vo.setNeedsReview(attempt.getNeedsReview());
        vo.setStartedAt(attempt.getStartedAt());
        vo.setSubmittedAt(attempt.getSubmittedAt());
        vo.setDurationSec(attempt.getDurationSec());

        List<QbAnswer> answers = answerMapper.selectByAttemptId(attemptId);
        List<QbAttemptQuestion> attemptQuestions = attemptQuestionMapper.selectByAttemptId(attemptId);
        Map<Long, QbAttemptQuestion> questionByAttemptQuestionId = new HashMap<>();
        for (QbAttemptQuestion attemptQuestion : attemptQuestions) {
            questionByAttemptQuestionId.put(attemptQuestion.getId(), attemptQuestion);
        }
        List<AttemptResultVO.AnswerResultVO> list = new ArrayList<>();
        for (QbAnswer a : answers) {
            QbAttemptQuestion attemptQuestion = questionByAttemptQuestionId.get(a.getAttemptQuestionId());
            AttemptResultVO.AnswerResultVO ar = new AttemptResultVO.AnswerResultVO();
            ar.setAnswerId(a.getId());
            ar.setAttemptQuestionId(a.getAttemptQuestionId());
            ar.setOrderNo(attemptQuestion == null ? null : attemptQuestion.getOrderNo());
            ar.setQuestionId(a.getQuestionId());
            ar.setMaxScore(attemptQuestion == null ? null : attemptQuestion.getScore());
            ar.setFinalScore(a.getFinalScore());
            ar.setAutoScore(a.getAutoScore());
            ar.setIsCorrect(a.getIsCorrect());
            ar.setAnswerContent(a.getAnswerContent());
            ar.setSnapshotJson(attemptQuestion == null ? null : repairSnapshotMojibake(attemptQuestion.getSnapshotJson()));
            list.add(ar);
        }
        list.sort(Comparator.comparing(AttemptResultVO.AnswerResultVO::getOrderNo, Comparator.nullsLast(Integer::compareTo)));
        vo.setAnswers(list);
        return vo;
    }

    private SubmissionContext prepareAttemptSubmission(Long attemptId, Long userId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) throw BizException.of(ErrorCode.NOT_FOUND, "\u4f5c\u7b54\u4e0d\u5b58\u5728");
        if (!Objects.equals(attempt.getUserId(), userId)) throw BizException.of(ErrorCode.FORBIDDEN, "\u65e0\u6743\u63d0\u4ea4\u8be5\u4f5c\u7b54");
        if (attempt.getStatus() == null || attempt.getStatus() != AttemptStatusEnum.IN_PROGRESS.getCode()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "\u5f53\u524d\u4f5c\u7b54\u4e0d\u5904\u4e8e\u8fdb\u884c\u4e2d\u72b6\u6001");
        }

        LocalDateTime submittedAt = resolveSubmissionTime(attempt, LocalDateTime.now());
        answerMapper.submitAllByAttempt(attemptId, submittedAt);

        int durationSec = calculateDurationSec(attempt, submittedAt);

        QbAttempt upd = new QbAttempt();
        upd.setId(attemptId);
        upd.setStatus(AttemptStatusEnum.GRADING.getCode());
        upd.setSubmittedAt(submittedAt);
        upd.setDurationSec(durationSec);
        upd.setTotalScore(0);
        upd.setObjectiveScore(0);
        upd.setSubjectiveScore(0);
        upd.setNeedsReview(0);
        attemptMapper.updateAfterSubmit(upd);
        QbAttempt submitted = attemptMapper.selectById(attemptId);
        if (submitted != null) {
            recordAudit(userId, "ATTEMPT_SUBMIT", "ATTEMPT", attemptId, null, attemptAuditSnapshot(submitted));
        }
        return new SubmissionContext(attemptId, userId);
    }

    private void autoSubmitExpiredAttempt(QbAttempt attempt, Long userId) {
        if (attempt == null
                || attempt.getStatus() == null
                || attempt.getStatus() != AttemptStatusEnum.IN_PROGRESS.getCode()) {
            return;
        }
        LocalDateTime deadlineAt = resolveAttemptDeadline(attempt, null);
        if (deadlineAt == null || LocalDateTime.now().isBefore(deadlineAt)) {
            return;
        }
        submitAttempt(attempt.getId(), userId);
        throw BizException.of(ErrorCode.CONFLICT, "\u4f5c\u7b54\u5df2\u8d85\u65f6\uff0c\u7cfb\u7edf\u5df2\u81ea\u52a8\u63d0\u4ea4\uff0c\u8bf7\u5230\u4f5c\u7b54\u8bb0\u5f55\u67e5\u770b\u7ed3\u679c");
    }

    private QbAssignment resolveAttemptAssignment(QbAttempt attempt) {
        if (attempt == null || attempt.getAssignmentId() == null) {
            return null;
        }
        return assignmentMapper.selectById(attempt.getAssignmentId());
    }

    private LocalDateTime resolveAttemptDeadline(QbAttempt attempt, QbAssignment assignment) {
        if (attempt == null) {
            return null;
        }
        QbAssignment currentAssignment = assignment != null ? assignment : resolveAttemptAssignment(attempt);
        if (currentAssignment == null) {
            return null;
        }
        LocalDateTime deadlineAt = currentAssignment.getEndTime();
        Integer timeLimitMin = currentAssignment.getTimeLimitMin();
        if (timeLimitMin != null && timeLimitMin > 0 && attempt.getStartedAt() != null) {
            LocalDateTime limitDeadline = attempt.getStartedAt().plusMinutes(timeLimitMin);
            deadlineAt = earlierTime(deadlineAt, limitDeadline);
        }
        return deadlineAt;
    }

    private Integer resolveRemainingSeconds(LocalDateTime deadlineAt) {
        if (deadlineAt == null) {
            return null;
        }
        long seconds = Duration.between(LocalDateTime.now(), deadlineAt).getSeconds();
        return (int) Math.max(seconds, 0);
    }

    private LocalDateTime resolveSubmissionTime(QbAttempt attempt, LocalDateTime now) {
        LocalDateTime deadlineAt = resolveAttemptDeadline(attempt, null);
        if (deadlineAt != null && now.isAfter(deadlineAt)) {
            return deadlineAt;
        }
        return now;
    }

    private int calculateDurationSec(QbAttempt attempt, LocalDateTime submittedAt) {
        if (attempt == null || attempt.getStartedAt() == null || submittedAt == null) {
            return 0;
        }
        return (int) Math.max(Duration.between(attempt.getStartedAt(), submittedAt).getSeconds(), 0);
    }

    private LocalDateTime earlierTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private void gradeAttemptSafely(SubmissionContext context) {
        try {
            gradeSubmittedAttempt(context.attemptId(), context.userId());
        } catch (Exception e) {
            log.error("Async grading failed for attempt {}", context.attemptId(), e);
        }
    }

    private void gradeSubmittedAttempt(Long attemptId, Long userId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            log.warn("Attempt {} disappeared before grading started", attemptId);
            return;
        }
        if (!Objects.equals(attempt.getUserId(), userId)) {
            log.warn("Attempt {} belongs to user {}, but async grading received {}", attemptId, attempt.getUserId(), userId);
            return;
        }
        if (attempt.getStatus() == null
                || (attempt.getStatus() != AttemptStatusEnum.GRADING.getCode()
                && attempt.getStatus() != AttemptStatusEnum.SUBMITTED.getCode())) {
            log.warn("Skip grading attempt {} because status is {}", attemptId, attempt.getStatus());
            return;
        }

        List<QbAttemptQuestion> aqs = attemptQuestionMapper.selectByAttemptId(attemptId);
        List<QbAnswer> answers = answerMapper.selectByAttemptId(attemptId);
        Map<Long, QbAnswer> byAttemptQuestionId = new HashMap<>();
        for (QbAnswer a : answers) {
            byAttemptQuestionId.put(a.getAttemptQuestionId(), a);
        }

        int objectiveScore = 0;
        int subjectiveScore = 0;
        int needsReview = 0;

        for (QbAttemptQuestion aq : aqs) {
            QbAnswer ans = byAttemptQuestionId.get(aq.getId());
            if (ans == null) {
                continue;
            }

            Integer questionType = aq.getQuestionType();
            if (questionType == null) {
                questionType = extractQuestionType(aq.getSnapshotJson());
            }

            if (isObjective(questionType)) {
                LocalDateTime gradedAt = LocalDateTime.now();
                String correct = extractStandardAnswer(aq.getSnapshotJson());
                boolean ok = isObjectiveCorrect(questionType, correct, ans.getAnswerContent());
                int score = ok ? (aq.getScore() == null ? 0 : aq.getScore()) : 0;

                answerMapper.updateScoring(ans.getId(), score, score, ok ? 1 : 0, gradedAt);
                objectiveScore += score;

                QbGradingRecord gr = new QbGradingRecord();
                gr.setAnswerId(ans.getId());
                gr.setGradingMode(GradingModeEnum.AUTO.getCode());
                gr.setScore(score);
                gr.setDetailJson(buildAutoGradingDetailJson(correct, ans.getAnswerContent()));
                gr.setLlmCallId(null);
                gr.setConfidence(1.0);
                gr.setNeedsReview(0);
                gr.setReviewerId(null);
                gr.setReviewComment(null);
                gr.setIsFinal(1);
                gradingRecordMapper.insert(gr);

                updateStats(userId, ans, aq, ok ? 1 : 0, gradedAt);
                if (!ok) {
                    wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), gradedAt);
                }
                continue;
            }

            int maxScore = aq.getScore() == null ? 0 : aq.getScore();
            LocalDateTime gradedAt = LocalDateTime.now();
            if (isAnswerBlank(ans.getAnswerContent())) {
                answerMapper.updateScoring(ans.getId(), 0, 0, 0, gradedAt);
                updateStats(userId, ans, aq, 0, gradedAt);
                wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), gradedAt);

                QbGradingRecord gr = new QbGradingRecord();
                gr.setAnswerId(ans.getId());
                gr.setGradingMode(GradingModeEnum.AUTO.getCode());
                gr.setScore(0);
                gr.setDetailJson("{\"\u539f\u56e0\":\"\u7b54\u6848\u4e3a\u7a7a\",\"\u8bf4\u660e\":\"\u5b66\u751f\u7b54\u6848\u4e3a\u7a7a\uff0c\u6309 0 \u5206\u5904\u7406\uff0c\u672a\u8c03\u7528\u5927\u6a21\u578b\"}");
                gr.setConfidence(1.0);
                gr.setNeedsReview(0);
                gr.setIsFinal(1);
                gradingRecordMapper.insert(gr);
                continue;
            }

            LlmGradeResult llm = tryLlmGrade(ans, aq, maxScore);

            if (llm != null && llm.success && llm.score != null) {
                int score = Math.max(0, Math.min(maxScore, llm.score));
                subjectiveScore += score;
                needsReview = needsReview | (llm.needsReview ? 1 : 0);

                answerMapper.updateScoring(ans.getId(), 0, score, score == maxScore ? 1 : 0, gradedAt);

                QbGradingRecord gr = new QbGradingRecord();
                gr.setAnswerId(ans.getId());
                gr.setGradingMode(GradingModeEnum.LLM.getCode());
                gr.setScore(score);
                gr.setDetailJson(buildLlmDetailJson(score, llm.confidence, llm.needsReview, llm.comment));
                gr.setLlmCallId(llm.llmCallId);
                gr.setConfidence(llm.confidence);
                gr.setNeedsReview(llm.needsReview ? 1 : 0);
                gr.setReviewerId(null);
                gr.setReviewComment(llm.comment);
                gr.setIsFinal(llm.needsReview ? 0 : 1);
                gradingRecordMapper.insert(gr);

                updateStats(userId, ans, aq, score == maxScore ? 1 : 0, gradedAt);
                if (score < maxScore) {
                    wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), gradedAt);
                }
            } else {
                needsReview = 1;
                answerMapper.updateScoring(ans.getId(), 0, 0, 0, gradedAt);
                updateStats(userId, ans, aq, 0, gradedAt);
                wrongQuestionMapper.upsertWrong(userId, aq.getQuestionId(), gradedAt);

                QbGradingRecord gr = new QbGradingRecord();
                gr.setAnswerId(ans.getId());
                gr.setGradingMode(GradingModeEnum.MANUAL.getCode());
                gr.setScore(0);
                gr.setDetailJson("{\"\u8bf4\u660e\":\"\u9700\u8981\u4eba\u5de5\u590d\u6838\"}");
                gr.setNeedsReview(1);
                gr.setIsFinal(0);
                gradingRecordMapper.insert(gr);
            }
        }

        int totalScore = objectiveScore + subjectiveScore;
        QbAttempt upd = new QbAttempt();
        upd.setId(attemptId);
        upd.setStatus(needsReview == 1 ? AttemptStatusEnum.GRADING.getCode() : AttemptStatusEnum.GRADED.getCode());
        upd.setSubmittedAt(attempt.getSubmittedAt());
        upd.setDurationSec(attempt.getDurationSec());
        upd.setTotalScore(totalScore);
        upd.setObjectiveScore(objectiveScore);
        upd.setSubjectiveScore(subjectiveScore);
        upd.setNeedsReview(needsReview);
        attemptMapper.updateAfterSubmit(upd);

        userAbilityService.recomputeAndPersist(userId);
    }

    @Override
    public PageResponse<QbAttempt> myAttempts(Integer attemptType, long page, long size, Long userId) {
        Integer safeAttemptType = null;
        if (attemptType != null) {
            if (attemptType != AttemptTypeEnum.ASSIGNMENT.getCode()
                    && attemptType != AttemptTypeEnum.PRACTICE.getCode()) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "\u4f5c\u7b54\u7c7b\u578b\u53ea\u80fd\u662f 1 \u6216 2");
            }
            safeAttemptType = attemptType;
        }
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<QbAttempt> rows = attemptMapper.pageByUser(userId, safeAttemptType, offset, safeSize);
        long total = attemptMapper.countByUser(userId, safeAttemptType);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    // ================= helpers =================

    private void initAnswersForAttempt(Long attemptId, Long userId) {
        List<QbAttemptQuestion> inserted = attemptQuestionMapper.selectByAttemptId(attemptId);
        for (QbAttemptQuestion aq : inserted) {
            QbAnswer ans = new QbAnswer();
            ans.setAttemptId(attemptId);
            ans.setAttemptQuestionId(aq.getId());
            ans.setQuestionId(aq.getQuestionId());
            ans.setUserId(userId);
            ans.setAnswerContent(null);
            Integer answerFormat = extractAnswerFormat(aq.getSnapshotJson());
            ans.setAnswerFormat(answerFormat == null ? 1 : answerFormat);
            ans.setAnswerStatus(AnswerStatusEnum.DRAFT.getCode());
            ans.setAutoScore(0);
            ans.setFinalScore(0);
            ans.setIsCorrect(0);
            answerMapper.insert(ans);
        }
    }

    private String buildQuestionSnapshot(Long questionId) {
        try {
            QbQuestion q = questionMapper.selectById(questionId);
            if (q == null) {
                throw BizException.of(ErrorCode.NOT_FOUND, "\u9898\u76ee\u4e0d\u5b58\u5728: " + questionId);
            }

            List<QbQuestionOption> options = optionMapper.selectByQuestionId(questionId);
            List<Map<String, Object>> knowledgeRelations = knowledgeSnapshotRelations(questionId);

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", q.getId());
            snapshot.put("title", q.getTitle());
            snapshot.put("questionType", q.getQuestionType());
            snapshot.put("difficulty", q.getDifficulty());
            snapshot.put("chapter", q.getChapter());
            snapshot.put("stem", q.getStem());
            snapshot.put("standardAnswer", q.getStandardAnswer());
            snapshot.put("answerFormat", q.getAnswerFormat());
            snapshot.put("analysisText", q.getAnalysisText());
            snapshot.put("analysisSource", q.getAnalysisSource());
            snapshot.put("llmAnalyses", buildQuestionLlmAnalyses(questionId));
            snapshot.put("knowledgeRelations", knowledgeRelations);
            snapshot.put("options", options);
            return objectMapper.writeValueAsString(snapshot);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u751f\u6210\u9898\u76ee\u5feb\u7167\u5931\u8d25: " + e.getMessage());
        }
    }

    private String normalizePracticeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "\u7ec3\u4e60\u6a21\u5f0f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String normalized = mode.trim().toLowerCase();
        if ("random".equals(normalized) || "adaptive".equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, "\u7ec3\u4e60\u6a21\u5f0f\u53c2\u6570\u4e0d\u5408\u6cd5");
    }

    private int normalizePracticeTotalScore(Integer totalScore) {
        if (totalScore == null) {
            return 100;
        }
        if (totalScore <= 0) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "\u603b\u5206\u5fc5\u987b\u5927\u4e8e 0");
        }
        return Math.min(totalScore, 1000);
    }

    private List<Long> normalizeLongList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Long> resolveVisibleTeacherIds(Long studentId) {
        List<Long> teacherIds = classMemberMapper.listTeacherIdsByStudentId(studentId);
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }
        return teacherIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<QbQuestion> selectPracticeQuestionsByIds(List<Long> questionIds, List<Long> visibleTeacherIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "\u9898\u76ee\u7f16\u53f7\u5217\u8868\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (questionIds.size() > 100) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "\u9898\u76ee\u7f16\u53f7\u6570\u91cf\u4e0d\u80fd\u8d85\u8fc7 100");
        }
        List<QbQuestion> rows = questionMapper.selectPublishedByIds(questionIds, visibleTeacherIds);
        Map<Long, QbQuestion> byId = new HashMap<>();
        for (QbQuestion row : rows) {
            byId.put(row.getId(), row);
        }
        List<QbQuestion> ordered = new ArrayList<>();
        for (Long qid : questionIds) {
            QbQuestion q = byId.get(qid);
            if (q == null) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "\u9898\u76ee\u4e0d\u5b58\u5728\u6216\u672a\u53d1\u5e03: " + qid);
            }
            ordered.add(q);
        }
        return ordered;
    }

    private List<QbQuestion> selectAdaptivePracticeQuestions(Long userId,
                                                             int questionCount,
                                                             List<Long> knowledgePointIds,
                                                             List<String> chapters,
                                                             List<Integer> questionTypes,
                                                             List<Long> visibleTeacherIds) {
        long candidateLimit = Math.min(600L, Math.max(questionCount * 8L, 80L));
        List<QbQuestion> candidates = questionMapper.searchForPractice(knowledgePointIds, chapters, questionTypes, visibleTeacherIds, candidateLimit);
        if (candidates == null || candidates.isEmpty()) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u5f53\u524d\u7b5b\u9009\u8303\u56f4\u5185\u6ca1\u6709\u53ef\u7528\u7684\u5df2\u53d1\u5e03\u9898\u76ee");
        }

        List<Long> questionIds = candidates.stream()
                .map(QbQuestion::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u81ea\u9002\u5e94\u7ec3\u4e60\u5019\u9009\u9898\u96c6\u4e3a\u7a7a");
        }

        Map<Long, QbQuestionUserStat> statByQuestionId = new HashMap<>();
        List<QbQuestionUserStat> statRows = questionUserStatMapper.selectByUserIdAndQuestionIds(userId, questionIds);
        if (statRows != null) {
            for (QbQuestionUserStat row : statRows) {
                if (row != null && row.getQuestionId() != null) {
                    statByQuestionId.put(row.getQuestionId(), row);
                }
            }
        }

        Set<Long> unresolvedWrongQuestionIds = new HashSet<>();
        List<Long> wrongRows = wrongQuestionMapper.selectUnresolvedQuestionIds(userId, questionIds);
        if (wrongRows != null) {
            for (Long qid : wrongRows) {
                if (qid != null) {
                    unresolvedWrongQuestionIds.add(qid);
                }
            }
        }

        Map<Long, List<Long>> knowledgePointIdsByQuestionId = new HashMap<>();
        List<QuestionKnowledge> knowledgeLinks = questionKnowledgeMapper.selectByQuestionIds(questionIds);
        if (knowledgeLinks != null) {
            for (QuestionKnowledge link : knowledgeLinks) {
                if (link == null || link.getQuestionId() == null || link.getKnowledgePointId() == null) {
                    continue;
                }
                knowledgePointIdsByQuestionId.computeIfAbsent(link.getQuestionId(), k -> new ArrayList<>()).add(link.getKnowledgePointId());
            }
        }

        Map<Long, Double> masteryByKnowledgePointId = new HashMap<>();
        List<StudentKnowledgeState> masteryRows = studentKnowledgeStateMapper.selectByUserId(userId);
        if (masteryRows != null) {
            for (StudentKnowledgeState row : masteryRows) {
                if (row == null || row.getKnowledgePointId() == null || row.getMasteryValue() == null) {
                    continue;
                }
                masteryByKnowledgePointId.put(row.getKnowledgePointId(), clamp01(row.getMasteryValue().doubleValue()));
            }
        }

        double theta = abilityScoreToTheta(studentProfileService.abilityScore(userId));

        List<AdaptiveCandidateScore> ranked = new ArrayList<>();
        for (QbQuestion q : candidates) {
            if (q == null || q.getId() == null) {
                continue;
            }
            Long qid = q.getId();

            double beta = difficultyToBeta(q.getDifficulty());
            double successProb = sigmoid(theta - beta);
            double abilityMatch = 1.0 - Math.min(1.0, Math.abs(successProb - ADAPTIVE_TARGET_SUCCESS_RATE) / ADAPTIVE_TARGET_SUCCESS_RATE);

            List<Long> qKnowledgePointIds = knowledgePointIdsByQuestionId.get(qid);
            double weakKnowledgePressure = 0.0;
            if (qKnowledgePointIds != null && !qKnowledgePointIds.isEmpty()) {
                double sum = 0.0;
                int cnt = 0;
                for (Long knowledgePointId : qKnowledgePointIds) {
                    Double mastery = masteryByKnowledgePointId.get(knowledgePointId);
                    if (mastery == null) {
                        continue;
                    }
                    sum += (1.0 - mastery);
                    cnt++;
                }
                if (cnt > 0) {
                    weakKnowledgePressure = sum / cnt;
                } else {
                    weakKnowledgePressure = 0.35;
                }
            }

            double wrongBoost = unresolvedWrongQuestionIds.contains(qid) ? 1.0 : 0.0;
            QbQuestionUserStat stat = statByQuestionId.get(qid);
            int attemptCount = stat == null ? 0 : safeNonNegative(stat.getAttemptCount());
            double novelty = 1.0 - Math.min(1.0, attemptCount / 6.0);
            double jitter = Math.random() * ADAPTIVE_RANDOM_JITTER;

            double adaptiveScore =
                    ADAPTIVE_MATCH_WEIGHT * abilityMatch
                            + ADAPTIVE_WEAK_KNOWLEDGE_WEIGHT * weakKnowledgePressure
                            + ADAPTIVE_WRONG_WEIGHT * wrongBoost
                            + ADAPTIVE_NOVELTY_WEIGHT * novelty
                            + jitter;
            ranked.add(new AdaptiveCandidateScore(q, adaptiveScore));
        }

        if (ranked.isEmpty()) {
            throw BizException.of(ErrorCode.BIZ_ERROR, "\u81ea\u9002\u5e94\u7ec3\u4e60\u5019\u9009\u9898\u96c6\u4e3a\u7a7a");
        }

        ranked.sort(
                Comparator.comparingDouble(AdaptiveCandidateScore::adaptiveScore).reversed()
                        .thenComparing(x -> x.question().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(x -> x.question().getId(), Comparator.nullsLast(Comparator.reverseOrder()))
        );

        int picked = Math.min(questionCount, ranked.size());
        List<QbQuestion> selected = new ArrayList<>(picked);
        for (int i = 0; i < picked; i++) {
            selected.add(ranked.get(i).question());
        }
        return selected;
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Integer> normalizeQuestionTypes(List<Integer> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return null;
        }
        List<Integer> normalized = questionTypes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (Integer t : normalized) {
            QuestionTypeEnum type = QuestionTypeEnum.of(t);
            if (type == null || !type.isEnabledNow()) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "\u9898\u578b\u4e0d\u53ef\u7528\u6216\u53c2\u6570\u65e0\u6548: " + t);
            }
        }
        return normalized;
    }

    private Integer extractAnswerFormat(String snapshotJson) {
        try {
            Map<String, Object> m = readSnapshotMap(snapshotJson);
            return asInt(m.get("answerFormat"));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractQuestionType(String snapshotJson) {
        try {
            Map<String, Object> m = readSnapshotMap(snapshotJson);
            return asInt(m.get("questionType"));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractStandardAnswer(String snapshotJson) {
        try {
            Map<String, Object> m = readSnapshotMap(snapshotJson);
            Object v = m.get("standardAnswer");
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isObjectiveCorrect(Integer questionType, String standardAnswer, String userAnswer) {
        if (standardAnswer == null) standardAnswer = "";
        if (userAnswer == null) userAnswer = "";
        String sa = standardAnswer.trim();
        String ua = userAnswer.trim();

        if (questionType == QuestionTypeEnum.SINGLE.getCode()) {
            return sa.equalsIgnoreCase(ua);
        }
        if (questionType == QuestionTypeEnum.MULTIPLE.getCode()) {
            Set<String> s1 = splitMulti(sa);
            Set<String> s2 = splitMulti(ua);
            return s1.equals(s2);
        }
        if (questionType == QuestionTypeEnum.TRUE_FALSE.getCode()) {
            return normalizeTF(sa).equals(normalizeTF(ua));
        }
        if (questionType == QuestionTypeEnum.BLANK.getCode()) {
            // Compare blank answers after removing whitespace.
            return sa.replaceAll("\\s+", "").equalsIgnoreCase(ua.replaceAll("\\s+", ""));
        }
        return false;
    }

    private Set<String> splitMulti(String s) {
        s = s.trim();
        if (s.isEmpty()) return new HashSet<>();
        if (s.contains(",")) {
            String[] arr = s.split(",");
            Set<String> set = new HashSet<>();
            for (String a : arr) {
                if (!a.isBlank()) set.add(a.trim().toUpperCase());
            }
            return set;
        }
        // e.g. "AC" -> A,C
        Set<String> set = new HashSet<>();
        for (char c : s.toCharArray()) {
            if (!Character.isWhitespace(c)) set.add(String.valueOf(c).toUpperCase());
        }
        return set;
    }

    private String normalizeTF(String s) {
        s = s.trim().toLowerCase();
        if (s.equals("t") || s.equals("true") || s.equals("1") || s.equals("\u5bf9") || s.equals("\u6b63\u786e")) return "true";
        if (s.equals("f") || s.equals("false") || s.equals("0") || s.equals("\u9519") || s.equals("\u9519\u8bef")) return "false";
        return s;
    }

    private void updateStats(Long userId, QbAnswer answer, QbAttemptQuestion aq, int correctInc, LocalDateTime at) {
        questionUserStatMapper.upsert(userId, aq.getQuestionId(), correctInc, at);
        // Ordinary bank/assignment/exam attempts are deliberately excluded from
        // the personalized-resource evidence loop. Generated questions are
        // applied once by InteractionSubmissionService.
        if (StringUtils.hasText(aq.getGeneratedQuestionCode())) {
            ResourceInteractionSubmitRequest request = new ResourceInteractionSubmitRequest();
            request.setGeneratedQuestionCode(aq.getGeneratedQuestionCode());
            request.setAnswer(answer.getAnswerContent());
            request.setActionOrigin("SYSTEM_REQUIRED");
            interactionSubmissionService.submit(userId, "attempt-answer-" + answer.getId(), request);
        }
    }

    private List<Long> parseKnowledgePointIds(String knowledgeSnapshotJson) {
        if (knowledgeSnapshotJson == null || knowledgeSnapshotJson.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, Object>> relations = objectMapper.readValue(knowledgeSnapshotJson, new TypeReference<List<Map<String, Object>>>() {});
            return relations.stream().map(item -> asLong(item.get("knowledgePointId"))).filter(Objects::nonNull).distinct().toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> readSnapshotMap(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Integer asInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return null;
        }
    }

    private Long asLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeKnowledgeSnapshot(Long questionId) throws Exception {
        return objectMapper.writeValueAsString(knowledgeSnapshotRelations(questionId));
    }

    private List<Map<String, Object>> knowledgeSnapshotRelations(Long questionId) {
        return questionKnowledgeMapper.selectByQuestionId(questionId).stream().map(relation -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("knowledgePointId", relation.getKnowledgePointId());
            item.put("weight", relation.getWeight());
            item.put("relationType", relation.getRelationType());
            item.put("isPrimary", relation.getIsPrimary());
            item.put("confidence", relation.getConfidence());
            item.put("sourceType", relation.getSourceType());
            return item;
        }).toList();
    }

    private double difficultyToBeta(Integer difficulty) {
        int safeDifficulty = difficulty == null ? 3 : difficulty;
        int d = Math.max(ADAPTIVE_MIN_DIFFICULTY, Math.min(ADAPTIVE_MAX_DIFFICULTY, safeDifficulty));
        return (d - 3) * ADAPTIVE_DIFFICULTY_STEP;
    }

    private double abilityScoreToTheta(Integer abilityScore) {
        int safe = abilityScore == null ? 0 : Math.max(ADAPTIVE_MIN_ABILITY, Math.min(ADAPTIVE_MAX_ABILITY, abilityScore));
        double normalized = safe / 100.0;
        double clipped = Math.max(ADAPTIVE_MIN_SIGMOID, Math.min(ADAPTIVE_MAX_SIGMOID, normalized));
        return Math.log(clipped / (1.0 - clipped));
    }

    private int safeNonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double sigmoid(double x) {
        if (x >= 0) {
            double z = Math.exp(-x);
            return 1.0 / (1.0 + z);
        }
        double z = Math.exp(x);
        return z / (1.0 + z);
    }

    private String repairSnapshotMojibake(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return snapshotJson;
        }
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            repairMojibakeInNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return snapshotJson;
        }
    }

    private void repairMojibakeInNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            var obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            var fields = obj.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode child = entry.getValue();
                if (child != null && child.isTextual()) {
                    obj.put(entry.getKey(), TextRepairUtil.repairGbkUtf8Mojibake(child.asText()));
                } else {
                    repairMojibakeInNode(child);
                }
            }
            return;
        }
        if (node.isArray()) {
            var arr = (com.fasterxml.jackson.databind.node.ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child != null && child.isTextual()) {
                    arr.set(i, objectMapper.getNodeFactory().textNode(TextRepairUtil.repairGbkUtf8Mojibake(child.asText())));
                } else {
                    repairMojibakeInNode(child);
                }
            }
        }
    }

    private String sanitizeSnapshotForStudent(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            if (root.isObject()) {
                var obj = (com.fasterxml.jackson.databind.node.ObjectNode) root;
                obj.remove(List.of("standardAnswer", "analysisText", "analysisSource", "analysisLlmCallId", "llmAnalyses"));

                // remove isCorrect from options
                JsonNode options = root.get("options");
                if (options != null && options.isArray()) {
                    for (JsonNode o : options) {
                        if (o.isObject()) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) o).remove("isCorrect");
                        }
                    }
                }
                ensureTrueFalseOptionsForStudent(obj);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return snapshotJson;
        }
    }

    private void ensureTrueFalseOptionsForStudent(com.fasterxml.jackson.databind.node.ObjectNode root) {
        JsonNode questionType = root.get("questionType");
        if (questionType == null || questionType.asInt() != QuestionTypeEnum.TRUE_FALSE.getCode()) {
            return;
        }
        JsonNode options = root.get("options");
        if (options != null && options.isArray() && options.size() > 0) {
            return;
        }
        var defaultOptions = objectMapper.createArrayNode();
        defaultOptions.addObject()
                .put("optionLabel", "对")
                .put("optionContent", "对")
                .put("sortOrder", 1);
        defaultOptions.addObject()
                .put("optionLabel", "错")
                .put("optionContent", "错")
                .put("sortOrder", 2);
        root.set("options", defaultOptions);
    }

    private String shuffleOptionsInSnapshot(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            JsonNode options = root.get("options");
            if (options != null && options.isArray() && options.size() > 1) {
                List<JsonNode> list = new ArrayList<>();
                options.forEach(list::add);
                Collections.shuffle(list);
                com.fasterxml.jackson.databind.node.ArrayNode arr = objectMapper.createArrayNode();
                for (JsonNode n : list) {
                    arr.add(n);
                }
                if (root.isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("options", arr);
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return snapshotJson;
        }
    }

    private String safeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildAutoGradingDetailJson(String correct, String answer) {
        return "{\"\\u6807\\u51c6\\u7b54\\u6848\":\"" + safeJson(correct)
                + "\",\"\\u5b66\\u751f\\u7b54\\u6848\":\"" + safeJson(answer) + "\"}";
    }

    private String buildLlmDetailJson(Integer score, Double confidence, boolean needsReview, String comment) {
        String safeScore = score == null ? "null" : String.valueOf(score);
        String safeConfidence = confidence == null ? "null" : String.valueOf(confidence);
        return "{\"\\u5206\\u6570\":" + safeScore
                + ",\"\\u7f6e\\u4fe1\\u5ea6\":" + safeConfidence
                + ",\"\\u9700\\u8981\\u590d\\u6838\":" + needsReview
                + ",\"\\u8bc4\\u8bed\":\"" + safeJson(comment) + "\"}";
    }

    private String normalizeAnswerContent(String answerContent) {
        return answerContent == null ? "" : answerContent;
    }

    private boolean isAnswerBlank(String answerContent) {
        return answerContent == null || answerContent.trim().isEmpty();
    }

    private LlmGradeResult tryLlmGrade(QbAnswer ans, QbAttemptQuestion aq, int maxScore) {
        try {
            String questionTitle = null;
            String stem = null;
            String standardAnswer = null;
            String analysisText = null;
            try {
                JsonNode root = objectMapper.readTree(aq.getSnapshotJson());
                questionTitle = root.path("title").asText(null);
                stem = root.path("stem").asText(null);
                standardAnswer = root.path("standardAnswer").asText(null);
                analysisText = root.path("analysisText").asText(null);
            } catch (Exception ignore) {
            }

            String prompt = LlmPromptBuilder.buildSubjectiveGradingPrompt(
                    questionTitle,
                    stem,
                    standardAnswer,
                    analysisText,
                    ans.getAnswerContent(),
                    maxScore,
                    null,
                    null
            );

            QbLlmCall call = llmService.chatCompletion(2, ans.getId(), prompt);
            if (call.getCallStatus() == null || call.getCallStatus() != 1) {
                return null;
            }
            String content = llmService.extractContent(call.getResponseText());
            if (content == null) {
                return null;
            }

            JsonNode json;
            try {
                json = objectMapper.readTree(content);
            } catch (Exception e) {
                content = content.trim();
                content = content.replaceAll("^```json", "").replaceAll("```$", "").trim();
                try {
                    json = objectMapper.readTree(content);
                } catch (Exception ignore) {
                    return null;
                }
            }

            LlmGradeResult r = new LlmGradeResult();
            r.success = true;
            r.llmCallId = call.getId();
            r.score = readIntField(json, "\u5206\u6570", "score");
            r.confidence = readDoubleField(json, "\u7f6e\u4fe1\u5ea6", "confidence");
            r.needsReview = readBooleanField(json, "\u9700\u8981\u590d\u6838", "needsReview") != Boolean.FALSE;
            r.comment = readTextField(json, "\u8bc4\u8bed", "comment");
            r.rawDetailJson = content;
            if (r.confidence != null && r.confidence < 0.55) {
                r.needsReview = true;
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readIntField(JsonNode json, String... fieldNames) {
        JsonNode node = findField(json, fieldNames);
        return node == null ? null : node.asInt();
    }

    private Double readDoubleField(JsonNode json, String... fieldNames) {
        JsonNode node = findField(json, fieldNames);
        return node == null ? null : node.asDouble();
    }

    private Boolean readBooleanField(JsonNode json, String... fieldNames) {
        JsonNode node = findField(json, fieldNames);
        return node == null ? null : node.asBoolean();
    }

    private String readTextField(JsonNode json, String... fieldNames) {
        JsonNode node = findField(json, fieldNames);
        return node == null ? null : node.asText();
    }

    private JsonNode findField(JsonNode json, String... fieldNames) {
        if (json == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null || fieldName.isBlank() || !json.has(fieldName)) {
                continue;
            }
            JsonNode node = json.get(fieldName);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private List<QuestionLlmAnalysisVO> buildQuestionLlmAnalyses(Long questionId) {
        List<QbLlmCall> calls = llmCallMapper.selectByBiz(1, questionId);
        List<QuestionLlmAnalysisVO> analyses = new ArrayList<>();
        for (LlmProperties.ModelProvider provider : llmProperties.questionAnalysisProviders()) {
            analyses.add(buildQuestionLlmAnalysis(provider, calls));
        }
        return analyses;
    }

    private QuestionLlmAnalysisVO buildQuestionLlmAnalysis(LlmProperties.ModelProvider provider, List<QbLlmCall> calls) {
        QuestionLlmAnalysisVO vo = new QuestionLlmAnalysisVO();
        vo.setAnalysisKey(provider.resolveKey());
        vo.setAnalysisLabel(provider.resolveLabel());
        vo.setModelName(provider.getModel());
        vo.setHasAnalysis(false);

        QbLlmCall latestCall = findLatestQuestionAnalysisCall(provider, calls, null);
        QbLlmCall latestSuccessfulCall = findLatestQuestionAnalysisCall(provider, calls, 1);
        if (latestCall == null) {
            vo.setCallStatus(-1);
            vo.setAnalysisText("尚未生成解析");
            return vo;
        }

        if (latestSuccessfulCall != null) {
            vo.setHasAnalysis(true);
            fillQuestionAnalysisContent(vo, latestSuccessfulCall);
            if (Objects.equals(latestCall.getId(), latestSuccessfulCall.getId())
                    || Objects.equals(latestCall.getCallStatus(), 1)) {
                vo.setCallStatus(1);
                return vo;
            }
            if (Objects.equals(latestCall.getCallStatus(), 0)) {
                vo.setCallStatus(0);
                vo.setErrorMessage("正在重新生成，当前展示上一次成功解析");
                return vo;
            }
            vo.setCallStatus(2);
            vo.setErrorMessage(trimToNull(latestCall.getResponseText()));
            if (vo.getErrorMessage() == null) {
                vo.setErrorMessage("最近一次生成失败，当前展示上一次成功解析");
            }
            return vo;
        }

        vo.setLlmCallId(latestCall.getId());
        vo.setCallStatus(latestCall.getCallStatus());
        vo.setLatencyMs(latestCall.getLatencyMs());
        vo.setCreatedAt(latestCall.getCreatedAt());

        if (latestCall.getCallStatus() != null && latestCall.getCallStatus() == 0) {
            vo.setAnalysisText("解析生成中，请稍后刷新查看结果");
            return vo;
        }

        vo.setAnalysisText("解析生成失败");
        vo.setErrorMessage(trimToNull(latestCall.getResponseText()));
        return vo;
    }

    private void fillQuestionAnalysisContent(QuestionLlmAnalysisVO vo, QbLlmCall call) {
        vo.setLlmCallId(call.getId());
        vo.setLatencyMs(call.getLatencyMs());
        vo.setCreatedAt(call.getCreatedAt());
        String content = llmService.extractContent(call.getResponseText());
        if (content == null || content.isBlank()) {
            vo.setAnalysisText("解析已生成，但暂未提取到正文内容");
            return;
        }
        vo.setAnalysisText(content.trim());
    }

    private QbLlmCall findLatestQuestionAnalysisCall(LlmProperties.ModelProvider provider,
                                                     List<QbLlmCall> calls,
                                                     Integer expectedStatus) {
        if (calls == null || calls.isEmpty()) {
            return null;
        }
        QbLlmCall latestCall = null;
        for (QbLlmCall call : calls) {
            if (call == null || !matchesQuestionAnalysisProvider(provider, call.getModelName())) {
                continue;
            }
            if (expectedStatus != null && !Objects.equals(expectedStatus, call.getCallStatus())) {
                continue;
            }
            if (latestCall == null || isNewerQuestionAnalysisCall(call, latestCall)) {
                latestCall = call;
            }
        }
        return latestCall;
    }

    private boolean isNewerQuestionAnalysisCall(QbLlmCall candidate, QbLlmCall current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (candidate.getId() != null && current.getId() != null) {
            return candidate.getId() > current.getId();
        }
        if (candidate.getCreatedAt() != null && current.getCreatedAt() != null) {
            return candidate.getCreatedAt().isAfter(current.getCreatedAt());
        }
        return candidate.getCreatedAt() != null || current.getCreatedAt() == null;
    }

    private boolean matchesQuestionAnalysisProvider(LlmProperties.ModelProvider provider, String modelName) {
        if (provider == null || modelName == null || modelName.isBlank()) {
            return false;
        }
        String normalizedModelName = modelName.trim().toLowerCase(Locale.ROOT);
        if (provider.getModel() != null && normalizedModelName.equals(provider.getModel().trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (provider.getKey() != null && normalizedModelName.equals(provider.getKey().trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return provider.getLabel() != null && normalizedModelName.equals(provider.getLabel().trim().toLowerCase(Locale.ROOT));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> attemptAuditSnapshot(QbAttempt attempt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", attempt.getId());
        snapshot.put("assignmentId", attempt.getAssignmentId());
        snapshot.put("paperId", attempt.getPaperId());
        snapshot.put("userId", attempt.getUserId());
        snapshot.put("attemptType", attempt.getAttemptType());
        snapshot.put("attemptNo", attempt.getAttemptNo());
        snapshot.put("status", attempt.getStatus());
        snapshot.put("startedAt", attempt.getStartedAt());
        snapshot.put("submittedAt", attempt.getSubmittedAt());
        snapshot.put("totalScore", attempt.getTotalScore());
        snapshot.put("objectiveScore", attempt.getObjectiveScore());
        snapshot.put("subjectiveScore", attempt.getSubjectiveScore());
        snapshot.put("needsReview", attempt.getNeedsReview());
        return snapshot;
    }

    private void recordAudit(Long userId,
                             String action,
                             String entityType,
                             Long entityId,
                             Object beforeData,
                             Object afterData) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.record(userId, action, entityType, entityId, beforeData, afterData);
    }

    private static class LlmGradeResult {
        boolean success;
        Long llmCallId;
        Integer score;
        Double confidence;
        boolean needsReview = true;
        String comment;
        String rawDetailJson;
    }

    private record SubmissionContext(Long attemptId, Long userId) {
    }

    private record AdaptiveCandidateScore(QbQuestion question, double adaptiveScore) {
    }
}

