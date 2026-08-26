package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAbilityServiceImplTest {

    @Mock
    private StudentProfileService studentProfileService;

    @InjectMocks
    private UserAbilityServiceImpl userAbilityService;

    @Test
    void recomputeAndPersist_shouldDelegateToPersistentProfileService() {
        userAbilityService.recomputeAndPersist(1001L);
        verify(studentProfileService).refreshAssessment(1001L);
    }
}
