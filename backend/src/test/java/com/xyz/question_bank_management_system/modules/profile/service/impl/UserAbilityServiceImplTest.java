package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.xyz.question_bank_management_system.modules.profile.entity.QbAbilitySample;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAnswerMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbUserAbilityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAbilityServiceImplTest {

    @Mock
    private QbAnswerMapper answerMapper;
    @Mock
    private QbUserAbilityMapper userAbilityMapper;

    @InjectMocks
    private UserAbilityServiceImpl userAbilityService;

    @Test
    void recomputeAndPersist_shouldWriteZeroWhenNoHistory() {
        when(answerMapper.selectAbilitySamplesByUserId(1001L)).thenReturn(List.of());

        userAbilityService.recomputeAndPersist(1001L);

        verify(userAbilityMapper).upsert(1001L, 0);
    }

    @Test
    void recomputeAndPersist_shouldProduceHigherScoreForConsistentCorrectAnswers() {
        List<QbAbilitySample> samples = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            QbAbilitySample sample = new QbAbilitySample();
            sample.setQuestionId(10_000L + i);
            sample.setMaxScore(5);
            sample.setFinalScore(5);
            sample.setDifficulty(3);
            sample.setEventTime(LocalDateTime.now().minusDays(20L - i));
            samples.add(sample);
        }
        when(answerMapper.selectAbilitySamplesByUserId(1002L)).thenReturn(samples);

        userAbilityService.recomputeAndPersist(1002L);

        ArgumentCaptor<Integer> scoreCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(userAbilityMapper).upsert(org.mockito.ArgumentMatchers.eq(1002L), scoreCaptor.capture());
        assertTrue(scoreCaptor.getValue() > 50);
    }
}
