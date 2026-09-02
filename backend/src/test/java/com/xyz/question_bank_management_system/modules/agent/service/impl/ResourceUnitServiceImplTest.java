package com.xyz.question_bank_management_system.modules.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.agent.entity.ResourceUnit;
import com.xyz.question_bank_management_system.modules.agent.mapper.PersonalizedResourceMapper;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeGraphVersion;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgeGraphVersionMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.repository.Neo4jKnowledgeGraphRepository;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPath;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathItem;
import com.xyz.question_bank_management_system.modules.learning.entity.LearningPathVersion;
import com.xyz.question_bank_management_system.modules.learning.mapper.LearningPathV1Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceUnitServiceImplTest {
    @Mock LearningPathV1Mapper pathMapper;
    @Mock PersonalizedResourceMapper resourceMapper;
    @Mock KnowledgeGraphVersionMapper versionMapper;
    @Mock KnowledgePointMapper pointMapper;
    @Mock Neo4jKnowledgeGraphRepository graph;

    @Test void aggregateWithoutStartCode_reusesReadyUnitBeforePlanningAnother() {
        LearningPath path = new LearningPath(); path.setId(1L); path.setCourseId(2L);
        LearningPathVersion version = new LearningPathVersion(); version.setId(3L);
        LearningPathItem step = new LearningPathItem(); step.setId(4L); step.setStage("LEARNING");
        ResourceUnit existing = new ResourceUnit(); existing.setId(5L); existing.setStatus("READY");
        when(pathMapper.selectByCode("path-x")).thenReturn(path);
        when(pathMapper.selectActiveVersion(1L)).thenReturn(version);
        when(pathMapper.selectSteps(3L)).thenReturn(List.of(step));
        when(resourceMapper.selectUnitByVersionAndStep(3L, 4L)).thenReturn(existing);

        ResourceUnitServiceImpl service = new ResourceUnitServiceImpl(pathMapper, resourceMapper, versionMapper, pointMapper, graph, new ObjectMapper());
        assertSame(existing, service.aggregate("path-x", null));
        verify(resourceMapper, never()).insertUnit(any());
        verifyNoInteractions(versionMapper, pointMapper, graph);
    }
}
