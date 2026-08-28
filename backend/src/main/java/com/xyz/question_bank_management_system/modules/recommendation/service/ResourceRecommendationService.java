package com.xyz.question_bank_management_system.modules.recommendation.service;
import com.xyz.question_bank_management_system.modules.recommendation.dto.*; import com.xyz.question_bank_management_system.modules.recommendation.entity.*; import java.util.List;
public interface ResourceRecommendationService { List<StudentResourceRecommendation> refresh(Long userId); List<StudentResourceRecommendation> mine(Long userId); void updateStatus(Long recommendationId, RecommendationStatusRequest request, Long userId); Long feedback(Long resourceId, ResourceFeedbackRequest request, Long userId); }
