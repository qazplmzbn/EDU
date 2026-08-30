package com.xyz.question_bank_management_system.modules.knowledge.service;

import com.xyz.question_bank_management_system.modules.knowledge.dto.GraphVersionRelationRequest;
import java.util.Map;

public interface KnowledgeGraphVersionService {
    Map<String,Object> createDraft(Long courseId, String description, Long operatorId);
    Map<String,Object> replaceRelations(String versionCode, GraphVersionRelationRequest request, Long operatorId);
    Map<String,Object> validate(String versionCode);
    Map<String,Object> publish(String versionCode);
}
