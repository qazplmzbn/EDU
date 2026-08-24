package com.xyz.question_bank_management_system.modules.knowledge.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgePointSaveRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgePointService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/learning/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {
    private final KnowledgePointService service;
    @GetMapping @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')") public ApiResponse<List<KnowledgePoint>> list(){ return ApiResponse.ok(service.list()); }
    @PostMapping @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Long> create(@Valid @RequestBody KnowledgePointSaveRequest request){ return ApiResponse.ok(service.create(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> update(@PathVariable Long id,@Valid @RequestBody KnowledgePointSaveRequest request){ service.update(id,request); return ApiResponse.ok(); }
    @GetMapping("/{id}/delete-impact") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<DeleteImpactVO> deleteImpact(@PathVariable Long id){ return ApiResponse.ok(service.deleteImpact(id)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> delete(@PathVariable Long id){ service.delete(id,SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
}
