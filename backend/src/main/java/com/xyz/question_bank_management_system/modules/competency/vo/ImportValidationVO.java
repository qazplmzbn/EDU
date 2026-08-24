package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportValidationVO {
    private boolean valid;
    private String validationHash;
    private int fetchedCount;
    private int insertedCount;
    private int updatedCount;
    private int restoredCount;
    private int skippedCount;
    private int failedCount;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private String entityType;
        private String businessKey;
        private String action;
        private String message;
        public Item() { }
        public Item(String entityType, String businessKey, String action, String message) {
            this.entityType = entityType; this.businessKey = businessKey; this.action = action; this.message = message;
        }
    }
}
