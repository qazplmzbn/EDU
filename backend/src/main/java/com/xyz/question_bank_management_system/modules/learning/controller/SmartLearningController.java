package com.xyz.question_bank_management_system.modules.learning.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgeGraphExtractionRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceRecommendRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceUpsertRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningPathSnapshotSaveRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PersonalizedPracticeRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.PracticeStartRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.bank.service.AttemptService;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgeGraphService;
import com.xyz.question_bank_management_system.modules.learning.service.SmartLearningService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.AttemptStartVO;
import com.xyz.question_bank_management_system.modules.knowledge.vo.KnowledgeGraphExtractionVO;
import com.xyz.question_bank_management_system.modules.learning.vo.PersonalizedPracticePlanVO;
import com.xyz.question_bank_management_system.modules.learning.vo.SmartLearningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class SmartLearningController {

    private final SmartLearningService smartLearningService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final AttemptService attemptService;

    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<QbLearningResource>> resources(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Long knowledgePointId,
                                                           @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(smartLearningService.resources(keyword, knowledgePointId, limit));
    }

    @PostMapping("/resources")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Long> createResource(@RequestBody LearningResourceUpsertRequest request) {
        return ApiResponse.ok(smartLearningService.createResource(request, SecurityContextUtil.getUserId()));
    }

    @PostMapping("/resources/{id}/recommend-targets")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<SmartLearningVO.ResourceRecommendationPublishResult> recommendResourceTargets(@PathVariable Long id,
                                                                                                           @RequestBody(required = false) LearningResourceRecommendRequest request) {
        return ApiResponse.ok(smartLearningService.recommendResourceTargets(id, request, SecurityContextUtil.getUserId(), hasRole("ROLE_ADMIN")));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> updateResource(@PathVariable Long id, @RequestBody LearningResourceUpsertRequest request) {
        smartLearningService.updateResource(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        smartLearningService.deleteResource(id);
        return ApiResponse.ok();
    }

    @PostMapping("/behaviors")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Long> recordBehavior(@RequestBody QbLearningBehavior behavior) {
        return ApiResponse.ok(smartLearningService.recordBehavior(behavior, SecurityContextUtil.getUserId()));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.LearningProfile> profile() {
        return ApiResponse.ok(smartLearningService.profile(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/profile/report")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.StudentProfileReport> profileReport() {
        return ApiResponse.ok(smartLearningService.profileReport(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.LearningRecommendation> recommendations() {
        return ApiResponse.ok(smartLearningService.recommendations(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/path-recommendation")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.LearningPathRecommendation> pathRecommendation(@RequestParam(required = false) String stage,
                                                                                           @RequestParam(required = false) String goal,
                                                                                           @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(smartLearningService.pathRecommendation(SecurityContextUtil.getUserId(), stage, goal, days));
    }

    @PostMapping("/path-recommendation/snapshots")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.LearningPathSnapshotSaved> savePathSnapshot(@RequestBody(required = false) LearningPathSnapshotSaveRequest request) {
        throw BizException.of(ErrorCode.CONFLICT, "旧学习路径快照已进入只读兼容模式，请使用课程正式学习路径接口");
    }

    @GetMapping("/path-recommendation/snapshots/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SmartLearningVO.LearningPathRecommendation> pathSnapshotDetail(@PathVariable Long id) {
        return ApiResponse.ok(smartLearningService.pathSnapshotDetail(SecurityContextUtil.getUserId(), id));
    }

    @GetMapping("/path-recommendation/snapshots")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SmartLearningVO.LearningPathSnapshotItem>> pathSnapshots(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(smartLearningService.pathSnapshots(SecurityContextUtil.getUserId(), limit));
    }

    @GetMapping("/knowledge-relations")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<QbKnowledgeRelation>> knowledgeRelations() {
        return ApiResponse.ok(knowledgeGraphService.relations());
    }

    @PostMapping("/knowledge-graph/extract")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeGraphExtractionVO> extractKnowledgeGraph(@RequestBody KnowledgeGraphExtractionRequest request) {
        return ApiResponse.ok(knowledgeGraphService.extract(request));
    }

    @PostMapping(value = "/knowledge-graph/extract-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeGraphExtractionVO> extractKnowledgeGraphFile(@RequestPart("file") MultipartFile file,
                                                                             @RequestParam(required = false) Boolean autoSave,
                                                                             @RequestParam(required = false) String providerKey) {
        return ApiResponse.ok(knowledgeGraphService.extractFromFile(file, autoSave, providerKey));
    }

    @PostMapping("/knowledge-relations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> createKnowledgeRelation(@RequestBody QbKnowledgeRelation relation) {
        return ApiResponse.ok(knowledgeGraphService.createRelation(relation));
    }

    @PutMapping("/knowledge-relations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateKnowledgeRelation(@PathVariable Long id, @RequestBody QbKnowledgeRelation relation) {
        knowledgeGraphService.updateRelation(id, relation);
        return ApiResponse.ok();
    }

    @DeleteMapping("/knowledge-relations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteKnowledgeRelation(@PathVariable Long id) {
        knowledgeGraphService.deleteRelation(id);
        return ApiResponse.ok();
    }

    @GetMapping("/personalized-practice/plan")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PersonalizedPracticePlanVO> personalizedPracticePlan(PersonalizedPracticeRequest request) {
        return ApiResponse.ok(smartLearningService.personalizedPracticePlan(SecurityContextUtil.getUserId(), request));
    }

    @PostMapping("/personalized-practice/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttemptStartVO> startPersonalizedPractice(@RequestBody PersonalizedPracticeRequest request) {
        Long userId = SecurityContextUtil.getUserId();
        PracticeStartRequest practiceRequest = smartLearningService.buildPersonalizedPracticeRequest(userId, request);
        return ApiResponse.ok(attemptService.startPracticeAttempt(practiceRequest, userId));
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
