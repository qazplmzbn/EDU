package com.xyz.question_bank_management_system.modules.profile.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StageLearningEvaluationVO {
    private Long evaluationId;
    private Boolean generated;
    private Long studentId;
    private String studentName;
    private String stageKey;
    private String stageName;
    private LocalDate stageStart;
    private LocalDate stageEnd;
    private LocalDateTime generatedAt;

    private Integer abilityScore;
    private Integer attemptCount;
    private Integer completedAttemptCount;
    private Double averageScore;
    private Double masteryAverage;

    private String overallLevel;
    private String summary;
    private String algorithmStatus;
    private String algorithmPlaceholder;

    private List<Dimension> dimensions = new ArrayList<>();
    private List<WeakKnowledgePoint> weakKnowledgePoints = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public Long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Long evaluationId) { this.evaluationId = evaluationId; }
    public Boolean getGenerated() { return generated; }
    public void setGenerated(Boolean generated) { this.generated = generated; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStageKey() { return stageKey; }
    public void setStageKey(String stageKey) { this.stageKey = stageKey; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public LocalDate getStageStart() { return stageStart; }
    public void setStageStart(LocalDate stageStart) { this.stageStart = stageStart; }
    public LocalDate getStageEnd() { return stageEnd; }
    public void setStageEnd(LocalDate stageEnd) { this.stageEnd = stageEnd; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Integer getAbilityScore() { return abilityScore; }
    public void setAbilityScore(Integer abilityScore) { this.abilityScore = abilityScore; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Integer getCompletedAttemptCount() { return completedAttemptCount; }
    public void setCompletedAttemptCount(Integer completedAttemptCount) { this.completedAttemptCount = completedAttemptCount; }
    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
    public Double getMasteryAverage() { return masteryAverage; }
    public void setMasteryAverage(Double masteryAverage) { this.masteryAverage = masteryAverage; }
    public String getOverallLevel() { return overallLevel; }
    public void setOverallLevel(String overallLevel) { this.overallLevel = overallLevel; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getAlgorithmStatus() { return algorithmStatus; }
    public void setAlgorithmStatus(String algorithmStatus) { this.algorithmStatus = algorithmStatus; }
    public String getAlgorithmPlaceholder() { return algorithmPlaceholder; }
    public void setAlgorithmPlaceholder(String algorithmPlaceholder) { this.algorithmPlaceholder = algorithmPlaceholder; }
    public List<Dimension> getDimensions() { return dimensions; }
    public void setDimensions(List<Dimension> dimensions) { this.dimensions = dimensions; }
    public List<WeakKnowledgePoint> getWeakKnowledgePoints() { return weakKnowledgePoints; }
    public void setWeakKnowledgePoints(List<WeakKnowledgePoint> weakKnowledgePoints) { this.weakKnowledgePoints = weakKnowledgePoints; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public static class Dimension {
        private String code;
        private String name;
        private Integer score;
        private String level;
        private String description;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class WeakKnowledgePoint {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double masteryValue;
        private Integer attemptCount;

        public Long getKnowledgePointId() { return knowledgePointId; }
        public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
        public String getKnowledgePointName() { return knowledgePointName; }
        public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
        public Double getMasteryValue() { return masteryValue; }
        public void setMasteryValue(Double masteryValue) { this.masteryValue = masteryValue; }
        public Integer getAttemptCount() { return attemptCount; }
        public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    }
}
