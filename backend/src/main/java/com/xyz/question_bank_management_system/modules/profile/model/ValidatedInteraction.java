package com.xyz.question_bank_management_system.modules.profile.model;
import lombok.Data;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
@Data public class ValidatedInteraction {private Long id;private Long interactionSeq;private Long userId;private Long courseId;private BigDecimal scoreNormalized;private BigDecimal questionDifficulty;private BigDecimal gradingConfidence=BigDecimal.ONE;private String questionPurpose;private Map<Long,BigDecimal> knowledgeWeights=new LinkedHashMap<>();}
