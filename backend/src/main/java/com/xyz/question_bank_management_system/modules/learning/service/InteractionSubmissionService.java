package com.xyz.question_bank_management_system.modules.learning.service;
import com.xyz.question_bank_management_system.modules.learning.dto.ResourceInteractionSubmitRequest;import java.util.Map;
public interface InteractionSubmissionService {Map<String,Object> submit(Long userId,String requestId,ResourceInteractionSubmitRequest request);Map<String,Object> result(Long userId,String interactionCode);Map<String,Object> applyProfileEvidence(Long interactionId);}
