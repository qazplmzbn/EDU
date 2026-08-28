package com.xyz.question_bank_management_system.modules.course.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.course.dto.CoursePathGenerateRequest;
import com.xyz.question_bank_management_system.modules.course.service.CourseLearningPathService;
import com.xyz.question_bank_management_system.modules.course.vo.CourseProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.LearningPathDetailVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentCourseController {
    private final CourseLearningPathService service;

    @GetMapping("/courses")
    public ApiResponse<List<CourseProgressVO>> courses() {
        return ApiResponse.ok(service.visibleCourses(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ApiResponse<CourseProgressVO> progress(@PathVariable Long courseId) {
        return ApiResponse.ok(service.courseProgress(courseId, SecurityContextUtil.getUserId()));
    }

    @PostMapping("/courses/{courseId}/paths")
    public ApiResponse<Long> generatePath(@PathVariable Long courseId, @RequestBody(required = false) @Valid CoursePathGenerateRequest request) {
        return ApiResponse.ok(service.generateCoursePath(courseId, request, SecurityContextUtil.getUserId()));
    }

    @GetMapping("/paths")
    public ApiResponse<List<LearningPathDetailVO>> paths() {
        return ApiResponse.ok(service.studentPaths(SecurityContextUtil.getUserId()));
    }

    @GetMapping("/paths/{pathId}")
    public ApiResponse<LearningPathDetailVO> path(@PathVariable Long pathId) {
        return ApiResponse.ok(service.studentPath(pathId, SecurityContextUtil.getUserId()));
    }

    @PostMapping("/paths/{pathId}/items/{itemId}/complete")
    public ApiResponse<LearningPathDetailVO> complete(@PathVariable Long pathId, @PathVariable Long itemId) {
        return ApiResponse.ok(service.completePathItem(pathId, itemId, SecurityContextUtil.getUserId()));
    }
}
