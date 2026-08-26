package com.xyz.question_bank_management_system.modules.profile.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentBasicProfile;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentLearningGoal;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentLearningPreference;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/student-profile")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentProfileController {
    private final StudentProfileService profileService;

    @GetMapping("/basic") public ApiResponse<StudentBasicProfile> basic() { return ApiResponse.ok(profileService.basicProfile(SecurityContextUtil.getUserId())); }
    @PutMapping("/basic") public ApiResponse<Void> saveBasic(@RequestBody @Valid StudentBasicProfile profile) { profileService.saveBasicProfile(SecurityContextUtil.getUserId(), profile); return ApiResponse.ok(); }
    @GetMapping("/goals") public ApiResponse<List<StudentLearningGoal>> goals() { return ApiResponse.ok(profileService.goals(SecurityContextUtil.getUserId())); }
    @PostMapping("/goals") public ApiResponse<Long> saveGoal(@RequestBody @Valid StudentLearningGoal goal) { return ApiResponse.ok(profileService.saveGoal(SecurityContextUtil.getUserId(), goal)); }
    @PutMapping("/goals/{id}") public ApiResponse<Long> updateGoal(@PathVariable Long id,@RequestBody @Valid StudentLearningGoal goal) { goal.setId(id); return ApiResponse.ok(profileService.saveGoal(SecurityContextUtil.getUserId(), goal)); }
    @GetMapping("/preferences") public ApiResponse<List<StudentLearningPreference>> preferences() { return ApiResponse.ok(profileService.preferences(SecurityContextUtil.getUserId())); }
    @PutMapping("/preferences") public ApiResponse<Void> savePreference(@RequestBody @Valid StudentLearningPreference preference) { profileService.savePreference(SecurityContextUtil.getUserId(), preference); return ApiResponse.ok(); }
}
