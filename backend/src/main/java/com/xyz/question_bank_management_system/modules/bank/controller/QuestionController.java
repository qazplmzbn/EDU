package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.QuestionStatusEnum;
import com.xyz.question_bank_management_system.common.enums.QuestionTypeEnum;
import com.xyz.question_bank_management_system.modules.bank.dto.QuestionBankReviewRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.QuestionLlmAnalysisRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.QuestionSearchQuery;
import com.xyz.question_bank_management_system.modules.bank.dto.QuestionUpsertRequest;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.bank.service.QuestionService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.QuestionDetailVO;
import com.xyz.question_bank_management_system.modules.bank.vo.QuestionLlmBatchResultVO;
import com.xyz.question_bank_management_system.modules.bank.vo.QuestionListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
public class QuestionController {

    private final QuestionService questionService;
    private final QbClassMemberMapper classMemberMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Long> create(@RequestBody @Valid QuestionUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(questionService.create(request, uid));
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long questionId, @RequestBody @Valid QuestionUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        questionService.update(questionId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        questionService.delete(questionId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @GetMapping("/{questionId}")
    public ApiResponse<QuestionDetailVO> detail(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        boolean isTeacher = hasRole("ROLE_TEACHER");
        if (isAdmin) {
            return ApiResponse.ok(questionService.detail(questionId, uid, true));
        }
        return ApiResponse.ok(questionService.detailForViewer(questionId, uid, isTeacher, false));
    }

    @GetMapping
    public ApiResponse<PageResponse<QuestionListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer bankReviewStatus,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String tagIds,
            @RequestParam(required = false, defaultValue = "all") String source,
            @RequestParam(required = false, defaultValue = "false") Boolean studentView,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        QuestionSearchQuery q = new QuestionSearchQuery();
        q.setKeyword(keyword);
        q.setChapter(chapter);
        q.setDifficulty(difficulty);
        q.setQuestionType(normalizeEnabledQuestionType(questionType));
        q.setStatus(status);
        q.setBankReviewStatus(bankReviewStatus);
        q.setTagId(tagId);
        if (tagIds != null && !tagIds.isBlank()) {
            try {
                List<Long> ids = Arrays.stream(tagIds.split(","))
                        .filter(s -> !s.isBlank())
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                q.setTagIds(ids);
            } catch (NumberFormatException e) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "标签编号必须使用逗号分隔的数字");
            }
        }
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        boolean isTeacher = hasRole("ROLE_TEACHER");
        boolean forceStudentView = Boolean.TRUE.equals(studentView);

        if (forceStudentView || (!isTeacher && !isAdmin)) {
            q.setStatus(QuestionStatusEnum.PUBLISHED.getCode());
            q.setViewerId(uid);
            q.setStudentScope(true);
            q.setVisibleTeacherIds(classMemberMapper.listTeacherIdsByStudentId(uid));
        } else if (isTeacher && !isAdmin) {
            q.setTeacherScope(true);
            q.setViewerId(uid);
            q.setSourceType(normalizeSource(source));
        }
        return ApiResponse.ok(questionService.search(q, page, size));
    }

    @PostMapping("/{questionId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> publish(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        questionService.publish(questionId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @PostMapping("/{questionId}/bank-review/submit")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> submitBankReview(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        questionService.submitForBankReview(questionId, uid);
        return ApiResponse.ok();
    }

    @PostMapping("/{questionId}/bank-review/cancel")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> cancelBankReview(@PathVariable Long questionId) {
        Long uid = SecurityContextUtil.getUserId();
        questionService.cancelBankReview(questionId, uid);
        return ApiResponse.ok();
    }

    @PostMapping("/{questionId}/bank-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reviewBankQuestion(@PathVariable Long questionId,
                                                @RequestBody @Valid QuestionBankReviewRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        questionService.reviewBankQuestion(questionId, request, uid);
        return ApiResponse.ok();
    }

    @PostMapping("/{questionId}/analysis/llm")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<QuestionLlmBatchResultVO> llmAnalysis(@PathVariable Long questionId,
                                                             @RequestBody(required = false) QuestionLlmAnalysisRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        String providerKey = request == null ? null : request.getProviderKey();
        return ApiResponse.ok(questionService.generateAnalysisByLlm(questionId, providerKey, uid, isAdmin));
    }

    private boolean hasAnyRole(String... roles) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> Arrays.asList(roles).contains(a));
    }

    private boolean hasRole(String role) {
        return hasAnyRole(role);
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "all";
        }
        String normalized = source.trim().toLowerCase();
        if ("all".equals(normalized) || "mine".equals(normalized) || "bank".equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, "题目来源参数不合法");
    }

    private Integer normalizeEnabledQuestionType(Integer questionType) {
        if (questionType == null) {
            return null;
        }
        QuestionTypeEnum type = QuestionTypeEnum.of(questionType);
        if (type == null || !type.isEnabledNow()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "题型不可用或参数无效: " + questionType);
        }
        return questionType;
    }
}

