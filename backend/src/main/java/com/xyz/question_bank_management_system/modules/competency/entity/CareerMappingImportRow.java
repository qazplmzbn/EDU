package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data public class CareerMappingImportRow { private Long id; private Long batchId; private Integer rowNo; private String occupationLabelEn; private String occupationLabelZh; private String skillRelation; private String skillTitleEn; private String skillTitleZh; private String courseName; private String knowledgeModule; private String knowledgePoint; private String onetKnowledge; private String onetKnowledgeImportance; private String mappingType; private BigDecimal confidence; private String evidence; private Long occupationId; private Long skillId; private Long courseId; private String moduleExternalId; private Long knowledgePointId; private String normalizedMappingType; private String matchStatus; private String matchReason; private Long reviewerId; private LocalDateTime reviewedAt; }
