package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionUserStat;
import com.xyz.question_bank_management_system.modules.profile.entity.QbTagMastery;
import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;
import com.xyz.question_bank_management_system.modules.bank.entity.QbWrongQuestion;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionUserStatMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbTagMasteryMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbUserAbilityMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbWrongQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.service.StatsService;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final QbWrongQuestionMapper wrongQuestionMapper;
    private final QbQuestionUserStatMapper questionUserStatMapper;
    private final QbTagMasteryMapper tagMasteryMapper;
    private final QbUserAbilityMapper userAbilityMapper;

    @Override
    public PageResponse<QbWrongQuestion> wrongQuestions(Long userId, Long tagId, String chapter, Boolean isResolved, long page, long size) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        String chapterFilter = trimToNull(chapter);
        Integer resolvedFlag = isResolved == null ? null : (isResolved ? 1 : 0);

        List<QbWrongQuestion> rows = wrongQuestionMapper.pageByFilter(
                userId, tagId, chapterFilter, resolvedFlag, offset, safeSize
        );
        long total = wrongQuestionMapper.countByFilter(userId, tagId, chapterFilter, resolvedFlag);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public void resolveWrongQuestion(Long userId, Long questionId) {
        int updated = wrongQuestionMapper.resolve(userId, questionId, LocalDateTime.now());
        if (updated <= 0) {
            throw BizException.of(ErrorCode.NOT_FOUND, "错题记录不存在");
        }
    }

    @Override
    public PageResponse<QbQuestionUserStat> questionStats(Long userId, Long questionId, long page, long size) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<QbQuestionUserStat> rows = questionUserStatMapper.pageByFilter(userId, questionId, offset, safeSize);
        long total = questionUserStatMapper.countByFilter(userId, questionId);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public List<QbTagMastery> mastery(Long userId, Integer tagType) {
        int safeTagType = tagType == null ? 1 : tagType;
        if (safeTagType < 1 || safeTagType > 3) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "标签类型必须是 1、2 或 3");
        }
        return tagMasteryMapper.selectByUserIdAndTagType(userId, safeTagType);
    }

    @Override
    public QbUserAbility ability(Long userId) {
        QbUserAbility ability = userAbilityMapper.selectByUserId(userId);
        if (ability != null) {
            return ability;
        }
        QbUserAbility empty = new QbUserAbility();
        empty.setUserId(userId);
        empty.setAbilityScore(0);
        return empty;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
