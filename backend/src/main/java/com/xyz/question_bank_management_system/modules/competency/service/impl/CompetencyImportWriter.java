package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest.*;
import com.xyz.question_bank_management_system.modules.competency.entity.*;
import com.xyz.question_bank_management_system.modules.competency.mapper.*;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePoint;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeRelation;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgeRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
class CompetencyImportWriter {
    private final OccupationMapper occupationMapper; private final OccupationAliasMapper aliasMapper;
    private final SkillMapper skillMapper; private final OccupationSkillMapper occupationSkillMapper;
    private final KnowledgePointMapper pointMapper; private final SkillKnowledgeMapper skillKnowledgeMapper;
    private final KnowledgeRelationMapper relationMapper;

    @Transactional
    public Counts write(CompetencyImportValidateRequest request) {
        Counts counts = new Counts(); Map<String, Long> occupations = new HashMap<>(); Map<String, Long> skills = new HashMap<>(); Map<String, Long> points = new HashMap<>();
        for (OccupationInput input : request.getOccupations()) {
            Occupation existing = occupationMapper.selectBySource(request.getSourceName(), input.getSourceRef()); Occupation entity = new Occupation();
            entity.setNameZh(input.getNameZh().trim()); entity.setNameEn(blank(input.getNameEn())); entity.setCategoryCode(blank(input.getCategoryCode())); entity.setDescription(blank(input.getDescription())); entity.setSourceName(request.getSourceName()); entity.setSourceRef(input.getSourceRef().trim()); entity.setVersion(blank(input.getVersion()));
            if (existing == null) { occupationMapper.insert(entity); counts.inserted++; occupations.put(input.getSourceRef(), entity.getId()); }
            else { entity.setId(existing.getId()); occupationMapper.updateImported(entity); counts.updated++; occupations.put(input.getSourceRef(), entity.getId()); }
        }
        for (SkillInput input : request.getSkills()) {
            Skill existing=skillMapper.selectBySource(request.getSourceName(), input.getSourceRef()); Skill entity=new Skill(); entity.setNameZh(input.getNameZh().trim()); entity.setSkillType(defaultValue(input.getSkillType(),"technical")); entity.setDescription(blank(input.getDescription())); entity.setSourceName(request.getSourceName()); entity.setSourceRef(input.getSourceRef().trim());
            if(existing==null){skillMapper.insert(entity);counts.inserted++;skills.put(input.getSourceRef(),entity.getId());}else{entity.setId(existing.getId());skillMapper.updateImported(entity);counts.updated++;skills.put(input.getSourceRef(),entity.getId());}
        }
        for (KnowledgePointInput input : request.getKnowledgePoints()) {
            KnowledgePoint existing=pointMapper.selectByCode(input.getCode()); KnowledgePoint entity=new KnowledgePoint(); entity.setName(input.getName().trim()); entity.setCode(input.getCode().trim()); entity.setLevel(input.getLevel()==null?1:input.getLevel()); entity.setKnowledgeType(defaultValue(input.getKnowledgeType(),"concept")); entity.setDifficulty(input.getDifficulty()==null?3:input.getDifficulty()); entity.setDescription(blank(input.getDescription()));
            if(existing==null){pointMapper.insert(entity);counts.inserted++;points.put(input.getCode(),entity.getId());}else{entity.setId(existing.getId());entity.setParentId(existing.getParentId());pointMapper.update(entity);counts.updated++;points.put(input.getCode(),entity.getId());}
        }
        for (KnowledgePointInput input : request.getKnowledgePoints()) if (input.getParentCode()!=null&&!input.getParentCode().isBlank()) { KnowledgePoint child=pointMapper.selectByCode(input.getCode()); KnowledgePoint parent=pointMapper.selectByCode(input.getParentCode()); child.setParentId(parent.getId()); pointMapper.update(child); }
        for (OccupationAliasInput input : request.getOccupationAliases()) { Long occupationId=occupations.get(input.getOccupationSourceRef()); String type=defaultValue(input.getAliasType(),"market"); if(aliasMapper.selectByBusinessKey(occupationId,input.getAliasName().trim(),type)==null){OccupationAlias entity=new OccupationAlias();entity.setOccupationId(occupationId);entity.setAliasName(input.getAliasName().trim());entity.setAliasType(type);aliasMapper.insert(entity);counts.inserted++;} }
        for (OccupationSkillInput input : request.getOccupationSkills()) { Long occupationId=occupations.get(input.getOccupationSourceRef()); Long skillId=skills.get(input.getSkillSourceRef()); String type=defaultValue(input.getRequirementType(),"essential"); OccupationSkill existing=occupationSkillMapper.selectByBusinessKey(occupationId,skillId,type); OccupationSkill entity=new OccupationSkill();entity.setOccupationId(occupationId);entity.setSkillId(skillId);entity.setRequirementType(type);entity.setImportanceScore(input.getImportanceScore());entity.setRequiredLevel(input.getRequiredLevel());entity.setSourceRef(blank(input.getSourceRef()));if(existing==null){occupationSkillMapper.insert(entity);counts.inserted++;}else{entity.setId(existing.getId());occupationSkillMapper.update(entity);counts.updated++;} }
        for (SkillKnowledgeInput input : request.getSkillKnowledge()) { Long skillId=skills.get(input.getSkillSourceRef()); Long pointId=pointId(input.getKnowledgeCode(),points); String type=defaultValue(input.getRequirementType(),"core"); SkillKnowledge existing=skillKnowledgeMapper.selectByBusinessKey(skillId,pointId,type); SkillKnowledge entity=new SkillKnowledge();entity.setSkillId(skillId);entity.setKnowledgePointId(pointId);entity.setRequirementType(type);entity.setWeight(defaultDecimal(input.getWeight()));entity.setConfidence(defaultDecimal(input.getConfidence()));entity.setSourceType(defaultValue(input.getSourceType(),"manual"));entity.setSourceRef(blank(input.getSourceRef()));entity.setEvidenceText(blank(input.getEvidenceText()));if(existing==null){skillKnowledgeMapper.insert(entity);counts.inserted++;}else{entity.setId(existing.getId());skillKnowledgeMapper.update(entity);counts.updated++;} }
        for (KnowledgeRelationInput input : request.getKnowledgeRelations()) { Long sourceId=pointId(input.getSourceCode(),points); Long targetId=pointId(input.getTargetCode(),points); String type=defaultValue(input.getRelationType(),"prerequisite"); KnowledgeRelation existing=relationMapper.selectByBusinessKey(sourceId,targetId,type); KnowledgeRelation entity=new KnowledgeRelation();entity.setSourceId(sourceId);entity.setTargetId(targetId);entity.setRelationType(type);entity.setWeight(defaultDecimal(input.getWeight()));entity.setConfidence(defaultDecimal(input.getConfidence()));entity.setSourceType(defaultValue(input.getSourceType(),"import"));entity.setDescription(blank(input.getDescription()));if(existing==null){relationMapper.insert(entity);counts.inserted++;}else{entity.setId(existing.getId());relationMapper.updateImported(entity);counts.updated++;} }
        return counts;
    }
    private Long pointId(String code, Map<String,Long> points){Long id=points.get(code);return id!=null?id:pointMapper.selectByCode(code).getId();}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();} private String defaultValue(String value,String fallback){String normalized=blank(value);return normalized==null?fallback:normalized.toLowerCase(Locale.ROOT);} private BigDecimal defaultDecimal(BigDecimal value){return value==null?BigDecimal.ONE:value;} static class Counts { int inserted; int updated; }
}
