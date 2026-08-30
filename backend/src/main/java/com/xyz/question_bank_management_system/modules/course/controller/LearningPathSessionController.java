package com.xyz.question_bank_management_system.modules.course.controller;
import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.course.service.PathRefreshApplicationService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor public class LearningPathSessionController {
 private final PathRefreshApplicationService service;
 @PostMapping("/api/v1/learning-path-sessions") @PreAuthorize("hasRole('STUDENT')") public ApiResponse<Map<String,Object>> create(@RequestHeader("Idempotency-Key")String key,@RequestBody CreateRequest request){return ApiResponse.ok(service.create(SecurityContextUtil.getUserId(),request.courseId,request.targetKnowledgePointId,key));}
 @GetMapping("/api/v1/learning-path-sessions/{pathCode}") @PreAuthorize("isAuthenticated()") public ApiResponse<Map<String,Object>> get(@PathVariable String pathCode){return ApiResponse.ok(service.get(SecurityContextUtil.getUserId(),pathCode,SecurityContextUtil.currentRoles().contains("ADMIN")));}
 @PostMapping("/api/v1/learning-path-sessions/{pathCode}/refresh") @PreAuthorize("hasRole('STUDENT')") public ApiResponse<Map<String,Object>> refresh(@PathVariable String pathCode){return ApiResponse.ok(service.refresh(SecurityContextUtil.getUserId(),pathCode,"MANUAL_REPLAN",false));}
 @PostMapping("/internal/v1/learning-path-sessions/{pathCode}/evaluate-event") @PreAuthorize("hasAuthority('INTERNAL_SERVICE') or hasRole('ADMIN')") public ApiResponse<Map<String,Object>> event(@PathVariable String pathCode,@RequestBody Map<String,Object> body){return ApiResponse.ok(service.evaluateEvent(pathCode,body));}
 @Data public static class CreateRequest {private Long courseId;private Long targetKnowledgePointId;}
}
