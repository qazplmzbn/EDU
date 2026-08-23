package com.xyz.question_bank_management_system.modules.bank.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.TagCreateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.TagUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbTag;
import com.xyz.question_bank_management_system.modules.bank.service.TagService;
import com.xyz.question_bank_management_system.modules.bank.vo.TagTreeNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<QbTag>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(tagService.list(keyword));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<TagTreeNode>> tree() {
        return ApiResponse.ok(tagService.tree());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> create(@RequestBody @Valid TagCreateRequest request) {
        return ApiResponse.ok(tagService.create(request));
    }

    @PutMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long tagId, @RequestBody @Valid TagUpdateRequest request) {
        tagService.update(tagId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return ApiResponse.ok();
    }
}
