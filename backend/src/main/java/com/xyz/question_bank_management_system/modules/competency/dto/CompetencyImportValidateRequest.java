package com.xyz.question_bank_management_system.modules.competency.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompetencyImportValidateRequest {
    private String sourceName;
    private String syncVersion;
    private List<OccupationInput> occupations = new ArrayList<>();
    private List<OccupationAliasInput> occupationAliases = new ArrayList<>();
    private List<SkillInput> skills = new ArrayList<>();
    private List<OccupationSkillInput> occupationSkills = new ArrayList<>();
    private List<KnowledgePointInput> knowledgePoints = new ArrayList<>();
    private List<SkillKnowledgeInput> skillKnowledge = new ArrayList<>();
    private List<KnowledgeRelationInput> knowledgeRelations = new ArrayList<>();

    @Data public static class OccupationInput { private String sourceRef; private String nameZh; private String nameEn; private String categoryCode; private String description; private String version; }
    @Data public static class OccupationAliasInput { private String occupationSourceRef; private String aliasName; private String aliasType; }
    @Data public static class SkillInput { private String sourceRef; private String nameZh; private String skillType; private String description; }
    @Data public static class OccupationSkillInput { private String occupationSourceRef; private String skillSourceRef; private String requirementType; private BigDecimal importanceScore; private BigDecimal requiredLevel; private String sourceRef; }
    @Data public static class KnowledgePointInput { private String code; private String name; private String parentCode; private Integer level; private String knowledgeType; private Integer difficulty; private String description; }
    @Data public static class SkillKnowledgeInput { private String skillSourceRef; private String knowledgeCode; private String requirementType; private BigDecimal weight; private BigDecimal confidence; private String sourceType; private String sourceRef; private String evidenceText; }
    @Data public static class KnowledgeRelationInput { private String sourceCode; private String targetCode; private String relationType; private BigDecimal weight; private BigDecimal confidence; private String sourceType; private String description; }
}
