package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.xyz.question_bank_management_system.modules.profile.service.UserAbilityService;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAbilityServiceImpl implements UserAbilityService {
    private final StudentProfileService studentProfileService;

    @Override
    public void recomputeAndPersist(Long userId) {
        studentProfileService.refreshAssessment(userId);
    }
}
