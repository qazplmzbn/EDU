package com.xyz.question_bank_management_system.modules.knowledge.service;

import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgePointSaveRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import java.util.List;

public interface KnowledgePointService {
    List<KnowledgePoint> list();
    Long create(KnowledgePointSaveRequest request);
    void update(Long id, KnowledgePointSaveRequest request);
    DeleteImpactVO deleteImpact(Long id);
    void delete(Long id, Long operatorId);
}
