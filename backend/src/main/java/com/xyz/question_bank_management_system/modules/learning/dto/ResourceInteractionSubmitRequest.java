package com.xyz.question_bank_management_system.modules.learning.dto;
import lombok.Data;import java.time.OffsetDateTime;
@Data public class ResourceInteractionSubmitRequest {private String generatedQuestionCode;private Object answer;private String actionOrigin;private OffsetDateTime clientOccurredAt;}
