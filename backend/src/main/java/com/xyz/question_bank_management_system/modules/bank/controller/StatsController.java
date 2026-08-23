package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.entity.*;
import com.xyz.question_bank_management_system.modules.profile.entity.QbTagMastery;
import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;
import com.xyz.question_bank_management_system.modules.bank.service.StatsService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/wrong-questions")
    public ApiResponse<PageResponse<QbWrongQuestion>> wrongQuestions(
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) Boolean isResolved,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(statsService.wrongQuestions(uid, tagId, chapter, isResolved, page, size));
    }

    @PostMapping("/wrong-questions/{questionId}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        statsService.resolveWrongQuestion(uid, questionId);
        return ApiResponse.ok();
    }

    @GetMapping("/mastery")
    public ApiResponse<List<QbTagMastery>> mastery(@RequestParam(defaultValue = "1") Integer tagType) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(statsService.mastery(uid, tagType));
    }

    @GetMapping("/ability")
    public ApiResponse<QbUserAbility> ability() {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(statsService.ability(uid));
    }

    @GetMapping("/question-stats")
    public ApiResponse<PageResponse<QbQuestionUserStat>> questionStats(
            @RequestParam(required = false) Long questionId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(statsService.questionStats(uid, questionId, page, size));
    }
}
