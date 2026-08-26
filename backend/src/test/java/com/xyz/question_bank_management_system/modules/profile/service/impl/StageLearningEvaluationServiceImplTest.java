package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentAbilityState;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.mapper.StageEvaluationMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentAbilityStateMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageLearningEvaluationServiceImplTest {
    @Mock private StageEvaluationMapper evaluationMapper;
    @Mock private StudentAbilityStateMapper abilityStateMapper;
    @Mock private StudentProfileService profileService;
    @Mock private QbClassMemberMapper classMemberMapper;
    @Mock private SysUserMapper userMapper;

    private StageLearningEvaluationServiceImpl service() {
        return new StageLearningEvaluationServiceImpl(evaluationMapper, abilityStateMapper, profileService, classMemberMapper, userMapper, new ObjectMapper());
    }

    @Test
    void myEvaluation_shouldRemainReadOnlyWhenNoPersistedRecord() {
        SysUser student = new SysUser(); student.setId(1001L); student.setDisplayName("student");
        when(userMapper.selectById(1001L)).thenReturn(student);
        when(profileService.abilityScore(1001L)).thenReturn(40);

        StageLearningEvaluationVO result = service().myEvaluation(1001L, "month", null, null);

        assertFalse(result.getGenerated());
        assertEquals("NOT_GENERATED", result.getAlgorithmStatus());
        verify(evaluationMapper, never()).insert(any());
        verify(profileService, never()).createSnapshot(anyLong(), anyString(), any());
    }

    @Test
    void generate_shouldPersistSnapshotAndNewEvaluationVersion() {
        when(classMemberMapper.listStudentIdsByTeacherId(2001L)).thenReturn(List.of(1001L));
        StudentProfileSnapshot snapshot = new StudentProfileSnapshot(); snapshot.setId(501L);
        when(profileService.createSnapshot(1001L, "assessment", null)).thenReturn(snapshot);
        StudentAbilityState ability = new StudentAbilityState(); ability.setDimensionCode("ABILITY"); ability.setDimensionName("能力水平"); ability.setScore(BigDecimal.valueOf(82));
        when(abilityStateMapper.selectByUserId(1001L)).thenReturn(List.of(ability));
        SysUser student = new SysUser(); student.setId(1001L); student.setDisplayName("student");
        when(userMapper.selectById(1001L)).thenReturn(student);
        when(profileService.abilityScore(1001L)).thenReturn(82);

        StageLearningEvaluationVO result = service().generate(2001L, false, 1001L, "month", null, null);

        assertTrue(result.getGenerated());
        ArgumentCaptor<com.xyz.question_bank_management_system.modules.profile.entity.StageEvaluation> captor = ArgumentCaptor.forClass(com.xyz.question_bank_management_system.modules.profile.entity.StageEvaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        assertEquals(501L, captor.getValue().getProfileSnapshotId());
        assertEquals("final", captor.getValue().getStatus());
        verify(profileService).refreshAssessment(1001L);
    }
}
