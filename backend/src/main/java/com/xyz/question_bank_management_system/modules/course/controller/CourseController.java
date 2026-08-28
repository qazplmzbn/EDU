package com.xyz.question_bank_management_system.modules.course.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.course.dto.CourseKnowledgeReplaceRequest;
import com.xyz.question_bank_management_system.modules.course.dto.CourseUpsertRequest;
import com.xyz.question_bank_management_system.modules.course.entity.Course;
import com.xyz.question_bank_management_system.modules.course.entity.CourseKnowledge;
import com.xyz.question_bank_management_system.modules.course.service.CourseLearningPathService;
import com.xyz.question_bank_management_system.modules.course.vo.CourseStudentProgressVO;
import com.xyz.question_bank_management_system.modules.course.vo.LearningPathDetailVO;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class CourseController {
    private final CourseLearningPathService service;

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CourseUpsertRequest request) {
        return ApiResponse.ok(service.createCourse(request, SecurityContextUtil.getUserId(), isAdmin()));
    }

    @PutMapping("/{courseId}")
    public ApiResponse<Void> update(@PathVariable Long courseId, @RequestBody @Valid CourseUpsertRequest request) {
        service.updateCourse(courseId, request, SecurityContextUtil.getUserId(), isAdmin());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> delete(@PathVariable Long courseId) {
        service.deleteCourse(courseId, SecurityContextUtil.getUserId(), isAdmin());
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<List<Course>> mine() {
        return ApiResponse.ok(service.manageableCourses(SecurityContextUtil.getUserId(), isAdmin()));
    }

    @GetMapping("/{courseId}/knowledge")
    public ApiResponse<List<CourseKnowledge>> knowledge(@PathVariable Long courseId) {
        return ApiResponse.ok(service.courseKnowledge(courseId, SecurityContextUtil.getUserId(), isAdmin()));
    }

    @PutMapping("/{courseId}/knowledge")
    public ApiResponse<Void> replaceKnowledge(@PathVariable Long courseId, @RequestBody @Valid CourseKnowledgeReplaceRequest request) {
        service.replaceCourseKnowledge(courseId, request, SecurityContextUtil.getUserId(), isAdmin());
        return ApiResponse.ok();
    }

    @GetMapping("/{courseId}/students/progress")
    public ApiResponse<List<CourseStudentProgressVO>> studentProgress(@PathVariable Long courseId) {
        return ApiResponse.ok(service.courseStudentProgress(courseId, SecurityContextUtil.getUserId(), isAdmin()));
    }

    @GetMapping("/paths/{pathId}")
    public ApiResponse<LearningPathDetailVO> path(@PathVariable Long pathId) {
        return ApiResponse.ok(service.teacherPath(pathId, SecurityContextUtil.getUserId(), isAdmin()));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
