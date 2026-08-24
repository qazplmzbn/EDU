package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.competency.entity.Occupation;
import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkill;
import com.xyz.question_bank_management_system.modules.competency.entity.Skill;
import com.xyz.question_bank_management_system.modules.competency.entity.SkillKnowledge;
import com.xyz.question_bank_management_system.modules.competency.vo.DeleteImpactVO;
import java.util.List;

public interface CompetencyCatalogService {
    PageResponse<Occupation> occupations(String keyword, Integer page, Integer size);
    Occupation occupation(Long id);
    List<OccupationSkill> occupationSkills(Long id);
    PageResponse<Skill> skills(String keyword, Integer page, Integer size);
    Skill skill(Long id);
    List<SkillKnowledge> skillKnowledge(Long id);
    DeleteImpactVO occupationDeleteImpact(Long id);
    DeleteImpactVO skillDeleteImpact(Long id);
    void deleteOccupation(Long id, Long operatorId);
    void deleteSkill(Long id, Long operatorId);
    void detachOccupationAlias(Long id, Long operatorId);
    void detachOccupationSkill(Long id, Long operatorId);
    void detachSkillKnowledge(Long id, Long operatorId);
}
