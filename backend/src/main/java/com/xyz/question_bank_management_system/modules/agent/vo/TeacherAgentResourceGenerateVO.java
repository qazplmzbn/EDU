package com.xyz.question_bank_management_system.modules.agent.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TeacherAgentResourceGenerateVO {

    private String runId;
    private List<AgentTrace> agentTrace = new ArrayList<>();
    private List<AgentDiscussionMessage> discussionMessages = new ArrayList<>();
    private DecisionSummary decisionSummary;
    private List<ResourceDraft> resources = new ArrayList<>();

    @Data
    public static class AgentTrace {
        private String agentId;
        private String agentName;
        private String status;
        private String summary;
        private String modelName;
        private Long llmCallId;
    }

    @Data
    public static class AgentDiscussionMessage {
        private String id;
        private Integer turnIndex;
        private Integer round;
        private String agentId;
        private String agentName;
        private String replyToAgentId;
        private String replyToAgentName;
        private String role;
        private String content;
        private List<String> evidenceRefs = new ArrayList<>();
        private String createdAt;
        private String status;
        private String modelName;
        private Long llmCallId;
    }

    @Data
    public static class DecisionSummary {
        private String status;
        private String headline;
        private String currentProblem;
        private List<String> weakPoints = new ArrayList<>();
        private List<String> recommendedResources = new ArrayList<>();
        private List<String> recommendedPractice = new ArrayList<>();
        private String teacherAction;
        private List<String> evidenceRefs = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceDraft {
        private String draftId;
        private String title;
        private String resourceType;
        private String summary;
        private String content;
        private String sourceUrl;
        private String mediaType;
        private String generationScope;
        private String targetId;
        private String sourceStatus;
        private String publishTarget;
        private VideoAsset videoAsset;
        private List<VideoScene> videoScenes = new ArrayList<>();
        private List<VideoRecommendation> videoRecommendations = new ArrayList<>();
        private Map<String, Object> videoConfig;
        private List<QuizQuestion> quizQuestions = new ArrayList<>();
        private List<ExerciseQuestion> exerciseQuestions = new ArrayList<>();
        private Long knowledgePointId;
        private Long tagId;
        private Map<String, Object> agentOutputs;
        private Map<String, Object> personalizationBasis;
        private ReviewReport reviewReport;
        private ModelSource modelSource;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoScene {
        private String title;
        private String narration;
        private String visualPrompt;
        private String boardText;
        private Integer durationSeconds;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoRecommendation {
        private String title;
        private String platform;
        private String url;
        private String cover;
        private String duration;
        private String viewCount;
        private String reason;
        private List<String> matchedKnowledgePoints = new ArrayList<>();
        private String sourceStatus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoAsset {
        private String title;
        private String platform;
        private String url;
        private String coverUrl;
        private String duration;
        private String renderMode;
        private String sourceStatus;
        private String reason;
        private List<String> matchedKnowledgePoints = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExerciseQuestion {
        private String questionType;
        private String stem;
        private List<String> options = new ArrayList<>();
        private String answer;
        private String explanation;
        private String difficulty;
        private String knowledgePoint;
        private Long knowledgePointId;
        private Long tagId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuizQuestion {
        private String questionType;
        private String stem;
        private List<String> options = new ArrayList<>();
        private String answer;
        private String explanation;
        private String wrongFeedback;
        private String knowledgePoint;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewReport {
        private Integer qualityScore;
        private Integer relevanceScore;
        private Integer consistencyScore;
        private Boolean passed;
        private String comments;
        private List<String> revisionSuggestions = new ArrayList<>();
    }

    @Data
    public static class ModelSource {
        private String generatorModel;
        private String qualityReviewerModel;
        private String consistencyReviewerModel;
        private List<Long> llmCallIds = new ArrayList<>();
    }
}
