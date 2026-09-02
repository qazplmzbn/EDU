package com.xyz.question_bank_management_system.modules.profile.controller;
import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.profile.model.ValidatedInteraction;
import com.xyz.question_bank_management_system.modules.profile.service.*;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor public class V1ProfileController {
 private final ProfileQueryService queryService;private final ProfileAggregationService aggregationService;private final com.xyz.question_bank_management_system.modules.learning.service.InteractionSubmissionService interactionService;
 @GetMapping("/api/v1/students/me/profile-summary") @PreAuthorize("hasRole('STUDENT')") public ApiResponse<Map<String,Object>> summary(@RequestParam Long courseId){return ApiResponse.ok(queryService.summary(SecurityContextUtil.getUserId(),courseId));}
 @GetMapping("/internal/v1/profiles/{userId}/courses/{courseId}/knowledge-states") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<Map<String,Object>> knowledge(@PathVariable Long userId,@PathVariable Long courseId,@RequestParam(required=false) List<Long> ids){return ApiResponse.ok(queryService.knowledgeStates(userId,courseId,ids));}
 @GetMapping("/internal/v1/profiles/{userId}/courses/{courseId}/resource-preferences") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<Map<String,Object>> preferences(@PathVariable Long userId,@PathVariable Long courseId){return ApiResponse.ok(queryService.resourcePreferences(userId,courseId));}
 @GetMapping("/internal/v1/profiles/{userId}/courses/{courseId}/cognitive-profile") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<Map<String,Object>> cognitive(@PathVariable Long userId,@PathVariable Long courseId){return ApiResponse.ok(queryService.cognitiveProfile(userId,courseId));}
 @PostMapping("/internal/v1/profiles/interactions/{interactionId}/apply") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<Map<String,Object>> applyInteraction(@PathVariable Long interactionId){return ApiResponse.ok(interactionService.applyProfileEvidence(interactionId));}
 @PostMapping("/internal/v1/profiles/{userId}/courses/{courseId}/recalibrate") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<?> recalibrate(@PathVariable Long userId,@PathVariable Long courseId,@RequestBody List<ValidatedInteraction> history){return ApiResponse.ok(aggregationService.recalibrate(userId,courseId,history));}
}
