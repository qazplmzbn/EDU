package com.xyz.question_bank_management_system.modules.knowledge.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class GraphVersionRelationRequest {
    private List<RelationItem> relations = new ArrayList<>();

    @Data
    public static class RelationItem {
        private String relationCode;
        private Long sourceKnowledgePointId;
        private Long targetKnowledgePointId;
        private String relationType;
        private BigDecimal weight;
        private BigDecimal confidence;
        private String sourceType;
        private List<Long> sourceChunkIds = new ArrayList<>();
    }
}
