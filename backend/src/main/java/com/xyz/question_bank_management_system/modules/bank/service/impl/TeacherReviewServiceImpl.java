package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.AttemptStatusEnum;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAnswer;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import com.xyz.question_bank_management_system.modules.bank.entity.QbGradingRecord;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAppealMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetClassMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptQuestionMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbGradingRecordMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.bank.service.TeacherReviewService;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import com.xyz.question_bank_management_system.util.LlmPromptBuilder;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAnswerEvidenceVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentScoreItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentStudentDetailVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAssignmentTargetItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherLlmBatchGradeVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherReviewAnswerItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherReviewServiceImpl implements TeacherReviewService {

    private final QbAnswerMapper answerMapper;
    private final QbAppealMapper appealMapper;
    private final QbAssignmentMapper assignmentMapper;
    private final QbAssignmentTargetMapper targetMapper;
    private final QbAssignmentTargetClassMapper targetClassMapper;
    private final QbAttemptMapper attemptMapper;
    private final QbAttemptQuestionMapper attemptQuestionMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final QbGradingRecordMapper gradingRecordMapper;
    private final QbLlmCallMapper llmCallMapper;
    private final SysUserMapper sysUserMapper;
    private final LlmService llmService;
    private final UserAbilityService userAbilityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResponse<TeacherReviewAnswerItemVO> reviewAnswers(Long assignmentId,
                                                                 Long studentId,
                                                                 Integer questionType,
                                                                 Boolean needsReview,
                                                                 long page,
                                                                 long size,
                                                                 Long actorId,
                                                                 boolean isAdmin) {
        if (assignmentId != null) {
            loadManageableAssignment(assignmentId, actorId, isAdmin);
        }
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);
        Integer needsReviewInt = needsReview == null ? 1 : (needsReview ? 1 : 0);
        Long ownerId = isAdmin ? null : actorId;

        List<TeacherReviewAnswerItemVO> rows = answerMapper.pageTeacherReview(assignmentId, studentId, questionType, needsReviewInt, ownerId, offset, safeSize);
        long total = answerMapper.countTeacherReview(assignmentId, studentId, questionType, needsReviewInt, ownerId);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public TeacherAnswerEvidenceVO evidence(Long answerId, Long actorId, boolean isAdmin) {
        QbAnswer answer = loadAnswer(answerId);
        QbAttempt attempt = loadAttempt(answer.getAttemptId());
        ensureCanReviewAttempt(attempt, actorId, isAdmin);
        QbAttemptQuestion aq = loadAttemptQuestion(answer.getAttemptQuestionId());

        TeacherAnswerEvidenceVO vo = new TeacherAnswerEvidenceVO();
        vo.setAnswerId(answer.getId());
        vo.setStudentAnswer(answer.getAnswerContent());

        vo.setStudent(buildEvidenceStudent(answer.getUserId()));

        vo.setQuestionSnapshot(parseSnapshot(aq.getSnapshotJson()));

        vo.setGradingRecords(buildEvidenceRecords(answerId));
        return vo;
    }

    @Override
    @Transactional
    public void manualGrade(Long answerId, Integer score, String comment, Long reviewerId, boolean isAdmin) {
        QbAnswer answer = loadAnswer(answerId);
        QbAttempt attempt = loadAttempt(answer.getAttemptId());
        ensureCanReviewAttempt(attempt, reviewerId, isAdmin);
        QbAttemptQuestion aq = loadAttemptQuestion(answer.getAttemptQuestionId());

        int maxScore = aq.getScore() == null ? 0 : aq.getScore();
        if (score == null || score < 0 || score > maxScore) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "分数超出允许范围");
        }

        int previousScore = currentFinalScore(answer);
        int safeScore = applyScoreAndDelta(answer, aq, score);
        QbGradingRecord record = buildManualGradingRecord(answerId, safeScore, reviewerId, comment);

        record.setDetailJson("{\"来源\":\"人工评分\"}");
        gradingRecordMapper.insert(record);

        refreshAbilityIfScoreChanged(answer.getUserId(), previousScore, safeScore);
        refreshAttemptReviewState(answer.getAttemptId());
    }

    @Override
    @Transactional
    public List<Long> llmRetry(Long answerId,
                               String providerKey,
                               Double temperature,
                               Integer times,
                               Long actorId,
                               boolean isAdmin) {
        QbAnswer answer = loadAnswer(answerId);
        QbAttempt attempt = loadAttempt(answer.getAttemptId());
        ensureCanReviewAttempt(attempt, actorId, isAdmin);
        QbAttemptQuestion aq = loadAttemptQuestion(answer.getAttemptQuestionId());

        int safeTimes = times == null ? 1 : Math.max(1, Math.min(5, times));
        String prompt = buildLlmRetryPrompt(answer, aq, providerKey, temperature);

        List<Long> llmCallIds = new ArrayList<>();
        boolean abilityNeedsRefresh = false;
        boolean gradeApplied = false;
        for (int i = 0; i < safeTimes; i++) {
            QbLlmCall call = llmService.chatCompletion(2, answerId, prompt, providerKey, actorId);
            if (call != null && call.getId() != null) {
                llmCallIds.add(call.getId());
            }
            if (call == null || call.getCallStatus() == null || call.getCallStatus() != 1) {
                continue;
            }
            ParsedLlmGrade parsed = parseLlmGradeResult(call);
            if (parsed == null || parsed.score == null) {
                continue;
            }
            int previousScore = currentFinalScore(answer);
            int finalScore = applyScoreAndDelta(answer, aq, parsed.score);
            gradingRecordMapper.insert(buildLlmGradingRecord(answerId, call.getId(), finalScore, parsed));
            abilityNeedsRefresh = abilityNeedsRefresh || finalScore != previousScore;
            gradeApplied = true;
        }
        if (!gradeApplied) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "LLM 未返回可解析的评分结果，请调整模型后重试");
        }
        if (abilityNeedsRefresh) {
            userAbilityService.recomputeAndPersist(answer.getUserId());
        }
        refreshAttemptReviewState(answer.getAttemptId());
        return llmCallIds;
    }

    @Override
    @Transactional
    public TeacherLlmBatchGradeVO llmBatch(List<Long> answerIds,
                                           Long assignmentId,
                                           Boolean needsReview,
                                           String providerKey,
                                           Double temperature,
                                           Integer times,
                                           Long actorId,
                                           boolean isAdmin) {
        List<Long> targets = resolveBatchAnswerIds(answerIds, assignmentId, needsReview, actorId, isAdmin);
        TeacherLlmBatchGradeVO result = new TeacherLlmBatchGradeVO();
        for (Long answerId : targets) {
            TeacherLlmBatchGradeVO.ItemVO item = new TeacherLlmBatchGradeVO.ItemVO();
            item.setAnswerId(answerId);
            try {
                List<Long> callIds = llmRetry(answerId, providerKey, temperature, times, actorId, isAdmin);
                item.setSuccess(true);
                item.setMessage("ok");
                item.setLlmCallIds(callIds);
                QbAnswer updated = answerMapper.selectById(answerId);
                item.setFinalScore(updated == null ? null : updated.getFinalScore());
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                item.setSuccess(false);
                item.setMessage(ex.getMessage());
                result.setFailureCount(result.getFailureCount() + 1);
            }
            result.getItems().add(item);
        }
        return result;
    }

    @Override
    public PageResponse<TeacherAssignmentScoreItemVO> assignmentScores(Long assignmentId,
                                                                       long page,
                                                                       long size,
                                                                       Long actorId,
                                                                       boolean isAdmin) {
        loadManageableAssignment(assignmentId, actorId, isAdmin);

        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<TeacherAssignmentScoreItemVO> rows = attemptMapper.pageByAssignmentForTeacher(assignmentId, offset, safeSize);
        long total = attemptMapper.countByAssignmentForTeacher(assignmentId);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public PageResponse<TeacherAssignmentTargetItemVO> assignmentTargets(Long assignmentId,
                                                                         long page,
                                                                         long size,
                                                                         Long actorId,
                                                                         boolean isAdmin) {
        QbAssignment assignment = loadManageableAssignment(assignmentId, actorId, isAdmin);
        List<Long> targetStudentIds = resolveAssignmentTargetStudentIds(assignment);

        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        int fromIndex = (int) Math.min(PageParamUtil.offset(safePage, safeSize), targetStudentIds.size());
        int toIndex = (int) Math.min(fromIndex + safeSize, targetStudentIds.size());

        List<TeacherAssignmentTargetItemVO> rows = new ArrayList<>();
        for (Long studentId : targetStudentIds.subList(fromIndex, toIndex)) {
            rows.add(buildAssignmentTargetItem(assignment.getId(), studentId));
        }
        return PageResponse.of(safePage, safeSize, targetStudentIds.size(), rows);
    }

    @Override
    public TeacherAssignmentStudentDetailVO assignmentStudentDetail(Long assignmentId,
                                                                   Long studentId,
                                                                   Long actorId,
                                                                   boolean isAdmin) {
        QbAssignment assignment = loadManageableAssignment(assignmentId, actorId, isAdmin);
        List<Long> targetStudentIds = resolveAssignmentTargetStudentIds(assignment);
        List<QbAttempt> attempts = attemptMapper.selectByAssignmentAndUser(assignmentId, studentId);
        if (!targetStudentIds.contains(studentId) && attempts.isEmpty()) {
            throw BizException.of(ErrorCode.FORBIDDEN, "该学生不在本次作业的目标范围内");
        }

        TeacherAssignmentStudentDetailVO vo = new TeacherAssignmentStudentDetailVO();
        vo.setAssignmentId(assignment.getId());
        vo.setAssignmentTitle(assignment.getAssignmentTitle());
        vo.setStudentId(studentId);
        applyStudentInfo(vo, studentId);
        vo.setAttempts(buildAttemptItems(attempts));
        return vo;
    }

    private TeacherAssignmentTargetItemVO buildAssignmentTargetItem(Long assignmentId, Long studentId) {
        TeacherAssignmentTargetItemVO vo = new TeacherAssignmentTargetItemVO();
        vo.setStudentId(studentId);
        applyStudentInfo(vo, studentId);

        List<QbAttempt> attempts = attemptMapper.selectByAssignmentAndUser(assignmentId, studentId);
        vo.setAttemptCount(attempts.size());
        vo.setCompleted(attempts.stream().anyMatch(this::isCompletedAttempt) ? 1 : 0);
        applyLatestAttemptInfo(vo, attempts);
        return vo;
    }

    private List<TeacherAssignmentStudentDetailVO.QuestionItemVO> buildAttemptQuestionDetails(Long attemptId) {
        List<QbAttemptQuestion> attemptQuestions = attemptQuestionMapper.selectByAttemptId(attemptId);
        Map<Long, QbAnswer> answerByAttemptQuestionId = indexAnswersByAttemptQuestionId(answerMapper.selectByAttemptId(attemptId));

        List<TeacherAssignmentStudentDetailVO.QuestionItemVO> rows = new ArrayList<>();
        attemptQuestions.stream()
                .sorted(Comparator.comparing(QbAttemptQuestion::getOrderNo, Comparator.nullsLast(Integer::compareTo)))
                .forEach(attemptQuestion -> rows.add(toQuestionItemVO(attemptQuestion, answerByAttemptQuestionId.get(attemptQuestion.getId()))));
        return rows;
    }

    private TeacherAnswerEvidenceVO.StudentVO buildEvidenceStudent(Long userId) {
        TeacherAnswerEvidenceVO.StudentVO student = new TeacherAnswerEvidenceVO.StudentVO();
        student.setId(userId);
        SysUser user = findUser(userId);
        student.setDisplayName(user == null ? null : user.getDisplayName());
        return student;
    }

    private List<TeacherAnswerEvidenceVO.GradingRecordVO> buildEvidenceRecords(Long answerId) {
        List<TeacherAnswerEvidenceVO.GradingRecordVO> rows = new ArrayList<>();
        for (QbGradingRecord record : gradingRecordMapper.selectByAnswerId(answerId)) {
            rows.add(toGradingRecordVO(record));
        }
        return rows;
    }

    private TeacherAnswerEvidenceVO.GradingRecordVO toGradingRecordVO(QbGradingRecord record) {
        TeacherAnswerEvidenceVO.GradingRecordVO row = new TeacherAnswerEvidenceVO.GradingRecordVO();
        row.setGradingMode(record.getGradingMode());
        row.setScore(record.getScore());
        row.setConfidence(record.getConfidence());
        row.setNeedsReview(record.getNeedsReview());
        row.setDetailJson(record.getDetailJson());
        row.setReviewComment(record.getReviewComment());
        row.setLlmCall(buildLlmCallVO(record.getLlmCallId()));
        return row;
    }

    private TeacherAnswerEvidenceVO.LlmCallVO buildLlmCallVO(Long llmCallId) {
        if (llmCallId == null) {
            return null;
        }
        QbLlmCall call = llmCallMapper.selectById(llmCallId);
        if (call == null) {
            return null;
        }
        TeacherAnswerEvidenceVO.LlmCallVO llm = new TeacherAnswerEvidenceVO.LlmCallVO();
        llm.setLlmCallId(call.getId());
        llm.setModelName(call.getModelName());
        llm.setPromptText(call.getPromptText());
        llm.setResponseText(call.getResponseText());
        llm.setResponseJson(call.getResponseJson());
        llm.setCallStatus(call.getCallStatus());
        return llm;
    }

    private QbGradingRecord buildManualGradingRecord(Long answerId, int score, Long reviewerId, String comment) {
        QbGradingRecord record = new QbGradingRecord();
        record.setAnswerId(answerId);
        record.setGradingMode(3);
        record.setScore(score);
        record.setNeedsReview(0);
        record.setReviewerId(reviewerId);
        record.setReviewComment(comment);
        record.setIsFinal(1);
        return record;
    }

    private QbGradingRecord buildLlmGradingRecord(Long answerId, Long llmCallId, int score, ParsedLlmGrade parsed) {
        QbGradingRecord record = new QbGradingRecord();
        record.setAnswerId(answerId);
        record.setGradingMode(2);
        record.setScore(score);
        record.setDetailJson(parsed.detailJson);
        record.setLlmCallId(llmCallId);
        record.setConfidence(parsed.confidence);
        record.setNeedsReview(parsed.needsReview ? 1 : 0);
        record.setReviewComment(joinReviewComment(parsed.comment, parsed.suggestion));
        record.setIsFinal(parsed.needsReview ? 0 : 1);
        return record;
    }

    private int currentFinalScore(QbAnswer answer) {
        return answer.getFinalScore() == null ? 0 : answer.getFinalScore();
    }

    private void refreshAbilityIfScoreChanged(Long userId, int previousScore, int currentScore) {
        if (previousScore != currentScore) {
            userAbilityService.recomputeAndPersist(userId);
        }
    }

    private void applyStudentInfo(TeacherAssignmentStudentDetailVO vo, Long studentId) {
        SysUser user = findUser(studentId);
        if (user == null) {
            return;
        }
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
    }

    private void applyStudentInfo(TeacherAssignmentTargetItemVO vo, Long studentId) {
        SysUser user = findUser(studentId);
        if (user == null) {
            return;
        }
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
    }

    private List<TeacherAssignmentStudentDetailVO.AttemptItemVO> buildAttemptItems(List<QbAttempt> attempts) {
        List<TeacherAssignmentStudentDetailVO.AttemptItemVO> rows = new ArrayList<>();
        for (QbAttempt attempt : attempts) {
            rows.add(toAttemptItemVO(attempt));
        }
        return rows;
    }

    private TeacherAssignmentStudentDetailVO.AttemptItemVO toAttemptItemVO(QbAttempt attempt) {
        TeacherAssignmentStudentDetailVO.AttemptItemVO row = new TeacherAssignmentStudentDetailVO.AttemptItemVO();
        row.setAttemptId(attempt.getId());
        row.setAttemptNo(attempt.getAttemptNo());
        row.setStatus(attempt.getStatus());
        row.setTotalScore(attempt.getTotalScore());
        row.setObjectiveScore(attempt.getObjectiveScore());
        row.setSubjectiveScore(attempt.getSubjectiveScore());
        row.setNeedsReview(attempt.getNeedsReview());
        row.setStartedAt(attempt.getStartedAt());
        row.setSubmittedAt(attempt.getSubmittedAt());
        row.setDurationSec(attempt.getDurationSec());
        row.setQuestions(buildAttemptQuestionDetails(attempt.getId()));
        return row;
    }

    private void applyLatestAttemptInfo(TeacherAssignmentTargetItemVO vo, List<QbAttempt> attempts) {
        if (attempts.isEmpty()) {
            return;
        }
        QbAttempt latestAttempt = attempts.get(0);
        vo.setLatestAttemptId(latestAttempt.getId());
        vo.setLatestAttemptStatus(latestAttempt.getStatus());
        vo.setLatestTotalScore(latestAttempt.getTotalScore());
        vo.setLatestNeedsReview(latestAttempt.getNeedsReview());
        vo.setLatestSubmittedAt(latestAttempt.getSubmittedAt());
    }

    private Map<Long, QbAnswer> indexAnswersByAttemptQuestionId(List<QbAnswer> answers) {
        Map<Long, QbAnswer> answerByAttemptQuestionId = new HashMap<>();
        for (QbAnswer answer : answers) {
            answerByAttemptQuestionId.put(answer.getAttemptQuestionId(), answer);
        }
        return answerByAttemptQuestionId;
    }

    private TeacherAssignmentStudentDetailVO.QuestionItemVO toQuestionItemVO(QbAttemptQuestion attemptQuestion, QbAnswer answer) {
        TeacherAssignmentStudentDetailVO.QuestionItemVO row = new TeacherAssignmentStudentDetailVO.QuestionItemVO();
        row.setAnswerId(answer == null ? null : answer.getId());
        row.setAttemptQuestionId(attemptQuestion.getId());
        row.setOrderNo(attemptQuestion.getOrderNo());
        row.setQuestionId(attemptQuestion.getQuestionId());
        row.setTitle(extractQuestionTitle(attemptQuestion.getSnapshotJson(), attemptQuestion.getQuestionId()));
        row.setQuestionType(attemptQuestion.getQuestionType());
        row.setMaxScore(attemptQuestion.getScore());
        row.setFinalScore(answer == null ? null : answer.getFinalScore());
        row.setAutoScore(answer == null ? null : answer.getAutoScore());
        row.setIsCorrect(answer == null ? null : answer.getIsCorrect());
        row.setAnswerContent(answer == null ? null : answer.getAnswerContent());
        row.setSnapshotJson(attemptQuestion.getSnapshotJson());
        return row;
    }

    private SysUser findUser(Long userId) {
        return userId == null ? null : sysUserMapper.selectById(userId);
    }

    private List<Long> resolveAssignmentTargetStudentIds(QbAssignment assignment) {
        LinkedHashSet<Long> studentIds = new LinkedHashSet<>();
        List<Long> directUserIds = targetMapper.listUserIdsByAssignmentId(assignment.getId());
        if (directUserIds != null) {
            studentIds.addAll(directUserIds);
        }
        List<Long> classIds = targetClassMapper.listClassIdsByAssignmentId(assignment.getId());
        if (classIds != null && !classIds.isEmpty()) {
            studentIds.addAll(classMemberMapper.listStudentIdsByClassIds(classIds));
        }
        if (!studentIds.isEmpty()) {
            return new ArrayList<>(studentIds);
        }
        if (assignment.getCreatedBy() != null) {
            studentIds.addAll(classMemberMapper.listStudentIdsByTeacherId(assignment.getCreatedBy()));
        }
        studentIds.addAll(attemptMapper.listUserIdsByAssignment(assignment.getId()));
        return new ArrayList<>(studentIds);
    }

    private List<Long> resolveBatchAnswerIds(List<Long> answerIds,
                                             Long assignmentId,
                                             Boolean needsReview,
                                             Long actorId,
                                             boolean isAdmin) {
        LinkedHashSet<Long> targets = new LinkedHashSet<>();
        if (answerIds != null) {
            answerIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> id > 0)
                    .limit(50)
                    .forEach(targets::add);
        }
        if (targets.isEmpty() && assignmentId != null) {
            loadManageableAssignment(assignmentId, actorId, isAdmin);
            Integer needsReviewInt = needsReview == null ? 1 : (needsReview ? 1 : 0);
            Long ownerId = isAdmin ? null : actorId;
            targets.addAll(answerMapper.listTeacherReviewAnswerIds(assignmentId, needsReviewInt, ownerId, 50));
        }
        if (targets.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "请选择要批改的答题，或指定作业ID");
        }
        if (targets.size() > 50) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "一次最多批改 50 道题");
        }
        return new ArrayList<>(targets);
    }

    private QbAssignment loadManageableAssignment(Long assignmentId, Long actorId, boolean isAdmin) {
        QbAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作业不存在");
        }
        if (!isAdmin && !Objects.equals(assignment.getCreatedBy(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权查看该作业");
        }
        return assignment;
    }

    private QbAnswer loadAnswer(Long answerId) {
        QbAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "答案不存在");
        }
        return answer;
    }

    private QbAttempt loadAttempt(Long attemptId) {
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答不存在");
        }
        return attempt;
    }

    private QbAttemptQuestion loadAttemptQuestion(Long attemptQuestionId) {
        QbAttemptQuestion aq = attemptQuestionMapper.selectById(attemptQuestionId);
        if (aq == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答题目不存在");
        }
        return aq;
    }

    private void ensureCanReviewAttempt(QbAttempt attempt, Long actorId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (attempt == null || attempt.getAssignmentId() == null) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该作答");
        }
        QbAssignment assignment = assignmentMapper.selectById(attempt.getAssignmentId());
        if (assignment == null || !Objects.equals(assignment.getCreatedBy(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权访问该作答");
        }
    }

    private boolean isCompletedAttempt(QbAttempt attempt) {
        if (attempt == null || attempt.getStatus() == null) {
            return false;
        }
        return attempt.getStatus() == AttemptStatusEnum.SUBMITTED.getCode()
                || attempt.getStatus() == AttemptStatusEnum.GRADING.getCode()
                || attempt.getStatus() == AttemptStatusEnum.GRADED.getCode();
    }

    private String extractQuestionTitle(String snapshotJson, Long questionId) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            String title = root.path("title").asText(null);
            if (title != null && !title.isBlank()) {
                return title;
            }
            String stem = root.path("stem").asText(null);
            if (stem != null && !stem.isBlank()) {
                return stem;
            }
        } catch (Exception ignore) {
        }
        return "题目 #" + (questionId == null ? "-" : questionId);
    }

    private String buildLlmRetryPrompt(QbAnswer answer, QbAttemptQuestion aq, String modelName, Double temperature) {
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
        int maxScore = aq.getScore() == null ? 0 : aq.getScore();
        return LlmPromptBuilder.buildSubjectiveGradingPrompt(
                questionTitle,
                stem,
                standardAnswer,
                analysisText,
                answer.getAnswerContent(),
                maxScore,
                modelName,
                temperature
        );
    }

    private int applyScoreAndDelta(QbAnswer answer, QbAttemptQuestion aq, int score) {
        int maxScore = aq.getScore() == null ? 0 : aq.getScore();
        int safeScore = Math.max(0, Math.min(maxScore, score));
        int oldFinalScore = answer.getFinalScore() == null ? 0 : answer.getFinalScore();
        int autoScore = answer.getAutoScore() == null ? 0 : answer.getAutoScore();
        int isCorrect = safeScore >= maxScore && maxScore > 0 ? 1 : 0;

        answerMapper.updateScoring(answer.getId(), autoScore, safeScore, isCorrect, LocalDateTime.now());

        int delta = safeScore - oldFinalScore;
        if (delta != 0) {
            if (isObjectiveQuestionType(aq.getQuestionType())) {
                attemptMapper.updateScoreDelta(answer.getAttemptId(), delta, delta, 0);
            } else {
                attemptMapper.updateScoreDelta(answer.getAttemptId(), delta, 0, delta);
            }
        }
        answer.setFinalScore(safeScore);
        return safeScore;
    }

    private void refreshAttemptReviewState(Long attemptId) {
        if (attemptId == null) {
            return;
        }
        QbAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return;
        }
        long pendingReviewCount = answerMapper.countPendingReviewByAttemptId(attemptId);
        long pendingAppealCount = appealMapper.countPendingByAttemptId(attemptId);
        boolean hasPendingWork = pendingReviewCount > 0 || pendingAppealCount > 0;
        attempt.setNeedsReview(hasPendingWork ? 1 : 0);
        attempt.setStatus(hasPendingWork ? AttemptStatusEnum.GRADING.getCode() : AttemptStatusEnum.GRADED.getCode());
        attemptMapper.updateAfterSubmit(attempt);
    }

    private ParsedLlmGrade parseLlmGradeResult(QbLlmCall call) {
        try {
            String content = llmService.extractContent(call.getResponseText());
            if (content == null || content.isBlank()) {
                return null;
            }
            JsonNode json = parseJsonNode(content);
            Integer score = readIntField(json, "分数", "score");
            if (json == null || !json.isObject() || score == null) {
                return null;
            }
            ParsedLlmGrade result = new ParsedLlmGrade();
            result.score = score;
            result.confidence = readDoubleField(json, "置信度", "confidence");
            result.needsReview = readBooleanField(json, "需要复核", "needsReview") != Boolean.FALSE;
            if (result.confidence != null && result.confidence < 0.55) {
                result.needsReview = true;
            }
            result.comment = readTextField(json, "评语", "comment");
            result.suggestion = readTextField(json, "建议", "suggestion", "advice", "recommendation");
            result.detailJson = buildLlmDetailJson(result.score, result.confidence, result.needsReview, result.comment, result.suggestion);
            return result;
        } catch (Exception ignore) {
            return null;
        }
    }

    private JsonNode parseJsonNode(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception ignore) {
            String normalized = content.trim();
            if (normalized.startsWith("```")) {
                normalized = normalized.replaceFirst("^```(?:json)?\\s*", "");
                normalized = normalized.replaceFirst("\\s*```$", "");
                try {
                    return objectMapper.readTree(normalized.trim());
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private boolean isObjectiveQuestionType(Integer questionType) {
        if (questionType == null) {
            return false;
        }
        return questionType == 1 || questionType == 2 || questionType == 3 || questionType == 4;
    }

    private Object parseSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshotJson, Object.class);
        } catch (Exception ignore) {
            return snapshotJson;
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

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String joinReviewComment(String comment, String suggestion) {
        if (comment == null || comment.isBlank()) {
            return suggestion;
        }
        if (suggestion == null || suggestion.isBlank()) {
            return comment;
        }
        return comment + "\n建议：" + suggestion;
    }

    private String buildLlmDetailJson(Integer score, Double confidence, boolean needsReview, String comment, String suggestion) {
        String safeScore = score == null ? "null" : String.valueOf(score);
        String safeConfidence = confidence == null ? "null" : String.valueOf(confidence);
        return "{\"分数\":" + safeScore
                + ",\"置信度\":" + safeConfidence
                + ",\"需要复核\":" + needsReview
                + ",\"评语\":\"" + safeJson(comment) + "\""
                + ",\"建议\":\"" + safeJson(suggestion) + "\"}";
    }

    private static class ParsedLlmGrade {
        Integer score;
        Double confidence;
        boolean needsReview = true;
        String comment;
        String suggestion;
        String detailJson;
    }
}
