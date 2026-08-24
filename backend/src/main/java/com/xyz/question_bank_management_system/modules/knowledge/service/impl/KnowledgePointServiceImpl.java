package com.xyz.question_bank_management_system.modules.knowledge.service.impl;

import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillKnowledgeMapper;
import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import com.xyz.question_bank_management_system.modules.knowledge.dto.KnowledgePointSaveRequest;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgeRelationMapper;
import com.xyz.question_bank_management_system.modules.knowledge.service.KnowledgePointService;
import com.xyz.question_bank_management_system.modules.learning.mapper.QbLearningResourceMapper;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final SkillKnowledgeMapper skillKnowledgeMapper;
    private final QbLearningResourceMapper resourceMapper;
    private final AuditLogService auditLogService;
    @Override public List<KnowledgePoint> list() { return pointMapper.selectAll(); }
    @Override @Transactional public Long create(KnowledgePointSaveRequest request) { validate(request, null); KnowledgePoint point = copy(request, new KnowledgePoint()); pointMapper.insert(point); return point.getId(); }
    @Override @Transactional public void update(Long id, KnowledgePointSaveRequest request) { KnowledgePoint existing = required(pointMapper.selectById(id)); validate(request,id); pointMapper.update(copy(request,existing)); }
    @Override public DeleteImpactVO deleteImpact(Long id) { required(pointMapper.selectById(id)); return impact(id); }
    @Override @Transactional public void delete(Long id, Long operatorId) { KnowledgePoint point = required(pointMapper.selectById(id)); DeleteImpactVO impact = impact(id); if (!impact.isCanDelete()) throw BizException.of(ErrorCode.CONFLICT, impact.getMessage()); pointMapper.softDelete(id); auditLogService.recordRequired(operatorId,"KNOWLEDGE_POINT_DELETE","knowledge_point",id,point,Map.of("isDeleted",1)); }
    private void validate(KnowledgePointSaveRequest request, Long currentId) {
        if (request.getParentId()!=null) { KnowledgePoint parent=required(pointMapper.selectById(request.getParentId())); if (Objects.equals(parent.getId(),currentId)) throw BizException.of(ErrorCode.PARAM_ERROR,"Knowledge point cannot be its own parent"); }
        String type=request.getKnowledgeType(); if (type!=null&&!List.of("concept","method","tool","principle","module").contains(type)) throw BizException.of(ErrorCode.PARAM_ERROR,"Invalid knowledge type");
    }
    private KnowledgePoint copy(KnowledgePointSaveRequest request, KnowledgePoint point) { point.setName(request.getName().trim()); point.setCode(blankToNull(request.getCode())); point.setParentId(request.getParentId()); point.setLevel(request.getLevel()==null?1:request.getLevel()); point.setKnowledgeType(request.getKnowledgeType()==null||request.getKnowledgeType().isBlank()?"concept":request.getKnowledgeType()); point.setDifficulty(request.getDifficulty()==null?3:request.getDifficulty()); point.setDescription(blankToNull(request.getDescription())); return point; }
    private DeleteImpactVO impact(Long id) { Map<String,Long> counts=new LinkedHashMap<>(); counts.put("knowledge_point_children",pointMapper.countChildren(id)); counts.put("knowledge_relation",relationMapper.countByKnowledgePointId(id)); counts.put("skill_knowledge",skillKnowledgeMapper.countByKnowledgePointId(id)); counts.put("qb_learning_resource",resourceMapper.countActiveByKnowledgePointId(id)); List<String> blockers=counts.entrySet().stream().filter(e->e.getValue()>0).map(Map.Entry::getKey).toList(); DeleteImpactVO result=new DeleteImpactVO(); result.setReferenceCounts(counts); result.setBlockingReferences(blockers); result.setDetachableReferences(blockers.stream().filter(v->v.equals("knowledge_relation")||v.equals("skill_knowledge")).toList()); result.setCanDelete(blockers.isEmpty()); result.setMessage(blockers.isEmpty()?"No active references":"Detach or reassign blocking references before deletion"); return result; }
    private KnowledgePoint required(KnowledgePoint value) { if(value==null) throw BizException.of(ErrorCode.NOT_FOUND,"Knowledge point not found"); return value; }
    private String blankToNull(String value){ return value==null||value.isBlank()?null:value.trim(); }
}
