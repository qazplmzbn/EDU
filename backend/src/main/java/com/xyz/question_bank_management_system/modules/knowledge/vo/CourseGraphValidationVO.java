package com.xyz.question_bank_management_system.modules.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseGraphValidationVO {
    private String courseCode;
    private String courseName;
    private String schemaVersion;
    private String mode;
    private Counts counts = new Counts();
    private List<Issue> errors = new ArrayList<>();
    private List<Issue> warnings = new ArrayList<>();
    private String sourceFileHash;
    private String normalizedHash;
    private String validationHash;
    private boolean valid;

    @Data
    public static class Counts {
        private int course;
        private int module;
        private int category;
        private int knowledgePoint;
        private int contains;
        private int prerequisite;
        private int similar;

        public int nodeCount() {
            return course + module + category + knowledgePoint;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        private String severity;
        private String issueCode;
        private String locationType;
        private String locationCode;
        private String message;
    }
}
