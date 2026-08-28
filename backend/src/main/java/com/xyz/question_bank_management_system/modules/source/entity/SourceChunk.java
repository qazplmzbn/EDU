package com.xyz.question_bank_management_system.modules.source.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class SourceChunk { private Long id; private Long documentId; private Integer chunkIndex; private String sectionTitle; private Integer pageStart; private Integer pageEnd; private String content; private String contentHash; private Integer tokenCount; private String vectorRef; private LocalDateTime createdAt; }
