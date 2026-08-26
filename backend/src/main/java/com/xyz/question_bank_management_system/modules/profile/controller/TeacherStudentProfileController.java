package com.xyz.question_bank_management_system.modules.profile.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSummary;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-profiles")
@RequiredArgsConstructor
public class TeacherStudentProfileController {
    private final StudentProfileService profileService;
    private final QbClassMemberMapper classMemberMapper;

    @GetMapping("/{studentId}/summary")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<StudentProfileSummary> summary(@PathVariable Long studentId) {
        boolean admin = SecurityContextUtil.currentRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        if (!admin && !classMemberMapper.listStudentIdsByTeacherId(SecurityContextUtil.getUserId()).contains(studentId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "只能查看自己班级学生的画像摘要");
        }
        return ApiResponse.ok(profileService.summary(studentId));
    }
}
