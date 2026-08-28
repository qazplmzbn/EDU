package com.xyz.question_bank_management_system.modules.source.service.impl;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.source.dto.EvidenceLinkRequest;
import com.xyz.question_bank_management_system.modules.source.entity.KnowledgeSource;
import com.xyz.question_bank_management_system.modules.source.entity.SourceChunk;
import com.xyz.question_bank_management_system.modules.source.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceEvidenceServiceImplTest {
 @Mock FileAssetMapper fileMapper; @Mock SourceDocumentMapper documentMapper; @Mock SourceChunkMapper chunkMapper; @Mock EvidenceLinkMapper linkMapper; @Mock KnowledgePointMapper pointMapper; @Mock QbLearningResourceMapper resourceMapper;
 @Test void linkKnowledge_persistsValidatedEvidence(){ KnowledgePoint point=new KnowledgePoint();point.setId(1L);SourceChunk chunk=new SourceChunk();chunk.setId(9L);when(pointMapper.selectById(1L)).thenReturn(point);when(chunkMapper.selectById(9L)).thenReturn(chunk);doAnswer(i->{((KnowledgeSource)i.getArgument(0)).setId(7L);return 1;}).when(linkMapper).upsertKnowledge(any());EvidenceLinkRequest req=new EvidenceLinkRequest();req.setSourceChunkId(9L);req.setSupportType("definition");req.setConfidence(java.math.BigDecimal.ONE);Long id=new SourceEvidenceServiceImpl(fileMapper,documentMapper,chunkMapper,linkMapper,pointMapper,resourceMapper).linkKnowledge(1L,req,99L);assertEquals(7L,id);ArgumentCaptor<KnowledgeSource> cap=ArgumentCaptor.forClass(KnowledgeSource.class);verify(linkMapper).upsertKnowledge(cap.capture());assertEquals("definition",cap.getValue().getSupportType());assertEquals(99L,cap.getValue().getReviewedBy()); }
}
