package com.xyz.question_bank_management_system.modules.agent.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ResourceItem {
    private Long id;
    private Long bundleId;
    private String itemCode;
    private String generatedQuestionCode;
    private String itemType;
    private String purpose;
    private String visibility;
    private String title;
    private String contentJson;
    private BigDecimal questionDifficulty;
    private String cognitiveLevel;
    private String gradingKeyJson;
    private Integer orderNo;
    private String status;
    private String normalizedTextHash;
    private Long simhash64;
    private LocalDateTime createdAt;

    /** Knowledge assessment is never visible through the ordinary resource endpoint. */
    public void setVisibility(String visibility) {
        this.visibility = "KNOWLEDGE_ASSESSMENT".equals(this.purpose)
                ? "HIDDEN_UNTIL_ASSESSMENT" : visibility;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
        if (purpose != null && (purpose.contains("PRACTICE") || purpose.contains("ASSESSMENT"))) {
            this.itemType = "generated_question";
        }
    }

    public void setGradingKeyJson(String gradingKeyJson) {
        this.gradingKeyJson = normalizeChoiceKey(gradingKeyJson);
    }

    private String normalizeChoiceKey(String value) {
        if (value == null || value.contains("\"standardAnswer\"")) return value;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"(?:correctOption|correctOptionId|correctAnswer)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(value);
        if (matcher.find()) {
            return value.replaceFirst("\\{", "{\\\"standardAnswer\\\":\\\"" + matcher.group(1) + "\\\",");
        }
        java.util.regex.Matcher chinese = java.util.regex.Pattern
                .compile("\\u6b63\\u786e\\u9009\\u9879\\u4e3a\\s*([A-D])")
                .matcher(value);
        return chinese.find() ? "{\"standardAnswer\":\"" + chinese.group(1) + "\",\"rawKey\":" + quote(value) + "}" : value;
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
