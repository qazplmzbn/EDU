package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperAddQuestionRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import com.xyz.question_bank_management_system.modules.bank.service.PaperService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.PaperDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class PaperController {

    private final PaperService paperService;

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid PaperUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        return ApiResponse.ok(paperService.create(request, uid));
    }

    @PutMapping("/{paperId}")
    public ApiResponse<Void> update(@PathVariable Long paperId, @RequestBody @Valid PaperUpsertRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        paperService.update(paperId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{paperId}")
    public ApiResponse<Void> delete(@PathVariable Long paperId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        paperService.delete(paperId, uid, isAdmin);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<PageResponse<QbPaper>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(paperService.page(page, size, uid, isAdmin));
    }

    @GetMapping("/{paperId}")
    public ApiResponse<PaperDetailVO> detail(@PathVariable Long paperId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(paperService.detail(paperId, uid, isAdmin));
    }

    @PostMapping("/{paperId}/questions")
    public ApiResponse<Long> addQuestion(@PathVariable Long paperId, @RequestBody @Valid PaperAddQuestionRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        return ApiResponse.ok(paperService.addQuestion(paperId, request, uid, isAdmin));
    }

    @PutMapping("/{paperId}/questions/batch")
    public ApiResponse<Void> batchUpdatePaperQuestions(@PathVariable Long paperId, @RequestBody @Valid PaperQuestionBatchUpdateRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        paperService.batchUpdatePaperQuestions(paperId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    @PutMapping("/questions/{paperQuestionId}")
    public ApiResponse<Void> updatePaperQuestion(@PathVariable Long paperQuestionId, @RequestBody @Valid PaperQuestionUpdateRequest request) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        paperService.updatePaperQuestion(paperQuestionId, request, uid, isAdmin);
        return ApiResponse.ok();
    }

    @DeleteMapping("/questions/{paperQuestionId}")
    public ApiResponse<Void> removePaperQuestion(@PathVariable Long paperQuestionId) {
        Long uid = SecurityContextUtil.getUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        paperService.removePaperQuestion(paperQuestionId, uid, isAdmin);
        return ApiResponse.ok();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }
}
