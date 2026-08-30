package com.xyz.question_bank_management_system.modules.agent.entity;
import lombok.Data;import java.time.LocalDateTime;
@Data public class ResourceReview {private Long id;private Long jobId;private Long bundleId;private Long blueprintId;private String expertRole;private String result;private String issueType;private String location;private String description;private String repairTarget;private String repairScope;private String repairAction;private String repairInstruction;private Integer critical;private Integer roundNo;private String reportJson;private LocalDateTime createdAt;}
