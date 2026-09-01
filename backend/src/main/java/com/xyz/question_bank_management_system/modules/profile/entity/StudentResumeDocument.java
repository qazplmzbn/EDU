package com.xyz.question_bank_management_system.modules.profile.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class StudentResumeDocument { private Long id; private Long userId; private Long fileAssetId; private String fileName; private String fileHash; private String parsedText; private String parseStatus; private String parserVersion; private String consentVersion; private LocalDateTime createdAt; private LocalDateTime parsedAt; private String errorMessage; }
