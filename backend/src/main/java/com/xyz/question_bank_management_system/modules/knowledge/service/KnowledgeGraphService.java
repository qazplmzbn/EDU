package com.xyz.question_bank_management_system.modules.knowledge.service;

import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgeGraphExtractionRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import com.xyz.question_bank_management_system.modules.knowledge.vo.KnowledgeGraphExtractionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeGraphService {
    List<QbKnowledgeRelation> relations();

    KnowledgeGraphExtractionVO extract(KnowledgeGraphExtractionRequest request);

    KnowledgeGraphExtractionVO extractFromFile(MultipartFile file, Boolean autoSave, String providerKey);

    Long createRelation(QbKnowledgeRelation relation);

    void updateRelation(Long relationId, QbKnowledgeRelation relation);

    void deleteRelation(Long relationId);
}
