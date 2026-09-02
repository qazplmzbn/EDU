package com.xyz.question_bank_management_system.modules.learning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbGradingRecordMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgeRelationMapper;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceRecommendRequest;
import com.xyz.question_bank_management_system.modules.learning.dto.LearningResourceUpsertRequest;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResourceTarget;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningBehaviorMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningPathSnapshotMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceTargetMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.ResourceKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmService;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.org.entity.QbClass;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentKnowledgeStateMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartLearningResourceTargetServiceTest {

    @Mock private QbKnowledgePointMapper knowledgePointMapper;
    @Mock private QbLearningResourceMapper resourceMapper;
    @Mock private QbLearningBehaviorMapper behaviorMapper;
    @Mock private StudentKnowledgeStateMapper knowledgeStateMapper;
    @Mock private StudentProfileService profileService;
    @Mock private QbAttemptMapper attemptMapper;
    @Mock private QbLlmCallMapper llmCallMapper;
    @Mock private QbGradingRecordMapper gradingRecordMapper;
    @Mock private QbKnowledgeRelationMapper knowledgeRelationMapper;
    @Mock private QbLearningPathSnapshotMapper pathSnapshotMapper;
    @Mock private QbLearningResourceTargetMapper targetMapper;
    @Mock private ResourceKnowledgeMapper resourceKnowledgeMapper;
    @Mock private QbClassMemberMapper classMemberMapper;
    @Mock private QbClassMapper classMapper;
    @Mock private LlmService llmService;

    @Test
    void classDeliveryPersistsOneClassTargetWithoutStudentId() {
        SmartLearningServiceImpl service = new SmartLearningServiceImpl(
                knowledgePointMapper, resourceMapper, behaviorMapper, knowledgeStateMapper, profileService,
                attemptMapper, llmCallMapper, gradingRecordMapper, knowledgeRelationMapper, pathSnapshotMapper,
                targetMapper, resourceKnowledgeMapper, classMemberMapper, classMapper, llmService, new ObjectMapper());
        QbLearningResource resource = new QbLearningResource();
        resource.setId(7L);
        QbClass clazz = new QbClass();
        clazz.setId(11L);
        clazz.setTeacherId(5L);
        when(resourceMapper.selectById(7L)).thenReturn(resource);
        when(classMapper.selectById(11L)).thenReturn(clazz);
        when(classMemberMapper.listStudentIdsByClassIds(List.of(11L))).thenReturn(List.of(21L, 22L));

        LearningResourceRecommendRequest request = new LearningResourceRecommendRequest();
        request.setTargetType("class");
        request.setClassId(11L);
        var result = service.recommendResourceTargets(7L, request, 5L, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QbLearningResourceTarget>> captor = ArgumentCaptor.forClass(List.class);
        verify(targetMapper).batchInsert(captor.capture());
        List<QbLearningResourceTarget> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals("class", saved.get(0).getTargetType());
        assertEquals(11L, saved.get(0).getClassId());
        assertNull(saved.get(0).getStudentId());
        assertEquals(2, result.getTargetCount());
    }

    @Test
    void createResourceRejectsInvalidJsonBeforeDatabaseInsert() {
        SmartLearningServiceImpl service = new SmartLearningServiceImpl(
                knowledgePointMapper, resourceMapper, behaviorMapper, knowledgeStateMapper, profileService,
                attemptMapper, llmCallMapper, gradingRecordMapper, knowledgeRelationMapper, pathSnapshotMapper,
                targetMapper, resourceKnowledgeMapper, classMemberMapper, classMapper, llmService, new ObjectMapper());
        LearningResourceUpsertRequest request = new LearningResourceUpsertRequest();
        request.setTitle("fixture");
        request.setPersonalizationBasis("not-json");

        assertThrows(BizException.class, () -> service.createResource(request, 5L));
        verify(resourceMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
