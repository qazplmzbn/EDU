package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.AppealStatusEnum;
import com.xyz.question_bank_management_system.common.enums.AttemptStatusEnum;
import com.xyz.question_bank_management_system.common.enums.GradingModeEnum;
import com.xyz.question_bank_management_system.common.enums.QuestionTypeEnum;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.AppealHandleRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAnswer;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAssignment;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAppeal;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.bank.entity.QbAttemptQuestion;
import com.xyz.question_bank_management_system.modules.bank.entity.QbGradingRecord;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAppealMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbGradingRecordMapper;
import com.xyz.question_bank_management_system.modules.bank.service.AppealService;
import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.AppealMyItemVO;
import com.xyz.question_bank_management_system.modules.bank.vo.TeacherAppealItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private final QbAppealMapper appealMapper;
    private final QbAnswerMapper answerMapper;
    private final QbAssignmentMapper assignmentMapper;
    private final QbAttemptQuestionMapper attemptQuestionMapper;
    private final QbAttemptMapper attemptMapper;
    private final QbGradingRecordMapper gradingRecordMapper;
    private final UserAbilityService userAbilityService;

    @Override
    @Transactional
    public Long submitAppeal(AppealCreateRequest request, Long userId) {
        QbAnswer answer = answerMapper.selectById(request.getAnswerId());
        if (answer == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "答案不存在");
        }
        if (!userId.equals(answer.getUserId())) {
            throw BizException.of(ErrorCode.FORBIDDEN, "不能申诉他人的答案");
        }
        long pendingCount = appealMapper.countPendingByAnswerAndUser(answer.getId(), userId);
        if (pendingCount > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "已有待处理申诉，请勿重复提交");
        }

        QbAppeal appeal = new QbAppeal();
        appeal.setAnswerId(answer.getId());
        appeal.setUserId(userId);
        appeal.setReasonText(request.getReasonText().trim());
        appeal.setAppealStatus(AppealStatusEnum.PENDING.getCode());
        appealMapper.insert(appeal);
        refreshAttemptReviewState(answer.getAttemptId());
        return appeal.getId();
    }

    @Override
    public PageResponse<AppealMyItemVO> pageMyAppeals(Long userId, Integer status, long page, long size) {
        Integer safeStatus = normalizeStatus(status, false);
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<AppealMyItemVO> rows = appealMapper.pageByUser(userId, safeStatus, offset, safeSize);
        long total = appealMapper.countByUser(userId, safeStatus);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public PageResponse<TeacherAppealItemVO> pageTeacherAppeals(Integer status,
                                                                long page,
                                                                long size,
                                                                Long actorId,
                                                                boolean isAdmin) {
        Integer safeStatus = normalizeStatus(status, true);
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);
        Long ownerId = isAdmin ? null : actorId;

        List<TeacherAppealItemVO> rows = appealMapper.pageForTeacher(safeStatus, ownerId, offset, safeSize);
        long total = appealMapper.countForTeacher(safeStatus, ownerId);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    @Transactional
    public void handleAppeal(Long appealId, AppealHandleRequest request, Long handlerId, boolean isAdmin) {
        QbAppeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "申诉记录不存在");
        }
        if (appeal.getAppealStatus() == null || appeal.getAppealStatus() != AppealStatusEnum.PENDING.getCode()) {
            throw BizException.of(ErrorCode.CONFLICT, "该申诉已处理");
        }

        String action = request.getAction().trim().toLowerCase(Locale.ROOT);
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "处理动作参数不合法");
        }

        QbAnswer answer = answerMapper.selectById(appeal.getAnswerId());
        if (answer == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "答案不存在");
        }
        QbAttempt attempt = attemptMapper.selectById(answer.getAttemptId());
        if (attempt == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答不存在");
        }
        ensureCanHandleAttempt(attempt, handlerId, isAdmin);

        QbAttemptQuestion attemptQuestion = attemptQuestionMapper.selectById(answer.getAttemptQuestionId());
        if (attemptQuestion == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "作答题目不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer currentFinalScore = safeInt(answer.getFinalScore());
        Integer targetFinalScore = currentFinalScore;
        int targetAppealStatus;

        if ("reject".equals(action)) {
            targetAppealStatus = AppealStatusEnum.REJECTED.getCode();
        } else {
            if (request.getFinalScore() != null) {
                int maxScore = safeInt(attemptQuestion.getScore());
                if (request.getFinalScore() > maxScore) {
                    throw BizException.of(ErrorCode.PARAM_ERROR, "最终分数不能超过满分 " + maxScore);
                }
                targetFinalScore = request.getFinalScore();
            }

            if (!targetFinalScore.equals(currentFinalScore)) {
                int maxScore = safeInt(attemptQuestion.getScore());
                int isCorrect = (maxScore > 0 && targetFinalScore >= maxScore) ? 1 : 0;
                answerMapper.updateScoring(answer.getId(), safeInt(answer.getAutoScore()), targetFinalScore, isCorrect, now);

                QbGradingRecord record = new QbGradingRecord();
                record.setAnswerId(answer.getId());
                record.setGradingMode(GradingModeEnum.MANUAL.getCode());
                record.setScore(targetFinalScore);
                record.setDetailJson("{\"来源\":\"申诉处理\"}");
                record.setNeedsReview(0);
                record.setReviewerId(handlerId);
                record.setReviewComment(request.getDecisionComment());
                record.setIsFinal(1);
                gradingRecordMapper.insert(record);

                updateAttemptScoreByDelta(answer.getAttemptId(), attemptQuestion.getQuestionType(), targetFinalScore - currentFinalScore);
                userAbilityService.recomputeAndPersist(answer.getUserId());
                targetAppealStatus = AppealStatusEnum.RESOLVED.getCode();
            } else {
                targetAppealStatus = AppealStatusEnum.APPROVED.getCode();
            }
        }

        appealMapper.updateHandle(
                appealId,
                targetAppealStatus,
                handlerId,
                now,
                request.getDecisionComment(),
                targetFinalScore
        );
        refreshAttemptReviewState(answer.getAttemptId());
    }

    private void updateAttemptScoreByDelta(Long attemptId, Integer questionType, int delta) {
        if (attemptId == null || delta == 0) {
            return;
        }
        int deltaObjective = isObjective(questionType) ? delta : 0;
        int deltaSubjective = isObjective(questionType) ? 0 : delta;
        attemptMapper.updateScoreDelta(attemptId, delta, deltaObjective, deltaSubjective);
    }

    private boolean isObjective(Integer questionType) {
        if (questionType == null) {
            return false;
        }
        return questionType == QuestionTypeEnum.SINGLE.getCode()
                || questionType == QuestionTypeEnum.MULTIPLE.getCode()
                || questionType == QuestionTypeEnum.TRUE_FALSE.getCode()
                || questionType == QuestionTypeEnum.BLANK.getCode();
    }

    private Integer normalizeStatus(Integer status, boolean defaultPendingForTeacher) {
        if (status == null) {
            return defaultPendingForTeacher ? AppealStatusEnum.PENDING.getCode() : null;
        }
        if (status < AppealStatusEnum.PENDING.getCode() || status > AppealStatusEnum.RESOLVED.getCode()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "申诉状态必须在 1 到 4 之间");
        }
        return status;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void ensureCanHandleAttempt(QbAttempt attempt, Long actorId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (attempt == null || attempt.getAssignmentId() == null) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权处理该申诉");
        }
        QbAssignment assignment = assignmentMapper.selectById(attempt.getAssignmentId());
        if (assignment == null || !Objects.equals(assignment.getCreatedBy(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权处理该申诉");
        }
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
}
