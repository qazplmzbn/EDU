package com.xyz.question_bank_management_system.modules.learning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceBundle;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceItem;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceUnit;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.agent.service.ResourceGenerationWorkflow;
import com.xyz.question_bank_management_system.modules.course.service.PathRefreshApplicationService;
import com.xyz.question_bank_management_system.modules.learning.dto.ResourceInteractionSubmitRequest;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathProgress;
import com.xyz.question_bank_management_system.modules.learning.entity.ResourceInteraction;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathV1Mapper;
import com.xyz.question_bank_management_system.modules.learning.service.ProfileEvidenceConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionSubmissionServiceImplTest {
    @Mock PersonalizedResourceMapper mapper;
    @Mock LearningPathV1Mapper pathMapper;
    @Mock ProfileEvidenceConsumer profileConsumer;
    @Mock PathRefreshApplicationService pathService;
    @Mock ResourceGenerationWorkflow resourceWorkflow;

    @Test void duplicateAtomicInsert_replaysCurrentReadInsteadOfThrowing() {
        ResourceItem item = new ResourceItem();
        item.setId(11L); item.setBundleId(12L); item.setGeneratedQuestionCode("generated-q");
        item.setPurpose("LEARNING_PRACTICE"); item.setVisibility("VISIBLE");
        item.setQuestionDifficulty(new BigDecimal("0.5")); item.setGradingKeyJson("{\"standardAnswer\":\"A\"}");
        ResourceBundle bundle = new ResourceBundle();
        bundle.setId(12L); bundle.setUserId(1L); bundle.setCourseId(2L); bundle.setResourceUnitId(13L); bundle.setVersion(1L); bundle.setStatus("PUBLISHED");
        ResourceUnit unit = new ResourceUnit(); unit.setId(13L); unit.setPathId(14L);
        LearningPath path = new LearningPath(); path.setId(14L); path.setUserId(1L); path.setPathCode("path-x"); path.setCurrentVersion(1L);
        LearningPathProgress progress = new LearningPathProgress(); progress.setLastProcessedInteractionSeq(0L);
        ResourceInteraction first = new ResourceInteraction();
        first.setId(99L); first.setUserId(1L); first.setInteractionCode("int-first"); first.setScoreNormalized(BigDecimal.ONE); first.setCorrect(1); first.setStatus("SUBMITTED");
        ResourceInteractionSubmitRequest request = new ResourceInteractionSubmitRequest();
        request.setGeneratedQuestionCode("generated-q"); request.setAnswer("A"); request.setActionOrigin("USER_INITIATED");

        when(mapper.selectInteractionByRequest("same-key")).thenReturn(null);
        when(mapper.selectQuestionForUpdate("generated-q")).thenReturn(item);
        when(mapper.selectBundleByIdForUpdate(12L)).thenReturn(bundle);
        when(mapper.selectUnitById(13L)).thenReturn(unit);
        when(pathMapper.selectPathById(14L)).thenReturn(path);
        when(pathMapper.selectProgressForUpdate(14L)).thenReturn(progress);
        when(mapper.itemKnowledge(11L)).thenReturn(List.of(Map.of("knowledge_point_id", 21L, "coverage_weight", BigDecimal.ONE, "is_primary", 1)));
        when(mapper.insertInteraction(any(ResourceInteraction.class))).thenReturn(0);
        when(mapper.selectInteractionByRequestForUpdate("same-key")).thenReturn(first);

        InteractionSubmissionServiceImpl service = new InteractionSubmissionServiceImpl(mapper, pathMapper, profileConsumer, pathService, resourceWorkflow, new ObjectMapper());
        Map<String, Object> result = service.submit(1L, "same-key", request);

        assertEquals("int-first", result.get("interactionCode"));
        verify(mapper).selectInteractionByRequestForUpdate("same-key");
        verify(mapper, never()).insertOutbox(any());
        verifyNoInteractions(profileConsumer, pathService, resourceWorkflow);
    }
}
