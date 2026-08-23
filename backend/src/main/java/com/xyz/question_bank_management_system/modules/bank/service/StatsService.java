package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.entity.*;
import com.xyz.question_bank_management_system.modules.profile.entity.QbTagMastery;
import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;

import java.util.List;

public interface StatsService {

    PageResponse<QbWrongQuestion> wrongQuestions(Long userId, Long tagId, String chapter, Boolean isResolved, long page, long size);

    void resolveWrongQuestion(Long userId, Long questionId);

    PageResponse<QbQuestionUserStat> questionStats(Long userId, Long questionId, long page, long size);

    List<QbTagMastery> mastery(Long userId, Integer tagType);

    QbUserAbility ability(Long userId);
}
