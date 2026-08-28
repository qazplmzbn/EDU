package com.xyz.question_bank_management_system.modules.recommendation.service.impl;

import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.QbKnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.*;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentProfileSnapshotMapper;
import com.xyz.question_bank_management_system.modules.recommendation.dto.RecommendationStatusRequest;
import com.xyz.question_bank_management_system.modules.recommendation.mapper.RecommendationMapper;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class ResourceRecommendationServiceImplTest {
 @Mock RecommendationMapper mapper; @Mock QbKnowledgePointMapper points; @Mock QbLearningResourceMapper resources; @Mock QbLearningResourceTargetMapper targets; @Mock ResourceKnowledgeMapper relations; @Mock StudentProfileSnapshotMapper snapshots;
 @Test void updateStatus_rejectsInvalidStatus(){ RecommendationStatusRequest req=new RecommendationStatusRequest();req.setStatus("bad");var service=new ResourceRecommendationServiceImpl(mapper,points,resources,targets,relations,snapshots);assertThrows(BizException.class,()->service.updateStatus(1L,req,2L));verifyNoInteractions(mapper); }
}
