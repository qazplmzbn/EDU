package com.xyz.question_bank_management_system.modules.learning.vo;

import com.xyz.question_bank_management_system.modules.knowledge.entity.QbKnowledgePoint;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningBehavior;
import com.xyz.question_bank_management_system.modules.learning.entity.QbLearningResource;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentKnowledgeState;
import lombok.Data;

import java.util.List;
import java.util.Map;

public final class SmartLearningVO {

    private SmartLearningVO() {
    }

    @Data
    public static class LearningProfile {
        private Integer abilityScore;
        private Long behaviorCount;
        private Long studyDurationSeconds;
        private List<StudentKnowledgeState> mastery;
        private List<QbKnowledgePoint> weakPoints;
        private List<QbLearningBehavior> recentBehaviors;
        private String advice;
    }

    @Data
    public static class LearningRecommendation {
        private List<QbKnowledgePoint> weakPoints;
        private List<QbLearningResource> resources;
        private List<Map<String, Object>> plan;
    }

    @Data
    public static class ResourceRecommendationPublishResult {
        private Long resourceId;
        private String targetType;
        private Long classId;
        private List<Long> studentIds;
        private Integer targetCount;
    }

    @Data
    public static class StudentProfileReport {
        private ReportSummary summary;
        private List<DimensionItem> radar;
        private List<ProfileInsight> insights;
        private List<TrendPoint> scoreTrend;
        private List<DimensionItem> evaluationDistribution;
        private ScoringItemAnalysis scoringItemAnalysis;
        private List<ProfileRecord> records;
    }

    @Data
    public static class ReportSummary {
        private Integer profileScore;
        private Long examCount;
        private Long practiceCount;
        private Long assistantChatCount;
        private Long weakDimensionCount;
        private String updatedAt;
    }

    @Data
    public static class DimensionItem {
        private String name;
        private Integer score;
        private String level;
    }

    @Data
    public static class ProfileInsight {
        private String type;
        private String title;
        private String description;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer score;
        private Integer rawScore;
        private Integer maxScore;
        private String sourceType;
        private Long attemptId;
    }

    @Data
    public static class ProfileRecord {
        private String type;
        private String title;
        private String time;
        private Integer score;
        private Integer maxScore;
        private Integer scoreRate;
        private String summary;
        private Long refId;
        private String detail;
    }

    @Data
    public static class ScoringItemAnalysis {
        private Long attemptCount;
        private String sourceType;
        private String summary;
        private String bestItem;
        private String weakestItem;
        private List<ScoringItem> items;
        private Map<String, ScoringItemAnalysis> variants;

        public double averageRate() {
            if (items == null || items.isEmpty()) {
                return 0;
            }
            return items.stream()
                    .mapToDouble(item -> item.getScoreRate() == null ? 0 : item.getScoreRate())
                    .average()
                    .orElse(0);
        }
    }

    @Data
    public static class ScoringItem {
        private String name;
        private Double scoreRate;
        private Double avgScore;
        private Double maxScore;
    }

    @Data
    public static class LearningPathRecommendation {
        private Long snapshotId;
        private String stage;
        private String goal;
        private Integer days;
        private String updatedAt;
        private StudentPathSummary summary;
        private PathBasis basis;
        private EvidenceSummary evidence;
        private DiagnosisSummary diagnosis;
        private PathStrategy strategy;
        private LlmAdviceBlock llmAdvice;
        private List<PathPhase> phases;
        private List<QbLearningResource> resources;
        private PrintableReportMeta reportMeta;
    }

    @Data
    public static class StudentPathSummary {
        private Integer profileScore;
        private Integer abilityScore;
        private Long behaviorCount;
        private Long studyMinutes;
        private Integer targetDays;
        private String mode;
        private String headline;
        private String pathTypeLabel;
        private String cycleLabel;
        private String primaryProblem;
        private String priorityLabel;
    }

    @Data
    public static class PathBasis {
        private List<String> weakPointNames;
        private List<String> pathPointNames;
        private List<String> weakDimensionNames;
        private Integer relationCount;
        private String advice;
        private String rule;
    }

    @Data
    public static class PathPhase {
        private String key;
        private String title;
        private String description;
        private String goal;
        private String checkpoint;
        private String riskReminder;
        private List<PathNode> nodes;
    }

    @Data
    public static class PathNode {
        private Long knowledgePointId;
        private String title;
        private String code;
        private Double masteryValue;
        private Integer estimatedMinutes;
        private String reason;
        private List<String> tasks;
        private List<QbLearningResource> resources;
        private String checkpoint;
        private List<String> relatedEvidence;
        private String expectedOutcome;
    }

    @Data
    public static class EvidenceSummary {
        private Integer completenessScore;
        private List<EvidenceSection> sections;
        private List<String> dialogueSignals;
        private List<String> missingItems;
    }

    @Data
    public static class EvidenceSection {
        private String title;
        private List<String> bullets;
    }

    @Data
    public static class DiagnosisSummary {
        private List<String> knowledgeGaps;
        private List<String> abilityRisks;
        private List<String> behaviorRisks;
        private List<String> examRisks;
        private List<String> opportunityPoints;
        private String conclusion;
    }

    @Data
    public static class PathStrategy {
        private String mode;
        private String label;
        private String targetCycle;
        private String priority;
        private String reason;
        private List<String> routingBasis;
        private List<String> goals;
    }

    @Data
    public static class LlmAdviceBlock {
        private String headline;
        private List<String> priorityActions;
        private List<String> riskWarnings;
        private List<String> resourceAdvice;
        private List<String> dialogueAdvice;
        private String explanation;
        private String source;
        private String modelName;
    }

    @Data
    public static class PrintableReportMeta {
        private Long snapshotId;
        private String printableTitle;
        private String generatedAt;
        private String generatedBy;
        private Integer evidenceCount;
        private String reportVersion;
        private String llmAdviceSource;
        private String snapshotSavedAt;
    }

    @Data
    public static class LearningPathSnapshotSaved {
        private Long id;
        private String title;
        private String generatedAt;
        private String stage;
        private String goal;
        private Integer days;
    }

    @Data
    public static class LearningPathSnapshotItem {
        private Long id;
        private String title;
        private String summaryText;
        private String stage;
        private String goal;
        private Integer days;
        private String generatedAt;
    }
}
