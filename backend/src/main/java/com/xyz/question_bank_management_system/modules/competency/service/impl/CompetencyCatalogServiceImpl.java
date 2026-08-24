package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.entity.*;
import com.xyz.question_bank_management_system.modules.competency.mapper.*;
import com.xyz.question_bank_management_system.modules.competency.service.CompetencyCatalogService;
import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompetencyCatalogServiceImpl implements CompetencyCatalogService {
    private final OccupationMapper occupationMapper;
    private final SkillMapper skillMapper;
    private final OccupationAliasMapper occupationAliasMapper;
    private final OccupationSkillMapper occupationSkillMapper;
    private final SkillKnowledgeMapper skillKnowledgeMapper;
    private final AuditLogService auditLogService;

    @Override public PageResponse<Occupation> occupations(String keyword, Integer page, Integer size) {
        int safePage = page == null ? 1 : Math.max(1, page); int safeSize = size == null ? 20 : Math.min(100, Math.max(1, size));
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(safePage, safeSize, occupationMapper.countPage(query), occupationMapper.selectPage(query, (long) (safePage - 1) * safeSize, safeSize));
    }
    @Override public Occupation occupation(Long id) { return required(occupationMapper.selectById(id), "Occupation not found"); }
    @Override public List<OccupationSkill> occupationSkills(Long id) { occupation(id); return occupationSkillMapper.selectByOccupationId(id); }
    @Override public PageResponse<Skill> skills(String keyword, Integer page, Integer size) {
        int safePage = page == null ? 1 : Math.max(1, page); int safeSize = size == null ? 20 : Math.min(100, Math.max(1, size));
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(safePage, safeSize, skillMapper.countPage(query), skillMapper.selectPage(query, (long) (safePage - 1) * safeSize, safeSize));
    }
    @Override public Skill skill(Long id) { return required(skillMapper.selectById(id), "Skill not found"); }
    @Override public List<SkillKnowledge> skillKnowledge(Long id) { skill(id); return skillKnowledgeMapper.selectBySkillId(id); }
    @Override public DeleteImpactVO occupationDeleteImpact(Long id) { occupation(id); return occupationImpact(id); }
    @Override public DeleteImpactVO skillDeleteImpact(Long id) { skill(id); return skillImpact(id); }

    @Override @Transactional
    public void deleteOccupation(Long id, Long operatorId) {
        Occupation entity = required(occupationMapper.selectByIdForUpdate(id), "Occupation not found");
        rejectIfReferenced(occupationImpact(id)); occupationMapper.softDelete(id);
        auditLogService.recordRequired(operatorId, "OCCUPATION_DELETE", "occupation", id, entity, Map.of("isDeleted", 1));
    }
    @Override @Transactional
    public void deleteSkill(Long id, Long operatorId) {
        Skill entity = required(skillMapper.selectByIdForUpdate(id), "Skill not found");
        rejectIfReferenced(skillImpact(id)); skillMapper.softDelete(id);
        auditLogService.recordRequired(operatorId, "SKILL_DELETE", "skill", id, entity, Map.of("isDeleted", 1));
    }
    @Override @Transactional public void detachOccupationAlias(Long id, Long operatorId) { detach(id, occupationAliasMapper.deleteById(id), operatorId, "OCCUPATION_ALIAS_DETACH", "occupation_alias"); }
    @Override @Transactional public void detachOccupationSkill(Long id, Long operatorId) { detach(id, occupationSkillMapper.deleteById(id), operatorId, "OCCUPATION_SKILL_DETACH", "occupation_skill"); }
    @Override @Transactional public void detachSkillKnowledge(Long id, Long operatorId) { detach(id, skillKnowledgeMapper.deleteById(id), operatorId, "SKILL_KNOWLEDGE_DETACH", "skill_knowledge"); }

    private void detach(Long id, int affected, Long operatorId, String action, String entityType) {
        if (affected != 1) throw BizException.of(ErrorCode.NOT_FOUND, "Relation not found");
        auditLogService.recordRequired(operatorId, action, entityType, id, Map.of("id", id), null);
    }
    private DeleteImpactVO occupationImpact(Long id) { return impact(Map.of("occupation_alias", occupationAliasMapper.countByOccupationId(id), "occupation_skill", occupationSkillMapper.countByOccupationId(id)), Set.of("occupation_alias", "occupation_skill")); }
    private DeleteImpactVO skillImpact(Long id) { return impact(Map.of("occupation_skill", occupationSkillMapper.countBySkillId(id), "skill_knowledge", skillKnowledgeMapper.countBySkillId(id)), Set.of("occupation_skill", "skill_knowledge")); }
    private DeleteImpactVO impact(Map<String, Long> counts, Set<String> detachable) {
        DeleteImpactVO result = new DeleteImpactVO(); result.setReferenceCounts(new LinkedHashMap<>(counts));
        List<String> blockers = counts.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).sorted().toList();
        result.setBlockingReferences(blockers); result.setDetachableReferences(blockers.stream().filter(detachable::contains).toList());
        result.setCanDelete(blockers.isEmpty()); result.setMessage(blockers.isEmpty() ? "No active references" : "Detach blocking relations before deletion"); return result;
    }
    private void rejectIfReferenced(DeleteImpactVO impact) { if (!impact.isCanDelete()) throw BizException.of(ErrorCode.CONFLICT, impact.getMessage()); }
    private <T> T required(T value, String message) { if (value == null) throw BizException.of(ErrorCode.NOT_FOUND, message); return value; }
}
