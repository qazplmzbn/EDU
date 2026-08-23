package com.xyz.question_bank_management_system.modules.knowledge.vo;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgeRelation;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeGraphExtractionVO {
    private Long llmCallId;
    private Integer savedCount;
    private String rawText;
    private List<QbKnowledgeRelation> relations;
}
