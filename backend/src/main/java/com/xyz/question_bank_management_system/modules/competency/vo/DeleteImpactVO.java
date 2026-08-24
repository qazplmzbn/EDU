package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DeleteImpactVO {
    private boolean canDelete;
    private Map<String, Long> referenceCounts = new LinkedHashMap<>();
    private List<String> blockingReferences = List.of();
    private List<String> detachableReferences = List.of();
    private String message;
}
