package com.xyz.question_bank_management_system.modules.competency.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.competency.entity.*;
import com.xyz.question_bank_management_system.modules.competency.service.CompetencyCatalogService;
import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/competency")
@RequiredArgsConstructor
public class CompetencyController {
    private final CompetencyCatalogService service;
    @GetMapping("/occupations") public ApiResponse<PageResponse<Occupation>> occupations(@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer page,@RequestParam(required=false) Integer size){ return ApiResponse.ok(service.occupations(keyword,page,size)); }
    @GetMapping("/occupations/{id}") public ApiResponse<Occupation> occupation(@PathVariable Long id){ return ApiResponse.ok(service.occupation(id)); }
    @GetMapping("/occupations/{id}/skills") public ApiResponse<List<OccupationSkill>> occupationSkills(@PathVariable Long id){ return ApiResponse.ok(service.occupationSkills(id)); }
    @GetMapping("/skills") public ApiResponse<PageResponse<Skill>> skills(@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer page,@RequestParam(required=false) Integer size){ return ApiResponse.ok(service.skills(keyword,page,size)); }
    @GetMapping("/skills/{id}") public ApiResponse<Skill> skill(@PathVariable Long id){ return ApiResponse.ok(service.skill(id)); }
    @GetMapping("/skills/{id}/knowledge-points") public ApiResponse<List<SkillKnowledge>> skillKnowledge(@PathVariable Long id){ return ApiResponse.ok(service.skillKnowledge(id)); }
    @GetMapping("/occupations/{id}/delete-impact") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<DeleteImpactVO> occupationImpact(@PathVariable Long id){ return ApiResponse.ok(service.occupationDeleteImpact(id)); }
    @DeleteMapping("/occupations/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> deleteOccupation(@PathVariable Long id){ service.deleteOccupation(id, SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
    @GetMapping("/skills/{id}/delete-impact") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<DeleteImpactVO> skillImpact(@PathVariable Long id){ return ApiResponse.ok(service.skillDeleteImpact(id)); }
    @DeleteMapping("/skills/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> deleteSkill(@PathVariable Long id){ service.deleteSkill(id, SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
    @DeleteMapping("/occupation-aliases/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> detachAlias(@PathVariable Long id){ service.detachOccupationAlias(id, SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
    @DeleteMapping("/occupation-skills/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> detachOccupationSkill(@PathVariable Long id){ service.detachOccupationSkill(id, SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
    @DeleteMapping("/skill-knowledge/{id}") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public ApiResponse<Void> detachSkillKnowledge(@PathVariable Long id){ service.detachSkillKnowledge(id, SecurityContextUtil.getUserId()); return ApiResponse.ok(); }
}
