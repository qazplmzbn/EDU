package com.xyz.question_bank_management_system.modules.competency.dto;

import lombok.Data;

@Data public class CareerMappingReviewRequest { private String decision; private Long occupationId; private Long skillId; private Long courseId; private String moduleExternalId; private Long knowledgePointId; private String reason; }
